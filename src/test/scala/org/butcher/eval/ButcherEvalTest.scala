package org.butcher.eval

import cats.effect.IO
import cats.implicits._
import com.amazonaws.util.Base64
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.{CsvMapper, CsvSchema}
import org.butcher.Implicits._
import org.butcher.kms.CryptoDsl.TaglessCrypto
import org.butcher.kms.{CryptoDsl, DataKey}
import org.butcher.parser.ButcherParser.nameSpecParser
import org.butcher.parser.UnknownExpr
import org.scalatest.{FunSuite, Matchers}

import scala.collection.JavaConverters._

class ButcherEvalTest extends FunSuite with Matchers {
  val b64EncodedPlainTextKey = "acZLXO+SWyeV95LYvUMExQtGeDHExNkAjvXbpbUEMK0="
  val b64EncodedCipherTextBlob = "AQIDAHhoNt+QMcK2fLVptebsdn939rqRYSkfDPtL70lK0fvadAGctDSWR9FFQo/sjJINvabqAAAAfjB8BgkqhkiG9w0BBwagbzBtAgEAMGgGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMRXCvv+D0JW3bZA6hAgEQgDvx1mHmiC1xdu4IDLY38QmgcVJf3vxxrM/v5I9OFL/kls9DkP1fhZI1GJtiJ3nQaEsYjO5oBSmsRdNEpA=="

  val dk = DataKey(Base64.decode(b64EncodedPlainTextKey), b64EncodedCipherTextBlob)

  lazy val crypto = new TaglessCrypto[IO](new CryptoDsl[IO] {
    override def generateKey(keyId: String): IO[Either[Throwable, DataKey]] = IO.pure(dk.asRight)

    override def encrypt(data: String, dk: DataKey): IO[Either[Throwable, String]] = IO.pure("foo".asRight)
  })

  val evaluator = new ButcherEval(crypto)

  test("eval") {
    val ml =
      s"""
         |column names in [driversLicence] then encrypt using kms key foo
         |column names in [firstName] then mask
         |""".stripMargin

    val expressions = fastparse.parse(ml.trim, nameSpecParser(_))
    val p = Map("firstName" -> "Satan", "driversLicence" -> "666")
    expressions.fold(
      onFailure = {(_, _, extra) => println(extra.trace().longMsg);false should be(true)},
      onSuccess = {
        case (es, _) =>
          evaluator.eval(es, p).sequence.fold({t => println(t); false should be(true)}, {
            r =>
              r should be(
                List(Butchered("driversLicence","foo"),
                  Butchered("firstName","c50d310cc268c44b1f99bf0dd61ad8d575f225e52f27847b08b2433bd9b97ee8")))
          })
      }
    )
  }

  test("unknown expression") {
    val es = Seq(UnknownExpr())
    val p = Map("firstName" -> "satan", "driversLicence" -> "666")
    evaluator.eval(es, p).sequence.isLeft should be(true)
  }

  test("csv") {
    val spec =
      s"""
         |column names in [driversLicence] then encrypt using kms key foo
         |column names in [firstName] then mask
         |""".stripMargin

    val expressions = fastparse.parse(spec.trim, nameSpecParser(_))

    val data =
      """
        |firstName,driversLicence
        |satan,666
        |god,333
        |""".stripMargin

    expressions.fold(
      onFailure = {(_, _, extra) => println(extra.trace().longMsg);false should be(true)},
      onSuccess = {
        case (es, _) =>
          val bootstrapSchema = CsvSchema.emptySchema().withHeader();
          val mapper = new CsvMapper()
          val mi: MappingIterator[java.util.Map[String, String]] = mapper.readerFor(classOf[java.util.Map[String, String]]).`with`(bootstrapSchema).readValues(data.trim)
          //toMap is to make it a immutable map
          val res = mi.readAll().asScala.map(_.asScala.toMap).map {
            row =>
              evaluator.eval(es, row)
          }
          res.flatten.toList.sequence.fold({t => println(t); false should be(true)}, {
            r =>
              r should be(
                List(Butchered("driversLicence","foo"),
                  Butchered("firstName","3815914799634fbdadf211431b8e372390fa35c0d54ed510993adb5b61525f48"),
                  Butchered("driversLicence","foo"),
                  Butchered("firstName","5723360ef11043a879520412e9ad897e0ebcb99cc820ec363bfecc9d751a1a99"),
                ))
          })

      }
    )
  }
}
