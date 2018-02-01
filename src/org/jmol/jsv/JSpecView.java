package org.jmol.jsv;

import java.util.Hashtable;


import org.jmol.api.JmolJSpecView;
import javajs.util.BS;
import org.jmol.modelset.Atom;
import javajs.util.Lst;

import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.StatusManager;
import org.jmol.viewer.Viewer;

import javajs.util.PT;

public class JSpecView implements JmolJSpecView {

  private Viewer vwr;
  @Override
  public void setViewer(Viewer vwr) {
    this.vwr = vwr;
  }
  
  @Override
  public void atomPicked(int atomIndex) {
    if (atomIndex < 0)
      return;
    String peak = getPeakAtomRecord(atomIndex);
    if (peak != null)
      sendJSpecView(peak + " src=\"JmolAtomSelect\"");
  }
  
  @SuppressWarnings("unchecked")
  private String getPeakAtomRecord(int atomIndex) {
    Atom[] atoms = vwr.ms.at;
    int iModel = atoms[atomIndex].mi;
    String type = null;
    switch (atoms[atomIndex].getElementNumber()) {
    case 1:
      type = "1HNMR";
      break;
    case 6:
      type = "13CNMR";
      break;
    default:
      return null;
    }
    Lst<String> peaks = (Lst<String>) vwr.ms.getInfo(iModel,
        "jdxAtomSelect_" + type);
    if (peaks == null)
      return null;
    //vwr.modelSet.htPeaks = null;
    //if (vwr.modelSet.htPeaks == null)
    vwr.ms.htPeaks = new Hashtable<String, BS>();
    Hashtable<String, BS> htPeaks = vwr.ms.htPeaks;
    for (int i = 0; i < peaks.size(); i++) {
      String peak = peaks.get(i);
      System.out.println("Jmol JSpecView.java peak="  + peak);
      BS bsPeak = htPeaks.get(peak);
      System.out.println("Jmol JSpecView.java bspeak="  + bsPeak);
      if (bsPeak == null) {
        htPeaks.put(peak, bsPeak = new BS());
        String satoms = PT.getQuotedAttribute(peak, "atoms");
        String select = PT.getQuotedAttribute(peak, "select");
        System.out.println("Jmol JSpecView.java satoms select " + satoms + " " + select);
        String script = "";
        if (satoms != null)
          script += "visible & (atomno="
              + PT.rep(satoms, ",", " or atomno=") + ")";
        else if (select != null)
          script += "visible & (" + select + ")";
        System.out.println("Jmol JSpecView.java script : " + script);
        bsPeak.or(vwr.getAtomBitSet(script));
      }
      System.out.println("Jmol JSpecView bsPeak now : " + bsPeak + " " + atomIndex);
      if (bsPeak.get(atomIndex))
        return peak;
    }
    return null;
  }


  private void sendJSpecView(String peak) {
    String msg = PT.getQuotedAttribute(peak, "title");
    if (msg != null)
      vwr.scriptEcho(Logger.debugging ? peak : msg);
    peak = vwr.fullName + "JSpecView: " + peak;
    Logger.info("Jmol.JSpecView.sendJSpecView Jmol>JSV " + peak);
    vwr.sm.syncSend(peak, ">", 0);
  }

  @Override
  public void setModel(int modelIndex) {
    int syncMode = ("sync on".equals(vwr.ms
        .getInfoM("jmolscript")) ? StatusManager.SYNC_DRIVER
        : vwr.sm.getSyncMode());
    if (syncMode != StatusManager.SYNC_DRIVER)
      return;
    String peak = (String) vwr.ms.getInfo(modelIndex, "jdxModelSelect");
    // problem is that SECOND load in jmol will not load new model in JSpecView
    if (peak != null)
      sendJSpecView(peak + " src=\"Jmol\"");
  }

  @Override
  public int getBaseModelIndex(int modelIndex) {
    String baseModel = (String) vwr.ms.getInfo(modelIndex,
        "jdxBaseModel");
    if (baseModel != null)
      for (int i = vwr.ms.mc; --i >= 0;)
        if (baseModel
            .equals(vwr.ms.getInfo(i, "jdxModelID")))
          return i;
    return modelIndex;
  }

