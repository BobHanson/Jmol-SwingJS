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

/*
 * VARNA is a Java library for quick automated drawings RNA secondary structure
 * Copyright (C) 2007 Yann Ponty
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.exceptions.ExceptionDrawingAlgorithm;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.exceptions.ExceptionModeleStyleBaseSyntaxError;
import fr.orsay.lri.varna.exceptions.ExceptionNAViewAlgorithm;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import fr.orsay.lri.varna.exceptions.ExceptionParameterError;
import fr.orsay.lri.varna.factories.RNAFactory;
import fr.orsay.lri.varna.interfaces.InterfaceParameterLoader;
import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation;
import fr.orsay.lri.varna.models.annotations.HighlightRegionAnnotation;
import fr.orsay.lri.varna.models.annotations.TextAnnotation;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModeleColorMap;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.ModelBaseStyle;
import fr.orsay.lri.varna.models.rna.RNA;

/**
 * An RNA 2d Panel demo applet
 * 
 * @author Yann Ponty
 * 
 */

public class VARNAConfigLoader {

	private static final int MAXSTYLE = 50;

	// Applet Options

	public static String algoOpt = "algorithm";
	public static String annotationsOpt = "annotations";
	public static String applyBasesStyleOpt = "applyBasesStyle";
	public static String auxBPsOpt = "auxBPs";
	public static String autoHelicesOpt = "autoHelices";
	public static String autoInteriorLoopsOpt = "autoInteriorLoops";
	public static String autoTerminalLoopsOpt = "autoTerminalLoops";

	public static String backboneColorOpt = "backbone";
	public static String backgroundColorOpt = "background";
	public static String baseInnerColorOpt = "baseInner";
	public static String baseNameColorOpt = "baseName";
	public static String baseNumbersColorOpt = "baseNum";
	public static String baseOutlineColorOpt = "baseOutline";
	public static String basesStyleOpt = "basesStyle";
	public static String borderOpt = "border";
	public static String bondColorOpt = "bp";
	public static String bpIncrementOpt = "bpIncrement";
	public static String bpStyleOpt = "bpStyle";

	public static String colorMapOpt = "colorMap";
	public static String colorMapCaptionOpt = "colorMapCaption";
	public static String colorMapDefOpt = "colorMapStyle";
	public static String colorMapMinOpt = "colorMapMin";
	public static String colorMapMaxOpt = "colorMapMax";
	public static String comparisonModeOpt = "comparisonMode";
	public static String chemProbOpt = "chemProb";
	public static String customBasesOpt = "customBases";
	public static String customBPsOpt = "customBPs";

	public static String drawNCOpt = "drawNC";
	public static String drawBasesOpt = "drawBases";
	public static String drawTertiaryOpt = "drawTertiary";
	public static String drawColorMapOpt = "drawColorMap";
	public static String drawBackboneOpt = "drawBackbone";

	public static String errorOpt = "error";

	public static String fillBasesOpt = "fillBases";
	public static String firstSequenceForComparisonOpt = "firstSequence";
	public static String firstStructureForComparisonOpt = "firstStructure";
	public static String flatExteriorLoopOpt = "flat";
	public static String flipOpt = "flip";

	public static String gapsBaseColorOpt = "gapsColor";

	public static String highlightRegionOpt = "highlightRegion";

	public static String nonStandardColorOpt = "nsBasesColor";
	public static String numColumnsOpt = "rows";
	public static String numRowsOpt = "columns";

	public static String orientationOpt = "orientation";

	public static String modifiableOpt = "modifiable";

	public static String periodNumOpt = "periodNum";

	public static String rotationOpt = "rotation";

	public static String secondSequenceForComparisonOpt = "secondSequence";
	public static String secondStructureForComparisonOpt = "secondStructure";
	public static String sequenceOpt = "sequenceDBN";
	public static String spaceBetweenBasesOpt = "spaceBetweenBases";
	public static String structureOpt = "structureDBN";

	public static String titleOpt = "title";
	public static String titleColorOpt = "titleColor";
	public static String titleSizeOpt = "titleSize";

	public static String URLOpt = "url";

	public static String warningOpt = "warning";

	public static String zoomOpt = "zoom";
	public static String zoomAmountOpt = "zoomAmount";

	// Applet assignable parameters
	private String _algo;
	public String _annotations;
	public String _chemProbs;
	private double _rotation;

	private String _sseq;
	private String _sstruct;

	private int _numRows;
	private int _numColumns;

	private String _title;
	private int _titleSize;
	private Color _titleColor;

	private String _auxBPs;
	private String _highlightRegion;

	private boolean _autoHelices;
	private boolean _autoInteriorLoops;
	private boolean _autoTerminalLoops;

	private boolean _drawBackbone;
	private Color _backboneColor;
	private Color _bondColor;
	private VARNAConfig.BP_STYLE _bpStyle;
	private Color _baseOutlineColor;
	private Color _baseInnerColor;
	private Color _baseNumColor;
	private Color _baseNameColor;
	private Color _gapsColor;
	private Color _nonStandardColor;

	private boolean _flatExteriorLoop;
	private String _flip;

	private String _customBases;
	private String _customBPs;

	private String _colorMapStyle;
	private String _colorMapCaption;
	private String _colorMapValues;
	private double _colorMapMin = Double.MIN_VALUE;
	private double _colorMapMax = Double.MAX_VALUE;

	private double _spaceBetweenBases = Double.MIN_VALUE;

	private boolean _drawNC;
	private boolean _drawBases;
	private boolean _drawTertiary;
	private boolean _drawColorMap;

	private boolean _fillBases;

	private int _periodResNum;
	private Dimension _border;

	private Color _backgroundColor;

	private String _orientation;

	private boolean _warning, _error;

	private boolean _modifiable;

	private double _zoom, _zoomAmount;

	private ArrayList<ModelBaseStyle> _basesStyleList;

	private boolean _comparisonMode;

	private String _firstSequence;
	private String _secondSequence;
	private String _firstStructure;
	private String _secondStructure;

	private VARNAPanel _mainSurface;

