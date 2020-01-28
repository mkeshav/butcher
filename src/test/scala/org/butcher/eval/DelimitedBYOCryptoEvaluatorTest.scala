package org.butcher.eval

import cats.data.EitherT
import cats.effect.{ContextShift, IO}
import cats.implicits._
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.{CsvMapper, CsvSchema}
import org.butcher.OpResult
import org.butcher.algebra.StorageDsl.TaglessStorage
import org.butcher.algebra.{CipherRow, EncryptionResult, StorageDsl}
import org.butcher.implicits._
import org.butcher.internals.parser.ButcherParser.block
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
       |encrypt columns [firstName, driversLicence] using key foo
       |with primary key columns [firstName]
       |""".stripMargin

  test("awesomeness") {
    val data = Map[String, String](
      "firstName" -> "satan", "driversLicence" -> "666", "donothing" -> "1"
    )
    val expected = Right(EvalResult("3815914799634fbdadf211431b8e372390fa35c0d54ed510993adb5b61525f48|c7e616822f366fb1b5e0756af498cc11d2c0862edcb32ca65882f622ff39de1b|1",
      "3815914799634fbdadf211431b8e372390fa35c0d54ed510993adb5b61525f48"))
    val parsed = fastparse.parse(spec.trim, block(_))
    val f = for {
      dk <- EitherT(c.generateKey("foo"))
      r <- EitherT(evaluator.eval(parsed, dk, data))
    } yield r

    val result = f.value.unsafeRunSync
    result.isRight should be(true)
    result should be(expected)
  }

  test("delimited") {
    val data =
      """
        |firstName,driversLicence,donothing
        |satan,666,1
        |god,333,2
        |""".stripMargin

    val bootstrapSchema = CsvSchema.emptySchema().withHeader().withColumnSeparator(',')
    val mapper = new CsvMapper()
    try {
      val mi: MappingIterator[java.util.Map[String, String]] = mapper.readerFor(classOf[java.util.Map[String, String]]).`with`(bootstrapSchema).readValues(data.trim)
      val expression = fastparse.parse(spec.trim, block(_))
      val result = mi.readAll().asScala.map(_.asScala.toMap).map {
        m =>
          val f = for {
            dk <- EitherT(c.generateKey("foo"))
            r <- EitherT(evaluator.eval(expression, dk, m))
          } yield r

          f.value
      }
      val seqd = result.toList.sequence.map(_.sequence)
      println(seqd.unsafeRunSync)
    } catch {
      case _: Throwable => fail()
    }
  }
}
