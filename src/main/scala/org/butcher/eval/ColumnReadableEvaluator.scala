package org.butcher.eval

import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import com.roundeights.hasher.Implicits._
import fastparse.Parsed
import io.circe.syntax._
import org.butcher.algebra.CryptoDsl.TaglessCrypto
import org.butcher.algebra.StorageDsl.TaglessStorage
import org.butcher.algebra.{DataKey, EncryptionResult}
import org.butcher.parser.{EncryptColumnsWithPKExpression, _}
import org.butcher.{ColumnReadable, OpResult}

class ColumnReadableEvaluator(dsl: TaglessCrypto[IO],
                              storage: TaglessStorage[IO]) extends Evaluator {

  override def eval[T >: Parsed[Expr]](expr: T, key: DataKey, row: ColumnReadable): IO[OpResult[EvalResult]] = {
    val res: IO[OpResult[EvalResult]] = expr.asInstanceOf[Parsed[Expr]].fold(
      onFailure = {(_, _, extra) => IO.pure(extra.trace().longMsg.asLeft[EvalResult])},
      onSuccess = {
        case (e, _) =>
          doWork(e, key, row).map {
            _.map {
              v =>
                val sensitiveMasked = v._2.map(m => (m._1, m._2.sha256.hex))
                val contentMap = row.toMap ++ sensitiveMasked.toMap
                val content = contentMap.map({case (_, v) => v}).mkString("|")
                EvalResult(content, v._1)
            }
          }
      }
    )
    res
  }

  private def extract(row: ColumnReadable, columns: Seq[String]) = {
    columns.map {
      c =>
        row.get(c).map(v => (c, v))
    }.toList.sequence
  }

  private def generateUniqueId(row: ColumnReadable, pkColumns: Seq[String]) = {
    val d = extract(row, pkColumns)
    d.map(_.sortBy(_._1).map(_._2).mkString.sha256.hex)
  }


  private def doWork(expr: Expr, key: DataKey, row: ColumnReadable): IO[OpResult[(String, List[(String, String)])]] = {
    expr match {
      case EncryptColumnsWithPKExpression(encryptColumns, pkColumns) =>
        val f = for {
          sensitive <- EitherT(IO.pure(extract(row, encryptColumns)))
          id <- EitherT(IO.pure(generateUniqueId(row, pkColumns)))
          er <- EitherT(dsl.encrypt(sensitive.toMap.asJson.noSpaces, key))
          _ <- EitherT(storage.put(EncryptionResult(key.cipher, id, er)))
        } yield (id, sensitive)

        f.value
      case _ => IO.pure("Unknown Expression".asLeft)
    }
  }
}
