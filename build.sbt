ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "tinto"
  )
  .aggregate(core, api, frontend)

lazy val core = (project in file("core"))
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"          % "1.0.13",
      "dev.zio" %% "zio-test"     % "1.0.13", // % "test"
      "dev.zio" %% "zio-test-sbt" % "1.0.13" % "test",
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
  .dependsOn(price, std_list)

lazy val api = (project in file("api"))
  .settings(
    name := "api",
    libraryDependencies ++= Seq(
      "dev.zio"               %% "zio"              % "1.0.13",
      "dev.zio"               %% "zio-test"         % "1.0.13", // % "test"
      "dev.zio"               %% "zio-test-sbt"     % "1.0.13" % "test",
      "com.github.ghostdogpr" %% "caliban"          % "1.3.1",
      "com.github.ghostdogpr" %% "caliban-zio-http" % "1.3.1",
      "ch.qos.logback"         % "logback-classic"  % "1.2.10",
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
  .dependsOn(core)

lazy val price    = ProjectRef(uri("https://github.com/palanga/aconcagua.git#v1.0.1"), "price")
lazy val std_list = ProjectRef(uri("https://github.com/palanga/aconcagua.git#v1.0.1"), "std_list")

lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "frontend",
    libraryDependencies ++= Seq(
      "dev.zio"   %%% "zio"      % "1.0.13",
      "dev.zio"    %% "zio-test" % "1.0.13" % Test,
      "com.raquo" %%% "laminar"  % "0.14.2",
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    scalaJSUseMainModuleInitializer := true,
  )
  .dependsOn(core)
