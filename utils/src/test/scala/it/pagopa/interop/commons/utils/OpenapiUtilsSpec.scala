package it.pagopa.interop.commons.utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class OpenapiUtilsSpec extends AnyWordSpecLike with Matchers {

  "Parsing a string representing a list of params" should {
    "retrieve an empty list if input string is []" in {
      val paramsTxt = "[]"
      val params    = OpenapiUtils.parseArrayParameters(paramsTxt)
      params shouldBe List.empty
    }

    "retrieve a non empty list if input string represent a comma separated list" in {
      val paramsTxt = "a,b,c"
      val params    = OpenapiUtils.parseArrayParameters(paramsTxt)
      params shouldBe List("a", "b", "c")
    }

  }

  "Verifying a list of params for a given condition" should {
    "retrieve true if the input list is empty" in {
      val params = List()
      val result = OpenapiUtils.verifyParametersByCondition(params)("condition")
      result shouldBe true
    }

    "retrieve true if the condition match" in {
      val params = List("a", "b", "c")
      val result = OpenapiUtils.verifyParametersByCondition(params)("a")
      result shouldBe true
    }

    "retrieve false if the condition does not match" in {
      val params = List("a", "b", "c")
      val result = OpenapiUtils.verifyParametersByCondition(params)("d")
      result shouldBe false
    }

  }

}
