package jspecview.common;

import org.jmol.util.Logger;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;
import jspecview.source.JDXSource;

public class SyncManager {
  
  private static String testScript = "<PeakData  index=\"1\" title=\"\" model=\"~1.1\" type=\"1HNMR\" xMin=\"3.2915\" xMax=\"3.2965\" atoms=\"15,16,17,18,19,20\" multiplicity=\"\" integral=\"1\"> src=\"JPECVIEW\" file=\"http://SIMULATION/$caffeine\"";
  
  static void syncToJmol(JSViewer vwr, PeakInfo pi) {
    vwr.peakInfoModelSentToJmol = pi.getModel();
    vwr.si.syncToJmol(jmolSelect(vwr.pd(), pi));
  }

  /**
   * incoming script processing of <PeakAssignment file="" type="xxx"...> record
   * from Jmol
   * @param vwr 
   * 
   * @param peakScript
   */
  public static void syncFromJmol(JSViewer vwr, String peakScript) {
    //Jmol>JSV
    if (peakScript.equals("TEST"))
      peakScript = testScript;
    //Logger.info("JSViewer.syncScript Jmol>JSV " + peakScript);
    if (peakScript.indexOf("<PeakData") < 0) {
      if (peakScript.startsWith("JSVSTR:")) {
        vwr.si.syncToJmol(peakScript);
        return;
      }
      vwr.runScriptNow(peakScript);
      if (peakScript.indexOf("#SYNC_PEAKS") >= 0)
        syncToJmolPeaksAfterSyncScript(vwr);
      return;
    }
    String sourceID = PT.getQuotedAttribute(peakScript, "sourceID");
    String type, model, file, jmolSource, index, atomKey;
    if (sourceID == null) {
      // todo: why the quotes??
      //peakScript = PT.rep(peakScript, "\\\"", "");
      file = PT.getQuotedAttribute(peakScript, "file");
      index = PT.getQuotedAttribute(peakScript, "index");
      if (file == null || index == null)
        return;
      file = PT.rep(file, "#molfile", "");// Jmol has loaded the model from the cache
      model = PT.getQuotedAttribute(peakScript, "model");
      jmolSource = PT.getQuotedAttribute(peakScript, "src");
      String modelSent = (jmolSource != null && jmolSource.startsWith("Jmol") ? null
          : vwr.peakInfoModelSentToJmol);
      if (model != null && modelSent != null && !model.equals(modelSent)) {
        Logger.info("JSV ignoring model " + model + "; should be " + modelSent);
        return;
      }
      vwr.peakInfoModelSentToJmol = null;
      if (vwr.panelNodes.size() == 0 || !vwr.checkFileAlreadyLoaded(file)) {
        Logger.info("file " + file
            + " not found -- JSViewer closing all and reopening");
        vwr.si.siSyncLoad(file);
      }
      type = PT.getQuotedAttribute(peakScript, "type");
      atomKey = null;
    } else {
      file = null;
      index = model = sourceID;
      atomKey = "," + PT.getQuotedAttribute(peakScript, "atom") + ",";
      type = "ID";
      jmolSource = sourceID; //??
    }

    PeakInfo pi = selectPanelByPeak(vwr, file, index, atomKey);
    PanelData pd = vwr.pd();
    pd.selectSpectrum(file, type, model, true);
    vwr.si.siSendPanelChange();
    pd.addPeakHighlight(pi);
    vwr.repaint(true);
    // round trip this so that Jmol highlights all equivalent atoms
    // and appropriately starts or clears vibration
    if (jmolSource == null || (pi != null && pi.getAtoms() != null))
      vwr.si.syncToJmol(jmolSelect(vwr.pd(), pi));
  }

  private static void syncToJmolPeaksAfterSyncScript(JSViewer vwr) {
    JDXSource source = vwr.currentSource;
    if (source == null)
      return;
    try {
      String file = "file=" + PT.esc(source.getFilePath());
      Lst<Spectrum> spectra = source.getSpectra();
      Lst<PeakInfo> peaks = spectra.get(spectra.size() - 1).getPeakList();
      SB sb = new SB();
      sb.append("[");
      int n = peaks.size();
      for (int i = 0; i < n; i++) {
        String s = peaks.get(i).toString();
        s = s + " " + file;
        sb.append(PT.esc(s));
        if (i > 0)
          sb.append(",");
      }
      sb.append("]");
      vwr.si.syncToJmol("Peaks: " + sb);
    } catch (Exception e) {
      // ignore bad structures -- no spectrum
    }
  }

  /**
   * @param vwr 
   * @param file
   * @param index
   * @param atomKey
   * @return PeakInfo entry if appropriate
   */
  private static PeakInfo selectPanelByPeak(JSViewer vwr, String file, String index, String atomKey) {
    Lst<PanelNode> panelNodes = vwr.panelNodes;
    if (panelNodes == null)
      return null;
    PeakInfo pi = null;
    for (int i = panelNodes.size(); --i >= 0;)
      panelNodes.get(i).pd().addPeakHighlight(null);
    pi = vwr.pd().selectPeakByFileIndex(file, index, atomKey);
    if (pi != null) {
      // found in current panel
      vwr.setNode(PanelNode.findNode(vwr.selectedPanel, panelNodes));
    } else {
      // must look elsewhere
      for (int i = panelNodes.size(); --i >= 0;) {
        PanelNode node = panelNodes.get(i);
        if ((pi = node.pd().selectPeakByFileIndex(file, index, atomKey)) != null) {
          vwr.setNode(node);
          break;
        }
      }
    }
    return pi;
  }

  private static String jmolSelect(PanelData pd, PeakInfo pi) {
    String script = ("IR".equals(pi.getType()) || "RAMAN".equals(pi.getType()) ? "vibration ON; selectionHalos OFF;"
        : "vibration OFF; selectionhalos "
            + (pi.getAtoms() == null ? "OFF" : "ON"));
    return "Select: " + pi + " script=\"" + script + " \" sourceID=\""
        + pd.getSpectrum().sourceID + "\"";
  }


 
  
  
}