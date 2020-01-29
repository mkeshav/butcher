package org.butcher.internals.interpreters

import java.nio.ByteBuffer

import cats.data.EitherT
import cats.effect.IO
import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.{DecryptResult, GenerateDataKeyRequest, GenerateDataKeyResult}
import com.amazonaws.util.Base64
import org.butcher.algebra.CryptoDsl.TaglessCrypto
import org.butcher.algebra.DataKey
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}

class KMSCryptoIOInterpreterTest extends FunSuite with Matchers with MockFactory{
  val b64EncodedPlainTextKey = "acZLXO+SWyeV95LYvUMExQtGeDHExNkAjvXbpbUEMK0="
  val b64EncodedCipherTextBlob = "AQIDAHhoNt+QMcK2fLVptebsdn939rqRYSkfDPtL70lK0fvadAGctDSWR9FFQo/sjJINvabqAAAAfjB8BgkqhkiG9w0BBwagbzBtAgEAMGgGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMRXCvv+D0JW3bZA6hAgEQgDvx1mHmiC1xdu4IDLY38QmgcVJf3vxxrM/v5I9OFL/kls9DkP1fhZI1GJtiJ3nQaEsYjO5oBSmsRdNEpA=="

  val dk = DataKey(
    Base64.decode(b64EncodedPlainTextKey),
    b64EncodedCipherTextBlob, "AES")
  test("kms io interpreter") {
    val kms = stub[AWSKMS]
    val decryptResult = new DecryptResult().withPlaintext(ByteBuffer.wrap(Base64.decode(b64EncodedPlainTextKey)))
    (kms.decrypt _).when(*).returns(decryptResult)

    val interpreter = new KMSCryptoIOInterpreter(kms)
    val tl = new TaglessCrypto[IO](interpreter)
    val f = for {
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

}
