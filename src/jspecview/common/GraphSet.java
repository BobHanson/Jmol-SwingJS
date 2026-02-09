package jspecview.common;

import java.util.Hashtable;

import java.util.Map;

//import javajs.J2SRequireImport;
import javajs.api.GenericColor;
import javajs.util.BS;
import javajs.util.DF;
import javajs.util.Lst;

import org.jmol.api.GenericGraphics;
import org.jmol.util.Font;
import org.jmol.util.Logger;
import javajs.util.PT;

import jspecview.api.AnnotationData;
import jspecview.api.JSVPanel;
import jspecview.api.VisibleInterface;
import jspecview.common.Annotation.AType;
import jspecview.common.PanelData.LinkMode;
import jspecview.common.Spectrum.IRMode;
import jspecview.dialog.JSVDialog;

//@J2SRequireImport(jspecview.dialog.JSVDialog.class)
class GraphSet implements XYScaleConverter {

  private final static int ARROW_RESET = -1;
  private final static int ARROW_HOME = 0;
  private final static int ARROW_LEFT = 1;
  private final static int ARROW_RIGHT = 2;
  private final static int ARROW_UP = 3;
  private final static int ARROW_DOWN = 4;

  private GraphSet gs2dLinkedX;
  private GraphSet gs2dLinkedY;
  private boolean cur1D2Locked;

  private Lst<Highlight> highlights = new Lst<Highlight>();
  Lst<Spectrum> spectra = new Lst<Spectrum>();

  private boolean isSplittable = true;
  private boolean allowStacking = true; // not MS

  private Lst<Annotation> annotations;
  private MeasurementData selectedSpectrumMeasurements;
  private MeasurementData selectedSpectrumIntegrals;
  private Annotation lastAnnotation;
  Measurement pendingMeasurement;
  private Integral pendingIntegral;
  private Lst<Spectrum> graphsTemp = new Lst<Spectrum>();
  private PlotWidget[] widgets;
  private boolean isLinked;
  private boolean haveSingleYScale;

  final static double RT2 = Math.sqrt(2.0);
  private static GenericColor veryLightGrey;

  GraphSet(PanelData pd) {
    this.pd = pd;
    jsvp = pd.jsvp;
    g2d = pd.g2d;
  }

  /**
   * iSpectrumMovedTo
   * 
   * -- indicates spectrum moved to by user
   * 
   * -- originally 0
   * 
   * -- set in mouseMovedEvent only when nSpectra > 1: to iSpecBold if iSpecBold
   * >= 0 or to -1 if showAllStacked or to getSplitPoint(yPixel)
   * 
   * -- used in doZoom to set spectrum number for the new View object int iSpec
   * = (iSpecBold >= 0 ? iSpecBold : iSpectrumMovedTo);
   * 
   * -- used in drawAll to set the frame with the purple boundary int iSpec =
   * (nSpectra == 1 ? 0 : !showAllStacked ? iSpectrumMovedTo : iSpecBold >= 0 ?
   * iSpecBold : iSpectrumSelected);
   * 
   */
  /* very */private int iSpectrumMovedTo = -1;

  int setSpectrumMovedTo(int i) {
    return iSpectrumMovedTo = i;
  }

  /**
   * iSpectrumClicked
   * 
   * -- indicates spectrum clicked on by user -- when set T/F, also sets
   * iSpectrumSelected T/F
   * 
   * -- initially 0
   * 
   * -- set in checkSpectrumClickEvent from PanelData.setCurrentGraphSet when
   * nSplit == 1 && showAllStacked && isClick to spectrum number if on spectrum
   * to -1 if click is not on a spectrum
   * 
   * -- set in MouseClickEvent to previous spectrum clicked if it is a double
   * click and the previous click was on a spectrum (also sets iSpectrumSelected
   * in that case)
   * 
   * -- set in processPendingMeasurement to index of previous pendingMeasurement
   * when clickCount == 1
   * 
   * -- used in mouseClickEvent if (iSpectrumClicked >= 0) {
   * processPendingMeasurement(xPixel, yPixel, 2); }
   * 
   * -- used in processPendingMeasurement pendingMeasurement = new
   * Measurement(this, spectra.get(iSpectrumClicked)...
   * 
   * 
   */
  /* very */private int iSpectrumClicked;

  private void setSpectrumClicked(int i) {
    stackSelected = showAllStacked;
    if (i < 0 || iSpectrumClicked != i) {
      lastClickX = Double.NaN;
      lastPixelX = Integer.MAX_VALUE;
    }
    iSpectrumClicked = setSpectrumSelected(setSpectrumMovedTo(i));
  }

  /**
   * iSpectrumSelected
   * 
   * -- indicates current spectrum index selected -- by clicking Left/Right
   * arrow -- by clicking on a spectrum --
   * 
   * -- originally -1 -- [0,nSpectra) indicates selected by clicking or peak
   * picking -- Integer.MIN_VALUE -- none selected (and display none)
   * 
   * -- set in PanelData.setCurrentGraphSet to currentSplitPoint when gs.nSplit
   * > 1 && !gs.showAllStacked
   * 
   * -- set in checkArrowLeftRightClick to selected spectrum if LEFT or RIGHT,
   * or to -1 if HOME circle
   * 
   * -- set in checkSpectrumClickEvent to spectrum clicked on, or to -1 if
   * clicked off-spectrum
   * 
   * -- set in mouseClickEvent along with iSpectrumClicked to the previously
   * clicked spectrum when there is a double click.
   * 
   * -- set in selectSpectrum based on filePath, type, and model to -1 if
   * nSpectra == 1, or to the selected spectrum index if there is a match, or to
   * Integer.MIN_VALUE if this isn't the current graph set and there is a
   * selected spectrum already ??
   * 
   * -- used all over the place, in checkArrowLeftRightClick,
   * checkArrowUpDownClick, checkSpectrum, doPlot, drawAll, drawPeakTabs,
   * drawPlot, drawSpectrum, getFixedSelectedSpectrumIndex, isDrawNoSpectra, and
   * selectSpectrum,
   * 
   * -- used in doPlot to return true when a split is to be shown, or when
   * showAllStacked is true, or when no spectrum is selected, or when this is
   * the spectrum selected
   * 
   */

  /* very */private int iSpectrumSelected = -1;

  private int setSpectrumSelected(int i) {
    boolean isNew = (i != iSpectrumSelected);
    iSpectrumSelected = i;
    if (isNew) {
      getCurrentView();
    }
    return iSpectrumSelected;
  }

  private boolean stackSelected = false;
  private BS bsSelected = new BS();

  // needed by PanelData

  ViewData viewData;
  boolean reversePlot;
  int nSplit = 1;
  int yStackOffsetPercent = 0;

  /**
   * if nSplit > 1, then showAllStacked is false, but if nSplit == 1, then
   * showAllStacked may be true or false
   */
  boolean showAllStacked = true;

  // needed by AwtGraphSet

  Lst<ViewData> viewList;
  ImageView imageView;
  private PanelData pd;
  private boolean sticky2Dcursor;
  int nSpectra; // also needed by PanelData

  void closeDialogsExcept(AType type) {
    if (dialogs != null)
      for (Map.Entry<String, AnnotationData> e : dialogs.entrySet()) {
        AnnotationData ad = e.getValue();
        if (isDialog(ad) && (type == AType.NONE || ad.getAType() != type))
          ((JSVDialog) ad).setVisible(false);
      }
  }

  void dispose() {
    spectra = null;
    viewData = null;
    viewList = null;
    annotations = null;
    lastAnnotation = null;
    pendingMeasurement = null;
    imageView = null;
    graphsTemp = null;
    widgets = null;
    disposeImage();
    if (dialogs != null)
      for (Map.Entry<String, AnnotationData> e : dialogs.entrySet()) {
        AnnotationData ad = e.getValue();
        if (isDialog(ad))
          ((JSVDialog) ad).dispose();
      }
    dialogs = null;
  }

  private double fracX = 1, fracY = 1, fX0 = 0, fY0 = 0; // take up full screen

  private PlotWidget zoomBox1D, zoomBox2D, pin1Dx0, pin1Dx1, // ppm range --
      // horizontal bar
      // on 1D spectrum
      pin1Dy0, pin1Dy1, // y-scaling -- vertical bar on 1D spectrum and left of
      // 2D when no 1D
      pin1Dx01, pin1Dy01, // center pins for those
      pin2Dx0, pin2Dx1, // ppm range -- horizontal bar on 2D spectrum
      pin2Dy0, pin2Dy1, // subspectrum range -- vertical bar on 2D spectrum
      pin2Dx01, pin2Dy01, // center pins for those
      cur2Dx0, cur2Dx1, // 2D x cursors -- derived from pin1Dx0 and pin1Dx1
      // values
      cur1D2x1, cur1D2x2, // 1D cursors derived from 2D cursors
      cur2Dy; // 2D y cursor -- points to currently displayed 1D slice

  // for the 1D plot area:
  /**
   * the bounds of the inner graph area
   */
  private int xPixel0, yPixel0, xPixel1, yPixel1;

  /**
   * the width and height of the inner graph area
   */
  private int xPixels, yPixels;

  /***
   * the corners of the area in the frame allocated to a given spectrum
   */
  private int xPixel00, yPixel00, xPixel11, yPixel11;

  private int yPixel000;

  private int xPixel10, xPixels0;

  private int xVArrows, xHArrows, yHArrows;

  private boolean allowStackedYScale = true;
  private boolean drawXAxisLeftToRight;
  private boolean xAxisLeftToRight = true;
  private int iPreviousSpectrumClicked = -1;
  private boolean haveSelectedSpectrum;

  private boolean zoomEnabled;
  private int currentZoomIndex;

  private double lastClickX = Double.NaN;
  private int lastPixelX = Integer.MAX_VALUE;

  private boolean isDrawNoSpectra() {
    return (iSpectrumSelected == Integer.MIN_VALUE);
  }

  /**
   * 
   * @return spectrum index selected by user from a peak pick, a spectrum pick
   *         with showAllStacked, but set to 0 if out of range
   */

  private int getFixedSelectedSpectrumIndex() {
    return Math.max(iSpectrumSelected, 0);
  }

  private int height;
  private int width;
  private int right;
  private int top;
  private int left;
  private int bottom;

  private PeakInfo piMouseOver;
  private final Coordinate coordTemp = new Coordinate();
  private final static int minNumOfPointsForZoom = 3;

  final private int FONT_PLAIN = 0;
  final private int FONT_BOLD = 1;
  final private int FONT_ITALIC = 2;

  private boolean is2DSpectrum;

  Spectrum getSpectrum() {
    // could be a 2D spectrum or a set of mass spectra
    return getSpectrumAt(getFixedSelectedSpectrumIndex())
        .getCurrentSubSpectrum();
  }

  /**
   * Returns the <code>Spectrum</code> at the specified index
   * 
   * @param index
   *        the index of the <code>Spectrum</code>
   * @return the <code>Spectrum</code> at the specified index
   */
  Spectrum getSpectrumAt(int index) {
    return spectra.get(index);
  }

  int getSpectrumIndex(Spectrum spec) {
    for (int i = spectra.size(); --i >= 0;)
      if (spectra.get(i) == spec)
        return i;
    return -1;
  }

  private void addSpec(Spectrum spec) {
    spectra.addLast(spec);
    nSpectra++;
  }

  void splitStack(boolean doSplit) {
    if (doSplit && isSplittable) {
      nSplit = nSpectra;
      showAllStacked = false;
      setSpectrumClicked(iSpectrumSelected);
      pd.currentSplitPoint = iSpectrumSelected;
    } else {
      nSplit = 1;
      showAllStacked = allowStacking && !doSplit;
      setSpectrumClicked(iSpectrumSelected);
    }
    stackSelected = false;
    setFractionalPositions(pd, pd.graphSets, LinkMode.NONE);
    pd.setTaintedAll();
  }

  private void setPositionForFrame(int iSplit) {
    if (iSplit < 0)
      iSplit = 0;
    int marginalHeight = height - 50;
    xPixel00 = (int) (width * fX0);
    xPixel11 = (int) (xPixel00 + width * fracX - 1);
    xHArrows = xPixel00 + 25;
    xVArrows = xPixel11 - right / 2;
    xPixel0 = xPixel00 + (int) (left * (1 - fX0));
    xPixel10 = xPixel1 = xPixel11 - right;
    xPixels0 = xPixels = xPixel1 - xPixel0 + 1;
    // only the very top spectrum needs an offset
    // -- to move it below the coordinate string
    yPixel000 = (fY0 == 0 ? 25 : 0) + (int) (height * fY0);
    yPixel00 = yPixel000 + (int) (marginalHeight * fracY * iSplit);
    yPixel11 = yPixel00 + (int) (marginalHeight * fracY) - 1;
    yHArrows = yPixel11 - 12;
    yPixel0 = yPixel00 + top / 2;
    yPixel1 = yPixel11 - bottom / 2;
    yPixels = yPixel1 - yPixel0 + 1;
    if (imageView != null && is2DSpectrum) {
      setImageWindow();
      if (pd.display1D) {
        double widthRatio = (pd.display1D
            ? 1.0 * (xPixels0 - imageView.xPixels) / xPixels0
            : 1);
        xPixels = (int) Math.floor(widthRatio * xPixels0 * 0.8);
        xPixel1 = xPixel0 + xPixels - 1;
      } else {
        xPixels = 0;
        xPixel1 = imageView.xPixel0 - 30;
      }
    }
  }

  private boolean hasPoint(int xPixel, int yPixel) {
    return (xPixel >= xPixel00 && xPixel <= xPixel11 && yPixel >= yPixel000
        && yPixel <= yPixel11 * nSplit);
  }

  private boolean isInPlotRegion(int xPixel, int yPixel) {
    return (xPixel >= xPixel0 && xPixel <= xPixel1 && yPixel >= yPixel0
        && yPixel <= yPixel1);
  }

  int getSplitPoint(int yPixel) {
    return (yPixel1 == yPixel00 ? 0
        : Math.max(0, Math.min((yPixel - yPixel000) / (yPixel11 - yPixel00),
            nSplit - 1)));
  }

  private boolean isSplitWidget(int xPixel, int yPixel) {
    return isFrameBox(xPixel, yPixel, splitterX, splitterY);
  }

  private boolean isCloserWidget(int xPixel, int yPixel) {
    return isFrameBox(xPixel, yPixel, closerX, closerY);
  }

  /**
   * Initializes the graph set
   * 
   * @param startIndex
   * @param endIndex
   */

  private void initGraphSet(int startIndex, int endIndex) {
    if (veryLightGrey == null)
      veryLightGrey = g2d.getColor3(200, 200, 200);
    setPlotColors(ColorParameters.defaultPlotColors);
    xAxisLeftToRight = getSpectrumAt(0).shouldDisplayXAxisIncreasing();
    setDrawXAxis();
    int[] startIndices = new int[nSpectra];
    int[] endIndices = new int[nSpectra];
    bsSelected.setBits(0, nSpectra);
    // null means use standard offset spectrumOffsets = new int[nSpectra];
    allowStackedYScale = true;
    if (endIndex <= 0)
      endIndex = Integer.MAX_VALUE;
    isSplittable = (nSpectra > 1);// for now, could be:
    allowStacking = (spectra.get(0).isStackable());
    showAllStacked = allowStacking && (nSpectra > 1);
    for (int i = 0; i < nSpectra; i++) {
      int iLast = spectra.get(i).getXYCoords().length - 1;
      startIndices[i] = Coordinate.intoRange(startIndex, 0, iLast);
      endIndices[i] = Coordinate.intoRange(endIndex, 0, iLast);
      allowStackedYScale &= (spectra.get(i).getYUnits()
          .equals(spectra.get(0).getYUnits())
          && spectra.get(i).getUserYFactor() == spectra.get(0)
              .getUserYFactor());
    }
    getView(0, 0, 0, 0, startIndices, endIndices, null, null);
    viewList = new Lst<ViewData>();
    viewList.addLast(viewData);
  }

  private synchronized void getView(double x1, double x2, double y1, double y2,
                                    int[] startIndices, int[] endIndices,
                                    ScaleData[] viewScales,
                                    ScaleData[] yScales) {
    Lst<Spectrum> graphs = (graphsTemp.size() == 0 ? spectra : graphsTemp);
    Lst<Spectrum> subspecs = getSpectrumAt(0).getSubSpectra();
    boolean dontUseSubspecs = (subspecs == null
        || subspecs.size() == 2 && subspecs.get(1).isImaginary());
    // NMR real/imaginary
    boolean is2D = !getSpectrumAt(0).is1D();
    boolean useFirstSubSpecOnly = false;
    if (is2D && useFirstSubSpecOnly || dontUseSubspecs && y1 == y2) {
      // 2D spectrum or startup
      graphs = spectra;
    } else if (y1 == y2) {
      // start up, forced subsets (too many spectra)
      viewData = new ViewData(subspecs, y1, y2, getSpectrum().isContinuous());
      graphs = null;
    }
    if (graphs != null) {
      viewData = new ViewData(graphs, y1, y2, startIndices, endIndices,
          getSpectrumAt(0).isContinuous(), is2D);
      if (x1 != x2)
        getScale().setXRange(x1, x2);
    }
    if (viewScales != null) {
      ScaleData.copyScaleFactors(viewScales, viewData.getScaleData());
      if (yScales != null)
        ScaleData.copyYScales(yScales, viewData.getScaleData());
      getCurrentView();
    }
  }

  private boolean isNearby(Coordinate a1, Coordinate a2, XYScaleConverter c,
                           int range) {
    double x = a1.getXVal();
    int xp1 = c.toPixelX(x);
    int yp1 = toPixelY(a1.getYVal());
    x = a2.getXVal();
    int xp2 = c.toPixelX(x);
    int yp2 = toPixelY(a2.getYVal());
    return (Math.abs(xp1 - xp2) + Math.abs(yp1 - yp2) < range);
  }

  /**
   * Displays plot in reverse if val is true
   * 
   * @param val
   *        true or false
   */
  void setReversePlot(boolean val) {
    reversePlot = val;
    if (reversePlot)
      closeDialogsExcept(AType.NONE);
    setDrawXAxis();
  }

  private void setDrawXAxis() {
    drawXAxisLeftToRight = xAxisLeftToRight ^ reversePlot;
    for (int i = 0; i < spectra.size(); i++)
      (spectra.get(i)).setExportXAxisDirection(drawXAxisLeftToRight);
  }

  private boolean isInTopBar(int xPixel, int yPixel) {
    return (xPixel == fixX(xPixel) && yPixel > pin1Dx0.yPixel0 - 2
        && yPixel < pin1Dx0.yPixel1);
  }

  private boolean isInTopBar2D(int xPixel, int yPixel) {
    return (imageView != null && xPixel == imageView.fixX(xPixel)
        && yPixel > pin2Dx0.yPixel0 - 2 && yPixel < pin2Dx0.yPixel1);
  }

  private boolean isInRightBar(int xPixel, int yPixel) {
    return (yPixel == fixY(yPixel) && xPixel > pin1Dy0.xPixel1
        && xPixel < pin1Dy0.xPixel0 + 2);
  }

  private boolean isInRightBar2D(int xPixel, int yPixel) {
    return (imageView != null && yPixel == fixY(yPixel)
        && xPixel > pin2Dy0.xPixel1 && xPixel < pin2Dy0.xPixel0 + 2);
  }

  private double toX0(int xPixel) {
    return viewList.get(0).getScale().toX0(fixX(xPixel), xPixel0, xPixel1,
        drawXAxisLeftToRight);
  }

  private double toY0(int yPixel) {
    return viewList.get(0).getScale().toY0(fixY(yPixel), yPixel0, yPixel1);
  }

  @Override
  public double toX(int xPixel) {
    if (imageView != null && imageView.isXWithinRange(xPixel))
      return imageView.toX(xPixel);
    return getScale().toX(fixX(xPixel), xPixel1, drawXAxisLeftToRight);
  }

  @Override
  public double toY(int yPixel) {
    return getScale().toY(yPixel, yPixel0);
  }

  @Override
  public int toPixelX(double dx) {
    return getScale().toPixelX(dx, xPixel0, xPixel1, drawXAxisLeftToRight);
  }

  @Override
  public int toPixelY(double yVal) {
    return getScale().toPixelY(yVal, yPixel1);
  }

  private int toPixelX0(double x) {
    return viewList.get(0).getScale().toPixelX0(x, xPixel0, xPixel1,
        drawXAxisLeftToRight);
  }

  private int toPixelY0(double y) {
    return fixY(viewList.get(0).getScale().toPixelY0(y, yPixel0, yPixel1));
  }

  @Override
  public int fixX(int xPixel) {
    return Coordinate.intoRange(xPixel, xPixel0, xPixel1);
  }

  @Override
  public int fixY(int yPixel) {
    return Coordinate.intoRange(yPixel, yPixel0, yPixel1);
  }

  @Override
  public int getXPixel0() {
    return xPixel0;
  }

  @Override
  public int getXPixels() {
    return xPixels;
  }

  @Override
  public int getYPixels() {
    return yPixels;
  }

  @Override
  public ScaleData getScale() {
    return viewData.getScale();
  }

  private int toPixelYint(double yVal) {
    return yPixel1
        - (int) (Double.isNaN(yVal) ? Integer.MIN_VALUE : yPixels * yVal);
  }

  private Annotation findAnnotation2D(Coordinate xy) {
    for (int i = annotations.size(); --i >= 0;) {
      Annotation a = annotations.get(i);
      if (isNearby(a, xy, imageView, 10))
        return a;
    }
    return null;
  }

  private void addAnnotation(Annotation annotation, boolean isToggle) {
    if (annotations == null)
      annotations = new Lst<Annotation>();
    boolean removed = false;
    for (int i = annotations.size(); --i >= 0;)
      if (annotation.is2D
          ? isNearby(annotations.get(i), annotation, imageView, 10)
          : annotation.equals(annotations.get(i))) {
        removed = true;
        annotations.removeItemAt(i);
      }
    if (annotation.text.length() > 0 && (!removed || !isToggle))
      annotations.addLast(annotation);
  }

