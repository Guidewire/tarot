package com.guidewire.tarot;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import java.util.concurrent.CopyOnWriteArraySet;

public class LogbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
  public static interface IListener {
    void event(ILoggingEvent e);
  }

  private static final CopyOnWriteArraySet<IListener> listeners = new CopyOnWriteArraySet<IListener>();

  public static void addListener(final IListener listener) {
    listeners.add(listener);
  }

  public static void removeListener(final IListener listener) {
    listeners.remove(listener);
  }

  protected static void notifyListeners(final ILoggingEvent e) {
    for(IListener listener : listeners) {
      listener.event(e);
    }
  }

  @Override
  protected void append(ILoggingEvent e) {
    notifyListeners(e);
  }
}
