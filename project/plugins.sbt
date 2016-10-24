logLevel := Level.Warn

addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings" % "0.2.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.4")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")

resolvers += Resolver.typesafeRepo("releases") //used for sbt-codacy-coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0-RC2")
addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.3.5")

addSbtPlugin("com.orrsella" % "sbt-stats" % "1.0.5")