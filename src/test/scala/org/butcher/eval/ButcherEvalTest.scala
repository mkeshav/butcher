package org.butcher.eval

import cats.effect.IO
import cats.implicits._
import com.amazonaws.util.Base64
import org.butcher.kms.CryptoDsl.TaglessCrypto
import org.butcher.kms.{CryptoDsl, DataKey}
import org.scalatest.{FunSuite, Matchers}


class ButcherEvalTest extends FunSuite with Matchers {
  val b64EncodedPlainTextKey = "acZLXO+SWyeV95LYvUMExQtGeDHExNkAjvXbpbUEMK0="
  val b64EncodedCipherTextBlob = "AQIDAHhoNt+QMcK2fLVptebsdn939rqRYSkfDPtL70lK0fvadAGctDSWR9FFQo/sjJINvabqAAAAfjB8BgkqhkiG9w0BBwagbzBtAgEAMGgGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMRXCvv+D0JW3bZA6hAgEQgDvx1mHmiC1xdu4IDLY38QmgcVJf3vxxrM/v5I9OFL/kls9DkP1fhZI1GJtiJ3nQaEsYjO5oBSmsRdNEpA=="

  val dk = DataKey(Base64.decode(b64EncodedPlainTextKey), b64EncodedCipherTextBlob)

  val spec =
    s"""
       |column names in [driversLicence] then encrypt using kms key foo
       |column names in [firstName] then mask
       |""".stripMargin

  val data =
    """
      |firstName,driversLicence,donothing
      |satan,666,1
      |god,333,2
      |""".stripMargin


  lazy val crypto = new TaglessCrypto[IO](new CryptoDsl[IO] {
    override def generateKey(keyId: String): IO[Either[Throwable, DataKey]] = IO.pure(dk.asRight)

    override def encrypt(data: String, dk: DataKey): IO[Either[Throwable, String]] = IO.pure("foo".asRight)

    override def decrypt(value: String): IO[Either[Throwable, String]] = IO.pure("foo".asRight)
  })

  val evaluator = new ButcherEval(crypto)

  test("unknown expression") {
    val p = Map("firstName" -> "satan", "driversLicence" -> "666")
    evaluator.evalWithHeader("column is blah", data).isLeft should be(true)
  }

  test("csv") {
    evaluator.evalWithHeader(spec, data).fold({t => println(t); false should be(true)}, {
      r =>
        val expected =
          """
            |firstName|driversLicence|donothing
            |3815914799634fbdadf211431b8e372390fa35c0d54ed510993adb5b61525f48|foo|1
            |5723360ef11043a879520412e9ad897e0ebcb99cc820ec363bfecc9d751a1a99|foo|2
            |""".stripMargin
        r should be(expected.trim)
    })
  }
}