	private boolean _useNonStandardColor;
	private boolean _useGapsColor;
	private double _bpIncrement;

	private boolean _useInnerBaseColor;
	private boolean _useBaseNameColor;
	private boolean _useBaseNumbersColor;
	private boolean _useBaseOutlineColor;

	private String _URL;

	protected ArrayList<VARNAPanel> _VARNAPanelList = new ArrayList<VARNAPanel>();

	InterfaceParameterLoader _optionProducer;

	public VARNAConfigLoader(InterfaceParameterLoader il) {
		_optionProducer = il;
	}

	public ArrayList<VARNAPanel> createVARNAPanels()
			throws ExceptionParameterError,
			ExceptionModeleStyleBaseSyntaxError, ExceptionNonEqualLength,
			IOException, ExceptionFileFormatOrSyntax, ExceptionLoadingFailed {
		_VARNAPanelList.clear();
		retrieveParametersValues();
		return _VARNAPanelList;
	}

	public int getNbRows() {
		return this._numRows;
	}

	public int getNbColumns() {
		return this._numColumns;
	}

	private void initValues() {

		// Applet assignable parameters
		_algo = "radiate";
		_auxBPs = "";
		_autoHelices = false;
		_autoInteriorLoops = false;
		_autoTerminalLoops = false;
		_annotations = "";
		_backgroundColor = VARNAConfig.DEFAULT_BACKGROUND_COLOR;
		_customBases = "";
		_customBPs = "";
		_chemProbs = "";

		_colorMapStyle = "";
		_colorMapValues = "";
		_colorMapCaption = "";
		_drawColorMap = false;

		_drawBases = true;
		_fillBases = true;
		_drawNC = true;
		_drawTertiary = true;
		_border = new Dimension(0, 0);
		_sseq = "";// =
		// "CAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAGUCAGUGUCAGACUGCAIA";
		_sstruct = "";// =
		// "..(((((...(((((...(((((...(((((.....)))))...))))).....(((((...(((((.....)))))...))))).....)))))...)))))..";
		_periodResNum = VARNAConfig.DEFAULT_PERIOD;
		_rotation = 0.0;
		_title = "";
		_titleSize = VARNAConfig.DEFAULT_TITLE_FONT.getSize();

		_backboneColor = VARNAConfig.DEFAULT_BACKBONE_COLOR;
		_drawBackbone = true;

		_bondColor = VARNAConfig.DEFAULT_BOND_COLOR;
		_bpStyle = VARNAConfig.DEFAULT_BP_STYLE;

		_highlightRegion = "";

		_baseOutlineColor = VARNAConfig.BASE_OUTLINE_COLOR_DEFAULT;
		_baseInnerColor = VARNAConfig.BASE_INNER_COLOR_DEFAULT;
		_baseNumColor = VARNAConfig.BASE_NUMBER_COLOR_DEFAULT;
		_baseNameColor = VARNAConfig.BASE_NAME_COLOR_DEFAULT;

		_titleColor = VARNAConfig.DEFAULT_TITLE_COLOR;
		_warning = false;
		_error = true;
		_modifiable = true;
		_zoom = VARNAConfig.DEFAULT_ZOOM;
		_zoomAmount = VARNAConfig.DEFAULT_AMOUNT;

		_comparisonMode = false;
		_firstSequence = "";
		_firstStructure = "";
		_secondSequence = "";
		_secondStructure = "";

		_gapsColor = VARNAConfig.DEFAULT_DASH_BASE_COLOR;
		_useGapsColor = false;
		_nonStandardColor = VARNAConfig.DEFAULT_SPECIAL_BASE_COLOR;
		_useNonStandardColor = false;

		_useInnerBaseColor = false;
		_useBaseNameColor = false;
		_useBaseNumbersColor = false;
		_useBaseOutlineColor = false;

		_bpIncrement = VARNAConfig.DEFAULT_BP_INCREMENT;

		_URL = "";
		_flatExteriorLoop = true;
		_flip = "";
		_orientation = "";
		_spaceBetweenBases = VARNAConfig.DEFAULT_SPACE_BETWEEN_BASES;
	}

	public static Color getSafeColor(String col, Color def) {
		Color result;
		try {
			result = Color.decode(col);

		} catch (NumberFormatException e) {
			try {
				result = Color.getColor(col, def);
			} catch (Exception e2) {
				// Not a valid color
				return def;
			}
		}
		return result;
	}

	public static final String LEONTIS_WESTHOF_BP_STYLE = "lw";
	public static final String LEONTIS_WESTHOF_BP_STYLE_ALT = "lwalt";
	public static final String SIMPLE_BP_STYLE = "simple";
	public static final String RNAVIZ_BP_STYLE = "rnaviz";
	public static final String NONE_BP_STYLE = "none";

	private VARNAConfig.BP_STYLE getSafeBPStyle(String opt,
			VARNAConfig.BP_STYLE def) {
		VARNAConfig.BP_STYLE b = VARNAConfig.BP_STYLE.getStyle(opt);
		if (b!= null)
		{
			return b;
		} else {
			return def;
		}
	}