  @Override
  public String processSync(String script, int jsvMode) {
    if (Logger.debugging)
      Logger.info("org.jmol.jsv.JSpecView jsvMode=" + jsvMode + " script=" + script);
    switch (jsvMode ) {
    default:
      return null;
    case JC.JSV_SEND_JDXMOL:
      vwr.sm.syncSend(vwr.fullName + "JSpecView" + script.substring(10), ">", 0);
      return null;
    case JC.JSV_CLOSE:
    case JC.JSV_SEND_H1SIMULATE:
    case JC.JSV_SEND_C13SIMULATE:
      vwr.sm.syncSend(vwr.fullName + "JSpecView:" + script, ">", 0);
      return null;
    case JC.JSV_STRUCTURE:
      // application only -- NO! This is 2D -- does no good. We need
      // a full 3D model!
      if (vwr.isApplet)
        return null;
      return null;//"~_thisModel = _modelNumber; load append DATA 'myfile'" + script.substring(7) + "\nEND 'myfile';model @~_thisModel info 'atomMap' @{compare({1.1} {2.1} 'MAP' 'H')}; zap {*}[0]";
      // these are Jmol atom indexes. The second number will be >= n, and all must be incremented by 1.
      
//      for (var i = 0; i < map.length; i++) {
//        var c = map[i];
//        A[c[0] + 1] = c[1] - n + 1;
//        B[c[1] - n + 1] = c[0] + 1;
//      }
//      return {fromJmol:A, toJmol:B}; // forward and rev.    
    case JC.JSV_SELECT:
      // from JSpecView peak pick or possibly model change
      String filename = PT.getQuotedAttribute(script, "file");
      // this is a real problem -- JSpecView will have unmatched
      // model if a simulation.
      boolean isSimulation = (filename != null && filename
          .startsWith(FileManager.SIMULATION_PROTOCOL));
      // id='~1.1' is getting tucked into file="...." now
      String id = (!isSimulation || vwr.isApplet ? "" : PT.getQuotedAttribute(filename.replace('\'', '"'), "id"));
      if (isSimulation && !vwr.isApplet && 
          (filename.startsWith(FileManager.SIMULATION_PROTOCOL + "C13/MOL=") || filename.startsWith(FileManager.SIMULATION_PROTOCOL + "H1/MOL=")))
        filename = null; // from our sending; don't reload
      else
        filename = PT.rep(filename, "#molfile", "");
      String modelID = (isSimulation ? "molfile" : PT.getQuotedAttribute(
          script, "model"));
      String baseModel = PT.getQuotedAttribute(script, "baseModel");
      String atoms = PT.getQuotedAttribute(script, "atoms");
      String select = PT.getQuotedAttribute(script, "select");
      String script2 = PT.getQuotedAttribute(script, "script");
      if (id == null || id.length() == 0)
        id = (modelID == null ? null : (filename == null ? "" : filename
          + "#")
          + modelID);
      if ("".equals(baseModel))
        id += ".baseModel";
      int modelIndex = (id == null ? -3 : vwr.getModelIndexFromId(id));
      if (modelIndex == -2)
        return null; // file was found, or no file was indicated, but not this model -- ignore
      if (modelIndex != -1 || filename == null) {
        script = "";
      } else if (isSimulation && !vwr.isApplet) {
        return null;
      } else {
        if (isSimulation)
          filename += "#molfile";
        script = "load " + PT.esc(filename);
      }
      //script = PT.rep(script, FileManager.SIMULATION_PROTOCOL, "");
      if (id != null)
        script += ";model " + PT.esc(id);
      // needs work here to use the atomMap if it exists.
      
      if (atoms != null)
        script += ";select visible & (@" + PT.rep(atoms, ",", " or @") + ")";
      else if (select != null)
        script += ";select visible & (" + select + ")";
      if (script2 != null)
        script += ";" + script2;
      return script;
    case JC.JSV_SETPEAKS:
      // JSpecView sending us the peak information it has
      String[] list = Escape.unescapeStringArray(script.substring(7));
      Lst<String> peaks = new Lst<String>();
      String type = "1HNMR";
      for (int i = 0; i < list.length; i++) {
        if (i == 0 && list[i].indexOf(FileManager.SIMULATION_PROTOCOL + "C13/") >= 0)
          type = "13CNMR";
        peaks.addLast(list[i]);
      }
      vwr.ms.setInfo(vwr.am.cmi, "jdxAtomSelect_" + type, peaks);
      return null;
    }
  }
}
