package org.butcher.eval

import cats.data.EitherT
import cats.effect.IO
import org.butcher.parser._
import com.roundeights.hasher.Implicits._
import org.butcher.{ColumnReadable, OpResult}
import org.butcher.kms.CryptoDsl.TaglessCrypto
import org.butcher.parser.ButcherParser.nameSpecParser
import cats.implicits._
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.{CsvMapper, CsvSchema}

import scala.collection.JavaConverters._
import org.butcher.implicits._

import scala.collection.mutable

class ButcherEval(dsl: TaglessCrypto[IO]) {
  def evalWithHeader(spec: String, data: String): OpResult[String] = {
    val expressions = fastparse.parse(spec.trim, nameSpecParser(_))
    val bootstrapSchema = CsvSchema.emptySchema().withHeader();
    val mapper = new CsvMapper()
    try {
      val mi: MappingIterator[java.util.Map[String, String]] = mapper.readerFor(classOf[java.util.Map[String, String]]).`with`(bootstrapSchema).readValues(data.trim)
      val s = mi.getParser.getSchema.asInstanceOf[CsvSchema]
      val header = s.iterator().asScala.map(_.getName).mkString("|")
      val res = expressions.fold(
        onFailure = {(_, _, extra) => List(extra.trace().longMsg.asLeft)},
        onSuccess = {
          case (es, _) =>
            val withRowIndex = mi.readAll().asScala.map(_.asScala.toMap).zipWithIndex
            val r: mutable.Seq[OpResult[String]] = withRowIndex.map {
              case (row, rowIdx) =>
                val masked = eval(es, row)
                masked.sequence.map {
                  m =>
                    (row ++ m.toMap).map({case (_, v) => v}).mkString("|")
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

  private def eval(ops: Seq[Expr], row: ColumnReadable[String]): List[OpResult[(String, String)]] = {
    ops.foldLeft(List.empty[OpResult[(String, String)]]) {
      case (acc, ColumnNamesMaskExpr(columns)) =>
        val masked = columns.map {
          c =>
            row.get(c).map(v => c -> v.sha256.hex)
        }
        acc ++ masked
      case (acc, ColumnNamesEncryptExpr(columns, keyId)) =>
        val encrypted = columns.map {
          c =>
            row.get(c).flatMap {
              v =>
                val io = for {
                  dk <- EitherT(dsl.generateKey(keyId))
                  ed <- EitherT(dsl.encrypt(v, dk))
                } yield ed

                io.value.unsafeRunSync().map(enc => c -> enc)
            }
        }
        acc ++ encrypted
      case (acc, _) => acc ++ List("Unknown expression".asLeft)
    }
  }
}
