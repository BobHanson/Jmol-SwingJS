package org.jmol.api;

public interface GenericMouseInterface {

  boolean processEvent(int id, int x, int y, int modifiers, long time);

  void clear();

  void dispose();

	void processTwoPointGesture(double[][][] touches);

}
