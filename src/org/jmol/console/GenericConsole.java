/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-26 16:57:51 -0500 (Thu, 26 Apr 2007) $
 * $Revision: 7502 $
 *
 * Copyright (C) 2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.console;

import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.PT;

import org.jmol.api.JmolAbstractButton;
import org.jmol.api.JmolAppConsoleInterface;
import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolScriptEditorInterface;
import org.jmol.c.CBK;
import org.jmol.i18n.GT;
import org.jmol.script.T;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public abstract class GenericConsole implements JmolAppConsoleInterface, JmolCallbackListener {
  
  protected GenericTextArea input;
  protected GenericTextArea output;

  public Viewer vwr;
  
  protected void setViewer(Viewer vwr) {
    this.vwr = vwr;
    if (labels == null) {
      Map<String, String> l = new Hashtable<String, String>();
      l.put("title", GT._("Jmol Script Console") + " " + Viewer.getJmolVersion());
      setupLabels(l);
      labels = l;
    }

  }

  protected static Map<String, String> labels;
  protected Map<String, Object> menuMap = new Hashtable<String, Object>();
  protected JmolAbstractButton editButton, runButton, historyButton, stateButton;
  protected JmolAbstractButton clearOutButton, clearInButton, loadButton;
   
  abstract protected boolean isMenuItem(Object source);  
  abstract protected void layoutWindow(String enabledButtons);
  abstract protected void setTitle();  
  @Override
  abstract public void setVisible(boolean visible);
  @Override
  abstract public JmolScriptEditorInterface getScriptEditor();
  @Override
  abstract public void dispose();

  abstract protected JmolAbstractButton setButton(String text);
 
  protected JmolAbstractButton addButton(JmolAbstractButton b, String label) {
    b.addConsoleListener(this);
    menuMap.put(label, b);
    return b; 
  }

  protected JmolAbstractButton getLabel1() {
    return null;
  }

  protected void setupLabels(Map<String, String>  labels) {
    // these three are for ImageDialog 
    labels.put("saveas", GT._("&Save As..."));
    labels.put("file", GT._("&File"));
    labels.put("close", GT._("&Close"));
    setupLabels0(labels);
  }

  protected void setupLabels0(Map<String, String>  labels) {
    labels.put("help", GT._("&Help"));
    labels.put("search", GT._("&Search..."));
    labels.put("commands", GT._("&Commands"));
    labels.put("functions", GT._("Math &Functions"));
    labels.put("parameters", GT._("Set &Parameters"));
    labels.put("more", GT._("&More"));
    labels.put("Editor", GT._("Editor"));
    labels.put("State", GT._("State"));
    labels.put("Run", GT._("Run"));
    labels.put("Clear Output", GT._("Clear Output"));
    labels.put("Clear Input", GT._("Clear Input"));
    labels.put("History", GT._("History"));
    labels.put("Load", GT._("Load"));
    labels.put("label1", GT
        ._("press CTRL-ENTER for new line or paste model data and press Load"));
    labels.put("default",
        GT._("Messages will appear here. Enter commands in the box below. Click the console Help menu item for on-line help, which will appear in a new browser window."));
  }

  protected void setLabels() {
    boolean doTranslate = GT.setDoTranslate(true);
    editButton = setButton("Editor");
    stateButton = setButton("State");
    runButton = setButton("Run");
    clearOutButton = setButton("Clear Output");
    clearInButton = setButton("Clear Input");
    historyButton = setButton("History");
    loadButton = setButton("Load");
    defaultMessage = getLabel("default");
    setTitle();
    GT.setDoTranslate(doTranslate);
  }

  public static String getLabel(String key) {
    return labels.get(key);
  }

  protected void displayConsole() {
    layoutWindow(null);
    outputMsg(defaultMessage);
  }

  protected String defaultMessage;
  protected JmolAbstractButton label1;
  
  protected void updateLabels() {
    return;
  }

  abstract protected String nextFileName(String stub, int nTab);
  public int nTab = 0;
  private String incompleteCmd;
  
  public String completeCommand(String thisCmd) {
    if (thisCmd.length() == 0)
      return null;
    String strCommand = (nTab <= 0 || incompleteCmd == null ? thisCmd
        : incompleteCmd);
    incompleteCmd = strCommand;
    String[] splitCmd = GenericConsole.splitCommandLine(thisCmd);
    if (splitCmd == null)
      return null;
    boolean asCommand = splitCmd[2] == null;
    boolean inBrace = (splitCmd[3] != null);
    String notThis = splitCmd[asCommand ? 1 : 2];
    String s = splitCmd[1];
    if (notThis.length() == 0)
      return null;
    T token = T.getTokenFromName(s.trim().toLowerCase());
    int cmdtok = (token == null ? 0 : token.tok);
    boolean isSelect = T.tokAttr(cmdtok, T.atomExpressionCommand);
    splitCmd = GenericConsole.splitCommandLine(strCommand);
    String cmd = null;
    if (!asCommand && (notThis.charAt(0) == '"' || notThis.charAt(0) == '\'')) {
      char q = notThis.charAt(0);
      notThis = PT.trim(notThis, "\"\'");
      String stub = PT.trim(splitCmd[2], "\"\'");
      cmd = nextFileName(stub, nTab);
      if (cmd != null)
        cmd = splitCmd[0] + splitCmd[1] + q + cmd + q;
    } else {
      Map<String, Object> map = null;
      if (!asCommand) {
        notThis = s;
        if (inBrace || splitCmd[2].startsWith("$") 
            //|| T.isIDcmd(cmdtok) 
            || isSelect) {
          map = new Hashtable<String, Object>();
          vwr.getObjectMap(map, inBrace || isSelect ? '{' : splitCmd[2].startsWith("$") ? '$' : '0');
        }
      }
      cmd = T.completeCommand(map, s.equalsIgnoreCase("set "), asCommand, asCommand ? splitCmd[1]
          : splitCmd[2], nTab);
      cmd = splitCmd[0]
          + (cmd == null ? notThis : asCommand ? cmd : splitCmd[1] + cmd);
    }
    return (cmd == null || cmd.equals(strCommand) ? null : cmd);
  }

  protected void doAction(Object source) {
    if (source == runButton) {
      execute(null);
    } else if (source == editButton) {
      vwr.getProperty("DATA_API","scriptEditor", null);
    } else if (source == historyButton) {
      clearContent(vwr.getSetHistory(Integer.MAX_VALUE));
    } else if (source == stateButton) {
      clearContent(vwr.getStateInfo());
      // problem here is that in some browsers, you cannot clip from
      // the editor.
      //vwr.getProperty("DATA_API","scriptEditor", new String[] { "current state" , vwr.getStateInfo() });
    } else     
      if (source == clearInButton) {
        input.setText("");
        return;
      }
      if (source == clearOutButton) {
        output.setText("");
        return;
      }
      if (source == loadButton) {
        vwr.loadInlineAppend(input.getText(), false);
        return;
      }
      if (isMenuItem(source)) {
        execute(((JmolAbstractButton) source).getName());
        return;
      }
  }

  protected void execute(String strCommand) {
    String cmd = (strCommand == null ? input.getText() : strCommand);
    if (strCommand == null)
      input.setText(null);
    String strErrorMessage = vwr.script(cmd + JC.SCRIPT_EDITOR_IGNORE);
    if (strErrorMessage != null && !strErrorMessage.equals("pending"))
      outputMsg(strErrorMessage);
  }

  protected void destroyConsole() {
    // if the vwr is an applet, when we close the console
    // we 
    if (vwr.isApplet)
      vwr.getProperty("DATA_API", "getAppConsole", Boolean.FALSE);
  }

  public static void setAbstractButtonLabels(Map<String, Object> menuMap,
                               Map<String, String> labels) {
    for (String key: menuMap.keySet()) {
      JmolAbstractButton m = (JmolAbstractButton) menuMap.get(key);
      String label = labels.get(key);
      if (key.indexOf("Tip") == key.length() - 3) {
        m.setToolTipText(labels.get(key));
      } else {
        char mnemonic = getMnemonic(label);
        if (mnemonic != ' ')
          m.setMnemonic(mnemonic);
        label = getLabelWithoutMnemonic(label);
        m.setText(label);
      }
    }
  }

  public static String getLabelWithoutMnemonic(String label) {
    if (label == null) {
      return null;
    }
    int index = label.indexOf('&');
    if (index == -1) {
      return label;
    }
    return label.substring(0, index) +
      ((index < label.length() - 1) ? label.substring(index + 1) : "");
  }

  static char getMnemonic(String label) {
    if (label == null) {
      return ' ';
    }
    int index = label.indexOf('&');
    if ((index == -1) || (index == label.length() - 1)){
      return ' ';
    }
    return label.charAt(index + 1);
  }

  public static void map(Object button, String key, String label,
                         Map<String, Object> menuMap) {
    char mnemonic = getMnemonic(label);
    if (mnemonic != ' ')
      ((JmolAbstractButton) button).setMnemonic(mnemonic);
    if (menuMap != null)
      menuMap.put(key, button);
  }

  ///////////// JmolCallbackListener interface

  // Allowing for just the callbacks needed to provide status feedback to the console.
  // For applications that embed Jmol, see the example application Integration.java.

  @Override
  public boolean notifyEnabled(CBK type) {
    // See org.jmol.viewer.JmolConstants.java for a complete list
    switch (type) {
    case ECHO:
    case MEASURE:
    case MESSAGE:
    case PICK:
      return true;
    case ANIMFRAME:
    case APPLETREADY:
    case ATOMMOVED:
    case CLICK:
    case DRAGDROP:
    case ERROR:
    case EVAL:
    case HOVER:
    case IMAGE:
    case LOADSTRUCT:
    case MINIMIZATION:
    case SERVICE:
    case RESIZE:
    case SCRIPT:
    case SYNC:
    case STRUCTUREMODIFIED:
      break;
    }
    return false;
  }

  @Override
  @SuppressWarnings("incomplete-switch")
  public void notifyCallback(CBK type, Object[] data) {
    String strInfo = (data == null || data[1] == null ? null : data[1]
        .toString());
    switch (type) {
    case ECHO:
      sendConsoleEcho(strInfo);
      break;
    case MEASURE:
      String mystatus = (String) data[3];
      if (mystatus.indexOf("Picked") >= 0 || mystatus.indexOf("Sequence") >= 0) // picking mode
        sendConsoleMessage(strInfo);
      else if (mystatus.indexOf("Completed") >= 0)
        sendConsoleEcho(strInfo.substring(strInfo.lastIndexOf(",") + 2,
            strInfo.length() - 1));
      break;
    case MESSAGE:
      sendConsoleMessage(data == null ? null : strInfo);
      break;
    case PICK:
      sendConsoleMessage(strInfo);
      break;
    }
  }

  @Override
  public String getText() {
    return output.getText();
  }

  @Override
  public void sendConsoleEcho(String strEcho) {
    if (strEcho == null) {
      // null here means new language
      updateLabels();
      outputMsg(null);
      strEcho = defaultMessage;
    } else if (strEcho.equals("\0")) {
      /**
       * @j2sNative
       * 
       * Clazz.Console.clear();
       */
      {}
      strEcho = null;
    }
    outputMsg(strEcho);
  }

  private void outputMsg(String message) {
    int n = (message == null ? -1 : message.length());
    switch (n) {
    case -1:
      output.setText("");
      return;
    default:
      if (message.charAt(n - 1) == '\n')
        break;
      //$FALL-THROUGH$
    case 0:
      message += "\n";
    }
    output.append(message);
  }

  protected void clearContent(String text) {
    output.setText(text);
  }
  
  @Override
  public void sendConsoleMessage(String strInfo) {
    // null here indicates "clear console"
    if (strInfo != null && output.getText().startsWith(defaultMessage))
      outputMsg(null);
    outputMsg(strInfo);
  }
  
  @Override
  public void setCallbackFunction(String callbackType, String callbackFunction) {
    // application-dependent option
  }

  @Override
  public void zap() {
  }

  // key listener actions
  
  protected void recallCommand(boolean up) {
    String cmd = vwr.getSetHistory(up ? -1 : 1);
    if (cmd != null)
      input.setText(PT.escUnicode(cmd));
  }
  
  /**
   * 
   * @param kcode
   * @param kid
   * @param isControlDown
   * @return  1 = consume; 2 = super.process; 3 = both
   */
  protected int processKey(int kcode, int kid, boolean isControlDown) {
    int mode = 0;
    switch (kid) {
    case KeyEvent.KEY_PRESSED:
      switch (kcode) {
      case KeyEvent.VK_TAB:
        String s = input.getText();
        if (s.endsWith("\n") || s.endsWith("\t"))
          return 0;
        mode = 1;
        if (input.getCaretPosition() == s.length()) {
          String cmd = completeCommand(s);
          if (cmd != null)
            input.setText(PT.escUnicode(cmd).replace('\t',' '));
          nTab++;
          return mode;
        }
        break;
      case KeyEvent.VK_ESCAPE:
        mode = 1;
        input.setText("");
        break;
      }
      nTab = 0;
      if (kcode == KeyEvent.VK_ENTER && !isControlDown) {
        execute(null);
        return mode;
      }
      if (kcode == KeyEvent.VK_UP || kcode == KeyEvent.VK_DOWN) {
        recallCommand(kcode == KeyEvent.VK_UP);
        return mode;
      }
      break;
    case KeyEvent.KEY_RELEASED:
      if (kcode == KeyEvent.VK_ENTER && !isControlDown)
        return mode;
      break;
    }
    return mode | 2;
  }
  
  /**
   * separate a command line into three sections:
   * 
   * prefix....;cmd ........ token
   * 
   * where token can be a just-finished single or double quote or
   * a string of characters
   * 
   * @param cmd
   * @return String[] {prefix, cmd..... token}
   */
  private static String[] splitCommandLine(String cmd) {
    String[] sout = new String[4];
    boolean isEscaped1 = false;
    boolean isEscaped2 = false;
    boolean isEscaped = false;
    if (cmd.length() == 0)
      return null;
    int ptQ = -1;
    int ptCmd = 0;
    int ptToken = 0;
    int nBrace = 0;
    char ch;
    for (int i = 0; i < cmd.length(); i++) {
      switch(ch = cmd.charAt(i)) {
      case '"':
        if (!isEscaped && !isEscaped1) {
          isEscaped2 = !isEscaped2;
          if (isEscaped2)
            ptQ = ptToken = i;
        }
        break;
      case '\'':
        if (!isEscaped && !isEscaped2) {
          isEscaped1 = !isEscaped1;
          if (isEscaped1)
            ptQ = ptToken = i;
        }
        break;
      case '\\':
        isEscaped = !isEscaped;
        continue;
      case ' ':
        if (!isEscaped && !isEscaped1 && !isEscaped2) {
          ptToken = i + 1;
          ptQ = -1;
        }
        break;
      case ';':
        if (!isEscaped1 && !isEscaped2) {
          ptCmd = ptToken = i + 1;
          ptQ = -1;
          nBrace = 0;
        }
        break;
      case '{':
      case '}':
        if (!isEscaped1 && !isEscaped2) {
          nBrace += (ch == '{' ? 1 : -1);
          ptToken = i + 1;
          ptQ = -1;
        }
        break;
      default:
        if (!isEscaped1 && !isEscaped2)
          ptQ = -1;
      }
      isEscaped = false;        
     }
    sout[0] = cmd.substring(0, ptCmd);
    sout[1] = (ptToken == ptCmd ? cmd.substring(ptCmd) : cmd.substring(ptCmd, (ptToken > ptQ ? ptToken : ptQ)));
    sout[2] = (ptToken == ptCmd ? null : cmd.substring(ptToken));
    sout[3] = (nBrace > 0 ? "{" : null);
    return sout;
  }


}
