package org.butcher.eval

import cats.data.EitherT
import cats.effect.{ContextShift, IO}
import cats.implicits._
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.{CsvMapper, CsvSchema}
import org.butcher.algebra.DataKey
import org.butcher.algebra.StorageDsl.TaglessStorage
import org.butcher.implicits._
import org.butcher.internals.interpreters.DynamoStorageIOInterpreter
import org.butcher.parser.ButcherParser.block
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import org.test.dynamo._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

class ColumnReadableEvaluatorTest extends FunSuite with Matchers with BeforeAndAfterAll {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  val endpoint = sys.env.getOrElse("DYNAMO_ENDPOINT", "http://localhost:8000")
  val db = createClient(endpoint)
  val di = new DynamoStorageIOInterpreter("test", db)
  lazy val storage = new TaglessStorage[IO](di)

  val c = KeyGen.crypto
  val evaluator = new ColumnReadableEvaluator(c, storage)
  val spec =
    s"""
       |encrypt columns [firstName, driversLicence] using key foo
       |with primary key columns [firstName]
       |""".stripMargin

  lazy val expression = fastparse.parse(spec.trim, block(_))

  test("missing columns") {
    val dk = DataKey("foo".getBytes, "bar", "AES")
    val data = Map[String, String](
      "firstName" -> "satan", "donothing" -> "1"
    )

    evaluator.eval(expression, dk, data).unsafeRunSync.fold(
      {t => t should be("Column driversLicence not found")},
      {_ => false should be(true)})
  }

  test("awesomeness") {
    val data = Map[String, String](
      "firstName" -> "satan", "driversLicence" -> "666", "donothing" -> "1"
    )
    val expected = Right(EvalResult(
      "3815914799634fbdadf211431b8e372390fa35c0d54ed510993adb5b61525f48|c7e616822f366fb1b5e0756af498cc11d2c0862edcb32ca65882f622ff39de1b|1",
      "3815914799634fbdadf211431b8e372390fa35c0d54ed510993adb5b61525f48"))
    val f = for {
      dk <- EitherT(c.generateKey("foo"))
      r <- EitherT(evaluator.eval(expression, dk, data))
    } yield r

    val result = f.value.unsafeRunSync
    result.isRight should be(true)
    result should be(expected)

    storage.get("3815914799634fbdadf211431b8e372390fa35c0d54ed510993adb5b61525f48")
      .unsafeRunSync.isRight should be(true)

  }

  test("delimited") {
    val data =
      """
        |firstName,driversLicence,donothing
        |satan,666,1
        |god,333,2
        |""".stripMargin

    val expected = List(
      EvalResult("3815914799634fbdadf211431b8e372390fa35c0d54ed510993adb5b61525f48|c7e616822f366fb1b5e0756af498cc11d2c0862edcb32ca65882f622ff39de1b|1","3815914799634fbdadf211431b8e372390fa35c0d54ed510993adb5b61525f48"),
      EvalResult("5723360ef11043a879520412e9ad897e0ebcb99cc820ec363bfecc9d751a1a99|556d7dc3a115356350f1f9910b1af1ab0e312d4b3e4fc788d2da63668f36d017|2","5723360ef11043a879520412e9ad897e0ebcb99cc820ec363bfecc9d751a1a99")
    )
    val bootstrapSchema = CsvSchema.emptySchema().withHeader().withColumnSeparator(',')
    val mapper = new CsvMapper()
    try {
      val mi: MappingIterator[java.util.Map[String, String]] = mapper.readerFor(classOf[java.util.Map[String, String]]).`with`(bootstrapSchema).readValues(data.trim)
      val result = mi.readAll().asScala.map(_.asScala.toMap).map {
        m =>
          val f = for {
            dk <- EitherT(c.generateKey("foo"))
            r <- EitherT(evaluator.eval(expression, dk, m))
          } yield r

          f.value
      }
      val seqd = result.toList.sequence.map(_.sequence)
      seqd.unsafeRunSync should be(Right(expected))
    } catch {
      case _: Throwable => fail()
    }
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    try {
      createTable("test", db)
    } catch {
      case e: Throwable => e.printStackTrace(System.err)
    }
  }

  override protected def afterAll(): Unit = {
    db.shutdown()
    super.afterAll()
  }
}
