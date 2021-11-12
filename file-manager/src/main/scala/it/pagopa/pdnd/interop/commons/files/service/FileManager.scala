package it.pagopa.pdnd.interop.commons.files.service

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.commons.files.service.impl.{BlobStorageManagerImpl, FileManagerImpl, S3ManagerImpl}

import java.io.{ByteArrayOutputStream, File}
import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Try}

/** Defines common operations for file management
  */
trait FileManager {

  /** Stores the specified file in a unique location
    * @param tokenId the unique identifier of the location
    * @param fileParts file contents and its information as retrieved from an Akka HTTP call
    * @return the path where the file has been stored
    */
  def store(tokenId: UUID, fileParts: (FileInfo, File)): Future[StorageFilePath]

  /** Returns the stream of the file located at the specified path
    * @param filePath the path of the file to retrieve
    * @return <code>java.io.ByteArrayOutputStream</code> of the file
    */
  def get(filePath: StorageFilePath): Future[ByteArrayOutputStream]

  /** Deletes the file located at the specified path
    * @param filePath the of the file to delete
    * @return true if the deletion happens properly, false otherwise.
    */
  def delete(filePath: StorageFilePath): Future[Boolean]
}

object FileManager {

  /** Returns an instance of the specific [[FileManager]]. Currently, the admittable implementations are:
    *
    * <ul>
    *     <li><code>File</code> - in memory implementation</li>
    *     <li><code>BlobStorage</code> - Azure implementation</li>
    *     <li><code>S3</code> - S3 implementation</li>
    * </ul>
    * <br>
    * @param fileManager the identifier of the specific instance
    * @return the specific instance
    */
  def getConcreteImplementation(fileManager: String): Try[FileManager] = {
    fileManager match {
      case "File"           => Try { new FileManagerImpl() }
      case "BlobStorage"    => Try { new BlobStorageManagerImpl() }
      case "S3"             => Try { new S3ManagerImpl() }
      case wrongManager @ _ => Failure(new RuntimeException(s"Unsupported file manager: $wrongManager"))
    }
  }
}
