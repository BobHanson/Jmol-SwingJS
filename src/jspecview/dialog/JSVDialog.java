package jspecview.dialog;

import java.util.Map;

import javajs.util.DF;

import javajs.util.PT;

import jspecview.api.AnnotationData;
import jspecview.api.JSVPanel;
import jspecview.api.PlatformDialog;
import jspecview.common.Annotation;
import jspecview.common.Coordinate;
import jspecview.common.IntegralData;
import jspecview.common.Spectrum;
import jspecview.common.JSViewer;
import jspecview.common.MeasurementData;
import jspecview.common.PanelData;
import jspecview.common.Parameters;
import jspecview.common.PeakData;

abstract public class JSVDialog extends Annotation implements AnnotationData {

	abstract public int[] getPosXY();

	public String optionKey;
	public Map<String, Object> options;
	
	protected AType type;
	protected String title;
	protected JSViewer vwr;
	protected Spectrum spec;
	protected DialogManager manager;
	protected PlatformDialog dialog;
	protected JSVPanel jsvp;
	protected Object txt1, txt2, txt3;
	protected Object combo1; // measurement listing, peaks
	protected MeasurementData xyData;
	protected Parameters myParams;
	protected int precision = 1;

	private int[] loc;
	private Object showHideButton; // text changeable
	private boolean addClearBtn, addCombo1;
	protected boolean addApplyBtn;
	private boolean isNumeric; // not Views or OverlayLegend
	private boolean defaultVisible; // not OverlayLegend
	private String subType;
	private String graphSetKey;
	private Object[][] tableData;
	private boolean addUnits;
	private String[] unitOptions;
	private int[] formatOptions;
	private Integer unitPtr;
	private boolean isON = true;

	abstract protected void addUniqueControls();

	abstract public boolean callback(String id, String msg);

	/**
	 * 
	 * required initializer; from JSViewer
	 * 
	 * @param title
	 * @param viewer
	 * @param spec
	 * @return this
	 */
	public JSVDialog setParams(String title, JSViewer viewer, Spectrum spec) {
		title = DialogManager.fixTitle(title);
		this.title = title;
		this.vwr = viewer;
		this.spec = spec;
		manager = viewer.getDialogManager();
		jsvp = viewer.selectedPanel;
		myParams = ((Parameters) viewer.getPlatformInterface("Parameters"))
				.setName("dialogParams");
		subType = (spec == null ? "!" : spec.getTypeLabel());
		optionKey = type + "_" + subType;
		options = manager.getDialogOptions();
		if (spec != null) {
				Object[] specOptions = spec.getDefaultAnnotationInfo(type);
			options.put(optionKey, specOptions);
			unitOptions = (String[]) specOptions[0];
			formatOptions = (int[]) specOptions[1];
			unitPtr = (Integer) options.get(optionKey + "_unitPtr");
			if (unitPtr == null)
				unitPtr = (Integer) specOptions[2];
		}
		switch (type) {
		case Integration:
			isNumeric = true;
			addClearBtn = true;
			defaultVisible = true;
			addApplyBtn = true;
			break;
		case Measurements:
			isNumeric = true;
			addClearBtn = true;
			// no apply button
			addCombo1 = true;
			defaultVisible = true;
			break;
		case OverlayLegend:
			break;
		case PeakList:
			isNumeric = true;
			addApplyBtn = true;
			defaultVisible = true;
			break;
		case Views:
			defaultVisible = true;
			break;
		case NONE:
			break;
		}
		initDialog();
		return this;
	}


	// //// frame construction ////////

	private void initDialog() {
		dialog = manager.getDialog(this);
		restoreDialogPosition(jsvp, getPosXY());
		dialog.setTitle(title);
		layoutDialog();
	}

