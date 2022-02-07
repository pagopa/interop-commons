package it.pagopa.pdnd.interop.commons.files.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import com.azure.storage.blob.specialized.BlockBlobClient
import com.azure.storage.blob.{BlobClient, BlobServiceClient, BlobServiceClientBuilder}
import it.pagopa.pdnd.interop.commons.files.StorageConfiguration.storageAccountInfo
import it.pagopa.pdnd.interop.commons.files.service.{FileManager, StorageFilePath}
import org.slf4j.{Logger, LoggerFactory}

import java.io.{ByteArrayOutputStream, File}
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

final class BlobStorageManagerImpl extends FileManager {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  lazy val azureBlobClient: BlobServiceClient = {
    val accountName: String    = storageAccountInfo.applicationId
    val accountKey: String     = storageAccountInfo.applicationSecret
    val endpointSuffix: String = storageAccountInfo.endpoint
    val connectionString =
      s"DefaultEndpointsProtocol=https;AccountName=$accountName;AccountKey=$accountKey;EndpointSuffix=$endpointSuffix"
    val storageClient: BlobServiceClient =
      new BlobServiceClientBuilder().connectionString(connectionString).buildClient()
    storageClient
  }

  override def store(containerPath: String)(resourceId: UUID, fileParts: (FileInfo, File)): Future[StorageFilePath] =
    Future.fromTry {
      Try {
        val blobKey = createBlobKey(resourceId.toString, fileName = fileParts._1.getFileName)
        logger.debug("Storing file id {} at path {}", resourceId.toString, blobKey)
        val blobContainerClient    = azureBlobClient.getBlobContainerClient(containerPath)
        val blobClient: BlobClient = blobContainerClient.getBlobClient(blobKey)
        blobClient.uploadFromFile(fileParts._2.getPath)
        logger.debug("File {} stored", resourceId.toString)
        blobKey
      }
    }

  override def copy(
    containerPath: String
  )(filePathToCopy: String, resourceId: UUID, fileName: String): Future[StorageFilePath] = {
    Future.fromTry {
      Try {
        logger.debug("Copying file {}", filePathToCopy)
        val destination            = createBlobKey(resourceId.toString, fileName = fileName)
        val blobContainerClient    = azureBlobClient.getBlobContainerClient(containerPath)
        val blobClient: BlobClient = blobContainerClient.getBlobClient(destination)
        blobClient.copyFromUrl(filePathToCopy)
        logger.debug("File {} copied", filePathToCopy)
        destination
      }
    }
  }

  override def get(containerPath: String)(filePath: String): Future[ByteArrayOutputStream] = Future.fromTry {
    Try {
      logger.debug("Getting file {} from container {}", filePath, containerPath)
      val blobContainerClient                 = azureBlobClient.getBlobContainerClient(containerPath)
      val blobClient: BlockBlobClient         = blobContainerClient.getBlobClient(filePath).getBlockBlobClient
      val dataSize: Int                       = blobClient.getProperties.getBlobSize.toInt
      val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream(dataSize)
      blobClient.download(outputStream)
      outputStream
    }
  }

  override def delete(containerPath: String)(filePath: String): Future[Boolean] = {
    Try {
      logger.debug("Deleting file {} from container {}", filePath, containerPath)
      val blobContainerClient         = azureBlobClient.getBlobContainerClient(containerPath)
      val blobClient: BlockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient
      blobClient.delete()
    }.fold(error => Future.failed[Boolean](error), _ => Future.successful(true))
  }

  private def createBlobKey(resourceId: String, fileName: String): String =
    s"${storageAccountInfo.path}/$resourceId/$fileName"

}
