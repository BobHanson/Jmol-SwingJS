/*
 * JUnit TestCase for the Smarter Adapter
 */

package org.jmol.adapter.smarter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;

import javajs.util.BinaryDocument;
import javajs.util.PT;
import javajs.util.Rdr;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jmol.util.JUnitLogger;
import org.jmol.util.Logger;

public class TestSmarterJmolAdapter extends TestSuite {

  private String datafileDirectory = "../Jmol-datafiles";

  public TestSmarterJmolAdapter() {
    super();
  }

  public TestSmarterJmolAdapter(Class<?> theClass, String name) {
    super(theClass, name);
  }

  public TestSmarterJmolAdapter(Class<?> theClass) {
    super(theClass);
  }

  public TestSmarterJmolAdapter(String name) {
    super(name);
  }

  static String testOne;
  
  /**
   * @return Test suite containing tests for all files
   */
  public static Test suite() {
    TestSmarterJmolAdapter result = new TestSmarterJmolAdapter("Test for org.jmol.adapter.smarter.SmarterJmolAdapter");
    result.datafileDirectory = System.getProperty("test.datafile.directory", result.datafileDirectory);
    //result.addDirectory(false, "abint", "out");
    //result.addDirectory(false, "aces2", "dat");
    //result.addDirectory(false, "aces2", "out");
    
    //testOne = "zmatrix";
    
    result.addDirectory("adf", "adf;out", "Adf");
    result.addDirectory("aims", "in", "Aims");
    result.addDirectory("aminoacids", "mol", "Mol");
    result.addDirectory("aminoacids", "pdb", "Pdb");
    result.addDirectory("animations", "cml", "XmlCml");
    result.addDirectory("animations", "pdb;pdb.gz", "Pdb");
    result.addDirectory("animations", "xyz", "Xyz");
    result.addDirectory("castep", "cell;phonon", "Castep");
    result.addDirectory("cif", "mmcif", "MMCif");
    result.addDirectory("cif", "cif", "Cif");
    result.addDirectory("cif", "mmtf", "MMTF");
    result.addDirectory("c3xml", "c3xml", "XmlChem3d");
    result.addDirectory("cml", "cml", "XmlCml");
    result.addDirectory("crystal", "out;outp", "Crystal");
    result.addDirectory("crystals", "mol", "Mol");
    result.addDirectory("crystals", "pdb", "Pdb");
    result.addDirectory("csf", "csf", "Csf");
    result.addDirectory("cube",  "cub.gz;cube.gz", "Cube");
    result.addDirectory("dgrid",  "adf", "Dgrid");
    result.addDirectory("dmol",  "outmol", "Dmol");
    result.addDirectory("folding", "xyz;xyz.gz", "FoldingXyz");
    result.addDirectory("../Jmol-FAH/projects", "xyz;xyz.gz", "FoldingXyz");
    result.addDirectory("gamess", "log;out", ";Gamess;GamessUS;GamessUK;");
    result.addDirectory("gaussian", "log;out", "Gaussian");
    result.addDirectory("gennbo", "out;36;37", "GenNBO");
    result.addDirectory("ghemical", "gpr", "GhemicalMM");
    result.addDirectory("gromacs", "gro", "Gromacs");
    result.addDirectory("gulp", "gout;got", "Gulp");
    result.addDirectory("hyperchem", "hin", "HyperChem");
    result.addDirectory("hyperchem", "hpr", "HyperChem");
    result.addDirectory("jaguar", "out", "Jaguar");
    result.addDirectory("modifiedGroups", "cif", "MMCif");
    result.addDirectory("modifiedGroups", "pdb", "Pdb");
    result.addDirectory("mol", "v3000;mol;sdf", "Mol");
    result.addDirectory("mol2", "mol2", "Mol2");
    result.addDirectory("molpro", "xml", "XmlMolpro");
    result.addDirectory("mopac", "arc;archive", "MopacArchive");
    result.addDirectory("mopac", "out", "Mopac");
    result.addDirectory("mopac", "gpt2", "MopacGraphf");
    result.addDirectory("mopac", "mgf", "MopacGraphf");
    result.addDirectory("odyssey", "odydata", "Odyssey");
    result.addDirectory("odyssey", "xodydata", "XmlOdyssey");
    result.addDirectory("nwchem", "nwo", "NWChem");
    result.addDirectory("pdb", "pdb;pdb.gz", "Pdb");
    result.addDirectory("pymol", "pse", "PyMOL");
    // result.pmesh files are not molecular data files
    result.addDirectory("quantumEspresso", "out", "Espresso");
    result.addDirectory("psi3", "out", "Psi");
    result.addDirectory("qchem", "out", "Qchem");
    result.addDirectory("shelx", "res", "Shelx");
    result.addDirectory("siesta", "fdf;out", "Siesta");    
    result.addDirectory("spartan", "smol", "SpartanSmol");
    result.addDirectory("spartan", "txt;sp4", "Spartan");
    result.addDirectory("sparchive", "sparchive;spartan", "Spartan");
    result.addDirectory("vasp", "xml", "XmlVasp");
    result.addDirectory("vasp", "dat", "VaspOutcar");
    result.addDirectory("vasp", "poscar", "VaspPoscar");
    result.addDirectory("wien2k", "struct", "Wien2k");
    result.addDirectory("webmo", "mo", "WebMO");
    result.addDirectory("xsd", "xsd", "XmlXsd");
    result.addDirectory("xyz", "xyz", "Xyz");
    result.addDirectory("zmatrix", "txt;zmat", "Input");
    result.addDirectory("zmatrix", "inp", "=Input"); // force InputReader and no other
    return result;
  }