	public static String[][] getParameterInfo() {
		String[][] info = {
				// Parameter Name Kind of Value Description
				{
						algoOpt,
						"String",
						"Drawing algorithm, choosen from ["
								+ VARNAConfigLoader.ALGORITHM_NAVIEW + ","
								+ VARNAConfigLoader.ALGORITHM_LINE + ","
								+ VARNAConfigLoader.ALGORITHM_RADIATE + ","
								+ VARNAConfigLoader.ALGORITHM_CIRCULAR + "]" },
				{ annotationsOpt, "string", "A set of textual annotations" },
				{ applyBasesStyleOpt, "String", "Base style application" },
				{
						auxBPsOpt,
						"String",
						"Adds a list of (possibly non-canonical) base-pairs to those already defined by the main secondary structure (Ex: \"(1,10);(2,11);(3,12)\"). Custom BP styles can be specified (Ex: \"(2,11):thickness=4;(3,12):color=#FF0000\")." },
				{ autoHelicesOpt, "", "" },
				{ autoInteriorLoopsOpt, "", "" },
				{ autoTerminalLoopsOpt, "", "" },
				{ backboneColorOpt, "Color", "Backbone color (Ex: #334455)" },
				{ backgroundColorOpt, "Color", "Background color (Ex: #334455)" },
				{ baseInnerColorOpt, "Color",
						"Default value for inner base color (Ex: #334455)" },
				{ baseNameColorOpt, "Color",
						"Residues font color (Ex: #334455)" },
				{ baseNumbersColorOpt, "Color",
						"Base numbers font color (Ex: #334455)" },
				{ baseOutlineColorOpt, "Color",
						"Base outline color (Ex: #334455)" },
				{ basesStyleOpt, "String", "Base style declaration" },
				{ borderOpt, "String",
						"Border width and height in pixels (Ex: \"20x40\")" },
				{ bondColorOpt, "Color", "Base pair color (Ex: #334455)" },
				{ bpIncrementOpt, "float",
						"Distance between nested base-pairs (i.e. arcs) in linear representation" },
				{
						bpStyleOpt,
						"String",
						"Look and feel for base pairs drawings, choosen from ["
								+ VARNAConfigLoader.LEONTIS_WESTHOF_BP_STYLE+ "," 
								+ VARNAConfigLoader.LEONTIS_WESTHOF_BP_STYLE_ALT+ "," 
								+ VARNAConfigLoader.NONE_BP_STYLE + ","
								+ VARNAConfigLoader.SIMPLE_BP_STYLE + ","
								+ VARNAConfigLoader.RNAVIZ_BP_STYLE + "]" },
				{ chemProbOpt, "", "" },
				{
						colorMapOpt,
						"String",
						"Associates a list of numerical values (eg '0.2,0.4,0.6,0.8') with the RNA bases with respect to their natural order, and modifies the color used to fill these bases according to current color map style." },
				{ colorMapCaptionOpt, "String",
						"Sets current color map caption." },
				{
						colorMapDefOpt,
						"String",
						"Selects a specific color map style. It can be either one of the predefined styles (eg 'red', 'green', 'blue', 'bw', 'heat', 'energy') or a new one (eg '0:#FFFF00;1:#ffFFFF;6:#FF0000')." },
				{ colorMapMinOpt, "", "" },
				{ colorMapMaxOpt, "", "" },
				{ comparisonModeOpt, "boolean", "Activates comparison mode" },
				{ customBasesOpt, "", "" },
				{ customBPsOpt, "", "" },
				{ drawBackboneOpt, "boolean",
						"True if the backbone must be drawn, false otherwise" },
				{ drawColorMapOpt, "", "" },
				{ drawNCOpt, "boolean",
						"Toggles on/off display of non-canonical base-pairs" },
				{ drawBasesOpt, "boolean", "Shows/hide the outline of bases" },
				{ drawTertiaryOpt, "boolean",
						"Toggles on/off display of tertiary interaction, ie pseudoknots" },
				{ errorOpt, "boolean", "Show errors" },
				{ fillBasesOpt, "boolean",
						"Fills or leaves empty the inner portions of bases" },
				{ firstSequenceForComparisonOpt, "String",
						"In comparison mode, sequence of first RNA" },
				{ firstStructureForComparisonOpt, "String",
						"In comparison mode, structure of first RNA" },
				{ flatExteriorLoopOpt, "boolean",
						"Toggles on/off (true/false) drawing exterior bases on a straight line" },
				{ flipOpt, "String",
						"Draws a set of exterior helices, identified by the argument string, in clockwise order (default drawing is counter-clockwise). The argument is a semicolon-separated list of helices, each identified by a base or a base-pair (eg. \"2;20-34\")." },
				{ gapsBaseColorOpt, "Color",
						"Define and use custom color for gaps bases in comparison mode" },
				{ highlightRegionOpt, "string", "Highlight a set of contiguous regions" },
				{ modifiableOpt, "boolean", "Allows/prohibits modifications" },
				{ nonStandardColorOpt, "Color",
						"Define and use custom color for non-standard bases in comparison mode" },
				{ numColumnsOpt, "int", "Sets number of columns" },
				{ numRowsOpt, "int", "Sets number of rows" },
				{
						orientationOpt,
						"float",
						"Sets the general orientation of an RNA, i.e. the deviation of the longest axis (defined by the most distant couple of bases) from the horizontal axis." },
				{ periodNumOpt, "int", "Periodicity of base-numbering" },
				{ secondSequenceForComparisonOpt, "String",
						"In comparison mode, sequence of second RNA" },
				{ secondStructureForComparisonOpt, "String",
						"In comparison mode, structure of second RNA" },
				{ sequenceOpt, "String", "Raw RNA sequence" },
				{ structureOpt, "String",
						"RNA structure given in dot bracket notation (DBN)" },
				{
						rotationOpt,
						"float",
						"Rotates RNA after initial drawing (Ex: '20' for a 20 degree counter-clockwise rotation)" },
				{ titleOpt, "String", "RNA drawing title" },
				{ titleColorOpt, "Color", "Title color (Ex: #334455)" },
				{ titleSizeOpt, "int", "Title font size" },
				{ spaceBetweenBasesOpt, "float",
						"Sets the space between consecutive bases" },
				{ warningOpt, "boolean", "Show warnings" },
				{ zoomOpt, "int", "Zoom coefficient" },
				{ zoomAmountOpt, "int", "Zoom increment on user interaction" } };
		return info;
	}

