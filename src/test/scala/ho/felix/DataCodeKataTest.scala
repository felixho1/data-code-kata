package ho.felix

import ho.felix.config.{Input,Spec}
import ho.felix.util.FileUtil
import org.scalatest.funsuite.AnyFunSuite

class DataCodeKataTest extends AnyFunSuite {

  test("Convert Fixed Width to Comma Delimited") {
    val length_list = List(40,15,10,25,15)
    val fixed_length_text = "972D24C3DF621864DC2394D0B4D408812826EC6D16.5           2020-10-242020-10-24 13:38:42      Resolved       "
    val expected = "972D24C3DF621864DC2394D0B4D408812826EC6D,16.5,2020-10-24,2020-10-24 13:38:42,Resolved"
    assert(FileUtil.fixedWidthToCsv(fixed_length_text, length_list) === expected)
  }

  test("Parse Main Method Arguments") {
    val arguments = List("/tmp/spec.json")
    val expected = Input.parseArgs(arguments) match {
      case Left(x) => true
      case Right(x) => false
    }
    assert(expected === true )
  }

  test("Parse Json Content") {
    val jsonString =
      """{
        |    "ColumnNames": [
        |        "f1",
        |        "f2",
        |        "f3",
        |        "f4",
        |        "f5"
        |    ],
        |    "Offsets": [
        |        "40",
        |        "15",
        |        "10",
        |        "25",
        |        "15"
        |    ],
        |    "FixedWidthEncoding": "windows-1252",
        |    "IncludeHeader": "True",
        |    "DelimitedEncoding": "utf-8"
        |}
        |""".stripMargin
    val spec = Spec.parseJson(jsonString).toOption
    assert(spec.isDefined === true)
    assert(spec.map(_.FixedWidthEncoding) === Some("windows-1252"))
    assert(spec.map(_.IncludeHeader) === Some("True"))
    assert(spec.map(_.Offsets.length) === Some(5))
  }
}
