package org.jmol.api;

import java.util.Map;

import javajs.util.Lst;


public interface JmolJDXMOLParser {

  String getFirstModelWithPeaks();

  JmolJDXMOLParser set(JmolJDXMOLReader loader,
                                             String filePath,
                                             Map<String, Object> htParams);

  String getRecord(String key) throws Exception;

  String getAttribute(String line, String tag);
  
  boolean readModels() throws Exception;

  int readPeaks(boolean isSignals, int peakCount)
      throws Exception;

  void setLine(String s);

  String readACDMolFile() throws Exception;

  boolean readACDAssignments(int nPoints, boolean isPeakAssignment, Lst<String[]> list) throws Exception;

  int setACDAssignments(String model, String mytype, int peakCount,
                        Lst<String[]> acdlist, String molFile) throws Exception;

}
