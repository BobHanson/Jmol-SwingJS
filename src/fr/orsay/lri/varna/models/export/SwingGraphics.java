package fr.orsay.lri.varna.models.export;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Arc2D.Double;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

public class SwingGraphics implements VueVARNAGraphics {
	private BasicStroke _dashedStroke;
	private BasicStroke _plainStroke;
	Graphics2D _g2d;
	private boolean _debug = false;

	
	public SwingGraphics(Graphics2D g2d)
	{
		_g2d = g2d;
		float[] dash = { 5.0f, 5.0f };
		_dashedStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND,	BasicStroke.JOIN_ROUND, 3.0f, dash, 0);
		_plainStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND,	BasicStroke.JOIN_ROUND, 3.0f);
	}
	
	public Dimension getStringDimension(String s) {
		FontMetrics fm = _g2d.getFontMetrics();
		Rectangle2D r = fm.getStringBounds(s, _g2d);
		return (new Dimension((int) r.getWidth(), (int) fm.getAscent()
				- fm.getDescent()));
	}

	public void drawStringCentered(String res, double x,
			double y) {
		Dimension d = getStringDimension(res);
		x -= (double) d.width / 2.0;
		y += (double) d.height / 2.0;
		if (_debug)
		{
		    Stroke bck = _g2d.getStroke();
		    _g2d.setStroke(_plainStroke);
		    _g2d.draw(new Rectangle2D.Double(x, y - d.height, d.width, d.height));
		    _g2d.setStroke(bck);
		}
		_g2d.drawString(res, (float) (x), (float) (y));
	}

	public void draw(GeneralPath s) {
		_g2d.draw(s);
	}

	public void drawArc(double x, double y, double rx, double ry,
			double angleStart, double angleEnd) {
		_g2d.drawArc((int) (x-rx/2.), (int) (y-ry/2.), (int) rx, (int) ry, (int) angleStart, (int) angleEnd);	
	}

	public void drawLine(double x1, double y1, double x2, double y2) {
		_g2d.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
	}

	public void drawCircle(double x, double y, double r) {
		_g2d.draw(new Ellipse2D.Double(x, y, r, r));
	}

	public void drawRect(double x, double y, double w, double h) {
		_g2d.drawRect((int)x, (int)y, (int)w, (int)h);
	}

	public void drawRoundRect(double x, double y, double w, double h,
			double rx, double ry) {
		_g2d.drawRoundRect((int)x, (int)y, (int)w, (int)h, (int)rx, (int)ry);
	}

	public void drawString(String s, double x, double y) {
		_g2d.drawString(s, (float)x, (float)y);
	}

	public void fill(GeneralPath s) {
		_g2d.fill(s);
	}

	public void fillCircle(double x, double y, double r) {
		_g2d.fill(new Ellipse2D.Double(x, y, r, r));
	}

	public void fillRect(double x, double y, double w, double h) {
		_g2d.fill(new Rectangle2D.Double(x, y, w, h));
	}

	public void fillRoundRect(double x, double y, double w, double h,
			double rx, double ry) {
		_g2d.fillRoundRect((int)x, (int)y, (int)w, (int)h, (int)rx, (int)ry);
	}

	public Color getColor() {
		return _g2d.getColor();
	}

	public void setColor(Color c) {
		_g2d.setColor(c);
	}

	public void setSelectionStroke() {
		_g2d.setStroke(_dashedStroke);
	}

	public void setFont(Font f) {
		_g2d.setFont(f);
	}

	public void setPlainStroke() {
		_g2d.setStroke(_plainStroke);
	}
	
	private BasicStroke deriveStroke(BasicStroke s, double t)
	{
		return new BasicStroke((float)t, s.getEndCap(), s.getLineJoin(), s.getMiterLimit(), s.getDashArray(), s.getDashPhase()) ;
	}

	public void setStrokeThickness(double t) {
		boolean dashed = (_g2d.getStroke()==_dashedStroke); 
		_plainStroke = deriveStroke(_plainStroke, t);
		//_dashedStroke = deriveStroke(_dashedStroke, t);
		if(dashed)
		{  _g2d.setStroke(_dashedStroke); }
		else
		{ _g2d.setStroke(_plainStroke);	}
	}

}
