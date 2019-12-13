package org.butcher.parser

import org.scalatest.{Matchers, PropSpec}
import ButcherParser._
import org.scalatest.prop.PropertyChecks

import scala.util.Random

class ButcherParserTest extends PropSpec with PropertyChecks with Matchers {
  property("indices parse - success") {
    val l = (1 to 10).map(i => Random.nextInt(i*1000))
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
    val l = (1 to 10).map(i => Random.alphanumeric.take(16).mkString)
    val data =
      Table(
        ("input", "result"),
        (s"column names in [ ${l.mkString(", ")} ] then mask", ColumnNamesMaskExpr(l)),
      )

    forAll(data) { (i: String, expected: Expr) =>
      val r = fastparse.parse(i, columnNamesLineMaskParser(_))
      r.fold(
        onFailure = {(_, _, extra) => println(extra.trace().longMsg);false should be(true)},
        onSuccess = {case (expr, _) => expr should be(expected)}
      )
    }
  }

  property("multiline - names") {
    val lc1 = (1 to 10).map(_ => Random.alphanumeric.take(16).mkString)
    val lc2 = (1 to 10).map(_ => Random.alphanumeric.take(16).mkString)

    val ml =
      s"""
         |column names in [ ${lc2.mkString(",")}] then encrypt using kms key foo
         |column names in [${lc1.mkString(",")} ] then mask
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
  property("multiline - indices") {
    val lc1 = (1 to 10).toList
    val lc2 = (1 to 10).toList

    val ml =
      s"""
         |column indices in [${lc2.mkString(",")}] then encrypt using kms key foo
         |column indices in [${lc1.mkString(",")} ] then mask
         |""".stripMargin
    val data =
      Table(
        ("input", "result"),
        (ml, Seq(ColumnIndicesEncryptExpr(lc2, "foo"), ColumnIndicesMaskExpr(lc1))),
      )

    forAll(data) { (i: String, expected: Seq[Expr]) =>
      val r = fastparse.parse(i.trim, indicesSpecParser(_))
      r.fold(
        onFailure = {(_, _, extra) => println(extra.trace().longMsg);false should be(true)},
        onSuccess = {case (es, _) => es should be(expected)}
      )
    }
  }
}
