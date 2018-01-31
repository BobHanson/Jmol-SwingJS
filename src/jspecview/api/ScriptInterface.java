package jspecview.api;

import javajs.util.Lst;

import jspecview.common.Spectrum;
import jspecview.common.PanelNode;
import jspecview.common.ScriptToken;
import jspecview.source.JDXSource;

public interface ScriptInterface {

	// java.awt.Component methods
	public void repaint();
	public void setCursor(int id);

	// from JSVAppletInterface or JSVInterface
	
	public boolean isSigned();
	public void runScript(String script);
	public boolean runScriptNow(String script);
	public void syncToJmol(String value);
	public void writeStatus(String msg);
	
	// JSpecView methods
	public void siCheckCallbacks(String title);

	public void siSourceClosed(JDXSource source);

	public void siExecHidden(boolean b);

	public String siLoaded(String value);

	public void siExecScriptComplete(String msg, boolean isOK);

	public void siExecSetCallback(ScriptToken st, String value);

	public void siExecTest(String value);

	public JSVPanel siGetNewJSVPanel(Spectrum spec);

	public JSVPanel siGetNewJSVPanel2(Lst<Spectrum> specs);

	public void siOpenDataOrFile(Object data, String name, Lst<Spectrum> specs,
			String url, int firstSpec, int lastSpec, boolean doCheck, String script, String id);

	public void siProcessCommand(String script);
	
	public void siSendPanelChange();

	public void siSetCurrentSource(JDXSource source);

	public void siSetLoaded(String fileName, String filePath);

	public void siSetMenuEnables(PanelNode node, boolean isSplit);

	public void siNodeSet(PanelNode node);

	public void siSetPropertiesFromPreferences(JSVPanel jsvp, boolean b);

	public void siSetSelectedPanel(JSVPanel jsvp);

	public void siSyncLoad(String fileName);

	public void siUpdateBoolean(ScriptToken st, boolean TF);

	public void siUpdateRecentMenus(String filePath);

	public void siValidateAndRepaint(boolean isAll);
	
	public void siNewWindow(boolean isSelected, boolean fromFrame);
	
}

