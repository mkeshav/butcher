package org.butcher.parser

import com.amazonaws.services.kms.AWSKMSClientBuilder
import fastparse.NoWhitespace._
import fastparse._
sealed trait Expr

sealed trait ColumnNameExpr extends Expr {
  def columns: Seq[String]
}

sealed trait ColumnIndexExpr extends Expr {
  def columns: Seq[Int]
}

final case class ColumnNamesMaskExpr(override val columns: Seq[String]) extends Expr with ColumnNameExpr
final case class ColumnIndicesMaskExpr(override val columns: Seq[Int]) extends Expr with ColumnIndexExpr

final case class ColumnNamesEncryptExpr(override val columns: Seq[String], keyId: String) extends Expr with ColumnNameExpr
final case class ColumnIndicesEncryptExpr(override val columns: Seq[Int], keyId: String) extends Expr with ColumnIndexExpr

trait ColumnReadable[T] {
  def get(column: String): Either[Throwable, T]
  def get(index: Int): Either[Throwable, T]
}

case class Butchered(column: String, value: String)

object ButcherParser {
  def Newline[_: P] = P( NoTrace(StringIn("\r\n", "\n")) )
  private def numberParser[_: P]: P[Int] = P( CharIn("0-9").rep(1).!.map(_.toInt) )
  private def indicesParser[_: P]: P[Seq[Int]] = P(numberParser.!.map(_.toInt).rep(1, sep=","))
  private def tokenParser[_: P]: P[String] = P( CharIn("A-Za-z0-9_\\-").rep(1).!.map(_.mkString) )
  private def tokensParser[_: P]: P[Seq[String]] = P(tokenParser.!.rep(min = 1, sep = ","))
  def columnNamesLineMaskParser[_: P]: P[Expr]  = P(IgnoreCase("column names in [") ~ tokensParser ~ IgnoreCase("] then mask") ~ Newline.rep(1).?).map {
    ColumnNamesMaskExpr(_)
  }
  def columnIndicesLineMaskParser[_: P]: P[Expr] = P(IgnoreCase("column indices in [") ~ indicesParser ~ IgnoreCase("] then mask") ~ Newline.rep(1).?).map {
    ColumnIndicesMaskExpr(_)
  }
  def columnNamesLineEncryptParser[_: P]: P[Expr]  = P(IgnoreCase("column names in [") ~ tokensParser ~ IgnoreCase("] then encrypt using kms key ") ~ tokenParser ~ Newline.rep(1).?).map {
    case (columns, k) => ColumnNamesEncryptExpr(columns, k)
  }

  def columnIndicesLineEncryptParser[_: P]: P[Expr]  = P(IgnoreCase("column indices in [") ~ indicesParser ~ IgnoreCase("] then encrypt using kms key ") ~ tokenParser ~ Newline.rep(1).?).map {
    case (columns, k) => ColumnIndicesEncryptExpr(columns, k)
  }

  def lineParser[_: P] = P(columnNamesLineMaskParser | columnNamesLineEncryptParser)
  def nameSpecParser[_: P] = P(lineParser.rep)
}
