package org.butcher.eval

import cats.data.EitherT
import cats.effect.IO
import org.butcher.parser._
import com.roundeights.hasher.Implicits._
import org.butcher.ColumnReadable
import org.butcher.kms.CryptoDsl.TaglessCrypto
import org.butcher.parser.ButcherParser.nameSpecParser
import cats.implicits._
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.{CsvMapper, CsvSchema}

import scala.collection.JavaConverters._
import org.butcher.implicits._

import scala.collection.mutable

class ButcherEval(dsl: TaglessCrypto[IO]) {
  def evalWithHeader(spec: String, data: String): Either[Throwable, List[String]] = {
    val expressions = fastparse.parse(spec.trim, nameSpecParser(_))
    val res = expressions.fold(
      onFailure = {(_, _, extra) => List(Left(new Throwable(extra.trace().longMsg)))},
      onSuccess = {
        case (es, _) =>
          val bootstrapSchema = CsvSchema.emptySchema().withHeader();
          val mapper = new CsvMapper()
          val mi: MappingIterator[java.util.Map[String, String]] = mapper.readerFor(classOf[java.util.Map[String, String]]).`with`(bootstrapSchema).readValues(data.trim)
          val r: mutable.Seq[Either[Throwable, String]] = mi.readAll().asScala.map(_.asScala.toMap).map {
            row =>
              val masked = eval(es, row)
              masked.sequence.map {
                m =>
                  (row ++ m.toMap).map({case (_, v) => v}).mkString("|")
              }
          }
          r
      }
    )
    res.toList.sequence
  }

  private def eval(ops: Seq[Expr], row: ColumnReadable[String]): List[Either[Throwable, (String, String)]] = {
    ops.foldLeft(List.empty[Either[Throwable, (String, String)]]) {
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
      case (acc, _) => acc ++ List(Left(new Throwable("Unknown expression")))
    }
  }
}
