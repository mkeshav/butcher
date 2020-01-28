package org.butcher.eval

import com.amazonaws.services.kms.{AWSKMS, AWSKMSClientBuilder}
import org.butcher.algebra.CryptoDsl.TaglessCrypto
import cats.effect.IO
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import org.butcher.OpResult
import org.butcher.algebra.StorageDsl.TaglessStorage
import org.butcher.interpreters.{DynamoStorageIOInterpreter, KMSCryptoIOInterpreter}

class DelimitedKmsCryptoEvaluator(kmsClient: AWSKMS, tableName: String, dynamo: AmazonDynamoDBAsync) {
  private lazy val ki = new KMSCryptoIOInterpreter(kmsClient)
  private lazy val si = new DynamoStorageIOInterpreter(tableName, dynamo)
  private lazy val e = new DelimitedBYOCryptoEvaluator(new TaglessCrypto[IO](ki),
    new TaglessStorage[IO](si))

}
