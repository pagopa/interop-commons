package it.pagopa.interop.commons.files.service

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.interop.commons.files.service.impl.{FileManagerImpl, S3ManagerImpl}

import java.io.{ByteArrayOutputStream, File}
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor

trait FileManager {

  type StorageFilePath = String

  def store(containerPath: String, path: String)(resourceId: UUID, fileParts: (FileInfo, File)): Future[StorageFilePath]

  def storeBytes(
    containerPath: String,
    path: String
  )(resourceId: UUID, fileName: String, fileContent: Array[Byte]): Future[StorageFilePath]

  def copy(
    containerPath: String,
    path: String
  )(filePathToCopy: String, resourceId: UUID, fileName: String): Future[StorageFilePath]

  def get(containerPath: String)(filePath: StorageFilePath): Future[ByteArrayOutputStream]

  def delete(containerPath: String)(filePath: StorageFilePath): Future[Boolean]
}

object FileManager {

  sealed trait Kind
  final object File extends Kind
  final object S3   extends Kind

  def get(kind: FileManager.Kind)(blockingExecutionContext: ExecutionContextExecutor): FileManager = kind match {
    case File => new FileManagerImpl(blockingExecutionContext)
    case S3   => new S3ManagerImpl(blockingExecutionContext)
  }
}
