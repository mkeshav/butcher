package org.butcher.kms

import java.nio.ByteBuffer

import cats.data.Reader
import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.{DataKeySpec, DecryptRequest, GenerateDataKeyRequest}
import com.amazonaws.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import cats.syntax.either._
import org.butcher.OpResult

object KMSService {
  def encryptWith(data: String, dk: DataKey): Reader[AWSKMS, OpResult[String]] = Reader((kms: AWSKMS) => {
    try {
      encryptData(new SecretKeySpec(dk.plainText, "AES"), data) map {
        ed => s"key:${dk.cipher},data:$ed"
      }
    } catch {
      case e: Throwable => e.getMessage.asLeft
    }
  })

  def generateDataKey(keyId: String): Reader[AWSKMS, OpResult[DataKey]] = Reader((kms: AWSKMS) => {
    val gd = new GenerateDataKeyRequest().withKeyId(keyId).withKeySpec(DataKeySpec.AES_256)
    try {
      val gdkr = kms.generateDataKey(gd)
      val ptk = gdkr.getPlaintext.array()
      val cipherBlob = new String(Base64.encode(gdkr.getCiphertextBlob.array()))
      DataKey(ptk, cipherBlob).asRight
    } catch {
      case e: Throwable => e.getMessage.asLeft
    }
  })

  def parseAndDecrypt(value: String): Reader[AWSKMS, OpResult[String]] = Reader((kms: AWSKMS) => {
    try {
      val parts = value.split(",")
      val key = parts(0).split(":")(1)
      val data = parts(1).split(":")(1)

      val dkr = decryptKey(key).run(kms)
      dkr.map(ptk => decryptData(ptk, data))
    } catch {
      case e: Throwable => e.getMessage.asLeft
    }
  })

  private def decryptKey(base64EncodedCipher: String): Reader[AWSKMS, OpResult[SecretKeySpec]] = Reader((kms: AWSKMS) => {
    try {
      val dkr = new DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(Base64.decode(base64EncodedCipher)))
      val ptk = kms.decrypt(dkr).getPlaintext
      new SecretKeySpec(ptk.array(), "AES").asRight
    } catch {
      case e: Throwable => e.getMessage.asLeft
    }
  })

  private def decryptData(key: SecretKeySpec, encryptedData: String) = {
    val decodeBase64src = Base64.decode(encryptedData.getBytes)
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.DECRYPT_MODE, key)
    new String(cipher.doFinal(decodeBase64src))
  }

  private def encryptData(key: SecretKeySpec, data: String): OpResult[String] = {
    try {
      val cipher = Cipher.getInstance("AES")
      cipher.init(Cipher.ENCRYPT_MODE, key)
      val enc = cipher.doFinal(data.getBytes)
      new String(Base64.encode(enc)).asRight
    } catch {
      case e: Throwable => e.getMessage.asLeft
    }
  }

}
