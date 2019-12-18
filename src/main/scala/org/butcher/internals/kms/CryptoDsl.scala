package org.butcher.internals.kms

import cats.Monad
import cats.effect.IO
import com.amazonaws.services.kms.AWSKMS
import KMSService._
import org.butcher.OpResult

final case class DataKey(plainText: Array[Byte], cipher:String)

private[butcher] trait CryptoDsl[F[_]] {
  def generateKey(keyId: String): F[OpResult[DataKey]]
  def encrypt(data: String, dk: DataKey): F[OpResult[String]]
  def decrypt(value: String): F[OpResult[String]]
}

private[butcher] object CryptoDsl {
  class KMSCryptoIOInterpreter(kms: AWSKMS) extends CryptoDsl[IO] {
    override def generateKey(keyId: String): IO[OpResult[DataKey]] = IO(generateDataKey(keyId).run(kms))

    override def encrypt(data: String, dk: DataKey): IO[OpResult[String]] = IO(encryptWith(data, dk).run(kms))

    override def decrypt(value: String): IO[OpResult[String]] = IO(parseAndDecrypt(value).run(kms))
  }

  class TaglessCrypto[F[_]: Monad](dsl: CryptoDsl[F]) {
    def encrypt(data: String, dk: DataKey): F[OpResult[String]] = dsl.encrypt(data, dk)
    def generateKey(keyId: String): F[OpResult[DataKey]] = dsl.generateKey(keyId)
    def decrypt(value: String): F[OpResult[String]] = dsl.decrypt(value)
  }
}