	protected void layoutDialog() {
		dialog.startLayout();
		addUniqueControls();
		if (isNumeric) {
			getUnitOptions();
			if (addCombo1)
				combo1 = dialog.addSelectOption("cmbUnits", "Units", unitOptions,
						unitPtr.intValue(), addUnits);
			// txtFontSize = ((DialogHelper dialogHelper)).addInputOption("FontSize",
			// "Font Size", null, null, "10");
			if (addApplyBtn)
				dialog.addButton("btnApply", "Apply");
			showHideButton = dialog.addButton("btnShow", "Show");
			if (addClearBtn)
				dialog.addButton("btnClear", "Clear");
		}
		dialog.endLayout();
		checkEnables();
		dialog.setVisible(defaultVisible);
	}

	/**
	 * @param id
	 * @param msg
	 * @return true if consumed
	 */
	protected boolean callbackAD(String id, String msg) {
		if (id.equals("FOCUS")) {
			eventFocus();
		} else if (id.equals("tableSelect")) {
			tableSelect(msg);
		} else if (id.equals("btnClear")) {
			clear();
		} else if (id.equals("btnApply")) {
			eventApply();
		} else if (id.equals("btnShow")) {
			String label = dialog.getText(showHideButton);
			eventShowHide(label.equals("Show"));
		} else if (id.equals("cmbUnits")) {
			setPrecision(dialog.getSelectedIndex(combo1));
		} else if (id.startsWith("txt")) {
			eventApply();
		} else if (id.equals("windowClosing")) {
			done();
			return true;
		}
		if (jsvp != null)
			jsvp.doRepaint(true);
		return true;
	}

	/**
	 * @param dialogHelper
	 */
	protected void addUniqueControls(DialogManager dialogHelper) {
		// int and peak only
	}

	// /////// general interface to the outside world ////////

	@Override
	public AType getAType() {
		return type;
	}

	@Override
	public String getGraphSetKey() {
		return graphSetKey;
	}

	@Override
	public void setGraphSetKey(String key) {
		this.graphSetKey = key;
	}

	@Override
	public Spectrum getSpectrum() {
		return spec;
	}

	@Override
	public boolean getState() {
		return isON;
	}

	@Override
	public void setState(boolean b) {
		isON = b;
	}

	// ////// interface to DialogParams////////

	public void checkEnables() {
		boolean isShow = checkVisible();
		dialog.setText(showHideButton, isShow ? "Hide" : "Show");
	}

	public void createTable(Object[][] data, String[] header, int[] widths) {
		tableData = data;
		dialog.createTable(data, header, widths);
	}

	public void setTableSelectionEnabled(boolean enabled) {
		dialog.setCellSelectionEnabled(enabled);
	}

	@Override
	public Parameters getParameters() {
		return myParams;
	}

	public void showMessage(String msg, String title, int msgType) {
		manager.showMessageDialog(dialog, msg, title, msgType);
	}

	protected void setThreshold(double y) {
		dialog.setText(txt1, getThreasholdText(y));
	}

	public void setComboSelected(int i) {
		dialog.setSelectedIndex(combo1, i);
	}

	public void applyFromFields() {
		apply(null);
	}

	public JSVDialog reEnable() {
		paramsReEnable();
		return this;
	}

	public void dispose() {
		dialog.dispose();
	}

	public void setVisible(boolean visible) {
		dialog.setVisible(visible);
	}

	@Override
	public boolean isVisible() {
		return dialog.isVisible();
	}

	public void selectTableRow(int i) {
		dialog.selectTableRow(i);
	}

	public void repaint() {
		dialog.repaint();
	}

	public void setFields() {
		switch (type) {
		case Integration:
			break;
		case Measurements:
			break;
		case NONE:
			break;
		case PeakList:
			myParams = xyData.getParameters();
			setThreshold(myParams.peakListThreshold);
			setComboSelected(myParams.peakListInterpolation.equals("none") ? 1
							: 0);
			createData();
			break;
		case OverlayLegend:
			break;
		case Views:
			break;
		}
	}

	public void setFocus(boolean tf) {
		dialog.setFocus(tf);		
	}

