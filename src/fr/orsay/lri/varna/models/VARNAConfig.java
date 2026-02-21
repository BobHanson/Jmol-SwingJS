/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Université Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.models;

import java.awt.Color;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.models.annotations.HighlightRegionAnnotation;
import fr.orsay.lri.varna.models.annotations.TextAnnotation;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.ModeleBPStyle;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModeleColorMap;
import fr.orsay.lri.varna.utils.XMLUtils;

public class VARNAConfig implements Serializable, Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2853916694420964233L;
	/**
	 * 
	 */
	public static final int MAJOR_VERSION = 3;
	public static final int MINOR_VERSION = 9;
	
	public static String getFullName()
	{
		return "VARNA "+MAJOR_VERSION+"."+MINOR_VERSION;
	}

	/**
	 * Enum types and internal classes
	 */

	public enum BP_STYLE implements Serializable {
		NONE, SIMPLE, RNAVIZ, LW, LW_ALT;
		public String toString()
		{
			switch(this)
			{
				case LW: return "Leontis/Westhof (Centered)";
				case SIMPLE: return "Line";
				case RNAVIZ: return "Circles";
				case NONE: return "None";
				case LW_ALT: return "Leontis/Westhof (End)";
			}
			return "Unspecified";
		}
		public String getOpt()
		{
			switch(this)
			{
				case NONE: return "none";
				case SIMPLE: return "simple";
				case LW: return "lw";
				case RNAVIZ: return "rnaviz";
				case LW_ALT: return "lwalt";
			}
			return "x";
		}
		public static BP_STYLE getStyle(String opt)
		{
			for(BP_STYLE b: BP_STYLE.values())
			{
				if (opt.toLowerCase().equals(b.getOpt().toLowerCase()))
					return b;
			}
			return null;
		}
	};

	/**
	 * Default values for config options
	 */

	public static final double MAX_ZOOM = 60;
	public static final double MIN_ZOOM = 0.5;
	public static final double DEFAULT_ZOOM = 1;
	public static final double MAX_AMOUNT = 2;
	public static final double MIN_AMOUNT = 1.01;
	public static final double DEFAULT_AMOUNT = 1.2;
	public static final double DEFAULT_BP_THICKNESS = 1.0;
	public static final double DEFAULT_DIST_NUMBERS = 3.0;

	public static final int DEFAULT_PERIOD = 10;

	public static final Color DEFAULT_TITLE_COLOR = Color.black;
	public static final Color DEFAULT_BACKBONE_COLOR = Color.DARK_GRAY.brighter();
	public static final Color DEFAULT_BOND_COLOR = Color.blue;
	public static final Color DEFAULT_SPECIAL_BASE_COLOR = Color.green.brighter();
	public static final Color DEFAULT_DASH_BASE_COLOR = Color.yellow.brighter();
	public static final double DEFAULT_BASE_OUTLINE_THICKNESS = 1.5;
	public static final Color BASE_OUTLINE_COLOR_DEFAULT = Color.DARK_GRAY.brighter();
	public static final Color BASE_INNER_COLOR_DEFAULT = new Color(242, 242,242);
	public static final Color BASE_NUMBER_COLOR_DEFAULT = Color.DARK_GRAY;
	public static final Color BASE_NAME_COLOR_DEFAULT = Color.black;
	
	public static final Color DEFAULT_HOVER_COLOR  =  new Color(230, 230,230);

	public static final Color DEFAULT_BACKGROUND_COLOR = Color.WHITE;
	public static final Font DEFAULT_TITLE_FONT = new Font("SansSerif", Font.BOLD,18);
	public static final Font DEFAULT_BASE_FONT = new Font("SansSerif", Font.PLAIN, 18);
	public static final Font DEFAULT_NUMBERS_FONT = new Font("SansSerif",
			Font.BOLD, 18);
	public static final Font DEFAULT_MESSAGE_FONT = Font.decode("dialog-PLAIN-25");
	public static final Color DEFAULT_MESSAGE_COLOR = new Color(230, 230,230);


	public static final BP_STYLE DEFAULT_BP_STYLE = BP_STYLE.LW;

	public static final ModeleColorMap DEFAULT_COLOR_MAP = ModeleColorMap.defaultColorMap();
	public static final Color DEFAULT_COLOR_MAP_OUTLINE = Color.gray;
	public static final double DEFAULT_BP_INCREMENT = 0.65;

	public static double DEFAULT_COLOR_MAP_WIDTH = 120; 
	public static double DEFAULT_COLOR_MAP_HEIGHT = 30; 
	public static double DEFAULT_COLOR_MAP_X_OFFSET = 40; 
	public static double DEFAULT_COLOR_MAP_Y_OFFSET = 0; 
	public static int DEFAULT_COLOR_MAP_STRIPE_WIDTH = 2; 
	public static int DEFAULT_COLOR_MAP_FONT_SIZE = 20; 
	public static Color DEFAULT_COLOR_MAP_FONT_COLOR = Color.gray.darker(); 

	public static double DEFAULT_SPACE_BETWEEN_BASES = 1.0; 

	/**
	 * Various options.
	 */
	
	public static String XML_VAR_DRAW_OUTLINE = "drawoutline";
	public static String XML_VAR_FILL_BASE = "fillbase";
	public static String XML_VAR_AUTO_FIT = "autofit";
	public static String XML_VAR_AUTO_CENTER = "autocenter";
	public static String XML_VAR_MODIFIABLE = "modifiable";
	public static String XML_VAR_ERRORS = "errors";
	public static String XML_VAR_SPECIAL_BASES = "specialbases";
	public static String XML_VAR_DASH_BASES = "dashbases";
	public static String XML_VAR_USE_BASE_BPS = "usebasebps";
	public static String XML_VAR_DRAW_NC = "drawnc";
	public static String XML_VAR_DRAW_NON_PLANAR = "drawnonplanar";
	public static String XML_VAR_SHOW_WARNINGS = "warnings";
	public static String XML_VAR_COMPARISON_MODE = "comparison";
	public static String XML_VAR_FLAT = "flat";
	public static String XML_VAR_DRAW_BACKGROUND = "drawbackground";
	public static String XML_VAR_COLOR_MAP = "drawcm";
	public static String XML_VAR_DRAW_BACKBONE = "drawbackbone";
	
	public static String XML_VAR_CM_HEIGHT = "cmh";
	public static String XML_VAR_CM_WIDTH = "cmw";
	public static String XML_VAR_CM_X_OFFSET = "cmx";
	public static String XML_VAR_CM_Y_OFFSET = "cmy";
	public static String XML_VAR_DEFAULT_ZOOM = "defaultzoom";
	public static String XML_VAR_ZOOM_AMOUNT = "zoominc";
	public static String XML_VAR_BP_THICKNESS = "bpthick";
	public static String XML_VAR_BASE_THICKNESS = "basethick";
	public static String XML_VAR_DIST_NUMBERS = "distnumbers";
	
	public static String XML_VAR_NUM_PERIOD = "numperiod";
	
	public static String XML_VAR_MAIN_BP_STYLE = "bpstyle";

	public static String XML_VAR_CM = "cm";
	
	public static String XML_VAR_BACKBONE_COLOR = "backbonecol";
	public static String XML_VAR_HOVER_COLOR = "hovercol";
	public static String XML_VAR_BACKGROUND_COLOR = "backgroundcol";
	public static String XML_VAR_BOND_COLOR = "bondcol";
	public static String XML_VAR_TITLE_COLOR = "titlecol";
	public static String XML_VAR_SPECIAL_BASES_COLOR = "specialco";
	public static String XML_VAR_DASH_BASES_COLOR = "dashcol";
	public static String XML_VAR_SPACE_BETWEEN_BASES = "spacebetweenbases";
	
	public static String XML_VAR_TITLE_FONT = "titlefont";
	public static String XML_VAR_NUMBERS_FONT = "numbersfont";
	public static String XML_VAR_FONT_BASES = "basefont";
	
	public static String XML_VAR_CM_CAPTION = "cmcaption";
	public static String XML_VAR_TITLE = "title";
	
    
	public boolean _drawOutlineBases = true;
	public boolean _fillBases = true;
	public boolean _autoFit = true;
	public boolean _autoCenter = true;
	public boolean _modifiable = true;
	public boolean _errorsOn = false;
	public boolean _colorSpecialBases = false;
	public boolean _colorDashBases = false;
	public boolean _useBaseColorsForBPs = false;
	public boolean _drawnNonCanonicalBP = true;
	public boolean _drawnNonPlanarBP = true;
	public boolean _showWarnings = false;
	public boolean _comparisonMode = false;
	public boolean _flatExteriorLoop = true;
	public boolean _drawBackground = false;
	public boolean _drawColorMap = false;
	public boolean _drawBackbone = true;
	
	public double _colorMapHeight  = DEFAULT_COLOR_MAP_HEIGHT; 
	public double _colorMapWidth   = DEFAULT_COLOR_MAP_WIDTH; 
	public double _colorMapXOffset = DEFAULT_COLOR_MAP_X_OFFSET; 
	public double _colorMapYOffset = DEFAULT_COLOR_MAP_Y_OFFSET; 
	public double _zoom            = DEFAULT_ZOOM;
	public double _zoomAmount      = DEFAULT_AMOUNT;
	public double _bpThickness     = 1.0;
	public double _baseThickness   = DEFAULT_BASE_OUTLINE_THICKNESS;
	public double _distNumbers     = DEFAULT_DIST_NUMBERS;
	public double _spaceBetweenBases = DEFAULT_SPACE_BETWEEN_BASES;
	
	public int _numPeriod = DEFAULT_PERIOD;
	public BP_STYLE _mainBPStyle = DEFAULT_BP_STYLE;
	
	public ModeleColorMap _cm = DEFAULT_COLOR_MAP;	

	public Color _backboneColor      = DEFAULT_BACKBONE_COLOR;
	public Color _hoverColor         = DEFAULT_HOVER_COLOR;
	public Color _backgroundColor    = DEFAULT_BACKGROUND_COLOR;
	public Color _bondColor          = DEFAULT_BOND_COLOR;
	public Color _titleColor         = DEFAULT_TITLE_COLOR;
	public Color _specialBasesColor  = DEFAULT_SPECIAL_BASE_COLOR;
	public Color _dashBasesColor     = DEFAULT_DASH_BASE_COLOR;
	
	public Font _titleFont           = DEFAULT_TITLE_FONT;
	public Font _numbersFont         = DEFAULT_NUMBERS_FONT;
	public Font _fontBasesGeneral    = DEFAULT_BASE_FONT;
	
	public String _colorMapCaption = "";
	//public String _title = "";

	
	public static String XML_ELEMENT_NAME = "config";
	
	public void toXML(TransformerHandler hd) throws SAXException
	{
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("","",XML_VAR_DRAW_OUTLINE,"CDATA",   ""+_drawOutlineBases);
		atts.addAttribute("","",XML_VAR_FILL_BASE,"CDATA",      ""+_fillBases);
		atts.addAttribute("","",XML_VAR_AUTO_FIT,"CDATA",       ""+_autoFit);
		atts.addAttribute("","",XML_VAR_AUTO_CENTER,"CDATA",    ""+_autoCenter);
		atts.addAttribute("","",XML_VAR_MODIFIABLE,"CDATA",     ""+_modifiable);
		atts.addAttribute("","",XML_VAR_ERRORS,"CDATA",         ""+_errorsOn);
		atts.addAttribute("","",XML_VAR_SPECIAL_BASES,"CDATA",  ""+_colorSpecialBases);
		atts.addAttribute("","",XML_VAR_DASH_BASES,"CDATA",     ""+_colorDashBases);
		atts.addAttribute("","",XML_VAR_USE_BASE_BPS,"CDATA",   ""+_useBaseColorsForBPs);
		atts.addAttribute("","",XML_VAR_DRAW_NC,"CDATA",        ""+_drawnNonCanonicalBP);
		atts.addAttribute("","",XML_VAR_DRAW_NON_PLANAR,"CDATA",""+_drawnNonPlanarBP);
		atts.addAttribute("","",XML_VAR_SHOW_WARNINGS,"CDATA",  ""+_showWarnings);
		atts.addAttribute("","",XML_VAR_COMPARISON_MODE,"CDATA",""+_comparisonMode);
		atts.addAttribute("","",XML_VAR_FLAT,"CDATA",           ""+_flatExteriorLoop);
		atts.addAttribute("","",XML_VAR_DRAW_BACKGROUND,"CDATA",""+_drawBackground);
		atts.addAttribute("","",XML_VAR_COLOR_MAP,"CDATA",      ""+_drawColorMap);
		atts.addAttribute("","",XML_VAR_DRAW_BACKBONE,"CDATA",  ""+_drawBackbone);

		atts.addAttribute("","",XML_VAR_CM_HEIGHT,"CDATA",      ""+_colorMapHeight);
		atts.addAttribute("","",XML_VAR_CM_WIDTH,"CDATA",       ""+_colorMapWidth);
		atts.addAttribute("","",XML_VAR_CM_X_OFFSET,"CDATA",    ""+_colorMapXOffset);
		atts.addAttribute("","",XML_VAR_CM_Y_OFFSET,"CDATA",    ""+_colorMapYOffset);
		atts.addAttribute("","",XML_VAR_DEFAULT_ZOOM,"CDATA",   ""+_zoom);
		atts.addAttribute("","",XML_VAR_ZOOM_AMOUNT,"CDATA",    ""+_zoomAmount);
		atts.addAttribute("","",XML_VAR_BP_THICKNESS,"CDATA",   ""+_bpThickness);
		atts.addAttribute("","",XML_VAR_BASE_THICKNESS,"CDATA", ""+_baseThickness);
		atts.addAttribute("","",XML_VAR_DIST_NUMBERS,"CDATA",   ""+_distNumbers);
		atts.addAttribute("","",XML_VAR_SPACE_BETWEEN_BASES,"CDATA",   ""+_spaceBetweenBases);
		
			
		atts.addAttribute("","",XML_VAR_NUM_PERIOD,"CDATA",     ""+_numPeriod);
			
		atts.addAttribute("","",XML_VAR_MAIN_BP_STYLE,"CDATA",  ""+_mainBPStyle.getOpt());
			
		atts.addAttribute("","",XML_VAR_BACKBONE_COLOR,"CDATA",     XMLUtils.toHTMLNotation(_backboneColor));
		atts.addAttribute("","",XML_VAR_HOVER_COLOR,"CDATA",        XMLUtils.toHTMLNotation(_hoverColor));
		atts.addAttribute("","",XML_VAR_BACKGROUND_COLOR,"CDATA",   XMLUtils.toHTMLNotation(_backgroundColor));
		atts.addAttribute("","",XML_VAR_BOND_COLOR,"CDATA",         XMLUtils.toHTMLNotation(_bondColor));
		atts.addAttribute("","",XML_VAR_TITLE_COLOR,"CDATA",        XMLUtils.toHTMLNotation(_titleColor));
		atts.addAttribute("","",XML_VAR_SPECIAL_BASES_COLOR,"CDATA",XMLUtils.toHTMLNotation(_specialBasesColor));
		atts.addAttribute("","",XML_VAR_DASH_BASES_COLOR,"CDATA",   XMLUtils.toHTMLNotation(_dashBasesColor)); 

		atts.addAttribute("","",XML_VAR_CM,"CDATA",   				_cm.getParamEncoding()); 
		
		
		hd.startElement("","",XML_ELEMENT_NAME,atts);
		XMLUtils.toXML(hd, _titleFont,XML_VAR_TITLE_FONT);
		XMLUtils.toXML(hd, _numbersFont,XML_VAR_NUMBERS_FONT);
		XMLUtils.toXML(hd, _fontBasesGeneral,XML_VAR_FONT_BASES);
		
		XMLUtils.exportCDATAElem(hd,XML_VAR_CM_CAPTION, _colorMapCaption);
		hd.endElement("","",XML_ELEMENT_NAME);
	}
	
	
	

	public void loadFromXMLAttributes(Attributes attributes)
	{
		_drawOutlineBases      = Boolean.parseBoolean(attributes.getValue(XML_VAR_DRAW_OUTLINE));                             
		_fillBases             = Boolean.parseBoolean(attributes.getValue(XML_VAR_FILL_BASE));                                
		_autoFit              = Boolean.parseBoolean(attributes.getValue(XML_VAR_AUTO_FIT));                                 
		_autoCenter           = Boolean.parseBoolean(attributes.getValue(XML_VAR_AUTO_CENTER));                              
		_modifiable           = Boolean.parseBoolean(attributes.getValue(XML_VAR_MODIFIABLE));                               
		_errorsOn             = Boolean.parseBoolean(attributes.getValue(XML_VAR_ERRORS));                                   
		_colorSpecialBases    = Boolean.parseBoolean(attributes.getValue(XML_VAR_SPECIAL_BASES));                            
		_colorDashBases       = Boolean.parseBoolean(attributes.getValue(XML_VAR_DASH_BASES));                               
		_useBaseColorsForBPs  = Boolean.parseBoolean(attributes.getValue(XML_VAR_USE_BASE_BPS));                             
		_drawnNonCanonicalBP  = Boolean.parseBoolean(attributes.getValue(XML_VAR_DRAW_NC));                                  
		_drawnNonPlanarBP     = Boolean.parseBoolean(attributes.getValue(XML_VAR_DRAW_NON_PLANAR));                          
		_showWarnings         = Boolean.parseBoolean(attributes.getValue(XML_VAR_SHOW_WARNINGS));                            
		_comparisonMode       = Boolean.parseBoolean(attributes.getValue(XML_VAR_COMPARISON_MODE));                          
		_flatExteriorLoop     = Boolean.parseBoolean(attributes.getValue(XML_VAR_FLAT));                                     
		_drawBackground       = Boolean.parseBoolean(attributes.getValue(XML_VAR_DRAW_BACKGROUND));                          
		_drawColorMap         = Boolean.parseBoolean(attributes.getValue(XML_VAR_COLOR_MAP));                                
		_drawBackbone         = Boolean.parseBoolean(attributes.getValue(XML_VAR_DRAW_BACKBONE));     
		
		_colorMapHeight       = Double.parseDouble(attributes.getValue(XML_VAR_CM_HEIGHT));
		_colorMapWidth        = Double.parseDouble(attributes.getValue(XML_VAR_CM_WIDTH));
		_colorMapXOffset      = Double.parseDouble(attributes.getValue(XML_VAR_CM_X_OFFSET));
		_colorMapYOffset      = Double.parseDouble(attributes.getValue(XML_VAR_CM_Y_OFFSET));
		_zoom 				  = Double.parseDouble(attributes.getValue(XML_VAR_DEFAULT_ZOOM));
		_zoomAmount		      = Double.parseDouble(attributes.getValue(XML_VAR_ZOOM_AMOUNT));
		_bpThickness          = Double.parseDouble(attributes.getValue(XML_VAR_BP_THICKNESS));
		_baseThickness        = Double.parseDouble(attributes.getValue(XML_VAR_BASE_THICKNESS));
		_distNumbers          = Double.parseDouble(attributes.getValue(XML_VAR_DIST_NUMBERS));
		_spaceBetweenBases    = XMLUtils.getDouble(attributes, XML_VAR_SPACE_BETWEEN_BASES, DEFAULT_SPACE_BETWEEN_BASES);
		                                     			
		_numPeriod            = Integer.parseInt(attributes.getValue(XML_VAR_NUM_PERIOD));
		                                     			
		_mainBPStyle          = BP_STYLE.getStyle(attributes.getValue(XML_VAR_MAIN_BP_STYLE));           
		                                     			
		_backboneColor        = Color.decode(attributes.getValue(XML_VAR_BACKBONE_COLOR));                     
		_hoverColor           = Color.decode(attributes.getValue(XML_VAR_HOVER_COLOR));                        
		_backgroundColor      = Color.decode(attributes.getValue(XML_VAR_BACKGROUND_COLOR));                   
		_bondColor            = Color.decode(attributes.getValue(XML_VAR_BOND_COLOR));                         
		_titleColor           = Color.decode(attributes.getValue(XML_VAR_TITLE_COLOR));                        
		_specialBasesColor    = Color.decode(attributes.getValue(XML_VAR_SPECIAL_BASES_COLOR));                
		_dashBasesColor       = Color.decode(attributes.getValue(XML_VAR_DASH_BASES_COLOR));                   
                                                                             
		_cm                   = ModeleColorMap.parseColorMap(attributes.getValue(XML_VAR_CM)); 
	}
	
	
	
    public VARNAConfig clone ()
    {
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream ();
            ObjectOutputStream oout = new ObjectOutputStream (out);
            oout.writeObject (this);
            
            ObjectInputStream in = new ObjectInputStream (
                new ByteArrayInputStream (out.toByteArray ()));
            return (VARNAConfig)in.readObject ();
        }
        catch (Exception e)
        {
            throw new RuntimeException ("cannot clone class [" +
                this.getClass ().getName () + "] via serialization: " +
                e.toString ());
        }
    }

}
