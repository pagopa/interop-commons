package it.pagopa.interop.commons.jwt.service.impl

import it.pagopa.interop.commons.jwt.errors.DigestClaimNotFound
import it.pagopa.interop.commons.utils.TypeConversions.OptionOps

import java.util
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.Try

final case class Digest(alg: String, value: String) {
  import Digest.{algClaim, valueClaim}
  def toJavaMap: util.Map[String, String] = Map(algClaim -> alg, valueClaim -> value).asJava
}

object Digest {
  final val algClaim   = "alg"
  final val valueClaim = "value"

  def create(rawDigest: Map[String, AnyRef]): Try[Digest] = for {
    alg   <- rawDigest.get(algClaim).map(_.toString).toTry(DigestClaimNotFound(algClaim))
    value <- rawDigest.get(valueClaim).map(_.toString).toTry(DigestClaimNotFound(valueClaim))
  } yield Digest(alg, value)
}
