/**
 * 
 */
package jspecview.common;

public enum ExportType {
  UNK, SOURCE, DIF, FIX, SQZ, PAC, XY, DIFDUP, PNG, JPG, SVG, SVGI, CML, AML, PDF;

  public static ExportType getType(String type) {
    type = type.toUpperCase();
    if (type.equalsIgnoreCase(JSViewer.sourceLabel))
      return SOURCE;
    if (type.startsWith("XML"))
      return AML;
    for (ExportType mode : values())
      if (mode.name().equals(type)) 
        return mode;
    return UNK;
  }    
  
  public static boolean isExportMode(String ext) {
    return (getType(ext) != UNK);
  }
}