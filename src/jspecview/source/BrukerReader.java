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
import jspecview.common.Coordinate;
import jspecview.common.Spectrum;

/**
 * A class to read Bruker ZIP files and directories. The first acqus file found
 * in a ZIP file sets the spectrum for pdata/1. In the case of a (Java) file
 * directory, any(?) file in the main (numbered) Bruker directory or any pdata
 * directory can be targeted.
 * 
 * The File...Add File... chooser in Java now accepts directories and in that case
 * assumes they are Bruker directories. 
 * 
 * (There had been some code in here for reading ZIP files as collections of JDX spectra,
 * but we can't remember why that was there, and actually it didn't work anyway.)
 * 
 * 2D data may be off by a fraction of a Hz in the F1 (outer) dimension. This is
 * because I did not figure out how Bruker is arriving at FREQUENCY1.first.
 * Tests showed a 0.3 Hz offset. This is in the 6th or 7th decimal place, so
 * presumably that is not significant.
 * 
 * 2D reading has (never) been checked for cases where the Y axis is J. 
 * 
 * @author hansonr
 */
public class BrukerReader {

  private static final int TYPE_INT = 0;

  public BrukerReader() { // for dynamic loading
  }

  boolean allowPhasing = false;

  /**
   * Read through a ZIP file (Java or JavaScript) looking for title, acqus, procs, 1r, 1i,
   * 2rr. Parameters from title, acqus, and procs are put into a Map. 1r, 1i,
   * and 2rr are read as raw bytes and converted to doubles later.
   * 
   * An attempt is made to retreive files from the same folder holding acqu.
   * 
   * Currently, we are skipping 1i.
   * 
   * @param bytes raw ZIP byte data
   * @param fullPath
   * @return a JDXSource, which may hold multiple subspectra
   * @throws FileNotFoundException
   * @throws Exception
   */
  public JDXSource readBrukerZip(byte[] bytes, String fullPath)
      throws FileNotFoundException, Exception {
    try {
      ZipInputStream zis = new ZipInputStream(
          bytes == null ? new FileInputStream(fullPath)
              : new ByteArrayInputStream(bytes));
      ZipEntry ze;
      Map<String, String> map = new Hashtable<String, String>();
      byte[] data1r = new byte[0];
      byte[] data1i = new byte[0];
      byte[] data2rr = new byte[0];
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
        boolean isacq = false;
        if (zeShortName.equals("title")) {
          title = new String(getBytes(zis, (int) ze.getSize(), false));
          map.put("##title", title);
        } else if (zeShortName.equals("1r")) {
          data1r = getBytes(zis, (int) ze.getSize(), false);
        } else if (zeShortName.equals("1i")) {
          if (allowPhasing) 
            data1i = getBytes(zis, (int) ze.getSize(), false);
        } else if (zeShortName.equals("2rr")) {
          data2rr = getBytes(zis, (int) ze.getSize(), false);
        } else if (zeShortName.equals("proc2s") || zeShortName.equals("acqu2s")) {
          JDXReader.getHeaderMapS(new ByteArrayInputStream(
              getBytes(zis, (int) ze.getSize(), false)), map, "_2");
        } else if (zeShortName.equals("procs")
            || (isacq = zeShortName.equals("acqus"))) {
          if (isacq) {
            // set the root directory more specifically if we have found acqus
            // in principle, zip file directories do not have to be in any particular order,
            // but in practice they are always in order, so acqu will be hit first
            root = zeName.substring(0, pt + 1);
          }
          JDXReader.getHeaderMap(new ByteArrayInputStream(
              getBytes(zis, (int) ze.getSize(), false)), map);
        }
      }
      zis.close();
      map.put("##TITLE", title);
      return getSource(fullPath, map, data1r, data1i, data2rr);
    } catch (Exception e) {
      return null;
    }
  }

  
  /**
   * Read through a Bruker directory (Java only) looking for title, acqus, procs, 1r, 1i,
   * 2rr. Parameters from title, acqus, and procs are put into a Map. 1r, 1i,
   * and 2rr are read as raw bytes and converted to doubles later.
   * 
   * An attempt is made to retreive files from the same folder holding acqu.
   * 
   * Currently, we are skipping 1i.
   * 
   * @param fullPath
   * @return a JDXSource, which may hold multiple subspectra
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
    File brukerDir = pdata.getParentFile().getParentFile();
    Map<String, String> map = new Hashtable<String, String>();
    mapParameters(brukerDir, "acqus", map, null);
    mapParameters(brukerDir, "acqu2s", map, "_2");
    mapParameters(pdata, "procs", map, null);
    mapParameters(pdata, "proc2s", map, "_2");
    map.put("##TITLE", new String(getFileContentsAsBytes(new File(pdata, "title"))));
    byte[] data1r = getFileContentsAsBytes(new File(procs.getParent(), "1r"));
    byte[] data1i = (allowPhasing ? getFileContentsAsBytes(new File(procs.getParent(), "1i")) : new byte[0]);
    byte[] data2rr = getFileContentsAsBytes(new File(procs.getParent(), "2rr"));
    return getSource(brukerDir.toString(), map, data1r, data1i, data2rr);
  }

  private void mapParameters(File dir, String fname,
                             Map<String, String> map, String suffix) throws Exception {
    File f = new File(dir, fname);
    if (!f.exists())
      return;
    InputStream is = new FileInputStream(f);
    JDXReader.getHeaderMapS(is, map, suffix);
    is.close();
  }


  /**
   * Using the raw byte data for 1r or 2rr, create a JDXSource that is either
   * TYPE_SIMPLE (1D) or TYPE_BLOCK (2D). This could be changed to TYPE_NTUPLE
   * for 1D if it is desired to also include 1i.
   * 
   * The raw byte data are transformed to double. The NCPROC parameter is not
   * needed, as it only defines the integer data scaling, and that is not of any
   * particular need here, since we will scale anyway.
   * 
   * @param brukerDir
   * @param map
   * @param data1r
   * @param data1i
   * @param data2rr
   * @return a JDXSource
   * @throws IOException
   */
  private JDXSource getSource(String brukerDir, Map<String, String> map,
                              byte[] data1r, byte[] data1i, byte[] data2rr)
      throws IOException {
    //int np = Integer.parseInt(map.get("##$NCPROC")); not nec.
    //int np = Integer.parseInt(map.get("##$NCPROC")); not nec.
    int dtypp = Integer.parseInt(map.get("##$DTYPP"));
    int byteorp = (dtypp == TYPE_INT ? Integer.parseInt(map.get("##$BYTORDP"))
        : Integer.MAX_VALUE);
    if (dtypp == Integer.MIN_VALUE || byteorp == Integer.MIN_VALUE)
      return null;
    JDXSource source = null;
    if (data1r.length > 0) {
      // could add data1i here and make this TYPE_NTUPLE
      source = new JDXSource(
          (data1i.length == 0 ? JDXSource.TYPE_SIMPLE : JDXSource.TYPE_NTUPLE),
          brukerDir);
      setSource(getData(data1r, dtypp, byteorp),
          getData(data1i, dtypp, byteorp), map, source, false);
    } else if (data2rr.length > 0) {
      source = new JDXSource(JDXSource.TYPE_NTUPLE, brukerDir);
      setSource(getData(data2rr, dtypp, byteorp), null, map, source, true);
    }
    return source;
  }
  
  /**
   * Add spectra to a source based on data and
   * 
   * @param datar
   *        real data
   * @param datai
   *        imaginary data
   * @param map
   * @param source
   * @param is2D
   */
  private void setSource(double[] datar, double[] datai,
                         Map<String, String> map, JDXSource source,
                         boolean is2D) {
    Lst<String[]> LDRTable = new Lst<String[]>();

    Spectrum spectrum0 = new Spectrum();
    spectrum0.setTitle(map.get("##TITLE"));
    spectrum0.setJcampdx(is2D ? "6.0" : "5.1");
    spectrum0.setDataClass("XYDATA");
    spectrum0.setDataType(is2D ? "nD NMR SPECTRUM" : "NMR SPECTRUM");
    spectrum0.setContinuous(true);
    spectrum0.setIncreasing(false);
    spectrum0.setLongDate(map.get("##$DATE"));
    spectrum0.setOrigin("Bruker BioSpin GmbH/JSpecView");
    spectrum0.setOwner(map.get("##OWNER"));
    double freq = parseDouble(map.get("##$SFO1"));
    double ref = parseDouble(map.get("##$ABSF1"));
    if (ref == 0) {
      // hack for Xwin-NMR? 8prenyl from Robert Lancashire has ##$ABSF1=0
      ref = parseDouble(map.get("##$OFFSET"));
    }
    String nuc1 = cleanJDXValue(map.get("##$NUC1"));
    String nuc2 = cleanJDXValue(map.get("##$NUC2"));
    if (nuc2.length() == 0)
      nuc2 = nuc1;
    double sw_hz = parseDouble(map.get("##$SWP"));
    double sw = sw_hz / freq;
    double shift = ref - sw;
    String solvent = cleanJDXValue(map.get("##$SOLVENT"));
    String shiftType = "INTERNAL";
    JDXReader.addHeader(LDRTable, "##.SHIFTREFERENCE",
        shiftType + ", " + solvent + ", 1, " + ref);
    JDXReader.addHeader(LDRTable, "##.OBSERVEFREQUENCY", "" + freq);
    JDXReader.addHeader(LDRTable, "##.OBSERVENUCLEUS", nuc1);
    JDXReader.addHeader(LDRTable, "##SPECTROMETER/DATA SYSTEM",
        cleanJDXValue(map.get("##$INSTRUM")));
    spectrum0.setHeaderTable(LDRTable);

    spectrum0.setObservedNucleus(nuc1);
    spectrum0.setObservedFreq(freq);
    spectrum0.setHZtoPPM(true);
    if (is2D) {
      source.isCompoundSource = true;
      spectrum0.setNumDim(2);
      spectrum0.setNucleusAndFreq(nuc2, false);
      // from nmrglue bruker.py
      int si0 = Integer.parseInt(map.get("##$SI"));
      int si1 = Integer.parseInt(map.get("##$SI_2"));
      //int xdim0 = Math.min(si0, Integer.parseInt(map.get("##$XDIM")));
      //int xdim1 = Math.min(si1, Integer.parseInt(map.get("##$XDIM_2")));
      // assuming here that these are full in-memory, with no padding and no submatrices
      //      int[] shape = new int[] {si1, si0};
      //      int[] submatrix_shape = new int[] {xdim1, xdim0};
      //      int n1 = si1/xdim1;
      //      int n2 = si0/xdim0;
      //      int nsubs = n1 * n2;
      //      (si_1, si_0), (xdim_1, xdim_0)

      //      ##NTUPLES=  nD NMR SPECTRUM
      //          ##VAR_NAME= FREQUENCY1, FREQUENCY2, SPECTRUM
      //          ##SYMBOL= F1, F2, Y
      //          ##.NUCLEUS= 13C,  1H
      //          ##VAR_TYPE= INDEPENDENT,  INDEPENDENT,  DEPENDENT
      //          ##VAR_FORM= AFFN, AFFN, ASDF
      //          ##VAR_DIM=  2048, 1640, 3358720
      //          ##UNITS=  HZ, HZ, ARBITRARY UNITS
      //          ##FIRST=  22628,  4996.51,  2.47524e-07
      //          ##LAST= -2522.92, -998.691, 5.6322e-07
      //          ##MIN=  -2522.92, -998.691, 1.21577e-10
      //          ##MAX=  22628,  4996.51,  0.00500477
      //          ##FACTOR= 1,  1,  5.17736e-09

      //      ##PAGE= F1=22628
      //          ##FIRST=  22628,  4996.51,  2.47524e-07

      //      ##PAGE= F1=22603.4
      //          ##FIRST=  22628,  4996.51,  1.7571e-07
      // ...      
      //      ##PAGE= F1=-27673.8
      //          ##FIRST=  22628,  4996.51,  0

      double ref1 = parseDouble(map.get("##$ABSF1_2"));
      if (ref1 == 0) {
        // hack for Xwin-NMR? 8prenyl from Robert Lancashire has ##$ABSF1=0
        ref1 = parseDouble(map.get("##$OFFSET"));
      }
      double freq1 = parseDouble(map.get("##$SFO1_2"));
      double sw_hz1 = parseDouble(map.get("##$SWP_2"));
//      double sw1 = sw_hz1 / freq1;
//      double shift1 = ref1 - sw1;
      int npoints = si0;
      double xfactor = sw_hz / npoints;
      double xfactor1 = sw_hz1 / si1;
      double freq2 = freq1;
      freq1 = ref1 * freq1 - xfactor1;
      spectrum0.fileNPoints = npoints;
      spectrum0.fileFirstX = sw_hz - xfactor;
      spectrum0.fileLastX = 0;
//      int nc = Integer.parseInt(map.get("##$NCPROC_2"));
      double f = 1;//Math.pow(2,  nc);
      for (int j = 0, pt = 0; j < si1; j++) {
        Spectrum spectrum = new Spectrum();
        spectrum0.copyTo(spectrum);
        spectrum.setTitle(spectrum0.getTitle());
        spectrum.setY2D(freq1);
        spectrum.blockID = Math.random();
        spectrum0.fileNPoints = npoints;
        spectrum0.fileFirstX = sw_hz - xfactor;
        spectrum0.fileLastX = 0;
        // TODO 2D J might not have FREQUENCY1 here.
        spectrum.setY2DUnits("HZ");
        spectrum.setXFactor(1);
        spectrum.setYFactor(1);
        spectrum.setObservedNucleus(nuc2);
        spectrum.setObservedFreq(freq2);
        Coordinate[] xyCoords = new Coordinate[npoints];
        //   for ease of plotting etc. all data is stored internally in increasing order
        for (int i = 0; i < npoints; i++) {
          xyCoords[npoints - i - 1] = new Coordinate()
              .set((npoints - i) * xfactor / freq + shift, datar[pt++] * f);
        }
        spectrum.setXYCoords(xyCoords);
        source.addJDXSpectrum(null, spectrum, j > 0);
        freq1 -= xfactor1;
      }
    } else {
      int npoints = datar.length;
      double xfactor = sw_hz / npoints;
      spectrum0.fileFirstX = sw_hz - xfactor;
      spectrum0.fileLastX = 0;
      spectrum0.fileNPoints = npoints;
      Coordinate[] xyCoords = new Coordinate[npoints];
      //   for ease of plotting etc. all data is stored internally in increasing order
      for (int i = 0; i < npoints; i++) {
        xyCoords[npoints - i - 1] = new Coordinate()
            .set((npoints - i - 1) * xfactor / freq + shift, datar[i]);
      }
      spectrum0.setXYCoords(xyCoords);
      spectrum0.fileNPoints = npoints;
      spectrum0.setXFactor(xfactor);
      spectrum0.setYFactor(1);
      spectrum0.setXUnits("ppm");
      spectrum0.setYUnits("ARBITRARY UNITS");
      spectrum0.setNumDim(1);
      if (spectrum0.getMaxY() >= 10000)
        spectrum0.normalizeSimulation(1000);
      source.addJDXSpectrum(null, spectrum0, false);
    }
  }

  private static double parseDouble(String val) {
    return (val == null || val.length() == 0 ? Double.NaN : Double.parseDouble(val));
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

  private String cleanJDXValue(String val) {
    String s = (val == null ? ""
        : val.startsWith("<") ? val.substring(1, val.length() - 1) : val);
    return (s.equals("off") ? "" : s);
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

}
