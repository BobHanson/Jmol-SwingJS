package org.jmol.jsv;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.JmolJDXMOLParser;
import org.jmol.api.JmolJDXMOLReader;
import javajs.util.BS;
import org.jmol.util.Logger;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

/**
 * Parses JDX-MOL records ##$MODELS and ##$PEAKS/##$SIGNALS. Used in both Jmol
 * and JSpecView.
 * 
 * Also gets info from ACD Labs files JCAMP-DX=5.00 $$ ACD/SpecManager v 12.01
 * 
 */
public class JDXMOLParser implements JmolJDXMOLParser {

  private String line;
  private String lastModel = "";
  private String thisModelID;
  private String baseModel;

  private float vibScale;
  private String piUnitsX, piUnitsY;

  private JmolJDXMOLReader loader;

  private String modelIdList = "";
  private int[] peakIndex;
  private String peakFilePath;

  public JDXMOLParser() {
    // for reflection
  }

  @Override
  public JmolJDXMOLParser set(JmolJDXMOLReader loader, String filePath,
                              Map<String, Object> htParams) {
    this.loader = loader;
    peakFilePath = filePath;
    peakIndex = new int[1];
    if (htParams != null) {
      htParams.remove("modelNumber");
      // peakIndex will be passed on to additional files in a ZIP file load
      // the peak file path is stripped of the "|xxxx.jdx" part 
      if (htParams.containsKey("zipSet")) {
        peakIndex = (int[]) htParams.get("peakIndex");
        if (peakIndex == null) {
          peakIndex = new int[1];
          htParams.put("peakIndex", peakIndex);
        }
        if (!htParams.containsKey("subFileName"))
          peakFilePath = PT.split(filePath, "|")[0];
      }
    }
    return this;
  }

  /* (non-Javadoc)
   * @see org.jmol.jsv.JmolJDXModelPeakReader#getAttribute(java.lang.String, java.lang.String)
   */
  @Override
  public String getAttribute(String line, String tag) {
    String attr = PT.getQuotedAttribute(line, tag);
    return (attr == null ? "" : attr);
  }

  /* (non-Javadoc)
   * @see org.jmol.jsv.JmolJDXModelPeakReader#getRecord(java.lang.String)
   */
  @Override
  public String getRecord(String key) throws Exception {
    if (line == null || line.indexOf(key) < 0)
      return null;
    String s = line;
    while (s.indexOf(">") < 0)
      s += " " + readLine();
    return line = s;
  }

  /* (non-Javadoc)
   * @see org.jmol.jsv.JmolJDXModelPeakReader#readModels()
   */
  @Override
  public boolean readModels() throws Exception {
    if (!findRecord("Models"))
      return false;
    // if load xxx.jdx n  then we must temporarily set n to 1 for the base model reading
    // load xxx.jdx 0  will mean "load only the base model(s)"
    line = "";
    thisModelID = "";
    boolean isFirst = true;
    while (true) {
      line = loader.discardLinesUntilNonBlank();
      if (getRecord("<ModelData") == null)
        break;
      getModelData(isFirst);
      // updateModel here regardless???
      isFirst = false;
    }
    return true;
  }

  /**
   * MOL file embedded in JDX file
   * 
   */
  @Override
  public String readACDMolFile() throws Exception {
  	// Jmol only; JSpecView uses a tokenizer
    //##$MOLFILE=  $$ Empty String
    //  ACD/Labs03231213092D
    //  $$ Empty String
    // 11 10  0  0  0  0  0  0  0  0 12 V2000
    //...
    //$$$$
    SB sb = new SB();
    sb.append(line.substring(line.indexOf("=") + 1)).appendC('\n');
    while (readLine() != null && !line.contains("$$$$"))
      sb.append(line).appendC('\n');
    return PT.rep(sb.toString(), "  $$ Empty String", "");
  }

