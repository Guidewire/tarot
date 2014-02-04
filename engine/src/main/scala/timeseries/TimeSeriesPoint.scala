package com.guidewire.tarot.timeseries
import org.joda.time.DateTime

private[timeseries]
sealed case class TimeSeriesPoint[V] private[timeseries]
(time: DateTime, value: V)
