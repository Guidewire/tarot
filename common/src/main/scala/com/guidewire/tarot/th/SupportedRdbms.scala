package com.guidewire.tarot.th

/**
 * Provides a versioned list of supported RDBMSes that TH supports.
 *
 * This list would grow over time and it's best, if possible, to treat
 * it as a [[scala.collection.immutable.Stream]] of some arbitrary size.
 */
object SupportedRdbms {
  lazy val SQL_SERVER:Stream[Rdbms] = Stream(
      Rdbms("Sql Server 2008",    RdbmsVendor.SqlServer, "10.0")
    , Rdbms("Sql Server 2008 R2", RdbmsVendor.SqlServer, "10.50")
    , Rdbms("Sql Server 2012",    RdbmsVendor.SqlServer, "11.0")
  )

  lazy val ORACLE:Stream[Rdbms] = Stream(
      Rdbms("Oracle 10g", RdbmsVendor.Oracle, "10.1")
    , Rdbms("Oracle 11g", RdbmsVendor.Oracle, "11.1")
  )

  lazy val DB2:Stream[Rdbms] = Stream(
      Rdbms("DB2 9.0",  RdbmsVendor.DB2, "9.0")
    , Rdbms("DB2 10.0", RdbmsVendor.DB2, "10.0")
  )

  lazy val H2:Stream[Rdbms] = Stream(
      Rdbms("H2", RdbmsVendor.H2, "1.0")
  )

  def all():Stream[Rdbms] = Stream(
      SQL_SERVER
    , ORACLE
    , DB2
    , H2
  ).flatten
}
