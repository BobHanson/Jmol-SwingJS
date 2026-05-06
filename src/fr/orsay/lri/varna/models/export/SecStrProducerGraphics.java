package fr.orsay.lri.varna.models.export;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.GeneralPath;
import java.io.FileWriter;
import java.io.IOException;

import fr.orsay.lri.varna.exceptions.ExceptionWritingForbidden;

public class SecStrProducerGraphics implements VueVARNAGraphics{
	SecStrDrawingProducer _ss;
	double _thickness;
	Color _color;
	
	public SecStrProducerGraphics(SecStrDrawingProducer ss)
	{
		_ss = ss;
	}

	@Override
  public void draw(GeneralPath s) {
			_ss.fillPolygon(s, getColor());
	}
	
	@Override
  public void drawArc(double x, double y, double rx, double ry,
			double angleStart, double angleEnd) {
		
		
	}
	
	@Override
  public void drawLine(double x1, double y1, double x2, double y2) {
		_ss.drawLine(x1, -y1, x2, -y2, _thickness);
	}
	
	@Override
  public void drawCircle(double x, double y, double r) {
		_ss.drawCircle(x+0.5*r, -y-0.5*r, 0.5*r, _thickness);
	}

	@Override
  public void drawRect(double x, double y, double w, double h) {
		
		
	}
	
	@Override
  public void drawRoundRect(double x, double y, double w, double h,
			double rx, double ry) {
		
		
	}

	@Override
  public void drawStringCentered(String res, double x, double y) {
		_ss.drawText(x, -y, res);
	}

	@Override
  public void fill(GeneralPath s) {
		_ss.fillPolygon(s, getColor());
	}
	
	@Override
  public void fillCircle(double x, double y, double r) {
		_ss.fillCircle(x+0.5*r, -y-0.5*r, 0.5*r,  _thickness, _ss.getCurrentColor());
	}

	@Override
  public void fillRect(double x, double y, double w, double h) {
		
		
	}

	@Override
  public void fillRoundRect(double x, double y, double w, double h,
			double rx, double ry) {
		
		
	}
	@Override
  public Color getColor() {
		return _ss.getCurrentColor();
	}
	
	@Override
  public Dimension getStringDimension(String s) {
		
		return null;
	}

	@Override
  public void setColor(Color c) {
		_ss.setColor(c);
	}
	
	@Override
  public void setSelectionStroke() {
		
		
	}
	
	@Override
  public void setFont(Font f) {
		//System.out.println("Font "+f.getSize2D());
		_ss.setFont(SecStrDrawingProducer.FONT_HELVETICA_BOLD,f.getSize2D());
	}

	@Override
  public void setPlainStroke() {
		
		
	}

	@Override
  public void setStrokeThickness(double t) {
		_thickness = t;
	}
	
	public void saveToDisk(String path) throws ExceptionWritingForbidden
	{
		FileWriter fout;
		try {
			fout = new FileWriter(path);
			fout.write(_ss.export());
			fout.close();
		} catch (IOException e) {
			throw new ExceptionWritingForbidden(e.getMessage());
		}
	}

  @Override
  public boolean isVarnaPanel() {
    // irrelevant
    return true;
  }
}
