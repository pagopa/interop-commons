package it.pagopa.interop.commons.signer.service.impl

import it.pagopa.interop.commons.signer.model.SignatureAlgorithm
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.ThirdPartyCallError
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.KmsAsyncClient
import software.amazon.awssdk.services.kms.model.{SignRequest, SigningAlgorithmSpec}

import java.util.Base64
import scala.jdk.FutureConverters._
import scala.concurrent.Future
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration
import java.util.concurrent.Executor
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption
import scala.concurrent.ExecutionContextExecutor
import it.pagopa.interop.commons.signer.SignerConfiguration
import scala.concurrent.ExecutionContext

final class KMSSignerService(blockingEc: ExecutionContextExecutor) extends SignerService {

  implicit val ec: ExecutionContext = blockingEc

  private val asyncHttpClient: SdkAsyncHttpClient          =
    NettyNioAsyncHttpClient
      .builder()
      .connectionAcquisitionTimeout(SignerConfiguration.maxAcquisitionTimeout)
      .maxConcurrency(SignerConfiguration.maxConcurrency)
      .build()
  private val asyncConfiguration: ClientAsyncConfiguration =
    ClientAsyncConfiguration
      .builder()
      .advancedOption[Executor](SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, blockingEc)
      .build()

  private val kmsClient: KmsAsyncClient =
    KmsAsyncClient.builder().httpClient(asyncHttpClient).asyncConfiguration(asyncConfiguration).build()

  override def signData(keyId: String, signatureAlgorithm: SignatureAlgorithm)(data: String): Future[String] = {
    val request = createRequest(keyId, signatureAlgorithm, data)
    kmsClient
      .sign(request)
      .asScala
      .map { signResponse =>
        val bytes: SdkBytes         = signResponse.signature()
        val base64Signature: String = Base64.getEncoder.encodeToString(bytes.asByteArray())
        toJWSEncoding(base64Signature)
      }
      .recoverWith(ex => Future.failed(ThirdPartyCallError("KMS", ex.getMessage)))
  }

  private def toJWSEncoding(base64Text: String): String =
    base64Text
      .replaceAll("=", "")
      .replaceAll("\\+", "-")
      .replaceAll("/", "_")

  private def createRequest(keyId: String, signatureAlgorithm: SignatureAlgorithm, data: String): SignRequest =
    SignRequest
      .builder()
      .signingAlgorithm(getSignatureAlgorithm(signatureAlgorithm))
      .keyId(keyId)
      .message(SdkBytes.fromUtf8String(data))
      .build()

  private def getSignatureAlgorithm(signatureAlgorithm: SignatureAlgorithm): SigningAlgorithmSpec =
    signatureAlgorithm match {
      case SignatureAlgorithm.RSAPssSha256   => SigningAlgorithmSpec.RSASSA_PSS_SHA_256
      case SignatureAlgorithm.RSAPssSha384   => SigningAlgorithmSpec.RSASSA_PSS_SHA_384
      case SignatureAlgorithm.RSAPssSha512   => SigningAlgorithmSpec.RSASSA_PSS_SHA_512
      case SignatureAlgorithm.RSAPkcs1Sha256 => SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_256
      case SignatureAlgorithm.RSAPkcs1Sha384 => SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_384
      case SignatureAlgorithm.RSAPkcs1Sha512 => SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_512
      case SignatureAlgorithm.ECSha256       => SigningAlgorithmSpec.ECDSA_SHA_256
      case SignatureAlgorithm.ECSha384       => SigningAlgorithmSpec.ECDSA_SHA_384
      case SignatureAlgorithm.ECSha512       => SigningAlgorithmSpec.ECDSA_SHA_512
      case SignatureAlgorithm.Empty          => SigningAlgorithmSpec.UNKNOWN_TO_SDK_VERSION
    }

}
