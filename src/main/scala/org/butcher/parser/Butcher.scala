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
  private def numberParser[_: P]: P[Int] = P( CharIn("0-9").rep(1).!.map(_.toInt) )
  private def indicesParser[_: P]: P[Seq[Int]] = P(numberParser.!.map(_.toInt).rep(1, sep=","))
  private def tokenParser[_: P]: P[String] = P( CharIn("A-Za-z0-9_\\-").rep(1).!.map(_.mkString) )
  private def tokensParser[_: P]: P[Seq[String]] = P(tokenParser.!.rep(min = 1, sep = ","))
  def columnNamesLineParser[_: P]: P[Expr]  = P(IgnoreCase("column names in [") ~ tokensParser ~ IgnoreCase("] then ") ~ tokenParser ~ End).map {
    case (columns, _) => ColumnNamesActionExpr(columns, Hash)
  }
  def columnIndicesLineParser[_: P]: P[Expr]  = P(IgnoreCase("column indices in [") ~ indicesParser ~ IgnoreCase("] then ") ~ tokenParser ~ End).map {
    case (columns, _) => ColumnIndicesActionExpr(columns, Hash)
  }
}