	private void retrieveParametersValues() throws ExceptionParameterError {

		_numRows = 1;
		_numColumns = 1;
		_basesStyleList = new ArrayList<ModelBaseStyle>();

		try {
			_numRows = Integer.parseInt(_optionProducer.getParameterValue(
					numRowsOpt, "" + _numRows));
		} catch (NumberFormatException e) {
			throw new ExceptionParameterError(e.getMessage(), "'"
					+ _optionProducer.getParameterValue(numRowsOpt, ""
							+ _numRows)
					+ "' is not a integer value for the number of rows !");
		}
		try {
			_numColumns = Integer.parseInt(_optionProducer.getParameterValue(
					numColumnsOpt, "" + _numColumns));
		} catch (NumberFormatException e) {
			throw new ExceptionParameterError(e.getMessage(), "'"
					+ _optionProducer.getParameterValue(numColumnsOpt, ""
							+ _numColumns)
					+ "' is not a integer value for the number of columns !");
		}

		String tmp = null;
		for (int i = 0; i < MAXSTYLE; i++) {
			String opt = basesStyleOpt + i;
			tmp = _optionProducer.getParameterValue(opt, null);
			// System.out.println(opt+"->"+tmp);
			if (tmp != null) {
				ModelBaseStyle msb = new ModelBaseStyle();
				try {
					msb.assignParameters(tmp);
				} catch (ExceptionModeleStyleBaseSyntaxError e) {
					VARNAPanel.emitWarningStatic(e, null);
				}
				_basesStyleList.add(msb);
			} else {
				_basesStyleList.add(null);
			}
		}

		// _containerApplet.getLayout().
		int x;
		String n;
		initValues();
		for (int i = 0; i < _numColumns; i++) {
			for (int j = 0; j < _numRows; j++) {
				try {
					// initValues();
					x = 1 + j + i * _numRows;
					n = "" + x;
					if ((_numColumns == 1) && (_numRows == 1)) {
						n = "";
					}
					_useGapsColor = false;
					_useNonStandardColor = false;

					tmp = _optionProducer.getParameterValue(baseNameColorOpt
							+ n, "");
					if (!tmp.equals("")) {
						_useBaseNameColor = true;
						_baseNameColor = getSafeColor(tmp, _baseNameColor);
					}
					tmp = _optionProducer.getParameterValue(baseNumbersColorOpt
							+ n, "");
					if (!tmp.equals("")) {
						_useBaseNumbersColor = true;
						_baseNumColor = getSafeColor(tmp, _baseNumColor);
					}
					tmp = _optionProducer.getParameterValue(baseOutlineColorOpt
							+ n, "");
					if (!tmp.equals("")) {
						_useBaseOutlineColor = true;
						_baseOutlineColor = getSafeColor(tmp, _baseOutlineColor);
					}
					tmp = _optionProducer.getParameterValue(baseInnerColorOpt
							+ n, "");
					if (!tmp.equals("")) {
						_useInnerBaseColor = true;
						_baseInnerColor = getSafeColor(tmp, _baseInnerColor);
					}

					tmp = _optionProducer.getParameterValue(nonStandardColorOpt
							+ n, "");
					if (!tmp.equals("")) {
						_nonStandardColor = getSafeColor(tmp, _nonStandardColor);
						_useNonStandardColor = true;
					}
					tmp = _optionProducer.getParameterValue(gapsBaseColorOpt
							+ n, _gapsColor.toString());
					if (!tmp.equals("")) {
						_gapsColor = getSafeColor(tmp, _gapsColor);
						_useGapsColor = true;
					}
					try {
						_rotation = Double.parseDouble(_optionProducer
								.getParameterValue(rotationOpt + n,
										Double.toString(_rotation)));
					} catch (NumberFormatException e) {
						throw new ExceptionParameterError(e.getMessage(), "'"
								+ _optionProducer.getParameterValue(rotationOpt
										+ n, "" + _rotation)
								+ "' is not a valid float value for rotation!");
					}

					try {
						_colorMapMin = Double.parseDouble(_optionProducer
								.getParameterValue(colorMapMinOpt + n,
										Double.toString(this._colorMapMin)));
					} catch (NumberFormatException e) {
						throw new ExceptionParameterError(
								e.getMessage(),
								"'"
										+ _optionProducer.getParameterValue(
												colorMapMinOpt + n, ""
														+ _colorMapMin)
										+ "' is not a valid double value for min color map values range!");
					}

					try {
						_colorMapMax = Double.parseDouble(_optionProducer
								.getParameterValue(colorMapMaxOpt + n,
										Double.toString(this._colorMapMax)));
					} catch (NumberFormatException e) {
						throw new ExceptionParameterError(
								e.getMessage(),
								"'"
										+ _optionProducer.getParameterValue(
												colorMapMaxOpt + n, ""
														+ _colorMapMax)
										+ "' is not a valid double value for max color map values range!");
					}

					try {
						_bpIncrement = Double.parseDouble(_optionProducer
								.getParameterValue(bpIncrementOpt + n,
										Double.toString(_bpIncrement)));
					} catch (NumberFormatException e) {
					}

					try {
						_periodResNum = Integer.parseInt(_optionProducer
								.getParameterValue(periodNumOpt + n, ""
										+ _periodResNum));
					} catch (NumberFormatException e) {
						throw new ExceptionParameterError(
								e.getMessage(),
								"'"
										+ _optionProducer.getParameterValue(
												periodNumOpt + n, ""
														+ _periodResNum)
										+ "' is not a valid integer value for the period of residue numbers!");
					}
					try {
						_titleSize = Integer.parseInt(_optionProducer
								.getParameterValue(titleSizeOpt + n, ""
										+ _titleSize));
					} catch (NumberFormatException e) {
						throw new ExceptionParameterError(
								e.getMessage(),
								"'"
										+ _optionProducer.getParameterValue(
												titleSizeOpt + n, ""
														+ _titleSize)
										+ "' is not a valid integer value for the number of rows !");
					}

					try {
						_zoom = Double.parseDouble(_optionProducer
								.getParameterValue(zoomOpt + n, "" + _zoom));
					} catch (NumberFormatException e) {
						throw new ExceptionParameterError(
								e.getMessage(),
								"'"
										+ _optionProducer.getParameterValue(
												zoomOpt + n, "" + _zoom)
										+ "' is not a valid integer value for the zoom !");
					}

					try {
						_zoomAmount = Double.parseDouble(_optionProducer
								.getParameterValue(zoomAmountOpt + n, ""
										+ _zoomAmount));
					} catch (NumberFormatException e) {
						throw new ExceptionParameterError(
								e.getMessage(),
								"'"
										+ _optionProducer.getParameterValue(
												zoomAmountOpt + n, ""
														+ _zoomAmount)
										+ "' is not a valid integer value for the zoom amount!");
					}

					try {
						_spaceBetweenBases = Double.parseDouble(_optionProducer
								.getParameterValue(spaceBetweenBasesOpt + n, ""
										+ _spaceBetweenBases));
					} catch (NumberFormatException e) {
						throw new ExceptionParameterError(
								e.getMessage(),
								"'"
										+ _optionProducer.getParameterValue(
												spaceBetweenBasesOpt + n, ""
														+ _spaceBetweenBases)
										+ "' is not a valid integer value for the base spacing!");
					}

					_drawBases = Boolean.parseBoolean(_optionProducer
							.getParameterValue(drawBasesOpt + n, ""
									+ _drawBases));
					_fillBases = Boolean.parseBoolean(_optionProducer
							.getParameterValue(fillBasesOpt + n, ""
									+ _fillBases));
					_autoHelices = Boolean.parseBoolean(_optionProducer
							.getParameterValue(autoHelicesOpt + n, ""
									+ _autoHelices));
					_drawColorMap = Boolean.parseBoolean(_optionProducer
							.getParameterValue(drawColorMapOpt + n, ""
									+ _drawColorMap));
					_drawBackbone = Boolean.parseBoolean(_optionProducer
							.getParameterValue(drawBackboneOpt + n, ""
									+ _drawBackbone));
					_colorMapValues = _optionProducer.getParameterValue(
							colorMapOpt + n, _colorMapValues);
					_autoTerminalLoops = Boolean.parseBoolean(_optionProducer
							.getParameterValue(autoTerminalLoopsOpt + n, ""
									+ _autoTerminalLoops));
					_autoInteriorLoops = Boolean.parseBoolean(_optionProducer
							.getParameterValue(autoInteriorLoopsOpt + n, ""
									+ _autoInteriorLoops));
					_drawNC = Boolean.parseBoolean(_optionProducer
							.getParameterValue(drawNCOpt + n, "" + _drawNC));
					_flatExteriorLoop = Boolean.parseBoolean(_optionProducer
							.getParameterValue(flatExteriorLoopOpt + n, ""
									+ _flatExteriorLoop));
					_drawTertiary = Boolean.parseBoolean(_optionProducer
							.getParameterValue(drawTertiaryOpt + n, ""
									+ _drawTertiary));
					_warning = Boolean.parseBoolean(_optionProducer
							.getParameterValue(warningOpt + n, "false"));
					_error = Boolean.parseBoolean(_optionProducer
							.getParameterValue(errorOpt + n, "true"));
					_border = parseDimension(_optionProducer.getParameterValue(
							borderOpt + n, "0X0"));
					_comparisonMode = Boolean.parseBoolean(_optionProducer
							.getParameterValue(comparisonModeOpt + n, "false"));
					_firstSequence = _optionProducer.getParameterValue(
							firstSequenceForComparisonOpt + n, _firstSequence);
					_firstStructure = _optionProducer
							.getParameterValue(firstStructureForComparisonOpt
									+ n, _firstStructure);
					_secondSequence = _optionProducer
							.getParameterValue(secondSequenceForComparisonOpt
									+ n, _secondSequence);
					_secondStructure = _optionProducer.getParameterValue(
							secondStructureForComparisonOpt + n,
							_secondStructure);
					_annotations = _optionProducer.getParameterValue(
							annotationsOpt + n, _annotations);
					_URL = _optionProducer.getParameterValue(URLOpt + n, _URL);
					_algo = _optionProducer.getParameterValue(algoOpt + n,
							_algo);
					_customBases = _optionProducer.getParameterValue(
							customBasesOpt + n, _customBases);
					_auxBPs = _optionProducer.getParameterValue(auxBPsOpt + n,
							_auxBPs);
					_highlightRegion = _optionProducer.getParameterValue(
							highlightRegionOpt + n, _highlightRegion);
					_chemProbs = _optionProducer.getParameterValue(chemProbOpt
							+ n, _chemProbs);
					_customBPs = _optionProducer.getParameterValue(customBPsOpt
							+ n, _customBPs);
					_colorMapStyle = _optionProducer.getParameterValue(
							colorMapDefOpt + n, _colorMapStyle);
					_colorMapCaption = _optionProducer.getParameterValue(
							colorMapCaptionOpt + n, _colorMapCaption);
					_backboneColor = getSafeColor(
							_optionProducer.getParameterValue(backboneColorOpt
									+ n, _backboneColor.toString()),
							_backboneColor);
					_backgroundColor = getSafeColor(
							_optionProducer.getParameterValue(
									backgroundColorOpt + n,
									_backgroundColor.toString()),
							_backgroundColor);
					_bondColor = getSafeColor(
							_optionProducer.getParameterValue(bondColorOpt + n,
									_bondColor.toString()), _bondColor);
					_bpStyle = getSafeBPStyle(
							_optionProducer.getParameterValue(bpStyleOpt + n,
									""), _bpStyle);
					_flip = _optionProducer.getParameterValue(
							flipOpt + n, _flip);
					_orientation = _optionProducer.getParameterValue(
							orientationOpt + n, _orientation);
					_titleColor = getSafeColor(
							_optionProducer.getParameterValue(
									titleColorOpt + n, _titleColor.toString()),
							_titleColor);

					
					
					if (!_URL.equals("")) {
						_sstruct = "";
						_sseq = "";
						_title = "";
					}
					_title = _optionProducer.getParameterValue(titleOpt + n, _title);

					if (_comparisonMode && _firstSequence != null
							&& _firstStructure != null
							&& _secondSequence != null
							&& _secondStructure != null) {
					} else {
						_sseq = _optionProducer.getParameterValue(sequenceOpt
								+ n, _sseq);
						_sstruct = _optionProducer.getParameterValue(
								structureOpt + n, _sstruct);
						if (!_sseq.equals("") && !_sstruct.equals("")) {
							_URL = "";
						}
						_comparisonMode = false;
					}

					// applique les valeurs des parametres recuperees
					applyValues(n);
				} catch (ExceptionParameterError e) {
					VARNAPanel.errorDialogStatic(e, _mainSurface);
				} catch (ExceptionNonEqualLength e) {
					VARNAPanel.errorDialogStatic(e, _mainSurface);
				} catch (IOException e) {
					VARNAPanel.errorDialogStatic(e, _mainSurface);
				} catch (ExceptionFileFormatOrSyntax e) {
					VARNAPanel.errorDialogStatic(e, _mainSurface);
				} catch (ExceptionLoadingFailed e) {
					VARNAPanel.errorDialogStatic(e, _mainSurface);
				}
			}// fin de boucle sur les lignes
		}// fin de boucle sur les colonnes
	}