  /**
   * Add tests for each file in a directory.
   * @param directory
   *        Directory where the files are (relative to Jmol-datafiles)
   * @param ext
   *        Extension
   * @param typeAllowed
   *        Allowed file type
   */
  private void addDirectory(String directory, String ext, String typeAllowed) {

    // Checking files
    if (testOne != null && !directory.equals(testOne))
      return;
    File dir = new File(datafileDirectory, directory);
    String[] exts = PT.split(ext, ";");
    for (int ie = 0; ie < exts.length; ie++) {
      final String e = exts[ie];
      String[] files = dir.list(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.endsWith("." + e);
        }
      });
      if (files == null) {
        Logger.warn("No files in directory [" + directory + "] for extension ["
            + e + "]");
      } else {
        for (int i = 0; i < files.length; i++)
          addFile(e.endsWith(".gz"), directory, files[i], typeAllowed);
      }
    }
  }

  /**
   * Add test for a file.
   * 
   * @param gzipped Compressed file ?
   * @param directory Directory where the files are (relative to Jmol-datafiles)
   * @param filename File name
   * @param typeAllowed string that must contain the determined file type
   */
  private void addFile(boolean gzipped,
                       String directory,
                       String filename,
                       String typeAllowed) {

    File file = new File(new File(datafileDirectory, directory), filename);
    Test test = new TestSmarterJmolAdapterImpl(file, gzipped, typeAllowed);
    addTest(test);
  }
}

/**
 * Implementation of a test reading only one file. 
 */
class TestSmarterJmolAdapterImpl extends TestCase {

  private File file;
  private boolean gzipped;
  private String typeAllowed;
  private boolean mustForce;

  public TestSmarterJmolAdapterImpl(File file, boolean gzipped, String typeAllowed) {
    super("testFile");
    this.file = file;
    this.gzipped = gzipped;
    this.mustForce = (typeAllowed.startsWith("="));    
    if (mustForce)
      typeAllowed = typeAllowed.substring(1);
    this.typeAllowed = typeAllowed;
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#runTest()
   */
  @Override
  public void runTest() throws Throwable {
    testFile();
  }

  private static boolean continuing = true;

  /**
   * Tests reading of one file.
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  public void testFile() throws FileNotFoundException, IOException {
    if (!continuing)
      return;
    String fname = file.getCanonicalPath();
    System.out.println(fname);
    JUnitLogger.setInformation(file.getPath());
    InputStream iStream = new FileInputStream(file);
    if (gzipped)
      iStream = new GZIPInputStream(iStream, 512);
    BufferedInputStream bis = new BufferedInputStream(iStream);
    Logger.info(file.getPath());
    SmarterJmolAdapter adapter = new SmarterJmolAdapter();
    String type = null;
    boolean ok = true;
    Object reader;
    String fileType = adapter.getFileTypeName(bis);
    System.out.println(fileType);
    if (fileType == null) {
      reader = Rdr.getBufferedReader(bis,  null);
      fileType = adapter.getFileTypeName(reader);
    } else {
      //PyMOL or MMTF
      reader = new BinaryDocument().setStream(bis, true);
    }
    ok = (typeAllowed.equals(fileType) || typeAllowed.indexOf(";" + fileType
        + ";") >= 0);
    if (ok == mustForce) {
      continuing = false;
      fail("Wrong type for " + file.getPath() + ": " + fileType
          + " instead of " + typeAllowed);
    }
    if (mustForce)
      type = typeAllowed;
    Hashtable<String, Object> htParams = new Hashtable<String, Object>();
    htParams.put("fullPathName", fname);
    Object result = adapter.getAtomSetCollectionFromReaderType(file.getName(),
        type, reader, htParams);
    System.out.println(result);
    continuing = (result != null && result instanceof AtomSetCollection);
    assertNotNull("Nothing read for " + file.getPath(), result);
    assertFalse("Error returned for " + file.getPath() + ": " + result,
        result instanceof String);
    assertTrue("Not an AtomSetCollection for " + file.getPath(),
        result instanceof AtomSetCollection);
    int nAtoms = ((AtomSetCollection) result).ac;
    continuing &= (nAtoms > 0);
    assertTrue("No atoms loaded for " + file.getPath(), nAtoms > 0);
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#getName()
   */
  @Override
  public String getName() {
    if (file != null) {
      return super.getName() + " [" + file.getPath() + "]";
    }
    return super.getName();
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    JUnitLogger.activateLogger();
    JUnitLogger.setInformation(null);
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    JUnitLogger.setInformation(null);
    file = null;
    typeAllowed = null;
    mustForce = false;
  }
}
