package org.butcher.kms

import cats.data.Reader
import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.{DataKeySpec, GenerateDataKeyRequest}
import com.amazonaws.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import cats.syntax.either._

object KMSService {
  def encrypt(data: String, kmsKey: String): Reader[AWSKMS, Either[Throwable, String]] = Reader((kms: AWSKMS) => {
    val gd = new GenerateDataKeyRequest().withKeyId(kmsKey).withKeySpec(DataKeySpec.AES_256)
    try {
      val gdkr = kms.generateDataKey(gd)
      encryptData(new SecretKeySpec(gdkr.getPlaintext.array(), "AES"), data) map {
        ed =>
          val dataKey = new String(gdkr.getCiphertextBlob.array())
          val result = s"key:$dataKey,data:$ed"
          new String(Base64.encode(result.getBytes))
      }
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
