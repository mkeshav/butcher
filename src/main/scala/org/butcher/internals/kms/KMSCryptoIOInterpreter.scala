package org.butcher.internals.kms

import cats.effect.IO
import com.amazonaws.services.kms.AWSKMS
import org.butcher.OpResult
import org.butcher.algebra.{CryptoDsl, DataKey}
import org.butcher.internals.kms.KMSService.{encryptWith, generateDataKey, parseAndDecrypt}

class KMSCryptoIOInterpreter(kms: AWSKMS) extends CryptoDsl[IO] {
  override def generateKey(keyId: String): IO[OpResult[DataKey]] = IO(generateDataKey(keyId).run(kms))

  override def encrypt(data: String, dk: DataKey): IO[OpResult[String]] = IO(encryptWith(data, dk).run(kms))

  override def decrypt(value: String): IO[OpResult[String]] = IO(parseAndDecrypt(value).run(kms))
}
