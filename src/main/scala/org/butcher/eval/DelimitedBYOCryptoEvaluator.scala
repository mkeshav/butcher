package org.butcher.eval

import cats.data.EitherT
import cats.effect.IO
import org.butcher.internals.parser._
import com.roundeights.hasher.Implicits._
import org.butcher.{ColumnReadable, OpResult}
import org.butcher.algebra.CryptoDsl.TaglessCrypto
import org.butcher.internals.parser.ButcherParser.block
import cats.implicits._
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.{CsvMapper, CsvSchema}

import scala.collection.JavaConverters._
import org.butcher.implicits._
import io.circe.syntax._
import org.butcher.algebra.StorageDsl.TaglessStorage
import org.butcher.algebra.{EncryptionResult, StorageDsl}

import scala.collection.mutable

class DelimitedBYOCryptoEvaluator(dsl: TaglessCrypto[IO],
                                  storage: TaglessStorage[IO]) extends Evaluator {
  override def evalDelimited(spec: String, data: String, delimiter: Char = ','): OpResult[String] = {
    val expression = fastparse.parse(spec.trim, block(_))
    val bootstrapSchema = CsvSchema.emptySchema().withHeader().withColumnSeparator(delimiter)
    val mapper = new CsvMapper()
    try {
      val mi: MappingIterator[java.util.Map[String, String]] = mapper.readerFor(classOf[java.util.Map[String, String]]).`with`(bootstrapSchema).readValues(data.trim)
      val s = mi.getParser.getSchema.asInstanceOf[CsvSchema]
      val header = s.iterator().asScala.map(_.getName).mkString("|")
      val res = expression.fold(
        onFailure = {(_, _, extra) => List(extra.trace().longMsg.asLeft)},
        onSuccess = {
          case (expr, _) =>
            val withRowIndex = mi.readAll().asScala.map(_.asScala.toMap).zipWithIndex
            val r = withRowIndex.map {
              case (row, rowIdx) =>
                eval(expr, row).map {
                  v =>
                    val rid = v._1
                    val sensitive = v._2.map(m => (m._1, m._2.sha256.hex))
                    (row ++ sensitive.toMap ++ Map("encryptedRowId" -> rid)).map({case (_, v) => v}).mkString("|")
                }.leftMap(m => s"$rowIdx:$m")

            }
            r
        }
      )
      res.toList.sequence.map(rows => (header :: rows).mkString(System.lineSeparator))
    } catch {
      case e: Throwable => e.getMessage.asLeft
    }
  }

  private def extract(row: ColumnReadable[String], columns: Seq[String]) = {
    columns.map {
      c =>
        row.get(c).map(v => (c, v))
    }.toList.sequence
  }

  private def generateUniqueId(row: ColumnReadable[String], pkColumns: Seq[String]) = {
    val d = extract(row, pkColumns)
    d.map(_.sortBy(_._1).map(_._2).mkString.sha256.hex)
  }

  private def eval(op: Expr, row: ColumnReadable[String]) = {
    op match {
      case EncryptColumnsWithPKExpression(encryptColumns, keyId, pkColumns) =>
        val f = for {
          sensitive <- EitherT(IO.pure(extract(row, encryptColumns)))
          id <- EitherT(IO.pure(generateUniqueId(row, pkColumns)))
          dk <- EitherT(dsl.generateKey(keyId))
          er <- EitherT(dsl.encrypt(sensitive.toMap.asJson.noSpaces, dk))
          _ <- EitherT(storage.put(EncryptionResult(dk.cipher, id, er)))
        } yield (id, sensitive)

        f.value.unsafeRunSync
      case _ => "Unknown Expression".asLeft
    }
  }

  override def evalJson(spec: String, data: String): OpResult[String] = {
    "Use the right method son!!".asLeft
  }
}
