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
package fr.orsay.lri.varna.models.rna;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.exceptions.ExceptionModeleStyleBaseSyntaxError;
import fr.orsay.lri.varna.exceptions.ExceptionParameterError;
import fr.orsay.lri.varna.models.VARNAConfig;


public class ModeleBP implements Serializable, Comparable<ModeleBP> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1344722280822711931L;

	public enum Edge {
		WC, SUGAR, HOOGSTEEN;
	}
	public enum Stericity {
		CIS, TRANS;
	}

	private ModeleBase _partner5;
	private Edge _edge5;
	private ModeleBase _partner3;
	private Edge _edge3;
	private Stericity _stericity;
	private ModeleBPStyle _style;

	
	public static String XML_ELEMENT_NAME = "bp";
	public static String XML_VAR_PARTNER5_NAME = "part5";
	public static String XML_VAR_EDGE5_NAME = "edge5";
	public static String XML_VAR_PARTNER3_NAME = "part3";
	public static String XML_VAR_EDGE3_NAME = "edge3";
	public static String XML_VAR_STERICITY_NAME = "orient";
	public static String XML_VAR_SEC_STR_NAME = "secstr";

	public void toXML(TransformerHandler hd, boolean inSecondaryStructure) throws SAXException
	{
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("","",XML_VAR_PARTNER5_NAME,"CDATA",""+_partner5.getIndex());
		atts.addAttribute("","",XML_VAR_PARTNER3_NAME,"CDATA",""+_partner3.getIndex());
		atts.addAttribute("","",XML_VAR_EDGE5_NAME,"CDATA",""+_edge5);
		atts.addAttribute("","",XML_VAR_EDGE3_NAME,"CDATA",""+_edge3);
		atts.addAttribute("","",XML_VAR_STERICITY_NAME,"CDATA",""+_stericity);
		atts.addAttribute("","",XML_VAR_SEC_STR_NAME,"CDATA",""+inSecondaryStructure);
		hd.startElement("","",XML_ELEMENT_NAME,atts);
		_style.toXML(hd);
		hd.endElement("","",XML_ELEMENT_NAME);
	}
	
	public void toXML(TransformerHandler hd) throws SAXException
	{
		toXML(hd, false);
	}
	

	
	public ModeleBP(ModeleBase part5, ModeleBase part3) {
		this(part5, part3, Edge.WC, Edge.WC, Stericity.CIS);
	}

	//private static Random rnd = new Random(System.currentTimeMillis());

	public ModeleBP(ModeleBase part5, ModeleBase part3, Edge edge5,
			Edge edge3, Stericity ster) {
		_partner5 = part5;
		_partner3 = part3;
		_edge5 = edge5;
		_edge3 = edge3;
		_stericity = ster;
		_style = new ModeleBPStyle();
	}

	public ModeleBP(String text) throws ExceptionModeleStyleBaseSyntaxError, ExceptionParameterError {
		_style = new ModeleBPStyle();
		assignParameters(text);		
	}
	
	public void setStericity(Stericity s) {
		_stericity = s;
	}

	public void setEdge5(Edge e) {
		_edge5 = e;
	}

	public void setEdge3(Edge e) {
		_edge3 = e;
	}

	public void setStyle(ModeleBPStyle e) {
		_style = e;
	}
	
	public ModeleBPStyle getStyle() {
		return _style;
	}
		
	public boolean isCanonicalGC() {
		String si = _partner5.getContent();
		String sj = _partner3.getContent();
		if ((si.length() >= 1) && (sj.length() >= 1)) {
			char ci = si.toUpperCase().charAt(0);
			char cj = sj.toUpperCase().charAt(0);
			if (((ci == 'G') && (cj == 'C')) || ((ci == 'C') && (cj == 'G'))) {
				return isCanonical() && (getStericity() == Stericity.CIS);
			}
		}
		return false;
	}

	public boolean isCanonicalAU() {
		String si = _partner5.getContent();
		String sj = _partner3.getContent();
		if ((si.length() >= 1) && (sj.length() >= 1)) {
			char ci = si.toUpperCase().charAt(0);
			char cj = sj.toUpperCase().charAt(0);
			if (((ci == 'A') && (cj == 'U')) 
					|| ((ci == 'U') && (cj == 'A'))
					|| ((ci == 'U') && (cj == 'T'))
					|| ((ci == 'T') && (cj == 'U'))) {
				return isCanonical();
			}
		}
		return false;
	}

	public boolean isWobbleUG() {
		String si = _partner5.getContent();
		String sj = _partner3.getContent();
		if ((si.length() >= 1) && (sj.length() >= 1)) {
			char ci = si.toUpperCase().charAt(0);
			char cj = sj.toUpperCase().charAt(0);
			if (((ci == 'G') && (cj == 'U')) || ((ci == 'U') && (cj == 'G'))) {
				return (isCanonical());
			}
		}
		return false;
	}

	public boolean isCanonical() {
		return (_edge5 == Edge.WC) && (_edge3 == Edge.WC)
				&& (_stericity == Stericity.CIS);
	}

	public Stericity getStericity() {
		return _stericity;
	}

	public boolean isCIS() {
		return (_stericity == Stericity.CIS);
	}

	public boolean isTRANS() {
		return (_stericity == Stericity.TRANS);
	}

	public Edge getEdgePartner5() {
		return _edge5;
	}

	public Edge getEdgePartner3() {
		return _edge3;
	}
	
	public ModeleBase getPartner(ModeleBase mb) {
		if (mb == _partner3)
			return _partner5;
		else
			return _partner3;
	}

	public ModeleBase getPartner5() {
		return _partner5;
	}

	public ModeleBase getPartner3() {
		return _partner3;
	}

	public int getIndex5() {
		return _partner5.getIndex();
	}

	public int getIndex3() {
		return _partner3.getIndex();
	}

	public void setPartner5(ModeleBase mb) {
		_partner5 = mb;
	}

	public void setPartner3(ModeleBase mb) {
		_partner3 = mb;
	}



	public static final String PARAM_COLOR = "color";
	public static final String PARAM_THICKNESS = "thickness";
	public static final String PARAM_EDGE5 = "edge5";
	public static final String PARAM_EDGE3 = "edge3";
	public static final String PARAM_STERICITY = "stericity";

	public static final String VALUE_WATSON_CRICK = "wc";
	public static final String VALUE_HOOGSTEEN = "h";
	public static final String VALUE_SUGAR = "s";
	public static final String VALUE_CIS = "cis";
	public static final String VALUE_TRANS = "trans";

	public void assignParameters(String parametersValue)
			throws ExceptionModeleStyleBaseSyntaxError, ExceptionParameterError {
		if (parametersValue.equals(""))
			return;

		String[] parametersL = parametersValue.split(",");

		ArrayList<String> namesArray = new ArrayList<String>();
		ArrayList<String> valuesArray = new ArrayList<String>();
		String[] param;
		for (int i = 0; i < parametersL.length; i++) {
			param = parametersL[i].split("=");
			if (param.length != 2)
				throw new ExceptionModeleStyleBaseSyntaxError(
						"Bad parameter: '" + param[0] + "' ...");
			namesArray.add(param[0].replace(" ", ""));
			valuesArray.add(param[1].replace(" ", ""));

		}
		for (int i = 0; i < namesArray.size(); i++) {
			if (namesArray.get(i).toLowerCase().equals(PARAM_COLOR)) {
				try {
					_style.setCustomColor(ModelBaseStyle
							.getSafeColor(valuesArray.get(i)));
				} catch (NumberFormatException e) {
					throw new ExceptionParameterError(e.getMessage(),
							"Bad inner color Syntax:" + valuesArray.get(i));
				}
			} else if (namesArray.get(i).toLowerCase().equals(PARAM_THICKNESS)) {
				try {
					_style.setThickness(Double.parseDouble(valuesArray.get(i)));
				} catch (NumberFormatException e) {
					throw new ExceptionParameterError(e.getMessage(),
							"Bad value for bp thickness:" + valuesArray.get(i));
				}
			} else if (namesArray.get(i).toLowerCase().equals(PARAM_EDGE5)) {
				String s = valuesArray.get(i);
				if (s.toLowerCase().equals(VALUE_WATSON_CRICK)) {
					setEdge5(Edge.WC);
				} else if (s.toLowerCase().equals(VALUE_HOOGSTEEN)) {
					setEdge5(Edge.HOOGSTEEN);
				} else if (s.toLowerCase().equals(VALUE_SUGAR)) {
					setEdge5(Edge.SUGAR);
				} else
					throw new ExceptionParameterError("Bad value for edge:"
							+ valuesArray.get(i));
			} else if (namesArray.get(i).toLowerCase().equals(PARAM_EDGE3)) {
				String s = valuesArray.get(i);
				if (s.toLowerCase().equals(VALUE_WATSON_CRICK)) {
					setEdge3(Edge.WC);
				} else if (s.toLowerCase().equals(VALUE_HOOGSTEEN)) {
					setEdge3(Edge.HOOGSTEEN);
				} else if (s.toLowerCase().equals(VALUE_SUGAR)) {
					setEdge3(Edge.SUGAR);
				} else
					throw new ExceptionParameterError("Bad value for edge:"
							+ valuesArray.get(i));
			} else if (namesArray.get(i).toLowerCase().equals(PARAM_STERICITY)) {
				String s = valuesArray.get(i);
				if (s.toLowerCase().equals(VALUE_CIS)) {
					setStericity(Stericity.CIS);
				} else if (s.toLowerCase().equals(VALUE_TRANS)) {
					setStericity(Stericity.TRANS);
				} else
					throw new ExceptionParameterError(
							"Bad value for stericity:" + valuesArray.get(i));
			} else
				throw new ExceptionModeleStyleBaseSyntaxError(
						"Unknown parameter:" + namesArray.get(i));
		}
	}
	
	public String toString() {
		String result = "";
		result += "(" + _partner5.getIndex() + "," + _partner3.getIndex() + ")";
		//result += " [" + _partner5.getElementStructure() + ","
		//		+ _partner3.getElementStructure() + "]\n";
		//result += "  5':" + _partner5 + "\n";
		//result += "  3':" + _partner3;
		return result;
	}


	public int compareTo(ModeleBP mb) {
		if (getIndex5()!=mb.getIndex5())
		{  return getIndex5()-mb.getIndex5();  }
		return getIndex3()-mb.getIndex3(); 
		
	}
	
	

}
