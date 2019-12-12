package org.butcher.parser

import fastparse._, NoWhitespace._
sealed trait Expr

sealed trait ColumnNameExpr extends Expr {
  def columns: Seq[String]
}

sealed trait ColumnIndexExpr extends Expr {
  def columns: Seq[Int]
}

final case class ColumnNamesMaskExpr(override val columns: Seq[String]) extends Expr with ColumnNameExpr
final case class ColumnIndicesMaskExpr(override val columns: Seq[Int]) extends Expr with ColumnIndexExpr

final case class ColumnNamesEncryptExpr(override val columns: Seq[String], plainTextKey: String, encryptedKey: String) extends Expr with ColumnNameExpr
final case class ColumnIndicesEncryptExpr(override val columns: Seq[Int], plainTextKey: String, encryptedKey: String) extends Expr with ColumnIndexExpr

object Butcher {
  def Newline[_: P] = P( NoTrace(StringIn("\r\n", "\n")) )
  private def numberParser[_: P]: P[Int] = P( CharIn("0-9").rep(1).!.map(_.toInt) )
  private def indicesParser[_: P]: P[Seq[Int]] = P(numberParser.!.map(_.toInt).rep(1, sep=","))
  private def tokenParser[_: P]: P[String] = P( CharIn("A-Za-z0-9_\\-").rep(1).!.map(_.mkString) )
  private def tokensParser[_: P]: P[Seq[String]] = P(tokenParser.!.rep(min = 1, sep = ","))
  def columnNamesLineMaskParser[_: P]: P[Expr]  = P(IgnoreCase("column names in [") ~ tokensParser ~ IgnoreCase("] then hash") ~ Newline.rep(1).?).map {
    ColumnNamesMaskExpr(_)
  }
  def columnIndicesLineMaskParser[_: P]: P[Expr] = P(IgnoreCase("column indices in [") ~ indicesParser ~ IgnoreCase("] then hash") ~ Newline.rep(1).?).map {
    ColumnIndicesMaskExpr(_)
  }
  def columnNamesLineEncryptParser[_: P]: P[Expr]  = P(IgnoreCase("column names in [") ~ tokensParser ~ IgnoreCase("] then encrypt using ") ~ tokenParser ~ ":" ~ tokenParser ~ Newline.rep(1).?).map {
    case (columns, pt, ct) => ColumnNamesEncryptExpr(columns, pt, ct)
  }

  def columnIndicesLineEncryptParser[_: P]: P[Expr]  = P(IgnoreCase("column indices in [") ~ tokensParser ~ IgnoreCase("] then encrypt using ") ~ tokenParser ~ ":" ~ tokenParser ~ Newline.rep(1).?).map {
    case (columns, pt, ct) => ColumnNamesEncryptExpr(columns, pt, ct)
  }

  def lineParser[_: P] = P(columnNamesLineMaskParser | columnNamesLineEncryptParser)
  def nameSpecParser[_: P] = P(lineParser.rep)
}
