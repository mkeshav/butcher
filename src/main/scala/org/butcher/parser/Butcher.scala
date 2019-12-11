package org.butcher.parser

import fastparse._, NoWhitespace._
sealed trait Expr

sealed trait DelimitedExpr extends Expr {
  def columns: Seq[String]
}

sealed trait Action
case object Hash extends Action

final case class ActionExpr(override val columns: Seq[String], action: Action) extends Expr with DelimitedExpr

object Butcher {
  def tokenParser[_: P]: P[String] = P( CharIn("A-Za-z0-9_\\-").rep(1).!.map(_.mkString) )
  def inParser[_: P]: P[ActionExpr]  = P("column_name in [" ~ tokenParser.rep(1, sep = ",") ~ "] then " ~ tokenParser ~ End).map {
    case (columns, _) => ActionExpr(columns, Hash)
  }
}
