package com.guidewire.tarot.metrics

class ExampleMetricsLogs

object ExampleMetricsLogs {
  val METRICS_LOG_FILE_NAMES = Seq(
      "tarot-queue-2013-6-21.log"
    , "tarot-queue-2013-6-26.log"
  )

  val METRICS_LOG_FILE_URIS = {
    for {
      name <- METRICS_LOG_FILE_NAMES
      uri = classOf[ExampleMetricsLogs].getResource(name).toURI
    } yield name -> uri
  }.toMap

  val FIRST_METRICS_LOG_FILE_NAME =
    METRICS_LOG_FILE_NAMES.head

  val FIRST_METRICS_LOG_FILE_URI =
    METRICS_LOG_FILE_URIS.head._2
}
