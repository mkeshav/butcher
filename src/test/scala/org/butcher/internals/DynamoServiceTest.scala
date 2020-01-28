package org.butcher.internals

import java.util.UUID

import cats.effect.IO
import org.butcher.algebra.EncryptionResult
import org.butcher.internals.DynamoService._
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import org.test.dynamo.{createClient, createTable}

class DynamoServiceTest extends FunSuite with BeforeAndAfterAll with Matchers {
  val endpoint = sys.env.getOrElse("DYNAMO_ENDPOINT", "http://localhost:8000")
  val db = createClient(endpoint)

  test("all") {
    val rowId = UUID.randomUUID().toString
    val f = for {
      _ <- IO.fromFuture(IO(storeCipher("test", EncryptionResult("foo", rowId, "bar")).run(db)))
      g <- IO.fromFuture(IO(getCipherRow("test", rowId).run(db)))
      _ <- IO.fromFuture(IO(deleteCipherRow("test", rowId).run(db)))
    } yield g

    f.unsafeRunSync.fold({_ => false should be(true)},
    {
      r =>
        r.rowId should be(rowId)
    })

    val nf = IO.fromFuture(IO(getCipherRow("test", rowId).run(db))).unsafeRunSync
    nf.isLeft should be(true)

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
