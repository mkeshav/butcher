package org.butcher.parser

import org.butcher.parser.ButcherParser._
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpec

import scala.util.Random
import org.scalatest.prop.TableDrivenPropertyChecks._

class ButcherParserTest extends AnyPropSpec with Matchers {
  property("multiline") {
    val lc1 = (1 to 10).map(_ => Random.alphanumeric.take(16).mkString)
    val lc2 = (1 to 10).map(_ => Random.alphanumeric.take(16).mkString)

    val ml =
      s"""
         |encrypt columns [ ${lc2.mkString(", ")}]
         |with primary key columns [${lc1.mkString(",")} ]
         |""".stripMargin
    val data =
      Table(
        ("input", "result"),
        (ml, EncryptColumnsWithPKExpression(lc2, lc1)),
      )

    forAll(data) { (i: String, expected: Expr) =>
      val r = fastparse.parse(i.trim, block(_))
      r.fold(
        onFailure = {(_, _, extra) => println(extra.trace().longMsg);false should be(true)},
        onSuccess = {case (es, _) => es should be(expected)}
      )
    }
  }


  property("file") {
    val lc1 = (1 to 10).map(_ => Random.alphanumeric.take(16).mkString)
    val lc2 = (1 to 10).map(_ => Random.alphanumeric.take(16).mkString)

    val expected = Prg("s3://mybucket/mykey/my_awesome_data",
      List(EncryptColumnsWithPKExpression(lc2, lc1))
    )
    val ml =
      s"""
         |file s3://mybucket/mykey/my_awesome_data {
         |encrypt columns [ ${lc2.mkString(", ")}]
         |with primary key columns [${lc1.mkString(",")} ]
         |}
         |""".stripMargin
    val data =
      Table(
        ("input", "result"),
        (ml, expected),
      )

    forAll(data) { (i: String, expected: Prg) =>
      val r = fastparse.parse(i.trim, file(_))
      r.fold(
        onFailure = {(_, _, extra) => println(extra.trace().longMsg);false should be(true)},
        onSuccess = {case (es, _) => es should be(expected)}
      )
    }
  }
}
