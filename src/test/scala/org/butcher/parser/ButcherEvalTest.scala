package org.butcher.parser

import org.butcher.parser.Butcher.nameSpecParser
import org.scalatest.{FunSuite, Matchers}

import Butcher.eval
import cats.implicits._

case class Person(firstName: String, driversLicence: String) extends ColumnReadable[String] {
  override def get(column: String): Either[Throwable, String] = {
    column match {
      case "firstName" => Right(firstName)
      case "driversLicence" => Right(driversLicence)
      case _ => Left(new Throwable("Unknown Column"))
    }
  }

  override def get(index: Int): Either[Throwable, String] = Left(new Throwable("Not implemented"))
}
class ButcherEvalTest extends FunSuite with Matchers {
  test("eval") {
    val ml =
      s"""
         |column names in [driversLicence] then encrypt using kms key foo
         |column names in [firstName] then mask
         |""".stripMargin

    val expressions = fastparse.parse(ml.trim, nameSpecParser(_))
    val p = Person("Satan", "666")
    expressions.fold(
      onFailure = {(_, _, extra) => println(extra.trace().longMsg);false should be(true)},
      onSuccess = {
        case (es, _) =>
          eval(es, p).sequence.fold({t => println(t); false should be(true)}, {
            r =>
              println(r);false should be(true)
          })
      }
    )
  }
}
