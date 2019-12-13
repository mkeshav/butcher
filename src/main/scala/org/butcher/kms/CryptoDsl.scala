package org.butcher.kms

import cats.Monad
import cats.effect.IO
import com.amazonaws.services.kms.AWSKMS
import KMSService._

final case class DataKey(plainText: Array[Byte], cipher:String)

trait CryptoDsl[F[_]] {
  def generateKey(keyId: String): F[Either[Throwable, DataKey]]
  def encrypt(data: String, dk: DataKey): F[Either[Throwable, String]]
}

object CryptoDsl {
  class KMSCryptoIOInterpreter(kms: AWSKMS) extends CryptoDsl[IO] {
    override def generateKey(keyId: String): IO[Either[Throwable, DataKey]] = IO(generateDataKey(keyId).run(kms))

    override def encrypt(data: String, dk: DataKey): IO[Either[Throwable, String]] = IO(encryptWith(data, dk).run(kms))
  }

  class TaglessCrypto[F[_]: Monad](dsl: CryptoDsl[F]) {
    def encrypt(data: String, dk: DataKey): F[Either[Throwable, String]] = dsl.encrypt(data, dk)
    def generateKey(keyId: String): F[Either[Throwable, DataKey]] = dsl.generateKey(keyId)
  }
}