  private void setImageWindow() {
    imageView.setPixelWidthHeight((int) ((pd.display1D ? 0.6 : 1) * xPixels0),
        yPixels);
    imageView.setXY0(getSpectrumAt(0),
        (int) Math.floor(xPixel10 - imageView.xPixels), yPixel0);
  }

  private Measurement selectedMeasurement;
  private Integral selectedIntegral;

  private double lastXMax = Double.NaN;
  private int lastSpecClicked = -1;

  private boolean findNearestMaxMin() {
    if (nSpectra > 1 && iSpectrumClicked < 0)
      return false;
    xValueMovedTo = getSpectrum().findXForPeakNearest(xValueMovedTo);
    setXPixelMovedTo(xValueMovedTo, Double.MAX_VALUE, 0, 0);
    return !Double.isNaN(xValueMovedTo);
  }

  void setXPixelMovedTo(double x1, double x2, int xPixel1, int xPixel2) {
    if (x1 == Double.MAX_VALUE && x2 == Double.MAX_VALUE) {
      xPixelMovedTo = xPixel1;
      xPixelMovedTo2 = xPixel2;
      if (isLinked && sticky2Dcursor) {
        pd.setlinkedXMove(this, toX(xPixelMovedTo), false);
      }
      return;
    }
    if (x1 != Double.MAX_VALUE) {
      xPixelMovedTo = toPixelX(x1);
      if (fixX(xPixelMovedTo) != xPixelMovedTo)
        xPixelMovedTo = -1;
      xPixelMovedTo2 = -1;
      if (x1 != 1e10)
        setSpectrumClicked(getFixedSelectedSpectrumIndex());
    }
    if (x2 != Double.MAX_VALUE) {
      xPixelMovedTo2 = toPixelX(x2);
    }
  }

  private void processPendingMeasurement(int xPixel, int yPixel,
                                         int clickCount) {
    if (!isInPlotRegion(xPixel, yPixel) || is2dClick(xPixel, yPixel)) {
      pendingMeasurement = null;
      return;
    }
    double x = toX(xPixel);
    double y = toY(yPixel);
    double x0 = x;
    Measurement m;
    switch (clickCount) {
    case 0: // move
      pendingMeasurement.setPt2(toX(xPixel), toY(yPixel));
      break;
    case 3: // ctrl-click
    case 2: // 1st double-click
      if (iSpectrumClicked < 0)
        return;
      Spectrum spec = spectra.get(iSpectrumClicked);
      setScale(iSpectrumClicked);
      if (clickCount == 3) {
      } else {
        m = findMeasurement(selectedSpectrumMeasurements, xPixel, yPixel,
            Measurement.PT_XY1);
        if (m != null) {
          x = m.getXVal();
          y = m.getYVal();
        } else if ((m = findMeasurement(selectedSpectrumMeasurements, xPixel,
            yPixel, Measurement.PT_XY2)) != null) {
          x = m.getXVal2();
          y = m.getYVal2();
        } else {
          x = getNearestPeak(spec, x, y);
        }
      }
      pendingMeasurement = new Measurement().setM1(x, y, spec);
      pendingMeasurement.setPt2(x0, y);
      pd.setTaintedAll();
      pd.repaint();
      break;
    case 1: // single click -- save and continue
    case -2: // second double-click -- save and quit
    case -3: // second ctrl-click
      boolean isOK = (pendingMeasurement != null
          && isVisible(getDialog(AType.Measurements, -1)));
      while (isOK) {
        setScale(getSpectrumIndex(pendingMeasurement.spec));
        if (clickCount != 3) {
          if (!findNearestMaxMin()) {
            isOK = false;
            break;
          }
          xPixel = xPixelMovedTo;
        }
        x = toX(xPixel);
        y = toY(yPixel);
        pendingMeasurement.setPt2(x, y);
        if (pendingMeasurement.text.length() == 0) {
          isOK = false;
          break;
        }
        setMeasurement(pendingMeasurement);
        if (clickCount != 1) {
          isOK = false;
          break;
        }
        setSpectrumClicked(getSpectrumIndex(pendingMeasurement.spec));
        pendingMeasurement = new Measurement().setM1(x, y,
            pendingMeasurement.spec);
        break;
      }
      if (!isOK)
        pendingMeasurement = null;
      pd.setTaintedAll();
      pd.repaint();
      break;
    case 5: // (old) control-click
      if (findNearestMaxMin()) {
        int iSpec = getFixedSelectedSpectrumIndex();
        if (Double.isNaN(lastXMax) || lastSpecClicked != iSpec
            || pendingMeasurement == null) {
          lastXMax = xValueMovedTo;
          lastSpecClicked = iSpec;
          pendingMeasurement = new Measurement().setM1(xValueMovedTo,
              yValueMovedTo, spectra.get(iSpec));
        } else {
          pendingMeasurement.setPt2(xValueMovedTo, yValueMovedTo);
          if (pendingMeasurement.text.length() > 0)
            setMeasurement(pendingMeasurement);
          pendingMeasurement = null;
          lastXMax = Double.NaN;
        }
      } else {
        lastXMax = Double.NaN;
      }
      break;
    }
  }

  private boolean checkIntegralNormalizationClick(int xPixel, int yPixel) {
    if (selectedSpectrumIntegrals == null)
      return false;
    Integral integral = (Integral) findMeasurement(selectedSpectrumIntegrals,
        xPixel, yPixel, Measurement.PT_INT_LABEL);
    if (integral == null)
      return false;
    selectedIntegral = integral;
    pd.normalizeIntegral();
    updateDialog(AType.Integration, -1);
    setSpectrumClicked(getSpectrumIndex(integral.spec));
    return true;
  }

  /**
   * search for the nearest peak above/below the given y value
   * 
   * @param spec
   * 
   * @param x
   * @param y
   * @return nearest x value
   */
  private double getNearestPeak(Spectrum spec, double x, double y) {
    double x0 = Coordinate.getNearestXWithYAbove(spec.getXYCoords(), x, y,
        spec.isInverted(), false);
    double x1 = Coordinate.getNearestXWithYAbove(spec.getXYCoords(), x, y,
        spec.isInverted(), true);
    return (Double.isNaN(x0) ? x1
        : Double.isNaN(x1) ? x0
            : Math.abs(x0 - x) < Math.abs(x1 - x) ? x0 : x1);
  }

  private Measurement findMeasurement(MeasurementData measurements, int xPixel,
                                      int yPixel, int iPt) {
    if (measurements == null || measurements.size() == 0)
      return null;
    if (iPt == Measurement.PT_ON_LINE) {
      Measurement m = findMeasurement(measurements, xPixel, yPixel,
          Measurement.PT_ON_LINE1);
      if (m != null || measurements.get(0) instanceof Integral)
        return m;
      return findMeasurement(measurements, xPixel, yPixel,
          Measurement.PT_ON_LINE2); // lower bar,
      // near baseline
    }
    for (int i = measurements.size(); --i >= 0;) {
      Measurement m = measurements.get(i);
      int x1, x2, y1, y2;
      if (m instanceof Integral) {
        x1 = x2 = toPixelX(m.getXVal2());
        y1 = toPixelYint(m.getYVal());
        y2 = toPixelYint(m.getYVal2());
      } else {
        x1 = toPixelX(m.getXVal());
        x2 = toPixelX(m.getXVal2());
        y1 = y2 = (iPt == -2 ? yPixel1 - 2 : toPixelY(m.getYVal()));
      }
      switch (iPt) {
      case Measurement.PT_XY1:
        if (Math.abs(xPixel - x1) + Math.abs(yPixel - y1) < 4)
          return m;
        break;
      case Measurement.PT_XY2:
        if (Math.abs(xPixel - x2) + Math.abs(yPixel - y2) < 4)
          return m;
        break;
      case Measurement.PT_INT_LABEL: // label for integral
        y1 = y2 = (y1 + y2) / 2;
        x2 = x1 + 20; // estimate only
        //$FALL-THROUGH$
      default:
      case Measurement.PT_ON_LINE1:
      case Measurement.PT_ON_LINE2:
        if (isOnLine(xPixel, yPixel, x1, y1, x2, y2))
          return m;
        break;
      }

    }
    return null;
  }

  private void setMeasurement(Measurement m) {
    int iSpec = getSpectrumIndex(m.spec);
    AnnotationData ad = getDialog(AType.Measurements, iSpec);
    if (ad == null)
      addDialog(iSpec, AType.Measurements,
          ad = new MeasurementData(AType.Measurements, m.spec));
    ad.getData().addLast(m.copyM());
    updateDialog(AType.Measurements, -1);
  }

  private boolean checkArrowUpDownClick(int xPixel, int yPixel) {
    boolean ok = false;
    double f = (isArrowClick(xPixel, yPixel, ARROW_UP) ? RT2
        : isArrowClick(xPixel, yPixel, ARROW_DOWN) ? 1 / RT2 : 0);
    if (f != 0) {
      if (nSplit > 1)
        setSpectrumSelected(iSpectrumMovedTo);
      if ((nSpectra == 1 || iSpectrumSelected >= 0)
          && spectra.get(getFixedSelectedSpectrumIndex()).isTransmittance())
        f = 1 / f;
      viewData.scaleSpectrum(imageView == null ? iSpectrumSelected : -2, f);
      ok = true;
    } else if (isArrowClick(xPixel, yPixel, ARROW_RESET)) {
      resetViewCompletely();
      ok = true;
    }

    if (ok) {
      if (imageView != null) {
        update2dImage(false);
        resetPinsFromView();
      }
      pd.setTaintedAll();
    }
    return ok;
  }

  void resetViewCompletely() {
    // reset button between up/down arrows;
    clearViews();
    if (showAllStacked && !stackSelected)
      closeDialogsExcept(AType.NONE);
    viewData.resetScaleFactors();
    // did not work: view.setScaleFactor(iSpectrumSelected, 1);
    updateDialogs();
  }

  private boolean checkArrowLeftRightClick(int xPixel, int yPixel) {
    if (haveLeftRightArrows) {
      int dx = (isArrowClick(xPixel, yPixel, ARROW_LEFT) ? -1
          : isArrowClick(xPixel, yPixel, ARROW_RIGHT) ? 1 : 0);
      if (dx != 0) {
        setSpectrumClicked((iSpectrumSelected + dx) % nSpectra);
        return true;
      }
      if (isArrowClick(xPixel, yPixel, ARROW_HOME)) {
        if (showAllStacked) {
          showAllStacked = false;
          setSpectrumClicked(getFixedSelectedSpectrumIndex());
          return true;
        }
        showAllStacked = allowStacking;
        setSpectrumSelected(-1);
        stackSelected = false;
      }
    }
    return false;
  }

  private boolean isArrowClick(int xPixel, int yPixel, int type) {
    int pt;
    switch (type) {
    case ARROW_UP:
    case ARROW_DOWN:
    case ARROW_RESET:
      pt = (yPixel00 + yPixel11) / 2
          + (type == ARROW_UP ? -1 : type == ARROW_DOWN ? 1 : 0) * 15;
      return (Math.abs(xVArrows - xPixel) < 10 && Math.abs(pt - yPixel) < 10);
    case ARROW_LEFT:
    case ARROW_RIGHT:
    case ARROW_HOME:
      pt = xHArrows
          + (type == ARROW_LEFT ? -1 : type == ARROW_RIGHT ? 1 : 0) * 15;
      return (Math.abs(pt - xPixel) < 10 && Math.abs(yHArrows - yPixel) < 10);
    }
    return false;
  }

  private static final int MIN_DRAG_PIXELS = 5;// fewer than this means no zoom
  // or reset

  private boolean inPlotMove;
  private int xPixelMovedTo = -1;
  private int xPixelMovedTo2 = -1;
  private double yValueMovedTo;
  private double xValueMovedTo;
  private boolean haveLeftRightArrows;
  private int xPixelPlot1;
  private int xPixelPlot0;
  private int yPixelPlot0;
  private int yPixelPlot1;
  private Double nextClickForSetPeak;
  private int closerX, closerY, splitterX, splitterY;
  private boolean isPrintingOrSaving;

  private void setWidgetValueByUser(PlotWidget pw) {
    String sval;
    if (pw == cur2Dy)
      sval = "" + imageView.toSubspectrumIndex(pw.yPixel0);
    else if (pw == pin1Dx01)
      sval = "" + Math.min(pin1Dx0.getXVal(), pin1Dx1.getXVal()) + " - "
          + Math.max(pin1Dx0.getXVal(), pin1Dx1.getXVal());
    else if (pw == pin1Dy01)
      sval = "" + Math.min(pin1Dy0.getYVal(), pin1Dy1.getYVal()) + " - "
          + Math.max(pin1Dy0.getYVal(), pin1Dy1.getYVal());
    else if (pw == pin2Dx01)
      sval = "" + Math.min(pin2Dx0.getXVal(), pin2Dx1.getXVal()) + " - "
          + Math.max(pin2Dx0.getXVal(), pin2Dx1.getXVal());
    else if (pw == pin2Dy01)
      sval = "" + (int) Math.min(pin2Dy0.getYVal(), pin2Dy1.getYVal()) + " - "
          + (int) Math.max(pin2Dy0.getYVal(), pin2Dy1.getYVal());
    else
      sval = "" + pw.getValue();
    sval = pd.getInput("New value?", "Set Slider", sval);
    if (sval == null)
      return;
    sval = sval.trim();
    try {
      if (pw == pin1Dx01 || pw == pin1Dy01 || pw == pin2Dx01
          || pw == pin2Dy01) {
        int pt = sval.indexOf("-", 1);
        if (pt < 0)
          return;
        double val1 = Double.valueOf(sval.substring(0, pt)).doubleValue();
        double val2 = Double.valueOf(sval.substring(pt + 1)).doubleValue();
        if (pw == pin1Dx01) {
          doZoom(val1, pin1Dy0.getYVal(), val2, pin1Dy1.getYVal(), true, false,
              false, true, true);
        } else if (pw == pin1Dy01) { // also for 2D Z-range zoom
          doZoom(pin1Dx0.getXVal(), val1, pin1Dx1.getXVal(), val2,
              imageView == null, imageView == null, false, false, true);
        } else if (pw == pin2Dx01) {
          imageView.setView0(imageView.toPixelX0(val1), pin2Dy0.yPixel0,
              imageView.toPixelX0(val2), pin2Dy1.yPixel0);
          doZoom(val1, pin1Dy0.getYVal(), val2, pin1Dy1.getYVal(), false, false,
              false, true, true);
        } else if (pw == pin2Dy01) {
          imageView.setView0(pin2Dx0.xPixel0, imageView.toPixelY0(val1),
              pin2Dx1.xPixel0, imageView.toPixelY0(val2));
          doZoom(imageView.toX(imageView.xPixel0), getScale().minY,
              imageView.toX(imageView.xPixel0 + imageView.xPixels - 1),
              getScale().maxY, false, false, false, false, true);
        }
      } else {
        double val = Double.valueOf(sval).doubleValue();
        if (pw.isXtype) {
          double val2 = (pw == pin1Dx0 || pw == cur2Dx0 || pw == pin2Dx0
              ? pin1Dx1.getXVal()
              : pin1Dx0.getXVal());
          // 
          doZoom(val, 0, val2, 0, !pw.is2D, false, false, true, true);
        } else if (pw == cur2Dy) {
          setCurrentSubSpectrum((int) val);
        } else if (pw == pin2Dy0 || pw == pin2Dy1) {
          int val2 = (pw == pin2Dy0 ? pin2Dy1.yPixel0 : pin2Dy0.yPixel0);
          imageView.setView0(pin2Dx0.xPixel0,
              imageView.subIndexToPixelY((int) val), pin2Dx1.xPixel0, val2);
        } else {
          // 1D y-zoom
          double val2 = (pw == pin1Dy0 ? pin1Dy1.getYVal() : pin1Dy0.getYVal());
          doZoom(pin1Dx0.getXVal(), val, pin1Dx1.getXVal(), val2,
              imageView == null, imageView == null, false, false, true);
        }
      }
    } catch (Exception e) {
    }
  }

  private void removeAllHighlights(Spectrum spec) {
    if (spec == null)
      highlights.clear();
    else
      for (int i = highlights.size(); --i >= 0;)
        if (highlights.get(i).spectrum == spec)
          highlights.removeItemAt(i);
  }

  private Coordinate setCoordClicked(int xPixel, double x, double y) {
    if (y == 0)
      nextClickForSetPeak = null;
    if (Double.isNaN(x)) {
      pd.coordClicked = null;
      pd.coordsClicked = null;
      return null;
    }
    pd.coordClicked = new Coordinate().set(lastClickX = x, y);
    pd.coordsClicked = getSpectrum().getXYCoords();
    pd.xPixelClicked = (lastPixelX = xPixel);
    return pd.coordClicked;
  }

  /**
   * PlotWidgets are zoom boxes and slider points that are draggable. Some are
   * derived from others (center points and the 2D subIndex pointer). The first
   * time through, we have to create new pins. When the frame is resized, we
   * need to reset their positions along the slider based on their values, and
   * we need to also move the sliders to the right place.
   * 
   * @param needNewPins
   * @param subIndex
   * @param doDraw1DObjects
   */
  private void setWidgets(boolean needNewPins, int subIndex,
                          boolean doDraw1DObjects) {
    if (needNewPins || pin1Dx0 == null) {
      if (zoomBox1D == null)
        newPins();
      else
        resetPinPositions();
    }
    setDerivedPins(subIndex);
    setPinSliderPositions(doDraw1DObjects);
  }

  /**
   * Create new pins and set their default values. Note that we are making a
   * distinction between view.minY and view.minYOnScale. For X these are now the
   * same, but for Y they are not. This produces a nicer grid, but also an odd
   * jumpiness in the Y slider that is not totally predictable.
   * 
   */
  private void newPins() {
    zoomBox1D = new PlotWidget("zoomBox1D");
    pin1Dx0 = new PlotWidget("pin1Dx0");
    pin1Dx1 = new PlotWidget("pin1Dx1");
    pin1Dy0 = new PlotWidget("pin1Dy0");
    pin1Dy1 = new PlotWidget("pin1Dy1");
    pin1Dx01 = new PlotWidget("pin1Dx01");
    pin1Dy01 = new PlotWidget("pin1Dy01");
    cur1D2x1 = new PlotWidget("cur1D2x1");
    cur1D2x1.color = ScriptToken.PEAKTABCOLOR;
    cur1D2x2 = new PlotWidget("cur1D2x2");
    cur1D2x2.color = ScriptToken.PEAKTABCOLOR;
    if (imageView != null) {
      zoomBox2D = new PlotWidget("zoomBox2D");
      // these pins only present when no 1D is present
      pin2Dx0 = new PlotWidget("pin2Dx0");
      pin2Dx1 = new PlotWidget("pin2Dx1");
      pin2Dy0 = new PlotWidget("pin2Dy0");
      pin2Dy1 = new PlotWidget("pin2Dy1");
      pin2Dx01 = new PlotWidget("pin2Dx01");
      pin2Dy01 = new PlotWidget("pin2Dy01");
      // these pins only present when 1D and 2D
      cur2Dx0 = new PlotWidget("cur2Dx0");
      // these pins only present whenever 2D present
      cur2Dx1 = new PlotWidget("cur2Dx1");
      cur2Dy = new PlotWidget("cur2Dy");
      pin2Dy0.setY(0, imageView.toPixelY0(0));
      int n = getSpectrumAt(0).getSubSpectra().size();
      pin2Dy1.setY(n, imageView.toPixelY0(n));
    }
    setWidgetX(pin1Dx0, getScale().minX);
    setWidgetX(pin1Dx1, getScale().maxX);
    setWidgetY(pin1Dy0, getScale().minY);
    setWidgetY(pin1Dy1, getScale().maxY);

    widgets = new PlotWidget[] { zoomBox1D, zoomBox2D, pin1Dx0, pin1Dx01,
        pin1Dx1, pin1Dy0, pin1Dy01, pin1Dy1, pin2Dx0, pin2Dx01, pin2Dx1,
        pin2Dy0, pin2Dy01, pin2Dy1, cur2Dx0, cur2Dx1, cur2Dy, cur1D2x1,
        cur1D2x2 };
  }

  private void setWidgetX(PlotWidget pw, double x) {
    pw.setX(x, toPixelX0(x));
  }

  private void setWidgetY(PlotWidget pw, double y) {
    pw.setY(y, toPixelY0(y));
  }

  private void resetPinsFromView() {
    if (pin1Dx0 == null)
      return;
    setWidgetX(pin1Dx0, getScale().minXOnScale);
    setWidgetX(pin1Dx1, getScale().maxXOnScale);
    setWidgetY(pin1Dy0, getScale().minYOnScale);
    setWidgetY(pin1Dy1, getScale().maxYOnScale);
  }

  /**
   * use the pin values to find their positions along the slider
   * 
   */
  private void resetPinPositions() {
    resetX(pin1Dx0);
    resetY(pin1Dy0);
    resetY(pin1Dy1);
    if (imageView == null) {
      if (gs2dLinkedX != null)
        resetX(cur1D2x1);
      if (gs2dLinkedY != null)
        resetX(cur1D2x2);
    } else {
      pin2Dy0.setY(pin2Dy0.getYVal(), imageView.toPixelY0(pin2Dy0.getYVal()));
      pin2Dy1.setY(pin2Dy1.getYVal(), imageView.toPixelY0(pin2Dy1.getYVal()));
    }
  }

  private void resetX(PlotWidget p) {
    setWidgetX(p, p.getXVal());
  }

  private void resetY(PlotWidget p) {
    setWidgetY(p, p.getYVal());
  }

