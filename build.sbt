name         := "biomsef"
organization := "es.uvigo.ei.sing"
version      := "1.0.0"

scalaVersion := "2.11.7"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xfuture",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard"
)

libraryDependencies ++= Seq(
  // scala lang modules
  "org.scala-lang.modules" %% "scala-xml" % "1.0.4" ,

  // core java/scala
  "com.typesafe.slick"      %% "slick"                 % "3.0.0"  ,
  "com.typesafe.akka"       %% "akka-actor"            % "2.3.11" ,
  "com.typesafe.play"       %% "play-cache"            % "2.4.2"  ,
  "com.typesafe.play"       %% "play-slick"            % "1.0.0"  ,
  "com.typesafe.play"       %% "play-slick-evolutions" % "1.0.0"  ,
  "com.github.t3hnar"       %% "scala-bcrypt"          % "2.4"    ,
  "net.databinder.dispatch" %% "dispatch-core"         % "0.11.3" ,

  // database connectors
  "com.h2database" % "h2" % "1.4.187" ,

  // css/javascript
  "org.webjars" % "angularjs"            % "1.3.15"  exclude("org.webjars", "jquery") ,
  "org.webjars" % "bootstrap"            % "3.1.1-1" exclude("org.webjars", "jquery") ,
  "org.webjars" % "jquery"               % "1.11.3"  ,
  "org.webjars" % "underscorejs"         % "1.8.3"   ,
  "org.webjars" % "requirejs"            % "2.1.17"  ,
  "org.webjars" % "angular-ui-bootstrap" % "0.13.0"  ,
  "org.webjars" % "font-awesome"         % "4.3.0-2" ,
  "org.webjars" % "sigma.js"             % "1.0.3"

  // testing
  "org.scalatest"     %% "scalatest"    % "2.2.5"   % "test" ,
  "org.scalacheck"    %% "scalacheck"   % "1.12.3"  % "test" ,
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11"  % "test" ,
  "org.mockito"       %  "mockito-core" % "1.10.19" % "test"
)

import PlayKeys.playPackageAssets
import TwirlKeys.{ compileTemplates, templateImports }

enablePlugins(PlayScala)

sourceDirectory in Compile := baseDirectory.value / "src/main"
sourceDirectory in Test    := baseDirectory.value / "src/test"

scalaSource in Compile := (sourceDirectory in Compile).value / "scala"
scalaSource in Test    := (sourceDirectory in    Test).value / "scala"

resourceDirectory in Compile := (sourceDirectory in Compile).value / "resources"
resourceDirectory in Test    := (sourceDirectory in    Test).value / "resources"

sourceDirectory in Assets := (sourceDirectory in Compile).value / "assets"
pipelineStages := Seq(rjs, digest, gzip)

sourceDirectory in (Compile, compileTemplates) := (sourceDirectory in Compile).value / "twirl"
templateImports += "es.uvigo.ei.sing.biomsef.controller._"

mainClass     in assembly := Some("play.core.server.NettyServer")
fullClasspath in assembly += Attributed.blank(playPackageAssets.value)
