package org.butcher.internals.interpreters

import java.util.UUID

import cats.effect.IO
import org.butcher.algebra.EncryptionResult
import org.butcher.algebra.StorageDsl.TaglessStorage
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.test.dynamo.{createClient, createTable}

class DynamoStorageIOInterpreterTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {
  val endpoint = sys.env.getOrElse("DYNAMO_ENDPOINT", "http://localhost:8000")
  val db = createClient(endpoint)

  val interpreter = new DynamoStorageIOInterpreter("test",db)
  val tl = new TaglessStorage[IO](interpreter)

  test("all") {
    val rowId = UUID.randomUUID().toString
    val f = for {
      _ <- tl.put(EncryptionResult("foo", rowId, "bar"))
      g <- tl.get(rowId)
      _ <- tl.remove(rowId)
    } yield g

    f.unsafeRunSync.fold({_ => false should be(true)},
      {
        r =>
          r.rowId should be(rowId)
      })

    tl.get(rowId).unsafeRunSync.isLeft should be(true)
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
