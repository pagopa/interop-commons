import ProjectSettings.ProjectFrom

ThisBuild / scalaVersion      := "2.13.8"
ThisBuild / organization      := "it.pagopa"
ThisBuild / organizationName  := "Pagopa S.p.A."
ThisBuild / version           := ComputeVersion.version
Global / onChangedBuildSource := ReloadOnSourceChanges

val fileManagerModuleName = "file-manager"
val mailManagerModuleName = "mail-manager"
val jwtModuleName         = "jwt"
val signerModuleName      = "signer"
val utilsModuleName       = "utils"
val queueModuleName       = "queue-manager"
val cqrsModuleName        = "cqrs"

cleanFiles += baseDirectory.value / fileManagerModuleName / "target"
cleanFiles += baseDirectory.value / mailManagerModuleName / "target"
cleanFiles += baseDirectory.value / jwtModuleName / "target"
cleanFiles += baseDirectory.value / signerModuleName / "target"
cleanFiles += baseDirectory.value / utilsModuleName / "target"
cleanFiles += baseDirectory.value / queueModuleName / "target"

lazy val sharedSettings: SettingsDefinition = Seq(
  scalafmtOnCompile := true,
  libraryDependencies ++= Dependencies.Jars.commonDependencies,
  publishTo         := {
    val nexus = s"https://${System.getenv("MAVEN_REPO")}/nexus/repository/"
    Some((if (isSnapshot.value) "snapshots" else "releases") at nexus + "maven-snapshots/")
  }
)

lazy val utils = project
  .in(file(utilsModuleName))
  .settings(name := "interop-commons-utils", sharedSettings, libraryDependencies ++= Dependencies.Jars.akkaDependencies)
  .setupBuildInfo

lazy val fileManager = project
  .in(file(fileManagerModuleName))
  .settings(
    name := "interop-commons-file-manager",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.fileDependencies
  )
  .dependsOn(utils)
  .setupBuildInfo

lazy val mailManager = project
  .in(file(mailManagerModuleName))
  .settings(
    name        := "interop-commons-mail-manager",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.mailDependencies,
    Test / fork := true
  )
  .dependsOn(utils)
  .setupBuildInfo

lazy val signer = project
  .in(file(signerModuleName))
  .settings(
    name        := "interop-commons-signer",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.signerDependencies,
    Test / fork := true
  )
  .dependsOn(utils)
  .setupBuildInfo

lazy val jwtModule = project
  .in(file(jwtModuleName))
  .settings(
    name        := "interop-commons-jwt",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.jwtDependencies,
    Test / fork := true,
    Test / javaOptions += "-Dconfig.file=src/test/resources/reference.conf"
  )
  .dependsOn(utils, signer)
  .setupBuildInfo

lazy val queue = project
  .in(file(queueModuleName))
  .settings(
    name := "interop-commons-queue-manager",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.queueDependencies
  )
  .setupBuildInfo

lazy val cqrs = project
  .in(file(cqrsModuleName))
  .settings(
    name := "interop-commons-cqrs",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.cqrsDependencies
  )
  .dependsOn(utils)
  .setupBuildInfo

lazy val commons = (project in file("."))
  .aggregate(utils, fileManager, mailManager, signer, jwtModule, queue, cqrs)
  .settings(name := "interop-commons", publish / skip := true, publishLocal / skip := true)
