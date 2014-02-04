package com.guidewire.tarot.common

/**
 * Describes something that is versioned with an instance of [[com.guidewire.tarot.common.Version]].
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
trait Versioned[T] {
  def title:String
  def value:T
  def version:Version
}
