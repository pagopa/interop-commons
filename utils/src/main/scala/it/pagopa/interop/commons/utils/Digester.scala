package it.pagopa.interop.commons.utils

import org.apache.commons.codec.digest.DigestUtils

import java.io.File
import java.nio.file.Files

trait Digester[A] {
  def toSha256(value: A): String
  def toMD5(value: A): String
}

object Digester {
  def toSha256[A: Digester](value: A): String = implicitly[Digester[A]].toSha256(value)

  def toMD5[A: Digester](value: A): String = implicitly[Digester[A]].toMD5(value)

  implicit val fileDigester: Digester[File] = new Digester[File] {
    override def toSha256(value: File): String = DigestUtils.sha256Hex(Files.readAllBytes(value.toPath))

    override def toMD5(value: File): String = DigestUtils.md5Hex(Files.readAllBytes(value.toPath))
  }

  implicit val bytesDigester: Digester[Array[Byte]] = new Digester[Array[Byte]] {
    override def toSha256(value: Array[Byte]): String = DigestUtils.sha256Hex(value)

    override def toMD5(value: Array[Byte]): String = DigestUtils.md5Hex(value)
  }
}
