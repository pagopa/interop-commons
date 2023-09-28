import Versions._
import sbt._

object Dependencies {

  private[this] object logback {
    lazy val namespace = "ch.qos.logback"
    lazy val classic   = namespace % "logback-classic" % logbackVersion
  }

  private[this] object circe {
    lazy val namespace = "io.circe"
    lazy val yaml      = namespace %% "circe-yaml"    % circeVersion
    lazy val core      = namespace %% "circe-core"    % circeVersion
    lazy val generic   = namespace %% "circe-generic" % circeVersion
    lazy val parser    = namespace %% "circe-parser"  % circeVersion
  }

  private[this] object commons {
    lazy val fileUpload = "commons-fileupload" % "commons-fileupload" % commonsFileUploadVersion
  }

  private[this] object apacheCommons {
    lazy val text  = "org.apache.commons" % "commons-text"  % apacheCommonsTextVersion
    lazy val codec = "commons-codec"      % "commons-codec" % apacheCommonsCodecVersion
  }

  private[this] object scala {
    lazy val namespace = "org.scala-lang.modules"
    lazy val xml       = namespace %% "scala-xml" % scalaXMLVersion
  }

  private[this] object scalatest {
    lazy val namespace = "org.scalatest"
    lazy val core      = namespace %% "scalatest" % scalatestVersion
  }

  private[this] object scalamock {
    lazy val namespace = "org.scalamock"
    lazy val core      = namespace %% "scalamock" % scalaMockVersion
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

  private[this] object redis {
    lazy val jedis = "redis.clients" % "jedis" % jedisVersion
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

    lazy val utilsDependencies: Seq[ModuleID] = Seq(
      akka.http                  % Compile,
      akka.slf4j                 % Compile,
      akka.httpJson              % Compile,
      akka.httpJson4s            % Compile,
      akka.stream                % Compile,
      spray.spray                % Compile,
      typesafe.config            % Compile,
      cats.core                  % Compile,
      apacheCommons.text         % Compile,
      apacheCommons.codec        % Compile,
      logback.classic            % Compile,
      atlassian.swaggerValidator % Compile,
      lightbend.logging          % Compile,
      akka.httpTestkit           % Test,
      akka.streamTestkit         % Test
    )

    lazy val fileDependencies: Seq[ModuleID] = Seq(
      aws.s3               % Compile,
      aws.sts              % Runtime,
      typesafe.config      % Compile,
      jsoup.jsoup          % Compile,
      openhtmltopdf.core   % Compile,
      openhtmltopdf.pdfbox % Compile,
      openhtmltopdf.slf4j  % Runtime,
      openhtmltopdf.svg    % Compile,
      pdfbox.lib           % Compile,
      cats.core            % Compile,
      pdfcompare.lib       % Test
    )

    lazy val mailDependencies: Seq[ModuleID] = Seq(
      courier.mail             % Compile,
      "org.typelevel"         %% "literally"  % "1.1.0"  % Compile,
      "com.github.pureconfig" %% "pureconfig" % "0.17.2" % Compile,
      circe.core               % Compile, 
      circe.generic            % Compile, 
      circe.parser             % Compile,
      courier.testMocking      % Test
    )

    lazy val signerDependencies: Seq[ModuleID] = Seq(
      akka.stream              % Compile,
      aws.kms                  % Compile,
      aws.sts                  % Runtime,
      typesafe.config          % Compile,
      scalamock.core           % Test
    )

    lazy val jwtDependencies: Seq[ModuleID] = Seq(
      nimbus.joseJwt     % Compile,
      typesafe.config    % Compile,
      cats.core          % Compile,
      akka.streamTestkit % Test,
      akka.httpTestkit   % Test
    )

    lazy val queueDependencies: Seq[ModuleID] =
      Seq(aws.sts % Runtime, aws.sqs % Compile, typesafe.config % Compile, spray.spray % Compile, cats.core % Compile)

    lazy val cqrsDependencies: Seq[ModuleID] = Seq(
      akka.projectionSlick        % Compile,
      akka.actorTyped             % Compile,
      akka.persistenceJdbc        % Compile,
      akka.persistenceQuery       % Compile,
      akka.projectionEventSourced % Compile,
      mongodb.scalaDriver         % Compile,
      spray.spray                 % Compile,
      cats.core                   % Compile
    )

    lazy val rateLimiterDependencies: Seq[ModuleID] = Seq(
      redis.jedis        % Compile,
      akka.stream        % Compile,
      spray.spray        % Compile,
      cats.core          % Compile,
      scalamock.core     % Test,
      akka.httpTestkit   % Test,
      akka.streamTestkit % Test
    )

    lazy val parserDependencies: Seq[ModuleID] =
      Seq(cats.core % Compile, circe.core % Compile, circe.parser % Compile, circe.yaml % Compile, scala.xml % Compile)

    lazy val riskAnalysisDependencies: Seq[ModuleID] =
      Seq(
        cats.core                 % Compile, 
        spray.spray               % Compile, 
        akka.http                 % Compile,
        akka.slf4j                % Compile,
        akka.httpJson             % Compile,
        akka.httpJson4s           % Compile,
        akka.stream               % Compile
      )
  
    lazy val commonDependencies: Seq[ModuleID] = Seq(logback.classic % Runtime, scalatest.core % Test)
  }
}
