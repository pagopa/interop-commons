package it.pagopa.pdnd.interop.commons.utils

import it.pagopa.pdnd.interop.commons.utils.TypeConversions.StringOps

trait OpenapiUtils {
  def parseArrayParameters(params: String): List[String] = {
    if (params == "[]") List.empty else params.parseCommaSeparated
  }

  def verifyParametersByCondition[A](params: List[A]): A => Boolean = { s =>
    params.isEmpty || params.contains(s)
  }
}

object OpenapiUtils extends OpenapiUtils
