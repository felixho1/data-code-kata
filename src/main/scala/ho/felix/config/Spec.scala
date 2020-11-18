package ho.felix.config

import io.circe.Decoder
import io.circe.parser.parse
import cats.implicits._
import cats._
import ho.felix.util.DataCodeException

import scala.io.Source
import scala.util.Try

case class Spec (ColumnNames: List[String], Offsets: List[String], FixedWidthEncoding: String, IncludeHeader: String, DelimitedEncoding: String)

object Spec {
  implicit val decoderSpecJson: Decoder[Spec] =
    io.circe.generic.semiauto.deriveDecoder[Spec]

  /** Read Spec Json File content */
  def readJsonFile(jsonFilePath: String) = {
    Try(Source.fromFile(jsonFilePath).getLines.mkString)
      .toEither
      .leftMap(err => DataCodeException(s"Error Reading spec.json file -- ${err.toString}"))
  }

  /** Parse Json content */
  def parseJson(jsonStr: String) = {
    (for {
      json <- parse(jsonStr)
      tempObj <- json.as[Spec]
    } yield tempObj).leftMap(err => DataCodeException(s"Error Parsing Json Content -- ${err.toString}"))
  }
}