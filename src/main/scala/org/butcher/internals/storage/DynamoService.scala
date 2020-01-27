package org.butcher.internals.storage

import cats.data.Reader
import cats.syntax.either._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.gu.scanamo.{ScanamoAsync, Table}
import org.butcher.algebra.{CipherRow, EncryptionResult}

import scala.concurrent.Future
import org.butcher.time._
import com.gu.scanamo.syntax._
import org.butcher.OpResult

import scala.concurrent.ExecutionContext.Implicits.global

private[butcher] object DynamoService {
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

  def updateCipherRow(tableName: String, rowId: String, data: String):
    Reader[AmazonDynamoDBAsync, Future[OpResult[CipherRow]]] = Reader((dynamo: AmazonDynamoDBAsync) =>  {
    val ts = utcNowEpochMillis
    val t = Table[CipherRow](tableName)
    val ops = for {
      cr <- t.update('rowId -> rowId, set('data -> data) and set('ts -> ts))
    } yield cr

    ScanamoAsync.exec(dynamo)(ops).map(_.leftMap(de => de.toString))
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
