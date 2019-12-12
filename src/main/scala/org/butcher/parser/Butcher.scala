package org.butcher.parser

import java.util.Base64

import fastparse._
import NoWhitespace._
import com.roundeights.hasher.Implicits._
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import cats.syntax.either._
import com.amazonaws.services.kms.{AWSKMS, AWSKMSClientBuilder}
import org.butcher.kms.KMSService.encrypt
sealed trait Expr

sealed trait ColumnNameExpr extends Expr {
  def columns: Seq[String]
}

sealed trait ColumnIndexExpr extends Expr {
  def columns: Seq[Int]
}

final case class ColumnNamesMaskExpr(override val columns: Seq[String]) extends Expr with ColumnNameExpr
final case class ColumnIndicesMaskExpr(override val columns: Seq[Int]) extends Expr with ColumnIndexExpr

final case class ColumnNamesEncryptExpr(override val columns: Seq[String], kmsKey: String) extends Expr with ColumnNameExpr
final case class ColumnIndicesEncryptExpr(override val columns: Seq[Int], kmsKey: String) extends Expr with ColumnIndexExpr

trait ColumnReadable[T] {
  def get(column: String): Either[Throwable, T]
  def get(index: Int): Either[Throwable, T]
}

case class Butchered(column: String, value: String)

object Butcher {
  lazy val kmsClient = AWSKMSClientBuilder.standard().withRegion("ap-southeast-2").build()
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


  def eval(ops: Seq[Expr], row: ColumnReadable[String]): List[Either[Throwable, Butchered]] = {
    ops.foldLeft(List.empty[Either[Throwable, Butchered]]) {
      case (acc, ColumnNamesMaskExpr(columns)) =>
        val masked = columns.map {
          c =>
            row.get(c).map(v => Butchered(c, v.sha256.hex))
        }
        acc ++ masked
      case (acc, ColumnNamesEncryptExpr(columns, kmsKey)) =>
        val encrypted = columns.map {
          c =>
            row.get(c).flatMap {
              v =>
                encrypt(v, kmsKey).run(kmsClient).map(r => Butchered(c, r))
            }
        }
        acc ++ encrypted
      case (acc, _) => acc ++ List(new Throwable("Unknown expression").asLeft)
    }
  }
}
