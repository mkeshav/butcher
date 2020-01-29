package org.butcher.internals

import java.nio.ByteBuffer

import cats.data.Reader
import cats.implicits._
import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.{DataKeySpec, DecryptRequest, GenerateDataKeyRequest}
import com.amazonaws.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.butcher.OpResult
import org.butcher.algebra.DataKey

private[internals] object KMSService {
  def encryptWith(data: String, dk: DataKey): OpResult[String] = {
    try {
      encryptData(new SecretKeySpec(dk.plainText, dk.algorithm), data)
    } catch {
      case e: Throwable => e.getMessage.asLeft
    }
  }

  def decryptKey(base64EncodedCipher: String, algorithm: String = "AES"): Reader[AWSKMS, OpResult[SecretKeySpec]] = Reader((kms: AWSKMS) => {
    try {
      val dkr = new DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(Base64.decode(base64EncodedCipher)))
      val ptk = kms.decrypt(dkr).getPlaintext
      new SecretKeySpec(ptk.array(), algorithm).asRight
    } catch {
      case e: Throwable => e.getMessage.asLeft
    }
  })

  def decryptData(key: SecretKeySpec, encryptedData: String, algorithm: String = "AES"): OpResult[String] = {
    try {
      val decodeBase64src = Base64.decode(encryptedData.getBytes)
      val cipher = Cipher.getInstance(algorithm)
      cipher.init(Cipher.DECRYPT_MODE, key)
      new String(cipher.doFinal(decodeBase64src)).asRight
    } catch {
      case e: Throwable => e.getMessage.asLeft
    }
  }

  private def encryptData(key: SecretKeySpec, data: String, algorithm: String = "AES"): OpResult[String] = {
    try {
      val cipher = Cipher.getInstance(algorithm)
      cipher.init(Cipher.ENCRYPT_MODE, key)
      val enc = cipher.doFinal(data.getBytes)
      new String(Base64.encode(enc)).asRight
    } catch {
      case e: Throwable => e.getMessage.asLeft
    }
  }

}
