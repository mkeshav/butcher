package org.butcher.parser

import org.scalatest.{Matchers, PropSpec}
import Butcher._
import fastparse.Parsed
import org.scalatest.prop.PropertyChecks

import scala.util.Random

class ButcherTest extends PropSpec with PropertyChecks with Matchers {
  property("token parsing - success") {
    val rand = Random.alphanumeric.take(16).mkString
    val tokens =
      Table(
        ("input", "result"),
        (rand, Parsed.Success(rand, 16)),
      )

    forAll(tokens) { (i: String, expected: Parsed.Success[String]) =>
      fastparse.parse(i, tokenParser(_)) should be(expected)
    }
  }

  property("names parse - success") {
    val tokens =
      Table(
        ("input", "result", "length"),
        ("column names in [first_name] then hash", ColumnNamesActionExpr(Seq("first_name"), Hash), 38),
      )

    forAll(tokens) { (i: String, expected: Expr, length: Int) =>
      fastparse.parse(i, namesParser(_)) should be(Parsed.Success(expected, length))
    }
  }

  property("indices parse - success") {
    val tokens =
      Table(
        ("input", "result", "length"),
        ("column indices in [1] then hash", ColumnIndicesActionExpr(Seq(1), Hash), 31),
      )

    forAll(tokens) { (i: String, expected: Expr, length: Int) =>
      fastparse.parse(i, indicesParser(_)) should be(Parsed.Success(expected, length))
    }
  }
}
