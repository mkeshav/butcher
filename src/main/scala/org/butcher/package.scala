package org

import java.time.{ZoneId, ZonedDateTime}

package object butcher {
  type OpResult[T] = Either[String, T]
  trait ColumnReadable {
    def get(column: String): OpResult[String]
    def toMap: Map[String, String]
  }

  class NamedLookup(m: Map[String, String]) extends ColumnReadable {
    override def get(column: String): OpResult[String] = m.get(column).toRight(s"Column $column not found")

    override def toMap: Map[String, String] = m
  }

  object time {
    def utcNow: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))
    def utcNowEpochMillis: Long = utcNow.toInstant.toEpochMilli
  }

  object implicits {
    implicit def makeMyMapColumnReadable(m: Map[String, String]): NamedLookup = new NamedLookup(m)
  }
}