	private RNA _defaultRNA = new RNA();

	public void setRNA(RNA r) {
		_defaultRNA = r;
	}

	public static final String ALGORITHM_CIRCULAR = "circular";
	public static final String ALGORITHM_NAVIEW = "naview";
	public static final String ALGORITHM_LINE = "line";
	public static final String ALGORITHM_RADIATE = "radiate";
	public static final String ALGORITHM_VARNA_VIEW = "varnaview";
	public static final String ALGORITHM_MOTIF_VIEW = "motifview";

	private void applyValues(String n) throws ExceptionParameterError,
			ExceptionNonEqualLength, IOException, ExceptionFileFormatOrSyntax,
			ExceptionLoadingFailed {
		boolean applyOptions = true;
		int algoCode;
		if (_algo.equals(ALGORITHM_CIRCULAR))
			algoCode = RNA.DRAW_MODE_CIRCULAR;
		else if (_algo.equals(ALGORITHM_NAVIEW))
			algoCode = RNA.DRAW_MODE_NAVIEW;
		else if (_algo.equals(ALGORITHM_LINE))
			algoCode = RNA.DRAW_MODE_LINEAR;
		else if (_algo.equals(ALGORITHM_RADIATE))
			algoCode = RNA.DRAW_MODE_RADIATE;
		else if (_algo.equals(ALGORITHM_VARNA_VIEW))
			algoCode = RNA.DRAW_MODE_VARNA_VIEW;
		else if (_algo.equals(ALGORITHM_MOTIF_VIEW))
			algoCode = RNA.DRAW_MODE_MOTIFVIEW;
		else
			algoCode = RNA.DRAW_MODE_RADIATE;

		if (_comparisonMode) {
			_mainSurface = new VARNAPanel(_firstSequence, _firstStructure,
					_secondSequence, _secondStructure, algoCode, "");
		} else {
			_mainSurface = new VARNAPanel();
		}

		_VARNAPanelList.add(_mainSurface);
		_mainSurface.setSpaceBetweenBases(_spaceBetweenBases);
		_mainSurface.setTitle(_title);

		if (!_URL.equals("")) {
			URL url = null;
			try {

				_mainSurface.setSpaceBetweenBases(_spaceBetweenBases);

				url = new URL(_URL);
				URLConnection connexion = url.openConnection();
				connexion.setUseCaches(false);
				InputStream r = connexion.getInputStream();
				InputStreamReader inr = new InputStreamReader(r);

				if (_URL.toLowerCase().endsWith(
						VARNAPanel.VARNA_SESSION_EXTENSION)) {
					FullBackup f;
					f = VARNAPanel.importSession(r, _URL);
					_mainSurface.setConfig(f.config);
					_mainSurface.showRNA(f.rna);
					applyOptions = false;
				} else {
					Collection<RNA> rnas = RNAFactory.loadSecStr(
							new BufferedReader(inr),
							RNAFactory.guessFileTypeFromExtension(_URL));
					if (rnas.isEmpty()) {
						throw new ExceptionFileFormatOrSyntax(
								"No RNA in file '" + _URL + "'.");
					}
					RNA rna = rnas.iterator().next();
					rna.drawRNA(algoCode, _mainSurface.getConfig());
					_mainSurface.drawRNA(rna, algoCode);
				}
				if (!_title.isEmpty())
				{
					_mainSurface.setTitle(_title);
				}
			} catch (ExceptionFileFormatOrSyntax e) {
				if (url != null)
					e.setPath(url.getPath());
			} catch (ExceptionDrawingAlgorithm e) {
				_mainSurface.emitWarning(e.getMessage());
			}

		} else {
			if (!_comparisonMode) {
				if (!_sstruct.equals("")) {
					_mainSurface.drawRNA(_sseq, _sstruct, algoCode);
				} else {
					try {
						System.err.println("Printing default RNA "+_defaultRNA);
						_defaultRNA.drawRNA(algoCode, _mainSurface.getConfig());
					} catch (ExceptionDrawingAlgorithm e) {
						e.printStackTrace();
					}
					_mainSurface.drawRNA(_defaultRNA);
				}
			}
		}
		if (applyOptions)
		{
			if (_useInnerBaseColor) {
				_mainSurface.setBaseInnerColor(_baseInnerColor);
			}
			if (_useBaseOutlineColor) {
				_mainSurface.setBaseOutlineColor(_baseOutlineColor);
			}
			if (_useBaseNameColor) {
				_mainSurface.setBaseNameColor(_baseNameColor);
			}
			if (_useBaseNumbersColor) {
				_mainSurface.setBaseNumbersColor(_baseNumColor);
			}

			_mainSurface.setBackground(_backgroundColor);
			_mainSurface.setNumPeriod(_periodResNum);
			_mainSurface.setBackboneColor(_backboneColor);
			_mainSurface.setDefaultBPColor(_bondColor);
			_mainSurface.setBPHeightIncrement(_bpIncrement);
			_mainSurface.setBPStyle(_bpStyle);
			_mainSurface.setDrawBackbone(_drawBackbone);

			_mainSurface.setTitleFontColor(_titleColor);
			_mainSurface.setTitleFontSize(_titleSize);

			_mainSurface.getPopupMenu().get_itemShowWarnings()
					.setState(_warning);
			_mainSurface.setErrorsOn(_error);
			_mainSurface.setFlatExteriorLoop(_flatExteriorLoop);
			_mainSurface.setZoom(_zoom);
			_mainSurface.setZoomIncrement(_zoomAmount);
			_mainSurface.setBorderSize(_border);

			if (_useGapsColor) {
				_mainSurface.setGapsBasesColor(this._gapsColor);
				_mainSurface.setColorGapsBases(true);
			}

			if (_useNonStandardColor) {
				_mainSurface.setNonStandardBasesColor(_nonStandardColor);
				_mainSurface.setColorNonStandardBases(true);
			}

			_mainSurface.setShowNonPlanarBP(_drawTertiary);
			_mainSurface.setShowNonCanonicalBP(_drawNC);

			applyBasesStyle(n);

			if (!_customBases.equals(""))
				applyBasesCustomStyles(_mainSurface);

			if (!_highlightRegion.equals(""))
				applyHighlightRegion(_mainSurface);

			if (!_auxBPs.equals(""))
				applyAuxBPs(_mainSurface);

			if (!_chemProbs.equals(""))
				applyChemProbs(_mainSurface);

			if (!_customBPs.equals(""))
				applyBPsCustomStyles(_mainSurface);

			_mainSurface.setDrawOutlineBases(_drawBases);
			_mainSurface.setFillBases(_fillBases);
			_mainSurface.drawRNA();

			if (!_annotations.equals(""))
				applyAnnotations(_mainSurface);
			if (_autoHelices)
				_mainSurface.getVARNAUI().UIAutoAnnotateHelices();
			if (_autoTerminalLoops)
				_mainSurface.getVARNAUI().UIAutoAnnotateTerminalLoops();
			if (_autoInteriorLoops)
				_mainSurface.getVARNAUI().UIAutoAnnotateInteriorLoops();

			if (!_orientation.equals("")) {
				try {
					double d = 360 * _mainSurface.getOrientation()
							/ (2. * Math.PI);
					_rotation = Double.parseDouble(_orientation) - d;
				} catch (NumberFormatException e) {
					// TODO : Add some code here...
				}

			}
			_mainSurface.globalRotation(_rotation);

			_mainSurface.setModifiable(_modifiable);

			_mainSurface.setColorMapCaption(_colorMapCaption);
			applyColorMapStyle(_mainSurface);
			applyFlips(_mainSurface);
			applyColorMapValues(_mainSurface);
			
			// if (!_drawColorMap)
			// _mainSurface.drawColorMap(_drawColorMap);
		}
		// ajoute le VARNAPanel au conteneur
	}

