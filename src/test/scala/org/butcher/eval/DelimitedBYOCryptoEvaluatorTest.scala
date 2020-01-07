package org.butcher.eval

import cats.effect.{ContextShift, IO}
import cats.implicits._
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.{CsvMapper, CsvSchema}
import org.scalatest.{FunSuite, Matchers}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

class DelimitedBYOCryptoEvaluatorTest extends FunSuite with Matchers {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  val c = KeyGen.crypto
  val evaluator = new DelimitedBYOCryptoEvaluator(c)
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

  test("unknown expression") {
    val p = Map("firstName" -> "satan", "driversLicence" -> "666")
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

  test("eval: encrypt and decrypt") {
    evaluator.evalDelimited(spec, data).fold({t => println(t); false should be(true)}, {
      r =>
        val bootstrapSchema = CsvSchema.emptySchema().withHeader().withColumnSeparator('|')
        val mapper = new CsvMapper()
        val mi: MappingIterator[java.util.Map[String, String]] =
          mapper.readerFor(classOf[java.util.Map[String, String]]).`with`(bootstrapSchema).readValues(r.trim)
        val rows = mi.readAll().asScala.map(_.asScala.toMap)
        val ios = rows.map {
          row =>
            c.decrypt(row("driversLicence"))
        }
        ios.toList.parSequence.unsafeRunSync() should contain allElementsOf(List("666".asRight, "333".asRight))

        rows.map(row => row("donothing")) should contain allElementsOf(List("1", "2"))
        rows.map(row => row("firstName")) should contain allElementsOf(
          List("3815914799634fbdadf211431b8e372390fa35c0d54ed510993adb5b61525f48",
            "5723360ef11043a879520412e9ad897e0ebcb99cc820ec363bfecc9d751a1a99"))
    })
  }

  test("wrong method") {
    evaluator.evalJson(spec, data).isLeft should be(true)
  }

}
