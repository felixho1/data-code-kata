package ho.felix.util

import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Files, Paths}

import fs2.text.encode
import fs2.{Chunk, Pipe, Pull, Stream}
import ho.felix.config.Spec

import scala.collection.mutable.Builder
import scala.util.Try

object FileUtil {
  private val UTF8_ENCODING = "UTF-8"
  private val utf8BomSeq: Seq[Byte] = Array(0xef.toByte, 0xbb.toByte, 0xbf.toByte).toSeq

  /** Pre-processing for output file */
  def prepareOutputFile(filePath: String, spec: Spec): Unit = {
    if (spec.IncludeHeader.toBoolean)
      Files.write(Paths.get(filePath), s"""${spec.ColumnNames.mkString(",")}\n""".getBytes(StandardCharsets.UTF_8))
    else
      Files.deleteIfExists(Paths.get(filePath))
  }

  /** Convert Fixed Width line to comma delimited line */
  def fixedWidthToCsv(fixedWithText: String, lengthList: List[Int]): String = {
    val (_, value_list) = lengthList.foldLeft((0, List.empty[String])){(dup, len) =>
      (dup._1 + len, dup._2 :+ Try(fixedWithText.substring(dup._1, dup._1 + len).trim).getOrElse(""))
    }
    value_list.mkString(",")
  }

  /** Encodes a stream of `String` in to a stream of bytes using the specified charset. */
  def encodeToBytes[F[_]](charsetName: String): Pipe[F, String, Byte] =
    encode(Charset.forName(charsetName))

  /** Converts encoded `Chunk[Byte]` inputs to `String`. */
  def decodeC[F[_]](charsetName: String): Pipe[F, Chunk[Byte], String] = {
    val selectedCharset = Charset.forName(charsetName)

    /*
     * Returns the number of continuation bytes if `b` is an ASCII byte or a
     * leading byte of a multi-byte sequence, and -1 otherwise.
     */
    def continuationBytes(b: Byte): Int =
      if ((b & 0x80) == 0x00) 0 // ASCII byte
      else if ((b & 0xe0) == 0xc0) 1 // leading byte of a 2 byte seq
      else if ((b & 0xf0) == 0xe0) 2 // leading byte of a 3 byte seq
      else if ((b & 0xf8) == 0xf0) 3 // leading byte of a 4 byte seq
      else -1 // continuation byte or garbage

    /*
     * Returns the length of an incomplete multi-byte sequence at the end of
     * `bs`. If `bs` ends with an ASCII byte or a complete multi-byte sequence,
     * 0 is returned.
     */
    def lastIncompleteBytes(bs: Array[Byte]): Int = {
      /*
       * This is logically the same as this
       * code, but written in a low level way
       * to avoid any allocations and just do array
       * access
       *
       *
       *
        val lastThree = bs.drop(0.max(bs.size - 3)).toArray.reverseIterator
        lastThree
          .map(continuationBytes)
          .zipWithIndex
          .find {
            case (c, _) => c >= 0
          }
          .map {
            case (c, i) => if (c == i) 0 else i + 1
          }
          .getOrElse(0)

       */

      val minIdx = 0.max(bs.length - 3)
      var idx = bs.length - 1
      var counter = 0
      var res = 0
      while (minIdx <= idx) {
        val c = continuationBytes(bs(idx))
        if (c >= 0) {
          if (c != counter)
            res = counter + 1
          // exit the loop
          return res
        }
        idx = idx - 1
        counter = counter + 1
      }
      res
    }

    def processSingleChunk(
                            bldr: Builder[String, List[String]],
                            buffer: Chunk[Byte],
                            nextBytes: Chunk[Byte]
                          ): Chunk[Byte] = {
      // if processing ASCII or largely ASCII buffer is often empty
      val allBytes =
        if (buffer.isEmpty) nextBytes.toArray
        else Array.concat(buffer.toArray, nextBytes.toArray)

      val splitAt = allBytes.length - lastIncompleteBytes(allBytes)

      if (splitAt == allBytes.length) {
        // in the common case of ASCII chars
        // we are in this branch so the next buffer will
        // be empty
        bldr += new String(allBytes, selectedCharset)
        Chunk.empty
      } else if (splitAt == 0)
        Chunk.bytes(allBytes)
      else {
        bldr += new String(allBytes.take(splitAt), selectedCharset)
        Chunk.bytes(allBytes.drop(splitAt))
      }
    }

    def doPull(buf: Chunk[Byte], s: Stream[F, Chunk[Byte]]): Pull[F, String, Unit] =
      s.pull.uncons.flatMap {
        case Some((byteChunks, tail)) =>
          // use local and private mutability here
          var idx = 0
          val size = byteChunks.size
          val bldr = List.newBuilder[String]
          var buf1 = buf
          while (idx < size) {
            val nextBytes = byteChunks(idx)
            buf1 = processSingleChunk(bldr, buf1, nextBytes)
            idx = idx + 1
          }
          Pull.output(Chunk.seq(bldr.result())) >> doPull(buf1, tail)
        case None if buf.nonEmpty =>
          Pull.output1(new String(buf.toArray, selectedCharset))
        case None =>
          Pull.done
      }

    def processByteOrderMark(
                              buffer: Chunk.Queue[Byte] /* or null which we use as an Optional type to avoid boxing */,
                              s: Stream[F, Chunk[Byte]]
                            ): Pull[F, String, Unit] =
      s.pull.uncons1.flatMap {
        case Some((hd, tl)) =>
          val newBuffer0 =
            if (buffer ne null) buffer
            else Chunk.Queue.empty[Byte]

          val newBuffer: Chunk.Queue[Byte] = newBuffer0 :+ hd
          if (newBuffer.size >= 3) {
            val rem =
              if (charsetName == UTF8_ENCODING && newBuffer.startsWith(utf8BomSeq)) newBuffer.drop(3)
              else newBuffer
            doPull(Chunk.empty, Stream.emits(rem.chunks) ++ tl)
          } else
            processByteOrderMark(newBuffer, tl)
        case None =>
          if (buffer ne null)
            doPull(Chunk.empty, Stream.emits(buffer.chunks))
          else Pull.done
      }

    (in: Stream[F, Chunk[Byte]]) => processByteOrderMark(null, in).stream
  }

}
