package org.butcher.eval

import cats.effect.IO
import javax.crypto.{Cipher, KeyGenerator}
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64
import org.butcher.OpResult
import org.butcher.algebra.CryptoDsl.TaglessCrypto
import cats.syntax.either._
import com.sun.org.apache.xml.internal.security.algorithms.JCEMapper.Algorithm
import org.butcher.algebra.StorageDsl.TaglessStorage
import org.butcher.algebra.{CipherRow, CryptoDsl, DataKey, EncryptionResult, StorageDsl}

object KeyGen {
  private def genKey(algorithm: String, size: Int): Array[Byte] = {
    val generator = KeyGenerator.getInstance(algorithm)
    generator.init(size)
    generator.generateKey().getEncoded
  }
  private def decodeBase64(string: String) = Base64.decodeBase64(string)

  private def cipher(algorithm:String, mode: Int, b64secret: String): Cipher = {
    val k = new SecretKeySpec(decodeBase64(b64secret), algorithm)
    cipher(algorithm, mode, k)
  }

  private def cipher(algorithm:String, mode: Int, key: SecretKeySpec): Cipher = {
    val c = Cipher.getInstance(algorithm + "/ECB/PKCS5Padding")
    c.init(mode, key)
    c
  }

  private def decryptAES(b64secret: String) = {
    new SecretKeySpec(decodeBase64(b64secret), "AES")
  }

  lazy val crypto = new TaglessCrypto[IO](new CryptoDsl[IO] {
    override def generateKey(keyId: String): IO[OpResult[DataKey]] = IO{
      val ptk = genKey("AES", 256)
      DataKey(ptk, Base64.encodeBase64String(ptk), "AES").asRight
    }

    override def encrypt(data: String, dk: DataKey): IO[OpResult[String]] = {
      IO {
        val encoder = cipher("AES", Cipher.ENCRYPT_MODE, dk.cipher)
        val enc = new String(Base64.encodeBase64String(encoder.doFinal(data.getBytes)))
        s"key:${dk.cipher},data:$enc".asRight
      }
    }

    override def decrypt(key: SecretKeySpec, encryptedData: String): IO[OpResult[String]] = IO {
      val decoder = cipher("AES", Cipher.DECRYPT_MODE, key)
      new String(decoder.doFinal(decodeBase64(encryptedData))).asRight
    }

    override def decryptKey(cipher: String): IO[OpResult[SecretKeySpec]] = IO {
      decryptAES(cipher).asRight
    }
  })

}
