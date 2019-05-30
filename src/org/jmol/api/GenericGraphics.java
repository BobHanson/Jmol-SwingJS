package org.jmol.api;

import javajs.api.GenericColor;
import org.jmol.awtjs.swing.Font;



public interface GenericGraphics {

	GenericColor getColor1(int argb);

	GenericColor getColor3(int red, int green, int blue);
	
	GenericColor getColor4(int r, int g, int b, int a);

	Object newGrayScaleImage(Object g, Object image, int width, int height, int[] buffer);


	boolean canDoLineTo();

	void doStroke(Object g, boolean isBegin);

  void drawGrayScaleImage(Object g, Object image2D, int destX0, int destY0, int destX1, int destY1, int srcX0, int srcY0, int srcX1, int srcY1);

	void drawLine(Object g, int x0, int y0, int x1, int y1);

	void drawCircle(Object g, int x, int y, int diameter);

	void drawPolygon(Object g, int[] axPoints, int[] ayPoints, int nPoints);

	void drawRect(Object g, int xPixel, int yPixel,
			int xPixels, int yPixels);

	void drawString(Object g, String s, int x, int y);

	void drawStringRotated(Object g, String s, int x, int y, double angle);

	void fillCircle(Object g, int x, int y, int diameter);

	void fillPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints);

	void fillRect(Object g, int x, int y, int width, int height);
	
	void fillBackground(Object g, GenericColor bgcolor);

	void lineTo(Object g, int x2, int y2);

	void setGraphicsColor(Object g, GenericColor c);

	Font setFont(Object g, Font font);
	
	void setStrokeBold(Object g, boolean tf);

	void translateScale(Object g, double x, double y, double scale);

	void setWindowParameters(int width, int height);


}
