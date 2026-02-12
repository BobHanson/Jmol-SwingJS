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
import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.applications.templateEditor.Couple;
import fr.orsay.lri.varna.exceptions.ExceptionExportFailed;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionNAViewAlgorithm;
import fr.orsay.lri.varna.exceptions.ExceptionPermissionDenied;
import fr.orsay.lri.varna.exceptions.ExceptionUnmatchedClosingParentheses;
import fr.orsay.lri.varna.exceptions.ExceptionWritingForbidden;
import fr.orsay.lri.varna.factories.RNAFactory;
import fr.orsay.lri.varna.interfaces.InterfaceVARNAListener;
import fr.orsay.lri.varna.interfaces.InterfaceVARNAObservable;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.VARNAConfig.BP_STYLE;
import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation;
import fr.orsay.lri.varna.models.annotations.HighlightRegionAnnotation;
import fr.orsay.lri.varna.models.annotations.TextAnnotation;
import fr.orsay.lri.varna.models.export.PSExport;
import fr.orsay.lri.varna.models.export.SVGExport;
import fr.orsay.lri.varna.models.export.SecStrDrawingProducer;
import fr.orsay.lri.varna.models.export.TikzExport;
import fr.orsay.lri.varna.models.export.XFIGExport;
import fr.orsay.lri.varna.models.naView.NAView;
import fr.orsay.lri.varna.models.rna.ModeleBackboneElement.BackboneType;
import fr.orsay.lri.varna.models.templates.DrawRNATemplateCurveMethod;
import fr.orsay.lri.varna.models.templates.DrawRNATemplateMethod;
import fr.orsay.lri.varna.models.templates.RNATemplate;
import fr.orsay.lri.varna.models.templates.RNATemplateDrawingAlgorithmException;
import fr.orsay.lri.varna.models.templates.RNATemplateMapping;
import fr.orsay.lri.varna.utils.RNAMLParser;
import fr.orsay.lri.varna.utils.XMLUtils;
import fr.orsay.lri.varna.views.VueUI;

/**
 * The RNA model which contain the base list and the draw algorithm mode
 * 
 * @author darty
 * 
 */
