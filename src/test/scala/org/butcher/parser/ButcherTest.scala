package org.butcher.parser

import org.scalatest.{Matchers, PropSpec}
import Butcher._
import org.scalatest.prop.PropertyChecks

import scala.util.Random

class ButcherTest extends PropSpec with PropertyChecks with Matchers {
  property("indices parse - success") {
    val l = (1 to Random.nextInt(10)).map(i => Random.nextInt(i*1000))
    val data =
      Table(
        ("input", "result"),
        (s"column indices in [${l.mkString(",")}] then mask", ColumnIndicesMaskExpr(l)),
      )

    forAll(data) { (i: String, expected: Expr) =>
      val r = fastparse.parse(i, columnIndicesLineMaskParser(_))
      r.fold(
        onFailure = {(_, _, _) => false should be(true)},
        onSuccess = {case (expr, _) => expr should be(expected)}
      )
    }
  }

  property("names parse - success") {
    val l = (1 to Random.nextInt(10)).map(i => Random.alphanumeric.take(16).mkString)
    val data =
      Table(
        ("input", "result"),
        (s"column names in [${l.mkString(",")}] then mask", ColumnNamesMaskExpr(l)),
      )

    forAll(data) { (i: String, expected: Expr) =>
      val r = fastparse.parse(i, columnNamesLineMaskParser(_))
      r.fold(
        onFailure = {(_, _, _) => false should be(true)},
        onSuccess = {case (expr, _) => expr should be(expected)}
      )
    }
  }

  property("multiline") {
    val lc1 = (1 to Random.nextInt(10)).map(i => Random.alphanumeric.take(16).mkString)
    val lc2 = (1 to Random.nextInt(10)).map(i => Random.alphanumeric.take(16).mkString)

    val ml =
      s"""
         |column names in [${lc2.mkString(",")}] then encrypt using kms key foo
         |column names in [${lc1.mkString(",")}] then mask
         |""".stripMargin
    val data =
      Table(
        ("input", "result"),
        (ml, Seq(ColumnNamesEncryptExpr(lc2, "foo"), ColumnNamesMaskExpr(lc1))),
      )

    forAll(data) { (i: String, expected: Seq[Expr]) =>
      val r = fastparse.parse(i.trim, nameSpecParser(_))
      r.fold(
        onFailure = {(_, _, extra) => println(extra.trace().longMsg);false should be(true)},
        onSuccess = {case (es, _) => es should be(expected)}
      )
    }
  }
}
