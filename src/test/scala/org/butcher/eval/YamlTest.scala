package org.butcher.eval

import org.scalatest.{BeforeAndAfterAll}
import io.circe._
import io.circe.generic.auto._
import cats.syntax.either._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

case class Column(name: String, functions: List[String])
case class File(name: String, columns: List[Column])
case class Spec(files: List[File])
case class YamlParsed(spec: Spec)
class YamlTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {
  test("something") {
    val yaml =
      """
        |spec:
        | files:
        |   - name: test.csv
        |     columns:
        |       - name: id
        |         functions:
        |             - f1
        |             - f2
        |       - name: firstName
        |         functions:
        |           - f3
        |           - f4
        |""".stripMargin

    import io.circe.yaml.parser
    val json = parser.parse(yaml)
    val foo = json
      .leftMap(err => err: Error)
      .flatMap(_.as[YamlParsed])
      .valueOr(throw _)
    println(foo)
  }
}
