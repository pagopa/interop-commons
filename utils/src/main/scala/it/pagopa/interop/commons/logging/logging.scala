package it.pagopa.interop.commons

import com.typesafe.scalalogging.CanLog
import it.pagopa.interop.commons.utils.{CORRELATION_ID_HEADER, SUB, UID}

package object logging {
  type ContextFieldsToLog = Seq[(String, String)]

  /** Defines log message decoration for Interop
    */
  implicit case object CanLogContextFields extends CanLog[ContextFieldsToLog] {
    @inline private def optToLog(opt: Option[String]): String = opt.getOrElse("")

    override def logMessage(originalMsg: String, fields: ContextFieldsToLog): String = {
      val fieldsMap = fields.toMap
      s"[${optToLog(fieldsMap.get(UID).orElse(fieldsMap.get(SUB)))}] [${optToLog(fieldsMap.get(CORRELATION_ID_HEADER))}] - $originalMsg"
    }
  }
}
