package org

package object butcher {
  type OpResult[T] = Either[String, T]
  trait ColumnReadable[T] {
    def get(column: String): OpResult[T]
  }

  class NamedLookup[T](m: Map[String, T]) extends ColumnReadable[T] {
    override def get(column: String): OpResult[T] = m.get(column).toRight(s"Column $column not found")
  }

  object implicits {
    implicit def makeMyMapColumnReadable(m: Map[String, String]): NamedLookup[String] = new NamedLookup(m)
  }
}
