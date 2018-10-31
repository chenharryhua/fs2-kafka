import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport.ReleaseStep
import sbtrelease.ReleaseStateTransformations.reapply
import sbtrelease.Vcs

object LatestVersion extends AutoPlugin {
  object autoImport {
    lazy val latestVersion: SettingKey[String] =
      settingKey[String]("Latest released version")

    lazy val latestBinaryCompatibleVersion: SettingKey[Option[String]] =
      settingKey[Option[String]]("Latest released binary-compatible version")

    lazy val setLatestVersion: ReleaseStep = { state: State =>
      val extracted = Project.extract(state)

      val newLatestVersion = extracted.get(version in ThisBuild)
      val latestVersionFile = file("latestVersion.sbt")
      val latestVersionFileContents =
        s"""
          |latestVersion in ThisBuild := "$newLatestVersion"
          |latestBinaryCompatibleVersion in ThisBuild := Some("$newLatestVersion")
         """.stripMargin.trim + "\n"

      IO.write(latestVersionFile, latestVersionFileContents)
      Vcs.detect(file(".")).foreach { vcs =>
        vcs.add(latestVersionFile.getPath) !! state.log
        vcs.commit(
          message = s"Set latest version to $newLatestVersion",
          signOff = false,
          sign = true
        ) !! state.log
      }

      reapply(Seq(latestVersion in ThisBuild := newLatestVersion), state)
    }
  }
}
