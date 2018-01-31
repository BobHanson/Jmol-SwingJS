/*
 * JUnit TestCase for running various Jmol scripts
 */

package org.jmol.api;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.util.JUnitLogger;
import org.jmol.util.Profiling;
import org.openscience.jmol.app.Jmol;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * TestSuite for running various Jmol scripts. 
 */
public class TestScripts extends TestSuite {

  public TestScripts() {
    super();
  }

  public TestScripts(Class<?> theClass, String name) {
    super(theClass, name);
  }

  public TestScripts(Class<?> theClass) {
    super(theClass);
  }

  public TestScripts(String name) {
    super(name);
  }

  /**
   * @return Test suite containing tests for all scripts.
   */
  public static Test suite() {
    boolean performance = Boolean.getBoolean("test.performance");
    TestScripts result = new TestScripts("Test for scripts");
    String datafileDirectory = System.getProperty(
        "test.datafile.script.directory",
        "../Jmol-datafiles/tests/scripts");
    TestScripts resultCheck = new TestScripts("Test for checking scripts");
    resultCheck.addDirectory(datafileDirectory + "/check", true, performance);
    if (resultCheck.countTestCases() > 0) {
      result.addTest(resultCheck);
    }
    /*
    TestScripts resultCheckPerformance = new TestScripts("Test for checking scripts with performance testing");
    resultCheckPerformance.addDirectory(datafileDirectory + "/check_performance", true, true);
    if (resultCheckPerformance.countTestCases() > 0) {
      result.addTest(resultCheckPerformance);
    }
    */
    TestScripts resultRun = new TestScripts("Test for running scripts");
    resultRun.addDirectory(datafileDirectory + "/run", false, performance);
    if (resultRun.countTestCases() > 0) {
      result.addTest(resultRun);
    }
    TestScripts resultRunPerformance = new TestScripts("Test for running scripts with performance testing");
    resultRunPerformance.addDirectory(datafileDirectory + "/run_performance", false, true);
    if (resultRunPerformance.countTestCases() > 0) {
      result.addTest(resultRunPerformance);
    }
    return result;
  }

  /**
   * Add tests for each script in a directory.
   * 
   * @param directory Directory where the files are
   * @param checkOnly Flag for checking syntax only
   * @param performance Flag for checking performance
   */
  private void addDirectory(String directory,
                            boolean checkOnly,
                            boolean performance) {

    // Checking files
    File dir = new File(directory);
    String[] files = dir.list(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {
        if (name.endsWith(".spt")) {
          return true;
        }
        return false;
      }

    });
    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        addFile(directory, files[i], checkOnly, performance);
      }
    }

    // Checking sub directories
    String[] dirs = dir.list(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {
        File file = new File(dir, name);
        return file.isDirectory();
      }

    });
    if (dirs != null) {
      for (int i = 0; i < dirs.length; i++) {
        addDirectory(
            new File(directory, dirs[i]).getAbsolutePath(),
            checkOnly, performance);
      }
    }
  }

  /**
   * Add test for a file.
   * 
   * @param directory Directory where the files are
   * @param filename File name
   * @param checkOnly Flag for checking syntax only
   * @param performance Flag for checking performance
   */
  private void addFile(String directory,
                       String filename,
                       boolean checkOnly,
                       boolean performance) {

    File file = new File(directory, filename);
    Test test = new TestScriptsImpl(file, checkOnly, performance);
    addTest(test);
  }
}

/**
 * Implementation of a test running only one script. 
 */
class TestScriptsImpl extends TestCase {

  private File file;
  private boolean checkOnly;
  private boolean performance;

  private final int nbExecutions;

