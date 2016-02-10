name         := "biomsef"
organization := "es.uvigo.ei.sing"
version      := "1.1.1"

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
  "org.scala-lang.modules" %% "scala-xml" % "1.0.5" ,

  // core java/scala
  "com.typesafe.slick"      %% "slick"                 % "3.0.3"  ,
  "com.typesafe.akka"       %% "akka-actor"            % "2.3.14" ,
  "com.typesafe.play"       %% "play-cache"            % "2.4.3"  ,
  "com.typesafe.play"       %% "play-slick"            % "1.0.1"  ,
  "com.typesafe.play"       %% "play-slick-evolutions" % "1.0.1"  ,
  "com.github.t3hnar"       %% "scala-bcrypt"          % "2.5"    ,
  "net.databinder.dispatch" %% "dispatch-core"         % "0.11.3" ,

  // database connectors
  "mysql"            % "mysql-connector-java" % "5.1.36"  ,
  "org.mariadb.jdbc" % "mariadb-java-client"  % "1.1.8"   ,
  "com.h2database"   % "h2"                   % "1.4.187" ,

  // css/javascript
  "org.webjars" % "angularjs"            % "1.3.17" exclude("org.webjars", "jquery") ,
  "org.webjars" % "bootstrap"            % "3.1.1"  exclude("org.webjars", "jquery") ,
  "org.webjars" % "jquery"               % "1.11.3" ,
  "org.webjars" % "underscorejs"         % "1.8.3"  ,
  "org.webjars" % "requirejs"            % "2.1.20" ,
  "org.webjars" % "angular-ui-bootstrap" % "0.13.4" ,
  "org.webjars" % "font-awesome"         % "4.4.0"  ,
  "org.webjars" % "ng-tags-input"        % "2.3.0"  ,

  // testing
  "org.scalatest"     %% "scalatest"    % "2.2.5"   % "test" ,
  "org.scalacheck"    %% "scalacheck"   % "1.12.3"  % "test" ,
  "com.typesafe.akka" %% "akka-testkit" % "2.3.14"  % "test" ,
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

shellPrompt := { _ => "biomsef » " }