	public void update(Coordinate clicked, double xRange, int yOffset) {
		selectTableRow(-1);
		switch (type) {
		case Integration:
			loadData();
			checkEnables();
			break;
		case Measurements:
			loadData();
			checkEnables();
			break;
		case NONE:
			break;
		case PeakList:
			if (yOffset > 20)
				applyFromFields();
			if (xyData == null || clicked == null || yOffset > 20)
				return;
			int ipt = 0;
			double dx0 = 1e100;
			double xval = clicked.getXVal();
			PeakData md = (PeakData) xyData;
			double min = Math.abs(xRange / 20);
			for (int i = md.size(); --i >= 0;) {
				double dx = Math.abs(xval - md.get(i).getXVal());
				if (dx < dx0) {
					dx0 = dx;
					ipt = i;
				}
			}
			if (dx0 < min) {
				selectTableRow(md.size() - 2 - ipt);
				repaint();
			}
			break;
		case OverlayLegend:
			break;
		case Views:
			break;
		}
	}

	protected double lastNorm = 1;

	public MeasurementData getPeakData() {
		// could be called by outside, but isn't. 
		PeakData md = new PeakData(AType.PeakList, spec);
		md.setPeakList(myParams, precision, jsvp.getPanelData().getView());
		xyData = md;
		return null;
	}

	@Override
	public MeasurementData getData() {
		if (xyData == null)
			createData();
		return xyData;
	}

	public void setData(AnnotationData data) {
		myParams = data.getParameters();
		xyData = (MeasurementData) data;
	}

	@Override
	public void setSpecShift(double dx) {
		if (xyData != null)
			xyData.setSpecShift(dx);
	}

	public void setType(AType type) {
		this.type = type;
		switch (type) {
		case Measurements:
			addUnits = true;
			break;
		case Integration:
			break;
		case PeakList:
			break;
		case OverlayLegend:
			break;
		case Views:
			break;
		case NONE:
			break;

		}
	}

	////////////// protected methods
	
	protected int iRowColSelected = -1;
	protected int iSelected = -1;

	protected void apply(Object[] objects) {
		try {
			switch (type) {
			case Integration:
				double offset = Double.parseDouble((String) objects[0]);
				double scale = Double.parseDouble((String) objects[1]);
				//double minval = Double.parseDouble((String) objects[2]);
				myParams.integralOffset = offset;
				myParams.integralRange = scale;
				//myParams.integralMinY = minval;
				myParams.integralDrawAll = false;
				((IntegralData) getData()).update(myParams);
				break;
			case Measurements:
				// n/a
				break;
			case NONE:
				return;
			case PeakList:
				if (!skipCreate) {
					setThreshold(Double.NaN);
					createData();
				}
				skipCreate = false;
				break;
			case OverlayLegend:
				break;
			case Views:
				vwr.parameters.viewOffset = Double.parseDouble((String) objects[0]);
				break;
			}
			loadData();
			checkEnables();
			jsvp.doRepaint(true);
		} catch (Exception e) {
			// ignore
		}
	}

	protected void done() {
		if (jsvp != null && spec != null)
			jsvp.getPanelData().removeDialog(this);
		// setState(false);
		if (xyData != null)
			xyData.setState(isON);
		saveDialogPosition(getPosXY());
		dispose();
		jsvp.doRepaint(true);
	}

	
	// /////////// private methods

	/**
	 * 
	 * @param panel
	 *          a PPanel (Applet) or a JScrollPane (MainFrame)
	 * 
	 * @param posXY
	 *          static for a given dialog
	 */
	private void restoreDialogPosition(JSVPanel panel, int[] posXY) {
		if (panel != null) {
			if (posXY[0] == Integer.MIN_VALUE) {
				posXY[0] = 0;
				posXY[1] = -20;
			}
			int[] pt = manager.getLocationOnScreen(panel);
			int height = panel.getHeight();
			loc = new int[] { Math.max(0, pt[0] + posXY[0]),
					Math.max(0, pt[1] + height + posXY[1]) };
			dialog.setIntLocation(loc);
		}
	}

