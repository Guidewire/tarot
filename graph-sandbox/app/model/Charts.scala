package model

import com.guidewire.tarot.chart.Chart
import org.joda.time.ReadableDateTime

case class DateTimeHistogramChart(
  id:String,
  model:Chart[ReadableDateTime, Double]
)

case class DoubleLineChart(
  id:String,
  xMin:Double,
  yMin:Double,
  model:Chart[Double, Double]
)