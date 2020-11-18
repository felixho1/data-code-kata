package ho.felix.config

import scala.util.Try
import cats._
import cats.implicits._
import ho.felix.util.DataCodeException

case class Input(specJsonPath: String, inputFilePath: String, outputFilePath: String)
object Input {
  private def getSpecJsonPath(args: List[String]) =
    Try(args(0))
    .toEither
    .leftMap(err => DataCodeException(s"Spec Json File Path (first parameter) undefined -- ${err.toString}"))
  private def getInputFilePath(args: List[String]) =
    Try(args(1))
      .toEither
      .leftMap(err => DataCodeException(s"Input File Path (second parameter) undefined -- ${err.toString}"))
  private def getOutputFilePath(args: List[String]) = Try(args(2))
    .toEither
    .leftMap(err => DataCodeException(s"Output File Path (third parameter) undefined -- ${err.toString}"))

  /** Parse List[String] arguments from main method */
  def parseArgs(args: List[String]) = {
    (getSpecJsonPath(args).toValidatedNel,
      getInputFilePath(args).toValidatedNel,
      getOutputFilePath(args).toValidatedNel)
      .mapN((json, input, output) => Input(specJsonPath=json,inputFilePath=input, outputFilePath=output))
      .toEither
      .leftMap(xx => DataCodeException(xx.map(_.toString).mkString_("\n")))
  }
}
