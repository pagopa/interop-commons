package it.pagopa.interop.commons.parser

import cats.syntax.all._
import io.circe.{DecodingFailure, Json}
import it.pagopa.interop.commons.parser.errors.Errors
import it.pagopa.interop.commons.parser.errors.Errors.OpenapiVersionNotRecognized

import scala.xml.Elem

trait InterfaceParserUtils[A] {
  def getUrls(serviceInterface: A): Either[Throwable, List[String]]
  def getEndpoints(serviceInterface: A): Either[Throwable, List[String]]
}

object InterfaceParserUtils {

  private final val `3.1.0` = "3.1.0"
  private final val `3.0.3` = "3.0.3"
  private final val `3.0.2` = "3.0.2"
  private final val `3.0.1` = "3.0.1"
  private final val `3.0.0` = "3.0.0"
  private final val `2.0`   = "2.0"

  implicit class JsonOps(val serviceInterface: Json) extends AnyVal {
    def getVersion: Either[DecodingFailure, String] = openapiVersion orElse swaggerVersion

    private def openapiVersion: Either[DecodingFailure, String] =
      serviceInterface.hcursor.downField("openapi").as[String]

    private def swaggerVersion: Either[DecodingFailure, String] =
      serviceInterface.hcursor.downField("swagger").as[String]
  }

  def getUrls[A: InterfaceParserUtils](serviceInterface: A): Either[Throwable, List[String]] =
    implicitly[InterfaceParserUtils[A]].getUrls(serviceInterface)

  def getEndpoints[A: InterfaceParserUtils](serviceInterface: A): Either[Throwable, List[String]] =
    implicitly[InterfaceParserUtils[A]].getEndpoints(serviceInterface)

  implicit val openApiInterfaceExtractor: InterfaceParserUtils[Json] = new InterfaceParserUtils[Json] {
    override def getUrls(serviceInterface: Json): Either[Throwable, List[String]] =
      serviceInterface.getVersion.flatMap {
        case `2.0` => serviceInterface.hcursor.downField("host").as[String].map(List(_))
        case `3.0.0` | `3.0.1` | `3.0.2` | `3.0.3` | `3.1.0` =>
          serviceInterface.hcursor.downField("servers").as[List[Json]].flatMap(_.traverse(_.hcursor.get[String]("url")))
        case unknownVersion                                  => Left(OpenapiVersionNotRecognized(unknownVersion))
      }

    override def getEndpoints(serviceInterface: Json): Either[Throwable, List[String]] =
      serviceInterface.getVersion.flatMap {
        case `2.0`                                           =>
          serviceInterface.hcursor.downField("paths").keys.toRight(Errors.InterfaceExtractingInfoError).map(_.toList)
        case `3.0.0` | `3.0.1` | `3.0.2` | `3.0.3` | `3.1.0` =>
          serviceInterface.hcursor.downField("paths").keys.toRight(Errors.InterfaceExtractingInfoError).map(_.toList)
        case unknownVersion                                  => Left(OpenapiVersionNotRecognized(unknownVersion))
      }
  }

  implicit val soapInterfaceExtractor: InterfaceParserUtils[Elem] = new InterfaceParserUtils[Elem] {
    override def getUrls(serviceInterface: Elem): Either[Throwable, List[String]] =
      (serviceInterface \\ "definitions" \ "service" \ "port" \ "address").toList
        .asRight[Throwable]
        .ensure(Errors.InterfaceExtractingInfoError)(_.nonEmpty)
        .flatMap(
          _.traverse(node => node.attribute("location").map(_.text).toRight(Errors.InterfaceExtractingInfoError))
        )

    override def getEndpoints(xml: Elem): Either[Throwable, List[String]] =
      (xml \\ "definitions" \ "binding" \ "operation" \ "operation").toList
        .asRight[Throwable]
        .ensure(Errors.InterfaceExtractingInfoError)(_.nonEmpty)
        .flatMap(_.traverse(_.attribute("soapAction").map(_.text).toRight(Errors.InterfaceExtractingInfoError)))
  }

}
