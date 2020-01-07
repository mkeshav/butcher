package org.butcher.eval

import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import com.roundeights.hasher.Implicits._
import io.circe.Json
import org.butcher.algebra.CryptoDsl.TaglessCrypto
import org.butcher.internals.parser.ButcherParser.nameSpecParser
import org.butcher.internals.parser._
import org.butcher.{OpResult}
import io.circe.parser._
import io.circe.generic.auto._

class JsonBYOCryptoEvaluator(dsl: TaglessCrypto[IO]) extends Evaluator {
  override def evalDelimited(spec: String, data: String, delimiter: Char = ','): OpResult[String] = {
    "Use the right method son!!".asLeft
  }

  private def encrypt(keyId: String, v: String) = {
    val io = for {
      dk <- EitherT(dsl.generateKey(keyId))
      ed <- EitherT(dsl.encrypt(v, dk))
    } yield ed

    io.value.unsafeRunSync().getOrElse("Encryption failed")
  }

  private def eval(ops: Seq[Expr], row: Json): OpResult[Json] = {
    ops.foldLeft(row) {
      case (r, ColumnNamesMaskExpr(columns)) =>
        val res = columns.foldLeft(r.hcursor) {
          case (cursor, column) =>
            val mask = cursor.downField(column).withFocus(_.mapString(_.sha256.hex))
            mask.top.getOrElse(r).hcursor
        }
        res.top.getOrElse(r)
      case (r, ColumnNamesEncryptWithKmsExpr(columns, keyId)) =>
        val res = columns.foldLeft(r.hcursor) {
          case (cursor, column) =>
            val mask = cursor.downField(column).withFocus(_.mapString(v => encrypt(keyId, v)))
            mask.top.getOrElse(r).hcursor
        }
        res.top.getOrElse(r)
      case (_, _) => Json.Null
    }.asRight
  }

  override def evalJson(spec: String, data: String): OpResult[String] = {
    val expressions = fastparse.parse(spec.trim, nameSpecParser(_))
    expressions.fold(
      onFailure = {(_, _, extra) => extra.trace().longMsg.asLeft},
      onSuccess = {
        case (es, _) =>
          parse(data).flatMap {
            j =>
              eval(es, j).map(_.noSpaces)
          }.leftMap(pf => pf.toString)
      }
    )
  }
}