  /**
   * realign sliders to proper locations after resizing
   * 
   * @param doDraw1DObjects
   * 
   */
  private void setPinSliderPositions(boolean doDraw1DObjects) {
    pin1Dx0.yPixel0 = pin1Dx1.yPixel0 = pin1Dx01.yPixel0 = yPixel0 - 5;
    pin1Dx0.yPixel1 = pin1Dx1.yPixel1 = pin1Dx01.yPixel1 = yPixel0;
    cur1D2x1.yPixel1 = cur1D2x2.yPixel1 = yPixel0 - 5;
    cur1D2x1.yPixel0 = cur1D2x2.yPixel0 = yPixel1 + 6;
    if (imageView == null) {
      pin1Dy0.xPixel0 = pin1Dy1.xPixel0 = pin1Dy01.xPixel0 = xPixel1 + 5;
      pin1Dy0.xPixel1 = pin1Dy1.xPixel1 = pin1Dy01.xPixel1 = xPixel1;
    } else {
      pin1Dy0.xPixel0 = pin1Dy1.xPixel0 = pin1Dy01.xPixel0 = imageView.xPixel1
          + 15;
      pin1Dy0.xPixel1 = pin1Dy1.xPixel1 = pin1Dy01.xPixel1 = imageView.xPixel1
          + 10;
      pin2Dx0.yPixel0 = pin2Dx1.yPixel0 = pin2Dx01.yPixel0 = yPixel0 - 5;
      pin2Dx0.yPixel1 = pin2Dx1.yPixel1 = pin2Dx01.yPixel1 = yPixel0;
      pin2Dy0.xPixel0 = pin2Dy1.xPixel0 = pin2Dy01.xPixel0 = imageView.xPixel1
          + 5;
      pin2Dy0.xPixel1 = pin2Dy1.xPixel1 = pin2Dy01.xPixel1 = imageView.xPixel1;
      cur2Dx0.yPixel0 = cur2Dx1.yPixel0 = yPixel1 + 6;
      cur2Dx0.yPixel1 = cur2Dx1.yPixel1 = yPixel0 - 5;
      cur2Dx0.yPixel0 = cur2Dx1.yPixel0 = yPixel1 + 6;
      cur2Dx1.yPixel1 = cur2Dx1.yPixel1 = yPixel0 - 5;
      cur2Dy.xPixel0 = (doDraw1DObjects ? (xPixel1 + imageView.xPixel0) / 2
          : imageView.xPixel0 - 6);
      cur2Dy.xPixel1 = imageView.xPixel1 + 5;
    }
  }

  /**
   * The center pins and the 2D subspectrum slider values are derived from other
   * data
   * 
   * @param subIndex
   */
  private void setDerivedPins(int subIndex) {
    //??? TODO CHECK was not in Jmol-SwingJS widgetsAreSet = true;
    if (gs2dLinkedX != null)
      cur1D2x1.setX(cur1D2x1.getXVal(), toPixelX(cur1D2x1.getXVal()));
    if (gs2dLinkedY != null)
      cur1D2x2.setX(cur1D2x2.getXVal(), toPixelX(cur1D2x2.getXVal()));

    pin1Dx01.setX(0, (pin1Dx0.xPixel0 + pin1Dx1.xPixel0) / 2);
    pin1Dy01.setY(0, (pin1Dy0.yPixel0 + pin1Dy1.yPixel0) / 2);
    pin1Dx01.setEnabled(Math.min(pin1Dx0.xPixel0, pin1Dx1.xPixel0) > xPixel0
        || Math.max(pin1Dx0.xPixel0, pin1Dx1.xPixel0) < xPixel1);
    // note that toPixelY uses userYFactor, which is spectrum-dependent.
    // in a stacked set, this will be wrong. Perhaps no showing this pin1Dy01
    // then?
    pin1Dy01.setEnabled(Math.min(pin1Dy0.yPixel0, pin1Dy1.yPixel0) > Math
        .min(toPixelY(getScale().minY), toPixelY(getScale().maxY))
        || Math.max(pin1Dy0.yPixel0, pin1Dy1.yPixel0) < Math
            .max(toPixelY(getScale().minY), toPixelY(getScale().maxY)));
    if (imageView == null)
      return;
    double x = pin1Dx0.getXVal();
    cur2Dx0.setX(x, imageView.toPixelX(x));
    x = pin1Dx1.getXVal();
    cur2Dx1.setX(x, imageView.toPixelX(x));

    x = imageView.toX(imageView.xPixel0);
    pin2Dx0.setX(x, imageView.toPixelX0(x));
    x = imageView.toX(imageView.xPixel1);
    pin2Dx1.setX(x, imageView.toPixelX0(x));
    pin2Dx01.setX(0, (pin2Dx0.xPixel0 + pin2Dx1.xPixel0) / 2);

    double y = imageView.imageHeight - 1 - imageView.yView1;
    pin2Dy0.setY(y, imageView.toPixelY0(y));
    y = imageView.imageHeight - 1 - imageView.yView2;
    pin2Dy1.setY(y, imageView.toPixelY0(y));
    pin2Dy01.setY(0, (pin2Dy0.yPixel0 + pin2Dy1.yPixel0) / 2);

    cur2Dy.yPixel0 = cur2Dy.yPixel1 = imageView.subIndexToPixelY(subIndex);

    pin2Dx01.setEnabled(
        Math.min(pin2Dx0.xPixel0, pin2Dx1.xPixel0) != imageView.xPixel0
            || Math.max(pin2Dx0.xPixel0, pin2Dx1.xPixel1) != imageView.xPixel1);
    pin2Dy01.setEnabled(Math.min(pin2Dy0.yPixel0, pin2Dy1.yPixel0) != yPixel0
        || Math.max(pin2Dy0.yPixel0, pin2Dy1.yPixel1) != yPixel1);
  }

  /**
   * Zooms the spectrum between two coordinates
   * 
   * @param initX
   *        the X start coordinate of the zoom area
   * @param initY
   *        the Y start coordinate of the zoom area
   * @param finalX
   *        the X end coordinate of the zoom area
   * @param finalY
   *        the Y end coordinate of the zoom area
   * @param is1D
   *        TODO
   * @param is1DY
   *        TODO
   * @param checkRange
   * @param checkLinked
   *        TODO
   * @param addZoom
   */
  synchronized void doZoom(double initX, double initY, double finalX,
                           double finalY, boolean is1D, boolean is1DY,
                           boolean checkRange, boolean checkLinked,
                           boolean addZoom) {
    if (initX == finalX) {
      initX = getScale().minXOnScale;
      finalX = getScale().maxXOnScale;
    } else if (isLinked && checkLinked)
      pd.doZoomLinked(this, initX, finalX, addZoom, checkRange, is1D);
    if (initX > finalX) {
      double tempX = initX;
      initX = finalX;
      finalX = tempX;
    }
    if (initY > finalY) {
      double tempY = initY;
      initY = finalY;
      finalY = tempY;
    }

    boolean is2DGrayScaleChange = (!is1D && imageView != null
        && (imageView.minZ != initY || imageView.maxZ != finalY));

    if (!zoomEnabled && !is2DGrayScaleChange)
      return;

    // determine if the range of the area selected for zooming is within the
    // plot
    // area and if not ensure that it is

    if (checkRange) {
      if (!getScale().isInRangeX(initX) && !getScale().isInRangeX(finalX))
        return;
      if (!getScale().isInRangeX(initX)) {
        initX = getScale().minX;
      } else if (!getScale().isInRangeX(finalX)) {
        finalX = getScale().maxX;
      }
    } else {
      //viewData = viewList.get(0);
    }
    pd.setTaintedAll();
    ScaleData[] viewScales = viewData.getScaleData();
    int[] startIndices = new int[nSpectra];
    int[] endIndices = new int[nSpectra];
    graphsTemp.clear();
    Lst<Spectrum> subspecs = getSpectrumAt(0).getSubSpectra();
    boolean dontUseSubspecs = (subspecs == null || subspecs.size() == 2);
    // NMR real/imaginary
    boolean is2D = !getSpectrumAt(0).is1D();
    if (!is2D && !dontUseSubspecs) {
      graphsTemp.addLast(getSpectrum());
      if (!ScaleData.setDataPointIndices(graphsTemp, initX, finalX,
          minNumOfPointsForZoom, startIndices, endIndices))
        return;
    } else {
      if (!ScaleData.setDataPointIndices(spectra, initX, finalX,
          minNumOfPointsForZoom, startIndices, endIndices))
        return;
    }
    double y1 = initY;
    double y2 = finalY;
    boolean isXOnly = (y1 == y2);
    if (isXOnly) {
      double f = (!is2DGrayScaleChange && is1D
          ? f = getScale().spectrumScaleFactor
          : 1);
      if (Math.abs(f - 1) < 0.0001) {
        y1 = getScale().minYOnScale;
        y2 = getScale().maxYOnScale;
      }
    }
    ScaleData[] yScales = null;
    if (isXOnly || is1DY) {
      getCurrentView();
      yScales = viewData.getNewScales(iSpectrumSelected, isXOnly, y1, y2);
    }
    getView(initX, finalX, y1, y2, startIndices, endIndices, viewScales,
        yScales);
    setXPixelMovedTo(1E10, Double.MAX_VALUE, 0, 0);
    setWidgetX(pin1Dx0, initX);
    setWidgetX(pin1Dx1, finalX);
    setWidgetY(pin1Dy0, y1);
    setWidgetY(pin1Dy1, y2);
    if (imageView == null) {
      updateDialogs();
    } else {
      int isub = getSpectrumAt(0).getSubIndex();
      int ifix = imageView.fixSubIndex(isub);
      if (ifix != isub)
        setCurrentSubSpectrum(ifix);
      if (is2DGrayScaleChange)
        update2dImage(false);
    }
    if (addZoom)
      addCurrentZoom();
    // if (doRepaint)
    // pd.repaint();
  }

  private void updateDialogs() {
    updateDialog(AType.PeakList, -1);
    updateDialog(AType.Measurements, -1);
  }

  private void setCurrentSubSpectrum(int i) {
    Spectrum spec0 = getSpectrumAt(0);
    i = spec0.setCurrentSubSpectrum(i);
    if (spec0.isForcedSubset())
      viewData.setXRangeForSubSpectrum(getSpectrum().getXYCoords());
    pd.notifySubSpectrumChange(i, getSpectrum());
  }

  private void addCurrentZoom() {
    // add to and clean the zoom list
    if (viewList.size() > currentZoomIndex + 1)
      for (int i = viewList.size() - 1; i > currentZoomIndex; i--)
        viewList.removeItemAt(i);
    viewList.addLast(viewData);
    currentZoomIndex++;
  }

  private void setZoomTo(int i) {
    //imageView = null;
    currentZoomIndex = i;
    viewData = viewList.get(i);
    resetPinsFromView();
  }

  /**
   * Clears all views in the zoom list
   */
  void clearViews() {
    if (isLinked) {
      pd.clearLinkViews(this);
    }
    setZoom(0, 0, 0, 0);
    // leave first zoom
    for (int i = viewList.size(); --i >= 1;)
      viewList.removeItemAt(i);
  }

  /**
   * Principal drawing method
   * 
   * @param gMain
   * @param gFront
   * @param gBack
   * @param iSplit
   * @param needNewPins
   * @param doAll
   * @param pointsOnly
   */
  private void drawAll(Object gMain, Object gFront, Object gBack, int iSplit,
                       boolean needNewPins, boolean doAll, boolean pointsOnly) {
    setPositionForFrame(iSplit);
    g2d = pd.g2d; // may change when printing and testing JsPdfCreator
    this.gMain = gMain;
    Spectrum spec0 = getSpectrumAt(0);
    int subIndex = spec0.getSubIndex();
    is2DSpectrum = (!spec0.is1D()
        && (isLinked || pd.getBoolean(ScriptToken.DISPLAY2D))
        && (imageView != null || get2DImage(spec0)));
    if (imageView != null && doAll) {
      if (isPrintingOrSaving)
        g2d.newGrayScaleImage(gMain, image2D, imageView.imageWidth,
            imageView.imageHeight, imageView.getBuffer());
      if (is2DSpectrum)
        setPositionForFrame(iSplit);
      draw2DImage();
    }
    int iSelected = (stackSelected || !showAllStacked ? iSpectrumSelected : -1);
    boolean doYScale = (!showAllStacked || nSpectra == 1 || iSelected >= 0);
    boolean doDraw1DObjects = (imageView == null || pd.display1D);
    int n = (iSelected >= 0 ? 1 : 0);
    int iSpectrumForScale = getFixedSelectedSpectrumIndex();
    if (doDraw1DObjects && doAll) {
      fillBox(gMain, xPixel0, yPixel0, xPixel1, yPixel1,
          ScriptToken.PLOTAREACOLOR);
      if (iSelected < 0) {
        doYScale = true;
        for (int i = 0; i < nSpectra; i++)
          if (doPlot(i, iSplit)) {
            if (n++ == 0)
              continue;
            doYScale &= viewData.areYScalesSame(i - 1, i);
          }
      }
    }
    int iSpecForFrame = (nSpectra == 1 ? 0
        : !showAllStacked ? iSpectrumMovedTo : iSpectrumSelected);
    Object g2 = (gBack == gMain ? gFront : gBack);
    if (doAll) {
      boolean addCurrentBox = (pd.getCurrentGraphSet() == this && !isLinked // not if this is linked
          && (!isSplittable
              || (nSplit == 1 || pd.currentSplitPoint == iSplit)));
      boolean drawUpDownArrows = (zoomEnabled // must have zoom enabled
          && !isPrintingOrSaving && !isDrawNoSpectra() // must be drawing spectrum
          && pd.isCurrentGraphSet(this) // must be current
          && spectra.get(0).isScalable() // must be scalable
          && (addCurrentBox || nSpectra == 1) // must have a box or be just one
                                              // spectrum
          && (nSplit == 1 || pd.currentSplitPoint == iSpectrumMovedTo) // must have
                                                                       // one panel
                                                                       // or be the
                                                                       // spectrum
                                                                       // moved to
      );
      boolean addSplitBox = isSplittable;
      drawFrame(gMain, iSpecForFrame, addCurrentBox, addSplitBox,
          drawUpDownArrows);
    }
    if (pd.isCurrentGraphSet(this) // is current set
        && iSplit == pd.currentSplitPoint && (n < 2 // just one spectrum to show
            || iSpectrumSelected >= 0 // stacked and selected
        ))
      haveSelectedSpectrum = true;
    haveSingleYScale = (showAllStacked && nSpectra > 1
        ? allowStackedYScale && doYScale
        : true);
    if (doDraw1DObjects) {
      int yOffsetPixels = (int) (yPixels * (yStackOffsetPercent / 100f));
      haveLeftRightArrows = false;
      for (int i = 0, offset = 0; i < nSpectra; i++) {
        if (!doPlot(i, iSplit))
          continue;
        Spectrum spec = spectra.get(i);
        boolean isContinuous = spec.isContinuous();
        boolean onSpectrum = (iSpectrumMovedTo >= 0
            && ((nSplit > 1 ? i == iSpectrumMovedTo
                : isLinked || i == iSpectrumForScale) && !pd.isPrinting
                && isContinuous));
        boolean isGrey = (stackSelected && iSpectrumSelected >= 0
            && iSpectrumSelected != i);
        IntegralData ig = (!reversePlot
            && getShowAnnotation(AType.Integration, i)
            && (!showAllStacked || iSpectrumSelected == i)
                ? (IntegralData) getDialog(AType.Integration, i).getData()
                : null);
        setScale(i);
        if (nSplit > 1) {
          iSpectrumForScale = i;
        }
        boolean doDrawWidgets = !isGrey
            && (nSplit == 1 || showAllStacked || iSpectrumSelected == iSplit);
        boolean doDraw1DY = (doDrawWidgets && haveSelectedSpectrum
            && i == iSpectrumForScale);
        if (doDrawWidgets) {
          widgetsAreSet = true;
          resetPinsFromView();
          drawWidgets(gFront, g2, subIndex, needNewPins, doDraw1DObjects,
              doDraw1DY, false);
        }
        if (haveSingleYScale && i == iSpectrumForScale && doAll) {
          drawGrid(gMain);
          if (pd.isPrinting && nSplit == 1 && pd.graphSets.size() == 1)
            drawSpectrumSource(gMain, i);
        }
        if (doDrawWidgets)
          drawWidgets(gFront, g2, subIndex, false, doDraw1DObjects, doDraw1DY,
              true);
        if (!isDrawNoSpectra() && !isPrintingOrSaving
            && (nSpectra == 1 || iSpectrumSelected >= 0)
            && (haveSingleYScale && i == iSpectrumForScale
                || showAllStacked && stackSelected && i == iSpectrumSelected))
          drawHighlightsAndPeakTabs(gFront, g2, i);
        if (doAll) {
          if ((onSpectrum || isPrintingOrSaving) && !is2DSpectrum) {
            if (pd.titleOn && !pd.titleDrawn) {
              //String title = null;//(pd.isPrinting ? pd.getDrawTitle(pd.isPrinting, -1) : null);
              //if (title == null || title.length() == 0)
              String title = getSpectrumAt(i).getPeakTitle();
              if (title.length() > 0) {
                int y = (isPrintingOrSaving ? yPixel11 + 20 : height);
                pd.drawTitle(gMain, y, width, title);
                pd.titleDrawn = !isPrintingOrSaving;
              }
            }
          }
          if (haveSingleYScale && i == iSpectrumForScale) {
            if (pd.getBoolean(ScriptToken.YSCALEON))
              drawYScale(gMain, this);
          }
          if (haveSingleYScale && yPixel00 < 30) {
            if (pd.getBoolean(ScriptToken.YUNITSON))
              drawYUnits(gMain);
          }
        }
        boolean hasPendingIntegral = (!isGrey && pendingIntegral != null
            && spec == pendingIntegral.spec);
        if (doAll || hasPendingIntegral) {
          drawPlot(hasPendingIntegral && !doAll ? gFront : gMain, i, spec,
              isContinuous, offset, isGrey, null, onSpectrum,
              hasPendingIntegral, pointsOnly);
        }
        drawIntegration(gFront, i, offset, isGrey, ig, isContinuous,
            onSpectrum);
        drawMeasurements(gFront, i);
        if (pendingMeasurement != null && pendingMeasurement.spec == spec)
          drawMeasurement(gFront, pendingMeasurement);
        if (onSpectrum && xPixelMovedTo >= 0) {
          drawSpectrumPointer(gFront, spec, offset, ig);
        }
        if (nSpectra > 1 && nSplit == 1 && pd.isCurrentGraphSet(this)
            && doAll) {
          haveLeftRightArrows = true;
          if (!isPrintingOrSaving) {
            setScale(0);
            iSpecForFrame = (iSpectrumSelected);
            if (nSpectra != 2) {
              setPlotColor(gMain, (iSpecForFrame + nSpectra - 1) % nSpectra);
              fillArrow(gMain, ARROW_LEFT, yHArrows, xHArrows - 9, true);
              setCurrentBoxColor(gMain);
              fillArrow(gMain, ARROW_LEFT, yHArrows, xHArrows - 9, false);
            }
            if (iSpecForFrame >= 0) {
              setPlotColor(gMain, iSpecForFrame);
              fillCircle(gMain, xHArrows, yHArrows, true);
            }
            setCurrentBoxColor(gMain);
            fillCircle(gMain, xHArrows, yHArrows, false);
            setPlotColor(gMain, (iSpecForFrame + 1) % nSpectra);
            fillArrow(gMain, ARROW_RIGHT, yHArrows, xHArrows + 9, true);
            setCurrentBoxColor(gMain);
            fillArrow(gMain, ARROW_RIGHT, yHArrows, xHArrows + 9, false);
          }
        }
        offset -= yOffsetPixels;
      }
      if (doAll) {
        if (pd.getBoolean(ScriptToken.XSCALEON))
          drawXScale(gMain, this);
        if (pd.getBoolean(ScriptToken.XUNITSON))
          drawXUnits(gMain);
      }
    } else {
      if (doAll) {
        if (pd.getBoolean(ScriptToken.XSCALEON))
          drawXScale(gMain, imageView);
        if (pd.getBoolean(ScriptToken.YSCALEON))
          drawYScale(gMain, imageView);
        if (subIndex >= 0)
          draw2DUnits(gMain);
      }
      drawWidgets(gFront, g2, subIndex, needNewPins, doDraw1DObjects, true,
          false);
      // no 2D grid?
      drawWidgets(gFront, g2, subIndex, needNewPins, doDraw1DObjects, true,
          true);
      widgetsAreSet = true;
    }
    if (annotations != null)
      drawAnnotations(gFront, annotations, null);
    isPrintingOrSaving = false;
  }

  private void drawSpectrumSource(Object g, int i) {
    pd.printFilePath(g, pd.thisWidth - pd.right, yPixel0 - 20,
        spectra.get(i).getFilePath());
  }

  private boolean doPlot(int i, int iSplit) {
    boolean isGrey = (stackSelected && iSpectrumSelected >= 0
        && iSpectrumSelected != i);
    boolean ok = (showAllStacked || iSpectrumSelected == -1
        || iSpectrumSelected == i);
    return (nSplit > 1 ? i == iSplit : ok && (!pd.isPrinting || !isGrey));
  }

  //	private void hideAllDialogsExceptCurrent() {
  //		if (dialogs == null)
  //			return;
  //		boolean getInt = false;
  //		boolean getMeas = false;
  //		boolean getPeak = false;
  //		AnnotationData ad;
  //
  //		for (Map.Entry<String, AnnotationData> e : dialogs.entrySet()) {
  //			ad = e.getValue();
  //			if (isVisible(ad)) {
  //				// ((AnnotationDialog) ad).setVisible(false);
  //				switch (ad.getAType()) {
  //				case Integration:
  //					getInt = true;
  //					break;
  //				case Measurements:
  //					getMeas = true;
  //					break;
  //				case PeakList:
  //					getPeak = true;
  //					break;
  //				case NONE:
  //				}
  //			}
  //		}
  //		if (getInt)
  //			ad = jsvp.showDialog(AType.Integration);
  //		if (getMeas)
  //			ad = jsvp.showDialog(AType.Measurements);
  //		if (getPeak)
  //			ad = jsvp.showDialog(AType.PeakList);
  //
  //	}

