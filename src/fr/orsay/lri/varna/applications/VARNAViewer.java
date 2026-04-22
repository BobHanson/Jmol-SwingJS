package fr.orsay.lri.varna.applications;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.components.VARNAConsole;
import fr.orsay.lri.varna.controlers.ControleurScriptParser;
import fr.orsay.lri.varna.exceptions.ExceptionParameterError;
import fr.orsay.lri.varna.interfaces.InterfaceParameterLoader;
import fr.orsay.lri.varna.interfaces.VARNAViewerI;
import fr.orsay.lri.varna.models.BaseList;
import fr.orsay.lri.varna.models.FullBackup;
import fr.orsay.lri.varna.models.VARNAConfigLoader;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.RNA;

/**
 * 
 * A viewer for Jmol's VARNAPlugin. Like VARNAEditor, no editing (??) but like
 * VARNAConsole, only having one structure.
 * 
 * Generalized to work with VARNAapp for Jmol-SwingJS by Bob Hanson
 * 
 * @author Yann Ponty & Darty Kévin
 * 
 */

public class VARNAViewer extends VARNAGUI
    implements VARNAViewerI, InterfaceParameterLoader, ActionListener {

  private VARNAConsole _console;
  private ActionListener actionListener;

  public VARNAViewer() {
    super("VARNA", false);
    app.setDoInterpolate(false);
    app.setParameterSource(this);
  }

  @Override
  protected boolean getShowTitle() {
    return false;
  }

  @Override
  protected boolean getAllowCreate() {
    return false;
  }

  @Override
  protected boolean getShowListing() {
    return true;
  }

  @Override
  protected boolean allowDoubleClick() {
    return false;
  }

  /**
   * Create a VARNAPanel using this app's configuration, 
   * calling getParameterValue(key,def) here for anything that
   * is nonstandard.
   * @return configured VARNAPanel
   */
  public VARNAPanel getConfiguredPanel() {
    try {
      return VARNAConfigLoader.createVARNAPanel(this);
    } catch (ExceptionParameterError e) {
      return null;
    }
    
  }

  @Override
  public String getParameterValue(String key, String def) {
    switch (key) {
    case VARNAConfigLoader.bpStyleOpt:
      return VARNAConfigLoader.SIMPLE_BP_STYLE;
    }
    System.out.println("VV setting " + key + " to " + def);
    return def;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    switch (e.getActionCommand()) {
    case "selectModel":
      FullBackup b = (FullBackup) e.getSource();
      e.setSource(b.name);
      System.out.println("VV selectModel " + b.name);
      break;
    case "selectBases":
      if (e.getID() != VARNAapp.SEL_COMPLETE) {
        return;
      }
      BaseList baseList = (BaseList) e.getSource();
      ArrayList<ModeleBase> bases = baseList.getBases();
      int[] resnos = new int[bases.size()];
      for (int i = bases.size(); --i >= 0;) 
        resnos[i] = bases.get(i).getResidueNumber();
      e.setSource(resnos);
      break;
    default:
      return;
    }
    // back to the Jmol plugin
    SwingUtilities.invokeLater(()->{
      actionListener.actionPerformed(e);      
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public void newDSSRSequenceAndStructure(String modelName, Map<String, Object> dssrInfo) {
    Map<String, Object> dbn = (Map<String, Object>) dssrInfo.get("dbn");
    Map<String, Object> all = (Map<String, Object>) dbn.get("all_chains");
    String sstr = (String) all.get("sstr");
    String bseq = (String) all.get("bseq");
    RNA newRNA = app.selectOrAddSequenceAndStructure(modelName, bseq, sstr);
    if (newRNA != null) {
      List<Map<String, Object>> nts = (List<Map<String, Object>>) dssrInfo.get("nts");
      int[] resnos = getDSSRModelResNos(nts);
      newRNA.setResidueNumbers(resnos);
    }
  }

  @SuppressWarnings("cast")
  private static int[] getDSSRModelResNos(List<Map<String, Object>> nts) {
    int[] nums = new int[nts.size()];
    for (int i = nums.length; --i >= 0;) {
      Map<String, Object> res = (Map<String, Object>)nts.get(i);
      nums[i] = ((Number) res.get("nt_resnum")).intValue();
    }
    return nums;
  }

  /**
   * 
   * @param set a set of residue numbers to match 
   */
  public void selectResnoSet(HashSet<Integer> set) {
    app.getVARNAPanel().selectBasesByResno(set);
    
  }

  public JFrame finalizeFrame(JFrame parentFrame, ActionListener listener) {
    actionListener = listener;
    app.setActionListener(this);
    app.setVarnaPanel(getConfiguredPanel());
    return setFrame(parentFrame, null, 0, 0);
  }

  public void zap() {
    if (app != null)
      app.zap();
  }

  public void script(String cmd) {
    try {
      ControleurScriptParser.executeScript(app.getVARNAPanel(), cmd);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
