import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "buyOrWait"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.jsoup" % "jsoup" % "1.7.1",
    "org.scalatest" %% "scalatest" % "1.8" % "test",
    "postgresql" % "postgresql" % "9.1-901-1.jdbc4" // Add your project dependencies here,
    )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    // Add your own project settings here  
    resolvers += "releases" at "https://oss.sonatype.org/content/repositories/releases",
    resolvers += "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")

}
