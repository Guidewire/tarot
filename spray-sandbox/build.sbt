
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"    % "2.2.0",
  "com.typesafe.akka" %% "akka-slf4j"    % "2.2.0",
  "com.typesafe.akka" %% "akka-remote"   % "2.2.0",
  "com.typesafe.akka" %% "akka-agent"    % "2.2.0",
  "com.typesafe.akka" %% "akka-testkit"  % "2.2.0"        % "test"
)

libraryDependencies ++= Seq(
  "io.spray"           % "spray-can"     % "1.2-20130712",
  "io.spray"           % "spray-routing" % "1.2-20130712",
  "io.spray"          %% "spray-json"    % "1.2.5",
  "io.spray"           % "spray-testkit" % "1.2-20130712" % "test"
)

libraryDependencies ++= Seq(
  "org.slf4j"          % "slf4j-api"     % "1.7.5"
)

libraryDependencies ++= Seq(
  "ch.qos.logback"     % "logback-core"    % "1.0.13",
  "ch.qos.logback"     % "logback-classic" % "1.0.13"
)

libraryDependencies += "org.apache.jclouds" % "jclouds-all" % "1.6.1-incubating"

libraryDependencies += "org.scalaj" % "scalaj-time_2.10.0-M7" % "0.6"

libraryDependencies +=  "org.specs2" %% "specs2" % "1.14" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test"

libraryDependencies += "junit" % "junit" % "4.11" % "test"

resolvers += "spray nightlies repo" at "http://nightlies.spray.io"

resolvers += "spray repo" at "http://repo.spray.io"

seq(Revolver.settings: _*)
