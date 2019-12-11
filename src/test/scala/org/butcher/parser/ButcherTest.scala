package org.butcher.parser

import org.scalatest.{Matchers, PropSpec}
import Butcher._
import fastparse.Parsed
import org.scalatest.prop.PropertyChecks

class ButcherTest extends PropSpec with PropertyChecks with Matchers {
  property("token parsing - success") {
    val tokens =
      Table(
        ("input", "result"),
        ("first_name", Parsed.Success("first_name", 10)),
        ("last-name", Parsed.Success("last-name", 9)),
      )

    forAll(tokens) { (i: String, expected: Parsed.Success[String]) =>
      fastparse.parse(i, tokenParser(_)) should be(expected)
    }
  }
}