  /**
   * 
   * This is the short red marker that runs along the X axis when the mouse is
   * moved
   * 
   * @param gFront
   * @param spec
   * @param yOffset
   * @param ig
   */
  private void drawSpectrumPointer(Object gFront, Spectrum spec, int yOffset,
                                   IntegralData ig) {
    if (isPrintingOrSaving)
      return;
    // short vertical cursor
    setColorFromToken(gFront, ScriptToken.PEAKTABCOLOR);
    int iHandle = pd.integralShiftMode;
    if (ig != null) {
      if ((!pd.ctrlPressed || pd.isIntegralDrag)
          && !isOnSpectrum(pd.mouseX, pd.mouseY, -1)) {
        ig = null;
      } else if (iHandle == 0) {
        iHandle = getShiftMode(pd.mouseX, pd.mouseY);
        if (iHandle == 0)
          iHandle = Integer.MAX_VALUE;
      }
    }
    double y0 = yValueMovedTo;
    yValueMovedTo = (ig == null ? spec.getYValueAt(xValueMovedTo)
        : ig.getPercentYValueAt(xValueMovedTo));
    setCoordStr(xValueMovedTo, yValueMovedTo);
    if (iHandle != 0) {
      setPlotColor(gFront, iHandle == Integer.MAX_VALUE ? -1 : 0);
      if (iHandle < 0 || iHandle == Integer.MAX_VALUE) {
        drawHandle(gFront, xPixelPlot1, yPixelPlot0, 3, false);
      }
      if (iHandle > 0) {
        drawHandle(gFront, xPixelPlot0, yPixelPlot1, 3, false);
      }
      if (iHandle != Integer.MAX_VALUE)
        return;
    }
    if (ig != null)
      g2d.setStrokeBold(gFront, true);

    if (Double.isNaN(y0) || pendingMeasurement != null) {
      g2d.drawLine(gFront, xPixelMovedTo, yPixel0, xPixelMovedTo, yPixel1);
      if (xPixelMovedTo2 >= 0)
        g2d.drawLine(gFront, xPixelMovedTo2, yPixel0, xPixelMovedTo2, yPixel1);
      yValueMovedTo = Double.NaN;
    } else {
      int y = (ig == null ? yOffset + toPixelY(yValueMovedTo)
          : toPixelYint(yValueMovedTo / 100));
      if (y == fixY(y))
        g2d.drawLine(gFront, xPixelMovedTo, y - 10, xPixelMovedTo, y + 10);
    }
    if (ig != null)
      g2d.setStrokeBold(gFront, false);
  }

  void setScale(int i) {
    viewData.setScale(i, xPixels, yPixels, spectra.get(i).isInverted());
  }

  private void draw2DUnits(Object g) {
    String nucleusX = getSpectrumAt(0).nucleusX;
    String nucleusY = getSpectrumAt(0).nucleusY;
    setColorFromToken(g, ScriptToken.PLOTCOLOR);
    drawUnits(g, nucleusX, imageView.xPixel1 + 5 * pd.scalingFactor, yPixel1, 1,
        1.0);
    drawUnits(g, nucleusY, imageView.xPixel0 - 5 * pd.scalingFactor, yPixel0, 1,
        0);
  }

  private void drawPeakTabs(Object gFront, Object g2, Spectrum spec) {
    if (isPrintingOrSaving)
      return;
    Lst<PeakInfo> list = (nSpectra == 1 || iSpectrumSelected >= 0
        ? spec.getPeakList()
        : null);
    if (list != null && list.size() > 0) {
      if (piMouseOver != null && piMouseOver.spectrum == spec
          && pd.isMouseUp()) {
        g2d.setGraphicsColor(g2, g2d.getColor4(240, 240, 240, 140)); // very faint gray box
        drawPeak(g2, piMouseOver, 0);
        spec.setHighlightedPeak(piMouseOver);
      } else {
        spec.setHighlightedPeak(null);
      }
      setColorFromToken(gFront, ScriptToken.PEAKTABCOLOR);
      for (int i = list.size(); --i >= 0;) {
        PeakInfo p = list.get(i);
        drawPeak(gFront, p, p == spec.getSelectedPeak() ? 14 : 7);
      }
    }
  }

  private void drawPeak(Object g, PeakInfo pi, int tickSize) {
    if (isPrintingOrSaving)
      return;
    double xMin = pi.getXMin();
    double xMax = pi.getXMax();
    if (xMin == xMax)
      return;
    drawBar(g, pi, xMin, xMax, null, tickSize);
  }

  /**
   * 
   * Draw sliders, pins, and zoom boxes (only one of which would ever be drawn)
   * 
   * @param gFront
   * @param gBack
   * @param subIndex
   * @param needNewPins
   * @param doDraw1DObjects
   * @param doDraw1DY
   *        TODO
   * @param postGrid
   */
  private void drawWidgets(Object gFront, Object gBack, int subIndex,
                           boolean needNewPins, boolean doDraw1DObjects,
                           boolean doDraw1DY, boolean postGrid) {
    setWidgets(needNewPins, subIndex, doDraw1DObjects);
    if (isPrintingOrSaving
        && (imageView == null ? !cur1D2Locked : sticky2Dcursor))
      return;
    // boolean allowPin1y = true;//(nSplit > 1 || iSpectrumSelected < 0 ||
    // nSpectra == 1 || nSplit == 1 && !stackSelected);
    if (!pd.isPrinting && !postGrid) {
      // top/side slider bar backgrounds
      if (doDraw1DObjects) {
        fillBox(gFront, xPixel0, pin1Dx0.yPixel1, xPixel1, pin1Dx1.yPixel1 + 2,
            ScriptToken.GRIDCOLOR);
        fillBox(gFront, pin1Dx0.xPixel0, pin1Dx0.yPixel1, pin1Dx1.xPixel0,
            pin1Dx1.yPixel1 + 2, ScriptToken.PLOTCOLOR);
      } else {

        fillBox(gFront, imageView.xPixel0, pin2Dx0.yPixel1, imageView.xPixel1,
            pin2Dx0.yPixel1 + 2, ScriptToken.GRIDCOLOR);
        fillBox(gFront, pin2Dx0.xPixel0, pin2Dx0.yPixel1, pin2Dx1.xPixel0,
            pin2Dx1.yPixel1 + 2, ScriptToken.PLOTCOLOR);
        fillBox(gFront, pin2Dy0.xPixel1, yPixel1, pin2Dy1.xPixel1 + 2, yPixel0,
            ScriptToken.GRIDCOLOR);
        fillBox(gFront, pin2Dy0.xPixel1, pin2Dy0.yPixel1, pin2Dy1.xPixel1 + 2,
            pin2Dy1.yPixel0, ScriptToken.PLOTCOLOR);
      }
      fillBox(gFront, pin1Dy0.xPixel1, yPixel1, pin1Dy1.xPixel1 + 2, yPixel0,
          ScriptToken.GRIDCOLOR);
      if (doDraw1DY)
        fillBox(gFront, pin1Dy0.xPixel1, pin1Dy0.yPixel1, pin1Dy1.xPixel1 + 2,
            pin1Dy1.yPixel0, ScriptToken.PLOTCOLOR);
    }
    for (int i = 0; i < widgets.length; i++) {
      PlotWidget pw = widgets[i];
      if (pw == null || !pw.isPinOrCursor && !zoomEnabled)
        continue;
      boolean isLockedCursor = (pw == cur1D2x1 || pw == cur1D2x2
          || pw == cur2Dx0 || pw == cur2Dx1 || pw == cur2Dy);
      if ((pw.isPin || !pw.isPinOrCursor) == postGrid)
        continue;
      if (pw.is2D) {
        if (pw == cur2Dx0 && !doDraw1DObjects)
          continue;
      } else {
        boolean isPin1Dy = (pw == pin1Dy0 || pw == pin1Dy1 || pw == pin1Dy01);
        if ((imageView != null && doDraw1DObjects == isPin1Dy)
            || isPin1Dy && !doDraw1DY || pw == cur1D2x1 && gs2dLinkedX == null
            || pw == cur1D2x2 && gs2dLinkedY == null || pw == zoomBox1D
                && (pd.isIntegralDrag || pd.integralShiftMode != 0)) {
          if (!isLinked || imageView != null)
            continue;
        }
      }
      if (pd.isPrinting && !isLockedCursor)
        continue;
      if (pw.isPinOrCursor) {
        setColorFromToken(gFront, pw.color);
        g2d.drawLine(gFront, pw.xPixel0, pw.yPixel0, pw.xPixel1, pw.yPixel1);
        pw.isVisible = true;
        if (pw.isPin)
          drawHandle(gFront, pw.xPixel0, pw.yPixel0, 2, !pw.isEnabled);
      } else if (pw.xPixel1 != pw.xPixel0) {

        fillBox(gBack, pw.xPixel0, pw.yPixel0, pw.xPixel1, pw.yPixel1,
            pw == zoomBox1D && pd.shiftPressed ? ScriptToken.ZOOMBOXCOLOR2
                : ScriptToken.ZOOMBOXCOLOR);
      }
    }
  }

  /**
   * draw a bar, but not necessarily full height
   * 
   * @param g
   * @param pi
   * @param xMin
   *        units
   * @param xMax
   *        units
   * @param whatColor
   * @param tickSize
   */

  private void drawBar(Object g, PeakInfo pi, double xMin, double xMax,
                       ScriptToken whatColor, int tickSize) {

    //		double r = xMax + xMin;
    //		double d = Math.abs(xMax - xMin);
    //		double range = Math.abs(toX(xPixel1) - toX(xPixel0));
    //		if (tickSize > 0 && d > range / 20) {
    //			d = range / 20;
    //			xMin = r / 2 - d/2;
    //			xMax = r / 2 + d/2;
    //		}

    int x1 = toPixelX(xMin);
    int x2 = toPixelX(xMax);
    if (x1 > x2) {
      int tmp = x1;
      x1 = x2;
      x2 = tmp;
    }
    // if either pixel is outside of plot area
    x1 = fixX(x1);
    x2 = fixX(x2);
    if (x2 - x1 < 3) {
      x1 -= 2;
      x2 += 2;
    }
    if (pi != null)
      pi.setPixelRange(x1, x2);
    if (tickSize == 0) {
      fillBox(g, x1, yPixel0, x2, yPixel0 + yPixels, whatColor);
    } else {
      fillBox(g, x1, yPixel0 + 2, x2, yPixel0 + 5, whatColor);
      if (pi != null) {
        x1 = (x1 + x2) / 2;
        fillBox(g, x1 - 1, yPixel0 + 2, x1 + 1, yPixel0 + 2 + tickSize,
            whatColor);
      }
    }

  }

  /**
   * Draws the plot on the Panel
   * 
   * @param gFront
   *        the <code>Graphics</code> object
   * @param index
   *        the index of the Spectrum to draw
   * @param yOffset
   * @param isGrey
   * @param iData
   * @param isContinuous
   * @param isSelected
   */
  private void drawIntegration(Object gFront, int index, int yOffset,
                               boolean isGrey, IntegralData iData,
                               boolean isContinuous, boolean isSelected) {
    // Check if specInfo in null or xyCoords is null
    if (iData != null) {
      if (haveIntegralDisplayed(index))
        drawPlot(gFront, index, spectra.get(index), true, yOffset, false, iData,
            true, false, false);
      drawIntegralValues(gFront, index, yOffset);
    }
    Lst<Annotation> ratios = getIntegrationRatios(index);
    if (ratios != null)
      drawAnnotations(gFront, ratios, ScriptToken.INTEGRALPLOTCOLOR);
  }

  private MeasurementData getMeasurements(AType type, int iSpec) {
    AnnotationData ad = getDialog(type, iSpec);
    return (ad == null || ad.getData().size() == 0 || !ad.getState() ? null
        : ad.getData());
  }

  private void drawPlot(Object g, int index, Spectrum spec,
                        boolean isContinuous, int yOffset, boolean isGrey,
                        IntegralData ig, boolean isSelected,
                        boolean hasPendingIntegral, boolean pointsOnly) {
    Coordinate[] xyCoords = (ig == null ? spec.getXYCoords()
        : getIntegrationGraph(index).getXYCoords());
    boolean isIntegral = (ig != null);
    BS bsDraw = (isIntegral ? ig.getBitSet() : null);
    boolean fillPeaks = (hasPendingIntegral
        || spec.fillColor != null && isSelected);
    int iColor = (isGrey ? COLOR_BLACK
        : isIntegral ? COLOR_INTEGRAL : !allowStacking ? 0 : index);
    setPlotColor(g, iColor);
    boolean plotOn = true;
    int y0 = toPixelY(0);
    if (isIntegral)
      fillPeaks &= (y0 == fixY(y0));
    else
      y0 = fixY(y0);
    GenericColor cInt = (isIntegral || fillPeaks
        ? pd.getColor(ScriptToken.INTEGRALPLOTCOLOR)
        : null);
    GenericColor cFill = (cInt == null || spec.fillColor == null ? cInt
        : spec.fillColor);
    int iFirst = viewData.getStartingPointIndex(index);
    int iLast = viewData.getEndingPointIndex(index);
    if (isContinuous && !pointsOnly) {
      iLast--;
      // all graphics can do line to for now
      boolean doLineTo = (isIntegral || pendingIntegral != null)
          && g2d.canDoLineTo();
      if (doLineTo)
        g2d.doStroke(g, true);
      boolean isDown = false;
      for (int i = iFirst; i <= iLast; i++) {
        Coordinate point1 = xyCoords[i];
        Coordinate point2 = xyCoords[i + 1];
        int y1 = (isIntegral ? toPixelYint(point1.getYVal())
            : toPixelY(point1.getYVal()));
        if (y1 == Integer.MIN_VALUE)
          continue;
        int y2 = (isIntegral ? toPixelYint(point2.getYVal())
            : toPixelY(point2.getYVal()));
        if (y2 == Integer.MIN_VALUE)
          continue;
        double xv1 = point1.getXVal();
        double xv2 = point2.getXVal();
        int x1 = toPixelX(xv1);
        int x2 = toPixelX(xv2);
        y1 = fixY(yOffset + y1);
        y2 = fixY(yOffset + y2);
        if (isIntegral) {
          if (i == iFirst) {
            xPixelPlot1 = x1;
            yPixelPlot0 = y1;
          }
          xPixelPlot0 = x2;
          yPixelPlot1 = y2;
        }
        if (x2 == x1 && y1 == y2)
          continue;
        if (fillPeaks && hasPendingIntegral
            && pendingIntegral.overlaps(xv1, xv2)) {
          if (cFill != null) {
            g2d.doStroke(g, false);
            g2d.setGraphicsColor(g, cFill);
          }
          g2d.fillRect(g, Math.min(x1, x2), Math.min(y0, y1),
              Math.max(1, Math.abs(x2 - x1)), Math.abs(y0 - y1));
          if (cFill != null) {
            g2d.doStroke(g, false);
            g2d.doStroke(g, true);
            isDown = false;
            setPlotColor(g, iColor);
          }
          continue;
        }
        if (y1 == y2 && (y1 == yPixel0)) {
          continue;
        }
        if (bsDraw != null && bsDraw.get(i) != plotOn) {
          plotOn = bsDraw.get(i);
          if (doLineTo && isDown) {
            g2d.doStroke(g, false);
            g2d.doStroke(g, true);
            isDown = false;
          }
          if (!pd.isPrinting && pd.integralShiftMode != 0)
            setPlotColor(g, 0);
          else if (plotOn)
            setColorFromToken(g, ScriptToken.INTEGRALPLOTCOLOR);
          else
            setPlotColor(g, COLOR_GREY);
        }
        if (pd.isPrinting && !plotOn)
          continue;
        if (isDown) {
          g2d.lineTo(g, x2, y2);
        } else {
          g2d.drawLine(g, x1, y1, x2, y2);
          isDown = doLineTo;
        }
      }
      if (doLineTo)
        g2d.doStroke(g, false);
    } else {
      for (int i = iFirst; i <= iLast; i++) {
        Coordinate point = xyCoords[i];
        int y2 = toPixelY(point.getYVal());
        if (y2 == Integer.MIN_VALUE)
          continue;
        int x1 = toPixelX(point.getXVal());
        int y1 = toPixelY(Math.max(getScale().minYOnScale, 0));
        y1 = fixY(yOffset + y1);
        y2 = fixY(yOffset + y2);
        if (y1 == y2 && (y1 == yPixel0 || y1 == yPixel1))
          continue;
        if (pointsOnly)
          g2d.fillRect(g, x1 - 1, y2 - 1, 3, 3);
        else
          g2d.drawLine(g, x1, y1, x1, y2);
      }
      if (!pointsOnly && getScale().isYZeroOnScale()) {
        int y = yOffset + toPixelY(getScale().spectrumYRef);
        if (y == fixY(y))
          g2d.drawLine(g, xPixel1, y, xPixel0, y);
      }
    }
  }

  /**
   * 
   * @param g
   * @param iSpec
   * @param addCurrentBox
   * @param addSplitBox
   * @param drawUpDownArrows
   */
  private void drawFrame(Object g, int iSpec, boolean addCurrentBox,
                         boolean addSplitBox, boolean drawUpDownArrows) {
    if (!pd.gridOn || isPrintingOrSaving) {
      setColorFromToken(g, ScriptToken.GRIDCOLOR);
      g2d.drawRect(g, xPixel0, yPixel0, xPixels, yPixels);
      if (isPrintingOrSaving)
        return;
    }
    setCurrentBoxColor(g);
    if (drawUpDownArrows) {
      if (iSpec >= 0) {
        setPlotColor(g, iSpec);
        fillArrow(g, ARROW_UP, xVArrows, (yPixel00 + yPixel11) / 2 - 9, true);
        fillArrow(g, ARROW_DOWN, xVArrows, (yPixel00 + yPixel11) / 2 + 9, true);
        setCurrentBoxColor(g);
      }
      fillArrow(g, ARROW_UP, xVArrows, (yPixel00 + yPixel11) / 2 - 9, false);
      fillCircle(g, xVArrows, (yPixel00 + yPixel11) / 2, false);
      fillArrow(g, ARROW_DOWN, xVArrows, (yPixel00 + yPixel11) / 2 + 9, false);
    }

    if (imageView != null)
      return;
    if (addCurrentBox) {
      int x1 = xPixel00 + 10;
      int x2 = xPixel11 - 10;
      int y1 = yPixel00 + 1;
      int y2 = yPixel11 - 2;
      g2d.drawLine(g, x1, y1, x2, y1);
      g2d.drawLine(g, x2, y1, x2, y2);
      g2d.drawLine(g, x1, y2, x2, y2);
      splitterX = closerX = Integer.MIN_VALUE;
      drawBox(g, x2 - 10, y1, x2, y1 + 10, null);
      g2d.drawLine(g, x2 - 10, y1 + 10, x2, y1);
      g2d.drawLine(g, x2, y1 + 10, x2 - 10, y1);
      closerX = x2 - 10;
      closerY = y1;
      if (addSplitBox) {
        x2 -= 10;
        fillBox(g, x2 - 10, y1, x2, y1 + 10, null);
        splitterX = x2 - 10;
        splitterY = y1;

      }

    }
  }

  /**
   * Draws the grid on the Panel
   * 
   * @param g
   *        the <code>Graphics</code> object
   */
  private void drawGrid(Object g) {
    if (!pd.gridOn || imageView != null)
      return;
    setColorFromToken(g, ScriptToken.GRIDCOLOR);
    double lastX;
    if (Double.isNaN(getScale().firstX)) {
      lastX = getScale().maxXOnScale + getScale().steps[0] / 2;
      for (double val = getScale().minXOnScale; val < lastX; val += getScale().steps[0]) {
        int x = toPixelX(val);
        g2d.drawLine(g, x, yPixel0, x, yPixel1);
      }
    } else {
      lastX = getScale().maxXOnScale * 1.0001;
      for (double val = getScale().firstX; val <= lastX; val += getScale().steps[0]) {
        int x = toPixelX(val);
        g2d.drawLine(g, x, yPixel0, x, yPixel1);
      }
    }
    for (double val = getScale().firstY; val < getScale().maxYOnScale
        + getScale().steps[1] / 2; val += getScale().steps[1]) {
      int y = toPixelY(val);
      if (y == fixY(y))
        g2d.drawLine(g, xPixel0, y, xPixel1, y);
    }
  }

  Map<Double, String> mapX = new Hashtable<Double, String>();

  /**
   * Draws the x Scale
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param c
   *        could be 'this' or imageView
   */
  private void drawXScale(Object g, XYScaleConverter c) {

    setColorFromToken(g, ScriptToken.SCALECOLOR);
    if (pd.isPrinting)
      g2d.drawLine(g, c.getXPixel0(), yPixel1,
          c.getXPixel0() + c.getXPixels() - 1, yPixel1);
    int precision = getScale().precision[0];
    Font font = pd.setFont(g, c.getXPixels(), FONT_PLAIN,
        pd.isPrinting ? 10 : 12, false);
    int y1 = yPixel1;
    int y2 = yPixel1 + 4 * pd.scalingFactor;
    int y3 = yPixel1 + 2 * pd.scalingFactor;

    int h = font.getHeight();
    double dx = c.toPixelX(getScale().steps[0]) - c.toPixelX(0);
    double maxWidth = Math.abs(dx * 0.95);
    // we go overboard for ticks
    double firstX = getScale().firstX - getScale().steps[0];
    double lastX = (getScale().maxXOnScale + getScale().steps[0]) * 1.0001;
    for (int pass = 0; pass < 2; pass++) {
      if (pass == 1)
        ScaleData.fixScale(mapX);
      double prevX = 1e10;
      for (double val = firstX; val <= lastX; val += getScale().steps[0]) {
        int x = c.toPixelX(val);
        Double d = Double.valueOf(val);
        String s;
        switch (pass) {
        case 0:
          s = DF.formatDecimalDbl(val, precision);
          mapX.put(d, s);
          drawTick(g, x, y1, y2, c);
          dx = Math.abs(prevX - val);
          int ntick = getScale().minorTickCounts[0];
          if (ntick != 0) {
            double step = dx / ntick;
            for (int i = 1; i < ntick; i++) {
              double x1 = val - i * step;
              drawTick(g, c.toPixelX(x1), y1, y3, c);
            }
          }
          prevX = val;
          continue;
        case 1:
          s = mapX.get(d);
          if (s == null || x != c.fixX(x))
            continue;
          int w = pd.getStringWidth(s);
          int n = (x + w / 2 == c.fixX(x + w / 2) ? 2 : 0);
          if (n > 0)
            g2d.drawString(g, s, x - w / n, y2 + h);
          val += Math.floor(w / maxWidth) * getScale().steps[0];
          break;
        }
      }
    }
    mapX.clear();
  }

