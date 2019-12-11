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

  property("parse - success") {
    val rand = Random.alphanumeric.take(16).mkString
    val tokens =
      Table(
        ("input", "result"),
        ("column_name in [first_name] then hash", ActionExpr(Seq("first_name"), Hash)),
      )

    forAll(tokens) { (i: String, expected: ActionExpr) =>
      fastparse.parse(i, inParser(_)) should be(Parsed.Success(expected, 37))
    }
  }
}