	@Override
	public Lst<String[]> readACDAssignments(int nPoints, boolean isPeakAssignment)
			throws Exception {
		// NMR:
		// ##PEAK ASSIGNMENTS=(XYMA)
		// (25.13376,1.00, ,<1>)
		// (25.13376,1.00, ,<3>)
		// (63.97395,0.35, ,<2>)
		// ##$UVIR_ASSIGNMENT=ACDTABLE(X,Y,A,VT)
		// (1645.2935,33.1941,'1,3',undefined)
		// (3022.9614,65.476,'1,13',undefined)
		// (2854.6709,56.5426,'4,12',undefined)
		// ##$MS_FRAGMENTS=ACDTABLE(Fragment,Formula,Label,mzCalc,mzExp,TICCalc,TICExp,RICalc,AQI,RDBE)
		// ('7-8,11;11a;11b;8a;8b;8c',C3H5,M -
		// C7H9O,41.0386,41,0.046,4.5954,0.2754,1,4.0)
		// ('1,7-8,11;11a;11b;1a;8a;8b;8c',C4H6,M -
		// C6H8O,54.0464,54,0.1582,15.1322,0.9372,0.9568,4.0)
		// ('1-6,10;2a;2b;3a;6a;6b',C6H5O,M -
		// C4H9,93.0335,93,0.0872,8.7166,0.5041,1,4.0)
		
	  // also accepts old Chime method,looking for "select atomno=n":
	  // 38, -1, 1, <reset; select *; color atoms cpk; spacefill off; wireframe on; 
		//   select atomno=4; color atoms yellow; spacefill 90>


		Lst<String[]> list = new Lst<String[]>();
		try {
			readLine(); // flushes "XYMA"
			if (nPoints < 0)
				nPoints = Integer.MAX_VALUE;
			for (int i = 0; i < nPoints; i++) {
				String s = readLine();
				if (s == null || s.indexOf("#") == 0)
					break;
				if (isPeakAssignment) {
					while (s.indexOf(">") < 0)
						s += " " + readLine();
					s = s.trim();
				}
				s = PT.replaceAllCharacters(s, "()<>", " ").trim();
				if (s.length() == 0)
					break;
				int pt = s.indexOf("'");
				if (pt >= 0) {
					int pt2 = s.indexOf("'", pt + 1);
					s = s.substring(0, pt)
							+ PT.rep(s.substring(pt + 1, pt2), ",", ";")
							+ s.substring(pt2 + 1);
				}
				Logger.info("Peak Assignment: " + s);
				String[] tokens = PT.split(s, ",");
				list.addLast(tokens);
			}
		} catch (Exception e) {
			Logger.error("Error reading peak assignments at " + line + ": " + e);
		}
		return list;
	}

	@Override
	public int setACDAssignments(String model, String mytype, int peakCount,
			Lst<String[]> acdlist, String molFile) throws Exception {
		try {
			if (peakCount >= 0)
				peakIndex = new int[] { peakCount };
			boolean isMS = (mytype.indexOf("MASS") == 0);
			String file = " file=" + PT.esc(peakFilePath.replace('\\', '/'));
			model = " model=" + PT.esc(model + " (assigned)");
			piUnitsX = "";
			piUnitsY = "";
			float dx = getACDPeakWidth(mytype) / 2;
			Map<String, Object[]> htSets = new Hashtable<String, Object[]>();
			Lst<Object[]> list = new Lst<Object[]>();
			Map<String, String> zzcMap = null;
			int ptx, pta;
			int nAtoms = 0;
			if (isMS) {
				zzcMap = new Hashtable<String, String>();
				String[] tokens = PT.split(molFile, "M  ZZC");
				for (int i = tokens.length; --i >= 1;) {
					String[] ab = PT.getTokens(tokens[i]);
					nAtoms = Math.max(nAtoms, PT.parseInt(ab[0]));
					zzcMap.put(ab[1], ab[0]);
				}
				ptx = 4;
				pta = 0;
			} else if (mytype.indexOf("NMR") >= 0) {
				ptx = 0;
				pta = 3;
			} else {
				ptx = 0;
				pta = 2; // IR  Raman? UV?  - don't know
			}
			int nPeaks = acdlist.size();
			for (int i = 0; i < nPeaks; i++) {
				String[] data = acdlist.get(i);
				float x = PT.parseFloat(data[ptx]);
				String a = data[pta];
				if (isMS)
					a = fixACDAtomList(a, zzcMap, nAtoms);
				else
					a = a.replace(';', ',');
				if (a.indexOf("select") >= 0) {
					int pt = a.indexOf("select atomno=");
					if (pt < 0)
						continue;
					a = PT.split(a.substring(pt + 14), " ")[0];
				}
				String title = (isMS ? "m/z=" + Math.round(x) + ": " + data[2] + " (" + data[1] + ")" : 
					pta == 2 ? "" + (Math.round(x * 10) / 10f) : null);
				getStringInfo(file, title, mytype, model, a, htSets, "" + x, list,
						" atoms=\"%ATOMS%\" xMin=\"" + (x - dx) + "\" xMax=\"" + (x + dx)
								+ "\">");
			}
			return setPeakData(list, 0);
		} catch (Exception e) {
			return 0;
		}
	}

