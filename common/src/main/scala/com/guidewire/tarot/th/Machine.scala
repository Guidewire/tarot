package com.guidewire.tarot.th

import org.joda.time.DateTime

case class Machine(
  name:String,
  active:Boolean,

  dbms:Seq[Rdbms],
  jvms:Seq[Jvm],
  appServers:Seq[AppServer],
  browsers:Seq[Browser],
  status:MachineStatus.EnumVal,
  machineType:MachineType.EnumVal,

  load:Int,
  maxLoad:Int,

  lastUpdated:DateTime
)
