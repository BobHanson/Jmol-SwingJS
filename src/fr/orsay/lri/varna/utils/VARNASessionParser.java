/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Universit� Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation;
import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation.ChemProbAnnotationType;
import fr.orsay.lri.varna.models.annotations.HighlightRegionAnnotation;
import fr.orsay.lri.varna.models.annotations.TextAnnotation;
import fr.orsay.lri.varna.models.annotations.TextAnnotation.AnchorType;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.ModeleBP.Edge;
import fr.orsay.lri.varna.models.rna.ModeleBP.Stericity;
import fr.orsay.lri.varna.models.rna.ModeleBPStyle;
import fr.orsay.lri.varna.models.rna.ModeleBackbone;
import fr.orsay.lri.varna.models.rna.ModeleBackboneElement;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModeleBaseNucleotide;
import fr.orsay.lri.varna.models.rna.ModeleBasesComparison;
import fr.orsay.lri.varna.models.rna.ModelBaseStyle;
import fr.orsay.lri.varna.models.rna.RNA;
import fr.orsay.lri.varna.models.rna.VARNAPoint;

public class VARNASessionParser extends DefaultHandler {

	StringBuffer _buffer = null;
	ModeleBaseNucleotide mbn = null;
	ModeleBasesComparison mbc = null;
	ModeleBP mbp = null;
	ModeleBPStyle mbps = null;
	ModelBaseStyle msb = null;
	TextAnnotation ta = null;
	ModeleBackbone backbone = null;
	HighlightRegionAnnotation hra = null;
	RNA rna = null;
	Font f = null;
	VARNAConfig config = null;
	
    public VARNASessionParser() {
		super();
	}

