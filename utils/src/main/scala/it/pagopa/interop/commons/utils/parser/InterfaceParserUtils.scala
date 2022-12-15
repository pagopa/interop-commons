package it.pagopa.interop.commons.utils.parser

import cats.implicits._
import io.circe.Json
import it.pagopa.interop.commons.utils.errors.Errors

import scala.xml.Elem

trait InterfaceExtractor[A] {
  def getUrls(serviceInterface: A): Either[Throwable, List[String]]
  def getEndpoints(serviceInterface: A): Either[Throwable, List[String]]
}

object InterfaceExtractor {

  def getUrls[A](serviceInterface: A)(implicit ie: InterfaceExtractor[A]): Either[Throwable, List[String]] =
    ie.getUrls(serviceInterface)

  def getEndpoints[A](serviceInterface: A)(implicit ie: InterfaceExtractor[A]): Either[Throwable, List[String]] =
    ie.getEndpoints(serviceInterface)

  implicit val openApiInterfaceExtractor: InterfaceExtractor[Json] = new InterfaceExtractor[Json] {
    override def getUrls(serviceInterface: Json): Either[Throwable, List[String]] =
      serviceInterface.hcursor.downField("servers").as[List[Json]].flatMap(_.traverse(_.hcursor.get[String]("url")))

    override def getEndpoints(serviceInterface: Json): Either[Throwable, List[String]] = {
      serviceInterface.hcursor.downField("paths").keys.toRight(Errors.InterfaceExtractingInfoError).map(_.toList)
    }
  }

  implicit val soapInterfaceExtractor: InterfaceExtractor[Elem] = new InterfaceExtractor[Elem] {
    override def getUrls(serviceInterface: Elem): Either[Throwable, List[String]] =
      (serviceInterface \\ "definitions" \ "service" \ "port" \ "address").toList.traverse(node =>
        node.attribute("location").map(_.text).toRight(Errors.InterfaceExtractingInfoError)
      )

    override def getEndpoints(xml: Elem): Either[Throwable, List[String]] =
      (xml \\ "definitions" \ "binding" \ "operation" \ "operation").toList
        .traverse(_.attribute("soapAction").map(_.text).toRight(Errors.InterfaceExtractingInfoError))

  }

}
