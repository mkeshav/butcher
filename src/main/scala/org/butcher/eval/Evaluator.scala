package org.butcher.eval

import fastparse.Parsed
import org.butcher.{ColumnReadable, OpResult}
import org.butcher.algebra.DataKey
import org.butcher.internals.parser.Expr
import cats.effect.IO

final case class EvalResult(row: String, encryptedContentId: String)
trait Evaluator {
  def eval[T >: Parsed[Expr]](expr: T, key: DataKey, row: ColumnReadable):IO[OpResult[EvalResult]]
}
