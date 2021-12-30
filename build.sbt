ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "tinto"
  )
  .dependsOn(price, std_list)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

libraryDependencies += "dev.zio" %% "zio"          % "1.0.13"
libraryDependencies += "dev.zio" %% "zio-test"     % "1.0.13" % "test"
libraryDependencies += "dev.zio" %% "zio-test-sbt" % "1.0.13" % "test"
//libraryDependencies += "io.d11"  %% "zhttp" % "1.0.0.0-RC18"
//libraryDependencies += "dev.zio" %% "zio-json" % "0.2.0-M3"
libraryDependencies += "com.github.ghostdogpr" %% "caliban"          % "1.3.1"
libraryDependencies += "com.github.ghostdogpr" %% "caliban-zio-http" % "1.3.1" // routes for zio-http
libraryDependencies += "ch.qos.logback"         % "logback-classic"  % "1.2.10"

lazy val price    = ProjectRef(uri("https://github.com/palanga/aconcagua.git#v1.0.1"), "price")
lazy val std_list = ProjectRef(uri("https://github.com/palanga/aconcagua.git#v1.0.1"), "std_list")
