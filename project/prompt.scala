import sbt._
import Keys._

object GitPrompt {
  object devnull extends ProcessLogger {
    def info(s: => String) {}
    def error (s: => String) {}
    def buffer[T] (f: => T): T = f
  }

  def currBranch = (
    ("git status -sb" lines_! devnull headOption) map { line =>
      if ("" == line || line.startsWith("## Initial commit"))
        ":master"
      else
        ":" + (line.stripPrefix("## "))
    } getOrElse ""
  )

  def currCommit = (
    ("git log --pretty=format:'%h' -n 1" lines_! devnull headOption) map { line =>
      line match {
        case l if l.length > 2 && l.startsWith("'") => "@" + l.substring(1, l.length - 2)
        case _ => "@unknown"
      }
    } getOrElse ""
  )

  val build = {
    (state: State) => {
      val currProject = Project.extract(state).currentRef.project
      "%s%s%s> ".format(
        currProject, currBranch, currCommit
      )
    }
  }
}

