package it.pagopa.pdnd.interop.commons.files.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import com.azure.storage.blob.specialized.BlockBlobClient
import com.azure.storage.blob.{BlobClient, BlobServiceClient, BlobServiceClientBuilder}
import it.pagopa.pdnd.interop.commons.files.StorageConfiguration.storageAccountInfo
import it.pagopa.pdnd.interop.commons.files.service.{FileManager, StorageFilePath}

import java.io.{ByteArrayOutputStream, File}
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

final class BlobStorageManagerImpl extends FileManager {

  lazy val azureBlobClient = {
    val accountName: String    = storageAccountInfo.applicationId
    val accountKey: String     = storageAccountInfo.applicationSecret
    val endpointSuffix: String = storageAccountInfo.endpoint
    val connectionString =
      s"DefaultEndpointsProtocol=https;AccountName=$accountName;AccountKey=$accountKey;EndpointSuffix=$endpointSuffix"
    val storageClient: BlobServiceClient =
      new BlobServiceClientBuilder().connectionString(connectionString).buildClient()
    storageClient
  }

  override def store(id: UUID, fileParts: (FileInfo, File)): Future[StorageFilePath] = Future.fromTry {
    Try {
      val blobKey = createBlobKey(
        id.toString,
        contentType = fileParts._1.getContentType.toString(),
        fileName = fileParts._1.getFileName
      )

      val blobContainerClient    = azureBlobClient.getBlobContainerClient(storageAccountInfo.container)
      val blobClient: BlobClient = blobContainerClient.getBlobClient(blobKey)
      blobClient.uploadFromFile(fileParts._2.getPath)

      blobKey
    }
  }

  override def copy(
    filePathToCopy: String
  )(locationId: String, contentType: String, fileName: String): Future[StorageFilePath] = {
    Future.fromTry {
      Try {
        val destination            = createBlobKey(locationId, contentType = contentType, fileName = fileName)
        val blobContainerClient    = azureBlobClient.getBlobContainerClient(storageAccountInfo.container)
        val blobClient: BlobClient = blobContainerClient.getBlobClient(destination)
        blobClient.copyFromUrl(filePathToCopy)
        destination
      }
    }
  }

  private def createBlobKey(tokenId: String, contentType: String, fileName: String): String =
    s"parties/docs/$tokenId/${contentType}/$fileName"

  override def get(filePath: String): Future[ByteArrayOutputStream] = Future.fromTry {
    Try {
      val blobContainerClient                 = azureBlobClient.getBlobContainerClient(storageAccountInfo.container)
      val blobClient: BlockBlobClient         = blobContainerClient.getBlobClient(filePath).getBlockBlobClient
      val dataSize: Int                       = blobClient.getProperties.getBlobSize.toInt
      val outputStream: ByteArrayOutputStream = new ByteArrayOutputStream(dataSize)
      val _                                   = blobClient.downloadStream(outputStream)
      outputStream
    }
  }

  override def delete(filePath: String): Future[Boolean] = {
    Try {
      val blobContainerClient         = azureBlobClient.getBlobContainerClient(storageAccountInfo.container)
      val blobClient: BlockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient
      blobClient.delete
    }.fold(error => Future.failed[Boolean](error), _ => Future.successful(true))
  }

}
