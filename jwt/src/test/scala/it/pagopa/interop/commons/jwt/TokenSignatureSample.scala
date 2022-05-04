package it.pagopa.interop.commons.jwt

import akka.actor.ActorSystem
import it.pagopa.interop.commons.jwt.model.EC
import it.pagopa.interop.commons.jwt.service.impl.DefaultSessionTokenGenerator
import it.pagopa.interop.commons.vault.VaultConfig
import it.pagopa.interop.commons.vault.service.impl.VaultTransitServiceImpl

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

//main class for testing Vault transit API
object TokenSignatureSample extends App {

  implicit val system = ActorSystem("HelloSystem")
  implicit val ec     = system.getDispatcher

  val vc =
    VaultConfig("https://gateway-private.dev.pdnd-interop.pagopa.it", token = "YADAYADA", true, "/v1/transit/sign/")

  val impl = new VaultTransitServiceImpl(vc)

  val privateKeysHolder = new PrivateKeysKidHolder {
    override val ECPrivateKeyset: Set[KID]  = Set("test-ec256")
    override val RSAPrivateKeyset: Set[KID] = Set("test-sha")
  }

  val generator = new DefaultSessionTokenGenerator(impl, privateKeysHolder)

  val generation = generator.generate(
    jwtAlgorithmType = EC,
    claimsSet = Map.empty,
    audience = Set("pippo"),
    tokenIssuer = "me",
    validityDurationInSeconds = 3600
  )

  println(Await.result(generation, 10 seconds))
}
