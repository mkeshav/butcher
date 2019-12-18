package org.butcher.internals.parser

import fastparse.SingleLineWhitespace._
import fastparse._
sealed trait Expr

sealed trait ColumnNameExpr extends Expr {
  def columns: Seq[String]
}

sealed trait ColumnIndexExpr extends Expr {
  def columns: Seq[Int]
}

final case class UnknownExpr() extends Expr
final case class ColumnNamesMaskExpr(override val columns: Seq[String]) extends Expr with ColumnNameExpr
final case class ColumnIndicesMaskExpr(override val columns: Seq[Int]) extends Expr with ColumnIndexExpr

final case class ColumnNamesEncryptWithKmsExpr(override val columns: Seq[String], keyId: String) extends Expr with ColumnNameExpr
final case class ColumnIndicesEncryptWithKmsExpr(override val columns: Seq[Int], keyId: String) extends Expr with ColumnIndexExpr

private[butcher] object ButcherParser {
  private def Newline[_: P] = P( NoTrace(StringIn("\r\n", "\n")) )
  private def numberParser[_: P]: P[Int] = P( CharIn("0-9").rep(1).!.map(_.toInt) )
  private def commaSeparatedIndicesParser[_: P]: P[Seq[Int]] = P(numberParser.!.map(_.toInt).rep(1, sep=","))
  private def tokenParser[_: P]: P[String] = P( CharIn("A-Za-z0-9_\\-").rep(1).!.map(_.mkString) )
  private def commaSeparatedTokensParser[_: P]: P[Seq[String]] = P(tokenParser.!.rep(min = 1, sep = ","))
  private def columnNamesLineMaskParser[_: P]: P[Expr]  = P(IgnoreCase("column names in [") ~ commaSeparatedTokensParser ~ IgnoreCase("] then mask") ~ Newline.rep(1).?).map {
    ColumnNamesMaskExpr(_)
  }
  private def columnNamesLineEncryptParser[_: P]: P[Expr]  = P(IgnoreCase("column names in [") ~ commaSeparatedTokensParser ~ IgnoreCase("] then encrypt using kms key ") ~ tokenParser ~ Newline.rep(1).?).map {
    case (columns, k) => ColumnNamesEncryptWithKmsExpr(columns, k)
  }
  private def columnIndicesLineMaskParser[_: P]: P[Expr] = P(IgnoreCase("column indices in [") ~ commaSeparatedIndicesParser ~ IgnoreCase("] then mask") ~ Newline.rep(1).?).map {
    ColumnIndicesMaskExpr(_)
  }
  private def columnIndicesLineEncryptParser[_: P]: P[Expr]  = P(IgnoreCase("column indices in [") ~ commaSeparatedIndicesParser ~ IgnoreCase("] then encrypt using kms key ") ~ tokenParser ~ Newline.rep(1).?).map {
    case (columns, k) => ColumnIndicesEncryptWithKmsExpr(columns, k)
  }

  private def namesLineParser[_: P] = P(columnNamesLineMaskParser | columnNamesLineEncryptParser)
  private def indicesLineParser[_: P] = P(columnIndicesLineMaskParser | columnIndicesLineEncryptParser)
  def nameSpecParser[_: P] = P(namesLineParser.rep(1))
  def indicesSpecParser[_: P] = P(indicesLineParser.rep(1))
}
