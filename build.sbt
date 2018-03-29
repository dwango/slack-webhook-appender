import sbtrelease._
import ReleaseStateTransformations._
import scala.sys.process.Process

name := "slack-webhook-appender"

organization := "jp.co.dwango"

javacOptions in compile ++= Seq("-target", "8", "-source", "8")

javacOptions in (Compile, doc) ++= Seq("-locale", "en_US")

homepage := Some(url("https://github.com/dwango/slack-webhook-appender"))

licenses := Seq("MIT License" -> url("https://raw.githubusercontent.com/dwango/slack-webhook-appender/master/LICENSE"))

description := "Logback appender which posts logs to slack via incoming webhook"

fork in Test := true

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

val logbackVersion = "1.1.8"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  ReleaseStep{ state1 =>
    val extracted1 = Project extract state1
    IO.withTemporaryDirectory{ tempDir =>
      def inTempDir(commands: String*) = Process(command = commands, cwd = Some(tempDir)).!

      val remoteURL = "git@github.com:dwango/slack-webhook-appender.git"
      val mvnRepoBranch = "mvn-repo"
      Process(command = Seq("git", "clone", "-b", mvnRepoBranch, remoteURL, tempDir.getCanonicalPath)).!
      val state2 = extracted1.appendWithSession(
        settings = Seq(
          publishTo := Some(
            Resolver.file("mvn-repo", tempDir)(Patterns(true, Resolver.mavenStyleBasePattern))
          )
        ),
        state = state1
      )
      val extracted2 = Project extract state2
      extracted2.runAggregated(
        publish in Global in extracted2.get(thisProjectRef),
        state2
      )
      val v = extracted2 get version
      inTempDir("git", "add", "-f", ".")
      inTempDir("git", "commit", "-m", s"Maven artifacts for ${v}")
      SimpleReader.readLine(s"push to ${mvnRepoBranch}? (y/n)?") match {
        case Some(s) if s.toLowerCase == "y" =>
          inTempDir("git", "push", remoteURL, s"${mvnRepoBranch}:${mvnRepoBranch}")
        case _ =>
          sys.error("abort release")
      }
      state2
    }
  },
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)

// http://xerial.org/blog/2014/03/24/sbt/
autoScalaLibrary := false
crossPaths := false
