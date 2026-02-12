package fr.orsay.lri.varna.models.rna;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.Serializable;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class VARNAPoint implements Serializable {

	private static final long serialVersionUID = 8815373295131046029L;
	
	public double x = 0.0;
	public double y = 0.0;
	
	public void toXML(TransformerHandler hd) throws SAXException
	{
		toXML(hd,"");
	}	
	

	public static String XML_ELEMENT_NAME = "p";
	public static String XML_VAR_ROLE_NAME = "r";
	public static String XML_VAR_X_NAME = "x";
	public static String XML_VAR_Y_NAME = "y";

	public void toXML(TransformerHandler hd, String role) throws SAXException
	{
		AttributesImpl atts = new AttributesImpl();
		if (!role.equals(""))
		{
			atts.addAttribute("","",XML_VAR_ROLE_NAME,"CDATA",""+role);
		}
		atts.addAttribute("","",XML_VAR_X_NAME,"CDATA",""+x);
		atts.addAttribute("","",XML_VAR_Y_NAME,"CDATA",""+y);
		hd.startElement("","",XML_ELEMENT_NAME,atts);
		hd.endElement("","",XML_ELEMENT_NAME);
	}	

	public VARNAPoint()
	{ this(0.0,0.0); }
	
	public VARNAPoint(double px, double py)
    {
    	x = px; y = py;
    }
    public VARNAPoint(Point2D.Double p)
    {
    	this(p.x,p.y);
    }
 
    public double getX()
    {
    	return x;
    }
    
    public double getY()
    {
    	return y;
    }
    
    public Point2D.Double toPoint2D()
    {
    	return new Point2D.Double(x,y);
    }
    
    public String toString()
    {
    	return "("+x+","+y+")" ;
    }
}