	private String fixACDAtomList(String atoms, Map<String, String> zzcMap, int nAtoms) {
	  atoms = atoms.trim();
	  String[] tokens = PT.getTokens(atoms.replace(';', ' '));
    BS bs = new BS();
    boolean isM = false;
	  for (int i = 0; i < tokens.length; i++) {
	    String a = tokens[i];
	    isM = (a.indexOf("M") >= 0);
	    if (isM)
	    	a = "1-" + nAtoms;
	    int pt = a.indexOf('-');
	    if (pt >= 0) {
	      int i1 = PT.parseInt(a.substring(0, pt));
	      int i2 = PT.parseInt(a.substring(pt + 1)) + 1;
	      for (int k = i1; k < i2; k++)
	        bs.set(isM ? k : PT.parseInt(zzcMap.get("" + k)));
	    } else {
	      bs.set(PT.parseInt(zzcMap.get(a)));
	    }
	  }
    String s = bs.toJSON();
    return s.substring(1, s.length() - 1);
  }

  private float getACDPeakWidth(String type) {
	  return (type.indexOf("HNMR") >= 0 ? 0.05f
	  		: type.indexOf("CNMR") >= 0 ? 1f 
	      : type.indexOf("MASS") >= 0 ? 1f
	      : 10); // IR, Raman, UV/VIS
  }

  @Override
	public int readPeaks(boolean isSignals, int peakCount) throws Exception {
		try {
			if (peakCount >= 0)
				peakIndex = new int[] { peakCount };
			int offset = (isSignals ? 1 : 0);
			String tag1 = (isSignals ? "Signals" : "Peaks");
			String tag2 = (isSignals ? "<Signal" : "<PeakData");
			if (!findRecord(tag1))
				return 0;
			String file = " file=" + PT.esc(peakFilePath.replace('\\', '/'));
			String model = PT.getQuotedAttribute(line, "model");
			model = " model=" + PT.esc(model == null ? thisModelID : model);
			String mytype = PT.getQuotedAttribute(line, "type");
			piUnitsX = PT.getQuotedAttribute(line, "xLabel");
			piUnitsY = PT.getQuotedAttribute(line, "yLabel");
			Map<String, Object[]> htSets = new Hashtable<String, Object[]>();
			Lst<Object[]> list = new Lst<Object[]>();
			while (readLine() != null
					&& !(line = line.trim()).startsWith("</" + tag1)) {
				if (line.startsWith(tag2)) {
					getRecord(tag2);
					Logger.info(line);
					String title = PT.getQuotedAttribute(line, "title");
					if (mytype == null)
						mytype = PT.getQuotedAttribute(line, "type");
					String atoms = PT.getQuotedAttribute(line, "atoms");
					String key = ((int) (PT.parseFloat(PT
							.getQuotedAttribute(line, "xMin")) * 100))
							+ "_"
							+ ((int) (PT.parseFloat(PT.getQuotedAttribute(line, "xMax")) * 100));
					getStringInfo(file, title, mytype,
							(PT.getQuotedAttribute(line, "model") == null ? model : ""),
							atoms, htSets, key, list, line.substring(tag2.length()).trim());
				}
			}
			return setPeakData(list, offset);
		} catch (Exception e) {
			return 0;
		}
	}

