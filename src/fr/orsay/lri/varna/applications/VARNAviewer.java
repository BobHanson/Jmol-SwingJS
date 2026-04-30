package fr.orsay.lri.varna.applications;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import fr.orsay.lri.varna.components.VARNAPanel;
import fr.orsay.lri.varna.controlers.ControleurScriptParser;
import fr.orsay.lri.varna.exceptions.ExceptionParameterError;
import fr.orsay.lri.varna.interfaces.InterfaceParameterLoader;
import fr.orsay.lri.varna.interfaces.VARNAviewerI;
import fr.orsay.lri.varna.models.BaseSet;
import fr.orsay.lri.varna.models.FullBackup;
import fr.orsay.lri.varna.models.VARNAConfigLoader;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.RNA;

/**
 * 
 * A viewer for Jmol's VARNAPlugin. Does not allow editing, but
 * allows command line scripting; allows for multiple RNA associated with 
 * a single VARNAPanel
 * 
 * Generalized to work with VARNAapp for Jmol-SwingJS by Bob Hanson
 * 
 * @author Bob Hanson
 * 
 */

public class VARNAviewer extends VARNA
    implements VARNAviewerI, InterfaceParameterLoader, ActionListener {

  private ActionListener actionListener;
  private boolean notifyJmol;

  public VARNAviewer() {
    super("VARNA", false);
    app.setDoInterpolate(false);
    app.setParameterSource(this);
  }

  @Override
  protected boolean getEditable() {
    return false;
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

  @Override
  protected boolean showZoomWindow() {
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
  @SuppressWarnings("unchecked")
  public Object notifyCallback(VARNACallBack type, Object[] data) {
    switch (type) {
    default:
      System.err.println("Varna plugin callback for " + type);
      return null;
    case SETPLUGIN:
      JFrame parentFrame = (JFrame) data[0];
      actionListener = (ActionListener) data[1];
      app.setActionListener(this);
      app.setVarnaPanel(getConfiguredPanel());
      notifyJmol = false;
      JFrame varnaFrame = setFrame(parentFrame, null, 0, 0);
      notifyJmol = true;
      return varnaFrame;
    case GETFRAME:
      return (getVarnaPanel() == null ? null : frame);
    case SETDSSR:
      String modelName = (String) data[0];
      Integer modelID = (Integer) data[1];
      Map<String, Object> dssrInfo = (Map<String, Object>) data[2];
      notifyJmol = false;
      newDSSRSequenceAndStructure(modelName, modelID, dssrInfo);
      notifyJmol = true;
      return null;
    case ZAP:
      if (app != null)
        app.zap();
      frame.setVisible(false);
      return null;
    case SCRIPT:
      String cmd = ((String) data[1]).substring(6).trim();
      String err = script(cmd);
      return err;
    case COLOR:
      data = (Object[]) data[1];
      List<Color> colors = (List<Color>) data[1];
      app.colorBasesByResno(colors, (Map<Integer, Map<String, List<Integer>>>) data[0]);
      app.repaint();
      return null;
    case SELECT:
      notifyJmol = false;
      app.selectBasesByResno((Map<Integer, Map<String, List<Integer>>>) data[1]);
      notifyJmol = true;
      app.repaint();
      return null;
    case DESTROY:
      destroy();
      return null;
    }
  }

  private void destroy() {
    try {
    frame.setVisible(false);
    setFrame(null, null, -1, -1);
    app.destroy();
    } catch (Exception e) {
      // ignore
    }
  }

  /**
   * From VARNA
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    if (!notifyJmol)
      return;
    switch (e.getActionCommand()) {
    case "selectModel":
      FullBackup b = (FullBackup) e.getSource();
      e.setSource(b.name);
      System.out.println("VV selectModel " + b.name);
      break;
    case "selectBases":
      BaseSet baseList = (BaseSet) e.getSource();
      String jmolScript = null;
      switch (e.getID()) {
      case VARNAapp.SEL_CLEAR:
        jmolScript = "select none";
        //$FALL-THROUGH$
      case VARNAapp.SEL_SET:
        e.setSource(jmolScript);
        actionListener.actionPerformed(e);
        // now source is "<color1> <color2>"
        String script = "setSelectionColors(" + e.getSource() + ")";
        script(script);
        //$FALL-THROUGH$
      case VARNAapp.SEL_COMPLETE:
        ArrayList<ModeleBase> bases = baseList.getBaseList();
        int[] resnos = new int[bases.size()];
        for (int i = bases.size(); --i >= 0;)
          resnos[i] = bases.get(i).getResidueNumber();
        e.setSource(resnos);
        break;
      default:
        return;
      }
      break;
    default:
      return;
    }
    // back to the Jmol plugin
    SwingUtilities.invokeLater(() -> {
      actionListener.actionPerformed(e);
    });
  }

  @SuppressWarnings("unchecked")
  private void newDSSRSequenceAndStructure(String modelName, Integer modelID, Map<String, Object> dssrInfo) {
    Map<String, Object> dbn = (Map<String, Object>) dssrInfo.get("dbn");
    Map<String, Object> all = (Map<String, Object>) dbn.get("all_chains");
    String sstr = (String) all.get("sstr");
    String bseq = (String) all.get("bseq");
    RNA newRNA = app.selectOrAddSequenceAndStructure(modelName, modelID, bseq, sstr);
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

  public String script(String cmd) {
    try {
      ControleurScriptParser.executeScript(app.getVARNAPanel(), cmd);
    } catch (Exception e) {
      return e.getMessage();
    }
    return null;
  }

}
