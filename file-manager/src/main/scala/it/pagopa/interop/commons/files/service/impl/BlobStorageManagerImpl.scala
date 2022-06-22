package it.pagopa.interop.commons.files.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import com.azure.storage.blob.specialized.BlockBlobClient
import com.azure.storage.blob.{BlobClient, BlobServiceClient, BlobServiceClientBuilder}
import it.pagopa.interop.commons.files.StorageConfiguration.storageAccountInfo
import it.pagopa.interop.commons.files.service.FileManager
import org.slf4j.{Logger, LoggerFactory}

import java.io.{ByteArrayOutputStream, File}
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContextExecutor
import com.azure.core.util.BinaryData

final class BlobStorageManagerImpl(blockingExecutionContext: ExecutionContextExecutor) extends FileManager {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  lazy val azureBlobClient: BlobServiceClient = {
    val accountName: String              = storageAccountInfo.applicationId
    val accountKey: String               = storageAccountInfo.applicationSecret
    val endpointSuffix: String           = storageAccountInfo.endpoint
    val connectionString                 =
      s"DefaultEndpointsProtocol=https;AccountName=$accountName;AccountKey=$accountKey;EndpointSuffix=$endpointSuffix"
    val storageClient: BlobServiceClient =
      new BlobServiceClientBuilder().connectionString(connectionString).buildClient()
    storageClient
  }

  override def store(
    containerPath: String,
    path: String
  )(resourceId: UUID, fileParts: (FileInfo, File)): Future[StorageFilePath] = Future {
    val blobKey                = createBlobKey(resourceId.toString, path = path, fileName = fileParts._1.getFileName)
    logger.debug("Storing file id {} at path {}", resourceId.toString, blobKey)
    val blobContainerClient    = azureBlobClient.getBlobContainerClient(containerPath)
    val blobClient: BlobClient = blobContainerClient.getBlobClient(blobKey)
    blobClient.uploadFromFile(fileParts._2.getPath)
    logger.debug("File {} stored", resourceId.toString)
    blobKey
  }(blockingExecutionContext)

  override def storeBytes(
    containerPath: String,
    path: String
  )(resourceId: UUID, fileName: String, fileContents: Array[Byte]): Future[StorageFilePath] = Future {
    val blobKey                = createBlobKey(resourceId.toString, path = path, fileName = fileName)
    logger.debug("Storing file id {} at path {}", resourceId.toString, blobKey)
    val blobContainerClient    = azureBlobClient.getBlobContainerClient(containerPath)
    val blobClient: BlobClient = blobContainerClient.getBlobClient(blobKey)
    blobClient.upload(BinaryData.fromBytes(fileContents))
    logger.debug("File {} stored", resourceId.toString)
    blobKey
  }(blockingExecutionContext)

  override def copy(
    containerPath: String,
    path: String
  )(filePathToCopy: String, resourceId: UUID, fileName: String): Future[StorageFilePath] = Future {
    logger.debug("Copying file {}", filePathToCopy)
    val destination            = createBlobKey(resourceId.toString, path = path, fileName = fileName)
    val blobContainerClient    = azureBlobClient.getBlobContainerClient(containerPath)
    val blobClient: BlobClient = blobContainerClient.getBlobClient(destination)
    blobClient.copyFromUrl(filePathToCopy)
    logger.debug("File {} copied", filePathToCopy)
    destination
  }(blockingExecutionContext)

  override def get(containerPath: String)(filePath: String): Future[ByteArrayOutputStream] = Future {
    logger.debug("Getting file {} from container {}", filePath, containerPath)
    val blobContainerClient                 = azureBlobClient.getBlobContainerClient(containerPath)
    val blobClient: BlockBlobClient         = blobContainerClient.getBlobClient(filePath).getBlockBlobClient
    val dataSize: Int                       = blobClient.getProperties.getBlobSize.toInt
    val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream(dataSize)
    blobClient.download(outputStream)
    outputStream
  }(blockingExecutionContext)

  override def delete(containerPath: String)(filePath: String): Future[Boolean] = Future {
    logger.debug("Deleting file {} from container {}", filePath, containerPath)
    val blobContainerClient         = azureBlobClient.getBlobContainerClient(containerPath)
    val blobClient: BlockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient
    blobClient.delete()
    true
  }(blockingExecutionContext)

  private def createBlobKey(resourceId: String, path: String, fileName: String): String =
    s"$path/$resourceId/$fileName"

}
