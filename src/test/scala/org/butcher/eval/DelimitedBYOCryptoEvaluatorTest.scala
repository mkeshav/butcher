package org.butcher.eval

import cats.effect.{ContextShift, IO}
import cats.implicits._
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.{CsvMapper, CsvSchema}
import org.butcher.OpResult
import org.butcher.algebra.StorageDsl.TaglessStorage
import org.butcher.algebra.{CipherRow, EncryptionResult, StorageDsl}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

class DelimitedBYOCryptoEvaluatorTest extends FunSuite with Matchers {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  lazy val storage = new TaglessStorage[IO](new StorageDsl[IO] {
    override def put(er: EncryptionResult): IO[OpResult[CipherRow]] = IO {
      CipherRow(er.rowId, er.encryptedData, er.dataKey, System.currentTimeMillis).asRight
    }

    override def get(rowId: String): IO[OpResult[CipherRow]] = IO("Not Implemented".asLeft)

    override def remove(rowId: String): IO[OpResult[Int]] = IO("Not Implemented".asLeft)

    override def update(rowId: String, encryptedData: String): IO[OpResult[CipherRow]] = IO("Not Implemented".asLeft)
  })

  val c = KeyGen.crypto
  val evaluator = new DelimitedBYOCryptoEvaluator(c, storage)
  val spec =
    s"""
       |encrypt columns [driversLicence] using key foo
       |with primary key columns [firstName]
       |""".stripMargin

  val data =
    """
      |firstName,driversLicence,donothing
      |satan,666,1
      |god,333,2
      |""".stripMargin

  test("awesomeness") {
    val expected =
      """
        |firstName|driversLicence|donothing
        |satan|c7e616822f366fb1b5e0756af498cc11d2c0862edcb32ca65882f622ff39de1b|1|3815914799634fbdadf211431b8e372390fa35c0d54ed510993adb5b61525f48
        |god|556d7dc3a115356350f1f9910b1af1ab0e312d4b3e4fc788d2da63668f36d017|2|5723360ef11043a879520412e9ad897e0ebcb99cc820ec363bfecc9d751a1a99
        |""".stripMargin
    val result = evaluator.evalDelimited(spec, data)
    result.isRight should be(true)
    result should be(Right(expected.trim))
  }

  test("unknown expression") {
    evaluator.evalDelimited("column is blah", data).isLeft should be(true)
  }

  test("missing columns") {
    val d =
      """
        |firstName,donothing
        |satan,1
        |god,2
        |""".stripMargin

    evaluator.evalDelimited(spec, d).fold(
      {t => t should be("0:Column driversLicence not found")},
      {_ => false should be(true)
    })
  }

  test("wrong method") {
    evaluator.evalJson(spec, data).isLeft should be(true)
  }

}
