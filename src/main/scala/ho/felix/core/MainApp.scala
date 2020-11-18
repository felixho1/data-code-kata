package ho.felix.core

import org.slf4j.{Logger, LoggerFactory}
import ho.felix.config.{Input, Spec}
import java.nio.file.{Files, Paths, StandardOpenOption}
import cats.implicits._
import cats.effect._
import fs2._
import ho.felix.util.FileUtil

object MainApp extends IOApp {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def run(args: List[String]): IO[ExitCode] = {
    logger.info(s"args: $args")

    (
      for {
        inputParams <- Input.parseArgs(args)
        jsonStr <- Spec.readJsonFile(inputParams.specJsonPath)
        spec <- Spec.parseJson(jsonStr)
      } yield (inputParams, spec)
    ).fold(ex => {
      IO(logger.error(s"Program terminated because ${ex.error}")).as(ExitCode.Error)
    }, dup => {
      val (inputParams, spec) = dup
      logger.info(s"spec: $spec")
      IO(FileUtil.prepareOutputFile(inputParams.outputFilePath, spec)) >> Stream
        .resource(Blocker[IO])
        .flatMap { blocker =>
          io.file
            .readAll[IO](Paths.get(inputParams.inputFilePath), blocker, 4096)
            .through(_.chunks.through(FileUtil.decodeC(spec.FixedWidthEncoding.toUpperCase)))
            .through(text.lines)
            .filter(fws => fws.trim.length > 0)
            .map(fws => FileUtil.fixedWidthToCsv(fws, spec.Offsets.map(_.toInt)))
            .intersperse("\n")
            .through(FileUtil.encodeToBytes(spec.DelimitedEncoding.toUpperCase))
            .through(io.file.writeAll(Paths.get(inputParams.outputFilePath), blocker, List(StandardOpenOption.CREATE, StandardOpenOption.APPEND)))
        }
        .compile
        .drain >> IO(logger.info("Completed!!"))
        .as(ExitCode.Success)
    })
  }

}