  private void drawTick(Object g, int x, int y1, int y2, XYScaleConverter c) {
    if (x == c.fixX(x))
      g2d.drawLine(g, x, y1, x, y2);
  }

  /**
   * Draws the y Scale
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param c
   */
  private void drawYScale(Object g, XYScaleConverter c) {

    ScaleData sd = c.getScale();
    int precision = sd.precision[1];
    Font font = pd.setFont(g, c.getXPixels(), FONT_PLAIN,
        pd.isPrinting ? 10 : 12, false);
    int h = font.getHeight();
    double max = sd.maxYOnScale + sd.steps[1] / 2;
    int yLast = Integer.MIN_VALUE;
    setColorFromToken(g, ScriptToken.SCALECOLOR);
    for (int pass = 0; pass < 2; pass++) {
      if (pass == 1)
        ScaleData.fixScale(mapX);
      for (double val = sd.firstY; val < max; val += sd.steps[1]) {
        Double d = Double.valueOf(val);
        int x1 = c.getXPixel0();
        int y = c.toPixelY(val);
        if (y != c.fixY(y))
          continue;
        String s;
        if (pass == 0)
          g2d.drawLine(g, x1, y, x1 - 3 * pd.scalingFactor, y);
        if (Math.abs(y - yLast) <= h)
          continue;
        yLast = y;
        switch (pass) {
        case 0:
          s = DF.formatDecimalDbl(val, precision);
          mapX.put(d, s);
          break;
        case 1:
          s = mapX.get(d);
          if (s == null)
            continue;
          if (s.startsWith("0") && s.contains("E"))
            s = "0";
          g2d.drawString(g, s,
              (x1 - 4 * pd.scalingFactor - pd.getStringWidth(s)), y + h / 3);
          break;
        }
      }
    }
    mapX.clear();
  }

  /**
   * Draws the X Units
   * 
   * @param g
   *        the <code>Graphics</code> object
   */
  private void drawXUnits(Object g) {
    String units = spectra.get(0).getAxisLabel(true);
    if (units != null)
      drawUnits(g, units, xPixel1 + 25 * pd.scalingFactor,
          yPixel1 + 5 * pd.scalingFactor, 1, 1);
  }

  private void drawUnits(Object g, String s, int x, int y, double hOff,
                         double vOff) {
    setColorFromToken(g, ScriptToken.UNITSCOLOR);
    pd.setFont(g, (imageView == null ? this : imageView).getXPixels(),
        FONT_ITALIC | FONT_BOLD, 10, false);
    g2d.drawString(g, s, (int) (x - pd.getStringWidth(s) * hOff),
        (int) (y + pd.getFontHeight() * vOff));

  }

  /**
   * Draws the Y Units
   * 
   * @param g
   *        the <code>Graphics</code> object
   */
  private void drawYUnits(Object g) {
    String units = spectra.get(0).getAxisLabel(false);
    if (units == "")
      units = "ARBITRARY UNITS";
    if (units != null && !pd.unitsDrawn) {
      drawUnits(g, units, (pd.isPrinting ? 30 : 5) * pd.scalingFactor,
          yPixel0 + (pd.isPrinting ? 0 : 5) * pd.scalingFactor, 0, -1);
    }
    pd.unitsDrawn = true;
  }

  /**
   * 
   * @param gFront
   *        graphics for peak tabs
   * @param gBack
   *        graphics for highlight boxes
   * @param iSpec
   */
  private void drawHighlightsAndPeakTabs(Object gFront, Object gBack,
                                         int iSpec) {
    MeasurementData md = getMeasurements(AType.PeakList, iSpec);
    Spectrum spec = spectra.get(iSpec);
    if (isPrintingOrSaving) {
      if (md != null) {
        setColorFromToken(gFront, ScriptToken.PEAKTABCOLOR);
        printPeakList(gFront, spec, (PeakData) md);
      }
      return;
    }
    if (md == null) {
      for (int i = 0; i < highlights.size(); i++) {
        Highlight hl = highlights.get(i);
        if (hl.spectrum == spec) {
          pd.setHighlightColor(hl.color);
          drawBar(gBack, null, hl.x1, hl.x2, ScriptToken.HIGHLIGHTCOLOR, 0);
        }
      }
      if (pd.peakTabsOn)
        drawPeakTabs(gFront, gBack, spec);
    }
    int y;
    if (md != null) {
      y = (spec.isInverted() ? yPixel1 - 10 * pd.scalingFactor : yPixel0);
      setColorFromToken(gFront, ScriptToken.PEAKTABCOLOR);
      for (int i = md.size(); --i >= 0;) {
        Measurement m = md.get(i);
        int x = toPixelX(m.getXVal());
        g2d.drawLine(gFront, x, y, x, y + 10 * pd.scalingFactor);
      }
      if (isVisible(getDialog(AType.PeakList, iSpec))) {
        y = toPixelY(((PeakData) md).getThresh());
        if (y == fixY(y) && !pd.isPrinting)
          g2d.drawLine(gFront, xPixel0, y, xPixel1, y);
      }
    }
  }

  private void printPeakList(Object g, Spectrum spec, PeakData data) {
    String[][] sdata = data.getMeasurementListArray(null);
    if (sdata.length == 0)
      return;
    pd.setFont(g, xPixels, FONT_PLAIN, 8, false);
    int h = pd.getFontHeight();
    int[] xs = new int[data.size()];
    int[] xs0 = new int[data.size()];
    int dx = 0;
    int s5 = 5 * pd.scalingFactor;
    int s10 = 10 * pd.scalingFactor;
    int s15 = 15 * pd.scalingFactor;
    int s25 = 25 * pd.scalingFactor;
    for (int i = 0; i < sdata.length; i++) {
      xs0[i] = toPixelX(Double.parseDouble(sdata[i][1]));
      if (i == 0) {
        xs[i] = xs0[i];
        continue;
      }
      xs[i] = Math.max(xs[i - 1] + h, xs0[i] + h);
      dx += (xs[i] - xs0[i]);
    }
    dx /= 2 * sdata.length;
    if (xs[0] - dx < xPixel0 + s25)
      dx = xs[0] - (xPixel0 + s25);
    for (int i = 0; i < sdata.length; i++)
      xs[i] -= dx;

    boolean inverted = spec.isInverted();
    int y4 = pd.getStringWidth("99.9999");
    int y2 = (sdata[0].length >= 6 ? pd.getStringWidth("99.99") : 0);
    int f = (inverted ? -1 : 1);

    int y = (inverted ? yPixel1 : yPixel0) + f * (y2 + y4 + s15);
    for (int i = 0; i < sdata.length; i++) {
      g2d.drawLine(g, xs[i], y, xs[i], y + s5 * f);
      g2d.drawLine(g, xs[i], y + s5 * f, xs0[i], y + s10 * f);
      g2d.drawLine(g, xs0[i], y + s10 * f, xs0[i], y + s15 * f);
      if (y2 > 0 && sdata[i][4].length() > 0)
        g2d.drawLine(g, (xs[i] + xs[i - 1]) / 2, y - y4 + s5,
            (xs[i] + xs[i - 1]) / 2, y - y4 - s5);
    }

    y -= f * 2 * pd.scalingFactor;

    if (y2 > 0) {
      drawStringRotated(g, -90, xs[0] - s15, y, "  ppm");
      drawStringRotated(g, -90, xs[0] - s15, y - y4 - s5, " Hz");
    }
    for (int i = data.size(); --i >= 0;) {
      drawStringRotated(g, -90 * f, xs[i] + f * h / 3, y, sdata[i][1]);
      if (y2 > 0 && sdata[i][4].length() > 0) {
        int x = (xs[i] + xs[i - 1]) / 2 + h / 3;
        drawStringRotated(g, -90, x, y - y4 - s5, sdata[i][4]);
      }
    }
  }

  private void drawStringRotated(Object g, int angle, int x, int y, String s) {
    g2d.drawStringRotated(g, s, x, y, angle);
  }

  // determine whether there are any ratio annotations to draw
  private void drawAnnotations(Object g, Lst<Annotation> annotations,
                               ScriptToken whatColor) {
    pd.setFont(g, xPixels, FONT_BOLD, 18, false);
    for (int i = annotations.size(); --i >= 0;) {
      Annotation note = annotations.get(i);
      setAnnotationColor(g, note, whatColor);
      XYScaleConverter c = (note.is2D ? imageView : this);
      int x = c.toPixelX(note.getXVal());
      int y = (note.isPixels()
          ? (int) (yPixel0 + 10 + 10 * pd.scalingFactor - note.getYVal())
          : note.is2D ? imageView.subIndexToPixelY((int) note.getYVal())
              : toPixelY(note.getYVal()));
      g2d.drawString(g, note.text, x + note.offsetX * pd.scalingFactor,
          y - note.offsetY * pd.scalingFactor);
    }
  }

  private void drawIntegralValues(Object g, int iSpec, int yOffset) {
    MeasurementData integrals = getMeasurements(AType.Integration, iSpec);
    if (integrals != null) {
      if (pd.isPrinting)
        pd.setFont(g, xPixels, FONT_PLAIN, 8, false);
      else
        pd.setFont(g, xPixels, FONT_BOLD, 12, false);
      setColorFromToken(g, ScriptToken.INTEGRALPLOTCOLOR);
      int h = pd.getFontHeight();
      g2d.setStrokeBold(g, true);
      for (int i = integrals.size(); --i >= 0;) {
        Measurement in = integrals.get(i);
        if (in.getValue() == 0)
          continue;
        int x = toPixelX(in.getXVal2());
        int y1 = yOffset * pd.scalingFactor + toPixelYint(in.getYVal());
        int y2 = yOffset * pd.scalingFactor + toPixelYint(in.getYVal2());
        if (x != fixX(x) || y1 != fixY(y1) || y2 != fixY(y2))
          continue;

        if (!pd.isPrinting)
          g2d.drawLine(g, x, y1, x, y2);
        String s = "  " + in.text;
        g2d.drawString(g, s, x, (y1 + y2) / 2 + h / 3);
      }
      g2d.setStrokeBold(g, false);
    }
    if (iSpec == getFixedSelectedSpectrumIndex())
      selectedSpectrumIntegrals = integrals;
  }

  private void drawMeasurements(Object g, int iSpec) {
    MeasurementData md = getMeasurements(AType.Measurements, iSpec);
    if (md != null)
      for (int i = md.size(); --i >= 0;)
        drawMeasurement(g, md.get(i));
    if (iSpec == getFixedSelectedSpectrumIndex())
      selectedSpectrumMeasurements = md;
  }

  private void drawMeasurement(Object g, Measurement m) {
    if (m.text.length() == 0 && m != pendingMeasurement)
      return;
    pd.setFont(g, xPixels, FONT_BOLD, 12, false);
    g2d.setGraphicsColor(g,
        (m == pendingMeasurement ? pd.getColor(ScriptToken.PEAKTABCOLOR)
            : pd.BLACK));
    int x1 = toPixelX(m.getXVal());
    int y1 = toPixelY(m.getYVal());
    int x2 = toPixelX(m.getXVal2());
    if (Double.isNaN(m.getXVal()) || x1 != fixX(x1) || x2 != fixX(x2))
      return;
    boolean drawString = (Math.abs(x1 - x2) >= 2);
    boolean drawBaseLine = getScale().isYZeroOnScale() && m.spec.isHNMR();
    int x = (x1 + x2) / 2;
    g2d.setStrokeBold(g, true);
    if (drawString)
      g2d.drawLine(g, x1, y1, x2, y1);
    if (drawBaseLine)
      g2d.drawLine(g, x1 + 1, yPixel1 - 1, x2, yPixel1 - 1);
    g2d.setStrokeBold(g, false);
    if (drawString)
      g2d.drawString(g, m.text, x + m.offsetX, y1 - m.offsetY);
    if (drawBaseLine) {
      g2d.drawLine(g, x1, yPixel1, x1, yPixel1 - 6 * pd.scalingFactor);
      g2d.drawLine(g, x2, yPixel1, x2, yPixel1 - 6 * pd.scalingFactor);
    }
  }

  private PlotWidget getPinSelected(int xPixel, int yPixel) {
    if (widgets != null)
      for (int i = 0; i < widgets.length; i++) {
        if (widgets[i] != null && widgets[i].isPinOrCursor
            && widgets[i].selected(xPixel, yPixel)) {
          return widgets[i];
        }
      }
    return null;
  }

  void set2DCrossHairs(int xPixel, int yPixel) {
    double x;
    if (xPixel == imageView.fixX(xPixel) && yPixel == fixY(yPixel)) {
      pin1Dx1.setX(x = imageView.toX(xPixel), toPixelX(x));
      cur2Dx1.setX(x, xPixel);
      setCurrentSubSpectrum(imageView.toSubspectrumIndex(yPixel));
      if (isLinked) {
        double y = imageView.toY(yPixel);
        pd.set2DCrossHairsLinked(this, x, y, !sticky2Dcursor);
      }
    }
  }

  private void reset2D(boolean isX) {
    if (isX) {
      imageView.setView0(imageView.xPixel0, pin2Dy0.yPixel0, imageView.xPixel1,
          pin2Dy1.yPixel0);
      doZoom(0, getScale().minY, 0, getScale().maxY, true, false, false, false,
          true);
    } else {
      imageView.setView0(pin2Dx0.xPixel0, imageView.yPixel0, pin2Dx1.xPixel0,
          imageView.yPixel1);
      // pd.repaint();
    }
  }

  private boolean setAnnotationText(Annotation a) {
    String sval = pd.getInput("New text?", "Set Label", a.text);
    if (sval == null)
      return false;
    if (sval.length() == 0)
      annotations.removeObj(a);
    else
      a.text = sval;
    return true;
  }

  /**
   * @param x1
   *        start of integral or NaN to clear
   * @param x2
   *        end of (pending) integral or NaN to split
   * @param isFinal
   * @return true if successful
   * 
   * 
   * 
   */
  private boolean checkIntegral(double x1, double x2, boolean isFinal) {
    AnnotationData ad = getDialog(AType.Integration, -1);
    if (ad == null)
      return false;
    Integral integral = ((IntegralData) ad.getData()).addIntegralRegion(x1, x2);
    if (isFinal && isDialog(ad))
      ((JSVDialog) ad).update(null, 0, 0);

    if (Double.isNaN(x2))
      return false;
    pendingIntegral = (isFinal ? null : integral);
    pd.isIntegralDrag = !isFinal;

    selectedSpectrumIntegrals = null;
    return true;
  }

  private void setToolTipForPixels(int xPixel, int yPixel) {

    if (iSpectrumMovedTo != iSpectrumClicked
        || pd.getCurrentGraphSet() != this) {
      pd.setToolTipText("click spectrum to activate");
      return;
    }
    if (isSplitWidget(xPixel, yPixel)) {
      pd.setToolTipText("click to " + (nSplit > 1 ? "combine" : "split"));
      return;
    }
    if (isCloserWidget(xPixel, yPixel)) {
      pd.setToolTipText("click to close");
      return;
    }

    PlotWidget pw = getPinSelected(xPixel, yPixel);
    int precisionX = getScale().precision[0];
    int precisionY = getScale().precision[1];

    if (pw != null) {
      if (setStartupPinTip())
        return;
      String s;
      if (pw == pin1Dx01 || pw == pin2Dx01) {
        s = DF.formatDecimalDbl(Math.min(pin1Dx0.getXVal(), pin1Dx1.getXVal()),
            precisionX) + " - "
            + DF.formatDecimalDbl(
                Math.max(pin1Dx0.getXVal(), pin1Dx1.getXVal()), precisionX);
      } else if (pw == pin1Dy01) {
        s = DF.formatDecimalDbl(Math.min(pin1Dy0.getYVal(), pin1Dy1.getYVal()),
            precisionY) + " - "
            + DF.formatDecimalDbl(
                Math.max(pin1Dy0.getYVal(), pin1Dy1.getYVal()), precisionY);
      } else if (pw == cur2Dy) {
        int isub = imageView.toSubspectrumIndex(pw.yPixel0);
        s = get2DYLabel(isub, precisionX);
      } else if (pw == pin2Dy01) {
        s = "" + (int) Math.min(pin2Dy0.getYVal(), pin2Dy1.getYVal()) + " - "
            + (int) Math.max(pin2Dy0.getYVal(), pin2Dy1.getYVal());
      } else if (pw.isXtype) {
        s = DF.formatDecimalDbl(pw.getXVal(), precisionX);
      } else if (pw.is2D) {
        s = "" + (int) pw.getYVal();
      } else {
        s = DF.formatDecimalDbl(pw.getYVal(), precisionY);
      }
      pd.setToolTipText(s);
      return;
    }

    double yPt;
    if (imageView != null) {
      if (imageView.fixX(xPixel) == xPixel && fixY(yPixel) == yPixel) {

        int isub = imageView.toSubspectrumIndex(yPixel);
        String s = "y=" + get2DYLabel(isub, precisionX) + " / x="
            + DF.formatDecimalDbl(imageView.toX(xPixel), precisionX) + " "
            + getSpectrum().getAxisLabel(true);
        pd.setToolTipText(s);
        pd.coordStr = s;
        return;
      }
      if (!pd.display1D) {
        pd.setToolTipText("");
        pd.coordStr = "";
        return;
      }
    }
    double xPt = toX(fixX(xPixel));
    yPt = (imageView != null && imageView.isXWithinRange(xPixel)
        ? imageView.toSubspectrumIndex(fixY(yPixel))
        : toY(fixY(yPixel)));
    String xx = setCoordStr(xPt, yPt);
    int iSpec = getFixedSelectedSpectrumIndex();
    if (!isInPlotRegion(xPixel, yPixel)) {
      yPt = Double.NaN;
    } else if (nSpectra == 1) {
      // I have no idea what I was thinking here...
      // if (!getSpectrum().isHNMR()) {
      // yPt = spectra[0].getPercentYValueAt(xPt);
      // xx += ", " + formatterY.format(yPt);
      // }
    } else if (haveIntegralDisplayed(iSpec)) {
      yPt = getIntegrationGraph(iSpec).getPercentYValueAt(xPt);
      xx += ", " + DF.formatDecimalDbl(yPt, 1);
    }
    pd.setToolTipText((selectedIntegral != null ? "click to set value"
        : pendingMeasurement != null || selectedMeasurement != null
            ? (pd.hasFocus()
                ? "Press ESC to delete " + (selectedIntegral != null
                    ? "integral, DEL to delete all visible, or N to normalize"
                    : pendingMeasurement == null
                        ? "\"" + selectedMeasurement.text
                            + "\" or DEL to delete all visible"
                        : "measurement")
                : "")
            : Double.isNaN(yPt) ? null : xx)

    // + " :" + iSpectrumSelected + " :" + iSpectrumMovedTo

    );
  }

  private boolean isFrameBox(int xPixel, int yPixel, int boxX, int boxY) {
    return Math.abs(xPixel - (boxX + 5)) < 5
        && Math.abs(yPixel - (boxY + 5)) < 5;
  }

  private String setCoordStr(double xPt, double yPt) {
    String xx = DF.formatDecimalDbl(xPt, getScale().precision[0]);
    pd.coordStr = "(" + xx
        + (haveSingleYScale || iSpectrumSelected >= 0
            ? ", " + DF.formatDecimalDbl(yPt, getScale().precision[1])
            : "")
        + ")";

    return xx;
  }

  private boolean setStartupPinTip() {
    if (pd.startupPinTip == null)
      return false;
    pd.setToolTipText(pd.startupPinTip);
    pd.startupPinTip = null;
    return true;
  }

  private String get2DYLabel(int isub, int precision) {
    Spectrum spec = getSpectrumAt(0).getSubSpectra().get(isub);
    return DF.formatDecimalDbl(spec.getY2DPPM(), precision) + " PPM"
        + (spec.y2DUnits.equals("HZ")
            ? " (" + DF.formatDecimalDbl(spec.getY2D(), precision) + " HZ) "
            : "");
  }

  private boolean isOnSpectrum(int xPixel, int yPixel, int index) {
    Coordinate[] xyCoords = null;
    boolean isContinuous = true;
    boolean isIntegral = (index < 0);

    // ONLY getSpectrumAt(0).is1D();
    if (isIntegral) {
      AnnotationData ad = getDialog(AType.Integration, -1);
      if (ad == null)
        return false;
      xyCoords = ((IntegralData) ad.getData()).getXYCoords();
      index = getFixedSelectedSpectrumIndex();
    } else {
      setScale(index);
      Spectrum spec = spectra.get(index);
      xyCoords = spec.xyCoords;
      isContinuous = spec.isContinuous();
    }
    int yOffset = index * (int) (yPixels * (yStackOffsetPercent / 100f));

    int ix0 = viewData.getStartingPointIndex(index);
    int ix1 = viewData.getEndingPointIndex(index);
    if (isContinuous) {
      for (int i = ix0; i < ix1; i++) {
        Coordinate point1 = xyCoords[i];
        Coordinate point2 = xyCoords[i + 1];
        int x1 = toPixelX(point1.getXVal());
        int x2 = toPixelX(point2.getXVal());
        int y1 = (isIntegral ? toPixelYint(point1.getYVal())
            : toPixelY(point1.getYVal()));
        int y2 = (isIntegral ? toPixelYint(point2.getYVal())
            : toPixelY(point2.getYVal()));
        if (y1 == Integer.MIN_VALUE || y2 == Integer.MIN_VALUE)
          continue;
        y1 = fixY(y1) - yOffset;
        y2 = fixY(y2) - yOffset;
        if (isOnLine(xPixel, yPixel, x1, y1, x2, y2))
          return true;
      }
    } else {
      for (int i = ix0; i <= ix1; i++) {
        Coordinate point = xyCoords[i];
        int y2 = toPixelY(point.getYVal());
        if (y2 == Integer.MIN_VALUE)
          continue;
        int x1 = toPixelX(point.getXVal());
        int y1 = toPixelY(Math.max(getScale().minYOnScale, 0));
        y1 = fixY(y1);
        y2 = fixY(y2);
        if (y1 == y2 && (y1 == yPixel0 || y1 == yPixel1))
          continue;
        if (isOnLine(xPixel, yPixel, x1, y1, x1, y2))
          return true;
      }
    }
    return false;
  }

