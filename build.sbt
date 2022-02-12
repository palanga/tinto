ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.0"

lazy val root = (project in file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "tinto"
  )
  .aggregate(core, web, backend, frontend, endpoints, server, client, client_scalajs, mira)

val zioVersion     = "2.0.0-RC1"
val zhttpVersion   = "2.0.0-RC2"
val zioJsonVersion = "0.3.0-RC2"
val sttpVersion    = "3.4.1"
val laminarVersion = "0.14.2"
val logbackVersion = "1.2.10"

lazy val core = (project in file("core"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"          % zioVersion,
      "dev.zio" %% "zio-test"     % zioVersion, // % "test"
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
  .dependsOn(price, std_list)

lazy val web = (project in file("web"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "web"
  )
  .dependsOn(core, endpoints)

lazy val backend = (project in file("backend"))
  .settings(
    name := "backend",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % logbackVersion
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
  .dependsOn(core, web, server)

lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "frontend",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio"      % zioVersion,
      "dev.zio"  %% "zio-test" % zioVersion % Test,
      "dev.zio" %%% "zio-json" % zioJsonVersion,
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSLinkerConfig ~= { _.withSourceMap(true) },
    scalaJSUseMainModuleInitializer := true,
  )
  .dependsOn(core, web, mira, client_scalajs)

// lazy val api = (project in file("api"))
//   .settings(
//     name := "api",
//     libraryDependencies ++= Seq(
//       "dev.zio"               %% "zio"              % "1.0.13",
//       "dev.zio"               %% "zio-test"         % "1.0.13", // % "test"
//       "dev.zio"               %% "zio-test-sbt"     % "1.0.13" % "test",
//       "com.github.ghostdogpr" %% "caliban"          % "1.3.2",
//       "com.github.ghostdogpr" %% "caliban-zio-http" % "1.3.2",
//       "dev.zio"               %% "zio-json"         % "0.2.0-M3",
//       "ch.qos.logback"         % "logback-classic"  % "1.2.10",
//     ),
//     testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
//   )
//   .dependsOn(core)

lazy val endpoints = (project in file("endpoints"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "endpoints",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"          % zioVersion,
      "dev.zio" %% "zio-test"     % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
      "dev.zio" %% "zio-json"     % zioJsonVersion,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )

lazy val server = (project in file("server"))
  .settings(
    name := "server",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"          % zioVersion,
      "dev.zio" %% "zio-test"     % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
      "dev.zio" %% "zio-json"     % zioJsonVersion,
      "io.d11"  %% "zhttp"        % zioVersion,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
  .dependsOn(endpoints)

lazy val client = (project in file("client/jvm"))
  .settings(
    name := "client",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"          % zioVersion,
      "dev.zio" %% "zio-test"     % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
      "dev.zio" %% "zio-json"     % zioJsonVersion,
      "io.d11"  %% "zhttp"        % zioVersion,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
  .dependsOn(endpoints)

lazy val client_scalajs = (project in file("client/js"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "client-scalajs",
    libraryDependencies ++= Seq(
      "dev.zio"                       %%% "zio"          % zioVersion,
      "dev.zio"                       %%% "zio-test"     % zioVersion % "test",
      "dev.zio"                       %%% "zio-test-sbt" % zioVersion % "test",
      "dev.zio"                       %%% "zio-json"     % zioJsonVersion,
      "com.softwaremill.sttp.client3" %%% "core"         % sttpVersion,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
  .dependsOn(endpoints)

lazy val price    = ProjectRef(uri("https://github.com/palanga/aconcagua.git#v1.1.1"), "price")
lazy val std_list = ProjectRef(uri("https://github.com/palanga/aconcagua.git#v1.1.1"), "std_list")

lazy val mira = (project in file("mira"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "mira",
    libraryDependencies ++= Seq(
      "dev.zio"   %%% "zio"      % zioVersion,
      "dev.zio"    %% "zio-test" % zioVersion % Test,
      "com.raquo" %%% "laminar"  % laminarVersion,
    ),
  )

lazy val examples = (project in file("examples"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "examples",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % logbackVersion
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
  .dependsOn(endpoints, server, client)