	public InputSource createSourceFromURL(String path)
	{
		URL url = null;
		try {
			url = new URL(path);
			URLConnection connexion = url.openConnection();
			connexion.setUseCaches(false);
			InputStream r = connexion.getInputStream();
			InputStreamReader inr = new InputStreamReader(r);
			return new InputSource(inr);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return new InputSource(new StringReader(""));
	}
	
	public InputSource resolveEntity(String publicId, String systemId) {
			return new InputSource(new StringReader(""));
	}
	

	private TreeSet<String> _context = new TreeSet<String>();
	
	private void addToContext(String s)
	{
		_context.add(s);
	}

	private void removeFromContext(String s)
	{
		_context.remove(s);
	}

	private boolean contextContains(String s)
	{
		return _context.contains(s);
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals(VARNAPanel.XML_ELEMENT_NAME)) {
		}
		else if (qName.equals(VARNAConfig.XML_ELEMENT_NAME)){
			config = new VARNAConfig();
			config.loadFromXMLAttributes(attributes);
		}
		else if (qName.equals(RNA.XML_ELEMENT_NAME)){
			rna = new RNA();
			int mode = Integer.parseInt(attributes.getValue(RNA.XML_VAR_DRAWN_MODE_NAME));
			rna.setDrawMode(mode);
		}
		else if (qName.equals(ModeleBackbone.XML_ELEMENT_NAME)){
			backbone = new ModeleBackbone();
			rna.setBackbone(backbone);
		}
		else if (qName.equals(ModeleBackboneElement.XML_ELEMENT_NAME)){
			if (backbone!=null){
				int index = Integer.parseInt(attributes.getValue(ModeleBackboneElement.XML_VAR_INDEX_NAME));
				ModeleBackboneElement.BackboneType type = ModeleBackboneElement.BackboneType.getType(			
						(attributes.getValue(ModeleBackboneElement.XML_VAR_TYPE_NAME)));
				Color c = null;
				if (type == ModeleBackboneElement.BackboneType.CUSTOM_COLOR)
				{
					c = Color.decode(attributes.getValue(TextAnnotation.XML_VAR_COLOR_NAME));
					backbone.addElement(new ModeleBackboneElement(index, c));
				}
				else
				{
					backbone.addElement(new ModeleBackboneElement(index, type));					
				}
			}
		}
		else if (qName.equals(ModeleBackbone.XML_ELEMENT_NAME)){
			backbone = new ModeleBackbone();
		}
		else if (qName.equals(ModeleBaseNucleotide.XML_ELEMENT_NAME)){
			if (rna!=null){
				mbn = new ModeleBaseNucleotide(rna.getSize());
				if (mbn.getIndex()!=Integer.parseInt(attributes.getValue(ModeleBase.XML_VAR_INDEX_NAME)))
					throw new SAXException("Index mismatch for Base");
				mbn.setBaseNumber(Integer.parseInt(attributes.getValue(ModeleBase.XML_VAR_NUMBER_NAME)));
				mbn.setLabel(attributes.getValue(ModeleBase.XML_VAR_LABEL_NAME));
				mbn.setColorie(Boolean.parseBoolean(attributes.getValue(ModeleBase.XML_VAR_CUSTOM_DRAWN_NAME)));
				mbn.setValue(Double.parseDouble(attributes.getValue(ModeleBase.XML_VAR_VALUE_NAME)));
				rna.addBase(mbn);
			}
		}
		else if (qName.equals(XMLUtils.XML_FONT_ELEMENT_NAME)){
			f = XMLUtils.getFont(qName,attributes);
			if (contextContains(TextAnnotation.XML_ELEMENT_NAME))
			{
				ta.setFont(f);
				f=null;
			}
			else if (contextContains(VARNAConfig.XML_ELEMENT_NAME))
			{
				String role = attributes.getValue(XMLUtils.XML_ROLE_NAME);
				if (role.equals(VARNAConfig.XML_VAR_TITLE_FONT))
				{
					config._titleFont = XMLUtils.getFont(qName, attributes);
				}
				else if (role.equals(VARNAConfig.XML_VAR_NUMBERS_FONT))
				{
					config._numbersFont = XMLUtils.getFont(qName, attributes);
				}
				else if (role.equals(VARNAConfig.XML_VAR_FONT_BASES))
				{
					config._fontBasesGeneral = XMLUtils.getFont(qName, attributes);
				}
			}
		}
		else if (qName.equals(ModeleBaseNucleotide.XML_VAR_CONTENT_NAME)){
			_buffer = new StringBuffer();
		}
		else if (qName.equals(ModeleBasesComparison.XML_VAR_FIRST_CONTENT_NAME)){
			_buffer = new StringBuffer();
		}
		else if (qName.equals(ModeleBasesComparison.XML_VAR_SECOND_CONTENT_NAME)){
			_buffer = new StringBuffer();
		}
		else if (qName.equals(ModeleBasesComparison.XML_ELEMENT_NAME)){
			if (rna!=null){
				mbc = new ModeleBasesComparison(rna.getSize());
				if (mbc.getIndex()!=Integer.parseInt(attributes.getValue(ModeleBase.XML_VAR_INDEX_NAME)))
					throw new SAXException("Index mismatch for Base");
				mbc.setBaseNumber(Integer.parseInt(attributes.getValue(ModeleBase.XML_VAR_NUMBER_NAME)));
				mbc.setLabel(attributes.getValue(ModeleBase.XML_VAR_LABEL_NAME));
				mbc.set_appartenance(Integer.parseInt(attributes.getValue(ModeleBasesComparison.XML_VAR_MEMBERSHIP_NAME)));
				mbc.setColorie(Boolean.parseBoolean(attributes.getValue(ModeleBase.XML_VAR_CUSTOM_DRAWN_NAME)));
				mbc.setValue(Double.parseDouble(attributes.getValue(ModeleBase.XML_VAR_VALUE_NAME)));
				rna.addBase(mbc);
			}
		}
		else if (qName.equals(RNA.XML_VAR_NAME_NAME))
		{
			if (rna!=null){
				_buffer = new StringBuffer();
			}			
		}
		else if (qName.equals(ModeleBP.XML_ELEMENT_NAME))
		{
			Edge e5 = Edge.valueOf(attributes.getValue(ModeleBP.XML_VAR_EDGE5_NAME));
			Edge e3 = Edge.valueOf(attributes.getValue(ModeleBP.XML_VAR_EDGE3_NAME));
			Stericity s = Stericity.valueOf(attributes.getValue(ModeleBP.XML_VAR_STERICITY_NAME));
			int i5 = Integer.parseInt(attributes.getValue(ModeleBP.XML_VAR_PARTNER5_NAME)); 
			int i3 = Integer.parseInt(attributes.getValue(ModeleBP.XML_VAR_PARTNER3_NAME)); 
			boolean inSecStr = Boolean.parseBoolean(attributes.getValue(ModeleBP.XML_VAR_SEC_STR_NAME)); 
			mbp = new ModeleBP(rna.getBaseAt(i5),rna.getBaseAt(i3),e5,e3,s);
			if (inSecStr)
				rna.addBP(i5,i3,mbp);
			else
				rna.addBPAux(i5,i3,mbp);
		}
		else if (qName.equals(ChemProbAnnotation.XML_ELEMENT_NAME))
		{
			int i5 = Integer.parseInt(attributes.getValue(ChemProbAnnotation.XML_VAR_INDEX5_NAME)); 
			int i3 = Integer.parseInt(attributes.getValue(ChemProbAnnotation.XML_VAR_INDEX3_NAME));
			ChemProbAnnotation cpa = new ChemProbAnnotation(rna.getBaseAt(i5),rna.getBaseAt(i3));
			cpa.setColor(Color.decode(attributes.getValue(ChemProbAnnotation.XML_VAR_COLOR_NAME)));
			cpa.setIntensity(Double.parseDouble(attributes.getValue(ChemProbAnnotation.XML_VAR_INTENSITY_NAME)));
			cpa.setType(ChemProbAnnotationType.valueOf(attributes.getValue(ChemProbAnnotation.XML_VAR_TYPE_NAME)));
			cpa.setOut(Boolean.parseBoolean(attributes.getValue(ChemProbAnnotation.XML_VAR_OUTWARD_NAME)));
			rna.addChemProbAnnotation(cpa);
		}
		else if (qName.equals(TextAnnotation.XML_VAR_TEXT_NAME)){
			_buffer = new StringBuffer();
		}
		else if (qName.equals(VARNAConfig.XML_VAR_TITLE)){
			_buffer = new StringBuffer();
		}
		else if (qName.equals(VARNAConfig.XML_VAR_CM_CAPTION)){
			_buffer = new StringBuffer();
		}
		else if (qName.equals(TextAnnotation.XML_ELEMENT_NAME))
		{
			AnchorType t = AnchorType.valueOf(attributes.getValue(TextAnnotation.XML_VAR_TYPE_NAME));
			ta = new TextAnnotation("");
			ta.setColor(Color.decode(attributes.getValue(TextAnnotation.XML_VAR_COLOR_NAME)));
			ta.setAngleInDegres(Double.parseDouble(attributes.getValue(TextAnnotation.XML_VAR_ANGLE_NAME)));
			ta.setType(t);
		}
		else if (qName.equals(HighlightRegionAnnotation.XML_ELEMENT_NAME))
		{
			hra = new HighlightRegionAnnotation();
			rna.addHighlightRegion(hra);
			hra.setOutlineColor(Color.decode(attributes.getValue(HighlightRegionAnnotation.XML_VAR_OUTLINE_NAME)));
			hra.setFillColor(Color.decode(attributes.getValue(HighlightRegionAnnotation.XML_VAR_FILL_NAME)));
			hra.setRadius(Double.parseDouble(attributes.getValue(HighlightRegionAnnotation.XML_VAR_RADIUS_NAME)));
		}
		else if (qName.equals(XMLUtils.XML_BASELIST_ELEMENT_NAME))
		{
			_buffer = new StringBuffer();			
		}
		else if (qName.equals(VARNAPoint.XML_ELEMENT_NAME))
		{
			Point2D.Double vp = new Point2D.Double();
			vp.x = Double.parseDouble(attributes.getValue(VARNAPoint.XML_VAR_X_NAME));
			vp.y = Double.parseDouble(attributes.getValue(VARNAPoint.XML_VAR_Y_NAME));
			String role = attributes.getValue(VARNAPoint.XML_VAR_ROLE_NAME);
			if (contextContains(ModeleBaseNucleotide.XML_ELEMENT_NAME))
			{
				if (role != null)
				{
					if (role.equals(ModeleBase.XML_VAR_POSITION_NAME))
					{
						if (mbn!=null)
						{  mbn.setCoords(vp);  }
						else throw new SAXException("No Base model for this position Point");
					}
					else if (role.equals(ModeleBase.XML_VAR_CENTER_NAME))
					{
						if (mbn!=null)
						{  mbn.setCenter(vp);  }
						else throw new SAXException("No Base model for this center Point");
					}
					
				}				
			}
			if (contextContains(ModeleBasesComparison.XML_ELEMENT_NAME))
			{
				if (role != null)
				{
					if (role.equals(ModeleBase.XML_VAR_POSITION_NAME))
					{
						if (mbc!=null)
						{  mbc.setCoords(vp);  }					
						else throw new SAXException("No Base model for this position Point");
					}
					else if (role.equals(ModeleBase.XML_VAR_CENTER_NAME))
					{
						if (mbc!=null)
						{  mbc.setCenter(vp);  }
						else throw new SAXException("No Base model for this center Point");
					}
				}
			}
			if (contextContains(TextAnnotation.XML_ELEMENT_NAME))
			{
				if (ta!=null)
				ta.setAncrage(vp.x,vp.y);
				else throw new SAXException("No TextAnnotation model for this Point");
			}
		}
		else if (qName.equals(ModelBaseStyle.XML_ELEMENT_NAME))
		{
			msb = new ModelBaseStyle();
			msb.setBaseOutlineColor(Color.decode(attributes.getValue(ModelBaseStyle.XML_VAR_OUTLINE_NAME)));
			msb.setBaseInnerColor(Color.decode(attributes.getValue(ModelBaseStyle.XML_VAR_INNER_NAME)));
			msb.setBaseNameColor(Color.decode(attributes.getValue(ModelBaseStyle.XML_VAR_NAME_NAME)));
			msb.setBaseNumberColor(Color.decode(attributes.getValue(ModelBaseStyle.XML_VAR_NUMBER_NAME)));
			if (mbn!=null)
			{  mbn.setStyleBase(msb);  }
			else if (mbc!=null)
			{  mbc.setStyleBase(msb);  }
			msb = null;
		}
		else if (qName.equals(ModeleBPStyle.XML_ELEMENT_NAME))
		{
			mbps = new ModeleBPStyle();
			boolean customColor = Boolean.parseBoolean(attributes.getValue(ModeleBPStyle.XML_VAR_CUSTOM_STYLED_NAME));
			if (customColor)
				mbps.setCustomColor(Color.decode(attributes.getValue(ModeleBPStyle.XML_VAR_COLOR_NAME)));
			mbps.setThickness(Double.parseDouble(attributes.getValue(ModeleBPStyle.XML_VAR_THICKNESS_NAME)));
			mbps.setBent(Double.parseDouble(attributes.getValue(ModeleBPStyle.XML_VAR_BENT_NAME)));
			if (mbp!=null)
			{  mbp.setStyle(mbps);  }
			mbps = null;
		}
		addToContext(qName);                                                                                                                                                       
	}

	
	
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equals(ModeleBaseNucleotide.XML_VAR_CONTENT_NAME)){
			if (_buffer==null){
				throw new SAXException("Invalid location for tag "+ModeleBaseNucleotide.XML_VAR_CONTENT_NAME);
			}
			if (mbn==null){
				throw new SAXException("Invalid location for tag "+ModeleBaseNucleotide.XML_VAR_CONTENT_NAME);
			}
			String val = _buffer.toString();
			mbn.setContent(val);
		}
		else if (qName.equals(ModeleBasesComparison.XML_VAR_FIRST_CONTENT_NAME)){
			if (_buffer==null){
				throw new SAXException("Invalid location for tag "+ModeleBaseNucleotide.XML_VAR_CONTENT_NAME);
			}
			if (mbc==null){
				throw new SAXException("Invalid location for tag "+ModeleBaseNucleotide.XML_VAR_CONTENT_NAME);
			}
			String val = _buffer.toString();
			mbc.setBase1(val.trim().charAt(0));
		}
		else if (qName.equals(ModeleBasesComparison.XML_VAR_SECOND_CONTENT_NAME)){
			if (_buffer==null){
				throw new SAXException("Invalid location for tag "+ModeleBaseNucleotide.XML_VAR_CONTENT_NAME);
			}
			if (mbc==null){
				throw new SAXException("Invalid location for tag "+ModeleBaseNucleotide.XML_VAR_CONTENT_NAME);
			}
			String val = _buffer.toString();
			mbc.setBase2(val.trim().charAt(0));
		}
		else if (qName.equals(ModeleBaseNucleotide.XML_ELEMENT_NAME)){
			mbn = null;
		}
		else if (qName.equals(ModeleBP.XML_ELEMENT_NAME))
		{
			mbp = null;
		}
		else if (qName.equals(HighlightRegionAnnotation.XML_ELEMENT_NAME))
		{
			hra = null;
		}
		else if (qName.equals(TextAnnotation.XML_VAR_TEXT_NAME))
		{
			String text = _buffer.toString();
			ta.setText(text);
			_buffer = null;			
		}
		else if (qName.equals(RNA.XML_VAR_NAME_NAME))
		{
			if (rna!=null){
				rna.setName(_buffer.toString());
				_buffer = null;
			}			
		}
		else if (qName.equals(VARNAConfig.XML_VAR_CM_CAPTION)){
			config._colorMapCaption = _buffer.toString();
			_buffer = null;			
		}

		else if (qName.equals(TextAnnotation.XML_ELEMENT_NAME))
		{
			rna.addAnnotation(ta);
			ta = null;
		}
		else if (qName.equals(XMLUtils.XML_BASELIST_ELEMENT_NAME))
		{
			String result = _buffer.toString();
			ArrayList<ModeleBase> al = XMLUtils.toModeleBaseArray(result, rna);
			if (contextContains(TextAnnotation.XML_ELEMENT_NAME))
			{
				switch(ta.getType())
				{
				case POSITION:
					break;
				case BASE:
					ta.setAncrage(al.get(0));
					break;
				case HELIX:
				case LOOP:
					try {
						ta.setAncrage(al, ta.getType());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				default:
				}
			}
			if (contextContains(HighlightRegionAnnotation.XML_ELEMENT_NAME))
			{
			  hra.setBases(al);
			}
			_buffer = null;
		}

		removeFromContext(qName);                                                                                                                                                       
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		String lecture = new String(ch, start, length);
		if (_buffer != null)
			_buffer.append(lecture);
	}

	public void startDocument() throws SAXException {
	}

	public void endDocument() throws SAXException {
	}
	
	public RNA getRNA()
	{
		return rna;
	}

	public VARNAConfig getVARNAConfig()
	{
		return config;
	}

}