	private void saveDialogPosition(int[] posXY) {
		try {
			int[] pt = manager.getLocationOnScreen(dialog);
			posXY[0] += pt[0] - loc[0];
			posXY[1] += pt[1] - loc[1];
		} catch (Exception e) {
			// ignore
		}
	}
	
	private String getThreasholdText(double y) {
		if (Double.isNaN(y)) {
			PanelData pd = jsvp.getPanelData();
			double f = (pd.getSpectrum().isInverted() ? 0.1 : 0.9);
			Coordinate c = pd.getClickedCoordinate();
			y = (c == null ? (pd.getView().minYOnScale * f + pd.getView().maxYOnScale)
					* (1 - f)
					: c.getYVal());
		}
		String sy = DF.formatDecimalDbl(y, y < 1000 ? 2 : -2); // "#0.00" :
		// "0.00E0"
		return " " + sy;
	}

	private boolean checkVisible() {
		return vwr.pd().getShowAnnotation(type);
	}

	private void getUnitOptions() {
		String key = optionKey + "_format";
		Integer format = (Integer) options.get(key);
		if (format == null)
			options.put(key, format = Integer
					.valueOf(formatOptions[unitPtr == null ? 0 : unitPtr.intValue()]));
		// txtFormat = dialogHelper.addInputOption("numberFormat", "Number Format",
		// format, null, null, false);
	}

	private boolean skipCreate;
	
	protected void eventFocus() {
		if (spec != null)
			jsvp.getPanelData().jumpToSpectrum(spec);
		switch (type) {
		case Integration:
			if (iRowSelected >= 0) {
				iRowSelected++;
				tableCellSelect(-1, -1);
			}
			break;
		case Measurements:
			break;
		case NONE:
			break;
		case PeakList:
			createData();
			skipCreate = true;
			break;
		case OverlayLegend:
			break;
		case Views:
			break;
		}
	}

	protected void eventApply() {
		switch (type) {
		case Integration:
			break;
		case Measurements:
			break;
		case NONE:
			break;
		case PeakList:
			createData();
			skipCreate = true;
			break;
		case OverlayLegend:
			break;
		case Views:
			break;
		}
		applyFromFields();
	}

	private void eventShowHide(boolean isShow) {
		isON = isShow;
		if (isShow)
			eventApply();
		jsvp.doRepaint(true);
		checkEnables();
	}

	private void clear() {
		setState(true);
		if (xyData != null) {
			xyData.clear();
			applyFromFields();
		}
	}

	private void paramsReEnable() {
		switch (type) {
		case Integration:
			break;
		case Measurements:
			break;
		case NONE:
			break;
		case PeakList:
			skipCreate = true;
			break;
		case OverlayLegend:
			break;
		case Views:
			break;
		}
		setVisible(true);
		isON = true;
		applyFromFields();
	}

	private int iRowSelected = -1;
	private int iColSelected = -1;

	private void tableCellSelect(int iRow, int iCol) {
		System.out.println(iRow + " jSVDial " + iCol);
		if (iRow < 0) {
			iRow = iRowColSelected / 1000;
			iCol = iRowColSelected % 1000;
			iRowColSelected = -1;
		}
		Object value = tableData[iRow][1];
		int icolrow = iRow * 1000 + iCol;
		if (icolrow == iRowColSelected)
			return;
		iRowColSelected = icolrow;
		System.out.println("Setting rc = " + iRowColSelected + " " + spec);
		selectTableRow(iRowSelected);
		try {
			switch (type) {
			case Integration:
				callback("SHOWSELECTION", value.toString());
				checkEnables();
				break;
			case Measurements:
				break;
			case NONE:
				break;
			case PeakList:
				try {
					switch (iCol) {
					case 6:
					case 5:
					case 4:
						double x1 = Double.parseDouble((String) value);
						double x2 = Double
								.parseDouble((String) tableData[iRow + 3 - iCol][1]);
						jsvp.getPanelData().setXPointers(spec, x1, spec, x2);
						break;
					default:
						jsvp.getPanelData().findX(spec, Double.parseDouble((String) value));
					}
				} catch (Exception e) {
					jsvp.getPanelData().findX(spec, 1E100);
				}
				jsvp.doRepaint(false);
				break;
			case OverlayLegend:
				jsvp.getPanelData().setSpectrum(iRow, false);
				break;
			case Views:
				break;
			}
		} catch (Exception e) {
			// for parseDouble
		}
	}

