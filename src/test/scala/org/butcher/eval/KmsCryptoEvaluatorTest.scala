package org.butcher.eval

import java.nio.ByteBuffer

import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.{DecryptResult, GenerateDataKeyResult}
import com.amazonaws.util.Base64
import org.butcher.internals.kms.KMSCryptoIOInterpreter
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}

class KmsCryptoEvaluatorTest extends FunSuite with Matchers with MockFactory {
  val b64EncodedPlainTextKey = "acZLXO+SWyeV95LYvUMExQtGeDHExNkAjvXbpbUEMK0="
  val b64EncodedCipherTextBlob = "AQIDAHhoNt+QMcK2fLVptebsdn939rqRYSkfDPtL70lK0fvadAGctDSWR9FFQo/sjJINvabqAAAAfjB8BgkqhkiG9w0BBwagbzBtAgEAMGgGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMRXCvv+D0JW3bZA6hAgEQgDvx1mHmiC1xdu4IDLY38QmgcVJf3vxxrM/v5I9OFL/kls9DkP1fhZI1GJtiJ3nQaEsYjO5oBSmsRdNEpA=="

  val kms = stub[AWSKMS]
  val generateDataKeyResult = new GenerateDataKeyResult()
    .withPlaintext(ByteBuffer.wrap(Base64.decode(b64EncodedPlainTextKey)))
    .withCiphertextBlob(ByteBuffer.wrap(Base64.decode(b64EncodedCipherTextBlob)))
  (kms.generateDataKey _).when(*).returns(generateDataKeyResult)

  val decryptResult = new DecryptResult().withPlaintext(ByteBuffer.wrap(Base64.decode(b64EncodedPlainTextKey)))
  (kms.decrypt _).when(*).returns(decryptResult)

  val e = new KmsCryptoEvaluator(kms)

  test("eval with kms") {
    val spec =
      s"""
         |column names in [driversLicence] then encrypt using kms key foo
         |column names in [firstName] then mask
         |""".stripMargin

    val data =
      """
        |firstName,driversLicence,donothing
        |satan,666,1
        |god,333,2
        |""".stripMargin

    e.eval(spec, data).isRight should be(true)
  }
}
