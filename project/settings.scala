object Settings {
  val project       = "tarot"

  val company       = "Guidewire"

  val organization  = "com.guidewire"

  val scalaVersion  = "2.10.2"

  val scalacOptions = Seq("-deprecation", "-unchecked", "-feature", "-Xelide-below", "900")
  val javacOptions  = Seq("-Xlint:unchecked")

  def prompt        = GitPrompt.build
}

