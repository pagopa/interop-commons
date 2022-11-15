import sbt._
import sbt.nio.Keys._
import sbt.Keys._
import sbtbuildinfo.BuildInfoKeys.buildInfoOptions
import sbtbuildinfo.BuildInfoPlugin.autoImport.{BuildInfoKey, buildInfoKeys}
import sbtbuildinfo.{BuildInfoOption, BuildInfoPlugin}
import sbtghactions.GitHubActionsPlugin.autoImport._
import sbtghactions.GenerativePlugin.autoImport._
import sbtghpackages.GitHubPackagesPlugin.autoImport._
import RefPredicate._
import Ref._

import scala.sys.process._
import scala.util.Try

/** Allows customizations of build.sbt syntax.
  */
object ProjectSettings {

  // TODO since Git 2.22 we could use the following command instead: git branch --show-current
  private val currentBranch: Option[String] = Try(
    Process(s"git rev-parse --abbrev-ref HEAD").lineStream_!.head
  ).toOption

  private val commitSha: Option[String] = Try(Process(s"git rev-parse --short HEAD").lineStream_!.head).toOption

  private val interfaceVersion: String = ComputeVersion.version match {
    case ComputeVersion.tag(major, minor, _) => s"$major.$minor"
    case _                                   => "0.0"
  }

  // lifts some useful data in BuildInfo instance
  val buildInfoExtra: Seq[BuildInfoKey] = Seq[BuildInfoKey](
    "ciBuildNumber"    -> sys.env.get("BUILD_NUMBER"),
    "commitSha"        -> commitSha,
    "currentBranch"    -> currentBranch,
    "interfaceVersion" -> interfaceVersion
  )

  /** Extention methods for sbt Project instances.
    * @param project
    */
  implicit class ProjectFrom(project: Project) {
    def setupBuildInfo: Project = {
      project
        .enablePlugins(BuildInfoPlugin)
        .settings(buildInfoKeys ++= buildInfoExtra)
        .settings(buildInfoOptions += BuildInfoOption.BuildTime)
        .settings(buildInfoOptions += BuildInfoOption.ToJson)
    }
  }

  val sbtGithubActionsSettings: List[Def.Setting[_]] = List[Def.Setting[_]](
    githubWorkflowPublishTargetBranches := Seq(Equals(Branch("1.0.x")), StartsWith(Tag("v"))),
    githubWorkflowScalaVersions         := Seq("2.13.10"),
    githubOwner                         := "pagopa",
    githubRepository                    := "interop-commons"
  )

}
