package org.butcher.algebra

import org.butcher.OpResult

final case class EncryptionResult(dataKey: String, rowId: String, data: String)
final case class CipherRow(rowId: String, data: String, cipher: String, ts: Long)

trait StorageDsl[F[_]] {
  def put(er: EncryptionResult): F[OpResult[CipherRow]]
  def get(rowId: String): F[OpResult[CipherRow]]
  def remove(rowId: String): F[OpResult[Int]]
  def update(rowId: String, encryptedData: String): F[OpResult[CipherRow]]
}
