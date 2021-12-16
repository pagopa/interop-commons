package it.pagopa.pdnd.interop.commons

import com.typesafe.scalalogging.CanLog

package object logging {

  /** Defines log message decoration for PDND
    */
  implicit case object CanLogContextFields extends CanLog[ContextFieldsToLog] {
    @inline private def optToLog(opt: Option[String]): String = opt.getOrElse("")

    override def logMessage(originalMsg: String, fields: ContextFieldsToLog): String = {
      s"[${optToLog(fields.userId)}] [${optToLog(fields.correlationId)}] - $originalMsg"
    }
  }
}
