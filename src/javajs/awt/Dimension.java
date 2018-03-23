package javajs.awt;

public class Dimension {

  public int width;
  public int height;

  public Dimension(int w, int h) {
    set(w, h);
  }

  public Dimension set(int w, int h) {
    width = w;
    height = h;
    return this;
  }

}
