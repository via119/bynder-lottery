ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = project
  .in(file("."))
  .aggregate(
    lotteryService,
    lotteryServiceIntegrationTest,
  )

lazy val lotteryService = project
  .in(file("lottery-service"))
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .settings(
    name                 := "bynder-lottery",
    Docker / packageName := "bynder-lottery-via",
    testFrameworks       += new TestFramework("weaver.framework.CatsEffect"),
    libraryDependencies ++= Seq(
      doobie,
      doobiePostgres,
      pureconfigCats,
      pureconfigCatsEffect,
      http4sBlazeServer,
      http4sBlazeDsl,
      http4sBlazeCirce,
      circeCore,
      circeGeneric,
      circeParser,
      log4Cats,
      logback,
      scalaTest,
      weaver,
    ),
  )

lazy val lotteryServiceIntegrationTest = project
  .in(file("lottery-service-integration-test"))
  .dependsOn(lotteryService)
  .settings(
    name                 := "lottery-service-integration-test",
    testFrameworks       += new TestFramework("weaver.framework.CatsEffect"),
    libraryDependencies ++= Seq(
      weaver,
      postgresTestContainer,
    ),
  )

lazy val doobie               = "org.tpolecat"          %% "doobie-core"            % "1.0.0-RC2"
lazy val doobiePostgres       = "org.tpolecat"          %% "doobie-postgres"        % "1.0.0-RC2"
lazy val postgresqlJdbcDriver = "org.postgresql"         % "postgresql"             % "42.7.3"
lazy val pureconfigCats       = "com.github.pureconfig" %% "pureconfig-cats"        % "0.17.7"
lazy val pureconfigCatsEffect = "com.github.pureconfig" %% "pureconfig-cats-effect" % pureconfigCats.revision
lazy val http4sBlazeServer    = "org.http4s"            %% "http4s-blaze-server"    % "0.23.16"
lazy val http4sBlazeDsl       = "org.http4s"            %% "http4s-dsl"             % "0.23.27"
lazy val http4sBlazeCirce     = "org.http4s"            %% "http4s-circe"           % http4sBlazeDsl.revision
lazy val circeCore            = "io.circe"              %% "circe-core"             % "0.14.7"
lazy val circeGeneric         = "io.circe"              %% "circe-generic"          % circeCore.revision
lazy val circeParser          = "io.circe"              %% "circe-parser"           % circeCore.revision
lazy val log4Cats             = "org.typelevel"         %% "log4cats-slf4j"         % "2.7.0"
lazy val logback              = "ch.qos.logback"         % "logback-classic"        % "1.3.0"

lazy val scalaTest             = "org.scalatest"       %% "scalatest"                       % "3.2.19" % Test
lazy val weaver                = "com.disneystreaming" %% "weaver-cats"                     % "0.8.4"  % Test
lazy val postgresTestContainer = "com.dimafeng"        %% "testcontainers-scala-postgresql" % "0.41.4" % Test
