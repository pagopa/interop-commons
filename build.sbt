import ProjectSettings.ProjectFrom

ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / organization     := "it.pagopa"
ThisBuild / organizationName := "Pagopa S.p.A."
ThisBuild / version          := ComputeVersion.version

lazy val fileManagerModuleName = "file-manager"
lazy val mailManagerModuleName = "mail-manager"
lazy val jwtModuleName         = "jwt"
lazy val vaultModuleName       = "vault"
lazy val utilsModuleName       = "utils"

cleanFiles += baseDirectory.value / fileManagerModuleName / "target"
cleanFiles += baseDirectory.value / mailManagerModuleName / "target"
cleanFiles += baseDirectory.value / jwtModuleName / "target"
cleanFiles += baseDirectory.value / vaultModuleName / "target"
cleanFiles += baseDirectory.value / utilsModuleName / "target"

lazy val sharedSettings: SettingsDefinition = Seq(
  scalacOptions     := Seq(),
  scalafmtOnCompile := true,
  libraryDependencies ++= Dependencies.Jars.commonDependencies,
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
  updateOptions     := updateOptions.value.withGigahorse(false),
  publishTo         := {
    val nexus = s"https://${System.getenv("MAVEN_REPO")}/nexus/repository/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "maven-snapshots/")
    else
      Some("releases" at nexus + "maven-releases/")
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

lazy val jwtModule = project
  .in(file(jwtModuleName))
  .settings(name := "interop-commons-jwt", sharedSettings, libraryDependencies ++= Dependencies.Jars.jwtDependencies)
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

lazy val commons = (project in file("."))
  .aggregate(utils, fileManager, mailManager, vault, jwtModule)
  .settings(name := "interop-commons", publish / skip := true, publishLocal / skip := true)
