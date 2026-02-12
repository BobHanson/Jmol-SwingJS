package fr.orsay.lri.varna.utils;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Formatter;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.RNA;

public class XMLUtils {
	public static String toHTMLNotation(Color c)
	{
		Formatter f = new Formatter();
		f.format("#%02X%02X%02X", c.getRed(),c.getGreen(),c.getBlue());
		return f.toString();
	}

	public static void toXML(TransformerHandler hd, Font f) throws SAXException
	{
		toXML(hd, f,"");
	}
	

	public static String XML_BASELIST_ELEMENT_NAME = "baselist";
	public static String XML_FONT_ELEMENT_NAME = "font";
	public static String XML_ROLE_NAME = "role";
	public static String XML_NAME_NAME = "name";
	public static String XML_FAMILY_NAME = "family";
	public static String XML_STYLE_NAME = "style";
	public static String XML_SIZE_NAME = "size";
	
	public static void toXML(TransformerHandler hd, Font f, String role) throws SAXException
	{
		AttributesImpl atts = new AttributesImpl();
		if (!role.equals(""))
		  atts.addAttribute("","",XML_ROLE_NAME,"CDATA",""+role);
		atts.addAttribute("","",XML_NAME_NAME,"CDATA",""+f.getName());
		//atts.addAttribute("","",XML_FAMILY_NAME,"CDATA",""+f.getFamily());
		atts.addAttribute("","",XML_STYLE_NAME,"CDATA",""+f.getStyle());
		atts.addAttribute("","",XML_SIZE_NAME,"CDATA",""+f.getSize2D());
		hd.startElement("","",XML_FONT_ELEMENT_NAME,atts);
		hd.endElement("","",XML_FONT_ELEMENT_NAME);
	}

	public static Font getFont(String qName, Attributes attributes)
	{
		if (qName.equals(XMLUtils.XML_FONT_ELEMENT_NAME)){
			int style = Integer.parseInt(attributes.getValue(XMLUtils.XML_STYLE_NAME));			
			String name = (attributes.getValue(XMLUtils.XML_NAME_NAME));			
			double size = Double.parseDouble(attributes.getValue(XMLUtils.XML_SIZE_NAME));
			Font f = new Font(name, style, (int)size);
			return f.deriveFont((float)size);
		}
		return null;
	}
	

	public static void toXML(TransformerHandler hd, ModeleBase mb) throws SAXException
	{
		ArrayList<ModeleBase> m = new ArrayList<ModeleBase>();
		m.add(mb);
		toXML(hd, m);
	}


	public static void toXML(TransformerHandler hd, ArrayList<ModeleBase> m) throws SAXException
	{		
		AttributesImpl atts = new AttributesImpl();
		String result = "";
		for (ModeleBase mb: m)
		{
			if (!result.equals(""))
				result+= ",";
			result += mb.getIndex();
					
		}
		hd.startElement("","",XML_BASELIST_ELEMENT_NAME,atts);
		exportCDATAString(hd, result);
		hd.endElement("","",XML_BASELIST_ELEMENT_NAME);
	}

	public static ArrayList<ModeleBase> toModeleBaseArray(String baselist, RNA rna)
	{
		ArrayList<ModeleBase> result = new ArrayList<ModeleBase>();
		String[] data = baselist.trim().split(",");
		for(int i=0;i<data.length;i++)
		{
			int index = Integer.parseInt(data[i]);
			result.add(rna.getBaseAt(index));
		}
			
		return result;
	}
	
	public static void exportCDATAElem(TransformerHandler hd,String elem, String s) throws SAXException
	{
		char[] t = s.toCharArray();
		AttributesImpl atts = new AttributesImpl();
		hd.startElement("","",elem,atts);
		hd.startCDATA();
		hd.characters(t, 0, t.length);
		hd.endCDATA();
		hd.endElement("","",elem);
	}
	
	public static void exportCDATAString(TransformerHandler hd, String s) throws SAXException
	{
		char[] t = s.toCharArray();
		hd.startCDATA();
		hd.characters(t, 0, t.length);
		hd.endCDATA();
	}
	public static boolean getBoolean(Attributes attributes, String attName, boolean defVal)
	{
		String val = attributes.getValue(attName);
		if (val!=null)
		{
			return Boolean.parseBoolean(val);
		}
		return defVal;
	}
	public static int getInt(Attributes attributes, String attName, int defVal)
	{
		String val = attributes.getValue(attName);
		if (val!=null)
		{
			return Integer.parseInt(val);
		}
		return defVal;
	}
	public static double getDouble(Attributes attributes, String attName, double defVal)
	{
		String val = attributes.getValue(attName);
		if (val!=null)
		{
			return Double.parseDouble(val);
		}
		return defVal;
	}
	
}
