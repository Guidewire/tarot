import sbt._
import Keys._

object ThisProject {
  def id       = Settings.project
  def base     = file(".")
  def version  = Settings.version
  def settings = BuildSettings.defaults

  object module {
    def apply(suffix:String) = Settings.project + "-" + suffix

    def base(suffix:String)  = file(suffix)
    def version              = Settings.version
    def settings             = BuildSettings.defaults

    def playDependencies     = PlaySettings.playDependencies
    def playDefaults         = PlaySettings.playDefaults
  }
}
