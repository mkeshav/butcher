package org.butcher.kms

import java.nio.ByteBuffer

import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.{DecryptResult, GenerateDataKeyRequest, GenerateDataKeyResult}
import com.amazonaws.util.Base64
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}
import KMSService._

class KMSServiceTest extends FunSuite with MockFactory with Matchers {
  val b64EncodedPlainTextKey = "acZLXO+SWyeV95LYvUMExQtGeDHExNkAjvXbpbUEMK0="
  val b64EncodedCipherTextBlob = "AQIDAHhoNt+QMcK2fLVptebsdn939rqRYSkfDPtL70lK0fvadAGctDSWR9FFQo/sjJINvabqAAAAfjB8BgkqhkiG9w0BBwagbzBtAgEAMGgGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMRXCvv+D0JW3bZA6hAgEQgDvx1mHmiC1xdu4IDLY38QmgcVJf3vxxrM/v5I9OFL/kls9DkP1fhZI1GJtiJ3nQaEsYjO5oBSmsRdNEpA=="

  test("encrypt") {
    val kms = stub[AWSKMS]
    val generateDataKeyResult = new GenerateDataKeyResult()
      .withPlaintext(ByteBuffer.wrap(Base64.decode(b64EncodedPlainTextKey)))
      .withCiphertextBlob(ByteBuffer.wrap(Base64.decode(b64EncodedCipherTextBlob)))
    (kms.generateDataKey _).when(*).returns(generateDataKeyResult)

    val f = for {
      dk <- generateDataKey("foo").run(kms)
      ed <- encryptWith("foo", dk).run(kms)
    } yield ed

    f.fold({t => println(t); false should be(true)}, {
      v =>
        v should be("key:AQIDAHhoNt+QMcK2fLVptebsdn939rqRYSkfDPtL70lK0fvadAGctDSWR9FFQo/sjJINvabqAAAAfjB8BgkqhkiG9w0BBwagbzBtAgEAMGgGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMRXCvv+D0JW3bZA6hAgEQgDvx1mHmiC1xdu4IDLY38QmgcVJf3vxxrM/v5I9OFL/kls9DkP1fhZI1GJtiJ3nQaEsYjO5oBSmsRdNEpA==,data:5gVr+Ca1Tqs9BirpPopOmw==")
    })
  }

  test("encrypt exception") {
    val kms = stub[AWSKMS]
    (kms.generateDataKey _).when(_:GenerateDataKeyRequest).throwing(new Throwable("Say permission error"))
    val f = for {
      dk <- generateDataKey("foo").run(kms)
      ed <- encryptWith("foo", dk).run(kms)
    } yield ed
    f.isLeft should be(true)
  }
}
