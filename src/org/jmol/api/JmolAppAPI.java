package org.jmol.api;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

public interface JmolAppAPI {

  Dimension getHistoryWindowSize(String windowName);

  Point getHistoryWindowPosition(String windowName);

  void addHistoryWindowInfo(String name, Component window, Point border);

}
