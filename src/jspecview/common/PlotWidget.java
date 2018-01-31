package jspecview.common;

/**
 * Stores information about pins and zoom boxes,
 * which can be locked upon mousePressed events
 * and released upon mouseReleased events
 * We need actual x and y as well as pixel positions here
 * .
 */
class PlotWidget extends Coordinate {

  int xPixel0;
  int yPixel0;
  int xPixel1;
  int yPixel1;
  
  boolean isPin;
  boolean isPinOrCursor;
  boolean isXtype;
  boolean is2D;
  boolean is2Donly;
  boolean isEnabled = true;
  boolean isVisible = false;
  
  private String name;
	
  ScriptToken color = ScriptToken.PLOTCOLOR;
  
  @Override
  public String toString() {
    return name
        + (!isPinOrCursor ? "" + xPixel0 + " " + yPixel0 + " / " + xPixel1 + " "
            + yPixel1 : " x=" + getXVal() + "/" + xPixel0 + " y=" + getYVal()
            + "/" + yPixel0);
  }
 
  PlotWidget(String name) {
    this.name = name;
    isPin = (name.charAt(0) == 'p');
    isPinOrCursor = (name.charAt(0) != 'z');
    isXtype = (name.indexOf("x") >= 0);
    is2D = (name.indexOf("2D") >= 0);
    is2Donly = (is2D && name.charAt(0) == 'p');
  }

  boolean selected(int xPixel, int yPixel) {
    return (isVisible 
    		&& Math.abs(xPixel - xPixel0) < 5 && Math.abs(yPixel - yPixel0) < 5);
  }

  void setX(double x, int xPixel) {
    setXVal(x);
    //System.out.println("widget " + this);
    xPixel0 = xPixel1 = xPixel;
  }

  void setY(double y, int yPixel) {
    setYVal(y);
    yPixel0 = yPixel1 = yPixel;
  }

  double getValue() {
    return (isXtype ? getXVal() : getYVal());
  }

  void setEnabled(boolean enabled) {
    isEnabled = enabled;
  }

}