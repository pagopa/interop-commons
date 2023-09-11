package it.pagopa.interop.commons.files.service.impl

import akka.http.scaladsl.server.directives.FileInfo
import cats.implicits._
import it.pagopa.interop.commons.files.StorageConfiguration
import it.pagopa.interop.commons.files.service.FileManager
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.{AsyncRequestBody, AsyncResponseTransformer}
import software.amazon.awssdk.core.client.config.{ClientAsyncConfiguration, SdkAdvancedAsyncClientOption}
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.services.s3.{S3AsyncClient, S3AsyncClientBuilder, S3Configuration}

import java.io.{ByteArrayOutputStream, File}
import java.nio.file.Files
import java.util.concurrent.Executor
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._

final class S3ManagerImpl(blockingExecutionContext: ExecutionContextExecutor)(
  confOverride: S3AsyncClientBuilder => S3AsyncClientBuilder = identity
) extends FileManager {

  private val logger: Logger                = LoggerFactory.getLogger(this.getClass)
  private implicit val ec: ExecutionContext = blockingExecutionContext

  private val serviceConf: S3Configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build()

  private val asyncHttpClient: SdkAsyncHttpClient =
    NettyNioAsyncHttpClient.builder().maxConcurrency(StorageConfiguration.maxConcurrency).build()

  private val asyncConfiguration: ClientAsyncConfiguration =
    ClientAsyncConfiguration
      .builder()
      .advancedOption[Executor](SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, blockingExecutionContext)
      .build()

  private val asyncClient: S3AsyncClient = confOverride(
    S3AsyncClient
      .builder()
      .serviceConfiguration(serviceConf)
      .httpClient(asyncHttpClient)
      .asyncConfiguration(asyncConfiguration)
  )
    .build()

  override def close(): Unit = {
    asyncHttpClient.close()
    asyncClient.close()
  }

  private def s3Key(path: String, resourceId: String, fileName: String): String =
    s"${path}/${resourceId}/${fileName}".replaceAll("//", "/").stripPrefix("/")

  override def store(
    containerPath: String,
    path: String
  )(resourceId: String, fileParts: (FileInfo, File)): Future[StorageFilePath] = {
    val key: String                        = s3Key(path, resourceId, fileParts._1.getFileName)
    logger.debug("Storing resource {} at path {}", resourceId, key)
    val putObjectRequest: PutObjectRequest =
      PutObjectRequest.builder.bucket(containerPath).key(key).contentMD5(calcContentMd5(fileParts._2)).build
    val asyncRequestBody: AsyncRequestBody = AsyncRequestBody.fromFile(fileParts._2)
    asyncClient.putObject(putObjectRequest, asyncRequestBody).asScala.as(key)
  }

  override def storeBytes(
    containerPath: String,
    path: String
  )(resourceId: String, fileName: String, fileContents: Array[Byte]): Future[StorageFilePath] = {
    val key: String                        = s3Key(path, resourceId, fileName)
    logger.debug("Storing resource {} at path {}", resourceId, key)
    val putObjectRequest: PutObjectRequest =
      PutObjectRequest.builder.bucket(containerPath).key(key).contentMD5(calcContentMd5(fileContents)).build
    val asyncRequestBody: AsyncRequestBody = AsyncRequestBody.fromBytes(fileContents)
    asyncClient.putObject(putObjectRequest, asyncRequestBody).asScala.as(key)
  }

  override def storeBytes(containerPath: String, path: String, fileName: String)(
    fileContents: Array[Byte]
  ): Future[StorageFilePath] = storeBytes(containerPath, path)("", fileName, fileContents)

  override def listFiles(container: String)(prefix: String): Future[List[StorageFilePath]] = {
    def reqBuilder: ListObjectsV2Request.Builder = {
      val partial = ListObjectsV2Request.builder().bucket(container)
      if (prefix.stripMargin('/').isBlank) partial else partial.prefix(prefix)
    }

    def list(request: ListObjectsV2Request): Future[ListObjectsV2Response] = asyncClient.listObjectsV2(request).asScala
    def extractKeys(response: ListObjectsV2Response): List[String] = response.contents().asScala.toList.map(_.key)

    def getAll(request: ListObjectsV2Request)(acc: List[String]): Future[List[String]] = list(request).flatMap {
      response =>
        val all: List[String] = extractKeys(response) ++ acc
        if (response.isTruncated()) getAll(reqBuilder.continuationToken(response.nextContinuationToken).build())(all)
        else Future.successful(all)
    }

    getAll(reqBuilder.build())(Nil)
  }

  override def getFile(container: String)(path: String): Future[Array[Byte]] = {
    val request: GetObjectRequest = GetObjectRequest.builder().bucket(container).key(path).build()
    asyncClient
      .getObject[ResponseBytes[GetObjectResponse]](request, AsyncResponseTransformer.toBytes[GetObjectResponse]())
      .asScala
      .map(_.asByteArray())
  }

  override def getAllFiles(container: String)(prefix: String): Future[Map[String, Array[Byte]]] =
    listFiles(container)(prefix)
      .flatMap(files => Future.traverse(files)(p => getFile(container)(p).map((p, _))).map(_.toMap))

  override def copy(
    container: String,
    path: String
  )(filePathToCopy: String, resourceId: String, fileName: String): Future[StorageFilePath] = {
    logger.debug("Copying file {}", filePathToCopy)
    val key: String    = s3Key(path, resourceId, fileName)
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

  def calcContentMd5(file: File): String = calcContentMd5(Files.readAllBytes(file.toPath))

  def calcContentMd5(byteArray: Array[Byte]): String = new String(Base64.encodeBase64(DigestUtils.md5(byteArray)))
}
