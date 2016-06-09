organization := "pt.tecnico.dsi"
name := "ldap"
version := "0.0.1"

scalaVersion := "2.11.8"
initialize := {
  val required = "1.8"
  val current  = sys.props("java.specification.version")
  assert(current == required, s"Unsupported JDK: java.specification.version $current != $required")
}
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

scalacOptions ++= Seq(
  "-deprecation",                   //Emit warning and location for usages of deprecated APIs.
  "-encoding", "UTF-8",             //Use UTF-8 encoding. Should be default.
  "-feature",                       //Emit warning and location for usages of features that should be imported explicitly.
  "-language:implicitConversions",  //Explicitly enables the implicit conversions feature
  "-unchecked",                     //Enable detailed unchecked (erasure) warnings
  "-Xfatal-warnings",               //Fail the compilation if there are any warnings.
  "-Xlint",                         //Enable recommended additional warnings.
  "-Yinline-warnings",              //Emit inlining warnings.
  "-Yno-adapted-args",              //Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ywarn-dead-code"                //Warn when dead code is identified.
)

libraryDependencies ++= Seq(
  // Actors
  "com.typesafe.akka" %% "akka-actor" % "2.4.7",
  //Ldap
  "org.ldaptive" % "ldaptive" % "1.1.0",
  "org.ldaptive" % "ldaptive-unboundid" % "1.1.0",
  "com.unboundid" % "unboundid-ldapsdk" % "3.1.1",
  //AES Random Number Generator
  "io.gatling.uncommons.maths" % "uncommons-maths" % "1.2.3", //the most recent version is not provided by uncommons.maths organization
  //Logging
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  //Testing
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  //Configuration
  "com.typesafe" % "config" % "1.3.0"
)

resolvers += Resolver.mavenLocal

autoAPIMappings := true
scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits", "-diagrams")

site.settings
site.includeScaladoc()
ghpages.settings
git.remoteRepo := s"git@github.com:ist-dsi/${name.value}.git"

licenses += "MIT" -> url("http://opensource.org/licenses/MIT")
homepage := Some(url(s"https://github.com/ist-dsi/${name.value}"))
scmInfo := Some(ScmInfo(homepage.value.get, s"git@github.com:ist-dsi/${name.value}.git"))

publishMavenStyle := true
publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging)
publishArtifact in Test := false

pomIncludeRepository := { _ => false }
pomExtra :=
  <developers>
    <developer>
      <id>magicknot</id>
      <name>David Duarte</name>
      <url>https://github.com/magicknot</url>
    </developer>
  </developers>
