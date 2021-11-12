import Versions._
import sbt._

object Dependencies {

  private[this] object logback {
    lazy val namespace = "ch.qos.logback"
    lazy val classic   = namespace % "logback-classic" % logbackVersion
  }

  private[this] object commons {
    lazy val fileUpload = "commons-fileupload" % "commons-fileupload" % commonsFileUploadVersion
  }

  private[this] object scalatest {
    lazy val namespace = "org.scalatest"
    lazy val core      = namespace %% "scalatest" % scalatestVersion
  }

  private[this] object typesafe {
    lazy val namespace = "com.typesafe"
    lazy val config    = namespace % "config" % typesafeConfigVersion
  }

  private[this] object akka {
    lazy val namespace  = "com.typesafe.akka"
    lazy val actorTyped = namespace           %% "akka-actor-typed"     % akkaVersion
    lazy val http       = namespace           %% "akka-http"            % akkaHttpVersion
    lazy val httpJson   = namespace           %% "akka-http-spray-json" % akkaHttpVersion
    lazy val httpJson4s = "de.heikoseeberger" %% "akka-http-json4s"     % akkaHttpJson4sVersion
    lazy val slf4j      = namespace           %% "akka-slf4j"           % akkaVersion
  }

  private[this] object azure {
    lazy val azureNamespace = "com.azure"
    lazy val storageBlob    = azureNamespace % "azure-storage-blob" % azureStorageBlobVersion
  }

  private[this] object aws {
    lazy val awsNamespace = "software.amazon.awssdk"
    lazy val s3           = awsNamespace % "s3" % awsSdkVersion
  }

  private[this] object courier {
    lazy val namespace = "com.github.daddykotex"
    lazy val mail      = namespace %% "courier" % courierVersion
  }

  object Jars {
    lazy val akkaDependencies: Seq[ModuleID] =
      Seq(
        akka.http       % Compile,
        akka.httpJson   % Compile,
        akka.httpJson4s % Compile,
        akka.slf4j      % Compile,
        akka.actorTyped % Compile
      )

    lazy val fileDependencies: Seq[ModuleID] =
      Seq(aws.s3 % Compile, azure.storageBlob % Compile, commons.fileUpload % Compile)

    lazy val mailDependencies: Seq[ModuleID] =
      Seq(courier.mail % Compile)

    lazy val commonDependencies: Seq[ModuleID] = Seq(
      // For making Java 12 happy
      "javax.annotation" % "javax.annotation-api" % "1.3.2" % "compile",
      typesafe.config    % Compile,
      logback.classic    % Compile,
      scalatest.core     % Test
    )
  }
}
