package org.butcher.eval

import org.butcher.OpResult

trait Evaluator {
  def evalDelimited(spec: String, data: String, delimiter: Char): OpResult[String]
  def evalJson(spec: String, data: String): OpResult[String]
}
