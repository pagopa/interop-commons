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

  private[this] object apacheCommons {
    lazy val namespace = "org.apache.commons"
    lazy val text      = namespace % "commons-text" % apacheCommonsTextVersion
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
    lazy val namespace              = "com.typesafe.akka"
    lazy val actorTyped             = namespace            %% "akka-actor-typed"             % akkaVersion
    lazy val http                   = namespace            %% "akka-http"                    % akkaHttpVersion
    lazy val httpJson               = namespace            %% "akka-http-spray-json"         % akkaHttpVersion
    lazy val httpJson4s             = "de.heikoseeberger"  %% "akka-http-json4s"             % akkaHttpJson4sVersion
    lazy val slf4j                  = namespace            %% "akka-slf4j"                   % akkaVersion
    lazy val stream                 = namespace            %% "akka-stream"                  % akkaVersion
    lazy val httpTestkit            = namespace            %% "akka-http-testkit"            % akkaHttpVersion
    lazy val streamTestkit          = namespace            %% "akka-stream-testkit"          % akkaVersion
    lazy val projectionSlick        = "com.lightbend.akka" %% "akka-projection-slick"        % slickProjectionVersion
    lazy val persistenceJdbc        = "com.lightbend.akka" %% "akka-persistence-jdbc"        % jdbcPersistenceVersion
    lazy val persistenceQuery       = namespace            %% "akka-persistence-query"       % akkaVersion
    lazy val projectionEventSourced = "com.lightbend.akka" %% "akka-projection-eventsourced" % projectionVersion

  }

  private[this] object spray {
    lazy val spray = "io.spray" %% "spray-json" % sprayJsonVersion
  }

  private[this] object aws {
    lazy val awsNamespace = "software.amazon.awssdk"
    lazy val kms          = awsNamespace % "kms" % awsVersion
    lazy val s3           = awsNamespace % "s3"  % awsVersion
    lazy val sqs          = awsNamespace % "sqs" % awsVersion
    lazy val sts          = awsNamespace % "sts" % awsVersion // Required to use IAM role on container
  }

  private[this] object courier {
    lazy val namespace   = "com.github.daddykotex"
    lazy val mail        = namespace                %% "courier"       % courierVersion
    lazy val testMocking = "org.jvnet.mock-javamail" % "mock-javamail" % mockJavaMailVersion
  }

  private[this] object jsoup {
    lazy val namespace = "org.jsoup"
    lazy val jsoup     = namespace % "jsoup" % jsoupVersion
  }

  private[this] object nimbus {
    lazy val namespace = "com.nimbusds"
    lazy val joseJwt   = namespace % "nimbus-jose-jwt" % nimbusVersion
  }

  private[this] object cats {
    lazy val namespace = "org.typelevel"
    lazy val core      = namespace %% "cats-core" % catsVersion
  }

  private[this] object vault {
    lazy val namespace = "com.bettercloud"
    lazy val driver    = namespace % "vault-java-driver" % vaultDriverVersion
  }

  private[this] object testContainers {
    lazy val namespace = "com.dimafeng"
    lazy val scalatest = namespace %% "testcontainers-scala-scalatest" % testContainersScalaVersion
    lazy val vault     = namespace %% "testcontainers-scala-vault"     % testContainersScalaVersion
  }

  private[this] object pdfbox {
    lazy val namespace = "org.apache.pdfbox"
    lazy val lib       = namespace % "pdfbox" % pdfBoxVersion
  }

  private[this] object pdfcompare {
    lazy val namespace = "de.redsix"
    lazy val lib       = namespace % "pdfcompare" % pdfCompareVersion
  }

  private[this] object openhtmltopdf {
    lazy val namespace = "com.openhtmltopdf"
    lazy val core      = namespace % "openhtmltopdf-core"        % openhtmltopdfVersion
    lazy val pdfbox    = namespace % "openhtmltopdf-pdfbox"      % openhtmltopdfVersion
    lazy val slf4j     = namespace % "openhtmltopdf-slf4j"       % openhtmltopdfVersion
    lazy val svg       = namespace % "openhtmltopdf-svg-support" % openhtmltopdfVersion
  }

  private[this] object lightbend {
    lazy val logging = "com.typesafe.scala-logging" %% "scala-logging" % lightbendLoggingVersion
  }

  private[this] object mongodb {
    lazy val scalaDriver = "org.mongodb.scala" %% "mongo-scala-driver" % mongodbScalaDriverVersion
  }

  private[this] object atlassian {
    lazy val namespace        = "com.atlassian.oai"
    lazy val swaggerValidator = namespace % "swagger-request-validator-core" % swaggerValidatorVersion
  }

  object Jars {
    lazy val akkaDependencies: Seq[ModuleID] =
      Seq(
        akka.http          % Compile,
        akka.httpJson      % Compile,
        akka.httpJson4s    % Compile,
        akka.slf4j         % Compile,
        akka.stream        % Compile,
        akka.actorTyped    % Compile,
        akka.httpTestkit   % Test,
        akka.streamTestkit % Test
      )

    lazy val fileDependencies: Seq[ModuleID] =
      Seq(
        aws.s3               % Compile,
        aws.sts              % Compile,
        commons.fileUpload   % Compile,
        jsoup.jsoup          % Compile,
        openhtmltopdf.core   % Compile,
        openhtmltopdf.pdfbox % Compile,
        openhtmltopdf.slf4j  % Compile,
        openhtmltopdf.svg    % Compile,
        pdfbox.lib           % Compile,
        pdfcompare.lib       % Test
      )

    lazy val mailDependencies: Seq[ModuleID] = Seq(courier.mail % Compile, courier.testMocking % Test)

    lazy val signerDependencies: Seq[ModuleID] =
      Seq(
        aws.kms                  % Compile,
        aws.sts                  % Compile,
        vault.driver             % Compile,
        testContainers.scalatest % Test,
        testContainers.vault     % Test
      )

    lazy val jwtDependencies: Seq[ModuleID] = Seq(nimbus.joseJwt % Compile)

    lazy val queueDependencies: Seq[ModuleID] = Seq(aws.sqs % Compile, aws.sts % Compile, spray.spray % Compile)

    lazy val cqrsDependencies: Seq[ModuleID] = Seq(
      akka.projectionSlick        % Compile,
      akka.actorTyped             % Compile,
      akka.persistenceJdbc        % Compile,
      akka.persistenceQuery       % Compile,
      akka.projectionEventSourced % Compile,
      mongodb.scalaDriver         % Compile,
      spray.spray                 % Compile
    )

    lazy val commonDependencies: Seq[ModuleID] = Seq(
      // For making Java 12 happy
      "javax.annotation"         % "javax.annotation-api" % "1.3.2" % "compile",
      apacheCommons.text         % Compile,
      atlassian.swaggerValidator % Compile,
      cats.core                  % Compile,
      logback.classic            % Compile,
      typesafe.config            % Compile,
      lightbend.logging          % Compile,
      scalatest.core             % Test
    )
  }
}