	private int setPeakData(Lst<Object[]> list, int offset) {
		int nH = 0;
		int n = list.size();
		for (int i = 0; i < n; i++) {
			Object[] o = list.get(i);
			String info = PT.rep((String) o[0], "%INDEX%", "" + (++peakIndex[0]));
			BS bs = (BS) o[1];
			if (bs != null) {
				String s = "";
				for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
					s += "," + (j + offset);
				int na = bs.cardinality();
				nH += na;
				info = PT.rep(info, "%ATOMS%", s.substring(1));
				info = PT.rep(info, "%S%", (na == 1 ? "" : "s"));
				info = PT.rep(info, "%NATOMS%", "" + na);
			}
			Logger.info("adding PeakData " + info);
			loader.addPeakData(info);
		}
		loader.setSpectrumPeaks(nH, piUnitsX, piUnitsY);
		return n;
}

	private void getStringInfo(String file, String title, String mytype,
			String model, String atoms, Map<String, Object[]> htSets,
			String key, Lst<Object[]> list, String more) {
		if ("HNMR".equals(mytype))
			mytype = "1HNMR";
		else if ("CNMR".equals(mytype))
			mytype = "13CNMR";
		String type = (mytype == null ? "" : " type=" + PT.esc(mytype));
		if (title == null)
			title = ("1HNMR".equals(mytype) ? "atom%S%: %ATOMS%; integration: %NATOMS%"
					: "");
		title = " title=" + PT.esc(title);
		String stringInfo = "<PeakData " + file + " index=\"%INDEX%\"" + title
				+ type + model + " " + more;
		if (atoms != null)
			stringInfo = PT.rep(stringInfo, "atoms=\"" + atoms + "\"",
					"atoms=\"%ATOMS%\"");
		Object[] o = htSets.get(key);
		if (o == null) {
			o = new Object[] { stringInfo, (atoms == null ? null : new BS()) };
			htSets.put(key, o);
			list.addLast(o);
		}
		if (atoms != null) {
			BS bs = (BS) o[1];
			atoms = atoms.replace(',', ' ');
			if (atoms.equals("*"))
				atoms = "0:1000";
      bs.or(BS.unescape("({" + atoms + "})"));
		}
	}

	private void getModelData(boolean isFirst) throws Exception {
    lastModel = thisModelID;
    thisModelID = getAttribute(line, "id");
    // read model only once for a given ID
    String key = ";" + thisModelID + ";";
    if (modelIdList.indexOf(key) >= 0) {
      line = loader.discardLinesUntilContains("</ModelData>");
      return;
    }
    modelIdList += key;
    baseModel = getAttribute(line, "baseModel");
    while (line.indexOf(">") < 0 && line.indexOf("type") < 0)
      readLine();
    String modelType = getAttribute(line, "type").toLowerCase();
    vibScale = PT.parseFloat(getAttribute(line, "vibrationScale"));
    if (modelType.equals("xyzvib"))
      modelType = "xyz";
    else if (modelType.length() == 0)
      modelType = null; // let Jmol set the type
    SB sb = new SB();
    while (readLine() != null && !line.contains("</ModelData>"))
      sb.append(line).appendC('\n');
    loader.processModelData(sb.toString(), thisModelID, modelType, baseModel,
        lastModel, Float.NaN, vibScale, isFirst);
  }

  /**
   * @param tag
   * @return line
   * @throws Exception
   */
  private boolean findRecord(String tag) throws Exception {
    if (line == null)
      readLine();
    if (line != null && line.indexOf("<" + tag) < 0)
      line = loader.discardLinesUntilContains2("<" + tag, "##");
    return (line != null && line.indexOf("<" + tag) >= 0);
  }

  private String readLine() throws Exception {
    return line = loader.rd();
  }

  @Override
  public void setLine(String s) {
    line = s;
  }

}
