package jspecview.common;

//import jspecview.util.Logger;

import javajs.util.PT;

public class PeakInfo {
  public final static PeakInfo nullPeakInfo = new PeakInfo();

  private double xMin, xMax, yMin, yMax;
  private int px0, px1;
  private String stringInfo;
  private String type;
  private String type2;
  private String index;
  private String file;
  private String filePathForwardSlash;

  private String title;
  private String model;
  private String atoms;
  private String id;
  public Spectrum spectrum;

  private String _match;

	private String atomKey;


  public PeakInfo() {
  }

  public PeakInfo(String s) {
    stringInfo = s;
    type = PT.getQuotedAttribute(s, "type");
    if (type == null)
      type = "";
    type = type.toUpperCase();
    int pt = type.indexOf('/');
    type2 = (pt < 0 ? "" : fixType(type.substring(type.indexOf('/') + 1)));
    if (pt >= 0)
      type = fixType(type.substring(0, pt)) + "/" + type2;
    else
      type = fixType(type);
    id = PT.getQuotedAttribute(s, "id");
    index = PT.getQuotedAttribute(s, "index");
    file = PT.getQuotedAttribute(s, "file");
    System.out.println("pi file=" + file);
    filePathForwardSlash = (file == null ? null : file.replace('\\','/'));

    model = PT.getQuotedAttribute(s, "model");
    boolean isBaseModel = s.contains("baseModel=\"\"");
    if (!isBaseModel)
      atoms = PT.getQuotedAttribute(s, "atoms");
    atomKey = "," + atoms + ",";
    title = PT.getQuotedAttribute(s, "title");
    _match = PT.getQuotedAttribute(s, "_match"); // PEAK command creates this
    xMax = PT.parseFloat(PT.getQuotedAttribute(s, "xMax"));
    xMin = PT.parseFloat(PT.getQuotedAttribute(s, "xMin"));
    yMax = PT.parseFloat(PT.getQuotedAttribute(s, "yMax"));
    yMin = PT.parseFloat(PT.getQuotedAttribute(s, "yMin"));
  }

  public boolean isClearAll() {
    return (spectrum == null);
  }

  public String getType() {
    return type;
  }

  public String getAtoms() {
    return atoms;
  }

  public double getXMax() {
    return xMax;
  }

  public double getXMin() {
    return xMin;
  }

  public double getYMin() {
    return yMin;
  }

  public double getYMax() {
    return yMax;
  }

  public double getX() {
    return (xMax + xMin) / 2;
  }

  public String getMatch() {
    return _match;
  }
  
  private static String fixType(String type) {
    return (type.equals("HNMR") ? "1HNMR" : type.equals("CNMR") ? "13CNMR"
        : type);
  }

  @Override
  public String toString() {
    return stringInfo;
  }

  public String getIndex() {
    return index;
  }

  public String getTitle() {
    return title;
  }

	public boolean checkFileIndex(String filePath, String sIndex, String sAtomKey) {
		return (sAtomKey != null ? atomKey.indexOf(sAtomKey) >= 0
				: sIndex.equals(index) && (filePath.equals(file) || filePath
				.equals(filePathForwardSlash)));
	}

  /**
   * type checks true for MS in GC/MS; reverse of checkType
   * 
   * @param filePath
   * @param type
   * @param model
   * @return true for MS in GC/MS; reverse of checkType
   */
	public boolean checkFileTypeModel(String filePath, 
			String type, String model) {
		return filePath.equals(file) && checkModel(model)
		  && this.type.endsWith(type);
	}

  public boolean checkTypeModel(String type, String model) {
    return checkType(type) //   GC/MS matches MS
        && checkModel(model);
  }

  private boolean checkModel(String model) {
    return (model != null && model.equals(this.model));
  }

  private boolean checkType(String type) {
    return (type.endsWith(this.type));
  }

  public boolean checkTypeMatch(PeakInfo pi) {
    return (checkType(pi.type) 
        && (checkId(pi._match) || checkModel(pi._match) || title.toUpperCase().indexOf(pi._match) >= 0));
  }

	private boolean checkId(String match) {
		if (match == null)
			return false;
		return (id != null && match.toUpperCase().startsWith("ID=") && match.substring(3).equals(id)
						|| (match = match.toUpperCase()).startsWith("INDEX=") && match.equals("INDEX=" + index)
						|| match.startsWith("#=") && match.equals("#=" + index)
		);
	}

	public String getModel() {
    return model;
  }

  public String getFilePath() {
    return file;
  }

  /**
   * a spectrum which, when loaded, should fire a message to load first peak --
   * GC for now
   * 
   * @return can autoselect on loading
   */
  public boolean autoSelectOnLoad() {
    return (type.startsWith("GC"));
  }

  public void setPixelRange(int x0, int x1) {
  	px0 = x0;
  	px1 = x1;
  }
  
  public double checkRange(int xPixel, double xVal) {
    if (xPixel != Integer.MAX_VALUE ? (px0 <= xPixel && px1 >= xPixel) : 
    	xVal >= xMin && xVal <= xMax) {
    	return Math.abs(xVal - getX());
    }
    return 1e100;
  }

	public int getXPixel() {
		return (px0 + px1) / 2;
	}

}
