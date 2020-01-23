package org.butcher.eval

import com.amazonaws.services.kms.{AWSKMS, AWSKMSClientBuilder}
import org.butcher.algebra.CryptoDsl.TaglessCrypto
import cats.effect.IO
import org.butcher.OpResult
import org.butcher.interpreters.KMSCryptoIOInterpreter

class DelimitedKmsCryptoEvaluator(kmsClient: AWSKMS) {
  private lazy val ki = new KMSCryptoIOInterpreter(kmsClient)
  private lazy val e = new DelimitedBYOCryptoEvaluator(new TaglessCrypto[IO](ki))

  def eval(spec: String, data: String): OpResult[String] = {
    e.evalDelimited(spec, data)
  }
}