public class RNA extends InterfaceVARNAObservable implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7541274455751497303L;

	/**
	 * Selects the "Feynman diagram" drawing algorithm that places the bases on
	 * a circle and draws the base-pairings as chords of the circle graph.
	 */

	public static final int DRAW_MODE_CIRCULAR = 1;
	/**
	 * Selects the "tree drawing" algorithm. Draws each loop on a circle whose
	 * radius depends on the number of bases involved in the loop. As some
	 * helices can be overlapping in the result, basic interaction is provided
	 * so that the user can "disentangle" the drawing by spinning the helices
	 * around the axis defined by their multiloop (bulge or internal loop)
	 * origin. This is roughly the initial placement strategy of RNAViz.
	 * 
	 * @see <a href="http://rnaviz.sourceforge.net/">RNAViz</a>
	 */
	public static final int DRAW_MODE_RADIATE = 2;

	/**
	 * Selects the NAView algorithm.
	 */
	public static final int DRAW_MODE_NAVIEW = 3;
	/**
	 * Selects the linear algorithm.
	 */
	public static final int DRAW_MODE_LINEAR = 4;

	public static final int DRAW_MODE_VARNA_VIEW = 5;

	/**
	 * Selects the RNAView algorithm.
	 */
	public static final int DRAW_MODE_MOTIFVIEW = 6;

	public static final int DRAW_MODE_TEMPLATE = 7;

	public static final int DEFAULT_DRAW_MODE = DRAW_MODE_RADIATE;

	public int BASE_RADIUS = 10;
	public static final double LOOP_DISTANCE = 40.0; // distance between base
														// pairs in an helix
	public static final double BASE_PAIR_DISTANCE = 65.0; // distance between
															// the two bases of
															// a pair
	public static final double MULTILOOP_DISTANCE = 35.0;
	public static final double VIRTUAL_LOOP_RADIUS = 40.0;

	public double CHEM_PROB_DIST = 14;
	public double CHEM_PROB_BASE_LENGTH = 30;
	public double CHEM_PROB_ARROW_HEIGHT = 10;
	public double CHEM_PROB_ARROW_WIDTH = 5;
	public double CHEM_PROB_TRIANGLE_WIDTH = 2.5;
	public double CHEM_PROB_PIN_SEMIDIAG = 6;
	public double CHEM_PROB_DOT_RADIUS = 6.;
	public static double CHEM_PROB_ARROW_THICKNESS = 2.0;

	public static ArrayList<String> NormalBases = new ArrayList<String>();
	{
		NormalBases.add("a");
		NormalBases.add("c");
		NormalBases.add("g");
		NormalBases.add("u");
		NormalBases.add("t");
	}

	public GeneralPath _debugShape = null;

	/**
	 * The draw algorithm mode
	 */
	private int _drawMode = DRAW_MODE_RADIATE;
	private boolean _drawn = false;
	private String _name = "";
	private String _id = "";
	public double _bpHeightIncrement = VARNAConfig.DEFAULT_BP_INCREMENT;
	/**
	 * the base list
	 */
	private ArrayList<ModeleBase> _listeBases;
	/**
	 * the strand list
	 */
	StructureTemp _listStrands = new StructureTemp();
	/**
	 * Additional bonds and info can be specified here.
	 */
	private ArrayList<ModeleBP> _structureAux = new ArrayList<ModeleBP>();
	private ArrayList<TextAnnotation> _listeAnnotations = new ArrayList<TextAnnotation>();
	private ArrayList<HighlightRegionAnnotation> _listeRegionHighlights = new ArrayList<HighlightRegionAnnotation>();
	private ArrayList<ChemProbAnnotation> _chemProbAnnotations = new ArrayList<ChemProbAnnotation>();
	private ModeleBackbone _backbone = new ModeleBackbone();

	public static String XML_ELEMENT_NAME = "RNA";
	public static String XML_VAR_BASE_SPACING_NAME = "spacing";
	public static String XML_VAR_DRAWN_NAME = "drawn";
	public static String XML_VAR_NAME_NAME = "name";
	public static String XML_VAR_DRAWN_MODE_NAME = "mode";
	public static String XML_VAR_ID_NAME = "id";
	public static String XML_VAR_BP_HEIGHT_NAME = "delta";
	public static String XML_VAR_BASES_NAME = "bases";
	public static String XML_VAR_BASEPAIRS_NAME = "BPs";
	public static String XML_VAR_ANNOTATIONS_NAME = "annotations";
	public static String XML_VAR_BACKBONE_NAME = "backbone";

	public void toXML(TransformerHandler hd) throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("", "", XML_VAR_DRAWN_NAME, "CDATA", "" + _drawn);
		atts.addAttribute("", "", XML_VAR_DRAWN_MODE_NAME, "CDATA", ""
				+ _drawMode);
		atts.addAttribute("", "", XML_VAR_ID_NAME, "CDATA", "" + _id);
		atts.addAttribute("", "", XML_VAR_BP_HEIGHT_NAME, "CDATA", ""
				+ _bpHeightIncrement);
		hd.startElement("", "", XML_ELEMENT_NAME, atts);

		atts.clear();
		hd.startElement("", "", XML_VAR_NAME_NAME, atts);
		XMLUtils.exportCDATAString(hd, "" + _name);
		hd.endElement("", "", XML_VAR_NAME_NAME);

		atts.clear();
		hd.startElement("", "", XML_VAR_BASES_NAME, atts);
		for (ModeleBase mb : _listeBases) {
			mb.toXML(hd);
		}
		hd.endElement("", "", XML_VAR_BASES_NAME);
		atts.clear();

		hd.startElement("", "", XML_VAR_BASEPAIRS_NAME, atts);
		for (ModeleBP mbp : getSecStrBPs()) {
			mbp.toXML(hd, true);
		}
		for (ModeleBP mbp : _structureAux) {
			mbp.toXML(hd, false);
		}
		hd.endElement("", "", XML_VAR_BASEPAIRS_NAME);
		atts.clear();

		getBackbone().toXML(hd);
		atts.clear();

		hd.startElement("", "", XML_VAR_ANNOTATIONS_NAME, atts);
		for (TextAnnotation ta : _listeAnnotations) {
			ta.toXML(hd);
		}
		for (HighlightRegionAnnotation hra : _listeRegionHighlights) {
			hra.toXML(hd);
		}
		for (ChemProbAnnotation cpa : _chemProbAnnotations) {
			cpa.toXML(hd);
		}
		hd.endElement("", "", XML_VAR_ANNOTATIONS_NAME);
		hd.endElement("", "", XML_ELEMENT_NAME);
	}

	public ModeleBackbone getBackbone() {
		return _backbone;
	}

	public void setBackbone(ModeleBackbone b) {
		_backbone = b;
	}

	transient private ArrayList<InterfaceVARNAListener> _listeVARNAListener = new ArrayList<InterfaceVARNAListener>();

	public RNA() {
		this("");
	}

	public RNA(String name) {
		_name = name;
		_listeBases = new ArrayList<ModeleBase>();
		_drawn = false;
		init();
	}

	public String toString() {
		if (_name.equals("")) {
			return getStructDBN();
		} else {
			return _name;
		}
	}

	public RNA(RNA r) {
		_drawMode = r._drawMode;
		_listeBases.addAll(r._listeBases);
		_listeVARNAListener = (ArrayList<InterfaceVARNAListener>) r._listeVARNAListener;
		_drawn = r._drawn;
		init();
	}

	public void init() {
	}

	public void saveRNADBN(String path, String title)
			throws ExceptionWritingForbidden {
		try {
			FileWriter out = new FileWriter(path);
			if (!title.equals("")) {
				out.write("> " + title + "\n");
			}
			out.write(getListeBasesToString());
			out.write('\n');
			String str = "";
			for (int i = 0; i < _listeBases.size(); i++) {
				if (_listeBases.get(i).getElementStructure() == -1) {
					str += '.';
				} else {
					if (_listeBases.get(i).getElementStructure() > i) {
						str += '(';
					} else {
						str += ')';
					}
				}
			}
			out.write(str);
			out.write('\n');
			out.close();
		} catch (IOException e) {
			throw new ExceptionWritingForbidden(e.getMessage());
		}
	}

	public Color getBaseInnerColor(int i, VARNAConfig conf) {
		Color result = _listeBases.get(i).getStyleBase().getBaseInnerColor();
		String res = _listeBases.get(i).getContent();
		if (conf._drawColorMap) {
			result = conf._cm.getColorForValue(_listeBases.get(i).getValue());
		} else if ((conf._colorDashBases && (res.contains("-")))) {
			result = conf._dashBasesColor;
		} else if ((conf._colorSpecialBases && !NormalBases.contains(res
				.toLowerCase()))) {
			result = conf._specialBasesColor;
		}
		return result;
	}

	public Color getBaseOuterColor(int i, VARNAConfig conf) {
		Color result = _listeBases.get(i).getStyleBase()
				.getBaseOutlineColor();
		return result;
	}

	private static double correctComponent(double c)
	{
	    c = c / 255.0;
	    if (c <= 0.03928) 
	    	c = c/12.92;
	    else 
	    	c = Math.pow(((c+0.055)/1.055) , 2.4);
	    return c;
	}
	public static double getLuminance(Color c)
	{
		return 0.2126 * correctComponent(c.getRed()) + 0.7152 * correctComponent(c.getGreen()) + 0.0722 * correctComponent(c.getBlue());
	}
	
	public static boolean whiteLabelPreferrable(Color c)
	{
		if (getLuminance(c) > 0.32)
			return false;
		return true;
	}
	

	
	public Color getBaseNameColor(int i, VARNAConfig conf) {
		Color result = _listeBases.get(i).getStyleBase().getBaseNameColor();
		if ( RNA.whiteLabelPreferrable(getBaseInnerColor(i, conf)))
		{
			result=Color.white;
		}

		return result;
	}

	public Color getBasePairColor(ModeleBP bp, VARNAConfig conf) {
		Color bondColor = conf._bondColor;
		if (conf._useBaseColorsForBPs) {
			bondColor = _listeBases.get(bp.getPartner5().getIndex())
					.getStyleBase().getBaseInnerColor();
		}
		if (bp != null) {
			bondColor = bp.getStyle().getColor(bondColor);
		}
		return bondColor;
	}

	public double getBasePairThickness(ModeleBP bp, VARNAConfig conf) {
		double thickness = bp.getStyle().getThickness(conf._bpThickness);
		return thickness;
	}

	private void drawSymbol(SecStrDrawingProducer out, double posx,
			double posy, double normx, double normy, double radius,
			boolean isCIS, ModeleBP.Edge e, double thickness) {
		Color bck = out.getCurrentColor();
		switch (e) {
		case WC:
			if (isCIS) {
				out.fillCircle(posx, posy, (radius / 2.0), thickness, bck);
			} else {
				out.fillCircle(posx, posy, (radius / 2.0), thickness,
						Color.white);
				out.setColor(bck);
				out.drawCircle(posx, posy, (radius / 2.0), thickness);
			}
			break;
		case HOOGSTEEN: {
			double xtab[] = new double[4];
			double ytab[] = new double[4];
			xtab[0] = posx - radius * normx / 2.0 - radius * normy / 2.0;
			ytab[0] = posy - radius * normy / 2.0 + radius * normx / 2.0;
			xtab[1] = posx + radius * normx / 2.0 - radius * normy / 2.0;
			ytab[1] = posy + radius * normy / 2.0 + radius * normx / 2.0;
			xtab[2] = posx + radius * normx / 2.0 + radius * normy / 2.0;
			ytab[2] = posy + radius * normy / 2.0 - radius * normx / 2.0;
			xtab[3] = posx - radius * normx / 2.0 + radius * normy / 2.0;
			ytab[3] = posy - radius * normy / 2.0 - radius * normx / 2.0;
			if (isCIS) {
				out.fillPolygon(xtab, ytab, bck);
			} else {
				out.fillPolygon(xtab, ytab, Color.white);
				out.setColor(bck);
				out.drawPolygon(xtab, ytab, thickness);
			}
		}
			break;
		case SUGAR: {
			double ix = radius * normx / 2.0;
			double iy = radius * normy / 2.0;
			double jx = radius * normy / 2.0;
			double jy = -radius * normx / 2.0;
			double xtab[] = new double[3];
			double ytab[] = new double[3];
			xtab[0] = posx - ix + jx;
			ytab[0] = posy - iy + jy;
			xtab[1] = posx + ix + jx;
			ytab[1] = posy + iy + jy;
			xtab[2] = posx - jx;
			ytab[2] = posy - jy;

			if (isCIS) {
				out.fillPolygon(xtab, ytab, bck);
			} else {
				out.fillPolygon(xtab, ytab, Color.white);
				out.setColor(bck);
				out.drawPolygon(xtab, ytab, thickness);
			}
		}
			break;
		}
		out.setColor(bck);
	}

	private void drawBasePairArc(SecStrDrawingProducer out, int i, int j,
			Point2D.Double orig, Point2D.Double dest, ModeleBP style,
			VARNAConfig conf) {
		double coef;
		double distance;
		Point2D.Double center = new Point2D.Double((orig.x + dest.x)/2., (orig.y + dest.y)/2. + BASE_RADIUS); 
		if (j - i == 1)
			coef = _bpHeightIncrement * 2;
		else
			coef = _bpHeightIncrement * 1;
		distance = (int) Math.round(dest.x - orig.x);
		if (conf._mainBPStyle != BP_STYLE.LW) {
			out.drawArc(center, distance, distance * coef, 180, 0);
		} else {
			double thickness = getBasePairThickness(style, conf);
			double radiusCircle = ((BASE_PAIR_DISTANCE - BASE_RADIUS) / 5.0);

			if (style.isCanonical()) {
				if (style.isCanonicalGC()) {
					if ((orig.x != dest.x) || (orig.y != dest.y)) {
						out.drawArc(center, distance - BASE_RADIUS / 2.,
								distance * coef - BASE_RADIUS / 2, 180, 0);
						out.drawArc(center, distance + BASE_RADIUS / 2.,
								distance * coef + BASE_RADIUS / 2, 180, 0);
					}
				} else if (!style.isWobbleUG()) {
					out.drawArc(center, distance, distance * coef, 180, 0);
					drawSymbol(out, center.x, center.y + distance * coef / 2., 180., 0,
							radiusCircle, style.isCIS(),
							style.getEdgePartner5(), thickness);
				} else {
					out.drawArc(orig, distance, distance * coef, 180, 0);
				}
			} else {
				ModeleBP.Edge p1 = style.getEdgePartner5();
				ModeleBP.Edge p2 = style.getEdgePartner3();
				out.drawArc(center, distance, distance * coef, 180, 0);
				if (p1 == p2) {
					drawSymbol(out, center.x, center.y + distance * coef / 2., 1., 0,
							radiusCircle, style.isCIS(),
							style.getEdgePartner5(), thickness);
				} else {
					drawSymbol(out, center.x - BASE_RADIUS,
							center.y + distance * coef / 2., 1., 0, radiusCircle,
							style.isCIS(), p1, thickness);
					drawSymbol(out, center.x + BASE_RADIUS,
							center.y + distance * coef / 2., 1., 0, radiusCircle,
							style.isCIS(), p2, thickness);
				}
			}
		}
	}

	private void drawBasePair(SecStrDrawingProducer out, Point2D.Double orig,
			Point2D.Double dest, ModeleBP style, VARNAConfig conf) {
		double dx = dest.x - orig.x;
		double dy = dest.y - orig.y;
		double dist = Math.sqrt((dest.x - orig.x) * (dest.x - orig.x)
				+ (dest.y - orig.y) * (dest.y - orig.y));
		dx /= dist;
		dy /= dist;
		double nx = -dy;
		double ny = dx;
		orig = new Point2D.Double(orig.x + BASE_RADIUS * dx, orig.y
				+ BASE_RADIUS * dy);
		dest = new Point2D.Double(dest.x - BASE_RADIUS * dx, dest.y
				- BASE_RADIUS * dy);
		if (conf._mainBPStyle == VARNAConfig.BP_STYLE.LW) {
			double thickness = getBasePairThickness(style, conf);
			double radiusCircle = ((BASE_PAIR_DISTANCE - BASE_RADIUS) / 5.0);

			if (style.isCanonical()) {
				if (style.isCanonicalGC()) {
					if ((orig.x != dest.x) || (orig.y != dest.y)) {
						nx *= BASE_RADIUS / 4.0;
						ny *= BASE_RADIUS / 4.0;
						out.drawLine((orig.x + nx), (orig.y + ny),
								(dest.x + nx), (dest.y + ny), conf._bpThickness);
						out.drawLine((orig.x - nx), (orig.y - ny),
								(dest.x - nx), (dest.y - ny), conf._bpThickness);
					}
				} else if (style.isCanonicalAU()) {
					out.drawLine(orig.x, orig.y, dest.x, dest.y,
							conf._bpThickness);
				} else if (style.isWobbleUG()) {
					double cx = (dest.x + orig.x) / 2.0;
					double cy = (dest.y + orig.y) / 2.0;
					out.drawLine(orig.x, orig.y, dest.x, dest.y,
							conf._bpThickness);
					drawSymbol(out, cx, cy, nx, ny, radiusCircle, false,
							ModeleBP.Edge.WC, thickness);
				}

				else {
					double cx = (dest.x + orig.x) / 2.0;
					double cy = (dest.y + orig.y) / 2.0;
					out.drawLine(orig.x, orig.y, dest.x, dest.y,
							conf._bpThickness);
					drawSymbol(out, cx, cy, nx, ny, radiusCircle,
							style.isCIS(), style.getEdgePartner5(), thickness);
				}
			} else {
				ModeleBP.Edge p1 = style.getEdgePartner5();
				ModeleBP.Edge p2 = style.getEdgePartner3();
				double cx = (dest.x + orig.x) / 2.0;
				double cy = (dest.y + orig.y) / 2.0;
				out.drawLine(orig.x, orig.y, dest.x, dest.y, conf._bpThickness);
				if (p1 == p2) {
					drawSymbol(out, cx, cy, nx, ny, radiusCircle,
							style.isCIS(), p1, thickness);
				} else {
					double vdx = (dest.x - orig.x);
					double vdy = (dest.y - orig.y);
					vdx /= 6.0;
					vdy /= 6.0;
					drawSymbol(out, cx + vdx, cy + vdy, nx, ny, radiusCircle,
							style.isCIS(), p2, thickness);
					drawSymbol(out, cx - vdx, cy - vdy, nx, ny, radiusCircle,
							style.isCIS(), p1, thickness);
				}
			}
		} else if (conf._mainBPStyle == VARNAConfig.BP_STYLE.RNAVIZ) {
			double xcenter = (orig.x + dest.x) / 2.0;
			double ycenter = (orig.y + dest.y) / 2.0;
			out.fillCircle(xcenter, ycenter, 3.0 * conf._bpThickness,
					conf._bpThickness, out.getCurrentColor());
		} else if (conf._mainBPStyle == VARNAConfig.BP_STYLE.SIMPLE) {
			out.drawLine(orig.x, orig.y, dest.x, dest.y, conf._bpThickness);
		}
	}

	private void drawColorMap(VARNAConfig _conf, SecStrDrawingProducer out) {
		double v1 = _conf._cm.getMinValue();
		double v2 = _conf._cm.getMaxValue();
		int x, y;
		double xSpaceAvail = 0;
		double ySpaceAvail = 0;
		double thickness = 1.0;
		/*
		 * ySpaceAvail =
		 * Math.min((getHeight()-rnabbox.height*scaleFactor-getTitleHeight
		 * ())/2.0,scaleFactor*(_conf._colorMapHeight+VARNAConfig.
		 * DEFAULT_COLOR_MAP_FONT_SIZE)); if ((int)ySpaceAvail==0) { xSpaceAvail
		 * =
		 * Math.min((getWidth()-rnabbox.width*scaleFactor)/2,scaleFactor*(_conf
		 * ._colorMapWidth)+VARNAConfig.DEFAULT_COLOR_MAP_STRIPE_WIDTH); }
		 */
		Rectangle2D.Double currentBBox = out.getBoundingBox();

		double xBase = (currentBBox.getMaxX() - _conf._colorMapWidth - _conf._colorMapXOffset);
		// double yBase = (minY - _conf._colorMapHeight +
		// _conf._colorMapYOffset);
		double yBase = (currentBBox.getMinY() - _conf._colorMapHeight - VARNAConfig.DEFAULT_COLOR_MAP_FONT_SIZE);

		for (int i = 0; i < _conf._colorMapWidth; i++) {
			double ratio = (((double) i) / ((double) _conf._colorMapWidth - 1));
			double val = v1 + (v2 - v1) * ratio;
			Color c = _conf._cm.getColorForValue(val);
			x = (int) (xBase + i);
			y = (int) yBase;
			out.fillRectangle(x, y, VARNAConfig.DEFAULT_COLOR_MAP_STRIPE_WIDTH,
					_conf._colorMapHeight, c);
		}
		out.setColor(VARNAConfig.DEFAULT_COLOR_MAP_OUTLINE);
		out.drawRectangle(xBase, yBase, (double) _conf._colorMapWidth
				+ VARNAConfig.DEFAULT_COLOR_MAP_STRIPE_WIDTH - 1,
				_conf._colorMapHeight, thickness);

		out.setColor(VARNAConfig.DEFAULT_COLOR_MAP_FONT_COLOR);
		out.setFont(out.getCurrentFont(),
				VARNAConfig.DEFAULT_COLOR_MAP_FONT_SIZE / 1.5);
		out.drawText(xBase, yBase + _conf._colorMapHeight
				+ VARNAConfig.DEFAULT_COLOR_MAP_FONT_SIZE / 1.7,
				"" + _conf._cm.getMinValue());
		out.drawText(xBase + VARNAConfig.DEFAULT_COLOR_MAP_STRIPE_WIDTH
				+ _conf._colorMapWidth, yBase + _conf._colorMapHeight
				+ VARNAConfig.DEFAULT_COLOR_MAP_FONT_SIZE / 1.7,
				"" + _conf._cm.getMaxValue());
		out.drawText(
				xBase
						+ (VARNAConfig.DEFAULT_COLOR_MAP_STRIPE_WIDTH + _conf._colorMapWidth)
						/ 2.0, yBase
						- (VARNAConfig.DEFAULT_COLOR_MAP_FONT_SIZE / 1.7),
				_conf._colorMapCaption);

	}

	private void renderRegionHighlights(SecStrDrawingProducer out,
			Point2D.Double[] realCoords, Point2D.Double[] realCenters) {
		for (HighlightRegionAnnotation r : _listeRegionHighlights) {
			GeneralPath s = r.getShape(realCoords, realCenters, 1.0);
			out.setColor(r.getFillColor());
			out.fillPolygon(s, r.getFillColor());
			out.setColor(r.getOutlineColor());
			out.drawPolygon(s, 1l);
		}

	}

	private void saveRNA(String path, VARNAConfig conf, double scale,
			SecStrDrawingProducer out) throws ExceptionWritingForbidden {
		out.setScale(scale);
		// Computing bounding boxes
		double EPSMargin = 40;
		double minX = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		double maxY = Double.MIN_VALUE;

		double x0, y0, x1, y1, xc, yc, xp, yp, dx, dy, norm;

		for (int i = 0; i < _listeBases.size(); i++) {
			minX = Math.min(minX, (_listeBases.get(i).getCoords().getX()
					- BASE_RADIUS - EPSMargin));
			minY = Math.min(minY, -(_listeBases.get(i).getCoords().getY()
					- BASE_RADIUS - EPSMargin));
			maxX = Math.max(maxX, (_listeBases.get(i).getCoords().getX()
					+ BASE_RADIUS + EPSMargin));
			maxY = Math.max(maxY, -(_listeBases.get(i).getCoords().getY()
					+ BASE_RADIUS + EPSMargin));
		}

		// Rescaling everything
		Point2D.Double[] coords = new Point2D.Double[_listeBases.size()];
		Point2D.Double[] centers = new Point2D.Double[_listeBases.size()];
		for (int i = 0; i < _listeBases.size(); i++) {
			xp = (_listeBases.get(i).getCoords().getX() - minX);
			yp = -(_listeBases.get(i).getCoords().getY() - minY);
			coords[i] = new Point2D.Double(xp, yp);

			Point2D.Double centerBck = getCenter(i);
			if (get_drawMode() == RNA.DRAW_MODE_NAVIEW
					|| get_drawMode() == RNA.DRAW_MODE_RADIATE) {
				if ((_listeBases.get(i).getElementStructure() != -1)
						&& i < _listeBases.size() - 1 && i > 1) {
					ModeleBase b1 = get_listeBases().get(i - 1);
					ModeleBase b2 = get_listeBases().get(i + 1);
					int j1 = b1.getElementStructure();
					int j2 = b2.getElementStructure();
					if ((j1 == -1) ^ (j2 == -1)) {
						// alors la position du nombre associé doit etre
						// décalé
						Point2D.Double a1 = b1.getCoords();
						Point2D.Double a2 = b2.getCoords();
						Point2D.Double c1 = b1.getCenter();
						Point2D.Double c2 = b2.getCenter();

						centerBck.x = _listeBases.get(i).getCoords().x
								+ (c1.x - a1.x) / c1.distance(a1)
								+ (c2.x - a2.x) / c2.distance(a2);
						centerBck.y = _listeBases.get(i).getCoords().y
								+ (c1.y - a1.y) / c1.distance(a1)
								+ (c2.y - a2.y) / c2.distance(a2);
					}
				}
			}
			xc = (centerBck.getX() - minX);
			yc = -(centerBck.getY() - minY);
			centers[i] = new Point2D.Double(xc, yc);
		}

		// Drawing background
		if (conf._drawBackground)
			out.setBackgroundColor(conf._backgroundColor);

		// Drawing region highlights
		renderRegionHighlights(out, coords, centers);

		// Drawing backbone
		if (conf._drawBackbone)
		{
			for (int i = 1; i < _listeBases.size(); i++) {
				Point2D.Double p1 = coords[i - 1];
				Point2D.Double p2 = coords[i];
				x0 = p1.x;
				y0 = p1.y;
				x1 = p2.x;
				y1 = p2.y;
				Point2D.Double vn = new Point2D.Double();
				double dist = p1.distance(p2);
				int a = _listeBases.get(i - 1).getElementStructure();
				int b = _listeBases.get(i).getElementStructure();
				BackboneType bt = _backbone.getTypeBefore(i);
				boolean consecutivePair = (a == i) && (b == i - 1);
	
				if (dist > 0) {
					if (bt != BackboneType.DISCONTINUOUS_TYPE) {
						Color c = _backbone.getColorBefore(i, conf._backboneColor);
						if (bt == BackboneType.MISSING_PART_TYPE) {
							c.brighter();
						}
						out.setColor(c);
	
						vn.x = (x1 - x0) / dist;
						vn.y = (y1 - y0) / dist;
						if (consecutivePair
								&&(getDrawMode() != RNA.DRAW_MODE_LINEAR)
								&& (getDrawMode() != RNA.DRAW_MODE_CIRCULAR)) {
							int dir = 0;
							if (i + 1 < coords.length) {
								dir = (testDirectionality(i - 1, i, i + 1) ? 1 : -1);
							} else if (i - 2 >= 0) {
								dir = (testDirectionality(i - 2, i - 1, i) ? 1 : -1);
							}
							Point2D.Double centerSeg = new Point2D.Double(
									(p1.x + p2.x) / 2.0, (p1.y + p2.y) / 2.0);
							double centerDist = RNA.VIRTUAL_LOOP_RADIUS * scale;
							Point2D.Double centerLoop = new Point2D.Double(
									centerSeg.x + centerDist * dir * vn.y,
									centerSeg.y - centerDist * dir * vn.x);
							// Debug crosshair
							//out.drawLine(centerLoop.x - 5, centerLoop.y,
							//		centerLoop.x + 5, centerLoop.y, 2.0);
							//out.drawLine(centerLoop.x, centerLoop.y - 5,
							//		centerLoop.x, centerLoop.y + 5, 2.0);
							
							double radius = centerLoop.distance(p1);
							double a1 = 360.
									* (Math.atan2(p1.y - centerLoop.y, p1.x - centerLoop.x))
									/ (2. * Math.PI);
							double a2 = 360.
									* (Math.atan2(p2.y - centerLoop.y, p2.x - centerLoop.x))
									/ (2. * Math.PI);
							if (dir>0)
							{
								double tmp = a1;
								a1 = a2;
								a2 = tmp;
							}
							if (a1 < 0) {
								a1 += 360.;
							}
							if (a2 < 0) {
								a2 += 360.;
							}
							out.drawArc(centerLoop, 2. * radius, 2. * radius, a1,
									a2);
						} else {
							out.drawLine((x0 + BASE_RADIUS * vn.x),
									(y0 + BASE_RADIUS * vn.y), (x1 - BASE_RADIUS
											* vn.x), (y1 - BASE_RADIUS * vn.y), 1.0);
						}
					}
				}
			}
		}

		// Drawing bonds
		for (int i = 0; i < _listeBases.size(); i++) {
			if (_listeBases.get(i).getElementStructure() > i) {
				ModeleBP style = _listeBases.get(i).getStyleBP();
				if (style.isCanonical() || conf._drawnNonCanonicalBP) {
					Color bpcol = getBasePairColor(style, conf);
					out.setColor(bpcol);

					int j = _listeBases.get(i).getElementStructure();
					x0 = coords[i].x;
					y0 = coords[i].y;
					x1 = coords[j].x;
					y1 = coords[j].y;
					dx = x1 - x0;
					dy = y1 - y0;
					norm = Math.sqrt(dx * dx + dy * dy);
					dx /= norm;
					dy /= norm;

					if (_drawMode == DRAW_MODE_CIRCULAR
							|| _drawMode == DRAW_MODE_RADIATE
							|| _drawMode == DRAW_MODE_NAVIEW) {
						drawBasePair(out, new Point2D.Double(x0, y0),
								new Point2D.Double(x1, y1), style, conf);
					} else if (_drawMode == DRAW_MODE_LINEAR) {
						drawBasePairArc(out, i, j, new Point2D.Double(x0, y0),
								new Point2D.Double(x1, y1), style, conf);
					}
				}
			}
		}

		// Drawing additional bonds
		if (conf._drawnNonPlanarBP) {
			for (int i = 0; i < _structureAux.size(); i++) {
				ModeleBP bp = _structureAux.get(i);
				out.setColor(getBasePairColor(bp, conf));

				int a = bp.getPartner5().getIndex();
				int b = bp.getPartner3().getIndex();

				if (bp.isCanonical() || conf._drawnNonCanonicalBP) {
					x0 = coords[a].x;
					y0 = coords[a].y;
					x1 = coords[b].x;
					y1 = coords[b].y;
					dx = x1 - x0;
					dy = y1 - y0;
					norm = Math.sqrt(dx * dx + dy * dy);
					dx /= norm;
					dy /= norm;
					if ((_drawMode == DRAW_MODE_CIRCULAR)
							|| (_drawMode == DRAW_MODE_RADIATE)
							|| _drawMode == DRAW_MODE_NAVIEW) {
						drawBasePair(out, new Point2D.Double(x0, y0),
								new Point2D.Double(x1, y1), bp, conf);
					} else if (_drawMode == DRAW_MODE_LINEAR) {
						drawBasePairArc(out, a, b, new Point2D.Double(x0, y0),
								new Point2D.Double(x1, y1), bp, conf);
					}
				}
			}
		}

		// Drawing Bases
		double baseFontSize = (1.5 * BASE_RADIUS);
		out.setFont(PSExport.FONT_HELVETICA_BOLD, baseFontSize);
		
		for (int i = 0; i < _listeBases.size(); i++) {
			x0 = coords[i].x;
			y0 = coords[i].y;
			
			Color baseInnerColor = getBaseInnerColor(i, conf);
			Color baseOuterColor = getBaseOuterColor(i, conf);
			Color baseNameColor = getBaseNameColor(i, conf);
			if ( RNA.whiteLabelPreferrable(baseInnerColor))
			{
				baseNameColor=Color.white;
			}


			if (_listeBases.get(i) instanceof ModeleBasesComparison) {
				ModeleBasesComparison mb = (ModeleBasesComparison) _listeBases
						.get(i);
				if (conf._fillBases) {
					out.fillRectangle(x0 - 1.5 * BASE_RADIUS, y0 - BASE_RADIUS,
							3 * BASE_RADIUS, 2 * BASE_RADIUS,
							baseInnerColor);
				}
				if (conf._drawOutlineBases) {
					out.setColor(baseOuterColor);
					out.drawRectangle(x0 - 1.5 * BASE_RADIUS, y0 - BASE_RADIUS,
							3 * BASE_RADIUS, 2 * BASE_RADIUS, 1l);
					out.drawLine(x0, y0 - BASE_RADIUS, x0, y0 + BASE_RADIUS, 1l);
				}
				
				out.setColor(baseNameColor);
				out.drawText(x0 - .75 * BASE_RADIUS, y0, "" + mb.getBase1());
				out.drawText(x0 + .75 * BASE_RADIUS, y0, "" + mb.getBase2());
			} else if (_listeBases.get(i) instanceof ModeleBaseNucleotide) {
				if (conf._fillBases) {
					out.fillCircle(x0, y0, BASE_RADIUS, 1l,
							baseInnerColor);
				}
				if (conf._drawOutlineBases) {
					out.setColor(baseOuterColor);
					out.drawCircle(x0, y0, BASE_RADIUS, 1l);
				}
				out.setColor(baseNameColor);
				out.drawText(x0, y0, _listeBases.get(i).getContent());

			}
		}

		// Drawing base numbers
		double numFontSize = (double) (1.5 * BASE_RADIUS);
		out.setFont(PSExport.FONT_HELVETICA_BOLD, numFontSize);

		for (int i = 0; i < _listeBases.size(); i++) {
			int basenum = _listeBases.get(i).getBaseNumber();
			if (basenum == -1) {
				basenum = i + 1;
			}
			ModeleBase mb = _listeBases.get(i);
			if (this.isNumberDrawn(mb, conf._numPeriod)) {
				out.setColor(mb.getStyleBase()
						.getBaseNumberColor());
				x0 = coords[i].x;
				y0 = coords[i].y;
				x1 = centers[i].x;
				y1 = centers[i].y;
				dx = x1 - x0;
				dy = y1 - y0;
				norm = Math.sqrt(dx * dx + dy * dy);
				dx /= norm;
				dy /= norm;
				Point2D.Double vn = VARNAPanel.computeExcentricUnitVector(i,coords,centers);
				
				out.drawLine((x0 + 1.5 * BASE_RADIUS * vn.x), (y0 + 1.5
						* BASE_RADIUS * vn.y), (x0 + 2.5 * BASE_RADIUS * vn.x),
						(y0 + 2.5 * BASE_RADIUS * vn.y), 1);
				out.drawText(
						(x0 + (conf._distNumbers + 1.0) * BASE_RADIUS * vn.x),
						(y0 + (conf._distNumbers + 1.0) * BASE_RADIUS * vn.y), mb.getLabel());
			}
		}
		renderAnnotations(out, minX, minY, conf);

		// Draw color map
		if (conf._drawColorMap) {
			drawColorMap(conf, out);
		}

		// Drawing Title
		Rectangle2D.Double currentBBox = out.getBoundingBox();
		double titleFontSize = (2.0 * conf._titleFont.getSize());
		out.setColor(conf._titleColor);
		out.setFont(PSExport.FONT_HELVETICA, titleFontSize);
		double yTitle = currentBBox.y - titleFontSize / 2.0;
		if (!getName().equals("")) {
			out.drawText((maxX - minX) / 2.0, yTitle, getName());
		}

		OutputStreamWriter fout;

		try {
			fout = new OutputStreamWriter(new FileOutputStream(path), "UTF-8");

			fout.write(out.export());
			fout.close();
		} catch (IOException e) {
			throw new ExceptionWritingForbidden(e.getMessage());
		}
	}

	Point2D.Double buildCaptionPosition(ModeleBase mb, double heightEstimate,
			VARNAConfig conf) {
		double radius = 2.0;
		if (isNumberDrawn(mb, conf._numPeriod)) {
			radius += (conf._distNumbers + 1.0);
		}
		Point2D.Double center = mb.getCenter();
		Point2D.Double p = mb.getCoords();
		double realDistance = BASE_RADIUS * radius + heightEstimate;
		return new Point2D.Double(center.getX() + (p.getX() - center.getX())
				* ((p.distance(center) + realDistance) / p.distance(center)),
				center.getY()
						+ (p.getY() - center.getY())
						* ((p.distance(center) + realDistance) / p
								.distance(center)));
	}

	public double getBPHeightIncrement() {
		return this._bpHeightIncrement;
	}

	public void setBPHeightIncrement(double d) {
		_bpHeightIncrement = d;
	}

	private void drawChemProbAnnotation(SecStrDrawingProducer out,
			ChemProbAnnotation cpa, Point2D.Double anchor, double minX,
			double minY) {
		out.setColor(cpa.getColor());
		Point2D.Double v = cpa.getDirVector();
		Point2D.Double vn = cpa.getNormalVector();
		Point2D.Double base = new Point2D.Double((anchor.x + CHEM_PROB_DIST
				* v.x), (anchor.y + CHEM_PROB_DIST * v.y));
		Point2D.Double edge = new Point2D.Double(
				(base.x + CHEM_PROB_BASE_LENGTH * cpa.getIntensity() * v.x),
				(base.y + CHEM_PROB_BASE_LENGTH * cpa.getIntensity() * v.y));
		double thickness = CHEM_PROB_ARROW_THICKNESS * cpa.getIntensity();
		switch (cpa.getType()) {
		case ARROW: {
			Point2D.Double arrowTip1 = new Point2D.Double(
					(base.x + cpa.getIntensity()
							* (CHEM_PROB_ARROW_WIDTH * vn.x + CHEM_PROB_ARROW_HEIGHT
									* v.x)),
					(base.y + cpa.getIntensity()
							* (CHEM_PROB_ARROW_WIDTH * vn.y + CHEM_PROB_ARROW_HEIGHT
									* v.y)));
			Point2D.Double arrowTip2 = new Point2D.Double(
					(base.x + cpa.getIntensity()
							* (-CHEM_PROB_ARROW_WIDTH * vn.x + CHEM_PROB_ARROW_HEIGHT
									* v.x)),
					(base.y + cpa.getIntensity()
							* (-CHEM_PROB_ARROW_WIDTH * vn.y + CHEM_PROB_ARROW_HEIGHT
									* v.y)));
			out.drawLine(base.x - minX, minY - base.y, edge.x - minX, minY
					- edge.y, thickness);
			out.drawLine(base.x - minX, minY - base.y, arrowTip1.x - minX, minY
					- arrowTip1.y, thickness);
			out.drawLine(base.x - minX, minY - base.y, arrowTip2.x - minX, minY
					- arrowTip2.y, thickness);
		}
			break;
		case PIN: {
			Point2D.Double side1 = new Point2D.Double(
					(edge.x - cpa.getIntensity()
							* (CHEM_PROB_PIN_SEMIDIAG * v.x)),
					(edge.y - cpa.getIntensity()
							* (CHEM_PROB_PIN_SEMIDIAG * v.y)));
			Point2D.Double side2 = new Point2D.Double(
					(edge.x - cpa.getIntensity()
							* (CHEM_PROB_PIN_SEMIDIAG * vn.x)),
					(edge.y - cpa.getIntensity()
							* (CHEM_PROB_PIN_SEMIDIAG * vn.y)));
			Point2D.Double side3 = new Point2D.Double(
					(edge.x + cpa.getIntensity()
							* (CHEM_PROB_PIN_SEMIDIAG * v.x)),
					(edge.y + cpa.getIntensity()
							* (CHEM_PROB_PIN_SEMIDIAG * v.y)));
			Point2D.Double side4 = new Point2D.Double(
					(edge.x + cpa.getIntensity()
							* (CHEM_PROB_PIN_SEMIDIAG * vn.x)),
					(edge.y + cpa.getIntensity()
							* (CHEM_PROB_PIN_SEMIDIAG * vn.y)));
			GeneralPath p2 = new GeneralPath();
			p2.moveTo((float) (side1.x - minX), (float) (minY - side1.y));
			p2.lineTo((float) (side2.x - minX), (float) (minY - side2.y));
			p2.lineTo((float) (side3.x - minX), (float) (minY - side3.y));
			p2.lineTo((float) (side4.x - minX), (float) (minY - side4.y));
			p2.closePath();
			out.fillPolygon(p2, cpa.getColor());
			out.drawLine(base.x - minX, minY - base.y, edge.x - minX, minY
					- edge.y, thickness);
		}
			break;
		case TRIANGLE: {
			Point2D.Double arrowTip1 = new Point2D.Double(
					(edge.x + cpa.getIntensity()
							* (CHEM_PROB_TRIANGLE_WIDTH * vn.x)),
					(edge.y + cpa.getIntensity()
							* (CHEM_PROB_TRIANGLE_WIDTH * vn.y)));
			Point2D.Double arrowTip2 = new Point2D.Double(
					(edge.x + cpa.getIntensity()
							* (-CHEM_PROB_TRIANGLE_WIDTH * vn.x)),
					(edge.y + cpa.getIntensity()
							* (-CHEM_PROB_TRIANGLE_WIDTH * vn.y)));
			GeneralPath p2 = new GeneralPath();
			p2.moveTo((float) (base.x - minX), (float) (minY - base.y));
			p2.lineTo((float) (arrowTip1.x - minX),
					(float) (minY - arrowTip1.y));
			p2.lineTo((float) (arrowTip2.x - minX),
					(float) (minY - arrowTip2.y));
			p2.closePath();
			out.fillPolygon(p2, cpa.getColor());
		}
			break;
		case DOT: {
			Double radius = CHEM_PROB_DOT_RADIUS * cpa.getIntensity();
			Point2D.Double center = new Point2D.Double((base.x + radius * v.x)
					- minX, minY - (base.y + radius * v.y));
			out.fillCircle(center.x, center.y, radius, thickness,
					cpa.getColor());
		}
			break;
		}
	}

	private void renderAnnotations(SecStrDrawingProducer out, double minX,
			double minY, VARNAConfig conf) {
		for (TextAnnotation textAnnotation : getAnnotations()) {
			out.setColor(textAnnotation.getColor());
			out.setFont(PSExport.FONT_HELVETICA_BOLD, 2.0 * textAnnotation
					.getFont().getSize());
			Point2D.Double position = textAnnotation.getCenterPosition();
			if (textAnnotation.getType() == TextAnnotation.AnchorType.BASE) {
				ModeleBase mb = (ModeleBase) textAnnotation.getAncrage();
				double fontHeight = Math.ceil(textAnnotation.getFont()
						.getSize());
				position = buildCaptionPosition(mb, fontHeight, conf);

			}
			out.drawText(position.x - minX, -(position.y - minY),
					textAnnotation.getTexte());
		}
		for (ChemProbAnnotation cpa : getChemProbAnnotations()) {
			Point2D.Double anchor = cpa.getAnchorPosition();
			drawChemProbAnnotation(out, cpa, anchor, minX, minY);
		}
	}

	public boolean isNumberDrawn(ModeleBase mb, int numPeriod) {
		if (numPeriod <= 0)
			return false;
		return ((mb.getIndex() == 0) || ((mb.getBaseNumber()) % numPeriod == 0) || (mb
				.getIndex() == get_listeBases().size() - 1));
	}

	public void saveRNAEPS(String path, VARNAConfig conf)
			throws ExceptionWritingForbidden {
		PSExport out = new PSExport();
		saveRNA(path, conf, 0.4, out);
	}

	public void saveRNAXFIG(String path, VARNAConfig conf)
			throws ExceptionWritingForbidden {
		XFIGExport out = new XFIGExport();
		saveRNA(path, conf, 20, out);
	}

	public void saveRNATIKZ(String path, VARNAConfig conf)
			throws ExceptionWritingForbidden {
		TikzExport out = new TikzExport();
		saveRNA(path, conf, 0.15, out);
	}

	public void saveRNASVG(String path, VARNAConfig conf)
			throws ExceptionWritingForbidden {
		SVGExport out = new SVGExport();
		saveRNA(path, conf, 0.5, out);
	}

	public Rectangle2D.Double getBBox() {
		Rectangle2D.Double result = new Rectangle2D.Double(10, 10, 10, 10);
		double minx, maxx, miny, maxy;
		minx = Double.MAX_VALUE;
		miny = Double.MAX_VALUE;
		maxx = -Double.MAX_VALUE;
		maxy = -Double.MAX_VALUE;
		for (int i = 0; i < _listeBases.size(); i++) {
			minx = Math.min(
					_listeBases.get(i).getCoords().getX() - BASE_RADIUS, minx);
			miny = Math.min(
					_listeBases.get(i).getCoords().getY() - BASE_RADIUS, miny);
			maxx = Math.max(
					_listeBases.get(i).getCoords().getX() + BASE_RADIUS, maxx);
			maxy = Math.max(
					_listeBases.get(i).getCoords().getY() + BASE_RADIUS, maxy);
		}
		result.x = minx;
		result.y = miny;
		result.width = Math.max(maxx - minx, 1);
		result.height = Math.max(maxy - miny, 1);
		if (_drawMode == RNA.DRAW_MODE_LINEAR) {
			double realHeight = _bpHeightIncrement * result.width / 2.0;
			result.height += realHeight;
			result.y -= realHeight;
		}
		return result;
	}

	public void setCoord(int index, Point2D.Double p) {
		setCoord(index, p.x, p.y);
	}

	public void setCoord(int index, double x, double y) {
		if (index < _listeBases.size()) {
			_listeBases.get(index).setCoords(new Point2D.Double(x, y));
		}
	}

	public Point2D.Double getCoords(int i) {
		if (i < _listeBases.size() && i >= 0) {
			return _listeBases.get(i).getCoords();
		}
		return new Point2D.Double();
	}

	public String getBaseContent(int i) {
		if ((i >= 0) && (i < _listeBases.size())) {
			return _listeBases.get(i).getContent();
		}
		return "";
	}

	public int getBaseNumber(int i) {
		if ((i >= 0) && (i < _listeBases.size())) {
			return _listeBases.get(i).getBaseNumber();
		}
		return -1;
	}

	public Point2D.Double getCenter(int i) {
		if (i < _listeBases.size()) {
			return _listeBases.get(i).getCenter();
		}

		return new Point2D.Double();
	}

	public void setCenter(int i, double x, double y) {
		setCenter(i, new Point2D.Double(x, y));
	}

	public void setCenter(int i, Point2D.Double p) {
		if (i < _listeBases.size()) {
			_listeBases.get(i).setCenter(p);
		}
	}

	public void drawRNACircle(VARNAConfig conf) {
		_drawn = true;
		_drawMode = DRAW_MODE_CIRCULAR;
		int radius = (int) ((3 * (_listeBases.size() + 1) * BASE_RADIUS) / (2 * Math.PI));
		double angle;
		for (int i = 0; i < _listeBases.size(); i++) {
			angle = -((((double) -(i + 1)) * 2.0 * Math.PI)
					/ ((double) (_listeBases.size() + 1)) - Math.PI / 2.0);
			_listeBases
					.get(i)
					.setCoords(
							new Point2D.Double(
									(radius * Math.cos(angle) * conf._spaceBetweenBases),
									(radius * Math.sin(angle) * conf._spaceBetweenBases)));
			_listeBases.get(i).setCenter(new Point2D.Double(0, 0));
		}
	}

	public void drawRNAVARNAView(VARNAConfig conf) {
		_drawn = true;
		_drawMode = DRAW_MODE_VARNA_VIEW;
		VARNASecDraw vs = new VARNASecDraw();
		vs.drawRNA(1, this);
	}

	public void drawRNALine(VARNAConfig conf) {
		_drawn = true;
		_drawMode = DRAW_MODE_LINEAR;
		for (int i = 0; i < get_listeBases().size(); i++) {
			get_listeBases().get(i).setCoords(
					new Point2D.Double(i * conf._spaceBetweenBases * 20, 0));
			get_listeBases().get(i).setCenter(
					new Point2D.Double(i * conf._spaceBetweenBases * 20, -10));
		}
	}

	public RNATemplateMapping drawRNATemplate(RNATemplate template, boolean straightBulges,
			VARNAConfig conf) throws RNATemplateDrawingAlgorithmException {
		return drawRNATemplate(template, conf,
				DrawRNATemplateMethod.getDefault(),
				DrawRNATemplateCurveMethod.getDefault(), straightBulges);
	}

	public RNATemplateMapping drawRNATemplate(RNATemplate template,
			VARNAConfig conf, DrawRNATemplateMethod helixLengthAdjustmentMethod, 
			boolean straightBulges)
			throws RNATemplateDrawingAlgorithmException {
		return drawRNATemplate(template, conf, helixLengthAdjustmentMethod,
				DrawRNATemplateCurveMethod.getDefault(),straightBulges);
	}

	public RNATemplateMapping drawRNATemplate(RNATemplate template,
			VARNAConfig conf,
			DrawRNATemplateMethod helixLengthAdjustmentMethod,
			DrawRNATemplateCurveMethod curveMethod,
			boolean straightBulges)
			throws RNATemplateDrawingAlgorithmException {
		_drawn = true;
		_drawMode = DRAW_MODE_TEMPLATE;

		DrawRNATemplate drawRNATemplate = new DrawRNATemplate(this);
		drawRNATemplate.drawRNATemplate(template, conf,
				helixLengthAdjustmentMethod, curveMethod, 
				straightBulges);
		return drawRNATemplate.getMapping();
	}

	private static double objFun(int n1, int n2, double r, double bpdist,
			double multidist) {
		return (((double) n1) * 2.0 * Math.asin(((double) bpdist) / (2.0 * r))
				+ ((double) n2) * 2.0
				* Math.asin(((double) multidist) / (2.0 * r)) - (2.0 * Math.PI));
	}

	public double determineRadius(int nbHel, int nbUnpaired, double startRadius) {
		return determineRadius(nbHel, nbUnpaired, startRadius,
				BASE_PAIR_DISTANCE, MULTILOOP_DISTANCE);
	}

	public static double determineRadius(int nbHel, int nbUnpaired,
			double startRadius, double bpdist, double multidist) {
		double xmin = bpdist / 2.0;
		double xmax = 3.0 * multidist + 1;
		double x = (xmin + xmax) / 2.0;
		double y = 10000.0;
		double ymin = -1000.0;
		double ymax = 1000.0;
		int numIt = 0;
		double precision = 0.00001;
		while ((Math.abs(y) > precision) && (numIt < 10000)) {
			x = (xmin + xmax) / 2.0;
			y = objFun(nbHel, nbUnpaired, x, bpdist, multidist);
			ymin = objFun(nbHel, nbUnpaired, xmax, bpdist, multidist);
			ymax = objFun(nbHel, nbUnpaired, xmin, bpdist, multidist);
			if (ymin > 0.0) {
				xmax = xmax + (xmax - xmin);
			} else if ((y <= 0.0) && (ymax > 0.0)) {
				xmax = x;
			} else if ((y >= 0.0) && (ymin < 0.0)) {
				xmin = x;
			} else if (ymax < 0.0) {
				xmin = Math.max(xmin - (x - xmin),
						Math.max(bpdist / 2.0, multidist / 2.0));
				xmax = x;
			}
			numIt++;
		}
		return x;
	}

	public void drawRNA(VARNAConfig conf) throws ExceptionNAViewAlgorithm {
		drawRNA(RNA.DEFAULT_DRAW_MODE, conf);
	}

	public void drawRNA(int mode, VARNAConfig conf)
			throws ExceptionNAViewAlgorithm {
		_drawMode = mode;
		switch (get_drawMode()) {
		case RNA.DRAW_MODE_RADIATE:
			drawRNARadiate(conf);
			break;
		case RNA.DRAW_MODE_LINEAR:
			drawRNALine(conf);
			break;
		case RNA.DRAW_MODE_CIRCULAR:
			drawRNACircle(conf);
			break;
		case RNA.DRAW_MODE_NAVIEW:
			drawRNANAView(conf);
			break;
		case RNA.DRAW_MODE_VARNA_VIEW:
			drawRNAVARNAView(conf);
			break;
		default:
			break;
		}

	}

	public int getDrawMode() {
		return _drawMode;
	}
	
	public static double HYSTERESIS_EPSILON = .15;
	public static final double[] HYSTERESIS_ATTRACTORS = {0.,Math.PI/4.,Math.PI/2.,3.*Math.PI/4.,Math.PI,5.*(Math.PI)/4.,3.*(Math.PI)/2,7.*(Math.PI)/4.};
	
	public static double normalizeAngle(double angle)
	{
		return normalizeAngle(angle,0.);
	}
	
	public static double normalizeAngle(double angle, double fromVal)
	{
		double toVal = fromVal +2.*Math.PI;
		double result = angle;
		while(result<fromVal)
		{
			result += 2.*Math.PI;
		}
		while(result >= toVal)
		{
			result -= 2.*Math.PI;
		}
		return result;		
	}
	
	public static double correctHysteresis(double angle)
	{
		double result = normalizeAngle(angle);
		
		for (int i=0;i<HYSTERESIS_ATTRACTORS.length;i++)
		{
			double att = HYSTERESIS_ATTRACTORS[i];
			if (Math.abs(normalizeAngle(att-result,-Math.PI))<HYSTERESIS_EPSILON)
			{
				result = att;
			}
		}
		return result;
	}
	

	
	private void distributeUnpaired(
			double radius,
			double angle, 
			double pHel, 
			double base,
			Point2D.Double center,
			Vector<Integer> bases)
	{
			double mydist = Math.abs(radius*(angle / (bases.size() + 1)));
			double addedRadius= 0.;
			Point2D.Double PA = new Point2D.Double(center.x + radius * Math.cos(base + pHel),
					center.y + radius * Math.sin(base + pHel));
			Point2D.Double PB = new Point2D.Double(center.x + radius * Math.cos(base + pHel+angle),
						center.y + radius * Math.sin(base + pHel+angle));
			double dist = PA.distance(PB);
			Point2D.Double VN = new Point2D.Double((PB.y-PA.y)/dist,(-PB.x+PA.x)/dist);
			if (mydist<2*BASE_RADIUS)
			{
				addedRadius=Math.min(1.0,(2*BASE_RADIUS-mydist)/4)*computeRadius(mydist, 2.29*(bases.size() + 1)*BASE_RADIUS-mydist);
			}
			
			
			ArrayList<Point2D.Double> pos = computeNewAngles(bases.size(),center,VN, angle,base + pHel,radius,addedRadius);
			for (int i = 0; i < bases.size(); i++) 
			{
				int k = bases.get(i);
				setCoord(k, pos.get(i));
			}				
		
	}

	private double computeRadius(double b, double pobj)
	{
		double a=b, aL=a, aU=Double.POSITIVE_INFINITY;
		double h = (a-b)*(a-b)/((a+b)*(a+b));
		double p = Math.PI*(a+b)*(1+h/4.+h*h/64.+h*h*h/256.+25.*h*h*h*h/16384.)/2.0;
		double aold = a+1.;
		while ((Math.abs(p-pobj)>10e-4)&&(aold!=a)){
			aold = a;
			if (p<pobj)
			{
				aL = a;
				if (aU==Double.POSITIVE_INFINITY)
				{a *= 2.;}
				else
				{ a = (a+aU)/2.; }
			}
			else
			{
				aU = a;
				a = (a+aL)/2.0;
			}
			h = (a-b)*(a-b)/((a+b)*(a+b));
			p = (Math.PI*(a+b)*(1+h/4.+h*h/64.+h*h*h/256.+25.*h*h*h*h/16384.))/2.0;
		}
		return a;
	}
	
	public static double computeAngle(Point2D.Double center, Point2D.Double p) {
		double dist = center.distance(p);
		double angle = Math.asin((p.y - center.y) / dist);
		if (p.x - center.x < 0) {
			angle = Math.PI - angle;
		}
		return angle;
	}
	
	private Point2D.Double rotatePoint(Point2D.Double center, Point2D.Double p,
			double angle) {
		double dist = p.distance(center);
		double oldAngle = Math.asin((p.y - center.y) / dist);

		if (p.x - center.x < 0) {
			oldAngle = Math.PI - oldAngle;
		}

		double newX = (center.x + dist * Math.cos(oldAngle + angle));
		double newY = (center.y + dist * Math.sin(oldAngle + angle));

		return new Point2D.Double(newX, newY);
	}


	
	private void rotateHelix(Point2D.Double center, int i, int j, double angle) {
		for (int k = i; k <= j; k++) {
			Point2D.Double oldp = getCoords(k);
			Point2D.Double newp = rotatePoint(center, oldp, angle);
			setCoord(k, newp);
			if ((k != i) && (k != j)) {

				Point2D.Double oldc = get_listeBases().get(k)
						.getCenter();
				Point2D.Double newc = rotatePoint(center, oldc, angle);
				setCenter(k,newc);
			}
		}
	}
	


	private void fixUnpairedPositions(boolean isDirect, 
			double angleRightPartner, 
			double angleLimitLeft, double angleLimitRight, double angleLeftPartner, 
			double radius, double base, Point2D.Double center,Vector<Integer> prevBases,Vector<Integer> nextBases)
	{
		if (isDirect) {
			double anglePrev = normalizeAngle(angleLimitLeft - angleRightPartner);
			double angleNext = normalizeAngle(angleLeftPartner - angleLimitRight);
			distributeUnpaired(radius,anglePrev, angleRightPartner, base,
					center,prevBases);
			distributeUnpaired(radius,-angleNext, angleLeftPartner, base,
					center,nextBases);
		} else {
			double anglePrev = normalizeAngle(angleLeftPartner - angleLimitRight);
			double angleNext = normalizeAngle(angleLimitLeft - angleRightPartner);
			distributeUnpaired(radius,-anglePrev, angleLeftPartner, base,
					center,prevBases);			
			distributeUnpaired(radius,angleNext, angleRightPartner, base,
					center,nextBases);
		}
	
	}
	
	private static Point2D.Double getPoint(double angleLine, double angleBulge, Point2D.Double center,  
			Point2D.Double VN,double radius, double addedRadius, double dirBulge)
	{
		return new Point2D.Double(
				center.x + radius * Math.cos(angleLine)+
				dirBulge*addedRadius*Math.sin(angleBulge)*VN.x,
				center.y + radius * Math.sin(angleLine)+
				dirBulge*addedRadius*Math.sin(angleBulge)*VN.y);
	}
	

	private ArrayList<Point2D.Double> computeNewAngles(int numPoints, Point2D.Double center,  
			Point2D.Double VN, double angle, double angleBase, double radius, double addedRadius)
	{
		ArrayList<Point2D.Double> result = new ArrayList<Point2D.Double>();
		if (numPoints>0)
		{
		ArrayList<Double> factors = new ArrayList<Double>();
		

		Point2D.Double prevP = new Point2D.Double(
				center.x + radius * Math.cos(angleBase),
				center.y + radius * Math.sin(angleBase));
		
		
		double fact = 0.;
		
		double angleBulge = 0.;
		double dirBulge = (angle<0)?-1.:1.;
		double dtarget =2.*BASE_RADIUS; 
		
		for (int i = 0; i < numPoints; i++) 
		{
				double lbound = fact;
				double ubound = 1.0;
				double angleLine = angleBase + angle*fact;
				angleBulge = Math.PI*fact;
				Point2D.Double currP = getPoint(angleLine, angleBulge, center,VN,radius, addedRadius, dirBulge);

				int numIter = 0;
				while ((Math.abs(currP.distance(prevP)-dtarget)>0.01)&& (numIter<100))
				{
					if (currP.distance(prevP)> dtarget)
					{
						ubound = fact;
						fact = (fact+lbound)/2.0;
					}
					else
					{
						lbound = fact;
						fact = (fact+ubound)/2.0;					
					}				
					angleLine = angleBase + angle*fact;
					angleBulge = Math.PI*fact;
					currP = getPoint(angleLine, angleBulge, center,VN,radius, addedRadius, dirBulge);
					numIter++;
				}
				factors.add(fact);
				prevP = currP;
		}
		
		
		double rescale = 1.0/(factors.get(factors.size()-1)+factors.get(0));

		for(int j=0;j<factors.size();j++)
		{
			factors.set(j,factors.get(j)*rescale);
		}
		
		 
		if (addedRadius>0)
		{
			prevP =  getPoint(angleBase, 0, center,VN,radius, addedRadius, dirBulge);
			double totDist = 0.0;
			for(int j=0;j<factors.size();j++)
			{
				double newfact = factors.get(j);
				double angleLine = angleBase + angle*newfact;
				angleBulge = Math.PI*newfact;
				Point2D.Double currP = getPoint(angleLine, angleBulge, center,VN,radius, addedRadius, dirBulge); 
				totDist += currP.distance(prevP);
				prevP = currP;
			}
			totDist += getPoint(angleBase+angle, Math.PI, center,VN,radius, addedRadius, dirBulge).distance(prevP);
			dtarget = totDist/(numPoints+1);
			fact = 0.0;
			factors=new ArrayList<Double>();
			prevP = new Point2D.Double(
					center.x + radius * Math.cos(angleBase),
					center.y + radius * Math.sin(angleBase));
			for (int i = 0; i < numPoints; i++) 
			{
					double lbound = fact;
					double ubound = 1.5;
					double angleLine = angleBase + angle*fact;
					angleBulge = Math.PI*fact;
					Point2D.Double currP = getPoint(angleLine, angleBulge, center,VN,radius, addedRadius, dirBulge);

					int numIter = 0;
					while ((Math.abs(currP.distance(prevP)-dtarget)>0.01)&& (numIter<100))
					{
						if (currP.distance(prevP)> dtarget)
						{
							ubound = fact;
							fact = (fact+lbound)/2.0;
						}
						else
						{
							lbound = fact;
							fact = (fact+ubound)/2.0;					
						}				
						angleLine = angleBase + angle*fact;
						angleBulge = Math.PI*fact;
						currP = getPoint(angleLine, angleBulge, center,VN,radius, addedRadius, dirBulge);
						numIter++;
					}
					factors.add(fact);
					prevP = currP;
			}
			rescale = 1.0/(factors.get(factors.size()-1)+factors.get(0));
				for(int j=0;j<factors.size();j++)
			{
				factors.set(j,factors.get(j)*rescale);
			}
		}	

		for(int j=0;j<factors.size();j++)
		{
			double newfact = factors.get(j);
			double angleLine = angleBase + angle*newfact;
			angleBulge = Math.PI*newfact;
			result.add(getPoint(angleLine, angleBulge, center,VN,radius, addedRadius, dirBulge));			
		}
		}	
		return result;
	}
	
	
	
	void drawLoop(int i, int j, double x, double y, double dirAngle,
			Point2D.Double[] coords, Point2D.Double[] centers, double[] angles, 
			boolean straightBulges) {
		if (i > j) {
			return;
		}

		// BasePaired
		if (_listeBases.get(i).getElementStructure() == j) {
			double normalAngle = Math.PI / 2.0;
			centers[i] = new Point2D.Double(x, y);
			centers[j] = new Point2D.Double(x, y);
			coords[i].x = (x + BASE_PAIR_DISTANCE
					* Math.cos(dirAngle - normalAngle) / 2.0);
			coords[i].y = (y + BASE_PAIR_DISTANCE
					* Math.sin(dirAngle - normalAngle) / 2.0);
			coords[j].x = (x + BASE_PAIR_DISTANCE
					* Math.cos(dirAngle + normalAngle) / 2.0);
			coords[j].y = (y + BASE_PAIR_DISTANCE
					* Math.sin(dirAngle + normalAngle) / 2.0);
			drawLoop(i + 1, j - 1, x + LOOP_DISTANCE * Math.cos(dirAngle), y
					+ LOOP_DISTANCE * Math.sin(dirAngle), dirAngle, coords,
					centers, angles, straightBulges);
		} else {
			int k = i;
			Vector<Integer> basesMultiLoop = new Vector<Integer>();
			Vector<Integer> helices = new Vector<Integer>();
			int l;
			while (k <= j) {
				l = _listeBases.get(k).getElementStructure();
				if (l > k) {
					basesMultiLoop.add(new Integer(k));
					basesMultiLoop.add(new Integer(l));
					helices.add(new Integer(k));
					k = l + 1;
				} else {
					basesMultiLoop.add(new Integer(k));
					k++;
				}
			}
			int mlSize = basesMultiLoop.size() + 2;
			int numHelices = helices.size() + 1;
			double totalLength = MULTILOOP_DISTANCE * (mlSize - numHelices)
					+ BASE_PAIR_DISTANCE * numHelices;
			double multiLoopRadius;
			double angleIncrementML;
			double angleIncrementBP;
			if (mlSize > 3) {
				multiLoopRadius = determineRadius(numHelices, mlSize
						- numHelices, (totalLength) / (2.0 * Math.PI),
						BASE_PAIR_DISTANCE, MULTILOOP_DISTANCE);
				angleIncrementML = -2.0
						* Math.asin(((float) MULTILOOP_DISTANCE)
								/ (2.0 * multiLoopRadius));
				angleIncrementBP = -2.0
						* Math.asin(((float) BASE_PAIR_DISTANCE)
								/ (2.0 * multiLoopRadius));
			} 
			else {
				multiLoopRadius = 35.0;
				angleIncrementBP = -2.0
						* Math.asin(((float) BASE_PAIR_DISTANCE)
								/ (2.0 * multiLoopRadius));
				angleIncrementML = (-2.0 * Math.PI - angleIncrementBP) / 2.0;
			}
			// System.out.println("MLr:"+multiLoopRadius+" iBP:"+angleIncrementBP+" iML:"+angleIncrementML);

			double centerDist = Math.sqrt(Math.max(Math.pow(multiLoopRadius, 2)
					- Math.pow(BASE_PAIR_DISTANCE / 2.0, 2), 0.0))
					- LOOP_DISTANCE;
			Point2D.Double mlCenter = new Point2D.Double(
					(x + (centerDist * Math.cos(dirAngle))),
					(y + (centerDist * Math.sin(dirAngle))));

			// Base directing angle for (multi|hairpin) loop, from the center's
			// perspective
			double baseAngle = dirAngle
					// U-turn
					+ Math.PI
					// Account for already drawn supporting base-pair
					+ 0.5 * angleIncrementBP
					// Base cannot be paired twice, so next base is at
					// "unpaired base distance"
					+ 1.0 * angleIncrementML;
			
			ArrayList<Integer> currUnpaired = new ArrayList<Integer>();
			Couple<Double,Double> currInterval = new Couple<Double,Double>(0.,baseAngle-1.0 * angleIncrementML);
			ArrayList<Couple<ArrayList<Integer>,Couple<Double,Double>>> intervals = new ArrayList<Couple<ArrayList<Integer>,Couple<Double,Double>>>();
			
			for (k = basesMultiLoop.size() - 1; k >= 0; k--) {
				l = basesMultiLoop.get(k).intValue();
				//System.out.println(l+" ");
				centers[l] = mlCenter;
				boolean isPaired = (_listeBases.get(l).getElementStructure() != -1);
				boolean isPaired3 = isPaired && (_listeBases.get(l).getElementStructure() < l);
				boolean isPaired5 = isPaired && !isPaired3;
				if (isPaired3) {
					if ((numHelices == 2) && straightBulges)
					{
						baseAngle = dirAngle-angleIncrementBP/2.;
					}
					else
					{
						baseAngle = correctHysteresis(baseAngle+angleIncrementBP/2.)-angleIncrementBP/2.;
					}
					currInterval.first = baseAngle;
					intervals.add(new Couple<ArrayList<Integer>,Couple<Double,Double>>(currUnpaired,currInterval));
					currInterval = new Couple<Double,Double>(-1.,-1.);  
					currUnpaired = new ArrayList<Integer>();
				}
				else if (isPaired5)
				{
					currInterval.second = baseAngle;
				}
				else
				{
					currUnpaired.add(l);
				}

				angles[l] = baseAngle;
				if (isPaired3)
				{ 
					baseAngle += angleIncrementBP;
				}
				else {
					baseAngle += angleIncrementML;
				}
			}
			currInterval.first = dirAngle
					- Math.PI
					- 0.5 * angleIncrementBP;
			intervals.add(new Couple<ArrayList<Integer>,Couple<Double,Double>>(currUnpaired,currInterval));
			//System.out.println("Inc. ML:"+angleIncrementML+" BP:"+angleIncrementBP);
			
			for(Couple<ArrayList<Integer>,Couple<Double,Double>> inter: intervals)
			{
				//double mid = inter.second.second;
				double mina = inter.second.first;
				double maxa = normalizeAngle(inter.second.second,mina);
				//System.out.println(""+mina+" " +maxa);
				
				for (int n=0;n<inter.first.size();n++)
				{
					double ratio = (1.+n)/(1.+inter.first.size());
					int b = inter.first.get(n);
					angles[b] = mina + (1.-ratio)*(maxa-mina);
				}
			}
			
			
			for (k = basesMultiLoop.size() - 1; k >= 0; k--) {
				l = basesMultiLoop.get(k).intValue();
				coords[l].x = mlCenter.x + multiLoopRadius
						* Math.cos(angles[l]);
				coords[l].y = mlCenter.y + multiLoopRadius
						* Math.sin(angles[l]);
			}	
			
			// System.out.println("n1:"+n1+" n2:"+n2);
			double newAngle;
			int m, n;
			for (k = 0; k < helices.size(); k++) {
				m = helices.get(k).intValue();
				n = _listeBases.get(m).getElementStructure();
				newAngle = (angles[m] + angles[n]) / 2.0;
				drawLoop(m + 1, n - 1, (LOOP_DISTANCE * Math.cos(newAngle))
						+ (coords[m].x + coords[n].x) / 2.0,
						(LOOP_DISTANCE * Math.sin(newAngle))
								+ (coords[m].y + coords[n].y) / 2.0, newAngle,
						coords, centers, angles, straightBulges);
			}
		}
	}

	private Vector<Integer> getPreviousUnpaired(Point h)
	{
		Vector<Integer> prevBases = new Vector<Integer>();
		boolean over = false;
		int i = h.y + 1;
		while (!over) {
			if (i >=get_listeBases().size()) {
				over = true;
			} else {
				if (get_listeBases().get(i)
						.getElementStructure() == -1) {
					prevBases.add(new Integer(i));
				} else {
					over = true;
				}
			}
			i++;
		}
		return prevBases;
	}

	private Vector<Integer> getNextUnpaired(Point h)
	{
		boolean over = false;
		int i = h.x - 1;
		Vector<Integer> nextBases = new Vector<Integer>();
		while (!over) {
			if (i < 0) {
				over = true;
			} else {
				if (get_listeBases().get(i)
						.getElementStructure() == -1) {
					nextBases.add(new Integer(i));
				} else {
					over = true;
				}
			}
			i--;
		}
		return nextBases;
	}

	
	public void rotateEverything(double delta, double base, double pLimL, double pLimR, Point h, Point ml, Hashtable<Integer,Point2D.Double> backupPos)
	{
		boolean isDirect = testDirectionality(ml.x, ml.y, h.x);
		Point2D.Double center = get_listeBases().get(h.x).getCenter();
		for(int k=h.x;k<=h.y;k++)
		{ backupPos.put(k, getBaseAt(k).getCoords()); }
		rotateHelix(center, h.x, h.y, delta);
		
		// Re-assigns unpaired atoms
		Point2D.Double helixStart = getCoords(h.x);
		Point2D.Double helixStop = getCoords(h.y);
		double pHelR,pHelL;
		if (isDirect) {
			pHelR = computeAngle(center, helixStop) - base;
			pHelL = computeAngle(center, helixStart) - base;
		} else {
			pHelL = computeAngle(center, helixStop) - base;
			pHelR = computeAngle(center, helixStart) - base;
		}

		Vector<Integer> prevBases = getPreviousUnpaired(h);
		Vector<Integer> nextBases = getNextUnpaired(h);
		
		double radius = center.distance(helixStart);

		for (int j = 0; j < prevBases.size(); j++) 
		{
			int k = prevBases.get(j);
			backupPos.put(k, getCoords(k));
		}
		for (int j = 0; j < nextBases.size(); j++) 
		{
			int k = nextBases.get(j);
			backupPos.put(k, getCoords(k));
		}
		fixUnpairedPositions(isDirect, pHelR, pLimL, pLimR, pHelL, radius, base,center,prevBases,nextBases);		
	}
	
	
	
	public void drawRNARadiate() {
		drawRNARadiate(-1.0, VARNAConfig.DEFAULT_SPACE_BETWEEN_BASES, true, true);
	}

	public void drawRNARadiate(VARNAConfig conf) {
		drawRNARadiate(-1.0, conf._spaceBetweenBases, conf._flatExteriorLoop, false);
	}

	public static final double FLAT_RECURSIVE_INCREMENT = 20.;
	
	public void drawRNARadiate(double dirAngle, double _spaceBetweenBases,
			boolean flatExteriorLoop, boolean straightBulges) {
		_drawn = true;
		straightBulges = true;
		_drawMode = DRAW_MODE_RADIATE;
		Point2D.Double[] coords = new Point2D.Double[_listeBases.size()];
		Point2D.Double[] centers = new Point2D.Double[_listeBases.size()];
		double[] angles = new double[_listeBases.size()];
		for (int i = 0; i < _listeBases.size(); i++) {
			coords[i] = new Point2D.Double(0, 0);
			centers[i] = new Point2D.Double(0, 0);
		}
		if (flatExteriorLoop) {
			dirAngle += 1.0 - Math.PI / 2.0;
			int i = 0;
			double x = 0.0;
			double y = 0.0;
			double vx = -Math.sin(dirAngle);
			double vy = Math.cos(dirAngle);
			while (i < _listeBases.size()) {
				coords[i].x = x;
				coords[i].y = y;
				centers[i].x = x + BASE_PAIR_DISTANCE * vy;
				centers[i].y = y - BASE_PAIR_DISTANCE * vx;
				int j = _listeBases.get(i).getElementStructure();
				if (j > i) {
					double increment = 0.;
					if (i+1<_listeBases.size())
					{
						if (_listeBases.get(i+1).getElementStructure()==-1)
						{
							//increment = -FLAT_RECURSIVE_INCREMENT;
						}
					}
					drawLoop(i, j, x + (BASE_PAIR_DISTANCE * vx / 2.0), y
							+ (BASE_PAIR_DISTANCE * vy / 2.0)+increment, dirAngle,
							coords, centers, angles, straightBulges);
					centers[i].x = coords[i].x + BASE_PAIR_DISTANCE * vy;
					centers[i].y = y - BASE_PAIR_DISTANCE * vx;
					i = j;
					x += BASE_PAIR_DISTANCE * vx;
					y += BASE_PAIR_DISTANCE * vy;
					centers[i].x = coords[i].x + BASE_PAIR_DISTANCE * vy;
					centers[i].y = y - BASE_PAIR_DISTANCE * vx;
				}
				x += MULTILOOP_DISTANCE * vx;
				y += MULTILOOP_DISTANCE * vy;
				i += 1;
			}
		} else {
			drawLoop(0, _listeBases.size() - 1, 0, 0, dirAngle, coords, centers, angles, straightBulges);
		}
		for (int i = 0; i < _listeBases.size(); i++) {
			_listeBases.get(i).setCoords(
					new Point2D.Double(coords[i].x * _spaceBetweenBases,
							coords[i].y * _spaceBetweenBases));
			_listeBases.get(i).setCenter(
					new Point2D.Double(centers[i].x * _spaceBetweenBases,
							centers[i].y * _spaceBetweenBases));
		}

		// TODO
		// change les centres des bases de la premiere helice vers la boucle la
		// plus proche
	}

	public void drawRNANAView(VARNAConfig conf) throws ExceptionNAViewAlgorithm {
		_drawMode = DRAW_MODE_NAVIEW;
		_drawn = true;

		ArrayList<Double> X = new ArrayList<Double>(_listeBases.size());
		ArrayList<Double> Y = new ArrayList<Double>(_listeBases.size());
		ArrayList<Short> pair_table = new ArrayList<Short>(_listeBases.size());

		for (int i = 0; i < _listeBases.size(); i++) {
			pair_table.add(Short.valueOf(String.valueOf(_listeBases.get(i)
					.getElementStructure())));
		}
		NAView naView = new NAView();
		naView.naview_xy_coordinates(pair_table, X, Y);

		// Updating individual base positions
		for (int i = 0; i < _listeBases.size(); i++) {
			_listeBases.get(i).setCoords(
					new Point2D.Double(
							X.get(i) * 2.5 * conf._spaceBetweenBases, Y.get(i)
									* 2.5 * conf._spaceBetweenBases));
		}

		// Updating centers
		for (int i = 0; i < _listeBases.size(); i++) {
			int indicePartner = _listeBases.get(i).getElementStructure();
			if (indicePartner != -1) {
				Point2D.Double base = _listeBases.get(i).getCoords();
				Point2D.Double partner = _listeBases.get(indicePartner)
						.getCoords();
				_listeBases.get(i).setCenter(
						new Point2D.Double((base.x + partner.x) / 2.0,
								(base.y + partner.y) / 2.0));
			} else {
				Vector<Integer> loop = getLoopBases(i);
				double tmpx = 0.0;
				double tmpy = 0.0;
				for (int j = 0; j < loop.size(); j++) {
					int partner = loop.elementAt(j);
					Point2D.Double loopmember = _listeBases.get(partner)
							.getCoords();
					tmpx += loopmember.x;
					tmpy += loopmember.y;
				}
				_listeBases.get(i).setCenter(
						new Point2D.Double(tmpx / loop.size(), tmpy
								/ loop.size()));
			}
		}
	}

	/*
	 * public void drawMOTIFView() { _drawn = true; _drawMode =
	 * DRAW_MODE_MOTIFVIEW; int spaceBetweenStrand =0; Motif motif = new
	 * Motif(this,get_listeBases()); motif.listStrand(); for (int i = 0; i <
	 * motif.getListStrand().sizeStruct(); i++ ){ for (int j = 0; j <
	 * motif.getListStrand().getStrand(i).sizeStrand(); j++ ){ int indice =
	 * motif.getListStrand().getStrand(i).getMB(j).getIndex();
	 * get_listeBases().get(indice).setCoords( new Point2D.Double(0,0));
	 * get_listeBases().get(indice).setCenter( new Point2D.Double(0, 0));
	 * 
	 * } } //Recherche du brin central int centralStrand =
	 * motif.getCentralStrand();
	 * 
	 * //Cas o? l'on a un motif en ?toile if(centralStrand!=-1){ //On positionne
	 * le brin central motif.positionneSpecificStrand(centralStrand,
	 * spaceBetweenStrand);
	 * 
	 * //On place les autres brins par rapport a ce brin central
	 * motif.orderStrands(centralStrand); }
	 * 
	 * else { centralStrand = 0; motif.positionneStrand(); motif.ajusteStrand();
	 * } motif.reajustement(); motif.deviationBasePair();
	 * motif.setCenterMotif(); }
	 */

	public ArrayList<ModeleBase> getAllPartners(int indice) {
		ArrayList<ModeleBase> result = new ArrayList<ModeleBase>();
		ModeleBase me = this.getBaseAt(indice);
		int i = me.getElementStructure();
		if (i != -1) {
			result.add(getBaseAt(i));
		}
		ArrayList<ModeleBP> msbps = getAuxBPs(indice);
		for (ModeleBP m : msbps) {
			result.add(m.getPartner(me));
		}
		return result;
	}

	public int get_drawMode() {
		return _drawMode;
	}

	public void setDrawMode(int drawMode) {
		_drawMode = drawMode;
	}

	public Set<Integer> getSeparatorPositions(String s) {
		HashSet<Integer> result = new HashSet<Integer>();
		int index = s.indexOf(DBNStrandSep);
		while (index >= 0) {
			result.add(index);
			index = s.indexOf(DBNStrandSep, index + 1);
		}
		return result;
	}

	public static String DBNStrandSep = "&";

	public void setRNA(String seq, String str)
			throws ExceptionFileFormatOrSyntax,
			ExceptionUnmatchedClosingParentheses {
		ArrayList<String> al = RNA.explodeSequence(seq);
		Set<Integer> sepPos = getSeparatorPositions(str);
		ArrayList<String> alRes = new ArrayList<String>();
		Set<Integer> resSepPos = new HashSet<Integer>();
		String strRes = "";
		for (int i = 0; i < al.size(); i++) {
			if (sepPos.contains(i) && al.get(i).equals(DBNStrandSep)) {
				resSepPos.add(alRes.size() - 1);
			} else {
				alRes.add(al.get(i));
				if (i<str.length())
				{
					strRes += str.charAt(i);
				}
				else
				{
					strRes += '.';
				}
			}
		}
		for (int i = al.size(); i < str.length(); i++) {
			alRes.add(" ");
			strRes += str.charAt(i);
		}
		setRNA(alRes, strRes);
		for (int i : resSepPos) {
			_backbone.addElement(new ModeleBackboneElement(i,
					BackboneType.DISCONTINUOUS_TYPE));
		}
	}

	public void setRNA(String seq) {
		ArrayList<String> s = RNA.explodeSequence(seq);
		int[] str = new int[s.size()];
		for (int i = 0; i < str.length; i++) {
			str[i] = -1;
		}
		try {
			setRNA(s, str);
		} catch (ExceptionFileFormatOrSyntax e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setRNA(String seq, int[] str)
			throws ExceptionFileFormatOrSyntax,
			ExceptionUnmatchedClosingParentheses {
		setRNA(RNA.explodeSequence(seq), str);
	}

	public void setRNA(String[] seq, int[] str)
			throws ExceptionFileFormatOrSyntax {
		setRNA(seq, str, 1);
	}

	public void setRNA(List<String> seq, int[] str)
			throws ExceptionFileFormatOrSyntax {
		setRNA(seq.toArray(new String[seq.size()]), str, 1);
	}

	public void setRNA(List<String> seq, int[] str, int baseIndex)
			throws ExceptionFileFormatOrSyntax {
		setRNA(seq.toArray(new String[seq.size()]), str, baseIndex);
	}

	public void setRNA(String[] seq, int[] str, int baseIndex)
			throws ExceptionFileFormatOrSyntax {
		clearAnnotations();
		_listeBases = new ArrayList<ModeleBase>();
		if (seq.length != str.length) {
			warningEmition("Sequence length " + seq.length
					+ " differs from that of secondary structure " + str.length
					+ ". \nAdapting sequence length ...");
			if (seq.length < str.length) {
				String[] nseq = new String[str.length];
				for (int i = 0; i < seq.length; i++) {
					nseq[i] = seq[i];
				}
				for (int i = seq.length; i < nseq.length; i++) {
					nseq[i] = "";
				}
				seq = nseq;
			} else {
				String[] seqTmp = new String[str.length];
				for (int i = 0; i < str.length; i++) {
					seqTmp[i] = seq[i];
				}
				seq = seqTmp;
			}
		}
		for (int i = 0; i < str.length; i++) {
			_listeBases.add(new ModeleBaseNucleotide(seq[i], i, baseIndex + i));
		}
		applyStruct(str);
	}

	/**
	 * Sets the RNA to be drawn. Uses when comparison mode is on. Will draw the
	 * super-structure passed in parameters and apply specials styles to the
	 * bases owning by each RNA alignment and both.
	 * 
	 * @param seq
	 *            - The sequence of the super-structure This sequence shall be
	 *            designed like this:
	 *            <code>firstRNA1stBaseSecondRNA1stBaseFirstRNA2ndBaseSecondRNA2ndBase [...]</code>
	 * <br>
	 *            <b>Example:</b> <code>AAC-GUAGA--UGG</code>
	 * @param struct
	 *            - The super-structure
	 * @param basesOwn
	 *            - The RNA owning bases array (each index will be:0 when common
	 *            base, 1 when first RNA alignment base, 2 when second RNA
	 *            alignment base)
	 * @throws ExceptionUnmatchedClosingParentheses
	 * @throws ExceptionFileFormatOrSyntax
	 */
	public void setRNA(String seq, String struct, ArrayList<Integer> basesOwn)
			throws ExceptionUnmatchedClosingParentheses,
			ExceptionFileFormatOrSyntax {
		clearAnnotations();
		_listeBases = new ArrayList<ModeleBase>();
		// On "parse" la structure (repérage des points, tiret et couples
		// parentheses ouvrante/fermante)
		int[] array_struct = parseStruct(struct);
		int size = struct.length();
		int j = 0;
		for (int i = 0; i < size; i++) {
			ModeleBase mb;
			if (seq.charAt(j) != seq.charAt(j + 1)) {
				ModeleBasesComparison mbc = new ModeleBasesComparison(
						seq.charAt(j), seq.charAt(j + 1), i);
				mbc.set_appartenance(basesOwn.get(i));
				mbc.setBaseNumber(i + 1);
				mb = mbc;
			} else {
				mb = new ModeleBaseNucleotide("" + seq.charAt(j), i, i + 1);

			}
			_listeBases.add(mb);
			j += 2;
		}
		for (int i = 0; i < size; i++) {
			if (array_struct[i] != -1) {
				this.addBPNow(i, array_struct[i]);
			}

			j += 2;
		}
	}

	public void setRNA(List<String> seq, String dbnStr)
			throws ExceptionUnmatchedClosingParentheses,
			ExceptionFileFormatOrSyntax {
		clearAnnotations();
		int[] finStr = RNAFactory.parseSecStr(dbnStr);
		setRNA(seq, finStr);
	}

	public static ArrayList<String> explodeSequence(String seq) {
		ArrayList<String> analyzedSeq = new ArrayList<String>();
		int i = 0;
		while (i < seq.length()) {
			if (seq.charAt(i) == '{') {
				boolean found = false;
				String buf = "";
				i++;
				while (!found & (i < seq.length())) {
					if (seq.charAt(i) != '}') {
						buf += seq.charAt(i);
						i++;
					} else {
						found = true;
					}
				}
				analyzedSeq.add(buf);
			} else {
				analyzedSeq.add("" + seq.charAt(i));
			}
			i++;
		}
		return analyzedSeq;
	}

	public int[] parseStruct(String str)
			throws ExceptionUnmatchedClosingParentheses,
			ExceptionFileFormatOrSyntax {
		int[] result = new int[str.length()];
		int unexpectedChar = -1;
		Stack<Integer> p = new Stack<Integer>();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '(') {
				p.push(new Integer(i));
			} else if (c == '.' || c == '-' || c == ':') {
				result[i] = -1;
			} else if (c == ')') {
				if (p.size() == 0) {
					throw new ExceptionUnmatchedClosingParentheses(i + 1);
				}
				int j = p.pop().intValue();
				result[i] = j;
				result[j] = i;
			} else {
				if (unexpectedChar == -1)
					unexpectedChar = i;
				break;
			}
		}

		if (unexpectedChar != -1) {
			// warningEmition("Unexpected Character at index:" +
			// unexpectedChar);
		}

		if (p.size() != 0) {
			throw new ExceptionUnmatchedClosingParentheses(
					p.pop().intValue() + 1);
		}

		return result;
	}

	public Point getHelixInterval(int index) {
		if ((index < 0) || (index >= _listeBases.size())) {
			return new Point(index, index);
		}
		int j = _listeBases.get(index).getElementStructure();
		if (j != -1) {
			int minH = index;
			int maxH = index;
			if (j > index) {
				maxH = j;
			} else {
				minH = j;
			}
			boolean over = false;
			while (!over) {
				if ((minH < 0) || (maxH >= _listeBases.size())) {
					over = true;
				} else {
					if (_listeBases.get(minH).getElementStructure() == maxH) {
						minH--;
						maxH++;
					} else {
						over = true;
					}
				}
			}
			minH++;
			maxH--;
			return new Point(minH, maxH);
		}
		return new Point(0, 0);
	}

	public Point getExteriorHelix(int index) {
		Point h = getHelixInterval(index);
		int a = h.x;
		int b = h.y;		
		while (!((h.x==0)))
		{			
			a = h.x;
			b = h.y;		
			h = getHelixInterval(a-1);
		}
		return new Point(a, b);
	}
	
	
	public ArrayList<Integer> getHelix(int index) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		if ((index < 0) || (index >= _listeBases.size())) {
			return result;
		}
		Point p = getHelixInterval(index);
		for (int i = p.x; i <= p.y; i++) {
			result.add(i);
			result.add(this._listeBases.get(i).getElementStructure());
		}
		return result;
	}

	public Point getMultiLoop(int index) {
		if ((index < 0) || (index >= _listeBases.size())) {
			return new Point(index, index);
		}
		Point h = getHelixInterval(index);
		int minH = h.x - 1;
		int maxH = h.y + 1;
		boolean over = false;
		while (!over) {
			if (minH < 0) {
				over = true;
				minH = 0;
			} else {
				if (_listeBases.get(minH).getElementStructure() == -1) {
					minH--;
				} else if (_listeBases.get(minH).getElementStructure() < minH) {
					minH = _listeBases.get(minH).getElementStructure() - 1;
				} else {
					over = true;
				}
			}
		}
		over = false;
		while (!over) {
			if (maxH > _listeBases.size() - 1) {
				over = true;
				maxH = _listeBases.size() - 1;
			} else {
				if (_listeBases.get(maxH).getElementStructure() == -1) {
					maxH++;
				} else if (_listeBases.get(maxH).getElementStructure() > maxH) {
					maxH = _listeBases.get(maxH).getElementStructure() + 1;
				} else {
					over = true;
				}
			}
		}
		return new Point(minH, maxH);
	}

	public Vector<Integer> getLoopBases(int startIndex) {
		Vector<Integer> result = new Vector<Integer>();

		if ((startIndex < 0) || (startIndex >= _listeBases.size())) {
			return result;
		}
		int index = startIndex;
		result.add(startIndex);
		if (_listeBases.get(index).getElementStructure() <= index) {
			index = (index + 1) % _listeBases.size();
		} else {
			index = _listeBases.get(index).getElementStructure();
			result.add(index);
			index = (index + 1) % _listeBases.size();
		}

		while (index != startIndex) {
			result.add(index);
			if (_listeBases.get(index).getElementStructure() == -1) {
				index = (index + 1) % _listeBases.size();
			} else {
				index = _listeBases.get(index).getElementStructure();
				result.add(index);
				index = (index + 1) % _listeBases.size();
			}
		}
		return result;
	}

	/**
	 * Returns the RNA secondary structure displayed by this panel as a
	 * well-parenthesized word, accordingly to the DBN format
	 * 
	 * @return This panel's secondary structure
	 */
	public String getStructDBN() {
		String result = "";
		for (int i = 0; i < _listeBases.size(); i++) {
			int j = _listeBases.get(i).getElementStructure();
			if (j == -1) {
				result += ".";
			} else if (i > j) {
				result += ")";
			} else {
				result += "(";
			}
		}
		return addStrandSeparators(result);
	}

	private ArrayList<ModeleBP> getNonCrossingSubset(
			ArrayList<ArrayList<ModeleBP>> rankedBPs) {
		ArrayList<ModeleBP> currentBPs = new ArrayList<ModeleBP>();
		Stack<Integer> pile = new Stack<Integer>();
		for (int i = 0; i < rankedBPs.size(); i++) {
			ArrayList<ModeleBP> lbp = rankedBPs.get(i);
			if (!lbp.isEmpty()) {
				ModeleBP bp = lbp.get(0);
				boolean ok = true;
				if (!pile.empty()) {
					int x = pile.peek();
					if ((bp.getIndex3() >= x)) {
						ok = false;
					}
				}
				if (ok) {
					lbp.remove(0);
					currentBPs.add(bp);
					pile.add(bp.getIndex3());
				}
			}
			if (!pile.empty() && (i == pile.peek())) {
				pile.pop();
			}
		}
		return currentBPs;
	}

	public ArrayList<int[]> paginateStructure() {
		ArrayList<int[]> result = new ArrayList<int[]>();
		// Mumbo jumbo to sort the basepair list
		ArrayList<ModeleBP> bps = this.getAllBPs();
		ModeleBP[] mt = new ModeleBP[bps.size()];
		bps.toArray(mt);
		Arrays.sort(mt, new Comparator<ModeleBP>() {
			public int compare(ModeleBP arg0, ModeleBP arg1) {
				if (arg0.getIndex5() != arg1.getIndex5())
					return arg0.getIndex5() - arg1.getIndex5();
				else
					return arg0.getIndex3() - arg1.getIndex3();

			}
		});
		ArrayList<ArrayList<ModeleBP>> rankedBps = new ArrayList<ArrayList<ModeleBP>>();
		for (int i = 0; i < getSize(); i++) {
			rankedBps.add(new ArrayList<ModeleBP>());
		}
		for (int i = 0; i < mt.length; i++) {
			rankedBps.get(mt[i].getIndex5()).add(mt[i]);
		}

		while (!bps.isEmpty()) {
			//System.out.println("Page: " + result.size());
			ArrayList<ModeleBP> currentBPs = getNonCrossingSubset(rankedBps);
			int[] ss = new int[this.getSize()];
			for (int i = 0; i < ss.length; i++) {
				ss[i] = -1;
			}

			for (int i = 0; i < currentBPs.size(); i++) {
				ModeleBP mbp = currentBPs.get(i);
				ss[mbp.getIndex3()] = mbp.getIndex5();
				ss[mbp.getIndex5()] = mbp.getIndex3();
			}
			bps.removeAll(currentBPs);
			result.add(ss);
		}
		return result;
	}

	private void showBasic(int[] res) {
		for (int i = 0; i < res.length; i++) {
			System.out.print(res[i] + ",");
		}
		System.out.println();

	}
	
	public int[] getStrandShifts()
	{
		int[] result = new int[getSize()];
		int acc = 0;
		for (int i=0;i<getSize();i++)
		{
			if (_backbone.getTypeBefore(i)==BackboneType.DISCONTINUOUS_TYPE)
			{
				acc++;
			}
			result[i] = acc;
		}
		return result;
		
	}
	
	public String addStrandSeparators(String s)
	{
		String res = "";
		for (int i=0;i<s.length();i++)
		{
			res += s.charAt(i);
			if (_backbone.getTypeAfter(i)==BackboneType.DISCONTINUOUS_TYPE)
			{
				res += DBNStrandSep;
			}
		}
		return res;
	}
	

	public String getStructDBN(boolean includeMostPKs) {
		String result = getStructDBN();
		if (includeMostPKs) {
			ArrayList<int[]> pages = paginateStructure();
			char[] res = new char[getSize()];
			for (int i = 0; i < res.length; i++) {
				res[i] = '.';
			}
			char[] open = { '(', '[', '{', '<', 'A', 'B', 'C', 'D', 'E', 'F',
					'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
					'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
			
			char[] close = new char[open.length];
			close[0] = ')';
			close[1] = ']';
			close[2] = '}';
			close[3] = '>';
			for (int i=4; i<open.length;i++)
			{
				close[i] = Character.toLowerCase(open[i]);
			}
			
			for (int p = 0; p < Math.min(pages.size(), open.length); p++) {
				int[] page = pages.get(p);
				//showBasic(page);
				for (int i = 0; i < res.length; i++) {
					if (page[i] != -1 && page[i] > i && res[i] == '.'
							&& res[page[i]] == '.') {
						res[i] = open[p];
						res[page[i]] = close[p];
					}
				}
			}
			result = "";
			for (int i = 0; i < res.length; i++) {
				result += res[i];
			}

		}
		return addStrandSeparators(result);

	}

	public String getStructDBN(int[] str) {
		String result = "";
		for (int i = 0; i < str.length; i++) {
			if (str[i] == -1) {
				result += ".";
			} else if (str[i] > i) {
				result += "(";
			} else {
				result += ")";
			}
		}
		return addStrandSeparators(result);
	}

	/**
	 * Returns the raw nucleotides sequence for the displayed RNA
	 * 
	 * @return The RNA sequence
	 */
	public String getSeq() {
		String result = "";
		for (int i = 0; i < _listeBases.size(); i++) {
			result += ((ModeleBase) _listeBases.get(i)).getContent();
		}
		return addStrandSeparators(result);
	}

	public String getStructBPSEQ() {
		String result = "";
		int[] str = getNonOverlappingStruct();
		for (int i = 0; i < _listeBases.size(); i++) {
			result += (i + 1) + " "
					+ ((ModeleBaseNucleotide) _listeBases.get(i)).getContent()
					+ " " + (str[i] + 1) + "\n";
		}
		return result;
	}

	public int[] getNonCrossingStruct() {
		int[] result = new int[_listeBases.size()];
		// Adding "planar" base-pairs
		for (int i = 0; i < _listeBases.size(); i++) {
			result[i] = _listeBases.get(i).getElementStructure();
		}
		return result;
	}

	public int[] getNonOverlappingStruct() {
		int[] result = getNonCrossingStruct();
		// Adding additional base pairs when possible (No more than one
		// base-pair per base)
		for (int i = 0; i < _structureAux.size(); i++) {
			ModeleBP msbp = _structureAux.get(i);
			ModeleBase mb5 = msbp.getPartner5();
			ModeleBase mb3 = msbp.getPartner3();
			int j5 = mb5.getIndex();
			int j3 = mb3.getIndex();
			if ((result[j3] == -1) && (result[j5] == -1)) {
				result[j3] = j5;
				result[j5] = j3;
			}
		}
		return result;
	}

	public String getStructCT() {
		String result = "";
		for (int i = 0; i < _listeBases.size(); i++) {
			result += (i + 1) + " "
					+ ((ModeleBase) _listeBases.get(i)).getContent() + " " + i
					+ " " + (i + 2) + " "
					+ (_listeBases.get(i).getElementStructure() + 1) + " "
					+ (i + 1) + "\n";
		}
		return result;
	}

	public void saveAsBPSEQ(String path, String title)
			throws ExceptionExportFailed, ExceptionPermissionDenied {
		try {
			FileWriter f = new FileWriter(path);
			f.write("# " + title + "\n");
			f.write(this.getStructBPSEQ() + "\n");
			f.close();
		} catch (IOException e) {
			throw new ExceptionExportFailed(e.getMessage(), path);
		}
	}

	public void saveAsCT(String path, String title)
			throws ExceptionExportFailed, ExceptionPermissionDenied {
		try {
			FileWriter f = new FileWriter(path);
			f.write("" + _listeBases.size() + " " + title + "\n");
			f.write(this.getStructCT() + "\n");
			f.close();
		} catch (IOException e) {
			throw new ExceptionExportFailed(e.getMessage(), path);
		}
	}

	public void saveAsDBN(String path, String title)
			throws ExceptionExportFailed, ExceptionPermissionDenied {
		try {
			FileWriter f = new FileWriter(path);
			f.write("> " + title + "\n");
			f.write(getListeBasesToString() + "\n");
			f.write(getStructDBN() + "\n");
			f.close();
		} catch (IOException e) {
			throw new ExceptionExportFailed(e.getMessage(), path);
		}
	}

	public String getListeBasesToString() {
		String s = new String();
		for (int i = 0; i < _listeBases.size(); i++) {
			s += ((ModeleBaseNucleotide) _listeBases.get(i)).getContent();
		}
		return addStrandSeparators(s);
	}

	public void applyBPs(ArrayList<ModeleBP> allbps) {
		ArrayList<ModeleBP> planar = new ArrayList<ModeleBP>();
		ArrayList<ModeleBP> others = new ArrayList<ModeleBP>();
		// System.err.println("Sequence: "+this.getSeq());
		RNAMLParser.planarize(allbps, planar, others, getSize());
		// System.err.println("All:"+allbps);
		// System.err.println("=> Planar: "+planar);
		// System.err.println("=> Others: "+others);

		for (ModeleBP mb : planar) {
			addBPnow(mb.getPartner5().getIndex(), mb.getPartner3().getIndex(),
					mb);
		}

		for (ModeleBP mb : others) {
			addBPAux(mb.getPartner5().getIndex(), mb.getPartner3().getIndex(),
					mb);
		}
	}

	public void set_listeBases(ArrayList<ModeleBase> _liste) {
		this._listeBases = _liste;
	}

	public void addVARNAListener(InterfaceVARNAListener rl) {
		_listeVARNAListener.add(rl);
	}

	public void warningEmition(String warningMessage) {
		for (int i = 0; i < _listeVARNAListener.size(); i++) {
			_listeVARNAListener.get(i).onWarningEmitted(warningMessage);
		}
	}

	public void applyStyleOnBases(ArrayList<Integer> basesList,
			ModelBaseStyle style) {
		for (int i = 1; i < basesList.size(); i++) {
			_listeBases.get(basesList.get(i)).setStyleBase(style);
		}
	}

	private int[] correctReciprocity(int[] str) {
		int[] result = new int[str.length];
		for (int i = 0; i < str.length; i++) {
			if (str[i] != -1) {
				if (i == str[str[i]]) {
					result[i] = str[i];
				} else {
					str[str[i]] = i;
				}
			} else {
				result[i] = -1;
			}
		}
		return result;
	}

	private void applyStruct(int[] str) throws ExceptionFileFormatOrSyntax {
		str = correctReciprocity(str);

		int[] planarSubset = RNAMLParser.planarize(str);
		_structureAux.clear();

		for (int i = 0; i < planarSubset.length; i++) {
			if (str[i] > i) {
				if (planarSubset[i] > i) {
					addBPNow(i, planarSubset[i]);
				} else if ((planarSubset[i] != str[i])) {
					addBPAux(i, str[i]);
				}
			}
		}

	}

	public ArrayList<ModeleBase> get_listeBases() {
		return _listeBases;
	}

	public int getSize() {
		return _listeBases.size();
	}

	public ArrayList<Integer> findAll() {
		ArrayList<Integer> listAll = new ArrayList<Integer>();
		for (int i = 0; i < get_listeBases().size(); i++) {
			listAll.add(i);
		}
		return listAll;
	}

	public ArrayList<Integer> findBulge(int index) {
		ArrayList<Integer> listUp = new ArrayList<Integer>();
		if (get_listeBases().get(index).getElementStructure() == -1) {
			int i = index;
			boolean over = false;
			while ((i < get_listeBases().size()) && !over) {
				int j = get_listeBases().get(i).getElementStructure();
				if (j == -1) {
					listUp.add(i);
					i++;
				} else {
					over = true;
				}
			}
			i = index - 1;
			over = false;
			while ((i >= 0) && !over) {
				int j = get_listeBases().get(i).getElementStructure();
				if (j == -1) {
					listUp.add(i);
					i--;
				} else {
					over = true;
				}
			}
		}
		return listUp;
	}

	public ArrayList<Integer> findStem(int index) {
		ArrayList<Integer> listUp = new ArrayList<Integer>();
		int i = index;
		do {
			listUp.add(i);
			int j = get_listeBases().get(i).getElementStructure();
			if (j == -1) {
				i = (i + 1) % getSize();
			} else {
				if ((j < i) && (index <= i) && (j <= index)) {
					i = j;
				} else {
					i = (i + 1) % getSize();
				}
			}
		} while (i != index);
		return listUp;
	}

	public int getHelixCountOnLoop(int indice) {
		int cptHelice = 0;
		if (indice < 0 || indice >= get_listeBases().size())
			return cptHelice;
		int i = indice;
		int j = get_listeBases().get(i).getElementStructure();
		// Only way to distinguish "supporting base-pair" from others
		boolean justJumped = false;
		if ((j != -1) && (j < i)) {
			i = j + 1;
			indice = i;
		}
		do {
			j = get_listeBases().get(i).getElementStructure();
			if ((j != -1) && (!justJumped)) {
				i = j;
				justJumped = true;
				cptHelice++;
			} else {
				i = (i + 1) % get_listeBases().size();
				justJumped = false;
			}
		} while (i != indice);
		return cptHelice;
	}

	public ArrayList<Integer> findLoop(int indice) {
		return findLoopForward(indice);
	}

	public ArrayList<Integer> findLoopForward(int indice) {
		ArrayList<Integer> base = new ArrayList<Integer>();
		if (indice < 0 || indice >= get_listeBases().size())
			return base;
		int i = indice;
		int j = get_listeBases().get(i).getElementStructure();
		// Only way to distinguish "supporting base-pair" from others
		boolean justJumped = false;
		if (j != -1) {
			i = Math.min(i, j) + 1;
			indice = i;
		}
		do {
			base.add(i);
			j = get_listeBases().get(i).getElementStructure();
			if ((j != -1) && (!justJumped)) {
				i = j;
				justJumped = true;
			} else {
				i = (i + 1) % get_listeBases().size();
				justJumped = false;
			}
		} while (i != indice);
		return base;
	}

	public ArrayList<Integer> findPair(int indice) {
		ArrayList<Integer> base = new ArrayList<Integer>();
		int j = get_listeBases().get(indice).getElementStructure();
		if (j != -1) {
			base.add(Math.min(indice, j));
			base.add(Math.max(indice, j));
		}

		return base;

	}

	public ArrayList<Integer> findLoopBackward(int indice) {
		ArrayList<Integer> base = new ArrayList<Integer>();
		if (indice < 0 || indice >= get_listeBases().size())
			return base;
		int i = indice;
		int j = get_listeBases().get(i).getElementStructure();
		// Only way to distinguish "supporting base-pair" from others
		boolean justJumped = false;
		if (j != -1) {
			i = Math.min(i, j) - 1;
			indice = i;
		}
		if (i < 0) {
			return base;
		}
		do {
			base.add(i);
			j = get_listeBases().get(i).getElementStructure();
			if ((j != -1) && (!justJumped)) {
				i = j;
				justJumped = true;
			} else {
				i = (i + get_listeBases().size() - 1) % get_listeBases().size();
				justJumped = false;
			}
		} while (i != indice);
		return base;
	}

	public ArrayList<Integer> findHelix(int indice) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		if (get_listeBases().get(indice).getElementStructure() != -1) {
			list.add(indice);
			list.add(get_listeBases().get(indice).getElementStructure());
			int i = 1, prec = get_listeBases().get(indice)
					.getElementStructure();
			while (indice + i < get_listeBases().size()
					&& get_listeBases().get(indice + i).getElementStructure() != -1
					&& get_listeBases().get(indice + i).getElementStructure() == prec - 1) {
				list.add(indice + i);
				list.add(get_listeBases().get(indice + i).getElementStructure());
				prec = get_listeBases().get(indice + i).getElementStructure();
				i++;
			}
			i = -1;
			prec = get_listeBases().get(indice).getElementStructure();
			while (indice + i >= 0
					&& get_listeBases().get(indice + i).getElementStructure() != -1
					&& get_listeBases().get(indice + i).getElementStructure() == prec + 1) {
				list.add(indice + i);
				list.add(get_listeBases().get(indice + i).getElementStructure());
				prec = get_listeBases().get(indice + i).getElementStructure();
				i--;
			}
		}
		return list;
	}

	public ArrayList<Integer> find3Prime(int indice) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		boolean over = false;
		while ((indice >= 0) && !over) {
			over = (get_listeBases().get(indice).getElementStructure() != -1);
			indice--;
		}
		indice++;
		if (over) {
			indice++;
		}
		for (int i = indice; i < get_listeBases().size(); i++) {
			list.add(i);
			if (get_listeBases().get(i).getElementStructure() != -1) {
				return new ArrayList<Integer>();
			}
		}
		return list;
	}

	public ArrayList<Integer> find5Prime(int indice) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i <= indice; i++) {
			list.add(i);
			if (get_listeBases().get(i).getElementStructure() != -1) {
				return new ArrayList<Integer>();
			}
		}
		return list;
	}

	public static Double angle(Point2D.Double p1, Point2D.Double p2,
			Point2D.Double p3) {
		Double alpha = Math.atan2(p1.y - p2.y, p1.x - p2.x);
		Double beta = Math.atan2(p3.y - p2.y, p3.x - p2.x);
		Double angle = (beta - alpha);

		// Correction de l'angle pour le resituer entre 0 et 2PI
		while (angle < 0.0 || angle > 2 * Math.PI) {
			if (angle < 0.0)
				angle += 2 * Math.PI;
			else if (angle > 2 * Math.PI)
				angle -= 2 * Math.PI;
		}
		return angle;
	}

	public ArrayList<Integer> findNonPairedBaseGroup(Integer get_nearestBase) {
		// detection 3', 5', bulge
		ArrayList<Integer> list = new ArrayList<Integer>();
		int indice = get_nearestBase;
		boolean nonpairedUp = true, nonpairedDown = true;
		while (indice < get_listeBases().size() && nonpairedUp) {
			if (get_listeBases().get(indice).getElementStructure() == -1) {
				list.add(indice);
				indice++;
			} else {
				nonpairedUp = false;
			}
		}
		indice = get_nearestBase - 1;
		while (indice >= 0 && nonpairedDown) {
			if (get_listeBases().get(indice).getElementStructure() == -1) {
				list.add(indice);
				indice--;
			} else {
				nonpairedDown = false;
			}
		}
		return list;
	}

	/*
	 * public boolean getDrawn() { return _drawn; }
	 */

	public ArrayList<ModeleBP> getStructureAux() {
		return _structureAux;
	}

	/**
	 * Translates a base number into its corresponding index. Although both
	 * should be unique, base numbers are not necessarily contiguous, and
	 * indices should be preferred for any reasonably complex algorithmic
	 * treatment.
	 * 
	 * @param num
	 *            The base number
	 * @return The first index whose associated Base model has base number
	 *         <code>num</code>, <code>-1</code> of no such base model exists.
	 */

	public int getIndexFromBaseNumber(int num) {
		for (int i = 0; i < this._listeBases.size(); i++) {
			if (_listeBases.get(i).getBaseNumber() == num) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Adds a base pair to this RNA's structure. Tries to add it to the
	 * secondary structure first, eventually adding it to the 'tertiary'
	 * interactions if it clashes with the current secondary structure.
	 * 
	 * @param baseNumber5
	 *            - Base number of the origin of this base pair
	 * @param baseNumber3
	 *            - Base number of the destination of this base pair
	 */

	public void addBPToStructureUsingNumbers(int baseNumber5, int baseNumber3) {
		int i = getIndexFromBaseNumber(baseNumber5);
		int j = getIndexFromBaseNumber(baseNumber3);
		addBP(i, j);
	}

	/**
	 * Adds a base pair to this RNA's structure. Tries to add it to the
	 * secondary structure first, possibly adding it to the 'tertiary'
	 * interactions if it clashes with the current secondary structure.
	 * 
	 * @param number5
	 *            - Base number of the origin of this base pair
	 * @param number3
	 *            - Base number of the destination of this base pair
	 */

	public void addBPToStructureUsingNumbers(int number5, int number3,
			ModeleBP msbp) {
		addBP(getIndexFromBaseNumber(number5), getIndexFromBaseNumber(number3),
				msbp);
	}

	public void addBP(int index5, int index3) {
		int i = index5;
		int j = index3;
		ModeleBase part5 = _listeBases.get(i);
		ModeleBase part3 = _listeBases.get(j);
		ModeleBP msbp = new ModeleBP(part5, part3);
		addBP(i, j, msbp);
	}

	public void addBP(int index5, int index3, ModeleBP msbp) {
		int i = index5;
		int j = index3;

		if (j < i) {
			int k = j;
			j = i;
			i = k;
		}
		if (i != -1) {
			for (int k = i; k <= j; k++) {
				ModeleBase tmp = _listeBases.get(k);
				int l = tmp.getElementStructure();
				if (l != -1) {
					if ((l <= i) || (l >= j)) {
						addBPAux(i, j, msbp);
						return;
					}
				}
			}
			addBPnow(i, j, msbp);
		}
	}

	public void removeBP(ModeleBP ms) {
		if (_structureAux.contains(ms)) {
			_structureAux.remove(ms);
		} else {
			ModeleBase m5 = ms.getPartner5();
			ModeleBase m3 = ms.getPartner3();
			int i = m5.getIndex();
			int j = m3.getIndex();
			if ((m5.getElementStructure() == m3.getIndex())
					&& (m3.getElementStructure() == m5.getIndex())) {
				m5.removeElementStructure();
				m3.removeElementStructure();
			}
		}
	}

	/**
	 * Register base-pair, no question asked. More precisely, this function will
	 * not try to determine if the base-pairs crosses any other.
	 * 
	 * @param i
	 * @param j
	 * @param msbp
	 */
	private void addBPNow(int i, int j) {
		if (j < i) {
			int k = j;
			j = i;
			i = k;
		}

		ModeleBase part5 = _listeBases.get(i);
		ModeleBase part3 = _listeBases.get(j);
		ModeleBP msbp = new ModeleBP(part5, part3);
		addBPnow(i, j, msbp);
	}

	/**
	 * Register base-pair, no question asked. More precisely, this function will
	 * not try to determine if the base-pairs crosses any other.
	 * 
	 * @param i
	 * @param j
	 * @param msbp
	 */
	private void addBPnow(int i, int j, ModeleBP msbp) {
		if (j < i) {
			int k = j;
			j = i;
			i = k;
		}
		ModeleBase part5 = _listeBases.get(i);
		ModeleBase part3 = _listeBases.get(j);
		msbp.setPartner5(part5);
		msbp.setPartner3(part3);
		part5.setElementStructure(j, msbp);
		part3.setElementStructure(i, msbp);
	}

	public void addBPAux(int i, int j) {
		ModeleBase part5 = _listeBases.get(i);
		ModeleBase part3 = _listeBases.get(j);
		ModeleBP msbp = new ModeleBP(part5, part3);
		addBPAux(i, j, msbp);
	}

	public void addBPAux(int i, int j, ModeleBP msbp) {
		if (j < i) {
			int k = j;
			j = i;
			i = k;
		}
		ModeleBase part5 = _listeBases.get(i);
		ModeleBase part3 = _listeBases.get(j);
		msbp.setPartner5(part5);
		msbp.setPartner3(part3);
		_structureAux.add(msbp);
	}

	public ArrayList<ModeleBP> getBPsAt(int i) {
		ArrayList<ModeleBP> result = new ArrayList<ModeleBP>();
		if (_listeBases.get(i).getElementStructure() != -1) {
			result.add(_listeBases.get(i).getStyleBP());
		}
		for (int k = 0; k < _structureAux.size(); k++) {
			ModeleBP bp = _structureAux.get(k);
			if ((bp.getPartner5().getIndex() == i)
					|| (bp.getPartner3().getIndex() == i)) {
				result.add(bp);
			}
		}
		return result;

	}

	public ModeleBP getBPStyle(int i, int j) {
		ModeleBP result = null;
		if (i > j) {
			int k = j;
			j = i;
			i = k;
		}
		if (_listeBases.get(i).getElementStructure() == j) {
			result = _listeBases.get(i).getStyleBP();
		}
		for (int k = 0; k < _structureAux.size(); k++) {
			ModeleBP bp = _structureAux.get(k);
			if ((bp.getPartner5().getIndex() == i)
					&& (bp.getPartner3().getIndex() == j)) {
				result = bp;
			}
		}
		return result;
	}

	public ArrayList<ModeleBP> getSecStrBPs() {
		ArrayList<ModeleBP> result = new ArrayList<ModeleBP>();
		for (int i = 0; i < this.getSize(); i++) {
			ModeleBase mb = _listeBases.get(i);
			int k = mb.getElementStructure();
			if ((k != -1) && (k > i)) {
				result.add(mb.getStyleBP());
			}
		}
		return result;
	}

	public ArrayList<ModeleBP> getAuxBPs() {
		ArrayList<ModeleBP> result = new ArrayList<ModeleBP>();
		for (ModeleBP bp : _structureAux) {
			result.add(bp);
		}
		return result;
	}

	public ArrayList<ModeleBP> getAllBPs() {
		ArrayList<ModeleBP> result = new ArrayList<ModeleBP>();
		result.addAll(getSecStrBPs());
		result.addAll(getAuxBPs());
		return result;
	}

	public ArrayList<ModeleBP> getAuxBPs(int i) {
		ArrayList<ModeleBP> result = new ArrayList<ModeleBP>();
		for (ModeleBP bp : _structureAux) {
			if ((bp.getPartner5().getIndex() == i)
					|| (bp.getPartner3().getIndex() == i)) {
				result.add(bp);
			}
		}
		return result;
	}

	public void setBaseInnerColor(Color c) {
		for (int i = 0; i < _listeBases.size(); i++) {
			ModeleBase mb = _listeBases.get(i);
			mb.getStyleBase().setBaseInnerColor(c);
		}
	}

	public void setBaseNumbersColor(Color c) {
		for (int i = 0; i < _listeBases.size(); i++) {
			ModeleBase mb = _listeBases.get(i);
			mb.getStyleBase().setBaseNumberColor(c);
		}
	}

	public void setBaseNameColor(Color c) {
		for (int i = 0; i < _listeBases.size(); i++) {
			ModeleBase mb = _listeBases.get(i);
			mb.getStyleBase().setBaseNameColor(c);
		}
	}

	public void setBaseOutlineColor(Color c) {
		for (int i = 0; i < _listeBases.size(); i++) {
			ModeleBase mb = _listeBases.get(i);
			mb.getStyleBase().setBaseOutlineColor(c);
		}
	}

	public String getName() {
		return _name;
	}

	public void setName(String n) {
		_name = n;
	}

	public ArrayList<TextAnnotation> getAnnotations() {
		return _listeAnnotations;
	}

	public boolean removeAnnotation(TextAnnotation t) {
		return _listeAnnotations.remove(t);
	}

	public void addAnnotation(TextAnnotation t) {
		_listeAnnotations.add(t);
	}

	public void removeAnnotation(String filter) {
		ArrayList<TextAnnotation> condamne = new ArrayList<TextAnnotation>();
		for (TextAnnotation t : _listeAnnotations) {
			if (t.getTexte().contains(filter)) {
				condamne.add(t);
			}
		}
		for (TextAnnotation t : condamne) {
			_listeAnnotations.remove(t);
		}
	}

	public void clearAnnotations() {
		_listeAnnotations.clear();
	}

	private boolean _strandEndsAnnotated = false;

	public void autoAnnotateStrandEnds() {
		if (!_strandEndsAnnotated) {
			int tailleListBases = _listeBases.size();
			boolean endAnnotate = false;
			addAnnotation(new TextAnnotation("5'", _listeBases.get(0)));
			for (int i = 0; i < _listeBases.size() - 1; i++) {
				int realposA = _listeBases.get(i).getBaseNumber();
				int realposB = _listeBases.get(i + 1).getBaseNumber();
				if (realposB - realposA != 1) {
					addAnnotation(new TextAnnotation("3'", _listeBases.get(i)));
					addAnnotation(new TextAnnotation("5'",
							_listeBases.get(i + 1)));
					if (i + 1 == _listeBases.size() - 1) {
						endAnnotate = true;
					}
				}
			}
			if (!endAnnotate) {
				addAnnotation(new TextAnnotation("3'",
						_listeBases.get(tailleListBases - 1)));
			}
			_strandEndsAnnotated = true;
		} else {
			removeAnnotation("3'");
			removeAnnotation("5'");
			_strandEndsAnnotated = false;
		}
	}

	public void autoAnnotateHelices() {
		Stack<Integer> p = new Stack<Integer>();
		p.push(0);
		int nbH = 1;
		while (!p.empty()) {
			int i = p.pop();
			if (i < _listeBases.size()) {
				ModeleBase mb = _listeBases.get(i);
				int j = mb.getElementStructure();
				if (j == -1) {
					p.push(i + 1);
				} else {
					if (j > i) {
						ModeleBase mbp = _listeBases.get(j);
						p.push(j + 1);
						ArrayList<ModeleBase> h = new ArrayList<ModeleBase>();
						int k = 1;
						while (mb.getElementStructure() == mbp.getIndex()) {
							h.add(mb);
							h.add(mbp);
							mb = _listeBases.get(i + k);
							mbp = _listeBases.get(j - k);

							k++;
						}
						try {
							addAnnotation(new TextAnnotation("H" + nbH++, h,
									TextAnnotation.AnchorType.HELIX));
						} catch (Exception e) {
							e.printStackTrace();
						}
						p.push(i + k);
					}
				}
			}
		}
	}

	public void autoAnnotateTerminalLoops() {
		Stack<Integer> p = new Stack<Integer>();
		p.push(0);
		int nbT = 1;
		while (!p.empty()) {
			int i = p.pop();
			if (i < _listeBases.size()) {
				ModeleBase mb = _listeBases.get(i);
				int j = mb.getElementStructure();
				if (j == -1) {
					int k = 1;
					ArrayList<ModeleBase> t = new ArrayList<ModeleBase>();
					while ((i + k < getSize())
							&& (mb.getElementStructure() == -1)) {
						t.add(mb);
						mb = _listeBases.get(i + k);
						k++;
					}
					if (mb.getElementStructure() != -1) {
						if (mb.getElementStructure() == i - 1) {
							try {
								t.add(_listeBases.get(i - 1));
								t.add(_listeBases.get(i + k - 1));
								addAnnotation(new TextAnnotation("T" + nbT++,
										t, TextAnnotation.AnchorType.LOOP));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						p.push(i + k - 1);
					}

				} else {
					if (j > i) {
						p.push(j + 1);
						p.push(i + 1);
					}
				}
			}
		}
	}

	public void autoAnnotateInteriorLoops() {
		Stack<Integer> p = new Stack<Integer>();
		p.push(0);
		int nbT = 1;
		while (!p.empty()) {
			int i = p.pop();
			if (i < _listeBases.size()) {
				ModeleBase mb = _listeBases.get(i);
				int j = mb.getElementStructure();
				if (j == -1) {
					int k = i + 1;
					ArrayList<ModeleBase> t = new ArrayList<ModeleBase>();
					boolean terminal = true;
					while ((k < getSize())
							&& ((mb.getElementStructure() >= i) || (mb
									.getElementStructure() == -1))) {
						t.add(mb);
						mb = _listeBases.get(k);
						if ((mb.getElementStructure() == -1)
								|| (mb.getElementStructure() < k))
							k++;
						else {
							p.push(k);
							terminal = false;
							k = mb.getElementStructure();
						}
					}
					if (mb.getElementStructure() != -1) {
						if ((mb.getElementStructure() == i - 1) && !terminal) {
							try {
								t.add(_listeBases.get(i - 1));
								t.add(_listeBases.get(k - 1));
								addAnnotation(new TextAnnotation("I" + nbT++,
										t, TextAnnotation.AnchorType.LOOP));
							} catch (Exception e) {
								e.printStackTrace();
							}
							p.push(k - 1);
						}
					}
				} else {
					if (j > i) {
						p.push(i + 1);
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public TextAnnotation getAnnotation(TextAnnotation.AnchorType type,
			ModeleBase base) {
		TextAnnotation result = null;
		for (TextAnnotation t : _listeAnnotations) {
			if (t.getType() == type) {
				switch (type) {
				case BASE:
					if (base == (ModeleBase) t.getAncrage())
						return t;
					break;
				case HELIX:
				case LOOP: {
					ArrayList<ModeleBase> mbl = (ArrayList<ModeleBase>) t
							.getAncrage();
					if (mbl.contains(base))
						return t;
				}
					break;
				}
			}
		}
		return result;
	}

	public void addChemProbAnnotation(ChemProbAnnotation cpa) {
		//System.err.println(cpa.isOut());
		_chemProbAnnotations.add(cpa);
	}

	public ArrayList<ChemProbAnnotation> getChemProbAnnotations() {
		return _chemProbAnnotations;
	}

	public void setColorMapValues(Double[] values, ModeleColorMap cm) {
		setColorMapValues(values, cm, false);
	}

	public void adaptColorMapToValues(ModeleColorMap cm) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < Math.min(_listeBases.size(), _listeBases.size()); i++) {
			ModeleBase mb = _listeBases.get(i);
			max = Math.max(max, mb.getValue());
			min = Math.min(min, mb.getValue());
		}
		cm.rescale(min, max);
	}
	
	
	private ArrayList<Double> loadDotPlot(StreamTokenizer st)
	{
		ArrayList<Double> result = new ArrayList<Double>();
		try {
			boolean inSeq = false;
			String sequence = "";
			ArrayList<Double> accumulator = new ArrayList<Double>();
			int type = st.nextToken();
			Hashtable<Couple<Integer,Integer>,Double> BP = new Hashtable<Couple<Integer,Integer>,Double>();
			while (type != StreamTokenizer.TT_EOF) {
				switch (type) {
					case (StreamTokenizer.TT_NUMBER):
						accumulator.add(st.nval);
						break;
					case (StreamTokenizer.TT_EOL):
						break;
					case (StreamTokenizer.TT_WORD):
						if (st.sval.equals("/sequence"))
						{
							inSeq = true;
						}
						else if (st.sval.equals("ubox"))
						{
							int i = accumulator.get(accumulator.size()-3).intValue()-1;
							int j = accumulator.get(accumulator.size()-2).intValue()-1;
							double val = accumulator.get(accumulator.size()-1);
							//System.err.println((char) type);			
							BP.put(new Couple<Integer, Integer>(Math.min(i, j), Math.max(i, j)),val*val);
							accumulator.clear();
						}
						else if (inSeq)
						{
							sequence += st.sval;
						}
						break;
					case ')':
						inSeq = false;
					break;
				}
				type = st.nextToken();
			}
			for (int i = 0; i < getSize(); i++) {
				int j = getBaseAt(i).getElementStructure();
				if (j != -1) {
					Couple<Integer, Integer> coor = new Couple<Integer, Integer>(
							Math.min(i, j), Math.max(i, j));
					if (BP.containsKey(coor)) {
						result.add(BP.get(coor));
					} else {
						result.add(0.);
					}
				} else {
					double acc = 1.0;
					for (int k = 0; k < getSize(); k++) {
						Couple<Integer, Integer> coor = new Couple<Integer, Integer>(
								Math.min(i, k), Math.max(i, k));
						if (BP.containsKey(coor)) {
							acc -= BP.get(coor);
						}
					}
					result.add(acc);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	public void readValues(Reader r, ModeleColorMap cm) {
		try {
			StreamTokenizer st = new StreamTokenizer(r);
			st.eolIsSignificant(true);
			st.wordChars('/', '/');
			st.parseNumbers();
			ArrayList<Double> vals = new ArrayList<Double>();
			ArrayList<Double> curVals = new ArrayList<Double>();
			int type = st.nextToken();
			boolean isDotPlot = false;
			if (type=='%')
			{
				vals = loadDotPlot(st);
				isDotPlot = true;
			}
			else
			{	
				while (type != StreamTokenizer.TT_EOF) {
					switch (type) {
					case (StreamTokenizer.TT_NUMBER):
						curVals.add(st.nval);
						break;
					case (StreamTokenizer.TT_EOL):
						if (curVals.size() > 0) {
							vals.add(curVals.get(curVals.size()-1));
							curVals = new ArrayList<Double>();
						}
						break;
					}
					type = st.nextToken();
				}
				if (curVals.size() > 0)
					vals.add(curVals.get(curVals.size()-1));
			}

			Double[] v = new Double[vals.size()];
			for (int i = 0; i < Math.min(vals.size(), getSize()); i++) {
				v[i] = vals.get(i);
			}
			setColorMapValues(v, cm, true);
			if (isDotPlot)
			{
				cm.setMinValue(0.0);
				cm.setMaxValue(1.0);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setColorMapValues(Double[] values, ModeleColorMap cm,
			boolean rescaleColorMap) {
		if (values.length > 0) {
			for (int i = 0; i < Math.min(values.length, _listeBases.size()); i++) {
				ModeleBase mb = _listeBases.get(i);
				mb.setValue(values[i]);
			}
			if (rescaleColorMap) {
				adaptColorMapToValues(cm);
			}
		}
	}

	public Double[] getColorMapValues() {
		Double[] values = new Double[_listeBases.size()];
		for (int i = 0; i < _listeBases.size(); i++) {
			values[i] = _listeBases.get(i).getValue();
		}
		return values;
	}

	public void rescaleColorMap(ModeleColorMap cm) {
		Double max = Double.MIN_VALUE;
		Double min = Double.MAX_VALUE;
		for (int i = 0; i < _listeBases.size(); i++) {
			Double value = _listeBases.get(i).getValue();
			max = Math.max(max, value);
			min = Math.min(min, value);
		}
		cm.rescale(min, max);
	}

	public void addBase(ModeleBase mb) {
		_listeBases.add(mb);
	}

	public void setSequence(String s) {
		setSequence(RNA.explodeSequence(s));
	}

	public void setSequence(List<String> s) {
		int i = 0;
		int j = 0;
		while ((i < s.size()) && (j < _listeBases.size())) {
			ModeleBase mb = _listeBases.get(j);
			if (mb instanceof ModeleBaseNucleotide) {
				((ModeleBaseNucleotide) mb).setBase(s.get(i));
				i++;
				j++;
			} else if (mb instanceof ModeleBasesComparison) {
				((ModeleBasesComparison) mb)
						.setBase1(((s.get(i).length() > 0) ? s.get(i).charAt(0)
								: ' '));
				((ModeleBasesComparison) mb)
						.setBase2(((s.get(i + 1).length() > 0) ? s.get(i + 1)
								.charAt(0) : ' '));
				i += 2;
				j++;
			} else
				j++;
		}
		for (i = _listeBases.size(); i < s.size(); i++) {
			_listeBases.add(new ModeleBaseNucleotide(s.get(i), i));
		}
	}

	public void eraseSequence() {
		int j = 0;
		while ((j < _listeBases.size())) {
			ModeleBase mb = _listeBases.get(j);
			if (mb instanceof ModeleBaseNucleotide) {
				((ModeleBaseNucleotide) mb).setBase("");
				j++;
			} else if (mb instanceof ModeleBasesComparison) {
				((ModeleBasesComparison) mb).setBase1(' ');
				((ModeleBasesComparison) mb).setBase2(' ');
				j++;
			} else
				j++;
		}
	}

	public RNA clone() {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream oout = new ObjectOutputStream(out);
			oout.writeObject(this);

			ObjectInputStream in = new ObjectInputStream(
					new ByteArrayInputStream(out.toByteArray()));
			return (RNA) in.readObject();
		} catch (Exception e) {
			throw new RuntimeException("cannot clone class ["
					+ this.getClass().getName() + "] via serialization: "
					+ e.toString());
		}
	}

	/**
	 * Returns the base at index <code>index</code>. Indices are contiguous in
	 * the sequence over an interval <code>[0,this.getSize()-1]</code>, where
	 * <code>n</code> is the length of the sequence.
	 * 
	 * @param index
	 *            The index, <code>0 &le; index < this.getSize()</code>, of the
	 *            base model
	 * @return The base model of index <code>index</code>
	 */
	public ModeleBase getBaseAt(int index) {
		return this._listeBases.get(index);
	}

	/**
	 * Returns the set of bases of indices in <code>indices</code>. Indices are
	 * contiguous in the sequence, and belong to an interval
	 * <code>[0,n-1]</code>, where <code>n</code> is the length of the sequence.
	 * 
	 * @param indices
	 *            A Collection of indices <code>i</code>,
	 *            <code>0 &le; index < this.getSize()</code>, where some base
	 *            models are found.
	 * @return A list of base model of indices in <code>indices</code>
	 */
	public ArrayList<ModeleBase> getBasesAt(
			Collection<? extends Integer> indices) {
		ArrayList<ModeleBase> mbs = new ArrayList<ModeleBase>();
		for (int i : indices) {
			mbs.add(getBaseAt(i));
		}
		return mbs;
	}

	public ArrayList<ModeleBase> getBasesBetween(int from, int to) {
		ArrayList<ModeleBase> mbs = new ArrayList<ModeleBase>();
		int bck = Math.min(from, to);
		to = Math.max(from, to);
		from = bck;
		for (int i = from; i <= to; i++) {
			mbs.add(getBaseAt(i));
		}
		return mbs;
	}

	public void addHighlightRegion(HighlightRegionAnnotation n) {
		_listeRegionHighlights.add(n);
	}

	public void removeHighlightRegion(HighlightRegionAnnotation n) {
		_listeRegionHighlights.remove(n);
	}

	public void removeChemProbAnnotation(ChemProbAnnotation a) {
		_chemProbAnnotations.remove(a);
	}

	public void clearChemProbAnnotations() {
		_chemProbAnnotations.clear();
	}

	public void addHighlightRegion(int from, int to, Color fill, Color outline,
			double radius) {
		_listeRegionHighlights.add(new HighlightRegionAnnotation(
				getBasesBetween(from, to), fill, outline, radius));
	}

	public void addHighlightRegion(int from, int to) {
		_listeRegionHighlights.add(new HighlightRegionAnnotation(
				getBasesBetween(from, to)));
	}

	public ArrayList<HighlightRegionAnnotation> getHighlightRegion() {
		return _listeRegionHighlights;
	}

	/**
	 * Rotates the RNA coordinates by a certain angle
	 * 
	 * @param angleDegres
	 *            Rotation angle, in degrees
	 */
	public void globalRotation(Double angleDegres) {
		if (_listeBases.size() > 0) {

			// angle en radian
			Double angle = angleDegres * Math.PI / 180;

			// initialisation du minimum et dumaximum
			Double maxX = _listeBases.get(0).getCoords().x;
			Double maxY = _listeBases.get(0).getCoords().y;
			Double minX = _listeBases.get(0).getCoords().x;
			Double minY = _listeBases.get(0).getCoords().y;
			// mise a jour du minimum et du maximum
			for (int i = 0; i < _listeBases.size(); i++) {
				if (_listeBases.get(i).getCoords().getX() < minX)
					minX = _listeBases.get(i).getCoords().getX();
				if (_listeBases.get(i).getCoords().getY() < minY)
					minY = _listeBases.get(i).getCoords().getY();
				if (_listeBases.get(i).getCoords().getX() > maxX)
					maxX = _listeBases.get(i).getCoords().getX();
				if (_listeBases.get(i).getCoords().getX() > maxY)
					maxY = _listeBases.get(i).getCoords().getY();
			}
			// creation du point central
			Point2D.Double centre = new Point2D.Double((maxX - minX) / 2,
					(maxY - minY) / 2);
			Double x, y;
			for (int i = 0; i < _listeBases.size(); i++) {
				// application de la rotation au centre de chaque base
				// x' = cos(theta)*(x-xc) - sin(theta)*(y-yc) + xc
				x = Math.cos(angle)
						* (_listeBases.get(i).getCenter().getX() - centre.x)
						- Math.sin(angle)
						* (_listeBases.get(i).getCenter().getY() - centre.y)
						+ centre.x;
				// y' = sin(theta)*(x-xc) + cos(theta)*(y-yc) + yc
				y = Math.sin(angle)
						* (_listeBases.get(i).getCenter().getX() - centre.x)
						+ Math.cos(angle)
						* (_listeBases.get(i).getCenter().getY() - centre.y)
						+ centre.y;
				_listeBases.get(i).setCenter(new Point2D.Double(x, y));

				// application de la rotation au coordonnees de chaque
				// base
				// x' = cos(theta)*(x-xc) - sin(theta)*(y-yc) + xc
				x = Math.cos(angle)
						* (_listeBases.get(i).getCoords().getX() - centre.x)
						- Math.sin(angle)
						* (_listeBases.get(i).getCoords().getY() - centre.y)
						+ centre.x;
				// y' = sin(theta)*(x-xc) + cos(theta)*(y-yc) + yc
				y = Math.sin(angle)
						* (_listeBases.get(i).getCoords().getX() - centre.x)
						+ Math.cos(angle)
						* (_listeBases.get(i).getCoords().getY() - centre.y)
						+ centre.y;
				_listeBases.get(i).setCoords(new Point2D.Double(x, y));
			}
		}
	}
	
	private static double MIN_DISTANCE = 10.;
	
	
	/**
	 * Flip an helix around its supporting base
	 */
	public void flipHelix(Point h) {
		if (h.x!=-1 && h.y!=-1 && h.x!=h.y)
		{
			int hBeg=h.x;
			int hEnd=h.y;
			Point2D.Double A = getCoords(hBeg);
			Point2D.Double B = getCoords(hEnd);
			Point2D.Double AB = new Point2D.Double(B.x - A.x, B.y - A.y);
			double normAB = Math.sqrt(AB.x * AB.x + AB.y * AB.y);
			// Creating a coordinate system centered on A and having
			// unit x-vector Ox.
			Point2D.Double O = A;
			Point2D.Double Ox = new Point2D.Double(AB.x / normAB, AB.y / normAB);
			Hashtable<Integer,Point2D.Double> old = new Hashtable<Integer,Point2D.Double>(); 
			for (int i = hBeg + 1; i < hEnd; i++) {
				Point2D.Double P = getCoords(i);
				Point2D.Double nP = project(O, Ox, P);
				old.put(i, nP);
				setCoord(i, nP);
				Point2D.Double Center = getCenter(i);
				setCenter(i, project(O, Ox, Center));
			}
		}
	}

	public static Point2D.Double project(Point2D.Double O, Point2D.Double Ox,
			Point2D.Double C) {
		Point2D.Double OC = new Point2D.Double(C.x - O.x, C.y - O.y);
		// Projection of OC on OI => OX
		double normOX = (Ox.x * OC.x + Ox.y * OC.y);
		Point2D.Double OX = new Point2D.Double((normOX * Ox.x), (normOX * Ox.y));
		// Portion of OC orthogonal to Ox => XC
		Point2D.Double XC = new Point2D.Double(OC.x - OX.x, OC.y - OX.y);
		// Reflexive image of C with respect to Ox => CP
		Point2D.Double OCP = new Point2D.Double(OX.x - XC.x, OX.y - XC.y);
		Point2D.Double CP = new Point2D.Double(O.x + OCP.x, O.y + OCP.y);
		return CP;
	}


	public boolean testDirectionality(int i, int j, int k) {

		// Which direction are we heading toward?
		Point2D.Double pi = getCoords(i);
		Point2D.Double pj = getCoords(j);
		Point2D.Double pk = getCoords(k);
		return testDirectionality(pi, pj, pk);
	}

	public static boolean testDirectionality(Point2D.Double pi,
			Point2D.Double pj, Point2D.Double pk) {

		// Which direction are we heading toward?
		double test = (pj.x - pi.x) * (pk.y - pj.y) - (pj.y - pi.y)
				* (pk.x - pj.x);
		return test < 0.0;
	}

	public double getOrientation() {
		double maxDist = Double.MIN_VALUE;
		double angle = 0;
		for (int i = 0; i < _listeBases.size(); i++) {
			ModeleBase b1 = _listeBases.get(i);
			for (int j = i + 1; j < _listeBases.size(); j++) {
				ModeleBase b2 = _listeBases.get(j);
				Point2D.Double p1 = b1._coords.toPoint2D();
				Point2D.Double p2 = b2._coords.toPoint2D();
				double dist = p1.distance(p2);
				if (dist > maxDist) {
					maxDist = dist;
					angle = computeAngle(p1, p2);
				}
			}
		}
		return angle;
	}

	public boolean hasVirtualLoops() {
		boolean consecutiveBPs = false;
		for (int i = 0; i < _listeBases.size(); i++) {
			int j = _listeBases.get(i).getElementStructure();
			if (j == i + 1) {
				consecutiveBPs = true;
			}

		}
		return ((_drawMode != DRAW_MODE_LINEAR)
				&& (_drawMode != DRAW_MODE_CIRCULAR) && (consecutiveBPs));
	}

	public String getHTMLDescription() {
		String result = "<table>";
		result += "<tr><td><b>Name:</b></td><td>" + this._name + "</td></tr>";
		result += "<tr><td><b>Length:</b></td><td>" + this.getSize()
				+ " nts</td></tr>";
		result += "<tr><td><b>Base-pairs:</b></td><td>"
				+ this.getAllBPs().size() + " </td></tr>";
		return result + "</table>";
	}

	public String getID() {
		return _id;
	}

	public void setID(String id) {
		_id = id;
	}

	public static ArrayList<Integer> getGapPositions(String gapString) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < gapString.length(); i++) {
			char c = gapString.charAt(i);
			if (c == '.' || c == ':') {
				result.add(i);
			}
		}
		return result;
	}

	public RNA restrictTo(String gapString) {
		return restrictTo(getGapPositions(gapString));
	}

	public RNA restrictTo(ArrayList<Integer> positions) {
		RNA result = new RNA();
		String oldSeq = this.getSeq();
		String newSeq = "";
		HashSet<Integer> removedPos = new HashSet<Integer>(positions);
		int[] matching = new int[oldSeq.length()];
		int j = 0;
		for (int i = 0; i < oldSeq.length(); i++) {
			matching[i] = j;
			if (!removedPos.contains(i)) {
				newSeq += oldSeq.charAt(i);
				j++;
			}
		}
		result.setRNA(newSeq);
		for (ModeleBP m : getAllBPs()) {
			if (removedPos.contains(m.getIndex5())
					|| removedPos.contains(m.getIndex3())) {
				int i5 = matching[m.getIndex5()];
				int i3 = matching[m.getIndex3()];
				ModeleBP msbp = new ModeleBP(result.getBaseAt(i5),
						result.getBaseAt(i3), m.getEdgePartner5(),
						m.getEdgePartner3(), m.getStericity());
				result.addBP(i5, i3, msbp);
			}
		}
		return result;
	}

	public void rescale(double d) {
		for (ModeleBase mb : _listeBases) {
			mb._coords.x *= d;
			mb._coords.y *= d;
			mb._center.x *= d;
			mb._center.y *= d;
		}
	}

	/**
	 * Necessary for DrawRNATemplate (which is why the method is
	 * package-visible).
	 */
	ArrayList<ModeleBase> getListeBases() {
		return _listeBases;
	}
}
