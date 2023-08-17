package it.pagopa.interop.commons.utils

import akka.pattern.StatusReply
import org.apache.commons.text.StringSubstitutor

import java.nio.charset.StandardCharsets
import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.{Base64, UUID}
import scala.concurrent.{Future, ExecutionContext}
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.{Failure, Success, Try}

/** Defined implicit type conversions for common classes used through Interop platform
  */
object TypeConversions {

  implicit class EitherOps[A](val either: Either[Throwable, A]) extends AnyVal {
    def toFuture: Future[A] = either.fold(e => Future.failed(e), a => Future.successful(a))
  }

  implicit class TryOps[A](val tryOp: Try[A]) extends AnyVal {
    def toFuture: Future[A]                        = tryOp.fold(e => Future.failed(e), a => Future.successful(a))
    def leftMap(f: Throwable => Throwable): Try[A] = tryOp match {
      case Failure(e)     => Failure(f(e))
      case x @ Success(_) => x
    }
    def as(e: Throwable): Try[A]                   = tryOp.leftMap(_ => e)
  }

  implicit class OptionOps[A](val option: Option[A]) extends AnyVal {
    def toFuture(e: Throwable): Future[A] = option.fold[Future[A]](Future.failed(e))(Future.successful)
    def toTry(e: Throwable): Try[A]       = option.fold[Try[A]](Failure(e))(t => Success(t))
    def toTry(msg: String): Try[A]        =
      option.map(Success(_)).getOrElse(Failure(new NoSuchElementException(msg)))
  }

  implicit class OffsetDateTimeOps(val dt: OffsetDateTime) extends AnyVal {
    def toMillis: Long                 = dt.toInstant.toEpochMilli
    def asFormattedString: Try[String] = Try(dt.format(dateFormatter))
  }

  implicit class StringOps(val str: String) extends AnyVal {
    def toUUID: Try[UUID]                     = Try(UUID.fromString(str))
    def toOffsetDateTime: Try[OffsetDateTime] = Try(OffsetDateTime.parse(str, dateFormatter))
    def toFutureUUID: Future[UUID]            = str.toUUID.toFuture
    def parseCommaSeparated: List[String]     = str.split(",").map(_.trim).toList.filterNot(_ == "")
    def decodeBase64: Try[String]             = Try {
      val decoded: Array[Byte] = Base64.getDecoder.decode(str.getBytes(StandardCharsets.UTF_8))
      new String(decoded, StandardCharsets.UTF_8)
    }
    def encodeBase64: Try[String] = Try(Base64.getEncoder.encodeToString(str.getBytes(StandardCharsets.UTF_8)))
    def toBase64SHA1: String      = Base64.getEncoder.encodeToString(sha1.digest(str.getBytes(StandardCharsets.UTF_8)))
    def toBase64MD5: String       = Base64.getEncoder.encodeToString(md5.digest(str.getBytes(StandardCharsets.UTF_8)))

    /** Replaces string variables in the format <code>\${VARIABLE}</code>
      * with the corresponding VARIABLE value as defined in <code>variables</code> input map.<br>
      * E.g.: invoking the following:
      * <br>
      * <code>"hello \${there}".interpolate(Map("there" -> "friend"))</code>
      * <br>will return:<br>
      * <code>"hello friend"</code>
      *
      * @param variables map of variables to replace
      * @return string with variables replaced
      */
    def interpolate(variables: Map[String, String]): String = new StringSubstitutor(variables.asJava).replace(str)
  }

  implicit class StatusReplyOps[A](val statusReply: StatusReply[A]) extends AnyVal {
    def toEither: Either[Throwable, A] =
      if (statusReply.isSuccess) Right(statusReply.getValue) else Left(statusReply.getError)

    def toTry: Try[A] =
      if (statusReply.isSuccess) Success(statusReply.getValue) else Failure(statusReply.getError)
  }

