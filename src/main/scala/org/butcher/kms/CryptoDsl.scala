package org.butcher.kms

import cats.Monad
import cats.effect.IO
import com.amazonaws.services.kms.AWSKMS

trait CryptoDsl[F[_]] {
  def encrypt(data: String, kmsKeyId: String): F[Either[Throwable, String]]
}

object CryptoDsl {
  class CryptoIOInterpreter(kms: AWSKMS) extends CryptoDsl[IO] {
    override def encrypt(data: String, kmsKeyId: String): IO[Either[Throwable, String]] = {
      IO(KMSService.encrypt(data, kmsKeyId).run(kms))
    }
  }

  class TaglessCrypto[F[_]: Monad](dsl: CryptoDsl[F]) {
    def encrypt(data: String, kmsKeyId: String): F[Either[Throwable, String]] = dsl.encrypt(data, kmsKeyId)
  }
}