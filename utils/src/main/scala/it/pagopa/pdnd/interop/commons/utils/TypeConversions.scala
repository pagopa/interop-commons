package it.pagopa.pdnd.interop.commons.utils

import akka.pattern.StatusReply

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.{Base64, UUID}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/** Defined implicit type conversions for common classes used through PDND interop platform
  */
object TypeConversions {

  implicit class EitherOps[A](val either: Either[Throwable, A]) extends AnyVal {
    def toFuture: Future[A] = either.fold(e => Future.failed(e), a => Future.successful(a))
  }

  implicit class TryOps[A](val tryOp: Try[A]) extends AnyVal {
    def toFuture: Future[A] = tryOp.fold(e => Future.failed(e), a => Future.successful(a))
  }

  implicit class OptionOps[A](val option: Option[A]) extends AnyVal {
    def toFuture(e: Throwable): Future[A] = option.fold[Future[A]](Future.failed(e))(Future.successful)
  }

  implicit class OffsetDateTimeOps(val dt: OffsetDateTime) extends AnyVal {
    def asFormattedString: Try[String] = Try {
      dt.format(dateFormatter)
    }
  }

  implicit class StringOps(val str: String) extends AnyVal {
    def toUUID: Try[UUID] = Try {
      UUID.fromString(str)
    }
    def toOffsetDateTime: Try[OffsetDateTime] = Try { OffsetDateTime.parse(str, dateFormatter) }
    def toFutureUUID: Future[UUID]            = str.toUUID.toFuture
    def parseCommaSeparated: List[String]     = str.split(",").map(_.trim).toList.filterNot(entry => entry == "")
    def decodeBase64: Try[String] = Try {
      val decoded: Array[Byte] = Base64.getDecoder.decode(str.getBytes(StandardCharsets.UTF_8))
      new String(decoded, StandardCharsets.UTF_8)
    }

    def toBase64SHA1: String = Base64.getEncoder.encodeToString(sha1.digest(str.getBytes(StandardCharsets.UTF_8)))
    def toBase64MD5: String  = Base64.getEncoder.encodeToString(md5.digest(str.getBytes(StandardCharsets.UTF_8)))
  }

  implicit class StatusReplyOps[A](val statusReply: StatusReply[A]) extends AnyVal {
    def toEither: Either[Throwable, A] =
      if (statusReply.isSuccess) Right(statusReply.getValue) else Left(statusReply.getError)

    def toTry: Try[A] =
      if (statusReply.isSuccess) Success(statusReply.getValue) else Failure(statusReply.getError)
  }

}
