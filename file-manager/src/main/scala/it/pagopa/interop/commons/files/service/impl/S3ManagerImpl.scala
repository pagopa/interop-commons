package it.pagopa.interop.commons.files.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.interop.commons.files.service.FileManager
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration

import cats.implicits._
import java.io.{ByteArrayOutputStream, File}
import java.util.UUID
import scala.concurrent.Future
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import scala.concurrent.ExecutionContext
import software.amazon.awssdk.core.async.AsyncRequestBody
import scala.jdk.FutureConverters._
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption
import it.pagopa.interop.commons.files.StorageConfiguration
import java.util.concurrent.Executor
import scala.concurrent.ExecutionContextExecutor
import software.amazon.awssdk.core.async.AsyncResponseTransformer

final class S3ManagerImpl(blockingExecutionContext: ExecutionContextExecutor) extends FileManager {

  private val logger: Logger                = LoggerFactory.getLogger(this.getClass)
  private implicit val ec: ExecutionContext = blockingExecutionContext

  private val serviceConf: S3Configuration        = S3Configuration.builder().pathStyleAccessEnabled(true).build()
  private val asyncHttpClient: SdkAsyncHttpClient =
    NettyNioAsyncHttpClient.builder().maxConcurrency(StorageConfiguration.maxConcurrency).build()
  private val asyncConfiguration: ClientAsyncConfiguration =
    ClientAsyncConfiguration
      .builder()
      .advancedOption[Executor](SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, blockingExecutionContext)
      .build()

  private val asyncClient: S3AsyncClient = S3AsyncClient
    .builder()
    .serviceConfiguration(serviceConf)
    .httpClient(asyncHttpClient)
    .asyncConfiguration(asyncConfiguration)
    .build()

  private def s3Key(path: String, resourceId: String, fileName: String): String =
    s"$path/$resourceId/$fileName"

  override def store(
    containerPath: String,
    path: String
  )(resourceId: UUID, fileParts: (FileInfo, File)): Future[StorageFilePath] = {
    val key: String                        = s3Key(path, resourceId.toString, fileParts._1.getFileName)
    logger.debug("Storing resource {} at path {}", resourceId.toString, key)
    val putObjectRequest: PutObjectRequest = PutObjectRequest.builder.bucket(containerPath).key(key).build
    val asyncRequestBody: AsyncRequestBody = AsyncRequestBody.fromFile(fileParts._2)
    asyncClient.putObject(putObjectRequest, asyncRequestBody).asScala.as(key)
  }

  override def storeBytes(
    containerPath: String,
    path: String
  )(resourceId: UUID, fileName: String, fileContents: Array[Byte]): Future[StorageFilePath] = {
    val key: String                        = s3Key(path, resourceId.toString, fileName)
    logger.debug("Storing resource {} at path {}", resourceId.toString, key)
    val putObjectRequest: PutObjectRequest = PutObjectRequest.builder.bucket(containerPath).key(key).build
    val asyncRequestBody: AsyncRequestBody = AsyncRequestBody.fromBytes(fileContents)
    asyncClient.putObject(putObjectRequest, asyncRequestBody).asScala.as(key)
  }

  override def copy(
    container: String,
    path: String
  )(filePathToCopy: String, resourceId: UUID, fileName: String): Future[StorageFilePath] = {
    logger.debug("Copying file {}", filePathToCopy)
    val key: String    = s3Key(path, resourceId.toString, fileName)
    val copyObjRequest = CopyObjectRequest.builder
      .destinationKey(key)
      .sourceKey(filePathToCopy)
      .sourceBucket(container)
      .destinationBucket(container)
      .build()

    asyncClient.copyObject(copyObjRequest).asScala.as(key)
  }

  override def get(containerPath: String)(filePath: String): Future[ByteArrayOutputStream] = {
    logger.debug("Getting file {} from container {}", filePath, containerPath)
    val getObjectRequest: GetObjectRequest = GetObjectRequest.builder.bucket(containerPath).key(filePath).build

    val s3Object: Future[ResponseBytes[GetObjectResponse]] =
      asyncClient.getObject(getObjectRequest, AsyncResponseTransformer.toBytes[GetObjectResponse]).asScala

    s3Object.map { response =>
      val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
      response.asInputStream().transferTo(stream)
      stream
    }
  }

  override def delete(containerPath: String)(path: String): Future[Boolean] = {
    logger.debug("Deleting file {} from container {}", path, containerPath)
    asyncClient
      .deleteObject(
        DeleteObjectRequest.builder
          .bucket(containerPath)
          .key(path)
          .build()
      )
      .asScala
      .map(_.deleteMarker().booleanValue())
  }

}
