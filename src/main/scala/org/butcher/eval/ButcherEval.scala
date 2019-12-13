package org.butcher.eval

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
      case (acc, ColumnNamesEncryptExpr(columns, kmsKey)) =>
        val encrypted = columns.map {
          c =>
            row.get(c).flatMap {
              v =>
                dsl.encrypt(v, kmsKey).unsafeRunSync().map(e => Butchered(c, e))
            }
        }
        acc ++ encrypted
      case (acc, _) => acc ++ List(new Throwable("Unknown expression").asLeft)
    }
  }
}
