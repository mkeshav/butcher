package org.butcher.internals.interpreters

import java.nio.ByteBuffer

import cats.data.EitherT
import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.{DecryptResult, GenerateDataKeyRequest, GenerateDataKeyResult}
import com.amazonaws.util.Base64
import org.butcher.algebra.CryptoDsl.TaglessCrypto
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}
import cats.effect.IO

class KMSCryptoIOInterpreterTest extends FunSuite with Matchers with MockFactory{
  val b64EncodedPlainTextKey = "acZLXO+SWyeV95LYvUMExQtGeDHExNkAjvXbpbUEMK0="
  val b64EncodedCipherTextBlob = "AQIDAHhoNt+QMcK2fLVptebsdn939rqRYSkfDPtL70lK0fvadAGctDSWR9FFQo/sjJINvabqAAAAfjB8BgkqhkiG9w0BBwagbzBtAgEAMGgGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMRXCvv+D0JW3bZA6hAgEQgDvx1mHmiC1xdu4IDLY38QmgcVJf3vxxrM/v5I9OFL/kls9DkP1fhZI1GJtiJ3nQaEsYjO5oBSmsRdNEpA=="

  test("kms io interpreter") {
    val kms = stub[AWSKMS]
    val generateDataKeyResult = new GenerateDataKeyResult()
      .withPlaintext(ByteBuffer.wrap(Base64.decode(b64EncodedPlainTextKey)))
      .withCiphertextBlob(ByteBuffer.wrap(Base64.decode(b64EncodedCipherTextBlob)))
    (kms.generateDataKey _).when(*).returns(generateDataKeyResult)

    val decryptResult = new DecryptResult().withPlaintext(ByteBuffer.wrap(Base64.decode(b64EncodedPlainTextKey)))
    (kms.decrypt _).when(*).returns(decryptResult)

    val interpreter = new KMSCryptoIOInterpreter(kms)
    val tl = new TaglessCrypto[IO](interpreter)
    val f = for {
      dk <- EitherT(tl.generateKey("foo"))
      ed <- EitherT(tl.encrypt("foo", dk))
      ss <- EitherT(tl.decryptKey(dk.cipher))
      dd <- EitherT(tl.decrypt(ss, ed))
    } yield (ed, dd)


    f.value.unsafeRunSync().fold({t => println(t); false should be(true)}, {
      v =>
        v._1 should be("5gVr+Ca1Tqs9BirpPopOmw==")
        v._2 should be("foo")
    })

  }

  test("encrypt exception") {
    val kms = stub[AWSKMS]
    (kms.generateDataKey _).when(_:GenerateDataKeyRequest).throwing(new Throwable("Say permission error"))
    val interpreter = new KMSCryptoIOInterpreter(kms)
    val tl = new TaglessCrypto[IO](interpreter)

    val f = for {
      dk <- tl.generateKey("foo")
    } yield dk
    f.unsafeRunSync.isLeft should be(true)
  }

}