	private void applyBasesStyle(String n) throws ExceptionParameterError {
		String tmp = null;
		for (int numStyle = 0; numStyle < _basesStyleList.size(); numStyle++) {
			if (_basesStyleList.get(numStyle) != null) {
				tmp = _optionProducer.getParameterValue(applyBasesStyleOpt
						+ (numStyle) + "on" + n, null);

				ArrayList<Integer> indicesList = new ArrayList<Integer>();
				if (tmp != null) {
					String[] basesList = tmp.split(",");
					for (int k = 0; k < basesList.length; k++) {
						String cand = basesList[k].trim();
						try {
							String[] args = cand.split("-");
							if (args.length == 1) {
								int baseNum = Integer.parseInt(cand);
								int index = _mainSurface.getRNA()
										.getIndexFromBaseNumber(baseNum);
								if (index != -1) {
									indicesList.add(index);
								}
							} else if (args.length == 2) {
								int baseNumFrom = Integer.parseInt(args[0]
										.trim());
								int indexFrom = _mainSurface.getRNA()
										.getIndexFromBaseNumber(baseNumFrom);
								int baseNumTo = Integer
										.parseInt(args[1].trim());
								int indexTo = _mainSurface.getRNA()
										.getIndexFromBaseNumber(baseNumTo);
								if ((indexFrom != -1) && (indexTo != -1)) {
									for (int l = indexFrom; l <= indexTo; l++)
										indicesList.add(l);
								}
							}
						} catch (NumberFormatException e) {
							throw new ExceptionParameterError(e.getMessage(),
									"Bad Base Index: " + basesList[k]);
						}
					}
					for (int k = 0; k < indicesList.size(); k++) {
						int index = indicesList.get(k);
						if ((index >= 0)
								&& (index < _mainSurface.getRNA()
										.get_listeBases().size())) {
							_mainSurface
									.getRNA()
									.get_listeBases()
									.get(index)
									.setStyleBase(_basesStyleList.get(numStyle));
						}
					}
				}
			}
		}// fin de boucle sur les styles

	}