	protected void loadData() {
		Object[][] data;
		String[] header;
		int[] widths;
		switch (type) {
		case Integration:
			if (xyData == null)
				createData();
			iSelected = -1;
			data = ((IntegralData) xyData).getMeasurementListArray(null);
			header = xyData.getDataHeader();
			widths = new int[] { 40, 65, 65, 50 };
			createTable(data, header, widths);
			break;
		case Measurements:
			if (xyData == null)
				return;
			data = xyData.getMeasurementListArray(dialog.getSelectedItem(combo1).toString());
			header = xyData.getDataHeader();
			widths = new int[] { 40, 65, 65, 50 };
			createTable(data, header, widths);
			break;
		case NONE:
			break;
		case PeakList:
			if (xyData == null)
				createData();
			data = ((PeakData) xyData).getMeasurementListArray(null);
			header = ((PeakData) xyData).getDataHeader();
			widths = new int[] { 40, 65, 50, 50, 50, 50, 50 };
			createTable(data, header, widths);
			setTableSelectionEnabled(true);
			break;
		case OverlayLegend:
			header = new String[] { "No.", "Plot Color", "Title" };
			data = vwr.selectedPanel.getPanelData().getOverlayLegendData();
			widths = new int[] { 30, 60, 250 };
			createTable(data, header, widths);
			setTableSelectionEnabled(true);
			break;
		case Views:
			break;
		}
	}

	private void createData() {
		switch (type) {
		case Integration:
			xyData = new IntegralData(spec, myParams);
			iSelected = -1;
			break;
		case Measurements:
			// n/a
			break;
		case NONE:
			break;
		case PeakList:
			try {
				double thresh = Double.parseDouble(dialog.getText(txt1));
				myParams.peakListThreshold = thresh;
				myParams.peakListInterpolation = dialog.getSelectedItem(combo1)
						.toString();
				myParams.precision = precision;
				PeakData md = new PeakData(AType.PeakList, spec);
				md.setPeakList(myParams, precision, jsvp.getPanelData().getView());
				xyData = md;
				loadData();
			} catch (Exception e) {
				// for parseDouble
			}
			break;
		case OverlayLegend:
			break;
		case Views:
			break;
		}
	}

	private void setPrecision(int i) {
		precision = formatOptions[i];
	}

	private void tableSelect(String url) {
		boolean isAdjusting = "true".equals(getField(url, "adjusting"));
		if (isAdjusting) {
			iColSelected = iRowSelected = -1;
			System.out.println("adjusting" + url);
			return;
		}
		int index = PT.parseInt(getField(url, "index"));
		switch ("ROW COL ROWCOL".indexOf(getField(url, "selector"))) {
		case 8:
			iColSelected = PT.parseInt(getField(url, "index2"));
			//$FALL-THROUGH$
		case 0:
			iRowSelected = index;
			System.out.println("r set to " + index);
			break;
		case 4:
			iColSelected = index;
			System.out.println("c set to " + index);
			break;
		}
		if (iColSelected >= 0 && iRowSelected >= 0) {
			tableCellSelect(iRowSelected, iColSelected);
		}
	}

	private String getField(String url, String name) {
		url += "&";
		String key = "&" + name + "=";
		int pt = url.indexOf(key);
		return (pt < 0 ? null : url.substring(pt + key.length(), url.indexOf("&",
				pt + 1)));
	}


}
