package org.butcher.interpreters

import cats.effect.IO
import com.amazonaws.services.kms.AWSKMS
import javax.crypto.spec.SecretKeySpec
import org.butcher.OpResult
import org.butcher.algebra.{CryptoDsl, DataKey}
import org.butcher.internals.kms.KMSService
import org.butcher.internals.kms.KMSService.{encryptWith, generateDataKey, decryptData}

private[butcher] class KMSCryptoIOInterpreter(kms: AWSKMS) extends CryptoDsl[IO] {
  override def generateKey(keyId: String): IO[OpResult[DataKey]] = IO(generateDataKey(keyId).run(kms))

  override def encrypt(data: String, dk: DataKey): IO[OpResult[String]] = IO(encryptWith(data, dk).run(kms))

  override def decryptKey(cipher: String): IO[OpResult[SecretKeySpec]] = IO(KMSService.decryptKey(cipher).run(kms))

  override def decrypt(key: SecretKeySpec, encryptedData: String): IO[OpResult[String]] = IO(decryptData(key, encryptedData))
}
