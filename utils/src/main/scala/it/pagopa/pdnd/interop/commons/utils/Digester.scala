package it.pagopa.pdnd.interop.commons.utils

import java.io.File
import java.nio.file.Files
import java.security.{DigestInputStream, MessageDigest}
import scala.annotation.tailrec

/** Returns file hash codes
  */
object Digester {

  /** Returns an hash code digest of the file for the specified algorithm
    * @param file
    * @param algorithm
    * @return
    */
  def createHash(file: File, algorithm: String): String = {
    val md  = MessageDigest.getInstance(algorithm)
    val dis = new DigestInputStream(Files.newInputStream(file.toPath), md)
    loop(dis.available > 0, { val _ = dis.read }, { dis.close() })
    md.digest.map(b => String.format("%02x", Byte.box(b))).mkString
  }

  /** Returns the MD5 hash code digest of the file
    * @param file
    * @return
    */
  def createMD5Hash(file: File): String = {
    createHash(file, "MD5")
  }

  @tailrec
  private def loop(cond: => Boolean, block: => Unit, closing: => Unit): Unit =
    if (cond) {
      block
      loop(cond, block, closing)
    } else {
      closing
    }

}
