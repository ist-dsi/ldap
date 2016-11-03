organization := "pt.tecnico.dsi"
name := "ldap"

javacOptions ++= Seq(
  "-Xlint",
  "-encoding", "UTF-8",
  "-Dfile.encoding=utf-8"
)

scalaVersion := "2.12.0"
scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-deprecation",                   //Emit warning and location for usages of deprecated APIs.
  "-encoding", "UTF-8",             //Use UTF-8 encoding. Should be default.
  "-feature",                       //Emit warning and location for usages of features that should be imported explicitly.
  "-language:implicitConversions",  //Explicitly enables the implicit conversions feature
  "-unchecked",                     //Enable detailed unchecked (erasure) warnings
  "-Xfatal-warnings",               //Fail the compilation if there are any warnings.
  "-Xlint",                         //Enable recommended additional warnings.
//  "-Yinline-warnings",              //Emit inlining warnings.
  "-Yno-adapted-args",              //Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ywarn-dead-code"                //Warn when dead code is identified.
)

val ldaptiveVersion = "1.2.0"
libraryDependencies ++= Seq(
  //Ldap
  "org.ldaptive" % "ldaptive" % ldaptiveVersion,
  "org.ldaptive" % "ldaptive-unboundid" % ldaptiveVersion,
  "com.unboundid" % "unboundid-ldapsdk" % "3.2.0",
  //AES Random Number Generator
  "io.gatling.uncommons.maths" % "uncommons-maths" % "1.2.3", //the most recent version is not provided by uncommons.maths organization
  //Logging
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  //Testing
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  //Configuration
  "com.typesafe" % "config" % "1.3.1"
)

shellPrompt := { s => Project.extract(s).currentProject.id + " > " }

autoAPIMappings := true
scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits", "-diagrams")

site.settings
site.includeScaladoc()
ghpages.settings
git.remoteRepo := s"git@github.com:ist-dsi/${name.value}.git"

licenses += "MIT" -> url("http://opensource.org/licenses/MIT")
homepage := Some(url(s"https://github.com/ist-dsi/${name.value}"))
scmInfo := Some(ScmInfo(homepage.value.get, git.remoteRepo.value))

publishMavenStyle := true
publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging)
publishArtifact in Test := false
sonatypeProfileName := organization.value

pomIncludeRepository := { _ => false }
pomExtra :=
  <developers>
    <developer>
      <id>magicknot</id>
      <name>David Duarte</name>
      <url>https://github.com/magicknot</url>
    </developer>
  </developers>

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  ReleaseStep(action = Command.process("doc", _)),
  setReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("ghpagesPushSite", _)),
  ReleaseStep(action = Command.process("publishSigned", _)),
  ReleaseStep(action = Command.process("sonatypeRelease", _)),
  pushChanges,
  setNextVersion
)
