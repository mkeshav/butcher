package org.butcher.eval

import com.amazonaws.services.kms.{AWSKMS, AWSKMSClientBuilder}
import org.butcher.algebra.CryptoDsl.TaglessCrypto
import org.butcher.internals.kms.KMSCryptoIOInterpreter
import cats.effect.IO
import org.butcher.OpResult

class KmsCryptoEvaluator(kmsClient: AWSKMS) {
  private lazy val ki = new KMSCryptoIOInterpreter(kmsClient)
  private lazy val e = new BYOCryptoEvaluator(new TaglessCrypto[IO](ki))

  def eval(spec: String, data: String): OpResult[String] = {
    e.evalWithHeader(spec, data)
  }
}
