ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .settings(
    name                 := "bynder-lottery",
    Docker / packageName := "bynder-lottery-via",
    libraryDependencies ++= Seq(doobie, doobiePostgres, pureconfigCats, pureconfigCatsEffect) ++ http4sDependencies ++ circeDependencies,
  )

lazy val pureconfigVersion = "0.17.7"

lazy val doobie               = "org.tpolecat"          %% "doobie-core"            % "1.0.0-RC2"
lazy val doobiePostgres       = "org.tpolecat"          %% "doobie-postgres"        % "1.0.0-RC2"
lazy val postgresqlJdbcDriver = "org.postgresql"         % "postgresql"             % "42.7.3"
lazy val pureconfigCats       = "com.github.pureconfig" %% "pureconfig-cats"        % pureconfigVersion
lazy val pureconfigCatsEffect = "com.github.pureconfig" %% "pureconfig-cats-effect" % pureconfigVersion
lazy val http4sDependencies   = Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.23.16",
  "org.http4s" %% "http4s-dsl"          % "0.23.27",
  "org.http4s" %% "http4s-circe"        % "0.23.27",
)

val circeVersion      = "0.14.1"
val circeDependencies = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
).map(_ % circeVersion)
