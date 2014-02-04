object Settings {
  val project       = "tarot"

  val version       = "1.0-SNAPSHOT"

  val organization  = "com.guidewire.tarot"

  val scalaVersion  = "2.10.2"

  val scalacOptions = Seq("-deprecation", "-unchecked", "-feature", "-Xelide-below", "900")
  val javacOptions  = Seq("-Xlint:unchecked")

  def prompt        = GitPrompt.build
}

