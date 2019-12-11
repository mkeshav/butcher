package org.butcher.parser

import org.scalatest.{Matchers, PropSpec}
import Butcher._
import org.scalatest.prop.PropertyChecks

import scala.util.Random

class ButcherTest extends PropSpec with PropertyChecks with Matchers {
  property("indices parse - success") {
    val l = (1 to Random.nextInt(10)).map(i => Random.nextInt(i*1000))
    val tokens =
      Table(
        ("input", "result"),
        (s"column indices in [${l.mkString(",")}] then hash", ColumnIndicesActionExpr(l, Hash)),
      )

    forAll(tokens) { (i: String, expected: Expr) =>
      val r = fastparse.parse(i, columnIndicesLineParser(_))
      r.fold(
        onFailure = {(_, _, _) => false should be(true)},
        onSuccess = {case (expr, _) => expr should be(expected)}
      )
    }
  }

  property("names parse - success") {
    val l = (1 to Random.nextInt(10)).map(i => Random.alphanumeric.take(16).mkString)
    val tokens =
      Table(
        ("input", "result"),
        (s"column names in [${l.mkString(",")}] then hash", ColumnNamesActionExpr(l, Hash)),
      )

    forAll(tokens) { (i: String, expected: Expr) =>
      val r = fastparse.parse(i, columnNamesLineParser(_))
      r.fold(
        onFailure = {(_, _, _) => false should be(true)},
        onSuccess = {case (expr, _) => expr should be(expected)}
      )
    }
  }
}
