import ReleaseTransformations._

lazy val `fs2-kafka` = project
  .in(file("."))
  .settings(
    dependencySettings,
    metadataSettings,
    mimaSettings,
    releaseSettings,
    scalaSettings,
    testSettings
  )

lazy val docs = project
  .in(file("docs"))
  .settings(
    mdocSettings,
    metadataSettings,
    noPublishSettings,
    scalaSettings
  )
  .dependsOn(`fs2-kafka`)

lazy val dependencySettings = Seq(
  libraryDependencies ++= Seq(
    "co.fs2" %% "fs2-core" % "1.0.0",
    "org.apache.kafka" % "kafka-clients" % "2.0.0"
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.5",
    "org.scalacheck" %% "scalacheck" % "1.14.0",
    "net.manub" %% "scalatest-embedded-kafka" % "2.0.0"
  ).map(_ % Test)
)

lazy val mdocSettings = Seq(
  libraryDependencies += "com.geirsson" % "mdoc" % "0.5.3" cross CrossVersion.full,
)

lazy val metadataSettings = Seq(
  organization := "com.ovoenergy",
  bintrayOrganization := Some("ovotech"),
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
)

lazy val mimaSettings = Seq(
  mimaPreviousArtifacts := {
    def isPublishing = publishArtifact.value

    latestBinaryCompatibleVersion.value match {
      case Some(version) if isPublishing =>
        Set(organization.value %% moduleName.value % version)
      case _ =>
        Set.empty
    }
  },
  mimaBinaryIssueFilters ++= {
    import com.typesafe.tools.mima.core._
    Seq()
  }
)

lazy val noPublishSettings = Seq(
  skip in publish := true,
  publishArtifact := false
)

lazy val releaseSettings = Seq(
  releaseCrossBuild := true,
  releaseUseGlobalVersion := true,
  releaseTagName := s"v${(version in ThisBuild).value}",
  releaseTagComment := s"Release version ${(version in ThisBuild).value}",
  releaseCommitMessage := s"Set version to ${(version in ThisBuild).value}",
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    setLatestVersion,
    releaseStepTask(updateReadme in ThisBuild),
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val scalaSettings = Seq(
  scalaVersion := "2.12.7",
  crossScalaVersions := Seq(scalaVersion.value, "2.11.12"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture",
    "-Ywarn-unused",
    "-Ypartial-unification"
  )
)

lazy val testSettings = Seq(
  logBuffered in Test := false,
  parallelExecution in Test := false,
  testOptions in Test += Tests.Argument("-oDF")
)

def runMdoc(args: String*) = Def.taskDyn {
  val in = (baseDirectory in `fs2-kafka`).value / "docs"
  val out = (baseDirectory in `fs2-kafka`).value
  val scalacOptionsString = (scalacOptions in Compile).value.mkString(" ")
  val siteVariables = List(
    "LATEST_VERSION" -> (latestVersion in ThisBuild).value
  ).map { case (k, v) => s"""--site.$k "$v"""" }.mkString(" ")
  (runMain in (docs, Compile)).toTask(s""" mdoc.Main --in "$in" --out "$out" --exclude "target" --scalac-options "$scalacOptionsString" $siteVariables ${args.mkString(" ")}""")
}

val generateReadme = taskKey[Unit]("Generates the readme using mdoc")
generateReadme in ThisBuild := runMdoc().value

val updateReadme = taskKey[Unit]("Generates and commits the readme")
updateReadme in ThisBuild := {
  (generateReadme in ThisBuild).value
  sbtrelease.Vcs.detect((baseDirectory in `fs2-kafka`).value).foreach { vcs =>
    vcs.add("readme.md").!
    vcs.commit(
      message = "Update readme to latest version",
      signOff = false,
      sign = true
    ).!
  }
}

def addCommandsAlias(name: String, values: List[String]) =
  addCommandAlias(name, values.mkString(";", ";", ""))

addCommandsAlias("validate", List(
  "coverage",
  "test",
  "coverageReport",
  "mimaReportBinaryIssues",
  "generateReadme"
))
