package org.butcher.parser

import fastparse._, NoWhitespace._
sealed trait Expr

sealed trait DelimitedExpr extends Expr {
  def fieldName: String
}

final case class HashActionExpr(override val fieldName: String) extends Expr with DelimitedExpr

object Butcher {
  def tokenParser[_: P]: P[String] = P( CharIn("A-Za-z0-9_\\-").rep(1).!.map(_.mkString) )
}