  public TestScriptsImpl(File file, boolean checkOnly, boolean performance) {
    super("testFile");
    this.file = file;
    this.checkOnly = checkOnly;
    this.performance = performance;
    int nbExec = 1;
    try {
      nbExec = Integer.parseInt(System.getProperty("test.nbExecutions", "1"));
    } catch (NumberFormatException e) {
      //
    }
    this.nbExecutions = nbExec;
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#runTest()
   */
  @Override
  public void runTest() throws Throwable {
    testScript();
  }

  /**
   * Execute test for a script.
   */
  public void testScript() {
    JUnitLogger.setInformation(file.getPath());
    if (performance) {
      runPerformanceTest();
      return;
    }
    runSimpleTest();
  }

  /**
   * Executes a performance test.
   */
  public void runPerformanceTest() {
    JFrame frame = new JFrame();
    Map<String, Object> viewerOptions = new Hashtable<String, Object>();
    if (checkOnly)
      viewerOptions.put("check", Boolean.TRUE);
    Jmol jmol = Jmol.getJmol(frame, 500, 500, viewerOptions);
    JmolViewer viewer = jmol.vwr;
    long beginFull = Profiling.getTime();
    for (int i = 0; i < nbExecutions; i++) {
      viewer.scriptWaitStatus("set defaultDirectory \"" + file.getParent().replace('\\', '/') + "\"", "");
      int lineNum = 0;
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new FileReader(file));
        String line = null;
        long beginScript = Profiling.getTime();
        while ((line = reader.readLine()) != null) {
          lineNum++;
          long begin = Profiling.getTime();
          if (line.indexOf("TESTBLOCKSTART") >= 0) {
            String s = "";
            while ((line = reader.readLine()) != null && line.indexOf("TESTBLOCKEND") < 0) {
              s += line + "\n";
              lineNum++;
            }
            line = s;
          }
          List<?> info = (List<?>) viewer.scriptWaitStatus(line, "scriptTerminated");
          long end = Profiling.getTime();
          if ((info != null) && (info.size() > 0)) {
            String error = info.get(0).toString();
            if (info.get(0) instanceof List<?>) {
              List<?> vector = (List<?>) info.get(0);
              if (vector.size() > 0) {
                if (vector.get(0) instanceof List<?>) {
                  vector = (List<?>) vector.get(0);
                  error = vector.get(vector.size() - 1).toString();
                }
              }
            }
            if (!error.equalsIgnoreCase("Jmol script terminated successfully")) {
              fail(
                  "Error in script [" + file.getPath() + "] " +
                  "at line " + lineNum + " (" + line + "):\n" +
                  error);
              }
            }
          if ((end - begin) > 0) {
            outputPerformanceMessage(end - begin, "execute [" + line + "]");
          }
        }
        long endScript = Profiling.getTime();
        outputPerformanceMessage(endScript - beginScript, "execute script [" + file.getPath() + "]");
      } catch (FileNotFoundException e) {
        fail("File " + file.getPath() + " not found");
      } catch (IOException e) {
        fail("Error reading line " + lineNum + " of " + file.getPath());
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e) {
            // Nothing
          }
        }
      }
    }
    long endFull = Profiling.getTime();
    if (nbExecutions > 1) {
      outputPerformanceMessage(endFull - beginFull, nbExecutions + " of script");
    }
  }

  /**
   * Executes a test.
   */
  public void runSimpleTest() {
    JUnitLogger.setInformation(file.getPath());

    JmolViewer viewer = JmolViewer.allocateViewer(new JFrame(), new SmarterJmolAdapter(),
        null, null, null, checkOnly ? "-n -c -l " : "-n -l ", null);
        //"-n -c " // set no display; checkOnly; no file opening
        //"-n -l " // set no display; list commmands as they execute
    String s = viewer.evalFile(file.getPath() + " -noqueue");
    assertNull("Error in script [" + file.getPath() + ":\n" + s, s);
  }

  /**
   * Output a performance message.
   * 
   * @param duration Duration in milliseconds.
   * @param message Message.
   */
  private void outputPerformanceMessage(long duration, String message) {
    String time = "            " + duration;
    time = time.substring(Math.min(12, time.length() - 12));
    System.err.println(time + Profiling.getUnit() + ": " + message);
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
  }
}