  implicit class LongOps(val l: Long) extends AnyVal {
    def toOffsetDateTime: Try[OffsetDateTime] = toOffsetDateTime(ZoneOffset.UTC)
    def toEuropeRomeOffsetDateTime            = Try(Instant.ofEpochMilli(l).atZone(europeRome).toOffsetDateTime())
    def toOffsetDateTime(offset: ZoneOffset): Try[OffsetDateTime] = Try(
      OffsetDateTime.ofInstant(Instant.ofEpochMilli(l), offset)
    )
  }

  implicit class RichFutureObject(val obj: Future.type) extends AnyVal {
    def traverseWithLatch[T, V](
      n: Int
    )(list: List[T])(f: T => Future[V])(implicit ec: ExecutionContext): Future[List[V]] = {
      def go(list: List[List[T]])(acc: List[V]): Future[List[V]] = list match {
        case head :: next => Future.traverse(head)(f).flatMap(vs => go(next)(acc ++ vs))
        case Nil          => Future.successful(acc)
      }

      go(list.grouped(n).toList)(Nil)
    }

    def parTraverse[T, V](list: List[T])(f: T => Future[V])(implicit ec: ExecutionContext): Future[List[Option[V]]] =
      Future
        .traverse(list)(x =>
          f(x).transformWith {
            case Failure(_) => Future.successful(Option.empty[V])
            case Success(v) => Future.successful(Option(v))
          }
        )

    def parCollect[T, V](list: List[T])(f: T => Future[V])(implicit ec: ExecutionContext): Future[List[V]] =
      parTraverse(list)(f).map(_.collect { case Some(v) => v })

    def parTraverseWithLatch[T, V](
      n: Int
    )(list: List[T])(f: T => Future[V])(implicit ec: ExecutionContext): Future[List[Option[V]]] =
      traverseWithLatch(n)(list)(x =>
        f(x).transformWith {
          case Failure(_) => Future.successful(Option.empty[V])
          case Success(v) => Future.successful(Option(v))
        }
      )

    def parCollectWithLatch[T, V](n: Int)(list: List[T])(f: T => Future[V])(implicit
      ec: ExecutionContext
    ): Future[List[V]] = parTraverseWithLatch(n)(list)(f).map(_.collect { case Some(v) => v })

    def sequentially[T, V](list: List[T])(f: T => Future[V])(implicit ec: ExecutionContext): Future[List[V]] = {
      def go(innerList: List[T])(acc: List[V]): Future[List[V]] = innerList match {
        case head :: next => f(head).flatMap(v => go(next)(acc :+ v))
        case Nil          => Future.successful(acc)
      }
      go(list)(Nil)
    }

    def sequentiallyAccumulateLeft[T, V, R](
      parameters: List[T]
    )(thunk: T => Future[V])(zero: R)(f: (R, V) => R)(implicit ec: ExecutionContext): Future[R] = {

      def loop(remaining: List[T])(acc: R): Future[R] = remaining match {
        case Nil          => Future.successful(acc)
        case head :: next => thunk(head).map(f(acc, _)).flatMap(loop(next)(_))
      }

      loop(parameters)(zero)
    }

    /** Runs `thunk` over `parameters` with some `parallelism` 
      * (grouping parameters in chunks of length `parallelism` and then traversing over the list) 
      * and ONE CHUNK AT A TIME accumulates the result as a `foldLeft(zero)(f)`
      */
    def accumulateLeft[T, V, R](
      parallelism: Int
    )(parameters: List[T])(thunk: T => Future[V])(zero: R)(f: (R, V) => R)(implicit ec: ExecutionContext): Future[R] = {

      def loop(remaining: List[List[T]])(acc: R): Future[R] = remaining match {
        case Nil          => Future.successful(acc)
        case head :: next => Future.traverse(head)(thunk).map(_.foldLeft(acc)(f)).flatMap(loop(next)(_))
      }

      loop(parameters.grouped(parallelism).toList)(zero)
    }
  }

}