	private void applyColorMapStyle(VARNAPanel vp) {
		if (_colorMapStyle.length() != 0) {
			vp.setColorMap(ModeleColorMap.parseColorMap(_colorMapStyle));
		}
	}

	private void applyColorMapValues(VARNAPanel vp) {
		if (!_colorMapValues.equals("")) {
			File f = new File(_colorMapValues);
			if(f.exists() && !f.isDirectory()) { 
				try {
					vp.readValues(new FileReader(f));
					vp.drawColorMap(true);
					System.err.println("Loaded "+_colorMapValues);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			else
			{
				String[] values = _colorMapValues.split("[;,]");
				ArrayList<Double> vals = new ArrayList<Double>();
				for (int i = 0; i < values.length; i++) {
					try {
						vals.add(Double.parseDouble(values[i]));
					} catch (Exception e) {
	
					}
				}
				Double[] result = new Double[vals.size()];
				vals.toArray(result);
				vp.setColorMapValues(result);
			}
			ModeleColorMap cm = vp.getColorMap();
			if (_colorMapMin != Double.MIN_VALUE) {
				// System.out.println("[A]"+_colorMapMin);
				cm.setMinValue(_colorMapMin);
			}
			if (_colorMapMax != Double.MAX_VALUE) {
				cm.setMaxValue(_colorMapMax);
			}
			_drawColorMap = true;
		}
	}

	private void applyBasesCustomStyles(VARNAPanel vp) {
		String[] baseStyles = _customBases.split(";");
		for (int i = 0; i < baseStyles.length; i++) {
			String thisStyle = baseStyles[i];
			String[] data = thisStyle.split(":");
			try {
				if (data.length == 2) {
					int baseNum = Integer.parseInt(data[0]);
					int index = _mainSurface.getRNA().getIndexFromBaseNumber(
							baseNum);
					if (index != -1) {
						String style = data[1];
						ModelBaseStyle msb = vp.getRNA().get_listeBases()
								.get(index).getStyleBase().clone();
						msb.assignParameters(style);
						vp.getRNA().get_listeBases().get(index)
								.setStyleBase(msb);
					}
				}
			} catch (Exception e) {
				System.err.println("ApplyBasesCustomStyle: " + e.toString());
			}
		}
	}

	private void applyHighlightRegion(VARNAPanel vp) {
		String[] regions = _highlightRegion.split(";");
		for (int i = 0; i < regions.length; i++) {
			String region = regions[i];
			try {
				HighlightRegionAnnotation nt = HighlightRegionAnnotation
						.parseHighlightRegionAnnotation(region, vp);
				if (nt != null) {
					vp.addHighlightRegion(nt);
				}
			} catch (Exception e) {
				System.err.println("Error in applyHighlightRegion: " + e.toString());
			}
		}
	}

	private Dimension parseDimension(String s) {
		Dimension d = new Dimension(0, 0);
		try {
			s = s.toLowerCase();
			int i = s.indexOf('x');
			String w = s.substring(0, i);
			String h = s.substring(i + 1);
			d.width = Integer.parseInt(w);
			d.height = Integer.parseInt(h);
		} catch (NumberFormatException e) {
		}
		return d;
	}

	private void applyBPsCustomStyles(VARNAPanel vp) {
		String[] baseStyles = _customBPs.split(";");
		for (int i = 0; i < baseStyles.length; i++) {
			String thisStyle = baseStyles[i];
			String[] data = thisStyle.split(":");
			try {
				if (data.length == 2) {
					String indices = data[0];
					String style = data[1];
					String[] data2 = indices.split(",");
					if (data2.length == 2) {
						String s1 = data2[0];
						String s2 = data2[1];
						if (s1.startsWith("(") && s2.endsWith(")")) {
							int a = Integer.parseInt(s1.substring(1));
							int b = Integer.parseInt(s2.substring(0,
									s2.length() - 1));
							ModeleBP msbp = vp.getRNA().getBPStyle(a, b);
							if (msbp != null) {
								msbp.assignParameters(style);
							}
						}
					}
				}
			} catch (Exception e) {
				System.err.println("ApplyBPsCustomStyle: " + e.toString());
			}
		}
	}

	private void applyChemProbs(VARNAPanel vp) {
		String[] chemProbs = _chemProbs.split(";");
		for (int i = 0; i < chemProbs.length; i++) {
			String thisAnn = chemProbs[i];
			String[] data = thisAnn.split(":");
			try {
				if (data.length == 2) {
					String indices = data[0];
					String style = data[1];
					String[] data2 = indices.split("-");
					if (data2.length == 2) {
						int a = Integer.parseInt(data2[0]);
						int b = Integer.parseInt(data2[1]);
						int c = vp.getRNA().getIndexFromBaseNumber(a);
						int d = vp.getRNA().getIndexFromBaseNumber(b);
						ArrayList<ModeleBase> mbl = vp.getRNA()
								.get_listeBases();
						ChemProbAnnotation cpa = new ChemProbAnnotation(
								mbl.get(c), mbl.get(d), style);
						vp.getRNA().addChemProbAnnotation(cpa);
					}
				}
			} catch (Exception e) {
				System.err.println("ChempProbs: " + e.toString());
			}
		}
	}

	private void applyAuxBPs(VARNAPanel vp) {
		String[] baseStyles = _auxBPs.split(";");

		for (int i = 0; i < baseStyles.length; i++) {
			String thisStyle = baseStyles[i];
			String[] data = thisStyle.split(":");
			try {
				if (data.length >= 1) {
					String indices = data[0];
					String[] data2 = indices.split(",");
					if (data2.length == 2) {
						String s1 = data2[0];
						String s2 = data2[1];
						if (s1.startsWith("(") && s2.endsWith(")")) {
							int a = Integer.parseInt(s1.substring(1));
							int b = Integer.parseInt(s2.substring(0,
									s2.length() - 1));
							int c = vp.getRNA().getIndexFromBaseNumber(a);
							int d = vp.getRNA().getIndexFromBaseNumber(b);

							ModeleBP msbp = new ModeleBP(vp.getRNA()
									.get_listeBases().get(c), vp.getRNA()
									.get_listeBases().get(d));
							if (data.length >= 2) {
								String style = data[1];
								msbp.assignParameters(style);
							}
							vp.getRNA()
									.addBPToStructureUsingNumbers(a, b, msbp);
						}
					}
				}
			} catch (ExceptionModeleStyleBaseSyntaxError e1) {
				System.err.println("AuxApplyBPs: " + e1.toString());
			} catch (ExceptionParameterError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void applyFlips(VARNAPanel vp) {
		String[] flips = _flip.split(";");
		for (String s: flips)
		{
			if (!s.isEmpty())
			{
				try{
					String[] data = s.split("-");
					int number = -1;
					if (data.length==1)
					{
						number = Integer.parseInt(data[0]);					
					}
					else if (data.length==2)
					{
						number = Integer.parseInt(data[1]);
					}
					if (number!=-1)
					{
						int i = vp.getRNA().getIndexFromBaseNumber(number);
						Point h = vp.getRNA().getExteriorHelix(i);
						vp.getRNA().flipHelix(h);
					}
				} catch (Exception e) {
					System.err.println("Flip Helices: " + e.toString());
				}
			}
		}
	}
	
	/**
	 * Format:
	 * string:[type=[H|B|L|P]|x=double|y=double|anchor=int|size=int|color
	 * =Color];
	 * 
	 * @param vp
	 */
	private void applyAnnotations(VARNAPanel vp) {
		String[] annotations = _annotations.split(";");
		for (int i = 0; i < annotations.length; i++) {
			String thisAnn = annotations[i];
			TextAnnotation ann = TextAnnotation.parse(thisAnn, vp);
			vp.addAnnotation(ann);
		}
	}


}


