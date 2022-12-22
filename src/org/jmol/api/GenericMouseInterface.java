package org.jmol.api;

import java.awt.event.KeyListener;

public interface GenericMouseInterface extends KeyListener {

  boolean processEvent(int id, int x, int y, int modifiers, long time);

  void clear();

  void dispose();

	void processTwoPointGesture(double[][][] touches);

  void processKeyEvent(Object event);
}
