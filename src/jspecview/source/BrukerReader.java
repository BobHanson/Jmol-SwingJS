package jspecview.source;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javajs.util.BinaryDocument;
import javajs.util.Lst;
import javajs.util.Rdr;
import jspecview.common.Coordinate;
import jspecview.common.Spectrum;

public class BrukerReader {

  private static final int TYPE_INT = 0;

  public BrukerReader() { // for dynamic loading
  }
  
  private final static String[] zipList = new String[]{"acqus", "procs", "1r", "title" };
  
  public JDXSource readBrukerZip(byte[] bytes, String fullPath)
      throws FileNotFoundException, Exception {
    try {
      ZipInputStream zis = new ZipInputStream(
          bytes == null ? new FileInputStream(fullPath)
              : new ByteArrayInputStream(bytes));
      ZipEntry ze;
      Map<String, String> map = new Hashtable<String, String>();
      byte[] data1r = null;
      String root = null;
      String title = null;
      out: while ((ze = zis.getNextEntry()) != null) {
        String zeName = ze.getName();
        int pt = zeName.lastIndexOf('/');
        String zeShortName = zeName.substring(pt + 1);
        if (root == null) {
          root = zeName.substring(0, pt + 1);
          pt = root.indexOf("/pdata/");
          if (pt >= 0)
            root = root.substring(0, pt + 1);
        }
        // Try to stay within a set. Not sure how this will work with multiple pdata
        if (!zeName.startsWith(root))
          break out;
        if (zeShortName.equals("title")) {
          title = new String(getBytes(zis, (int) ze.getSize(), false));
          map.put("##title", title);
        } else if (zeShortName.equals("1r")) {
          data1r = getBytes(zis, (int) ze.getSize(), false);
        } else if (zeShortName.equals("procs") || zeShortName.equals("acqus")) {
          JDXReader.getHeaderMap(
              new ByteArrayInputStream(getBytes(zis, (int) ze.getSize(), false)), map);
        }
      }
      zis.close();
      map.put("##TITLE", title);
      return getSource(data1r, map, fullPath);
   } catch (Exception e) {
      return null;
    }
  }

  
  /**
   * Read a Bruker directory looking for 1r.
   * 
   * CURRENTLY will fail to read if 1rr is present. 
   * 
   * @param fullPath
   *        any file in the directory containing procs or acqus or the pdata
   *        directory or a numbered pdata subdirectory, or the directory
   *        containing acqus
   * @return source
   * @throws FileNotFoundException
   * @throws Exception
   */
  public JDXSource readBrukerDir(String fullPath)
      throws FileNotFoundException, Exception {
    File dir = new File(fullPath);
    if (!dir.isDirectory()) {
      dir = dir.getParentFile();
    }
    File procs = new File(dir, "procs");
    if (!procs.exists())
      procs = new File(dir, "pdata/1/procs");
    File pdata = procs.getParentFile();
    String brukerDir = pdata.getParentFile().getParent();
    Map<String, String> map = new Hashtable<String, String>();
    InputStream is = new FileInputStream(new File(brukerDir, "acqus"));
    JDXReader.getHeaderMap(is, map);
    is.close();
    is = new FileInputStream(procs);
    JDXReader.getHeaderMap(is, map);
    is.close();
    map.put("##TITLE", new String(getFileContentsAsBytes(new File(pdata, "title"))));
    byte[] data1r = getFileContentsAsBytes(new File(procs.getParent(), "1r"));
    return getSource(data1r, map, brukerDir);
  }

  private JDXSource getSource(byte[] data1r, Map<String, String> map, String brukerDir) throws IOException {
    //int np = Integer.parseInt(map.get("##$NCPROC")); not nec.
    int dtypp = Integer.parseInt(map.get("##$DTYPP"));
    int byteorp = (dtypp == TYPE_INT ? Integer.parseInt(map.get("##$BYTORDP"))
        : Integer.MAX_VALUE);
    if (dtypp == Integer.MIN_VALUE || byteorp == Integer.MIN_VALUE)
      return null;
    double[] data = getData(data1r, dtypp, byteorp);
    JDXSource source = new JDXSource(JDXSource.TYPE_SIMPLE, brukerDir);
    setSource(data, map, source);
    return source;
  }


  private byte[] getFileContentsAsBytes(File file)
      throws FileNotFoundException, IOException {
    if (!file.exists())
      return new byte[0];
    int len = (int) file.length();
    return getBytes(new FileInputStream(file), len, true);
  }

  private byte[] getBytes(InputStream in, int len, boolean andClose) {
    byte[] bytes = new byte[len];
    try {
      int pos = 0;
      while (len > 0) {
        int n = in.read(bytes, pos, len);
        if (n < 0)
          break;
        len -= n;
        pos += n;
      }
      if (andClose)
        in.close();
      return bytes;
    } catch (Exception e) {
    }
    return new byte[0];
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
    double sw = sw_hz / freq;
    double xfactor = sw_hz / data.length;
    String solvent = cleanValue(map.get("##$SOLVENT"));
    String shiftType = "INTERNAL";
    JDXReader.addHeader(LDRTable, "##.SHIFTREFERENCE", shiftType + ", "
        + solvent + ", 1, " + "INTERNAL, CDCl3, 1, " + shiftRef);
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
      xyCoords[i] = new Coordinate()
          .set((npoints - i - 1) * xfactor / freq + shift, data[i]);
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
    if (spectrum.getMaxY() >= 10000)
      spectrum.normalizeSimulation(1000);
    source.addJDXSpectrum(null, (Spectrum) spectrum, false);
  }

  private String cleanValue(String val) {
    return (val == null ? ""
        : val.startsWith("<") ? val.substring(1, val.length() - 1) : val);
  }

  private double[] getData(byte[] bytes, int dtypp, int byteorp)
      throws IOException {
    int len = bytes.length / (dtypp == TYPE_INT ? 4 : 8);
    BinaryDocument doc = new BinaryDocument();
    doc.setStream(new BufferedInputStream(new ByteArrayInputStream(bytes)),
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
