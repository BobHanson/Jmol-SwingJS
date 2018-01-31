package org.jmol.api;

import java.util.Map;

import javajs.util.Lst;


public interface JmolJDXMOLParser {

  public abstract JmolJDXMOLParser set(JmolJDXMOLReader loader,
                                             String filePath,
                                             Map<String, Object> htParams);

  public abstract String getRecord(String key) throws Exception;

  public abstract String getAttribute(String line, String tag);
  
  public abstract boolean readModels() throws Exception;

  public abstract int readPeaks(boolean isSignals, int peakCount)
      throws Exception;

  public abstract void setLine(String s);

  public abstract String readACDMolFile() throws Exception;

  Lst<String[]> readACDAssignments(int nPoints, boolean isPeakAssignment) throws Exception;

  int setACDAssignments(String model, String mytype, int peakCount,
                        Lst<String[]> acdlist, String molFile) throws Exception;

}
