package fr.orsay.lri.varna.models.annotations;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.Serializable;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.utils.XMLUtils;


public class ChemProbAnnotation implements Serializable {

	public static final String HEADER_TEXT = "ChemProbAnnotation";

	/**
	 * 
	 */
	private static final long serialVersionUID = 5833315460145031242L;


	public enum ChemProbAnnotationType 
	{
		TRIANGLE,
		ARROW,
		PIN,
		DOT;
	};
	
	public static double DEFAULT_INTENSITY = 1.0;
	public static ChemProbAnnotationType DEFAULT_TYPE = ChemProbAnnotationType.ARROW;
	public static Color DEFAULT_COLOR = Color.blue.darker();

	private ModeleBase _mbfst;
	private ModeleBase _mbsnd;
	private Color _color;
	private double _intensity;
	private ChemProbAnnotationType _type;
	private boolean _outward;
	
	public static String XML_ELEMENT_NAME = "ChemProbAnnotation";
	public static String XML_VAR_INDEX5_NAME = "Index5";
	public static String XML_VAR_INDEX3_NAME = "Index3";
	public static String XML_VAR_COLOR_NAME = "Color";
	public static String XML_VAR_INTENSITY_NAME = "Intensity";
	public static String XML_VAR_TYPE_NAME = "Type";
	public static String XML_VAR_OUTWARD_NAME = "Outward";

	public void toXML(TransformerHandler hd) throws SAXException
	{
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("","",XML_VAR_INDEX5_NAME,"CDATA",""+_mbfst.getIndex());
		atts.addAttribute("","",XML_VAR_INDEX3_NAME,"CDATA",""+_mbsnd.getIndex());
		atts.addAttribute("","",XML_VAR_COLOR_NAME,"CDATA",XMLUtils.toHTMLNotation(_color));
		atts.addAttribute("","",XML_VAR_INTENSITY_NAME,"CDATA",""+_intensity);
		atts.addAttribute("","",XML_VAR_TYPE_NAME,"CDATA",""+_type);
		atts.addAttribute("","",XML_VAR_OUTWARD_NAME,"CDATA",""+_outward);
		hd.startElement("","",XML_ELEMENT_NAME,atts);
		hd.endElement("","",XML_ELEMENT_NAME);
	}

	
	public ChemProbAnnotation(ModeleBase mbfst, ModeleBase mbsnd, String styleDesc) {
		this(mbfst,mbsnd);
		applyStyle(styleDesc);
	}

	public ChemProbAnnotation(ModeleBase mbfst, ModeleBase mbsnd) {
		this(mbfst,mbsnd,ChemProbAnnotation.DEFAULT_TYPE,ChemProbAnnotation.DEFAULT_INTENSITY);
	}

	public ChemProbAnnotation(ModeleBase mbfst, ModeleBase mbsnd, double intensity) {
		this(mbfst,mbsnd,ChemProbAnnotation.DEFAULT_TYPE,intensity);
	}

	public ChemProbAnnotation(ModeleBase mbfst, ModeleBase mbsnd, ChemProbAnnotationType type) {
		this(mbfst,mbsnd,type,ChemProbAnnotation.DEFAULT_INTENSITY);
	}
	
	public ChemProbAnnotation(ModeleBase mbfst, ModeleBase mbsnd, ChemProbAnnotationType type, double intensity) {
		this(mbfst,mbsnd, type, intensity, DEFAULT_COLOR, true);
	}

	public ChemProbAnnotation(ModeleBase mbfst, ModeleBase mbsnd, ChemProbAnnotationType type, double intensity, Color color, boolean out) {
		if (mbfst.getIndex()>mbsnd.getIndex())
		{
			ModeleBase tmp = mbsnd;
			mbsnd = mbfst;
			mbfst = tmp;
		}
		_mbfst = mbfst;
		_mbsnd = mbsnd;
		_type = type;
		_intensity = intensity;
		_color = color;
		_outward = out;
	}

	public boolean isOut()
	{
		return _outward; 
	}
	
	public void setOut(boolean b)
	{
		_outward = b;
	}

	public Color getColor()
	{
		return _color; 
	}

	public double getIntensity()
	{
		return _intensity; 
	}
	
