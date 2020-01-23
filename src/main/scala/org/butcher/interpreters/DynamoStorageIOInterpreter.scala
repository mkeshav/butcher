package org.butcher.interpreters

import cats.effect.{ContextShift, IO}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import org.butcher.OpResult
import org.butcher.algebra.{CipherRow, EncryptionResult, StorageDsl}
import org.butcher.internals.storage.DynamoService

import scala.concurrent.ExecutionContext.Implicits.global

class DynamoStorageIOInterpreter(tableName: String, db: AmazonDynamoDBAsync) extends StorageDsl[IO] {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  override def get(rowId: String): IO[OpResult[CipherRow]] = {
    IO.fromFuture(IO(DynamoService.getCipherRow(tableName, rowId).run(db)))
  }

  override def put(er: EncryptionResult): IO[OpResult[CipherRow]] = {
    IO.fromFuture(IO(DynamoService.storeCipher(tableName, er).run(db)))
  }

  override def remove(rowId: String): IO[OpResult[Int]] = {
    IO.fromFuture(IO(DynamoService.deleteCipherRow(tableName, rowId).run(db)))
  }

  override def update(rowId: String, encryptedData: String): IO[OpResult[CipherRow]] = {
    IO.fromFuture(IO(DynamoService.updateCipherRow(tableName, rowId, encryptedData).run(db)))
  }
}
