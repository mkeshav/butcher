package org.butcher.algebra

import cats.Monad
import org.butcher.OpResult

final case class EncryptionResult(dataKey: String, rowId: String, encryptedData: String)
final case class CipherRow(rowId: String, data: String, cipher: String, ts: Long)

trait StorageDsl[F[_]] {
  def put(er: EncryptionResult): F[OpResult[CipherRow]]
  def get(rowId: String): F[OpResult[CipherRow]]
  def remove(rowId: String): F[OpResult[Int]]
}

object StorageDsl {
  class TaglessStorage[F[_]: Monad](dsl: StorageDsl[F]) {
    def put(er: EncryptionResult): F[OpResult[CipherRow]] = dsl.put(er)
    def get(rowId: String): F[OpResult[CipherRow]] = dsl.get(rowId)
    def remove(rowId: String): F[OpResult[Int]] = dsl.remove(rowId)
  }
}