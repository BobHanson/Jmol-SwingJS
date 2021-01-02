package org.jmol;

/*
 * An extraordinarily simple application that just creates a viewer,
 * runs a script, and returns the collective result of PRINT, MESSAGE or ECHO commands
 * 
 */
import org.jmol.api.JmolViewer;

public class TestScriptOutput {

  // Main application
  public static void main(String[] args) {
    new TestScriptOutput();
  }

  public TestScriptOutput() {
    JmolViewer viewer = JmolViewer.allocateViewer(null, null);
    String jmolScript = "print 'hello world: \n' + getProperty('appletInfo')";
    String strOutput = (String) viewer.scriptWaitStatus(jmolScript, null);
    System.out.println(strOutput);
  }
}