  // static methods

  private static double distance(int dx, int dy) {
    return Math.sqrt(dx * dx + dy * dy);
  }

  private static GraphSet findCompatibleGraphSet(Lst<GraphSet> graphSets,
                                                 Spectrum spec) {
    for (int i = 0; i < graphSets.size(); i++)
      if (Spectrum.areXScalesCompatible(spec, graphSets.get(i).getSpectrum(),
          false, false))
        return graphSets.get(i);
    return null;
  }

  private static boolean isGoodEvent(PlotWidget zOrP, PlotWidget p,
                                     boolean asX) {
    return (p == null
        ? (Math.abs(zOrP.xPixel1 - zOrP.xPixel0) > MIN_DRAG_PIXELS
            && Math.abs(zOrP.yPixel1 - zOrP.yPixel0) > MIN_DRAG_PIXELS)
        : asX ? Math.abs(zOrP.xPixel0 - p.xPixel0) > MIN_DRAG_PIXELS
            : Math.abs(zOrP.yPixel0 - p.yPixel0) > MIN_DRAG_PIXELS);
  }

  private final static int ONLINE_CUTOFF = 2;

  private static boolean isOnLine(int xPixel, int yPixel, int x1, int y1,
                                  int x2, int y2) {
    // near a point
    int dx1 = Math.abs(x1 - xPixel);
    if (dx1 < ONLINE_CUTOFF && Math.abs(y1 - yPixel) < ONLINE_CUTOFF)
      return true;
    int dx2 = x2 - xPixel;
    if (Math.abs(dx2) < ONLINE_CUTOFF && Math.abs(y2 - yPixel) < ONLINE_CUTOFF)
      return true;
    // between points
    int dy12 = y1 - y2;
    if (Math.abs(dy12) > ONLINE_CUTOFF && (y1 < yPixel) == (y2 < yPixel))
      return false;
    int dx12 = x1 - x2;
    if (Math.abs(dx12) > ONLINE_CUTOFF && (x1 < xPixel) == (x2 < xPixel))
      return false;
    return (distance(dx1, y1 - yPixel)
        + distance(dx2, yPixel - y2) < distance(dx12, dy12) + ONLINE_CUTOFF);
  }

  /**
   * 
   * @param pd
   * @param graphSets
   * @param linkMode
   *        NONE - a vertical stack AB - a COSY, with 1H on left and 2D on right
   *        ABC - a HETCOR, with 1H and 13C on left, 2D on right
   * 
   */
  private static void setFractionalPositions(PanelData pd,
                                             Lst<GraphSet> graphSets,
                                             LinkMode linkMode) {

    int n = graphSets.size();
    double f = 0;
    int n2d = 1;
    GraphSet gs;
    double y = 0;
    pd.isLinked = (linkMode != LinkMode.NONE);
    if (linkMode == LinkMode.NONE) {
      // for now, just a vertical stack
      for (int i = 0; i < n; i++) {
        gs = graphSets.get(i);
        f += (gs.getSpectrumAt(0).is1D() ? 1 : n2d) * gs.nSplit;
      }
      f = 1 / f;
      for (int i = 0; i < n; i++) {
        gs = graphSets.get(i);
        gs.isLinked = false;
        double g = (gs.getSpectrumAt(0).is1D() ? f : n2d * f);
        gs.fX0 = 0;
        gs.fY0 = y;
        gs.fracX = 1;
        gs.fracY = g;
        y += g * gs.nSplit;
      }
    } else {
      GraphSet gs2d = null;
      int i2d = -1;
      if (n == 2 || n == 3)
        for (int i = 0; i < n; i++) {
          gs = graphSets.get(i);
          if (!gs.getSpectrum().is1D()) {
            gs2d = gs;
            if (i2d >= 0)
              i = -2;
            i2d = i;
            break;
          }
        }
      if (i2d == -2 || i2d == -1 && n != 2) {
        setFractionalPositions(pd, graphSets, LinkMode.NONE);
        return;
      }
      for (int i = 0; i < n; i++) {
        gs = graphSets.get(i);
        gs.isLinked = true;
        Spectrum s1 = gs.getSpectrumAt(0);
        boolean is1D = s1.is1D();
        if (is1D) {
          if (gs2d != null) {
            Spectrum s2 = gs2d.getSpectrumAt(0);
            if (Spectrum.areLinkableX(s1, s2))
              gs.gs2dLinkedX = gs2d;
            if (Spectrum.areLinkableY(s1, s2))
              gs.gs2dLinkedY = gs2d;
          }
          gs.fX0 = 0;
          gs.fY0 = y;
          gs.fracX = (gs2d == null ? 1 : 0.5);
          gs.fracY = (n == 3 || gs2d == null ? 0.5 : 1);
          y += 0.5;
        } else {
          gs.fX0 = 0.5;
          gs.fY0 = 0;
          gs.fracX = 0.5;
          gs.fracY = 1;
        }
      }
    }

  }
  // highlight class

  /**
   * Private class to represent a Highlighted region of the spectrum display
   * <p>
   * Title: JSpecView
   * </p>
   * <p>
   * Description: JSpecView is a graphical viewer for chemical spectra specified
   * in the JCAMP-DX format
   * </p>
   * <p>
   * Copyright: Copyright (c) 2002
   * </p>
   * <p>
   * Company: Dept. of Chemistry, University of the West Indies, Mona Campus,
   * Jamaica
   * </p>
   * 
   * @author Debbie-Ann Facey
   * @author Khari A. Bryan
   * @author Prof Robert.J. Lancashire
   * @version 1.0.017032006
   */
  private class Highlight {
    double x1;
    double x2;
    GenericColor color;
    Spectrum spectrum;

    @Override
    public String toString() {
      return "highlight " + x1 + " " + x2 + " " + spectrum;
    }

    Highlight(double x1, double x2, Spectrum spec, GenericColor color) {
      this.x1 = x1;
      this.x2 = x2;
      this.color = color;
      spectrum = spec;
    }

