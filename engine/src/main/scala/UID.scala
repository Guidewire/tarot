package com.guidewire.tarot

/** Immutable unique identifier
  *
  * Immutable wrapper around arbitrary identifier types.
  * In this way, the host application can identify entities by strings,
  * integers, or other custom types.
  * In order to behave well, the underlying type should:
  *  - be immutable
  *  - have well-defined equality
  *  - be hashable
  *
  * =Discussion=
  * In order to interface with the outside world,
  * the following objects are given "unique identifiers":
  *  - [[MachineKind]]
  *  - [[SuiteKind]]
  *  - [[TrackObject]]
  *
  * For the most part, these identifiers are supplied by the host application.
  * [[UID]] is used by the application to communicate updates to Tarot
  * (i.e. ''which'' [[TrackObject]] instance changed?)
  * and by Tarot to convey recommendations to the application
  * (i.e. create a new machine of ''this'' [[MachineKind]]).
  *
  * @tparam T underlying type
  *
  * @note If two [[UID]] instances are equal,
  * then they should have the same referent. Otherwise bad things will happen.
  */
sealed case class UID[+T] (val value: T) {
  /**
   * Creates a value that's more friendly but reduces its uniqueness by making it
   * case-insensitive.
   */
  def friendlyValue: String = value.toString.replaceAll(" ", "-").toLowerCase
}
