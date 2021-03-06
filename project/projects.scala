import sbt._
import Keys._
import play.Project._

object Projects extends Build {

  lazy val aaa_default_project = root

  lazy val root = Project(
    id        = ThisProject.id,
    base      = ThisProject.base,
    settings  = ThisProject.settings,

    aggregate = Seq(common, engine, spray_sandbox, graph_sandbox /*aggregate_play_application*/)
  )

  lazy val common = Project(
    id        = ThisProject.module("common"),
    base      = ThisProject.module.base("common"),
    settings  = ThisProject.module.settings
  )

  lazy val engine = Project(
    id        = ThisProject.module("engine"),
    base      = ThisProject.module.base("engine"),
    settings  = ThisProject.module.settings
  )
    .dependsOn(common % "compile")

  lazy val simulator = Project(
    id        = ThisProject.module("simulator"),
    base      = ThisProject.module.base("simulator"),
    settings  = ThisProject.module.settings
  )
    .dependsOn(engine % "compile")

  lazy val spray_sandbox = Project(
    id        = ThisProject.module("spray-sandbox"),
    base      = ThisProject.module.base("spray-sandbox"),
    settings  = ThisProject.module.settings
  )
    .dependsOn(common    % "compile")
    .dependsOn(engine    % "compile")
    .dependsOn(simulator % "compile")

  lazy val graph_sandbox = play.Project(
    name      = ThisProject.module("graph-sandbox"),
    path      = ThisProject.module.base("graph-sandbox"),
    settings  = ThisProject.module.settings ++ ThisProject.module.playDefaults,
    dependencies = ThisProject.module.playDependencies,
    applicationVersion = ThisProject.module.version
  )
    .dependsOn(common % "compile")

//  lazy val aggregate_play_application = play.Project(
//    name      = ThisProject.module("aggregate-play-application"),
//    path      = ThisProject.module.base("graph-sandbox"),
//    settings  = ThisProject.module.settings ++ ThisProject.module.playDefaults,
//    dependencies = ThisProject.module.playDependencies,
//    applicationVersion = ThisProject.module.version
//  )
//    .dependsOn(common % "test;compile", graph_sandbox % "test;compile")
//    .aggregate(common, graph_sandbox)
}

