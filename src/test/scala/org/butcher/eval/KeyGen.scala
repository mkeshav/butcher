package org.butcher.eval

import cats.effect.IO
import javax.crypto.{Cipher, KeyGenerator}
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64
import org.butcher.OpResult
import org.butcher.algebra.CryptoDsl.TaglessCrypto
import cats.syntax.either._
import org.butcher.algebra.{CryptoDsl, DataKey}

object KeyGen {
  private def genKey(algorithm: String, size: Int): Array[Byte] = {
    val generator = KeyGenerator.getInstance(algorithm)
    generator.init(size)
    generator.generateKey().getEncoded
  }
  private def decodeBase64(string: String) = Base64.decodeBase64(string)

  private def cipher(algorithm:String, mode: Int, b64secret: String): Cipher = {
    val c = Cipher.getInstance(algorithm + "/ECB/PKCS5Padding")
    c.init(mode, new SecretKeySpec(decodeBase64(b64secret), algorithm))
    c
  }

  private def decryptAES(bytes: Array[Byte], b64secret: String): Array[Byte] = {
    val decoder = cipher("AES", Cipher.DECRYPT_MODE, b64secret)
    decoder.doFinal(bytes)
  }

  lazy val crypto = new TaglessCrypto[IO](new CryptoDsl[IO] {
    override def generateKey(keyId: String): IO[OpResult[DataKey]] = IO{
      val ptk = genKey("AES", 256)
      DataKey(ptk, Base64.encodeBase64String(ptk)).asRight
    }

    override def encrypt(data: String, dk: DataKey): IO[OpResult[String]] = {
      IO {
        val encoder = cipher("AES", Cipher.ENCRYPT_MODE, dk.cipher)
        val enc = new String(Base64.encodeBase64String(encoder.doFinal(data.getBytes)))
        s"key:${dk.cipher},data:$enc".asRight
      }
    }

    override def decrypt(value: String): IO[OpResult[String]] = IO {
      val parts = value.split(",")
      val key = parts(0).split(":")(1)
      val data = parts(1).split(":")(1)
      new String(decryptAES(decodeBase64(data), key)).asRight
    }
  })

}
