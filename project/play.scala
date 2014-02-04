import sbt._
import play.Project._

object PlaySettings {
  val dependencies = Seq(
      jdbc
    , anorm

//    , "org.scala-sbt" % "sbt" % "0.13.0-RC5" withSources()
//    , "org.scala-sbt" % "launcher" % "0.13.0-RC5" withSources()
//    , "org.fluentlenium" % "fluentlenium-core" % "0.7.8"
//    , "fr.greweb" %% "playcli" % "0.1"
//    , "org.scalatest" % "scalatest_2.10" % "1.9.1" withSources()
  )

  val defaults = Seq(

  )
}
