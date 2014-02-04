import sbt._
import Keys._
import play.Project._

object ProjectTemplates {
  object Root {
    def apply(aggregate:ProjectReference*):Project =
      apply("", aggregate)

    def apply(name:String, aggregate:Seq[ProjectReference]):Project = Project(
      id        = ThisProject.root(name),
      base      = ThisProject.root.base(),
      settings  = ThisProject.root.settings,

      aggregate = aggregate
    )
  }

  object Module {
    def apply(name:String):Project =
      apply("", name)

    def apply(prefix:String, name:String):Project = Project(
      id        = ThisProject.module(prefix, name),
      base      = ThisProject.module.base(name),
      settings  = ThisProject.module.settings
    )
  }

  object Play {
    def apply(name:String):Project =
      apply("", name)

    def apply(prefix:String, name:String):Project = play.Project(
      name         = ThisProject.play(prefix, name),
      path         = ThisProject.play.base(name),
      settings     = ThisProject.play.settings,
      dependencies = ThisProject.play.dependencies
    )
      .settings(version <<= ThisProject.play.version)
  }
}

