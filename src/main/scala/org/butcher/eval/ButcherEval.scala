package org.butcher.eval

import cats.data.EitherT
import cats.effect.IO
import org.butcher.parser._
import com.roundeights.hasher.Implicits._
import cats.syntax.either._
import org.butcher.kms.CryptoDsl.TaglessCrypto

class ButcherEval(dsl: TaglessCrypto[IO]) {
  def eval(ops: Seq[Expr], row: ColumnReadable[String]): List[Either[Throwable, Butchered]] = {
    ops.foldLeft(List.empty[Either[Throwable, Butchered]]) {
      case (acc, ColumnNamesMaskExpr(columns)) =>
        val masked = columns.map {
          c =>
            row.get(c).map(v => Butchered(c, v.sha256.hex))
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

                io.value.unsafeRunSync().map(enc => Butchered(c, enc))
            }
        }
        acc ++ encrypted
      case (acc, _) => acc ++ List(new Throwable("Unknown expression").asLeft)
    }
  }
}
