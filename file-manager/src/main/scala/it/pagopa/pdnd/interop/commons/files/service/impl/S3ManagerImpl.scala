package it.pagopa.pdnd.interop.commons.files.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.pdnd.interop.commons.files.StorageConfiguration.storageAccountInfo
import it.pagopa.pdnd.interop.commons.files.service.{FileManager, StorageFilePath}
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.sync.{RequestBody, ResponseTransformer}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}

import java.io.{ByteArrayOutputStream, File, InputStream}
import java.nio.file.Paths
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

final class S3ManagerImpl extends FileManager {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  lazy val s3Client: S3Client = {
    val awsCredentials =
      AwsBasicCredentials.create(storageAccountInfo.applicationId, storageAccountInfo.applicationSecret)
    val s3 = S3Client
      .builder()
      .region(Region.EU_CENTRAL_1)
      .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
      .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
      .build()
    s3
  }

  override def store(containerPath: String)(id: UUID, fileParts: (FileInfo, File)): Future[StorageFilePath] =
    Future.fromTry {

      Try {
        val s3Key = createS3Key(
          id.toString,
          contentType = fileParts._1.getContentType.toString(),
          fileName = fileParts._1.getFileName
        )
        logger.debug("Storing file id {} at path {}", id.toString, s3Key)
        val objectRequest =
          PutObjectRequest.builder
            .bucket(containerPath)
            .key(s3Key)
            .build

        val _ = s3Client.putObject(objectRequest, RequestBody.fromFile(Paths.get(fileParts._2.getPath)))
        logger.debug("File {} stored", id.toString)
        s3Key
      }
    }

  override def copy(
    container: String
  )(filePathToCopy: String, locationId: UUID, contentType: String, fileName: String): Future[StorageFilePath] =
    Future.fromTry {
      logger.debug("Copying file {}", filePathToCopy)
      Try {
        val destinationS3Key =
          createS3Key(locationId.toString, contentType = contentType, fileName = fileName)

        val copyObjRequest = CopyObjectRequest.builder
          .destinationKey(destinationS3Key)
          .sourceKey(filePathToCopy)
          .sourceBucket(container)
          .destinationBucket(container)
          .build

        val _ = s3Client.copyObject(copyObjRequest)
        logger.debug("File {} copied", filePathToCopy)

        destinationS3Key
      }
    }

  private def createS3Key(tokenId: String, contentType: String, fileName: String): String =
    s"parties/docs/$tokenId/${contentType}/$fileName"

  override def get(containerPath: String)(filePath: String): Future[ByteArrayOutputStream] = Future.fromTry {
    Try {
      logger.debug("Getting file {} from container {}", filePath, containerPath)
      val getObjectRequest: GetObjectRequest =
        GetObjectRequest.builder.bucket(containerPath).key(filePath).build
      val s3Object: ResponseBytes[GetObjectResponse] = s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes)
      val inputStream: InputStream                   = s3Object.asInputStream()
      val outputStream: ByteArrayOutputStream        = new ByteArrayOutputStream()
      val _                                          = inputStream.transferTo(outputStream)
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
