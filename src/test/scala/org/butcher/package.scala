package org

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClientBuilder}

import scala.collection.JavaConverters._

package object test {
  object dynamo {
    def createClient(endpoint: String) = {
      val awsCredentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials("", ""))
      AmazonDynamoDBAsyncClientBuilder.standard()
        .withCredentials(awsCredentialsProvider)
        .withEndpointConfiguration(
          new EndpointConfiguration(endpoint, "ap-southeast-2")).build()
    }
    def createTable(tableName: String, dynamoClient: AmazonDynamoDBAsync): Unit = {
      println(s"creating $tableName table")
      try {
        dynamoClient.describeTable(new DescribeTableRequest(tableName))
      } catch {
        case _: Throwable =>
          println(s"$tableName  table does not exist")
          val pt = new ProvisionedThroughput(5L, 5L)
          val hashK = new KeySchemaElement("rowId", KeyType.HASH)
          val hashAttr = new AttributeDefinition("rowId", ScalarAttributeType.S)
          val ctr = new CreateTableRequest(List(hashAttr).asJava, tableName,
            List(hashK).asJava, pt)

          dynamoClient.createTable(ctr)

          if (!waitForTableToBeActive(tableName, dynamoClient)) throw new Exception("Waited enough for the table to become active...")
          println(s"$tableName table created")
      }

      def waitForTableToBeActive(tableName: String, dynamoClient: AmazonDynamoDBAsync):Boolean = {
        println(s"waiting for table $tableName to be active")
        var i = 0
        while (i < 3) {
          val tdesc = dynamoClient.describeTable(new DescribeTableRequest(tableName)).getTable
          if (!tdesc.getTableStatus.equals(TableStatus.ACTIVE.toString)) Thread.sleep(200) else return true
          i+=1
        }
        false
      }
    }
  }
}
