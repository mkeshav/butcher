package org.butcher.parser

import org.butcher.parser.ButcherParser._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, PropSpec}

import scala.util.Random

class ButcherParserTest extends PropSpec with PropertyChecks with Matchers {
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

    val ml =
      s"""
         |file s3://mybucket/my_awesome_data.csv {
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
}
