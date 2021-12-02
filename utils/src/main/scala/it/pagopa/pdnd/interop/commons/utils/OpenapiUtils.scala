package it.pagopa.pdnd.interop.commons.utils

import it.pagopa.pdnd.interop.commons.utils.TypeConversions.StringOps

trait OpenapiUtils {
  def parseArrayParameters(params: String): List[String] = {
    if (params == "[]") List.empty else params.parseCommaSeparated
  }

  def verifyParametersByCondition(params: List[String]): String => Boolean = { s =>
    params.isEmpty || params.contains(s)
  }
}

object OpenapiUtils extends OpenapiUtils
