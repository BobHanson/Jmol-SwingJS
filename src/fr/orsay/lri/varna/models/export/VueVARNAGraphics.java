package fr.orsay.lri.varna.models.export;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;

public interface VueVARNAGraphics {
	public Dimension getStringDimension(String s);
	public void drawStringCentered(String res, double x, double y);
	public void setColor(Color c);
	public Color getColor();
	public void drawLine(double x1, double y1, double x2, double y2);
	public void drawRect(double x, double y, double w, double h);
	public void fillRect(double x, double y, double w, double h);
	public void drawCircle(double x, double y, double r);
	public void fillCircle(double x, double y, double r);
	public void drawRoundRect(double x, double y, double w, double h, double rx, double ry); 
	public void fillRoundRect(double x, double y, double w, double h, double rx, double ry); 
	public void drawArc(double x, double y, double rx, double ry, double angleStart, double angleEnd);
	//public void drawString(String s, double x, double y);
	public void draw(GeneralPath s);
	public void fill(GeneralPath s);
	public void setFont(Font f);
	public void setSelectionStroke();
	public void setPlainStroke();
	public void setStrokeThickness(double t);

}
