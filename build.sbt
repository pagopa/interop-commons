import ProjectSettings.ProjectFrom

ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / organization     := "it.pagopa"
ThisBuild / organizationName := "Pagopa S.p.A."
ThisBuild / version          := ComputeVersion.version

ThisBuild / githubOwner      := "pagopa"
ThisBuild / githubRepository := "interop-commons"
ThisBuild / resolvers += Resolver.githubPackages("pagopa")

val fileManagerModuleName = "file-manager"
val mailManagerModuleName = "mail-manager"
val jwtModuleName         = "jwt"
val vaultModuleName       = "vault"
val utilsModuleName       = "utils"
val queueModuleName       = "queue-manager"

cleanFiles += baseDirectory.value / fileManagerModuleName / "target"
cleanFiles += baseDirectory.value / mailManagerModuleName / "target"
cleanFiles += baseDirectory.value / jwtModuleName / "target"
cleanFiles += baseDirectory.value / vaultModuleName / "target"
cleanFiles += baseDirectory.value / utilsModuleName / "target"
cleanFiles += baseDirectory.value / queueModuleName / "target"

lazy val sharedSettings: SettingsDefinition = Seq(
  scalacOptions     := Seq(),
  scalafmtOnCompile := true,
  libraryDependencies ++= Dependencies.Jars.commonDependencies,
  updateOptions     := updateOptions.value.withGigahorse(false)
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
    name := "interop-commons-mail-manager",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.mailDependencies
  )
  .dependsOn(utils, fileManager)
  .setupBuildInfo

lazy val vault = project
  .in(file(vaultModuleName))
  .settings(
    name        := "interop-commons-vault",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.vaultDependencies,
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
  .dependsOn(utils, vault)
  .setupBuildInfo

lazy val queue = project
  .in(file(queueModuleName))
  .settings(
    name := "interop-commons-queue-manager",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.queueDependencies
  )
  .setupBuildInfo

lazy val commons = (project in file("."))
  .aggregate(utils, fileManager, mailManager, vault, jwtModule, queue)
  .settings(name := "interop-commons")
  .enablePlugins(NoPublishPlugin)
