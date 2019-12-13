package org.butcher.kms

import cats.data.Reader
import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.{DataKeySpec, GenerateDataKeyRequest}
import com.amazonaws.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import cats.syntax.either._

object KMSService {
  def encryptWith(data: String, dk: DataKey): Reader[AWSKMS, Either[Throwable, String]] = Reader((kms: AWSKMS) => {
    try {
      encryptData(new SecretKeySpec(dk.plainText, "AES"), data) map {
        ed => s"key:${dk.cipher},data:$ed"
      }
    } catch {
      case e: Throwable => e.asLeft
    }
  })

  def generateDataKey(keyId: String): Reader[AWSKMS, Either[Throwable, DataKey]] = Reader((kms: AWSKMS) => {
    val gd = new GenerateDataKeyRequest().withKeyId(keyId).withKeySpec(DataKeySpec.AES_256)
    try {
      val gdkr = kms.generateDataKey(gd)
      val ptk = gdkr.getPlaintext.array()
      val cipherBlob = new String(Base64.encode(gdkr.getCiphertextBlob.array()))
      DataKey(ptk, cipherBlob).asRight
    } catch {
      case e: Throwable => e.asLeft
    }
  })

  private def encryptData(key: SecretKeySpec, data: String): Either[Throwable, String] = {
    try {
      val cipher = Cipher.getInstance("AES")
      cipher.init(Cipher.ENCRYPT_MODE, key)
      val enc = cipher.doFinal(data.getBytes)
      new String(Base64.encode(enc)).asRight
    } catch {
      case e: Throwable => e.asLeft
    }
  }

}