    /**
     * Overides the equals method in class <code>Object</code>
     * 
     * @param obj
     *        the object that this <code>Highlight<code> is compared to
     * @return true if equal
     */

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Highlight))
        return false;
      Highlight hl = (Highlight) obj;
      return ((hl.x1 == this.x1) && (hl.x2 == this.x2));
    }

    @Override
    public int hashCode() {
      return (int) (x1 * 1000 + x2 * 1000000);
    }
  }

  // called only by PanelData

  String addAnnotation(Lst<String> args, String title) {
    if (args.size() == 0
        || args.size() == 1 && args.get(0).equalsIgnoreCase("none")) {
      annotations = null;
      lastAnnotation = null;
      return null;
    }
    if (args.size() < 4 && lastAnnotation == null)
      lastAnnotation = getAnnotation(
          (getScale().maxXOnScale + getScale().minXOnScale) / 2,
          (getScale().maxYOnScale + getScale().minYOnScale) / 2, title, false,
          false, 0, 0);
    Annotation annotation = getAnnotation(args, lastAnnotation);
    if (annotation == null)
      return null;
    if (annotations == null && args.size() == 1
        && args.get(0).charAt(0) == '\"') {
      String s = annotation.text;
      getSpectrum().setTitle(s);
      return s;
    }
    lastAnnotation = annotation;
    addAnnotation(annotation, false);
    return null;
  }

  /**
   * Add information about a region of the displayed spectrum to be highlighted
   * 
   * @param x1
   *        the x value of the coordinate where the highlight should start
   * @param x2
   *        the x value of the coordinate where the highlight should end
   * @param spec
   * @param color
   *        the color of the highlight
   */
  void addHighlight(double x1, double x2, Spectrum spec, GenericColor color) {
    if (spec == null)
      spec = getSpectrumAt(0);
    Highlight hl = new Highlight(x1, x2, spec,
        (color == null ? pd.getColor(ScriptToken.HIGHLIGHTCOLOR) : color));
    if (!highlights.contains(hl))
      highlights.addLast(hl);
  }

  void addPeakHighlight(PeakInfo peakInfo) {
    for (int i = spectra.size(); --i >= 0;) {
      Spectrum spec = spectra.get(i);
      removeAllHighlights(spec);
      if (peakInfo == null || peakInfo.isClearAll()
          || spec != peakInfo.spectrum)
        continue;
      String peak = peakInfo.toString();
      if (peak == null) {
        continue;
      }
      String xMin = PT.getQuotedAttribute(peak, "xMin");
      String xMax = PT.getQuotedAttribute(peak, "xMax");
      if (xMin == null || xMax == null)
        return;
      double x1 = PT.parseDouble(xMin);
      double x2 = PT.parseDouble(xMax);
      if (Double.isNaN(x1) || Double.isNaN(x2))
        return;
      pd.addHighlight(this, x1, x2, spec, 200, 140, 140, 100);
      spec.setSelectedPeak(peakInfo);
      if (getScale().isInRangeX(x1) || getScale().isInRangeX(x2)
          || x1 < getScale().minX && getScale().maxX < x2) {
      } else {
        setZoomTo(0);
      }
    }
  }

  void advanceSubSpectrum(int dir) {
    Spectrum spec0 = getSpectrumAt(0);
    int i = spec0.advanceSubSpectrum(dir);
    if (spec0.isForcedSubset())
      viewData.setXRangeForSubSpectrum(getSpectrum().getXYCoords());
    pd.notifySubSpectrumChange(i, getSpectrum());
  }

  synchronized boolean checkSpectrumClickedEvent(int xPixel, int yPixel,
                                                 int clickCount) {
    if (nextClickForSetPeak != null)
      return false;
    //if (clickCount > 0 && checkArrowLeftRightClick(xPixel, yPixel))
    //return true;
    if (clickCount > 1 || pendingMeasurement != null
        || !isInPlotRegion(xPixel, yPixel))
      return false;
    // in the plot area

    if (clickCount == 0) {
      // pressed

      boolean isOnIntegral = isOnSpectrum(xPixel, yPixel, -1);
      pd.integralShiftMode = (isOnIntegral ? getShiftMode(xPixel, yPixel) : 0);
      pd.isIntegralDrag = (pd.integralShiftMode == 0
          && (isOnIntegral || haveIntegralDisplayed(-1)
              && findMeasurement(getIntegrationGraph(-1), xPixel, yPixel,
                  Measurement.PT_ON_LINE) != null));

      if (pd.integralShiftMode != 0)
        return false;
    }

    if (!showAllStacked)
      return false;
    // in the stacked plot area

    stackSelected = false;
    for (int i = 0; i < nSpectra; i++) {
      if (!isOnSpectrum(xPixel, yPixel, i))
        continue;
      //boolean isNew = (i != iSpectrumSelected);
      setSpectrumClicked(iPreviousSpectrumClicked = i);
      return false;
    }
    // but not on a spectrum
    if (isDialogOpen())
      return false;
    setSpectrumClicked(-1);
    return stackSelected = false;
  }

  private int getShiftMode(int xPixel, int yPixel) {
    return (isStartEndIntegral(xPixel, false) ? yPixel
        : isStartEndIntegral(xPixel, true) ? -yPixel : 0);
  }

  private boolean isDialogOpen() {
    return (isVisible(getDialog(AType.Integration, -1))
        || isVisible(getDialog(AType.Measurements, -1))
        || isVisible(getDialog(AType.PeakList, -1)));
  }

  private boolean isStartEndIntegral(int xPixel, boolean isEnd) {
    return (isEnd ? xPixelPlot1 - xPixel < 20 : xPixel - xPixelPlot0 < 20);
  }

  private boolean widgetsAreSet = true;
  private int lastIntDragX;
  private int nextClickMode;

  synchronized boolean checkWidgetEvent(int xPixel, int yPixel,
                                        boolean isPress) {
    if (!widgetsAreSet)
      return false;
    widgetsAreSet = false;
    PlotWidget widget;
    if (isPress) {
      if (pd.clickCount == 2 && lastIntDragX != xPixel
          && !is2dClick(xPixel, yPixel)) {
        if (pendingMeasurement == null) {
          if (iSpectrumClicked == -1 && iPreviousSpectrumClicked >= 0) {
            setSpectrumClicked(iPreviousSpectrumClicked);
          }
          processPendingMeasurement(xPixel, yPixel, 2);
          return true;
        }
      } else if (!is2dClick(xPixel, yPixel)) {
        if (isOnSpectrum(xPixel, yPixel, -1)) {
          // split
          checkIntegral(toX(xPixel), Double.NaN, false);
        }

        if (lastIntDragX == xPixel) {
          // restart
          pd.isIntegralDrag = true;
          if (!checkIntegral(toX(xPixel), toX(xPixel), false))
            return false;
        }
      }
      if (pendingMeasurement != null)
        return true;

      widget = getPinSelected(xPixel, yPixel);
      if (widget == null) {
        yPixel = fixY(yPixel);
        if (xPixel < xPixel1) {
          if (pd.shiftPressed)
            setSpectrumClicked(iPreviousSpectrumClicked);
          xPixel = fixX(xPixel);
          if (zoomBox1D == null)
            newPins();
          zoomBox1D.setX(toX(xPixel), xPixel);
          zoomBox1D.yPixel0 = yPixel;
          widget = zoomBox1D;
        } else if (imageView != null && xPixel < imageView.xPixel1) {
          zoomBox2D.setX(imageView.toX(xPixel), imageView.fixX(xPixel));
          zoomBox2D.yPixel0 = yPixel;
          widget = zoomBox2D;
        }
      }
      pd.thisWidget = widget;
      return false;
    }
    nextClickForSetPeak = null;
    widget = pd.thisWidget;
    if (widget == null)
      return false;
    // mouse drag with widget
    if (widget == zoomBox1D) {
      zoomBox1D.xPixel1 = fixX(xPixel);
      zoomBox1D.yPixel1 = fixY(yPixel);
      if (pd.isIntegralDrag && zoomBox1D.xPixel0 != zoomBox1D.xPixel1) {
        if ((lastIntDragX <= xPixel) != (zoomBox1D.xPixel0 <= xPixel)) {
          // switched direction of integral drag
          zoomBox1D.xPixel0 = lastIntDragX;
          zoomBox1D.xPixel1 = xPixel;
          zoomBox1D.setXVal(toX(zoomBox1D.xPixel0));
        }
        lastIntDragX = xPixel;
        checkIntegral(zoomBox1D.getXVal(), toX(zoomBox1D.xPixel1), false);
      }
      return false;
    }
    if (!zoomEnabled)
      return false;
    if (widget == zoomBox2D) {
      zoomBox2D.xPixel1 = imageView.fixX(xPixel);
      zoomBox2D.yPixel1 = fixY(yPixel);
      return true;
    }
    if (widget == cur2Dy) {
      yPixel = fixY(yPixel);
      cur2Dy.yPixel0 = cur2Dy.yPixel1 = yPixel;
      setCurrentSubSpectrum(imageView.toSubspectrumIndex(yPixel));
      return true;
    }
    if (widget == cur2Dx0 || widget == cur2Dx1) {
      // xPixel = imageView.fixX(xPixel);
      // widget.setX(imageView.toX(xPixel), xPixel);
      // 2D x zoom change
      // doZoom(cur2Dx0.getXVal(), getScale().minY, cur2Dx1.getXVal(),
      // getScale().maxY, false, false, false, true);
      return false;
    }
    if (widget == pin1Dx0 || widget == pin1Dx1 || widget == pin1Dx01) {
      xPixel = fixX(xPixel);
      widget.setX(toX0(xPixel), xPixel);
      if (widget == pin1Dx01) {
        int dp = xPixel - ((pin1Dx0.xPixel0 + pin1Dx1.xPixel0) / 2);
        int dp1 = (dp < 0 ? dp : dp);
        int dp2 = (dp < 0 ? dp : dp);
        xPixel = pin1Dx0.xPixel0 + dp2;
        int xPixel1 = pin1Dx1.xPixel0 + dp1;
        if (dp == 0 || fixX(xPixel) != xPixel || fixX(xPixel1) != xPixel1)
          return true;
        pin1Dx0.setX(toX0(xPixel), xPixel);
        pin1Dx1.setX(toX0(xPixel1), xPixel1);

      }
      // 1D x zoom change
      doZoom(pin1Dx0.getXVal(), 0, pin1Dx1.getXVal(), 0, true, false, false,
          true, false);
      return true;
    }
    if (widget == pin1Dy0 || widget == pin1Dy1 || widget == pin1Dy01) {
      yPixel = fixY(yPixel);
      widget.setY(toY0(yPixel), yPixel);
      if (widget == pin1Dy01) {
        int dp = yPixel - (pin1Dy0.yPixel0 + pin1Dy1.yPixel0) / 2 + 1;
        yPixel = pin1Dy0.yPixel0 + dp;
        int yPixel1 = pin1Dy1.yPixel0 + dp;
        double y0 = toY0(yPixel);
        double y1 = toY0(yPixel1);
        if (Math.min(y0, y1) == getScale().minY
            || Math.max(y0, y1) == getScale().maxY)
          return true;
        pin1Dy0.setY(y0, yPixel);
        pin1Dy1.setY(y1, yPixel1);
      }
      // y-only zoom
      doZoom(0, pin1Dy0.getYVal(), 0, pin1Dy1.getYVal(), imageView == null,
          imageView == null, false, false, false);
      return true;
    }
    if (widget == pin2Dx0 || widget == pin2Dx1 || widget == pin2Dx01) {
      xPixel = imageView.fixX(xPixel);
      widget.setX(imageView.toX0(xPixel), xPixel);
      if (widget == pin2Dx01) {
        int dp = xPixel - (pin2Dx0.xPixel0 + pin2Dx1.xPixel0) / 2 + 1;
        xPixel = pin2Dx0.xPixel0 + dp;
        int xPixel1 = pin2Dx1.xPixel0 + dp;
        if (imageView.fixX(xPixel) != xPixel
            || imageView.fixX(xPixel1) != xPixel1)
          return true;
        pin2Dx0.setX(imageView.toX0(xPixel), xPixel);
        pin2Dx1.setX(imageView.toX0(xPixel1), xPixel1);
      }
      if (!isGoodEvent(pin2Dx0, pin2Dx1, true)) {
        reset2D(true);
        return true;
      }
      imageView.setView0(pin2Dx0.xPixel0, pin2Dy0.yPixel0, pin2Dx1.xPixel0,
          pin2Dy1.yPixel0);
      // 2D x zoom
      doZoom(pin2Dx0.getXVal(), getScale().minY, pin2Dx1.getXVal(),
          getScale().maxY, false, false, false, true, false);
      return true;
    }
    if (widget == pin2Dy0 || widget == pin2Dy1 || widget == pin2Dy01) {
      yPixel = fixY(yPixel);
      widget.setY(imageView.toSubspectrumIndex(yPixel), yPixel);
      if (widget == pin2Dy01) {
        int dp = yPixel - (pin2Dy0.yPixel0 + pin2Dy1.yPixel0) / 2 + 1;
        yPixel = pin2Dy0.yPixel0 + dp;
        int yPixel1 = pin2Dy1.yPixel0 + dp;
        if (yPixel != fixY(yPixel) || yPixel1 != fixY(yPixel1))
          return true;
        pin2Dy0.setY(imageView.toSubspectrumIndex(yPixel), yPixel);
        pin2Dy1.setY(imageView.toSubspectrumIndex(yPixel1), yPixel1);
      }
      if (!isGoodEvent(pin2Dy0, pin2Dy1, false)) {
        reset2D(false);
        return true;
      }
      imageView.setView0(pin2Dx0.xPixel0, pin2Dy0.yPixel0, pin2Dx1.xPixel1,
          pin2Dy1.yPixel1);
      // update2dImage(false);
      return true;
    }
    return false;
  }

  void clearIntegrals() {
    checkIntegral(Double.NaN, 0, false);
  }

  void clearMeasurements() {
    removeDialog(getFixedSelectedSpectrumIndex(), AType.Measurements);
  }

  static Lst<GraphSet> createGraphSetsAndSetLinkMode(PanelData pd,
                                                     JSVPanel jsvp,
                                                     Lst<Spectrum> spectra,
                                                     int startIndex,
                                                     int endIndex,
                                                     LinkMode linkMode) {
    Lst<GraphSet> graphSets = new Lst<GraphSet>();
    for (int i = 0; i < spectra.size(); i++) {
      Spectrum spec = spectra.get(i);
      GraphSet graphSet = (linkMode == LinkMode.NONE
          ? findCompatibleGraphSet(graphSets, spec)
          : null);
      if (graphSet == null)
        graphSets.addLast(graphSet = new GraphSet(jsvp.getPanelData()));
      graphSet.addSpec(spec);
    }
    setFractionalPositions(pd, graphSets, linkMode);
    for (int i = graphSets.size(); --i >= 0;) {
      graphSets.get(i).initGraphSet(startIndex, endIndex);
      Logger.info("JSVGraphSet " + (i + 1) + " nSpectra = "
          + graphSets.get(i).nSpectra);
    }
    return graphSets;
  }

  /**
   * 
   * entry point for a repaint
   * 
   * @param gMain
   *        primary canvas
   * @param gFront
   *        front-pane (glass pane) canvas
   * @param gBack
   *        back-pane canvas
   * @param width
   * @param height
   * @param left
   * @param right
   * @param top
   * @param bottom
   * @param isResized
   * @param taintedAll
   * @param pointsOnly
   */
  synchronized void drawGraphSet(Object gMain, Object gFront, Object gBack,
                                 int width, int height, int left, int right,
                                 int top, int bottom, boolean isResized,
                                 boolean taintedAll, boolean pointsOnly) {

    zoomEnabled = pd.getBoolean(ScriptToken.ENABLEZOOM);
    this.height = height * pd.scalingFactor;
    this.width = width * pd.scalingFactor;
    this.left = left * pd.scalingFactor;
    this.right = right * pd.scalingFactor;
    this.top = top * pd.scalingFactor;
    this.bottom = bottom * pd.scalingFactor;
    // yValueMovedTo = Double.NaN;
    haveSelectedSpectrum = false;
    selectedSpectrumIntegrals = null;
    selectedSpectrumMeasurements = null;
    isPrintingOrSaving = pd.isPrinting || pd.creatingImage;
    if (!isPrintingOrSaving && widgets != null)
      for (int j = 0; j < widgets.length; j++)
        if (widgets[j] != null)
          widgets[j].isVisible = false;
    for (int iSplit = 0; iSplit < nSplit; iSplit++) {
      // for now, at least, we only allow one 2D image
      drawAll(gMain, gFront, gBack, iSplit, isResized || nSplit > 1, taintedAll,
          pointsOnly);
    }
    setPositionForFrame(nSplit > 1 ? pd.currentSplitPoint : 0);
  }

  synchronized void escapeKeyPressed(boolean isDEL) {
    if (zoomBox1D != null)
      zoomBox1D.xPixel0 = zoomBox1D.xPixel1 = 0;
    if (zoomBox2D != null)
      zoomBox2D.xPixel0 = zoomBox2D.xPixel1 = 0;
    if (!inPlotMove)
      return;
    if (pendingMeasurement != null) {
      pendingMeasurement = null;
      return;
    }
    pd.thisWidget = null;
    pendingMeasurement = null;
    if (selectedSpectrumMeasurements != null && selectedMeasurement != null) {
      if (isDEL)
        selectedSpectrumMeasurements.clear(getScale().minXOnScale,
            getScale().maxXOnScale);
      else
        selectedSpectrumMeasurements.removeObj(selectedMeasurement);
      selectedMeasurement = null;
      updateDialog(AType.Measurements, -1);
    }
    if (selectedSpectrumIntegrals != null && selectedIntegral != null) {
      if (isDEL)
        selectedSpectrumIntegrals.clear(getScale().minXOnScale,
            getScale().maxXOnScale);
      else
        selectedSpectrumIntegrals.removeObj(selectedIntegral);
      selectedIntegral = null;
      updateDialog(AType.Integration, -1);
    }
  }

  static GraphSet findGraphSet(Lst<GraphSet> graphSets, int xPixel,
                               int yPixel) {
    for (int i = graphSets.size(); --i >= 0;)
      if (graphSets.get(i).hasPoint(xPixel, yPixel))
        return graphSets.get(i);
    return null;
  }

  PeakInfo findMatchingPeakInfo(PeakInfo pi) {
    PeakInfo pi2 = null;
    for (int i = 0; i < spectra.size(); i++)
      if ((pi2 = (spectra.get(i)).findMatchingPeakInfo(pi)) != null)
        break;
    return pi2;
  }

  int getCurrentSpectrumIndex() {
    return (nSpectra == 1 ? 0 : iSpectrumSelected);
  }

  Integral getSelectedIntegral() {
    return selectedIntegral;
  }

  boolean getShowAnnotation(AType type, int i) {
    AnnotationData id = getDialog(type, i);
    return (id != null && id.getState());
  }

  boolean hasFileLoaded(String filePath) {
    for (int i = spectra.size(); --i >= 0;)
      if (spectra.get(i).getFilePathForwardSlash().equals(filePath))
        return true;
    return false;
  }

  boolean haveSelectedSpectrum() {
    return haveSelectedSpectrum;
  }

  synchronized void mouseClickedEvent(int xPixel, int yPixel, int clickCount,
                                      boolean isControlDown) {
    selectedMeasurement = null;
    selectedIntegral = null;
    Double isNextClick = nextClickForSetPeak;
    nextClickForSetPeak = null;
    if (checkArrowUpDownClick(xPixel, yPixel)
        || checkArrowLeftRightClick(xPixel, yPixel))
      return;
    lastClickX = Double.NaN;
    lastPixelX = Integer.MAX_VALUE;
    if (isSplitWidget(xPixel, yPixel)) {
      splitStack(nSplit == 1);
      return;
    }
    if (isCloserWidget(xPixel, yPixel)) {
      pd.closeSpectrum();
      return;
    }
    PlotWidget pw = getPinSelected(xPixel, yPixel);
    if (pw != null) {
      setWidgetValueByUser(pw);
      return;
    }
    boolean is2D = is2dClick(xPixel, yPixel);
    if (clickCount == 2 && iSpectrumClicked == -1
        && iPreviousSpectrumClicked >= 0) {
      setSpectrumClicked(iPreviousSpectrumClicked);
    }
    if (!is2D && isControlDown) {
      setSpectrumClicked(iPreviousSpectrumClicked);
      if (pendingMeasurement != null) {
        processPendingMeasurement(xPixel, yPixel, -3);
      } else if (iSpectrumClicked >= 0) {
        processPendingMeasurement(xPixel, yPixel, 3);
      }
      return;
    }
    lastXMax = Double.NaN; // TODO: was for "is2D || !isControlDown
    if (clickCount == 2) {
      if (is2D) {
        if (sticky2Dcursor) {
          addAnnotation(getAnnotation(imageView.toX(xPixel),
              imageView.toSubspectrumIndex(yPixel), pd.coordStr, false, true, 5,
              5), true);
        }
        sticky2Dcursor = true;//!sticky2Dcursor;
        set2DCrossHairs(xPixel, yPixel);
        return;
      }

      // 1D double-click

      if (isInTopBar(xPixel, yPixel)) {
        // 1D x zoom reset to original
        doZoom(toX0(xPixel0), 0, toX0(xPixel1), 0, true, false, false, true,
            true);
      } else if (isInRightBar(xPixel, yPixel)) {
        doZoom(getScale().minXOnScale, viewList.get(0).getScale().minYOnScale,
            getScale().maxXOnScale, viewList.get(0).getScale().maxYOnScale,
            true, true, false, false, false);
      } else if (isInTopBar2D(xPixel, yPixel)) {
        reset2D(true);
      } else if (isInRightBar2D(xPixel, yPixel)) {
        reset2D(false);
      } else if (pendingMeasurement != null) {
        processPendingMeasurement(xPixel, yPixel, -2);
      } else if (iSpectrumClicked >= 0) {
        processPendingMeasurement(xPixel, yPixel, 2);
      }
      return;
    }

    // single click

    if (is2D) {
      if (annotations != null) {
        Coordinate xy = new Coordinate().set(imageView.toX(xPixel),
            imageView.toSubspectrumIndex(yPixel));
        Annotation a = findAnnotation2D(xy);
        if (a != null && setAnnotationText(a)) {
          return;
        }
      }
      if (clickCount == 1)
        sticky2Dcursor = false;
      set2DCrossHairs(xPixel, yPixel);
      return;
    }

    // 1D single click

    if (isInPlotRegion(xPixel, yPixel)) {
      if (selectedSpectrumIntegrals != null
          && checkIntegralNormalizationClick(xPixel, yPixel))
        return;
      if (pendingMeasurement != null) {
        processPendingMeasurement(xPixel, yPixel, 1);
        return;
      }

      setCoordClicked(xPixel, toX(xPixel), toY(yPixel));
      updateDialog(AType.PeakList, -1);
      if (isNextClick != null) {
        nextClickForSetPeak = isNextClick;
        shiftSpectrum(SHIFT_CLICKED, Double.NaN, Double.NaN);
        nextClickForSetPeak = null;
        return;
      }

    } else {
      setCoordClicked(0, Double.NaN, 0);
    }
    pd.notifyPeakPickedListeners(null);

  }

  private boolean is2dClick(int xPixel, int yPixel) {
    return (imageView != null && xPixel == imageView.fixX(xPixel)
        && yPixel == fixY(yPixel));
  }

  private void updateDialog(AType type, int iSpec) {
    AnnotationData ad = getDialog(type, iSpec);
    if (ad == null || !isVisible(ad))
      return;
    double xRange = toX(xPixel1) - toX(xPixel0);
    int yOffset = (getSpectrum().isInverted() ? yPixel1 - pd.mouseY
        : pd.mouseY - yPixel0);
    ((JSVDialog) ad).update(pd.coordClicked, xRange, yOffset);
  }

  private boolean isVisible(AnnotationData ad) {
    return (isDialog(ad) && ad.isVisible());
  }

  public void mousePressedEvent(int xPixel, int yPixel,
                                @SuppressWarnings("unused") int clickCount) {
    checkWidgetEvent(xPixel, yPixel, true);
  }

  /**
   * @param xPixel
   * @param yPixel
   */
  synchronized void mouseReleasedEvent(int xPixel, int yPixel) {
    if (pendingMeasurement != null) {
      if (Math.abs(toPixelX(pendingMeasurement.getXVal()) - xPixel) < 2)
        pendingMeasurement = null;
      processPendingMeasurement(xPixel, yPixel, -2);
      setToolTipForPixels(xPixel, yPixel);
      return;
    }
    if (pd.integralShiftMode != 0) {
      pd.integralShiftMode = 0;
      zoomBox1D.xPixel1 = zoomBox1D.xPixel0;
      return;
    }
    if (iSpectrumMovedTo >= 0)
      setScale(iSpectrumMovedTo);
    PlotWidget thisWidget = pd.thisWidget;
    if (pd.isIntegralDrag) {
      if (isGoodEvent(zoomBox1D, null, true)) {
        checkIntegral(toX(zoomBox1D.xPixel0), toX(zoomBox1D.xPixel1), true);
      }
      zoomBox1D.xPixel1 = zoomBox1D.xPixel0 = 0;
      pendingIntegral = null;
      pd.isIntegralDrag = false;
    } else if (thisWidget == zoomBox2D) {
      if (!isGoodEvent(zoomBox2D, null, true))
        return;
      imageView.setZoom(zoomBox2D.xPixel0, zoomBox2D.yPixel0, zoomBox2D.xPixel1,
          zoomBox2D.yPixel1);
      zoomBox2D.xPixel1 = zoomBox2D.xPixel0;
      // 2D xy zoom
      doZoom(imageView.toX(imageView.xPixel0), getScale().minY,
          imageView.toX(imageView.xPixel0 + imageView.xPixels - 1),
          getScale().maxY, false, false, false, true, true);
    } else if (thisWidget == zoomBox1D) {
      if (!isGoodEvent(zoomBox1D, null, true))
        return;
      int x1 = zoomBox1D.xPixel1;
      // 1D x zoom by zoomBox
      boolean doY = (pd.shiftPressed);
      doZoom(toX(zoomBox1D.xPixel0), (doY ? toY(zoomBox1D.yPixel0) : 0),
          toX(x1), (doY ? toY(zoomBox1D.yPixel1) : 0), true, doY, true, true,
          true);
      zoomBox1D.xPixel1 = zoomBox1D.xPixel0;
    } else if (thisWidget == pin1Dx0 || thisWidget == pin1Dx1
        || thisWidget == cur2Dx0 || thisWidget == cur2Dx1) {
      addCurrentZoom();
    }
  }

  synchronized void mouseMovedEvent(int xPixel, int yPixel) {
    if (xPixel == Integer.MAX_VALUE) {
      iSpectrumMovedTo = -1;
      return;
    }
    if (nSpectra > 1) {
      int iFrame = getSplitPoint(yPixel);
      setPositionForFrame(iFrame);
      setSpectrumMovedTo(nSplit > 1 ? iFrame : iSpectrumSelected);
      if (iSpectrumMovedTo >= 0)
        setScale(iSpectrumMovedTo);
    } else {
      iSpectrumMovedTo = iSpectrumSelected = 0;
    }
    inPlotMove = isInPlotRegion(xPixel, yPixel);
    setXPixelMovedTo(Double.MAX_VALUE, Double.MAX_VALUE,
        (inPlotMove ? xPixel : -1), -1);
    if (inPlotMove) {
      xValueMovedTo = toX(xPixelMovedTo);
      yValueMovedTo = getSpectrum().getYValueAt(xValueMovedTo);
    }
    if (pd.integralShiftMode != 0) {
      AnnotationData ad = getDialog(AType.Integration, -1);
      Coordinate[] xy = ((IntegralData) ad.getData()).getXYCoords();
      double y = xy[pd.integralShiftMode > 0 ? xy.length - 1 : 0].getYVal();

      ((IntegralData) ad.getData()).shiftY(pd.integralShiftMode,
          toPixelYint(y) + yPixel
              - (pd.integralShiftMode > 0 ? yPixelPlot1 : yPixelPlot0),
          yPixel0, yPixels);
    } else if (pd.isIntegralDrag) {
    } else if (pendingMeasurement != null) {
      processPendingMeasurement(xPixel, yPixel, 0);
      setToolTipForPixels(xPixel, yPixel);
    } else {
      selectedMeasurement = (inPlotMove && selectedSpectrumMeasurements != null
          ? findMeasurement(selectedSpectrumMeasurements, xPixel, yPixel,
              Measurement.PT_ON_LINE)
          : null);
      selectedIntegral = null;
      if (inPlotMove && selectedSpectrumIntegrals != null
          && selectedMeasurement == null) {
        selectedIntegral = (Integral) findMeasurement(selectedSpectrumIntegrals,
            xPixel, yPixel, Measurement.PT_ON_LINE);
        if (selectedIntegral == null)
          selectedIntegral = (Integral) findMeasurement(
              selectedSpectrumIntegrals, xPixel, yPixel,
              Measurement.PT_INT_LABEL);
      }
      setToolTipForPixels(xPixel, yPixel);
      if (imageView == null) {
        piMouseOver = null;
        int iSpec = (nSplit > 1 ? iSpectrumMovedTo : iSpectrumClicked);
        if (!isDrawNoSpectra() && iSpec >= 0) {
          Spectrum spec = spectra.get(iSpec);
          if (spec.getPeakList() != null) {
            coordTemp.setXVal(toX(xPixel));
            coordTemp.setYVal(toY(yPixel));
            piMouseOver = spec.findPeakByCoord(xPixel, coordTemp);
          }
        }
      } else {
        if (!pd.display1D && sticky2Dcursor) {
          set2DCrossHairs(xPixel, yPixel);
        }
      }
    }
  }

  /**
   * Displays the next view zoomed
   */
  void nextView() {
    if (currentZoomIndex + 1 < viewList.size())
      setZoomTo(currentZoomIndex + 1);
  }

  /**
   * Displays the previous view zoomed
   */
  void previousView() {
    if (currentZoomIndex > 0)
      setZoomTo(currentZoomIndex - 1);
  }

  /**
   * Resets the spectrum to it's original view
   */
  void resetView() {
    setZoomTo(0);
  }

  void removeAllHighlights() {
    removeAllHighlights(null);
  }

  /**
   * Remove the highlight at the specified index in the internal list of
   * highlights The index depends on the order in which the highlights were
   * added
   * 
   * @param index
   *        the index of the highlight in the list
   */
  void removeHighlight(int index) {
    highlights.removeItemAt(index);
  }

  /**
   * Remove the highlight specified by the starting and ending x value
   * 
   * @param x1
   *        the x value of the coordinate where the highlight started
   * @param x2
   *        the x value of the coordinate where the highlight ended
   */
  void removeHighlight(double x1, double x2) {
    for (int i = highlights.size(); --i >= 0;) {
      Highlight h = highlights.get(i);
      if (h.x1 == x1 && h.x2 == x2)
        highlights.removeItemAt(i);
    }
  }

  void scaleYBy(double factor) {
    if (imageView == null && !zoomEnabled)
      return;
    // from CTRL +/-
    viewData.scaleSpectrum(imageView == null ? iSpectrumSelected : -2, factor);
    if (imageView != null) {
      update2dImage(false);
      resetPinsFromView();
    }
    pd.refresh();
    // view.scaleSpectrum(-1, factor);
  }

  /**
   * here we are selecting a spectrum based on a message from Jmol matching type
   * and model
   * 
   * @param filePath
   * @param type
   * @param model
   * @return haveFound
   */
  boolean selectSpectrum(String filePath, String type, String model) {
    boolean haveFound = false;
    for (int i = spectra.size(); --i >= 0;)
      if ((filePath == null
          || getSpectrumAt(i).getFilePathForwardSlash().equals(filePath))
          && (getSpectrumAt(i).matchesPeakTypeModel(type, model))) {
        setSpectrumSelected(i);
        if (nSplit > 1)
          splitStack(true);
        haveFound = true;
      }
    if (nSpectra > 1 && !haveFound && iSpectrumSelected >= 0
        && !pd.isCurrentGraphSet(this))
      setSpectrumSelected(Integer.MIN_VALUE); // no plots in that case
    return haveFound;
  }

  PeakInfo selectPeakByFileIndex(String filePath, String index,
                                 String atomKey) {
    PeakInfo pi;
    for (int i = spectra.size(); --i >= 0;)
      if ((pi = getSpectrumAt(i).selectPeakByFileIndex(filePath, index,
          atomKey)) != null)
        return pi;
    return null;
  }

  void setSelected(int i) {
    if (i < 0) {
      bsSelected.clearAll();
      setSpectrumClicked(-1);
      return;
    }
    bsSelected.set(i);
    setSpectrumClicked((bsSelected.cardinality() == 1 ? i : -1));
    if (nSplit > 1 && i >= 0)
      pd.currentSplitPoint = i;
  }

  void setSelectedIntegral(double val) {
    Spectrum spec = selectedIntegral.getSpectrum();
    getIntegrationGraph(getSpectrumIndex(spec))
        .setSelectedIntegral(selectedIntegral, val);
  }

  /**
   * turn on or off a PeakList, Integration, or Measurement dialog
   * 
   * @param type
   * @param tfToggle
   */
  void setShowAnnotation(AType type, Boolean tfToggle) {
    AnnotationData ad = getDialog(type, -1);
    if (ad == null) {
      if (tfToggle != null && tfToggle != Boolean.TRUE)
        return; // does not exist and "OFF" -- ignore
      // does not exist, and TOGGLE or ON
      if (type == AType.PeakList || type == AType.Integration
          || type == AType.Measurements)
        pd.showDialog(type);
      return;
    }
    if (tfToggle == null) {
      // exists and "TOGGLE", but id could be an AnnotationData, not a JDialog
      if (isDialog(ad))
        ((JSVDialog) ad).setVisible(!((JSVDialog) ad).isVisible());
      // was tfToggle != null && ((AnnotationDialog) id).isVisible());
      else
        pd.showDialog(type); //	was			id.setState(!id.getState());
      return;
    }
    // exists and "ON" or "OFF"
    boolean isON = tfToggle.booleanValue();
    if (isON)
      ad.setState(isON);
    if (isON || isDialog(ad))
      pd.showDialog(type);
    if (!isON && isDialog(ad))
      ((JSVDialog) ad).setVisible(false);

    // if (type == AType.Integration)
    // checkIntegral(parameters, "UPDATE");
    // id.setState(tfToggle == null ? !id.getState() : tfToggle.booleanValue());
  }

  boolean checkIntegralParams(Parameters parameters, String value) {
    Spectrum spec = getSpectrum();
    if (!spec.canIntegrate() || reversePlot)
      return false;
    int iSpec = getFixedSelectedSpectrumIndex();
    AnnotationData ad = getDialog(AType.Integration, -1);
    if (value == null)// && ad != null)
      return true;
    switch (IntegralData.IntMode.getMode(value.toUpperCase())) {
    case NA:
      return false;
    case CLEAR:
      integrate(iSpec, null);
      integrate(iSpec, parameters);
      break;
    case ON:
      if (ad == null)
        integrate(iSpec, parameters);
      else
        ad.setState(true);
      break;
    case OFF:
      if (ad != null)
        ad.setState(false);
      //			integrate(iSpec, null);
      break;
    case TOGGLE:
      if (ad == null)
        integrate(iSpec, parameters);
      else
        ad.setState(!ad.getState());
      //			integrate(iSpec, ad == null ? parameters : null);
      break;
    case AUTO:
      if (ad == null) {
        checkIntegralParams(parameters, "ON");
        ad = getDialog(AType.Integration, -1);
      }
      if (ad != null)
        ((IntegralData) ad.getData()).autoIntegrate();
      break;
    case LIST:
      pd.showDialog(AType.Integration);
      break;
    case MARK:
      if (ad == null) {
        checkIntegralParams(parameters, "ON");
        ad = getDialog(AType.Integration, -1);
      }
      if (ad != null)
        ((IntegralData) ad.getData()).addMarks(value.substring(4).trim());
      break;
    case MIN:
      if (ad != null) {
        try {
          double val = Double.parseDouble(ScriptToken.getTokens(value).get(1));
          ((IntegralData) ad.getData()).setMinimumIntegral(val);
        } catch (Exception e) {
          // ignore
        }
      }
      break;
    case UPDATE:
      if (ad != null)
        ((IntegralData) ad.getData()).update(parameters);
    }
    updateDialog(AType.Integration, -1);
    return true;
  }

  void setSpectrum(int iSpec, boolean fromSplit) {
    if (fromSplit && nSplit > 1) {
      if (nSplit > 1)
        setSpectrumClicked(iSpec);
    } else {
      setSpectrumClicked(iSpec);
      stackSelected = false;
      showAllStacked = false;
    }
    if (iSpec >= 0)
      dialogsToFront(getSpectrum());
  }

  void setSpectrumJDX(Spectrum spec) {
    // T/A conversion for IR
    int pt = getFixedSelectedSpectrumIndex();
    spectra.removeItemAt(pt);
    spectra.add(pt, spec);
    pendingMeasurement = null;
    clearViews();
    viewData.newSpectrum(spectra);
  }

  void setZoom(double x1, double y1, double x2, double y2) {
    // called by
    // 1. double-clicking on a tree node in the application to reset (0,0,0,0)
    // 2. the YSCALE command (0,y1,0,y2)
    // 3. the ZOOM command (0,0,0,0) or (x1, 0, x2, 0) or (x1, y1, x2, y2)
    setZoomTo(0);
    if (x1 == 0 && x2 == 0 && y1 == 0 && y2 == 0) {
      newPins();
      // reset gray-factors as well
      imageView = null;
      // in case this is linked, transmit the x-zoom to linked spectra
      x1 = getScale().minXOnScale;
      x2 = getScale().maxXOnScale;
      //} else if (x1 == 0 && x2 == 0) {
      //	// y zoom only
      //	x1 = x2 = 0;
      //	imageView = null;
    } else {
      doZoom(x1, y1, x2, y2, true, (y1 != y2), false, true, true);
    }
  }

  static final int SHIFT_PEAK = 1;
  static final int SHIFT_SETX = 2;
  static final int SHIFT_X = 3;
  static final int SHIFT_CLICKED = 4;

  /**
   * apply a shift to the x-axis based on the next click or a specified value
   * 
   * @param mode
   *        one of PEAK(1), SETX(2), X(3), CLICKED(4)
   * @param xOld
   *        initial position or NaN to ask, or value
   * @param xNew
   *        DOUBLE_MAX_VALUE - reset, NaN - ask, or value
   * @return true if accomplished
   */
  boolean shiftSpectrum(int mode, double xOld, double xNew) {

    Spectrum spec = getSpectrum();
    if (!spec.isNMR() || !spec.is1D())
      return false;
    String ok = null;
    double dx = 0;
    if (xNew == Double.MAX_VALUE) {
      // setPeak NONE or setX NONE
      dx = -spec.addSpecShift(0);
    } else {
      switch (mode) {
      case SHIFT_X:
        dx = xNew;
        break;
      case SHIFT_PEAK:
      case SHIFT_SETX:
        nextClickMode = mode;
        if (Double.isNaN(xOld)) {
          ok = pd.getInput(
              "Click on "
                  + (mode == SHIFT_PEAK
                      ? "or beside a peak to set its chemical shift"
                      : "the spectrum set the chemical shift at that point")
                  + (xNew == Integer.MIN_VALUE ? "" : " to " + xNew) + ".",
              "Set Reference " + (mode == SHIFT_PEAK ? "for Peak" : "at Point"),
              "OK");
          nextClickForSetPeak = ("OK".equals(ok) ? Double.valueOf(xNew) : null);
          return false;
        }
        nextClickForSetPeak = null;
        //$FALL-THROUGH$
      case SHIFT_CLICKED:
        if (nextClickForSetPeak != null) {
          xNew = nextClickForSetPeak.doubleValue();
          nextClickForSetPeak = null;
        }
        if (Double.isNaN(xOld))
          xOld = lastClickX;
        if (nextClickMode == SHIFT_PEAK)
          xOld = getNearestPeak(spec, xOld, toY(pd.mouseY));
        if (Double.isNaN(xNew))
          try {
            String s = pd.getInput("New chemical shift (set blank to reset)",
                "Set Reference",
                DF.formatDecimalDbl(xOld, getScale().precision[0])).trim();
            if (s.length() == 0)
              xNew = xOld - spec.addSpecShift(0);
            else
              xNew = Double.parseDouble(s);
          } catch (Exception e) {
            return false;
          }
        dx = xNew - xOld;
        break;
      }
    }
    if (dx == 0)
      return false;
    spec.addSpecShift(dx);
    if (annotations != null)
      for (int i = annotations.size(); --i >= 0;)
        if (annotations.get(i).spec == spec)
          annotations.get(i).addSpecShift(dx);
    if (dialogs != null)
      for (Map.Entry<String, AnnotationData> e : dialogs.entrySet())
        if (e.getValue().getSpectrum() == spec)
          e.getValue().setSpecShift(dx);
    // double dx0 = getScale().specShift;
    // for (int i = viewList.size(); --i >= 0;)
    // viewList.get(i).addSpecShift(dx);
    // if (getScale().specShift == dx0)
    getScale().addSpecShift(dx);
    if (!Double.isNaN(lastClickX))
      lastClickX += dx;
    updateDialogs();
    doZoom(0, getScale().minYOnScale, 0, getScale().maxYOnScale, true, true,
        false, true, false);
    pd.setTaintedAll();
    pd.repaint();
    return true;
  }

  void toPeak(int istep) {
    istep *= (drawXAxisLeftToRight ? 1 : -1);
    if (Double.isNaN(lastClickX))
      lastClickX = lastPixelX = 0;
    Spectrum spec = getSpectrum();
    Coordinate coord = setCoordClicked(lastPixelX, lastClickX, 0);
    int iPeak = spec.setNextPeak(coord, istep);
    if (iPeak < 0)
      return;
    PeakInfo peak = spec.getPeakList().get(iPeak);
    spec.setSelectedPeak(peak);
    setCoordClicked(peak.getXPixel(), peak.getX(), 0);
    pd.notifyPeakPickedListeners(
        new PeakPickEvent(jsvp, pd.coordClicked, peak));
  }

  // methods that only act on SELECTED spectra

  void scaleSelectedBy(double f) {
    for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
        .nextSetBit(i + 1))
      viewData.scaleSpectrum(i, f);
  }

  // overridden methods

  @Override
  public String toString() {
    return "gs: " + nSpectra + " " + spectra + " "
        + spectra.get(0).getFilePath();
  }

  void setXPointer(Spectrum spec, double x) {
    if (spec != null)
      setSpectrumClicked(getSpectrumIndex(spec));
    xValueMovedTo = lastClickX = x;
    lastPixelX = toPixelX(x);
    setXPixelMovedTo(x, Double.MAX_VALUE, 0, 0);
    yValueMovedTo = Double.NaN;
  }

  void setXPointer2(Spectrum spec, double x) {
    if (spec != null)
      setSpectrumClicked(getSpectrumIndex(spec));
    setXPixelMovedTo(Double.MAX_VALUE, x, 0, 0);
  }

  boolean hasCurrentMeasurement(AType type) {
    return ((type == AType.Integration ? selectedSpectrumIntegrals
        : selectedSpectrumMeasurements) != null);
  }

  private Map<String, AnnotationData> dialogs;
  private Object[] aIntegrationRatios;

  AnnotationData getDialog(AType type, int iSpec) {
    if (iSpec == -1)
      iSpec = getCurrentSpectrumIndex();
    return (dialogs == null || iSpec < 0 ? null
        : dialogs.get(type + "_" + iSpec));
  }

  void removeDialog(int iSpec, AType type) {
    if (dialogs != null && iSpec >= 0)
      dialogs.remove(type + "_" + iSpec);
  }

  AnnotationData addDialog(int iSpec, AType type, AnnotationData dialog) {
    //		if (iSpec < 0) {
    //			iSpec = getSpectrumIndex(dialog.getSpectrum());
    //			dialog = null;
    //		}
    if (dialogs == null)
      dialogs = new Hashtable<String, AnnotationData>();
    String key = type + "_" + iSpec;
    dialog.setGraphSetKey(key);
    dialogs.put(key, dialog);
    return dialog;
  }

  void removeDialog(JSVDialog dialog) {
    String key = dialog.getGraphSetKey();
    dialogs.remove(key);
    AnnotationData data = dialog.getData();
    if (data != null)
      dialogs.put(key, data);
  }

  MeasurementData getPeakListing(int iSpec, Parameters p, boolean forceNew) {
    if (iSpec < 0)
      iSpec = getCurrentSpectrumIndex();
    if (iSpec < 0)
      return null;
    AnnotationData dialog = getDialog(AType.PeakList, -1);
    if (dialog == null) {
      if (!forceNew)
        return null;
      addDialog(iSpec, AType.PeakList,
          dialog = new PeakData(AType.PeakList, getSpectrum()));
    }
    ((PeakData) dialog.getData()).setPeakList(p, Integer.MIN_VALUE,
        viewData.getScale());
    if (isDialog(dialog))
      ((JSVDialog) dialog).setFields();
    return dialog.getData();
  }

  void setPeakListing(Boolean tfToggle) {
    AnnotationData dialog = getDialog(AType.PeakList, -1);
    JSVDialog ad = (isDialog(dialog) ? (JSVDialog) dialog
        : null);
    boolean isON = (tfToggle == null ? ad == null || !ad.isVisible()
        : tfToggle.booleanValue());
    if (isON) {
      pd.showDialog(AType.PeakList);
    } else if (isDialog(dialog)) {
        ((JSVDialog) dialog).setVisible(false);
    }
  }

  boolean haveIntegralDisplayed(int i) {
    AnnotationData ad = getDialog(AType.Integration, i);
    return (ad != null && ad.getState());
  }

  IntegralData getIntegrationGraph(int i) {
    AnnotationData ad = getDialog(AType.Integration, i);
    return (ad == null ? null : (IntegralData) ad.getData());
  }

  void setIntegrationRatios(String value) {
    int iSpec = getFixedSelectedSpectrumIndex();
    if (aIntegrationRatios == null)
      aIntegrationRatios = new Object[nSpectra];
    aIntegrationRatios[iSpec] = IntegralData
        .getIntegrationRatiosFromString(getSpectrum(), value);
  }

  /**
   * deprecated -- or at least not compatible with multiple spectra
   * 
   * @param i
   * @return list
   */
  @SuppressWarnings("unchecked")
  Lst<Annotation> getIntegrationRatios(int i) {
    return (Lst<Annotation>) (aIntegrationRatios == null ? null
        : aIntegrationRatios[i]);
  }

  boolean integrate(int iSpec, Parameters parameters) {
    Spectrum spec = getSpectrumAt(iSpec);
    if (parameters == null || !spec.canIntegrate()) {
      removeDialog(iSpec, AType.Integration);
      return false;
    }
    addDialog(iSpec, AType.Integration, new IntegralData(spec, parameters));
    return true;
  }

  IntegralData getIntegration(int iSpec, Parameters p, boolean forceNew) {
    if (iSpec < 0)
      iSpec = getCurrentSpectrumIndex();
    if (iSpec < 0)
      return null;
    AnnotationData dialog = getDialog(AType.Integration, -1);
    if (dialog == null) {
      if (!forceNew)
        return null;
      dialog = addDialog(iSpec, AType.Integration,
          new IntegralData(getSpectrum(), p));
    }
    return (IntegralData) dialog.getData();
  }

  Map<String, Object> getMeasurementInfo(AType type, int iSpec) {
    MeasurementData md;
    switch (type) {
    case PeakList:
      md = getPeakListing(iSpec, null, false);
      break;
    case Integration:
      md = getIntegration(iSpec, null, false);
      break;
    default:
      return null;
    }
    if (md == null)
      return null;
    Map<String, Object> info = new Hashtable<String, Object>();
    md.getInfo(info);
    return info;
  }

  Map<String, Object> getInfo(String key, int iSpec) {
    Map<String, Object> spectraInfo = new Hashtable<String, Object>();
    if ("".equals(key)) {
      spectraInfo.put("KEYS", "viewInfo spectra");
    } else if ("viewInfo".equalsIgnoreCase(key))
      return getScale().getInfo(spectraInfo);
    Lst<Map<String, Object>> specInfo = new Lst<Map<String, Object>>();
    spectraInfo.put("spectra", specInfo);
    for (int i = 0; i < nSpectra; i++) {
      if (iSpec >= 0 && i != iSpec)
        continue;
      Spectrum spec = spectra.get(i);
      Map<String, Object> info = spec.getInfo(key);
      if (iSpec >= 0 && key != null
          && (info.size() == 2 || key.equalsIgnoreCase("id"))) {
        if (info.size() == 2)
          info.remove("id");
        return info;
      }
      Parameters.putInfo(key, info, "type", spec.getDataType());
      Parameters.putInfo(key, info, "titleLabel", spec.getTitleLabel());
      Parameters.putInfo(key, info, "filePath",
          spec.getFilePath().replace('\\', '/'));
      Parameters.putInfo(key, info, "PeakList",
          (Parameters.isMatch(key, "PeakList")
              ? getMeasurementInfo(AType.PeakList, i)
              : null));
      Parameters.putInfo(key, info, "Integration",
          (Parameters.isMatch(key, "Integration")
              ? getMeasurementInfo(AType.Integration, i)
              : null));
      if (iSpec >= 0)
        return info;
      specInfo.addLast(info);
    }
    return spectraInfo;
  }

  /**
   * 
   * @param forPrinting
   *        (could be preparing to print)
   * @return null if ambiguous, otherwise JDXSpectrum.getTitle()
   * 
   */
  String getTitle(boolean forPrinting) {
    return (nSpectra == 1
        || iSpectrumSelected >= 0 && (!forPrinting || nSplit == 1)
            ? getSpectrum().getTitle()
            : null);
  }

  ScaleData getCurrentView() {
    setScale(getFixedSelectedSpectrumIndex());
    return viewData.getScale();
  }

  void set2DXY(double x, double y, boolean isLocked) {
    int p;
    if (gs2dLinkedX != null) {
      p = toPixelX(x);
      if (p != fixX(p)) {
        p = Integer.MIN_VALUE;
        x = Double.MAX_VALUE;
      }
      cur1D2x1.setX(x, p);
    }
    if (gs2dLinkedY != null) {
      p = toPixelX(y);
      if (p != fixX(p)) {
        p = Integer.MIN_VALUE;
        y = Double.MAX_VALUE;
      }
      cur1D2x2.setX(y, p);
    }
    cur1D2Locked = isLocked;
  }

  void dialogsToFront(Spectrum spec) {
    if (dialogs == null)
      return;
    if (spec == null)
      spec = getSpectrum();
    for (Map.Entry<String, AnnotationData> e : dialogs.entrySet()) {
      AnnotationData ad = e.getValue();
      if (isVisible(ad)) {
        if (spec == null)
          ((JSVDialog) ad).setVisible(true);
        else
          ((JSVDialog) ad).setFocus(ad.getSpectrum() == spec);
      }
    }
  }

  //////////////////////////// WAS AWTGRAPHSET //////////////

  void setPlotColors(Object oColors) {
    GenericColor[] colors = (GenericColor[]) oColors;
    if (colors.length > nSpectra) {
      GenericColor[] tmpPlotColors = new GenericColor[nSpectra];
      System.arraycopy(colors, 0, tmpPlotColors, 0, nSpectra);
      colors = tmpPlotColors;
    } else if (nSpectra > colors.length) {
      GenericColor[] tmpPlotColors = new GenericColor[nSpectra];
      int numAdditionColors = nSpectra - colors.length;
      System.arraycopy(colors, 0, tmpPlotColors, 0, colors.length);
      for (int i = 0, j = colors.length; i < numAdditionColors; i++, j++)
        tmpPlotColors[j] = generateRandomColor();
      colors = tmpPlotColors;
    }
    plotColors = colors;
  }

  private JSVPanel jsvp;
  private Object image2D;
  private GenericColor[] plotColors;
  private GenericGraphics g2d;
  private Object gMain;

  private void disposeImage() {
    /**
     * @j2sNative
     *
     *            if (this.image2D != null)
     *            this.image2D.parentNode.removeChild(this.image2D);
     * 
     */
    {
    }
    image2D = null;
    jsvp = null;
    pd = null;
    highlights = null;
    plotColors = null;
  }

  private GenericColor generateRandomColor() {
    while (true) {
      int red = (int) (Math.random() * 255);
      int green = (int) (Math.random() * 255);
      int blue = (int) (Math.random() * 255);
      GenericColor randomColor = g2d.getColor3(red, green, blue);
      if (randomColor.getRGB() != 0)
        return randomColor;
    }
  }

  void setPlotColor0(Object oColor) {
    plotColors[0] = (GenericColor) oColor;
  }

  /**
   * Returns the color of the plot at a certain index
   * 
   * @param index
   *        the index
   * @return the color of the plot
   */
  GenericColor getPlotColor(int index) {
    if (index >= plotColors.length)
      return null;
    return plotColors[index];
  }

  private void setColorFromToken(Object og, ScriptToken whatColor) {
    if (whatColor != null)
      g2d.setGraphicsColor(og,
          whatColor == ScriptToken.PLOTCOLOR ? plotColors[0]
              : pd.getColor(whatColor));
  }

  private final int COLOR_GREY = -3;
  private final int COLOR_BLACK = -2;
  private final int COLOR_INTEGRAL = -1;

  private void setPlotColor(Object og, int i) {
    GenericColor c;
    switch (i) {
    case COLOR_GREY:
      c = veryLightGrey;
      break;
    case COLOR_BLACK:
      c = pd.BLACK;
      break;
    case COLOR_INTEGRAL:
      c = pd.getColor(ScriptToken.INTEGRALPLOTCOLOR);
      break;
    default:
      c = plotColors[i];
      break;
    }
    g2d.setGraphicsColor(og, c);
  }

  /////////////// 2D image /////////////////

  private void draw2DImage() {
    if (imageView != null)
      g2d.drawGrayScaleImage(gMain, image2D, imageView.xPixel0,
          imageView.yPixel0, // destination 
          imageView.xPixel0 + imageView.xPixels - 1, // destination 
          imageView.yPixel0 + imageView.yPixels - 1, // destination 
          imageView.xView1, imageView.yView1, imageView.xView2,
          imageView.yView2); // source
  }

  private boolean get2DImage(@SuppressWarnings("unused") Spectrum spec0) {
    imageView = new ImageView();
    imageView.set(viewList.get(0).getScale());
    if (!update2dImage(true))
      return false;
    imageView.resetZoom();
    sticky2Dcursor = true;// I don't know why
    return true;
  }

  private boolean update2dImage(boolean isCreation) {
    imageView.set(viewData.getScale());
    Spectrum spec = getSpectrumAt(0);
    int[] buffer = imageView.get2dBuffer(spec, !isCreation);
    if (buffer == null) {
      image2D = null;
      imageView = null;
      return false;
    }
    if (isCreation) {
      buffer = imageView.adjustView(spec, viewData);
      imageView.resetView();
    }
    image2D = g2d.newGrayScaleImage(gMain, image2D, imageView.imageWidth,
        imageView.imageHeight, buffer);
    setImageWindow();
    return true;
  }

  private Annotation getAnnotation(double x, double y, String text,
                                   boolean isPixels, boolean is2d, int offsetX,
                                   int offsetY) {
    return new ColoredAnnotation().setCA(x, y, getSpectrum(), text, pd.BLACK,
        isPixels, is2d, offsetX, offsetY);
  }

  private Annotation getAnnotation(Lst<String> args,
                                   Annotation lastAnnotation) {
    return Annotation.getColoredAnnotation(g2d, getSpectrum(), args,
        lastAnnotation);
  }

  private void fillBox(Object g, int x0, int y0, int x1, int y1,
                       ScriptToken whatColor) {
    setColorFromToken(g, whatColor);
    g2d.fillRect(g, Math.min(x0, x1), Math.min(y0, y1), Math.abs(x0 - x1),
        Math.abs(y0 - y1));
  }

  private void drawBox(Object g, int x0, int y0, int x1, int y1,
                       ScriptToken whatColor) {
    setColorFromToken(g, whatColor);
    g2d.drawRect(g, Math.min(x0, x1), Math.min(y0, y1), Math.abs(x0 - x1) - 1,
        Math.abs(y0 - y1) - 1);
  }

  private void drawHandle(Object g, int x, int y, int size,
                          boolean outlineOnly) {
    if (outlineOnly)
      g2d.drawRect(g, x - size, y - size, size * 2, size * 2);
    else
      g2d.fillRect(g, x - size, y - size, size * 2 + 1, size * 2 + 1);
  }

  private void setCurrentBoxColor(Object g) {
    g2d.setGraphicsColor(g, pd.BLACK);
  }

  private void fillArrow(Object g, int type, int x, int y, boolean doFill) {
    int f = 1;
    switch (type) {
    case ARROW_LEFT:
    case ARROW_UP:
      f = -1;
      break;
    }
    int[] axPoints = new int[] { x - 5, x - 5, x + 5, x + 5, x + 8, x, x - 8 };
    int[] ayPoints = new int[] { y + 5 * f, y - f, y - f, y + 5 * f, y + 5 * f,
        y + 10 * f, y + 5 * f };
    switch (type) {
    case ARROW_LEFT:
    case ARROW_RIGHT:
      if (doFill)
        g2d.fillPolygon(g, ayPoints, axPoints, 7);
      else
        g2d.drawPolygon(g, ayPoints, axPoints, 7);
      break;
    case ARROW_UP:
    case ARROW_DOWN:
      if (doFill)
        g2d.fillPolygon(g, axPoints, ayPoints, 7);
      else
        g2d.drawPolygon(g, axPoints, ayPoints, 7);

    }
  }

  private void fillCircle(Object g, int x, int y, boolean doFill) {
    if (doFill)
      g2d.fillCircle(g, x - 4, y - 4, 8);
    else
      g2d.drawCircle(g, x - 4, y - 4, 8);
  }

  void setAnnotationColor(Object g, Annotation note, ScriptToken whatColor) {
    if (whatColor != null) {
      setColorFromToken(g, whatColor);
      return;
    }
    GenericColor color = null;
    if (note instanceof ColoredAnnotation)
      color = ((ColoredAnnotation) note).getColor();
    if (color == null)
      color = pd.BLACK;
    g2d.setGraphicsColor(g, color);
  }

  public void setSolutionColor(VisibleInterface vi, boolean isNone,
                               boolean asFitted) {
    for (int i = 0; i < nSpectra; i++) {
      Spectrum spec = spectra.get(i);
      int color = (isNone || !spec.canShowSolutionColor() ? -1
          : vi.getColour(spec, asFitted));
      spec.setFillColor(
          color == -1 ? null : pd.vwr.parameters.getColor1(color));
    }
  }

  public void setIRMode(IRMode mode, String type) {
    for (int i = 0; i < nSpectra; i++) {
      Spectrum spec = spectra.get(i);
      if (!spec.dataType.equals(type))
        continue;
      Spectrum spec2 = Spectrum.taConvert(spec, mode);
      if (spec2 != spec)
        pd.setSpecForIRMode(spec2);
    }
  }

  public int getSpectrumCount() {
    // TODO Auto-generated method stub
    return 0;
  }

  public void invertYAxis() {
    viewList.get(0).init(null, 0, 0,
        getSpectrum().invertYAxis().isContinuous());
    resetViewCompletely();
  }

  private static boolean isDialog(AnnotationData ad) {
    return (ad != null && ad.isDialog());
  }

}
