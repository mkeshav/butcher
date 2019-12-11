package org.butcher.parser

import fastparse._, NoWhitespace._
sealed trait Expr

sealed trait ColumnNameExpr extends Expr {
  def columns: Seq[String]
}

sealed trait ColumnIndexExpr extends Expr {
  def columns: Seq[Int]
}

sealed trait Action
case object Hash extends Action

final case class ColumnNamesActionExpr(override val columns: Seq[String], action: Action) extends Expr with ColumnNameExpr
final case class ColumnIndicesActionExpr(override val columns: Seq[Int], action: Action) extends Expr with ColumnIndexExpr

object Butcher {
  def numberParser[_: P]: P[Int] = P( CharIn("0-9").rep(1).!.map(_.mkString.toInt) )
  def tokenParser[_: P]: P[String] = P( CharIn("A-Za-z0-9_\\-").rep(1).!.map(_.mkString) )
  def namesParser[_: P]: P[Expr]  = P(IgnoreCase("column names in [") ~ tokenParser.rep(1, sep = ",") ~ IgnoreCase("] then ") ~ tokenParser ~ End).map {
    case (columns, _) => ColumnNamesActionExpr(columns, Hash)
  }
  def indicesParser[_: P]: P[Expr]  = P(IgnoreCase("column indices in [") ~ numberParser.rep(1, sep = ",") ~ IgnoreCase("] then ") ~ tokenParser ~ End).map {
    case (columns, _) => ColumnIndicesActionExpr(columns, Hash)
  }
}
