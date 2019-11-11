organization := "pt.tecnico.dsi"
name := "ldap"
version := "0.5.0-SNAPSHOT"

// =====================================================================================================================
// ==== Compile Options ================================================================================================
// =====================================================================================================================
javacOptions ++= Seq("-Xlint", "-encoding", "UTF-8", "-Dfile.encoding=utf-8")
scalaVersion := "2.13.1"
scalacOptions ++= Seq(
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-language:higherKinds",             // Allow higher-kinded types
  "-language:implicitConversions",     // Explicitly enables the implicit conversions feature
  "-Ybackend-parallelism", "4",        // Maximum worker threads for backend.
  "-Ybackend-worker-queue", "10",      // Backend threads worker queue size.
  "-Ymacro-annotations",               // Enable support for macro annotations, formerly in macro paradise.
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  "-Xmigration:2.14.0",                // Warn about constructs whose behavior may have changed since version.
  "-Xfatal-warnings", "-Werror",       // Fail the compilation if there are any warnings.
  "-Xlint:_",                          // Enables every warning. scalac -Xlint:help for a list and explanation
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-Wdead-code",                       // Warn when dead code is identified.
  "-Wextra-implicit",                  // Warn when more than one implicit parameter section is defined.
  "-Wnumeric-widen",                   // Warn when numerics are widened.
  "-Woctal-literal",                   // Warn on obsolete octal syntax.
  //"-Wself-implicit",                   // Warn when an implicit resolves to an enclosing self-definition.
  "-Wunused:_",                        // Enables every warning of unused members/definitions/etc
  "-Wunused:patvars",                  // Warn if a variable bound in a pattern is unused.
  "-Wunused:params",                   // Enable -Wunused:explicits,implicits. Warn if an explicit/implicit parameter is unused.
  "-Wunused:linted",                   // -Xlint:unused <=> Enable -Wunused:imports,privates,locals,implicits.
  //"-Wvalue-discard",                   // Warn when non-Unit expression results are unused.
)
// These lines ensure that in sbt console or sbt test:console the -Ywarn* and the -Xfatal-warning are not bothersome.
// https://stackoverflow.com/questions/26940253/in-sbt-how-do-you-override-scalacoptions-for-console-in-all-configurations
scalacOptions in (Compile, console) ~= (_ filterNot { option =>
  option.startsWith("-Ywarn") || option == "-Xfatal-warnings" || option.startsWith("-Xlint")
})
scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value

// ======================================================================================================================
// ==== Dependencies ====================================================================================================
// ======================================================================================================================
val ldaptiveVersion = "1.2.4"
libraryDependencies ++= Seq(
  //Ldap
  "org.ldaptive" % "ldaptive" % ldaptiveVersion,
  "org.ldaptive" % "ldaptive-unboundid" % ldaptiveVersion,
  "com.unboundid" % "unboundid-ldapsdk" % "4.0.12",
  //Logging
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  //Testing
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  //Configuration
  "com.typesafe" % "config" % "1.4.0"
)

// =====================================================================================================================
// ==== Scaladoc =======================================================================================================
// =====================================================================================================================
git.remoteRepo := s"git@github.com:ist-dsi/${name.value}.git"
git.useGitDescribe := true // Get version by calling `git describe` on the repository
val latestReleasedVersion = SettingKey[String]("latest released version")
latestReleasedVersion := git.gitDescribedVersion.value.getOrElse("0.0.1-SNAPSHOT")

// Define the base URL for the Scaladocs for your library. This will enable clients of your library to automatically
// link against the API documentation using autoAPIMappings.
apiURL := Some(url(s"${homepage.value.get}/api/${latestReleasedVersion.value}/"))
autoAPIMappings := true // Tell scaladoc to look for API documentation of managed dependencies in their metadata.
scalacOptions in (Compile, doc) ++= Seq(
  "-author",      // Include authors.
  "-diagrams",    // Create inheritance diagrams for classes, traits and packages.
  "-groups",      // Group similar functions together (based on the @group annotation)
  "-implicits",   // Document members inherited by implicit conversions.
  "-doc-title", name.value.capitalize,
  "-doc-version", latestReleasedVersion.value,
  "-doc-source-url", s"${homepage.value.get}/tree/v${latestReleasedVersion.value}€{FILE_PATH}.scala",
  "-sourcepath", (baseDirectory in ThisBuild).value.getAbsolutePath,
)

enablePlugins(GhpagesPlugin, SiteScaladocPlugin)
siteSubdirName in SiteScaladoc := s"api/${version.value}"
excludeFilter in ghpagesCleanSite := AllPassFilter // We want to keep the previous API versions, etc
val latestFileName = "latest"
val createLatestSymlink = taskKey[Unit](s"Creates a symlink named $latestFileName which points to the latest version.")
createLatestSymlink := {
  ghpagesSynchLocal.value // Ensure the ghpagesRepository already exists
  import java.nio.file.Files
  val path = (ghpagesRepository.value / "api" / latestFileName).toPath
  if (!Files.isSymbolicLink(path)) Files.createSymbolicLink(path, new File(latestReleasedVersion.value).toPath)
}
ghpagesPushSite := ghpagesPushSite.dependsOn(createLatestSymlink).value
ghpagesBranch := "scaladoc"
ghpagesNoJekyll := false
envVars in ghpagesPushSite := Map("SBT_GHPAGES_COMMIT_MESSAGE" -> s"Add Scaladocs for version ${latestReleasedVersion.value}")

// =====================================================================================================================
// ==== Publishing/Release =============================================================================================
// =====================================================================================================================
publishTo := sonatypePublishTo.value
publishArtifact in Test := false

licenses += "MIT" -> url("http://opensource.org/licenses/MIT")
homepage := Some(url(s"https://github.com/ist-dsi/${name.value}"))
scmInfo := Some(ScmInfo(homepage.value.get, git.remoteRepo.value))
developers ++= List(
  Developer("magicknot", "David Duarte", "", url("https://github.com/magicknot")),
  Developer("Lasering", "Simão Martins", "", new URL("https://github.com/Lasering")),
)

// Will fail the build/release if updates for the dependencies are found
dependencyUpdatesFailBuild := true

releaseUseGlobalVersion := false

releasePublishArtifactsAction := PgpKeys.publishSigned.value
import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  releaseStepTask(dependencyUpdates),
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepTask(Compile / doc),
  runTest,
  setReleaseVersion,
  tagRelease,
  releaseStepTask(ghpagesPushSite),
  publishArtifacts,
  setNextVersion,
  pushChanges,
)
