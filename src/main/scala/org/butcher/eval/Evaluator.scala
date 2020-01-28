package org.butcher.eval

import cats.effect.IO
import fastparse.Parsed
import org.butcher.algebra.DataKey
import org.butcher.parser.Expr
import org.butcher.{ColumnReadable, OpResult}

final case class EvalResult(row: String, encryptedContentId: String)
trait Evaluator {
  def eval[T >: Parsed[Expr]](expr: T, key: DataKey, row: ColumnReadable):IO[OpResult[EvalResult]]
}
