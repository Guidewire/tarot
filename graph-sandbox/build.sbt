
libraryDependencies ++= Seq(
  "io.spray"           % "spray-client"  % "1.1-M8",
  "io.spray"          %% "spray-json"    % "1.2.5"
)

libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test"

libraryDependencies += "junit" % "junit" % "4.11" % "test"

resolvers += "spray nightlies repo" at "http://nightlies.spray.io"

resolvers += "spray repo" at "http://repo.spray.io"

seq(Revolver.settings: _*)