	public ChemProbAnnotationType getType()
	{
		return _type; 
	}
	
	public void setColor(Color c){
		_color = c;
	}

	public void setIntensity(double d){
		_intensity = d;
	}
	
	public Point2D.Double getAnchorPosition()
	{
		Point2D.Double result = new Point2D.Double(
				(_mbfst.getCoords().x+_mbsnd.getCoords().x)/2.0,
				(_mbfst.getCoords().y+_mbsnd.getCoords().y)/2.0);
		return result;
	}
	
	public Point2D.Double getDirVector()
	{
		Point2D.Double norm = getNormalVector();
		Point2D.Double result = new Point2D.Double(-norm.y,norm.x);
		Point2D.Double anchor = getAnchorPosition();
		Point2D.Double center = new Point2D.Double(
				(_mbfst.getCenter().x+_mbsnd.getCenter().x)/2.0,
				(_mbfst.getCenter().y+_mbsnd.getCenter().y)/2.0);
		Point2D.Double vradius = new Point2D.Double(
				(center.x-anchor.x)/2.0,
				(center.y-anchor.y)/2.0);
		if (_outward)
		{
		  if (result.x*vradius.x+result.y*vradius.y>0)
		  {
			return new Point2D.Double(-result.x,-result.y);
		  }
		}
		else
		{
			  if (result.x*vradius.x+result.y*vradius.y<0)
			  {
				return new Point2D.Double(-result.x,-result.y);
			  }			
		}
		return result;		
	}
	public Point2D.Double getNormalVector()
	{
		Point2D.Double tmp;
		if (_mbfst==_mbsnd)
		{
			tmp = new Point2D.Double(
					(-(_mbsnd.getCenter().y-_mbsnd.getCoords().y)),
					((_mbsnd.getCenter().x-_mbsnd.getCoords().x)));			
		}
		else
		{
			tmp = new Point2D.Double(
				(_mbsnd.getCoords().x-_mbfst.getCoords().x)/2.0,
				(_mbsnd.getCoords().y-_mbfst.getCoords().y)/2.0);
		}
		
		double norm = tmp.distance(0, 0);
		Point2D.Double result = new Point2D.Double(tmp.x/norm,tmp.y/norm);
		return result;				
	}
	
	public static ChemProbAnnotationType annotTypeFromString(String value)
	{
		if (value.toLowerCase().equals("arrow"))
		{return ChemProbAnnotationType.ARROW;}
		else if (value.toLowerCase().equals("triangle"))
		{return ChemProbAnnotationType.TRIANGLE;}
		else if (value.toLowerCase().equals("pin"))
		{return ChemProbAnnotationType.PIN;}
		else if (value.toLowerCase().equals("dot"))
		{return ChemProbAnnotationType.DOT;}
		else
		{return ChemProbAnnotationType.ARROW;}
	}
	
	
	public void applyStyle(String styleDesc)
	{
		String[] chemProbs = styleDesc.split(",");
		for (int i = 0; i < chemProbs.length; i++) {
			String thisStyle = chemProbs[i];
			String[] data = thisStyle.split("=");
			if (data.length==2)
			{
				String name = data[0];
				String value = data[1];
				if (name.toLowerCase().equals("color"))
				{
					Color c = Color.decode(value);
					if (c==null)
					{ c = _color; }
					setColor(c);
				}
				else if (name.toLowerCase().equals("intensity"))
				{
					_intensity = Double.parseDouble(value);
				}
				else if (name.toLowerCase().equals("dir"))
				{
					_outward = value.toLowerCase().equals("out");
				}
				else if (name.toLowerCase().equals("glyph"))
				{
					_type= annotTypeFromString(value);
				}
			}
		}
	}
	
	public void setType(ChemProbAnnotationType s)
	{
		_type = s;
	}

	public ChemProbAnnotation clone()
	{
		ChemProbAnnotation result = new ChemProbAnnotation(this._mbfst,this._mbsnd);
		result._intensity = _intensity;
		result._type = _type;
		result._color= _color;
		result._outward = _outward;
		return result;
	}
	
	public String toString()
	{
		return "Chem. prob. "+this._type+" Base#"+this._mbfst.getBaseNumber()+"-"+this._mbsnd.getBaseNumber();
	}
	
}
