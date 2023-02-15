package it.pagopa.interop.commons.files.service

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.interop.commons.files.service.impl.{FileManagerImpl, S3ManagerImpl}

import java.io.{ByteArrayOutputStream, File}
import scala.concurrent.{ExecutionContextExecutor, Future}

trait FileManager {

  type StorageFilePath = String

  def store(containerPath: String, path: String)(
    resourceId: String,
    fileParts: (FileInfo, File)
  ): Future[StorageFilePath]

  def storeBytes(
    containerPath: String,
    path: String
  )(resourceId: String, fileName: String, fileContent: Array[Byte]): Future[StorageFilePath]

  def storeBytes(containerPath: String, path: String, filename: String)(
    fileContent: Array[Byte]
  ): Future[StorageFilePath]

  def listFiles(container: String)(path: String): Future[List[StorageFilePath]]

  def getFile(container: String)(path: String): Future[Array[Byte]]

  def getAllFiles(container: String)(path: String): Future[Map[String, Array[Byte]]]

  def copy(
    containerPath: String,
    path: String
  )(filePathToCopy: String, resourceId: String, fileName: String): Future[StorageFilePath]

  def get(containerPath: String)(filePath: StorageFilePath): Future[ByteArrayOutputStream]

  def delete(containerPath: String)(filePath: StorageFilePath): Future[Boolean]

  def close(): Unit
}

object FileManager {

  sealed trait Kind
  final object File extends Kind
  final object S3   extends Kind

  def get(kind: FileManager.Kind)(blockingExecutionContext: ExecutionContextExecutor): FileManager = kind match {
    case File => new FileManagerImpl(blockingExecutionContext)
    case S3   => new S3ManagerImpl(blockingExecutionContext)(identity)
  }
}
