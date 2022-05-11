package it.pagopa.interop.commons.files.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.interop.commons.files.service.{FileManager, StorageFilePath}
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.sync.{RequestBody, ResponseTransformer}
import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}

import java.io.{ByteArrayOutputStream, File, InputStream}
import java.nio.file.Paths
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

final class S3ManagerImpl extends FileManager {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  lazy val s3Client: S3Client =
    S3Client
      .builder()
      .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
      .build()

  override def store(
    containerPath: String,
    path: String
  )(resourceId: UUID, fileParts: (FileInfo, File)): Future[StorageFilePath] =
    Future.fromTry {

      Try {
        val s3Key         = createS3Key(resourceId.toString, path = path, fileName = fileParts._1.getFileName)
        logger.debug("Storing file id {} at path {}", resourceId.toString, s3Key)
        val objectRequest =
          PutObjectRequest.builder
            .bucket(containerPath)
            .key(s3Key)
            .build

        s3Client.putObject(objectRequest, RequestBody.fromFile(Paths.get(fileParts._2.getPath)))
        logger.debug("File {} stored", resourceId.toString)
        s3Key
      }
    }

  override def copy(
    container: String,
    path: String
  )(filePathToCopy: String, resourceId: UUID, fileName: String): Future[StorageFilePath] =
    Future.fromTry {
      logger.debug("Copying file {}", filePathToCopy)
      Try {
        val destinationS3Key = createS3Key(resourceId.toString, path = path, fileName = fileName)

        val copyObjRequest = CopyObjectRequest.builder
          .destinationKey(destinationS3Key)
          .sourceKey(filePathToCopy)
          .sourceBucket(container)
          .destinationBucket(container)
          .build

        s3Client.copyObject(copyObjRequest)
        logger.debug("File {} copied", filePathToCopy)

        destinationS3Key
      }
    }

  private def createS3Key(resourceId: String, path: String, fileName: String): String =
    s"$path/$resourceId/$fileName"

  override def get(containerPath: String)(filePath: String): Future[ByteArrayOutputStream] = Future.fromTry {
    Try {
      logger.debug("Getting file {} from container {}", filePath, containerPath)
      val getObjectRequest: GetObjectRequest         =
        GetObjectRequest.builder.bucket(containerPath).key(filePath).build
      val s3Object: ResponseBytes[GetObjectResponse] = s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes)
      val inputStream: InputStream                   = s3Object.asInputStream()
      val outputStream: ByteArrayOutputStream        = new ByteArrayOutputStream()

      inputStream.transferTo(outputStream)
      outputStream
    }
  }

  override def delete(containerPath: String)(path: String): Future[Boolean] = {
    Try {
      logger.debug("Deleting file {} from container {}", path, containerPath)
      s3Client.deleteObject(
        DeleteObjectRequest.builder
          .bucket(containerPath)
          .key(path)
          .build()
      )
    }.fold(error => Future.failed[Boolean](error), _ => Future.successful(true))
  }

}
