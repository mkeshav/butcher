package org.butcher.algebra

import cats.Monad
import org.butcher.OpResult

final case class DataKey(plainText: Array[Byte], cipher:String)

trait CryptoDsl[F[_]] {
  def generateKey(keyId: String): F[OpResult[DataKey]]
  def encrypt(data: String, dk: DataKey): F[OpResult[String]]
  def decrypt(value: String): F[OpResult[String]]
}

object CryptoDsl {
  class TaglessCrypto[F[_]: Monad](dsl: CryptoDsl[F]) {
    def encrypt(data: String, dk: DataKey): F[OpResult[String]] = dsl.encrypt(data, dk)
    def generateKey(keyId: String): F[OpResult[DataKey]] = dsl.generateKey(keyId)
    def decrypt(value: String): F[OpResult[String]] = dsl.decrypt(value)
  }
}