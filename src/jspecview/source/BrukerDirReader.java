package jspecview.source;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.BinaryDocument;
import javajs.util.Lst;
import jspecview.common.Coordinate;
import jspecview.common.Spectrum;

public class BrukerDirReader {

  private static final int TYPE_INT = 0;

  public BrukerDirReader() { // for dynamic loading
  }

  public JDXSource getJDXFromBrukerDir(String fullPath)
      throws FileNotFoundException, Exception {
    File dir = new File(fullPath);
    File procs = new File(dir, "procs");
    if (!procs.exists())
      procs = new File(dir, "pdata/1/procs");
    File pdata = procs.getParentFile();
    String brukerDir = pdata.getParentFile().getParent();
    Map<String, String> map = new Hashtable<String, String>();
    InputStream is = new FileInputStream(new File(brukerDir, "acqus"));
    JDXReader.getHeaderMap(is,
        map);
    is.close();
    is = new FileInputStream(procs);
    JDXReader.getHeaderMap(is, map);
    is.close();
    
    //int np = Integer.parseInt(map.get("##$NCPROC")); not nec.
    int dtypp = Integer.parseInt(map.get("##$DTYPP"));
    int byteorp = (dtypp == TYPE_INT ? Integer.parseInt(map.get("##$BYTORDP"))
        : Integer.MAX_VALUE);
    if (dtypp == Integer.MIN_VALUE || byteorp == Integer.MIN_VALUE)
      return null;
    double[] data = getData(new File(procs.getParent(), "1r"), dtypp, byteorp);
    JDXSource source = new JDXSource(JDXSource.TYPE_SIMPLE, brukerDir);
    map.put("##TITLE", getFileContents(new File(pdata, "title")));
    setSource(data, map, source);
    return source;
  }

  private String getFileContents(File file)
      throws FileNotFoundException, IOException {
    if (file.exists()) {
      int len = (int) file.length();
      try (InputStream in = new FileInputStream(file)) {
        byte[] bytes = new byte[len];
        in.read(bytes);
        return new String(bytes);
      }
    }
    return "";
  }

  private void setSource(double[] data, Map<String, String> map,
                         JDXSource source) {
    Lst<String[]> LDRTable = new Lst<String[]>();

    
    JDXDataObject spectrum = new Spectrum();
    spectrum.setTitle(map.get("##TITLE"));
    spectrum.setJcampdx("5.01");
    spectrum.setDataClass("XYDATA");
    spectrum.setDataType("NMR SPECTRUM");
    spectrum.setContinuous(true);
    spectrum.setIncreasing(false);
    spectrum.setYFactor(1);
    spectrum.setLongDate(map.get("##$DATE"));
    spectrum.setOrigin("Bruker BioSpin GmbH/JSpecView");
    spectrum.setOwner(map.get("##OWNER"));
    String sfreq = map.get("##$SFO1");
    double freq = Double.parseDouble(sfreq);
    String shiftRef = map.get("##$ABSF1");
    String nuc = cleanValue(map.get("##$NUC1"));
    double ref = Double.parseDouble(shiftRef);
    double sw_hz = Double.parseDouble(map.get("##$SWH"));
    double sw = sw_hz/freq;
    double xfactor = sw_hz/data.length;
    String solvent = cleanValue(map.get("##$SOLVENT"));
    String shiftType = "INTERNAL";
    JDXReader.addHeader(LDRTable, "##.SHIFTREFERENCE", shiftType + ", " + solvent + ", 1, " + "INTERNAL, CDCl3, 1, " + shiftRef);
    JDXReader.addHeader(LDRTable, "##.OBSERVEFREQUENCY", sfreq);
    JDXReader.addHeader(LDRTable, "##.OBSERVENUCLEUS", nuc);
    JDXReader.addHeader(LDRTable, "##SPECTROMETER/DATA SYSTEM",
        cleanValue(map.get("##$INSTRUM")));
    spectrum.setHeaderTable(LDRTable);

    spectrum.setObservedFreq(freq);
    spectrum.setHZtoPPM(true);

    int npoints = data.length;
    Coordinate[] xyCoords = new Coordinate[npoints];

    double shift = ref - sw;
    //   for ease of plotting etc. all data is stored internally in increasing order
    for (int i = 0; i < npoints; i++) {
      xyCoords[i] = new Coordinate().set((npoints - i - 1)*xfactor/freq+shift, data[i]);
    }
    xyCoords = Coordinate.reverse(xyCoords);
    spectrum.setXYCoords(xyCoords);
    spectrum.fileFirstX = sw_hz - xfactor;
    spectrum.fileLastX = 0;
    spectrum.fileNPoints = npoints;
    spectrum.setXFactor(xfactor);
    spectrum.setXUnits("ppm");
    spectrum.setYUnits("ARBITRARY UNITS");
    spectrum.setNumDim(1);
    spectrum.setObservedNucleus(nuc);
    source.addJDXSpectrum(null, (Spectrum) spectrum, false);
  }

  private String cleanValue(String val) {
    return (val == null ? "" : val.startsWith("<") ? val.substring(1, val.length() - 1) : val);
  }

  private double[] getData(File file, int dtypp, int byteorp)
      throws IOException {
    int len = (int) file.length() / (dtypp == TYPE_INT ? 4 : 8);
    BinaryDocument doc = new BinaryDocument();
    doc.setStream(new BufferedInputStream(new FileInputStream(file)),
        byteorp != 0);
    double[] ad = new double[len];
    double d = 0;
    double dmin = Double.MAX_VALUE, dmax = -Double.MAX_VALUE;
    if (dtypp == TYPE_INT) {
      for (int i = 0; i < len; i++) {
        double f = 1;//Math.pow(2, np);
        ad[i] = d = doc.readInt() * f;
        if (d < dmin)
          dmin = d;
        if (d > dmax)
          dmax = d;
      }
    } else {
      for (int i = 0; i < len; i++) {
        ad[i] = d = doc.readDouble();
        if (d < dmin)
          dmin = d;
        if (d > dmax)
          dmax = d;
      }
    }
    doc.close();
    return ad;
  }

}
