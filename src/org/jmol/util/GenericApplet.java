package org.jmol.util;

import java.awt.Event;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.api.JmolAppletInterface;
import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolStatusListener;
import org.jmol.api.JmolSyncInterface;
import org.jmol.c.CBK;
import org.jmol.i18n.GT;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

/**
 * A collection of all methods necessary for initialization of and communication with the applet, 
 * whether it be JavaScript or Java.
 * 
 */
public abstract class GenericApplet implements JmolAppletInterface,
    JmolStatusListener {

  protected static Map<String, Object> htRegistry;

  protected static boolean isJS = /** @j2sNative true || */false;

  private final static int SCRIPT_CHECK = 0;
  private final static int SCRIPT_WAIT = 1;
  private final static int SCRIPT_NOWAIT = 2;

  protected String codeBase;
  protected String documentBase;
  protected boolean isSigned;
  protected String language;

  protected boolean doTranslate = true;
  protected boolean haveDocumentAccess;
  protected boolean isStereoSlave;
  protected boolean mayScript;
  protected String htmlName;
  protected String fullName;
  protected String statusForm;
  protected String statusText;
  protected String statusTextarea;

  protected Object gRight;
  protected Viewer viewer;
  protected Map<CBK, String> callbacks;
  
  protected Map<String, Object> vwrOptions;

  protected boolean haveNotifiedError;

  protected Object appletObject;

  protected boolean isJNLP;

  private boolean loading;
  private String syncId;
  private SB outputBuffer;

  protected void init(Object applet) {
    callbacks = new Hashtable<CBK, String>();
    if (htRegistry == null)
      htRegistry = new Hashtable<String, Object>();

    appletObject = applet;
    htmlName = PT.split("" + getJmolParameter("name"), "_object")[0];
    syncId = getJmolParameter("syncId");
    fullName = htmlName + "__" + syncId + "__";
    System.out.println("Jmol JavaScript applet " + fullName + " initializing");
    int iLevel = (getValue("logLevel", (getBooleanValue("debug", false) ? "5"
        : "4"))).charAt(0) - '0';
    if (iLevel != 4)
      System.out.println("setting logLevel=" + iLevel
          + " -- To change, use script \"set logLevel [0-5]\"");
    Logger.setLogLevel(iLevel);
    GT.ignoreApplicationBundle();
    initOptions();
    checkIn(fullName, appletObject);
    initApplication();
  }

  private void initApplication() {
    vwrOptions.put("applet", Boolean.TRUE);
    if (getJmolParameter("statusListener") == null)
      vwrOptions.put("statusListener", this);
    language = getJmolParameter("language");
    if (language != null)
      vwrOptions.put("language", language);
    viewer = new Viewer(vwrOptions);
    viewer.pushHoldRepaint();
    String emulate = getValueLowerCase("emulate", "jmol");
    setStringProperty("defaults", emulate.equals("chime") ? "RasMol" : "Jmol");
    setStringProperty("backgroundColor",
        getValue("bgcolor", getValue("boxbgcolor", "black")));
    viewer.setBooleanProperty("frank", true);
    loading = true;
    for (CBK item : CBK.values())
      setValue(item.name() + "Callback", null);
    loading = false;
    if (language != null)
      System.out.print("requested language=" + language + "; ");
    doTranslate = (!"none".equals(language) && getBooleanValue("doTranslate",
        true));
    language = GT.getLanguage();
    System.out.println("language=" + language);

    if (callbacks.get(CBK.SCRIPT) == null
        && callbacks.get(CBK.ERROR) == null)
      if (callbacks.get(CBK.MESSAGE) != null || statusForm != null
          || statusText != null) {
        if (doTranslate && (getValue("doTranslate", null) == null)) {
          doTranslate = false;
          Logger
              .warn("Note -- Presence of message callback disables disable translation;"
                  + " to enable message translation use jmolSetTranslation(true) prior to jmolApplet()");
        }
        if (doTranslate)
          Logger
              .warn("Note -- Automatic language translation may affect parsing of message callbacks"
                  + " messages; use scriptCallback or errorCallback to process errors");
      }

    if (!doTranslate) {
      GT.setDoTranslate(false);
      Logger.warn("Note -- language translation disabled");
    }

    // should the popupMenu be loaded ?
    if (!getBooleanValue("popupMenu", true))
      viewer.getProperty("DATA_API", "disablePopupMenu", null);
    //experimental; never documented loadNodeId(getValue("loadNodeId", null));

    String menuFile = getJmolParameter("menuFile");
    if (menuFile != null)
      viewer.setMenu(menuFile, true);

    String script = getValue("script", "");
    //String test = null;

    // test =
    // "\n  Marvin  10270415522D\n\n  9  9  0  0  0  0            999 V2000\n   -2.2457    0.8188    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -2.2457   -0.0063    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -1.5313   -0.4188    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -0.8168   -0.0063    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -0.8168    0.8188    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -0.1023    1.2313    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n   -1.5313    1.2313    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -1.5313    2.8813    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -1.5313    2.0563    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n  7  1  1  0  0  0  0\n  7  5  2  0  0  0  0\n  1  2  2  0  0  0  0\n  2  3  1  0  0  0  0\n  3  4  2  0  0  0  0\n  4  5  1  0  0  0  0\n  5  6  1  0  0  0  0\n  8  9  1  0  0  0  0\n  7  9  1  0  0  0  0\nM  APO  1   9   1\nM  STY  1   1 SUP\nM  SAL   1  2   8   9\nM  SBL   1  1   9\nM  SMT   1 Et\nM  END\n";
    // test =
    // "#JVXL VERSION 1.4\n#created by Jmol Version 11.6.15  2008-11-24 13:39 on Wed Dec 09 20:51:19 CST 2009\nMO range (-4.286028, -4.170638, -6.45537) to (5.6358566, 10.584962, 3.6056142)\ncalculation type: ?\n-32 -4.286028 -4.170638 -6.45537 ANGSTROMS\n80 0.12402356 0.0 0.0\n80 0.0 0.184445 0.0\n80 0.0 0.0 0.1257623\n6 6.0 0.0 0.0 0.0\n6 6.0 1.5335295 0.0 0.0\n8 8.0 2.3333447 1.1734227 0.0\n6 6.0 2.3022442 2.08224 -1.0298198\n6 6.0 1.9440373 3.352165 -0.783915\n6 6.0 2.166842 4.391685 -1.8063345\n8 8.0 1.4540279 5.5320063 -1.5695299\n6 6.0 1.4419276 6.5410266 -2.576349\n6 6.0 0.4292079 7.5804462 -2.13194\n1 1.0 0.699213 8.036855 -1.1707217\n1 1.0 0.36370665 8.384962 -2.8751543\n1 1.0 -0.57431144 7.149238 -2.0169382\n1 1.0 2.4541469 6.9775352 -2.6722505\n1 1.0 1.1674224 6.103718 -3.555268\n8 8.0 2.9358563 4.365084 -2.7551525\n1 1.0 1.4746284 3.6545703 0.16250335\n1 1.0 2.660351 1.708333 -1.9975383\n6 6.0 2.025939 -0.95221835 -1.101621\n1 1.0 1.6514318 -1.9706379 -0.937718\n1 1.0 1.6784323 -0.62881225 -2.0926402\n1 1.0 3.1216602 -1.0091194 -1.1394216\n1 1.0 1.869836 -0.3798073 0.9982192\n6 6.0 -0.86121655 0.62561214 -1.0898211\n6 6.0 -0.8380163 2.0938404 -1.2457241\n6 6.0 -0.81681573 2.6353507 -2.5209484\n7 7.0 -0.79011524 3.084059 -3.6053698\n6 6.0 -1.1576223 2.898656 -0.16150327\n7 7.0 -1.4360279 3.5764692 0.75561434\n1 1.0 -0.59091145 0.1594031 -2.0609396\n1 1.0 -1.9027367 0.2891056 -0.885317\n1 1.0 -0.29010567 -1.0688207 0.08750176\n1 1.0 -0.314506 0.47200906 0.95541835\n-1 35 90 35 90 Jmol voxel format version 1.4\n# \n0.05 361 -1362 1362 -1.0 1.0 -1.0 1.0 rendering:isosurface ID \"homop\" fill noMesh noDots notFrontOnly frontlit\n 117879 5 75 6 74 5 6154 7 72 9 71 9 71 9 72 7 75 3 5915 7 72 9 70 11 69 11 69 11 70 10 71 7 5834 4 74 9 70 11 68 13 67 13 67 13 68 12 69 9 73 5 5754 7 71 11 68 13 67 13 66 14 67 13 67 13 68 11 71 7 5752 8 71 11 68 13 66 14 66 15 65 15 66 13 68 11 71 7 334 2 77 4 77 2 5256 8 70 12 68 13 66 14 66 14 66 14 67 13 68 11 71 7 173 3 75 6 74 7 72 8 73 6 75 4 5176 8 71 11 68 12 67 14 66 14 66 14 67 12 69 10 73 4 173 6 73 8 71 10 70 9 72 8 73 6 5176 6 72 10 69 11 69 12 68 12 68 11 70 9 73 5 175 2 75 7 72 9 71 10 70 10 70 9 72 7 5256 6 72 9 71 9 71 9 72 7 75 2 256 3 75 7 72 9 71 9 71 9 71 9 72 7 5993 7 72 9 71 9 71 9 71 8 74 5 5801 4 74 7 73 7 29 5 39 7 28 7 40 4 28 8 73 7 73 6 5800 5 74 7 72 9 71 9 71 9 28 4 40 7 29 5 75 5 77 1 4709 4 77 3 1008 7 72 9 70 10 70 10 71 9 72 7 75 3 4743 3 76 5 75 5 77 3 928 7 72 9 70 10 70 10 71 9 72 7 75 3 4742 4 76 5 75 5 76 4 928 6 73 8 71 10 70 10 71 9 72 7 4820 3 77 5 75 5 76 4 929 4 75 7 72 8 72 8 73 7 74 5 4822 1 78 4 76 4 77 3 1010 5 74 6 74 6 75 5 4982 2 78 2 279630\nW4?A7Kv(0Dwm)s(RKaX!S@<V0QmxtbS9C3Omxtb9B|#HVR:PQ|RBGb<HJB!-8:#vKSHeHZQ`&A0E@t&u(DQOaJ[6>OJRKgiv'trPM+rNK_@f7ob*7GZT4@>dX!q:`TWV@aG|ga{$l{'-v9-WUv+p2Wmuqb@[S7k>]u[Fw,*'Lq##%$l|R-5pEO=W?69JmAOWT<W'_XQcN[n*cJOw'm31X,GNHDmuXy^Izv+kA2<axMh_lP:ZqF1*-<!,lA;p.)w*)E8BQXQleUe,Z60]N<rx[zeCzs(b;23c`]buFGwJ6/3CgjDQ@a`Tv&vjk4dj4:,+eS'c'e5j-mMYzUS>YS*ES9Xc^Hj9!cv|wgBP74=JC'+G^UNYIRw@kON^c:I2*w,@re<=YqVOVpfMXiEKqP2Tntyt{_e125nBUkx4btL'&^'tFOE{#YXCwPuT[HX|;8XXq*P]T/O4;9TBwZPXsOS.STEk,e*rMsrUZOT^]s.d7GeQvLo8KU!4ql(*(dRZx]Gjazd_gg@G8kN@>FWtce=9e2^F<;CSlmC.(/ErmVLLSazymkr&w+OcjfT/H6A]^LK+v#w=Q45JQM,UG`631[smx/9'-']3h'g9{EKKwDX7t'y#<P^baZLp4S^+DXejjbR6Qx$>R^ecZF##uk(=JOK=%x|m<)&/K6![mrm[4>TINK2tJrk|pN0G()UDG)N#y.7cro?(3N?QDKA:5m!)>{Wl{yxfSHnw/BcymJ!Zd-X_ma0_Za6)S46n+]z=eAp0DAbuylL+gc`E99D].iTfkcLo@LQvz)3$xJgwBo>IiI<C:WTZ;.`/[;[%o%/1.1$$)9S4]Yr{pXu2;@^=FxUl^Jh0rFC6VA]Ca;A3B/u!dz%.rz7L[`=LdD2))1Dfa1ZFI8P>-e(kr-tJc;^rH%|#g$$#8O;In3.;ibxkw)h.@]LQND/GICGPinIc!MXjn.bD(Q7Bb[=q]W1%`*45Zz!A1))0A`lWYl@%w8p$9XR,vxM.MsTf(8'x#=YN_<];G[vysz96LWZVJ5VT.wqH^r:dd2x7N9sRHX-9[OiXX+tjb0C?EfL<44;Ll{v{jisss1NEPSN@(0HCpc4=_JFE[)u;<&K*9Qv9oKovNQxE|,dD[|bRJIReI,T8;4$'l#T#rgBYT'x|diBN^FGWn`HQaA9p7IrX1j:iC|*6N1>DStklvgLFUz)F,^jlfU7NFw6ixct+;6qXN0-k7RG[K733&z%HXj7W;uXGi?X@ZMpfL++N/&/6`j(S.tiBS;9qARHJ/3`8GSJHQ@WKT8hpDF./&&*)<;_]snyS6($+?aU+OAqyi>F4bJ&|m'R`{3YB5ahgED38-C1gBbhXS[o|]MHOap]X^q\n|~1361 \n#-------end of jvxl file data-------\n# cutoff = 0.05; pointsPerAngstrom = 8.062984; nSurfaceInts = 361; nBytesData = 3736; bicolor map\n# created using Jvxl.java\n# precision: false nColorData 1362\n# isosurface homop cutoff +0.05 mo homo fill;\n# isosurface ID \"homop\" fill noMesh noDots notFrontOnly frontlit\n# bytes read: 0; approximate voxel-only input/output byte ratio: 2794:1\n";
    // loadParam = test;

    String loadParam = getValue("loadInline", null);
    if (loadParam == null) {
      if ((loadParam = getValue("load", null)) != null)
        script = "load \"" + loadParam + "\";" + script;
      loadParam = null;
    }
    viewer.popHoldRepaint("applet init");
    if (loadParam != null && viewer.loadInline(loadParam) != null)
      script = "";
    if (script.length() > 0)
      scriptProcessor(script, null, SCRIPT_WAIT);
    viewer.notifyStatusReady(true);
  }

  @Override
  public void destroy() {
    gRight = null;
    viewer.notifyStatusReady(false);
    viewer = null;
    checkOut(fullName);
  }

  protected boolean getBooleanValue(String propertyName, boolean defaultValue) {
    String value = getValue(propertyName, defaultValue ? "true" : "");
    return (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on") || value
        .equalsIgnoreCase("yes"));
  }

  protected String getValue(String propertyName, String defaultValue) {
    String s = getJmolParameter(propertyName);
    System.out.println("Jmol getValue " + propertyName + " " + s);
    return (s == null ? defaultValue : s);
  }

  private String getValueLowerCase(String paramName, String defaultValue) {
    String value = getValue(paramName, defaultValue);
    if (value != null) {
      value = value.trim().toLowerCase();
      if (value.length() == 0)
        value = null;
    }
    return value;
  }

  private void setValue(String name, String defaultValue) {
    setStringProperty(name, getValue(name, defaultValue));
  }

  private void setStringProperty(String name, String value) {
    if (value == null)
      return;
    Logger.info(name + " = \"" + value + "\"");
    viewer.setStringProperty(name, value);
  }

  private String scriptProcessor(String script, String statusParams,
                                 int processType) {
    /*
    * Idea here is to provide a single point of entry
    * Synchronization may not work, because it is possible for the NOWAIT variety of
    * scripts to return prior to full execution 
    * 
    */
    //System.out.println("Jmol.script: " + script);
    if (script == null || script.length() == 0)
      return "";
    switch (processType) {
    case SCRIPT_CHECK:
      Object err = viewer.scriptCheck(script);
      return (err instanceof String ? (String) err : "");
    case SCRIPT_WAIT:
      if (statusParams != null)
        return viewer.scriptWaitStatus(script, statusParams).toString();
      return viewer.scriptWait(script);
    case SCRIPT_NOWAIT:
    default:
      return viewer.script(script);
    }
  }

  ///////// JmolSyncInterface //////////

  @Override
  public void register(String id, JmolSyncInterface jsi) {
    checkIn(id, jsi);
  }

  /**
   * JSpecView shares the JmolSyncInterface; used to get JSpecView
   */
  @Override
  public Map<String, Object> getJSpecViewProperty(String key) {
    // only on JSpecView side, as it is also JmolSyncInterface
    return null;
  }

  @Override
  synchronized public void syncScript(String script) {
    viewer.syncScript(script, "~", 0);
  }

  ////////////// JmolAppletInterface //////////////

  @Override
  public boolean handleEvent(Event e) {
    if (viewer == null)
      return false;
    return viewer.processMouseEvent(e.id, e.x, e.y, e.modifiers, e.when);
  }

  @Override
  public String getAppletInfo() {
    return GT
        .o(GT
            .$("Jmol Applet version {0} {1}.\n\nAn OpenScience project.\n\nSee http://www.jmol.org for more information"),
            new Object[] { JC.version, JC.date })
        + "\nhtmlName = "
        + PT.esc(htmlName)
        + "\nsyncId = "
        + PT.esc(syncId)
        + "\ndocumentBase = "
        + PT.esc(documentBase)
        + "\ncodeBase = " + PT.esc(codeBase);
  }

  @Override
  public void script(String script) {
    scriptNoWait(script);
  }

  @Override
  public String scriptCheck(String script) {
    if (script == null || script.length() == 0)
      return "";
    return scriptProcessor(script, null, SCRIPT_CHECK);
  }

  @Override
  public String scriptNoWait(String script) {
    if (script == null || script.length() == 0)
      return "";
    return scriptProcessor(script, null, SCRIPT_NOWAIT);
  }

  @Override
  public String scriptWait(String script) {
    return scriptWait(script, null);
  }

  @Override
  public String scriptWait(String script, String statusParams) {
    if (script == null || script.length() == 0)
      return "";
    outputBuffer = null;
    return scriptProcessor(script, statusParams, SCRIPT_WAIT);
  }

  @Override
  public String scriptWaitOutput(String script) {
    if (script == null || script.length() == 0)
      return "";
    outputBuffer = new SB();
    viewer.scriptWaitStatus(script, "");
    String str = (outputBuffer == null ? "" : outputBuffer.toString());
    outputBuffer = null;
    return str;
  }

  @Override
  public int getModelIndexFromId(String id) {
    return viewer.getModelIndexFromId(id);
  }
  
  /**
   * @param infoType
   * @return value
   * 
   */
  @Override
  public Object getProperty(String infoType) {
    return viewer.getProperty(null, infoType, "");
  }

  /**
   * @@XXXj2sOverride
   * 
   * @param infoType
   * @param paramInfo
   * @return value
   * 
   * 
   */

  @Override
  public Object getProperty(String infoType, String paramInfo) {
    /**
     * @j2sNative
     * 
     *            paramInfo || (paramInfo = "");
     * 
     */
    {
    }
    return viewer.getProperty(null, infoType, paramInfo);
  }

  /**
   * @param infoType
   * @return value
   * 
   */
  @Override
  public String getPropertyAsString(String infoType) {
    return viewer.getProperty("readable", infoType, "").toString();
  }

  /**
   * @@XXXj2sOverride
   * 
   * @param infoType
   * @param paramInfo
   * @return value
   * 
   * 
   */

  @Override
  public String getPropertyAsString(String infoType, String paramInfo) {
    /**
     * @j2sNative
     * 
     *            paramInfo || (paramInfo = "");
     * 
     */
    {
    }
    return viewer.getProperty("readable", infoType, paramInfo).toString();
  }

  /**
   * @param infoType
   * @return value
   * 
   */
  @Override
  public String getPropertyAsJSON(String infoType) {
    return viewer.getProperty("JSON", infoType, "").toString();
  }

  /**
   * @@XXXj2sOverride
   * 
   * @param infoType
   * @param paramInfo
   * @return value
   * 
   * 
   */

  @Override
  public String getPropertyAsJSON(String infoType, String paramInfo) {
    /**
     * @j2sNative
     * 
     *            paramInfo || (paramInfo = "");
     * 
     */
    {
    }
    return viewer.getProperty("JSON", infoType, paramInfo).toString();
  }

  @Override
  public String loadInlineString(String strModel, String script,
                                 boolean isAppend) {
    String errMsg = viewer.loadInlineAppend(strModel, isAppend);
    if (errMsg == null)
      script(script);
    return errMsg;
  }

  @Override
  public String loadInlineArray(String[] strModels, String script,
                                boolean isAppend) {
    if (strModels == null || strModels.length == 0)
      return null;
    String errMsg = viewer.loadInline(strModels, isAppend);
    if (errMsg == null)
      script(script);
    return errMsg;
  }

  @Override
  public String loadDOMNode(Object DOMNode) {
    // This should provide a route to pass in a browser DOM node
    // directly as a JSObject. Unfortunately does not seem to work with
    // current browsers
    return viewer.openDOM(DOMNode);
  }

  /**
   * @deprecated
   * @param strModel
   * @return error or null
   */
  @Override
  @Deprecated
  public String loadInline(String strModel) {
    return loadInlineString(strModel, "", false);
  }

  /**
   * @deprecated
   * @param strModel
   * @param script
   * @return error or null
   */
  @Override
  @Deprecated
  public String loadInline(String strModel, String script) {
    return loadInlineString(strModel, script, false);
  }

  /**
   * @deprecated
   * @param strModels
   * @return error or null
   */
  @Override
  @Deprecated
  public String loadInline(String[] strModels) {
    return loadInlineArray(strModels, "", false);
  }

  /**
   * @deprecated
   * @param strModels
   * @param script
   * @return error or null
   */
  @Override
  @Deprecated
  public String loadInline(String[] strModels, String script) {
    return loadInlineArray(strModels, script, false);
  }

  /// called by mystatuslisteners

  public void output(String s) {
    if (outputBuffer != null && s != null)
      outputBuffer.append(s).appendC('\n');
  }

  @Override
  public void setCallbackFunction(String callbackName, String callbackFunction) {
    //also serves to change language for callbacks and menu
    if (callbackName.equalsIgnoreCase("language")) {
      consoleMessage(""); // clear
      consoleMessage(null); // show default message
      return;
    }
    CBK callback = CBK.getCallback(callbackName);
    if (callback != null && (loading || callback != CBK.EVAL)) {
      if (callbackFunction == null)
        callbacks.remove(callback);
      else
        callbacks.put(callback, callbackFunction);
      return;
    }
    consoleMessage("Available callbacks include: "
        + CBK.getNameList().replace(';', ' ').trim());
  }

  private void consoleMessage(String message) {
    notifyCallback(CBK.ECHO, new Object[] { "", message });
  }

  /////////////  JmolStatusListener ///////////
  
  @Override
  public boolean notifyEnabled(CBK type) {
    switch (type) {
    case SERVICE:
      return false;
    case ECHO:
    case MESSAGE:
    case MEASURE:
    case PICK:
    case SELECT:
    case SYNC:
      return true;
    case ANIMFRAME:
    case DRAGDROP:
    case ERROR:
    case EVAL:
    case IMAGE:
    case LOADSTRUCT:
    case SCRIPT:
      return !isJNLP;
    case AUDIO: // Jmol 14.29.2
    case APPLETREADY: // Jmol 12.1.48
    case ATOMMOVED: // Jmol 12.1.48
    case CLICK:
    case HOVER:
    case MINIMIZATION:
    case RESIZE:
    case STRUCTUREMODIFIED:
      break;
    }
    return (callbacks.get(type) != null);
  }

  /**
   * @param type the callback type or null for getJsObjectInfo() -- Java applet only
   * @param data type-dependent
   */
  @SuppressWarnings("unchecked")
  @Override
  public void notifyCallback(CBK type, Object[] data) {
    String callback = (type == null ? null : callbacks.get(type));
    boolean doCallback = (type == null || callback != null && (data == null || data[0] == null));
    boolean toConsole = false;
    if (data != null)
      data[0] = htmlName;
    String strInfo = (data == null || data[1] == null ? null : data[1]
        .toString());

    //System.out.println("Jmol.java notifyCallback " + type + " " + callback
    //+ " " + strInfo);
    if (type != null)
    switch (type) {
    case APPLETREADY:
      data[3] = appletObject;
      break;
    case AUDIO:
    case ERROR:
    case EVAL:
    case HOVER:
    case IMAGE:
    case MINIMIZATION:
    case SERVICE:
    case RESIZE:
    case DRAGDROP:
    case ATOMMOVED:
    case SELECT:
      // just send it
      break;
    case CLICK:
      // x, y, action, int[] {action}
      // the fourth parameter allows an application to change the action
      if ("alert".equals(callback))
        strInfo = "x=" + data[1] + " y=" + data[2] + " action=" + data[3]
            + " clickCount=" + data[4];
      break;
    case ANIMFRAME:
      // Note: twos-complement. To get actual frame number, use
      // Math.max(frameNo, -2 - frameNo)
      // -1 means all frames are now displayed
      int[] iData = (int[]) data[1];
      int frameNo = iData[0];
      int fileNo = iData[1];
      int modelNo = iData[2];
      int firstNo = iData[3];
      int lastNo = iData[4];
      boolean isAnimationRunning = (frameNo <= -2);
      int animationDirection = (firstNo < 0 ? -1 : 1);
      int currentDirection = (lastNo < 0 ? -1 : 1);

      /*
       * animationDirection is set solely by the "animation direction +1|-1"
       * script command currentDirection is set by operations such as
       * "anim playrev" and coming to the end of a sequence in
       * "anim mode palindrome"
       * 
       * It is the PRODUCT of these two numbers that determines what direction
       * the animation is going.
       */
      if (doCallback) {
        data = new Object[] { htmlName,
            Integer.valueOf(Math.max(frameNo, -2 - frameNo)),
            Integer.valueOf(fileNo), Integer.valueOf(modelNo),
            Integer.valueOf(Math.abs(firstNo)),
            Integer.valueOf(Math.abs(lastNo)),
            Integer.valueOf(isAnimationRunning ? 1 : 0),
            Integer.valueOf(animationDirection),
            Integer.valueOf(currentDirection),
            data[2]/*entryName*/, data[3]/*morphModel*/
            };
      }
      break;
    case ECHO:
      boolean isPrivate = (data.length == 2);
      boolean isScriptQueued = (isPrivate || ((Integer) data[2]).intValue() == 1);
      if (!doCallback) {
        if (isScriptQueued)
          toConsole = true;
        doCallback = (!isPrivate && (callback = callbacks
            .get((type = CBK.MESSAGE))) != null);
      }
      if (!toConsole)
        output(strInfo);
      break;
    case LOADSTRUCT:
      String errorMsg = (String) data[4];
      if (errorMsg != null) {
        errorMsg = (errorMsg.indexOf("NOTE:") >= 0 ? "" : GT.$("File Error:"))
            + errorMsg;
        doShowStatus(errorMsg);
        notifyCallback(CBK.MESSAGE, new Object[] { "", errorMsg });
        return;
      }
      break;
    case MEASURE:
      // pending, deleted, or completed
      if (!doCallback)
        doCallback = ((callback = callbacks.get((type = CBK.MESSAGE))) != null);
      String status = (String) data[3];
      if (status.indexOf("Picked") >= 0 || status.indexOf("Sequence") >= 0) {// picking mode
        doShowStatus(strInfo); // set picking measure distance
        toConsole = true;
      } else if (status.indexOf("Completed") >= 0) {
        strInfo = status + ": " + strInfo;
        toConsole = true;
      }
      break;
    case MESSAGE:
      toConsole = !doCallback;
      doCallback &= (strInfo != null);
      if (!toConsole)
        output(strInfo);
      break;
    case PICK:
      doShowStatus(strInfo);
      toConsole = true;
      break;
    case SCRIPT:
      int msWalltime = ((Integer) data[3]).intValue();
      // general message has msWalltime = 0
      // special messages have msWalltime < 0
      // termination message has msWalltime > 0 (1 + msWalltime)
      if (msWalltime > 0) {
        // termination -- button legacy -- unused
      } else if (!doCallback) {
        // termination messsage ONLY if script callback enabled -- not to
        // message queue
        // for compatibility reasons
        doCallback = ((callback = callbacks.get((type = CBK.MESSAGE))) != null);
      }
      output(strInfo);
      doShowStatus(strInfo);
      break;
    case STRUCTUREMODIFIED:
//      int mode = ((Integer) data[1]).intValue();
//      int atomIndex = ((Integer) data[2]).intValue();
//      int modelIndex = ((Integer) data[3]).intValue();
//      switch (mode) {
//      case 1/-1: // assign atom
//      case 2/-2: // assign bond
//      case 3/-3: // 
//      case 4/-4: // delete atoms
//      case 5/-5: // delete models
//      }
      break;
    case SYNC:
      sendScript(strInfo, (String) data[2], true, doCallback);
      return;
    case MODELKIT:
      break;
    default:
      break;
    }
    if (toConsole) {
      JmolCallbackListener appConsole = (JmolCallbackListener) viewer
          .getProperty("DATA_API", "getAppConsole", null);
      if (appConsole != null) {
        appConsole.notifyCallback(type, data);
        output(strInfo);
        doSendJsTextareaStatus(strInfo);
      }
    }
    if (!doCallback || !mayScript)
      return;
    try {
      doSendCallback(callback, data, strInfo);
    } catch (Exception e) {
      if (!haveNotifiedError)
        if (Logger.debugging) {
          Logger.debug(type.name() + "Callback call error to " + callback
              + ": " + e);
        }
      haveNotifiedError = true;
    }
  }

  private String sendScript(String script, String appletName, boolean isSync,
                            boolean doCallback) {
    if (doCallback) {
      script = notifySync(script, appletName);
      // if the notified JavaScript function returns "" or 0, then 
      // we do NOT continue to notify the other applets
      if (script == null || script.length() == 0 || script.equals("0"))
        return "";
    }

    Lst<String> apps = new Lst<String>();
    findApplets(appletName, syncId, fullName, apps);
    int nApplets = apps.size();
    if (nApplets == 0) {
      if (!doCallback && !appletName.equals("*"))
        Logger.error(fullName + " couldn't find applet " + appletName);
      return "";
    }
    SB sb = (isSync ? null : new SB());
    boolean getGraphics = (isSync && script
        .equals(Viewer.SYNC_GRAPHICS_MESSAGE));
    boolean setNoGraphics = (isSync && script
        .equals(Viewer.SYNC_NO_GRAPHICS_MESSAGE));
    if (getGraphics)
      viewer.setStereo(false, (gRight = null));
    for (int i = 0; i < nApplets; i++) {
      String theApplet = apps.get(i);
      JmolSyncInterface app = (JmolSyncInterface) htRegistry.get(theApplet);
      boolean isScriptable = true;//(app instanceof JmolScriptInterface);
      if (Logger.debugging)
        Logger.debug(fullName + " sending to " + theApplet + ": " + script);
      try {
        if (isScriptable && (getGraphics || setNoGraphics)) {
          viewer.setStereo(isStereoSlave = getGraphics, gRight = ((JmolAppletInterface) app).setStereoGraphics(getGraphics));
          return "";
        }
        if (isSync)
          app.syncScript(script);
        else if (isScriptable)
          sb.append(((JmolAppletInterface) app).scriptWait(script, "output"))
              .append("\n");
      } catch (Exception e) {
        String msg = htmlName + " couldn't send to " + theApplet + ": "
            + script + ": " + e;
        Logger.error(msg);
        if (!isSync)
          sb.append(msg);
      }
    }
    return (isSync ? "" : sb.toString());
  }

  private String notifySync(String info, String appletName) {
    String syncCallback = callbacks.get(CBK.SYNC);
    if (!mayScript || syncCallback == null)
      return info;
    try {
      return doSendCallback(syncCallback, new Object[] { fullName, info,
          appletName }, null);
    } catch (Exception e) {
      if (!haveNotifiedError)
        if (Logger.debugging) {
          Logger.debug("syncCallback call error to " + syncCallback + ": " + e);
        }
      haveNotifiedError = true;
    }
    return info;
  }

  @Override
  public String eval(String strEval) {
    // may be appletName\1script
    int pt = strEval.indexOf("\1");
    if (pt >= 0)
      return sendScript(strEval.substring(pt + 1), strEval.substring(0, pt),
          false, false);
    if (!haveDocumentAccess)
      return "NO EVAL ALLOWED";
    if (callbacks.get(CBK.EVAL) != null) {
      notifyCallback(CBK.EVAL, new Object[] { null, strEval });
      return "";
    }
    return doEval(strEval);
  }

  @Override
  public float[][] functionXY(String functionName, int nX, int nY) {
    return doFunctionXY(functionName, nX, nY);
  }

  @Override
  public float[][][] functionXYZ(String functionName, int nX, int nY, int nZ) {
    return doFunctionXYZ(functionName, nX, nY, nZ);
  }

  @Override
  public String createImage(String fileName, String type, Object text_or_bytes,
                            int quality) {
    // not implemented
    return null;
  }

  @Override
  public Map<String, Object> getRegistryInfo() {
    checkIn(null, null); //cleans registry
    return htRegistry;
  }

  @Override
  public void showUrl(String urlString) {
    if (Logger.debugging)
      Logger.debug("showUrl(" + urlString + ")");
    if (urlString != null && urlString.length() > 0)
      try {
        doShowDocument(new URL((URL) null, urlString, null));
      } catch (MalformedURLException mue) {
        consoleMessage("Malformed URL:" + urlString);
      }
  }

  @Override
  public int[] resizeInnerPanel(String data) {
    return new int[] { viewer.getScreenWidth(), viewer.getScreenHeight() };
  }

  //////////// applet registration for direct applet-applet communication ////////////

  synchronized static void checkIn(String name, Object applet) {
    if (name != null) {
      Logger.info("AppletRegistry.checkIn(" + name + ")");
      htRegistry.put(name, applet);
    }
    if (Logger.debugging) {
      for (Map.Entry<String, Object> entry : htRegistry.entrySet()) {
        String theApplet = entry.getKey();
        Logger.debug(theApplet + " " + entry.getValue());
      }
    }
  }

  synchronized static void checkOut(String name) {
    htRegistry.remove(name);
  }

  synchronized static void findApplets(String appletName, String mySyncId,
                                       String excludeName, Lst<String> apps) {
    if (appletName != null && appletName.indexOf(",") >= 0) {
      String[] names = PT.split(appletName, ",");
      for (int i = 0; i < names.length; i++)
        findApplets(names[i], mySyncId, excludeName, apps);
      return;
    }
    String ext = "__" + mySyncId + "__";
    //System.out.println("findApplet looking for " + ext + " appletName=" + appletName + " " + htRegistry.containsKey(appletName));
    if (appletName == null || appletName.equals("*") || appletName.equals(">")) {
      for (String appletName2 : htRegistry.keySet()) {
        //System.out.println("findApplet key=" + appletName2);
        if (!appletName2.equals(excludeName) && appletName2.indexOf(ext) > 0) {
          //System.out.println("findApplet found " + appletName2);
          apps.addLast(appletName2);
        }
      }
      return;
    }
    if (excludeName.indexOf("_object") >= 0 && appletName.indexOf("_object") < 0)
      appletName += "_object";
    if (appletName.indexOf("__") < 0)
      appletName += ext;
    if (!htRegistry.containsKey(appletName))
      appletName = "jmolApplet" + appletName;
    if (!appletName.equals(excludeName) && htRegistry.containsKey(appletName)) {
      apps.addLast(appletName);
      //System.out.println("findApplet found2 " + appletName);
    }
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public void notifyAudioEnded(Object htParams) {
    viewer.sm.notifyAudioStatus((Map<String, Object>) htParams);
  }
  
  protected Map<String, Object> htParams;

  protected void setJSOptions(Map<String, Object> vwrOptions) {
    htParams = new Hashtable<String, Object>();
    if (vwrOptions == null)
      vwrOptions = new Hashtable<String, Object>();
    this.vwrOptions = vwrOptions;
    for (Map.Entry<String, Object> entry : vwrOptions.entrySet())
      htParams.put(entry.getKey().toLowerCase(), entry.getValue());
    documentBase = "" + vwrOptions.get("documentBase");
    codeBase = "" + vwrOptions.get("codePath");
  }

  protected void initOptions() {
    vwrOptions.remove("debug");
    vwrOptions.put("fullName", fullName);
    haveDocumentAccess = "true".equalsIgnoreCase(""
        + getValue("allowjavascript", "true"));
    mayScript = true;
  }

  protected String getJmolParameter(String paramName) {
    Object o = htParams.get(paramName.toLowerCase());
    return (o == null ? null : "" + o);
  }

  protected void doSendJsTextStatus(String message) {
    System.out.println(message);
    // not implemented
  }

  protected void doSendJsTextareaStatus(String message) {
    System.out.println(message);
    // not implemented
  }

  protected float[][] doFunctionXY(String functionName, int nX, int nY) {
    /*three options:
     * 
     *  nX > 0  and  nY > 0        return one at a time, with (slow) individual function calls
     *  nX < 0  and  nY > 0        return a string that can be parsed to give the list of values
     *  nX < 0  and  nY < 0        fill the supplied float[-nX][-nY] array directly in JavaScript 
     *  
     */

    //System.out.println("functionXY" + nX + " " + nY  + " " + functionName);
    float[][] fxy = new float[Math.abs(nX)][Math.abs(nY)];
    if (!mayScript || !haveDocumentAccess || nX == 0 || nY == 0)
      return fxy;
    try {
      if (nX > 0 && nY > 0) { // fill with individual function calls (slow)
        for (int i = 0; i < nX; i++)
          for (int j = 0; j < nY; j++) {
            /**
             * @j2sNative
             * 
             *            fxy[i][j] = window.eval(functionName)(this.htmlName, i, j);
             */
            {
            }
          }
      } else if (nY > 0) { // fill with parsed values from a string (pretty fast)
        String data;
        /**
         * @j2sNative
         * 
         *            data = window.eval(functionName)(this.htmlName, nX, nY);
         * 
         */
        {
          data = "";
        }
        nX = Math.abs(nX);
        float[] fdata = new float[nX * nY];
        Parser.parseStringInfestedFloatArray(data, null, fdata);
        for (int i = 0, ipt = 0; i < nX; i++) {
          for (int j = 0; j < nY; j++, ipt++) {
            fxy[i][j] = fdata[ipt];
          }
        }
      } else { // fill float[][] directly using JavaScript
        /**
         * @j2sNative
         * 
         *            data = window.eval(functionName)(this.htmlName, nX, nY, fxy);
         * 
         */
        {
          System.out.println(functionName);
        }
      }
    } catch (Exception e) {
      Logger.error("Exception " + e + " with nX, nY: " + nX + " " + nY);
    }
    // for (int i = 0; i < nX; i++)
    // for (int j = 0; j < nY; j++)
    //System.out.println("i j fxy " + i + " " + j + " " + fxy[i][j]);
    return fxy;
  }

  protected float[][][] doFunctionXYZ(String functionName, int nX, int nY,
                                      int nZ) {
    float[][][] fxyz = new float[Math.abs(nX)][Math.abs(nY)][Math.abs(nZ)];
    if (!mayScript || !haveDocumentAccess || nX == 0 || nY == 0 || nZ == 0)
      return fxyz;
    try {
      /**
       * @j2sNative
       * 
       *            window.eval(functionName)(this.htmlName, nX, nY, nZ, fxyz);
       * 
       */
      {
      }
    } catch (Exception e) {
      Logger.error("Exception " + e + " for " + functionName
          + " with nX, nY, nZ: " + nX + " " + nY + " " + nZ);
    }
    // for (int i = 0; i < nX; i++)
    // for (int j = 0; j < nY; j++)
    // for (int k = 0; k < nZ; k++)
    //System.out.println("i j k fxyz " + i + " " + j + " " + k + " " + fxyz[i][j][k]);
    return fxyz;
  }

  protected void doShowDocument(URL url) {
    String[] surl = PT.split(url.toString(), "?POST?");
    if (surl.length == 1) {
      /**
       * @j2sNative
       * 
       *            window.open(surl[0]);
       * 
       */
    {}
     return;
      }
   
    String f = "<form id=f method=POST action='" + surl[0] + "'>";
    f += "<input type='hidden' name='name' value='nmr-1h-prediction' id='name'>";
    f += "<input type='submit' value='working...'>";
    String[] fields = surl[1].split("&");
    for (int i = 0; i < fields.length; i++) {
      String field = fields[i];
      int pt = field.indexOf("=");
      String name = field.substring(0, pt);
      String value = field.substring(pt);
      if (value.indexOf("\n") >= 0) {
        f +="<textarea style='display:none' name=" + name + ">" + value +"</textarea>";
      } else {
        f +="<input type=hidden name=" + name + " value=\""+ value +"\">";
      }
    }
    f += "</form>";
    /**
     * @j2sNative
     * var w=window.open("");w.document.write(f);w.document.getElementById("f").submit();
     * 
     */
    {
      System.out.println(f + url);
    }
  }

  protected String doSendCallback(String callback, Object[] data, String strInfo) {
    if (callback == null || callback.length() == 0) {
    } else if (callback.equals("alert")) {
      /**
       * @j2sNative alert(strInfo); return "";
       */
      {
        System.out.println(strInfo);
      }
    } else {
      String[] tokens = PT.split(callback, ".");
      /**
       * @j2sNative
       * 
       *            try{ 
       *            var o = window[tokens[0]]; 
       *            for (var i = 1; i < tokens.length; i++) 
       *              o = o[tokens[i]];
       *            for (var i = 0; i < data.length; i++) 
       *              data[i] && data[i].booleanValue && (data[i] = data[i].booleanValue());
       *            return o.apply(null,data)
       *            } catch (e) { System.out.println(callback + " failed " + e); }
       */
      {
        System.out.println(tokens + " " + data);
      }
    }
    return "";
  }

  /**
   * return RAW JAVASCRIPT OBJECT, NOT A STRING 
   */
  protected String doEval(String strEval) {
    try {
      /**
       * 
       * @j2sNative
       * 
       *            return window.eval(strEval);
       */
      {
      }
    } catch (Exception e) {
      Logger.error("# error evaluating " + strEval + ":" + e.toString());
    }
    return "";
  }

  protected void doShowStatus(String message) {
    try {
      System.out.println(message);
    } catch (Exception e) {
      //ignore if page is closing
    }
  }

  /**
   * This method is only called by JmolGLmol applet._refresh();
   * 
   * @return enough data to update a WebGL view
   * 
   */
  public Object getGLmolView() {
    return viewer.getGLmolView();
  }

  /**
   * possibly called from JSmolApplet.js upon start up
   *  
   * @param fileName
   * @return error or null
   */
  
  public String openFile(String fileName) {
    return viewer.openFile(fileName);
  }

  // JSInterface -- methods called from JSmol JavaScript library
  
  public int cacheFileByName(String fileName, boolean isAdd) {
    return viewer.cacheFileByName(fileName, isAdd);
  }

  public void cachePut(String key, Object data) {
    viewer.cachePut(key, data);
  }

  public String getFullName() {
    return fullName;
  }


}
