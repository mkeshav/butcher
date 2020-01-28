package org.butcher.internals

import cats.data.Reader
import cats.effect.{ContextShift, IO}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.gu.scanamo.syntax.set
import com.gu.scanamo.{ScanamoAsync, Table}
import org.butcher.OpResult
import org.butcher.algebra.{CipherRow, EncryptionResult}
import org.butcher.time.utcNowEpochMillis

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.implicits._
import com.gu.scanamo.syntax._

private[internals] object DynamoService {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  def storeCipher(tableName: String, er: EncryptionResult):
    Reader[AmazonDynamoDBAsync, Future[OpResult[CipherRow]]] = Reader((dynamo: AmazonDynamoDBAsync) =>  {
    val cipherRow = CipherRow(er.rowId, er.encryptedData, er.dataKey, utcNowEpochMillis)
    val t = Table[CipherRow](tableName)
    val ops = for {
      _ <- t.put(cipherRow)
    } yield cipherRow

    ScanamoAsync.exec(dynamo)(ops).map(Right(_))
  })

  def getCipherRow(tableName: String, rowId: String):
    Reader[AmazonDynamoDBAsync, Future[OpResult[CipherRow]]] = Reader((dynamo: AmazonDynamoDBAsync) =>  {
    val t = Table[CipherRow](tableName)
    val ops = for {
      r <- t.get('rowId -> rowId)
    } yield r

    ScanamoAsync.exec(dynamo)(ops).map {
      case None => s"Row $rowId not found".asLeft
      case Some(v) =>
        v.leftMap(e => e.toString)
    }
  })
  
  def deleteCipherRow(tableName: String, rowId: String):
  Reader[AmazonDynamoDBAsync, Future[OpResult[Int]]] = Reader((dynamo: AmazonDynamoDBAsync) =>  {
    val t = Table[CipherRow](tableName)
    val ops = for {
      r <- t.delete('rowId -> rowId)
    } yield r

    ScanamoAsync.exec(dynamo)(ops).map(_.hashCode().asRight).recover {case e => e.getMessage.asLeft}
  })
}
