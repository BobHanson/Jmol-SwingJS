package org.jmol.g3d;

import java.util.Comparator;

import javajs.awt.Font;
import javajs.util.P3i;


class TextString extends P3i implements Comparator<TextString> {
  
  String text;
  Font font;
  int argb, bgargb;

  void setText(String text, Font font, int argb, int bgargb, int x, int y, int z) {
    this.text = text;
    this.font = font;
    this.argb = argb;
    this.bgargb = bgargb;
    this.x = x;
    this.y = y;
    this.z = z;
  }
  
  @Override
  public int compare(TextString a, TextString b) {
    return (a == null || b == null ? 0 : a.z > b.z ? -1 : a.z < b.z ? 1 : 0);
  }

  @Override
  public String toString() {
    return super.toString() + " " + text;
  }
}
