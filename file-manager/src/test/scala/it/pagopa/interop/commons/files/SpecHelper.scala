package it.pagopa.interop.commons.files

import com.typesafe.config.{Config, ConfigFactory}

trait SpecHelper {

  val testData = ConfigFactory.parseString(s"""
      get-url-duration-minutes = 5
      get-url-duration-minutes = 1
    """)

  val config: Config = ConfigFactory
    .parseResourcesAnySyntax("application-test")
    .withFallback(testData)
}
