package org.butcher.eval

import cats.effect.{ContextShift, IO}
import org.scalatest.{FunSuite, Matchers}
import io.circe.parser._
import cats.syntax.either._

import scala.concurrent.ExecutionContext.Implicits.global
class JsonBYOCryptoEvaluatorTest extends FunSuite with Matchers {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  val c = KeyGen.crypto
  val evaluator = new JsonBYOCryptoEvaluator(c)

  val spec =
    s"""
       |column names in [driversLicence] then encrypt using kms key foo
       |column names in [firstName] then mask
       |""".stripMargin

  test("wrong method") {
    evaluator.evalDelimited(spec, "").isLeft should be(true)
  }

  test("json") {
    val data =
      """
        |{
        |   "firstName": "blah",
        |   "driversLicence": "123"
        |}
        |""".stripMargin
    val res = evaluator.evalJson(spec, data.trim)
    res.fold(
      {t => println(t); false should be(true)},
      {
        v =>
          parse(v) match {
            case Left(e) => println(e); false should be(true)
            case Right(parsed) =>
              parsed.hcursor.downField("firstName").as[String] should be("8b7df143d91c716ecfa5fc1730022f6b421b05cedee8fd52b1fc65a96030ad52".asRight)
              val dl = parsed.hcursor.downField("driversLicence").as[String]
              dl.leftMap(_ => false should be(true)).foreach(enc => enc.startsWith("key") should be(true))
          }
      }
    )
  }
}
