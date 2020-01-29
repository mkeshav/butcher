package org.butcher.algebra

import cats.Monad
import javax.crypto.spec.SecretKeySpec
import org.butcher.OpResult

final case class DataKey(plainText: Array[Byte], cipher:String, algorithm: String)

trait CryptoDsl[F[_]] {
  def encrypt(data: String, dk: DataKey): F[OpResult[String]]
  def decrypt(key: SecretKeySpec, encryptedData: String): F[OpResult[String]]
  def decryptKey(cipher: String): F[OpResult[SecretKeySpec]]
}

object CryptoDsl {
  class TaglessCrypto[F[_]: Monad](dsl: CryptoDsl[F]) {
    def encrypt(data: String, dk: DataKey): F[OpResult[String]] = dsl.encrypt(data, dk)
    def decrypt(key: SecretKeySpec, encryptedData: String): F[OpResult[String]] = dsl.decrypt(key, encryptedData)
    def decryptKey(k: String): F[OpResult[SecretKeySpec]] = dsl.decryptKey(k)
  }
}