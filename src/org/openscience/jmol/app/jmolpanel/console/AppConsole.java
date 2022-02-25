/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.openscience.jmol.app.jmolpanel.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.Map;

import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.jmol.api.JmolAbstractButton;
import org.jmol.api.JmolDropEditor;
import org.jmol.api.JmolScriptManager;
import org.jmol.api.JmolStatusListener;
import org.jmol.api.JmolViewer;
import org.jmol.awt.FileDropper;
import org.jmol.awt.Platform;
import org.jmol.console.JmolConsole;
import org.jmol.console.ScriptEditor;
import org.jmol.i18n.GT;
import org.jmol.script.T;
import org.jmol.util.CommandHistory;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.PreferencesDialog;

import javajs.util.PT;

public class AppConsole extends JmolConsole
    implements EnterListener, JmolDropEditor {

  public static final String ALL_BUTTONS = "Editor Variables Clear History State UndoRedo Close Font Help";

  public JDialog jcd;
  protected ConsoleTextPane console;
  protected ConsoleDocument consoleDoc;

  private JmolAbstractButton varButton, haltButton, closeButton, clearButton,
      stepButton;
  private JmolAbstractButton helpButton, undoButton, redoButton, checkButton,
      topButton, fontButton;
  private JPanel buttonPanel = new JPanel();

  protected JScrollBar vBar, hBar;

  private int fontSize;
  private boolean noPrefs;


  @Override
  public void loadContent(String script) {
    getScriptEditor().setVisible(true);
    getScriptEditor().loadContent(script);
  }

  @Override
  public void loadFile(String fileName) {
    ScriptEditor se = (ScriptEditor) getScriptEditor();
    boolean isVisible = se.isVisible();
    getScriptEditor().loadFile(fileName);
    if (!isVisible)
      se.setVisible(false);    
    vwr.openFileAsyncSpecial(fileName, JmolScriptManager.NO_AUTOPLAY | JmolScriptManager.SCRIPT_ONLY | JmolScriptManager.PDB_CARTOONS);
  }

  // note:  "Check" "Top" "Step" not included in 12.1

  public AppConsole() {
    // required for Class.forName  
    // should be used only in the context:
    // appConsole = ((JmolApplicationConsoleInterface) Interface
    //       .getApplicationInterface("jmolpanel.AppConsole")).getAppConsole(vwr, display);
    // appConsole.start(vwr);
  }

  @Override
  public void start(Viewer vwr) {
    setup(vwr, null, null);
  }

  /**
   * general entry point
   * 
   * @param vwr
   * @param externalContainer
   *        a JFrame or JPanel or JDialog
   * @param enabledButtons
   */
  public AppConsole(JmolViewer vwr, Container externalContainer,
      String enabledButtons) {
    setup((Viewer) vwr, externalContainer, enabledButtons);
  }

  private void setup(Viewer vwr, Container externalContainer,
                     String enabledButtons) {

    //    boolean isMacOs = (/** @j2sNative false && */System.getProperty("os.name").toLowerCase()
    //        .contains("mac"));
    //system.out.println("appConsole " + System.getProperty("os.name"));
    if (!Viewer.isJS) {// || isMacOs) {
      // See http://stackoverflow.com/questions/7252749/how-to-use-command-c-command-v-shortcut-in-mac-to-copy-paste-text#answer-7253059
      InputMap im = (InputMap) UIManager.get("TextPane.focusInputMap");
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.META_DOWN_MASK),
          DefaultEditorKit.selectAllAction);
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK),
          DefaultEditorKit.copyAction);
      im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.META_DOWN_MASK),
          DefaultEditorKit.pasteAction);
      //    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
    }

    setViewer(vwr);
    Window w = Platform.getWindow((Container) vwr.display);
    vwrFrame = (w instanceof JFrame ? (JFrame) w : null);
    if (externalContainer == null) {
      jcd = new JDialog(vwrFrame, null, false);
      jcd.setName("JmolConsole");
      jcd.setSize(700, 400);
      jcd.setLocationRelativeTo(vwrFrame);
      this.externalContainer = jcd;
    } else {
      this.externalContainer = externalContainer;
      vwr.setConsole(this);
    }
    addWindowListener();
    layoutWindow(enabledButtons);
    new FileDropper(statusListener, vwr, this);

    //setVisible(true);
  }

  @Override
  public void updateFontSize() {
    int scale = (Viewer.isJS || noPrefs ? vwr.getConsoleFontScale()
        : PT.parseInt("" + (String) vwr.getProperty("DATA_API", "getPreference",
            "consoleFontScale")));
    scale = (scale < 0 ? 1 : scale) % 5;
    fontSize = scale * 4 + 12;
    if (console != null)
      console.setFont(new Font("dialog", Font.PLAIN, fontSize));
    vwr.notifyScriptEditor(Integer.MAX_VALUE, new Object[] { Integer.valueOf(fontSize)});
  }

  /*
   * methods sendConsoleEcho and sendConsoleMessage(strStatus)
   * are public in case developers want to use appConsole separate from the Jmol application.
   * 
   */

  @Override
  public void sendConsoleEcho(String strEcho) {
    if (strEcho == null) // new language
      updateLabels();
    else if (strEcho.equals("\0"))
      sendConsoleMessage(null);
    else
      console.outputEcho(strEcho);
    setError(false);
  }

  @Override
  public void sendConsoleMessage(String strStatus) {
    if (strStatus == null) {
      console.clearContent(null);
      console.outputStatus("");
    } else if (strStatus.indexOf("ERROR:") >= 0) {
      console.outputError(strStatus);
      setError(true);
    } else {
      console.outputStatus(strStatus);
      isError = false;
    }
    //system.out.println("AppConsole " + console.getText().length());
  }

  @Override
  protected JmolAbstractButton setButton(String label) {
    JmolAbstractButton b = super.setButton(label);
    buttonPanel.add((JButton) b);
    return b;
  }

  @Override
  protected void setupLabels0(Map<String, String> labels) {
    labels.put("Check", GT.$("Check"));
    labels.put("Clear", GT.$("Clear"));
    labels.put("Close", GT.$("Close"));
    labels.put("Halt", GT.$("Halt"));
    labels.put("Help", GT.$("Help"));
    labels.put("Editor", GT.$("Editor"));
    labels.put("History", GT.$("History"));
    labels.put("State", GT.$("State"));
    labels.put("Step", GT.$("Step"));
    labels.put("Top", GT.$("Top"));
    labels.put("Undo", GT.$("Undo"));
    labels.put("Redo", GT.$("Redo"));
    labels.put("Font", GT.$("Font"));
    labels.put("Variables", GT.$("Variables"));
  }

  @Override
  protected void layoutWindow(String enabledButtons) {
    setTitle();
    console = new ConsoleTextPane(this);
    console.setName("JmolConsole");
    console.setDropTarget(
        new DropTarget(console, new FileDropper(null, vwr, this)));
    console.setPrompt();
    console.setDragEnabled(true);
    if (enabledButtons == null)
      enabledButtons = ALL_BUTTONS;
    JScrollPane consolePane = new JScrollPane(console);
    vBar = consolePane.getVerticalScrollBar();
    hBar = consolePane.getHorizontalScrollBar();
    String[] tokens = PT.getTokens(enabledButtons);
    for (int i = 0; i < tokens.length; i++)
      enableButton(tokens[i]);
    setEnabled(undoButton, false);
    setEnabled(redoButton, false);

    //    container.setLayout(new BorderLayout());
    //  container.add(consolePane, BorderLayout.CENTER);
    JPanel buttonPanelWrapper = new JPanel();
    buttonPanelWrapper.setLayout(new BorderLayout());
    buttonPanelWrapper.add(buttonPanel, BorderLayout.CENTER);

    JSplitPane spane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, consolePane,
        buttonPanelWrapper);
    spane.setName("JmolConsoleSplitPane");

    consolePane.setMinimumSize(new Dimension(300, 300));
    consolePane.setPreferredSize(new Dimension(5000, 5000));
    buttonPanelWrapper.setMinimumSize(new Dimension(60, 60));
    buttonPanelWrapper.setMaximumSize(new Dimension(1000, 60));
    buttonPanelWrapper.setPreferredSize(new Dimension(60, 60));
    spane.setDividerSize(0);
    spane.setResizeWeight(0.95);
    getPane().add(spane);
    //    container.setLayout(new BorderLayout());
    //  container.add(consolePane,BorderLayout.CENTER);
    //container.add(buttonPanelWrapper,BorderLayout.SOUTH);

  }

  private static void setEnabled(JmolAbstractButton button, boolean TF) {
    if (button != null)
      button.setEnabled(TF);
  }

  private void enableButton(String name) {
    switch (("Check     " + "Clear     " + "Close     " + "Editor    "
        + "Halt      " + "Help      " + "History   " + "State     "
        + "Step      " + "Top       " + "UndoRedo  " + "Font      "
        + "Variables ").indexOf(name)) {
    case 0:
      checkButton = setButton("Check");
      break;
    case 10:
      clearButton = setButton("Clear");
      break;
    case 20:
      closeButton = setButton("Close");
      break;
    case 30:
      editButton = setButton("Editor");
      break;
    case 40:
      haltButton = setButton("Halt");
      break;
    case 50:
      helpButton = setButton("Help");
      break;
    case 60:
      historyButton = setButton("History");
      break;
    case 70:
      stateButton = setButton("State");
      break;
    case 80:
      stepButton = setButton("Step");
      break;
    case 90:
      topButton = setButton("Top");
      break;
    case 100:
      undoButton = setButton("Undo");
      redoButton = setButton("Redo");
      break;
    case 110:
      varButton = setButton("Variables");
      break;
    case 120:
      fontButton = setButton("Font");
      fontButton.setToolTipText(GT.$("toggle font size"));
    }
  }

  boolean isError = false;

  private void setError(boolean TF) {
    isError = TF;
  }

  @Override
  public void enterPressed() {
    executeCommandAsThread(null);
  }

  class ExecuteCommandThread extends Thread {

    String strCommand;

    ExecuteCommandThread(String command) {
      strCommand = command;
      this.setName("appConsoleExecuteCommandThread");
    }

    @Override
    public void run() {

      try {

        //        while (console.checking) {
        //          try {
        //            Thread.sleep(100); //wait for command checker
        //          } catch (Exception e) {
        //            break; //-- interrupt? 
        //          }
        //        }

        executeCommand(strCommand);
      } catch (Exception ie) {
        Logger.errorEx("execution command interrupted!", ie);
      }
    }
  }

  ExecuteCommandThread execThread;

  @Override
  protected void execute(String strCommand) {
    executeCommandAsThread(strCommand);
  }

  void executeCommandAsThread(String strCommand) {
    if (strCommand == null)
      strCommand = console.getCommandString().trim();
    boolean isUndo = strCommand.equalsIgnoreCase("undo");
    if ((isUndo || strCommand.equalsIgnoreCase("redo")) && checkUndoEnabled() != UNDO_USER) {
        undoRedo(isUndo);
        console.appendNewline();
        console.setPrompt();
        return;
    } else if (strCommand.equalsIgnoreCase("exitJmol")) {
      System.exit(0);
    } else if (strCommand.startsWith("font console")) {
      String s = strCommand.substring(12);
      if (PT.parseInt(s) > 0)
        s = "sansserif-" + s;
      console.setFont(Font.decode(s));
      strCommand = " ";
    } else if (strCommand.length() == 0) {
      strCommand = "!resume";
      checkUndoEnabled();
    }

    if (strCommand.length() > 0) {
      execThread = new ExecuteCommandThread(strCommand);
      execThread.start();
      //can't do this: 
      //SwingUtilities.invokeLater(execThread);
      //because then the thread runs from the event queue, and that 
      //causes PAUSE to hang the application on refresh()
    }
  }

  private static int MAXUNDO = 10;
  private String[] undoStack = new String[MAXUNDO + 1];
  private int undoPointer = 0;
  private boolean undoSaved = false;

  private void undoClear() {
    if (undoButton == null)
      return;
    for (int i = 0; i <= MAXUNDO; i++)
      undoStack[i] = null;
    undoPointer = 0;
    undoButton.setEnabled(false);
    redoButton.setEnabled(false);
  }

  final static int UNDO_NONE = 0;
  final static int UNDO_CONSOLE = 1;
  final static int UNDO_USER = 2;
  
  public int checkUndoEnabled() {
    boolean undoAllowed = vwr.getBooleanProperty("undoAuto");
    if (undoAllowed) {
      if (undoButton == null)
        return UNDO_NONE;
      undoAllowed = vwr.getBoolean(T.preservestate);
      undoButton.setEnabled(
          undoAllowed && undoPointer > 0 && undoStack[undoPointer - 1] != null);
      redoButton.setEnabled(undoAllowed && undoPointer < MAXUNDO
          && undoStack[undoPointer + 1] != null);
      return UNDO_CONSOLE;
    }
    // user has turned the console undo off
    if (undoButton != null && vwr.stm.getUndoMax() > 0) {
      undoButton.setEnabled(vwr.stm.canDo(T.undo));
      redoButton.setEnabled(vwr.stm.canDo(T.redo));
    }
    return UNDO_USER;
  }

  private void undoRedo(boolean isRedo) {
    // pointer is always left at the undo slot when a command is given
    // redo at CURRENT pointer position
    switch (checkUndoEnabled()) {
    case UNDO_NONE:
      return;
    case UNDO_USER:
      executeCommand(isRedo ? "redo" : "undo");
      return;
    }
    //dumpUndo("undoRedo1");
    int ptr = undoPointer + (isRedo ? 1 : -1);
    if (!undoSaved) {
      undoSave(false);
    }
    //dumpUndo("undoRedo2");
    if (ptr > MAXUNDO || ptr < 0)
      return;
    String state = undoStack[ptr];
    if (state != null) {
      state += CommandHistory.NOHISTORYATALL_FLAG;
      setError(false);
      vwr.evalStringQuiet(state);
      undoPointer = ptr;
    }
    checkUndoEnabled();
    //dumpUndo("undoRedo DONE");
  }

  private boolean dontsave;

  private JmolStatusListener statusListener;

  private void undoSave(boolean incrementPtr) {
    if (undoButton == null)
      return;
    if (!vwr.getBooleanProperty("undoAuto")
        || !vwr.getBoolean(T.preservestate))
      return;
    //delete redo items, since they will no longer be valid
    for (int i = undoPointer + 1; i <= MAXUNDO; i++)
      undoStack[i] = null;
    Logger.startTimer("(console");
    try {
      undoStack[undoPointer] = (String) vwr.getProperty("readable", "stateInfo",
          null);
      //shift stack if full
      if (incrementPtr && undoPointer == MAXUNDO) {
        for (int i = 1; i <= MAXUNDO; i++)
          undoStack[i - 1] = undoStack[i];
        undoStack[MAXUNDO] = null;
      } else if (incrementPtr)
        undoPointer++;
    } catch (Error e) {
      dontsave = true;
    }
    if (dontsave || Logger.checkTimer("(console", false) > 2000) {
      vwr.setBooleanProperty("undoAuto", false);
      undoClear();
      Logger.info("command processing slow; undo disabled");
    } else {
      checkUndoEnabled();
    }
    undoSaved = true;
    //dumpUndo("undoSave DONE");
  }

  /*
  private void dumpUndo(String string) {
    //system.out.println("\n" + string);
    for (int i = 0; i < 8 && i <= MAXUNDO; i++)
      //system.out.println((i == undoPointer ? ">" : " ") + i 
          + "\t" + (undoStack[i] == null ? null : "OK\t" + undoStack[i].substring(undoStack[i].indexOf(" background "),undoStack[i].indexOf(" background ") + 22 ) ));
     
    return;
  }
  */
  @SuppressWarnings("unchecked")
  void executeCommand(String strCommand) {
    boolean doWait;
    console.appendNewline();
    console.setPrompt();
    if (strCommand.length() == 0) {
      console.grabFocus();
      return;
    }
    if (strCommand.charAt(0) != '!'
        && vwr.getBooleanProperty("executionPaused"))
      strCommand = "!" + strCommand;
    if (strCommand.charAt(0) != '!' && !isError) {
      undoSave(true);
    }
    setError(false);
    undoSaved = false;

    String strErrorMessage = null;
    doWait = (strCommand.indexOf("WAITTEST ") == 0);
    boolean hasExtension = false;
    if (doWait) { //for testing, mainly
      // demonstrates using the statusManager system; probably hangs application.
      Object o = vwr.scriptWaitStatus(strCommand.substring(5),
          "+fileLoaded,+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated");
      if (o instanceof List) {
        List<List<List<Object>>> info = (List<List<List<Object>>>) o;
        /*
         * info = [ statusRecortSet0, statusRecortSet1, statusRecortSet2, ...]
         * statusRecordSet = [ statusRecord0, statusRecord1, statusRecord2, ...]
         * statusRecord = [int msgPtr, String statusName, int intInfo, String msg]    
         */
        for (int i = 0; i < info.size(); i++) {
          List<List<Object>> statusRecordSet = info.get(i);
          for (int j = 0; j < statusRecordSet.size(); j++) {
            List<Object> statusRecord = statusRecordSet.get(j);
            Logger.info("msg#=" + statusRecord.get(0) + " "
                + statusRecord.get(1) + " intInfo=" + statusRecord.get(2)
                + " stringInfo=" + statusRecord.get(3));
          }
        }
      }
      console.appendNewline();
    } else {
      boolean isScriptExecuting = vwr.isScriptExecuting();
      strErrorMessage = "";
      String str = strCommand;
      boolean isInterrupt = (str.charAt(0) == '!');
      hasExtension = (strCommand.indexOf(JC.SCRIPT_EXT) >= 0);
      if (isInterrupt)
        str = str.substring(1);
      if (vwr.checkHalt(str, isInterrupt))
        strErrorMessage = (isScriptExecuting
            ? "script execution halted with " + strCommand
            : "no script was executing - use exitJmol to exit Jmol");
      //the problem is that scriptCheck is synchronized, so these might get backed up. 
      if (strErrorMessage.length() > 0) {
        if (!hasExtension)
          console.outputError(strErrorMessage);
      } else {
        vwr.script(strCommand + JC.SCRIPT_GUI
            + (strCommand.indexOf(JC.SCRIPT_EXT) >= 0 ? "" : JC.SCRIPT_EDITOR_IGNORE));
      }
    }
    if (!hasExtension)
      console.grabFocus();
  }

  @Override
  protected void clearContent(String text) {
    console.clearContent(text);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    console.grabFocus(); // always grab the focus (e.g., after clear)
    Object source = e.getSource();

    if (source == topButton) {
      if (scriptEditor != null)
        scriptEditor.gotoTop();
      return;
    }
    if (source == checkButton) {
      if (scriptEditor != null)
        scriptEditor.checkScript(0);
    }
    if (source == stepButton) {
      if (scriptEditor != null)
        scriptEditor.doStep();
      return;
    }

    if (source == closeButton) {
      setVisible(false);
      return;
    }
    if (source == haltButton) {
      vwr.haltScriptExecution();
      return;
    }
    if (source == varButton) {
      execute("!show variables");
      return;
    }
    if (source == clearButton) {
      console.clearContent(null);
      return;
    }
    if (source == undoButton) {
      undoRedo(false);
      return;
    }
    if (source == redoButton) {
      undoRedo(true);
      return;
    }
    if (source == fontButton) {
      if (!Viewer.isJS && !noPrefs) {
        if (updateFont())
          return;
      }
      vwr.setConsoleFontScale((vwr.getConsoleFontScale() + 1) % 5);
      updateFontSize();
      return;
    }
    if (source == helpButton) {
      //        
      //      
      //      URL url = this.getClass().getClassLoader().getResource(
      //          "org/openscience/jmol/Data/guide/ch04.html");
      //      if (url == null)
      vwr.script("help");
      //      else
      //        (new HelpDialog(null, url)).setVisible(true);
    }
    super.actionPerformed(e);
  }

  private boolean updateFont() {
    PreferencesDialog d = null;
    try {
      d = (PreferencesDialog) vwr.getProperty("DATA_API",
          "getPreference", null);
      if (d != null)
        d.setFontScale(-1);
      return true;
    } catch (Exception ee) {
      noPrefs = true;
    }
    return false;
  }
  
  /**
   * Recall command history.
   * 
   * @param up
   *        - history up or down
   * @param pageup
   *        TODO
   */
  @Override
  protected void recallCommand(boolean up, boolean pageup) {
    String cmd = (pageup ? vwr.historyFind(
        console.pageUpBuffer == null ? (console.pageUpBuffer = consoleDoc.getCommandString())
            : console.pageUpBuffer,
        up ? -1 : 1) : vwr.getSetHistory(up ? -1 : 1));
    if (cmd == null) {
      EventQueue.invokeLater(new Runnable() {
        @Override
        public void run() {
          try {
            hBar.setValue(0);
          } catch (Throwable e) {
            //
          }
        }
      });
      return;
    }
    boolean isError = false;
    try {
      if (cmd.endsWith(CommandHistory.ERROR_FLAG)) {
        isError = true;
        cmd = cmd.substring(0, cmd.indexOf(CommandHistory.ERROR_FLAG));
      }
      cmd = trimGUI(cmd);
      consoleDoc.replaceCommand(cmd, isError);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }



  class ConsoleTextPane extends JTextPane implements KeyListener {

    private EnterListener enterListener;

    boolean checking = false;
    String pageUpBuffer;
    boolean checkingCommand;
    private Timer checkTimer;

    ConsoleTextPane(AppConsole appConsole) {
      super(new ConsoleDocument());
      updateFontSize();
      consoleDoc = (ConsoleDocument) getDocument();
      consoleDoc.setConsoleTextPane(this);
      this.enterListener = appConsole;
      addKeyListener(this);
    }

    public String getCommandString() {
      String cmd = consoleDoc.getCommandString();
      return cmd;
    }

    public void setPrompt() {
      consoleDoc.setPrompt();
    }

    public void appendNewline() {
      consoleDoc.appendNewline();
    }

    public void outputError(String strError) {
      consoleDoc.outputError(strError);
    }

    public void outputErrorForeground(String strError) {
      consoleDoc.outputErrorForeground(strError);
    }

    public void outputEcho(String strEcho) {
      consoleDoc.outputEcho(strEcho);
    }

    public void outputStatus(String strStatus) {
      consoleDoc.outputStatus(strStatus);
    }

    public void enterPressed() {
      if (enterListener != null)
        enterListener.enterPressed();
    }

    public void clearContent(String text) {
      consoleDoc.clearContent();
      if (text != null)
        consoleDoc.outputEcho(text);
      setPrompt();
    }

    /**
     * Custom key event processing for command 0 implementation.
     * 
     * Captures key up and key down strokes to call command history and
     * redefines the same events with control down to allow caret vertical
     * shift.
     * 
     * @see java.awt.Component#processKeyEvent(java.awt.event.KeyEvent)
     */

    @Override
    public void keyTyped(KeyEvent e) {
      processKey(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
      // tab
      processKey(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
      processKey(e);
    }

    protected void processKey(KeyEvent ke) {

      // ps: Wow - was direct overriding of processkeyEvent instead of using a listener! BH 2019

      // Id Control key is down, captures events does command
      // history recall and inhibits caret vertical shift.

      //system.out.println("AppConsole: " + consoleDoc.isAtEnd() + " " + ke);

      int kcode = ke.getKeyCode();
      int kid = ke.getID();
      if (kid == KeyEvent.KEY_PRESSED) {
        switch (kcode) {
        case KeyEvent.VK_C:
          if (ke.isControlDown() && ke.isAltDown()) {
            ke.consume();
            vwr.script("!quit");
            return;
          }
          break;
        case KeyEvent.VK_TAB:
          ke.consume();
          if (consoleDoc.isAtEnd()) {
            String cmd = completeCommand(consoleDoc.getCommandString());
            if (cmd != null)
              try {
                consoleDoc.replaceCommand(cmd, false);
              } catch (BadLocationException e) {
                //
              }
            nTab++;
            //checkCommand();
            return;
          }
          break;
        case KeyEvent.VK_ESCAPE:
          ke.consume();
          try {
            consoleDoc.replaceCommand("", false);
          } catch (BadLocationException e) {
            //
          }
          break;
        }
        nTab = 0;
      }
      if ((kcode == KeyEvent.VK_UP || kcode == KeyEvent.VK_DOWN)
          && ke.isControlDown() && consoleDoc.isAtEnd()) {
        if (kid == KeyEvent.KEY_PRESSED) {
          //System.out.println(pageUpBuffer);
          recallCommand(kcode == KeyEvent.VK_UP, true);
        }
        ke.consume();
        return;
      }
      pageUpBuffer = null;
      switch (kid) {
      case KeyEvent.KEY_PRESSED:
        switch (kcode) {
        case KeyEvent.VK_UP:
        case KeyEvent.VK_DOWN:
          if (consoleDoc.isAtEnd()) {
            recallCommand(kcode == KeyEvent.VK_UP, false);
            ke.consume();
          }
          break;
        }
        break;
      case KeyEvent.KEY_RELEASED:
        // not perfect -- help here?
        if (!checkingCommand && ke.getModifiers() < 2
            && (kcode == 32 || kcode > KeyEvent.VK_DOWN && kcode < 400
                || kcode == KeyEvent.VK_BACK_SPACE)) {
          checkingCommand = true;
          if (checkTimer == null) {
            checkTimer = new Timer(100, new ActionListener() {

              @Override
              public void actionPerformed(ActionEvent e) {
                checkingCommand = false;
                checkCommand();
              }

            });
            checkTimer.setRepeats(false);
            checkTimer.start();
          } else {
            checkTimer.restart();
          }
        }
        break;
      }
    }

    synchronized void checkCommand() {
      String strCommand = consoleDoc.getCommandString();
      //System.out.println("checkCommand " + strCommand);
      if (strCommand.length() == 0 || strCommand.charAt(0) == '!'
          || vwr.isScriptExecuting()
          || vwr.getBooleanProperty("executionPaused"))
        return;
      checking = true;
      Object res = vwr.scriptCheck(strCommand);

      //System.out.println( res);
      consoleDoc.colorCommand(res instanceof String ? consoleDoc.attError
          : consoleDoc.attUserInput);
      checking = false;
    }

  }

  @Override
  public String getText() {
    return console.getText();
  }

  public void setStatusListener(JmolStatusListener myStatusListener) {
    this.statusListener = myStatusListener;
  }

  class ConsoleDocument extends DefaultStyledDocument {

    private ConsoleTextPane consoleTextPane;

    SimpleAttributeSet attError;
    SimpleAttributeSet attEcho;
    SimpleAttributeSet attPrompt;
    SimpleAttributeSet attUserInput;
    SimpleAttributeSet attStatus;

    ConsoleDocument() {
      super();

      attError = new SimpleAttributeSet();
      StyleConstants.setForeground(attError, Color.red);

      attPrompt = new SimpleAttributeSet();
      StyleConstants.setForeground(attPrompt, Color.magenta);

      attUserInput = new SimpleAttributeSet();
      StyleConstants.setForeground(attUserInput, Color.black);

      attEcho = new SimpleAttributeSet();
      StyleConstants.setForeground(attEcho, Color.blue);
      StyleConstants.setBold(attEcho, true);

      attStatus = new SimpleAttributeSet();
      StyleConstants.setForeground(attStatus, Color.black);
      StyleConstants.setItalic(attStatus, true);
    }

    void setConsoleTextPane(ConsoleTextPane consoleTextPane) {
      this.consoleTextPane = consoleTextPane;
    }

    private Position positionBeforePrompt; // starts at 0, so first time isn't tracked (at least on Mac OS X)
    private Position positionAfterPrompt; // immediately after $, so this will track
    private int offsetAfterPrompt; // only still needed for the insertString override and replaceCommand

    boolean isAtEnd() {
      //system.out.println("isAtEnd " + consoleTextPane.getCaretPosition() + " " + getLength());
      return consoleTextPane.getCaretPosition() == getLength();
    }

    /**
     * Removes all content of the script window, and add a new prompt.
     */
    void clearContent() {
      try {
        super.remove(0, getLength());
      } catch (BadLocationException exception) {
        Logger.errorEx("Could not clear script window content", exception);
      }
    }

    void setPrompt() {
      try {
        //system.out.println("AppConsole setting $ ");
        super.insertString(getLength(), "$ ", attPrompt);
        setOffsetPositions();
        setCaretPosition(offsetAfterPrompt);

        //system.out.println("AppConsole caretPosition done " + consoleTextPane.getCaret());

      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    }

    void setOffsetPositions() {
      try {
        offsetAfterPrompt = getLength();
        positionBeforePrompt = createPosition(offsetAfterPrompt - 2);
        // after prompt should be immediately after $ otherwise tracks the end
        // of the line (and no command will be found) at least on Mac OS X it did.
        positionAfterPrompt = createPosition(offsetAfterPrompt - 1);

        //System.out.println("appconsole " + offsetAfterPrompt + " " + positionBeforePrompt + " " + positionAfterPrompt + "\n>>" + this.getText(0,  getLength()) + "<<");
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    }

    void setNoPrompt() {
      try {
        offsetAfterPrompt = getLength();
        positionAfterPrompt = positionBeforePrompt = createPosition(
            offsetAfterPrompt);
        setCaretPosition(offsetAfterPrompt);
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    }

    // it looks like the positionBeforePrompt does not track when it started out as 0
    // and a insertString at location 0 occurs. It may be better to track the
    // position after the prompt in stead
    void outputBeforePrompt(String str, SimpleAttributeSet attribute) {
      try {
        int pt = consoleTextPane.getCaretPosition();
        Position caretPosition = createPosition(pt);
        pt = positionBeforePrompt.getOffset();
        super.insertString(pt, str + "\n", attribute);
        //setOffsetPositions();
        offsetAfterPrompt += str.length() + 1;
        positionBeforePrompt = createPosition(offsetAfterPrompt - 2);
        positionAfterPrompt = createPosition(offsetAfterPrompt - 1);

        pt = caretPosition.getOffset();
        setCaretPosition(pt);
      } catch (Exception e) {
        e.printStackTrace();
        setCaretPosition(getLength());
      }
      EventQueue.invokeLater(new Runnable() {
        @Override
        public void run() {
          try {
            vBar.setValue(vBar.getMaximum());
          } catch (Throwable e) {
            //
          }
        }
      });
    }

    void outputError(String strError) {
      outputBeforePrompt(strError, attError);
    }

    void outputErrorForeground(String strError) {
      try {
        super.insertString(getLength(), strError + "\n", attError);
        setCaretPosition(getLength());
      } catch (BadLocationException e) {
        e.printStackTrace();

      }
    }

    void outputEcho(String strEcho) {
      outputBeforePrompt(strEcho, attEcho);
    }

    void outputStatus(String strStatus) {
      outputBeforePrompt(strStatus, attStatus);
    }

    void appendNewline() {
      try {
        super.insertString(getLength(), "\n", attUserInput);
        setCaretPosition(getLength());
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    }

    // override the insertString to make sure everything typed ends up at the end
    // or in the 'command line' using the proper font, and the newline is processed.
    @Override
    public synchronized void insertString(int offs, String str, AttributeSet a)
        throws BadLocationException {
      int ichNewline = str.indexOf('\n');
      // BH fix 2019.04.16 -- only SwingJS
      if (ichNewline != 0 || str != "\n" && str.length() > 0) {
        if (offs < offsetAfterPrompt) {
          offs = getLength();
        }
        super.insertString(offs, str, a == attError ? a : attUserInput);
        setCaretPosition(offs + str.length());
      }
      if (ichNewline >= 0) {
        consoleTextPane.enterPressed();
      }
    }

    private void setCaretPosition(int p) {
      consoleTextPane.setCaretPosition(p);
    }

    String getCommandString() {
      String strCommand = "";
      try {
        int cmdStart = positionAfterPrompt.getOffset();
        strCommand = getText(cmdStart, getLength() - cmdStart);
        while (strCommand.length() > 0 && strCommand.charAt(0) == ' ')
          strCommand = strCommand.substring(1);
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
      return strCommand;
    }

    @Override
    public void remove(int offs, int len) throws BadLocationException {
      if (offs < offsetAfterPrompt) {
        len -= offsetAfterPrompt - offs;
        if (len <= 0)
          return;
        offs = offsetAfterPrompt;
      }
      super.remove(offs, len);
      //      consoleTextPane.setCaretPosition(offs);
    }

    @Override
    public void replace(int offs, int length, String str, AttributeSet attrs)
        throws BadLocationException {
      if (offs < offsetAfterPrompt) {
        if (offs + length < offsetAfterPrompt) {
          offs = getLength();
          length = 0;
        } else {
          length -= offsetAfterPrompt - offs;
          offs = offsetAfterPrompt;
        }
      }
      super.replace(offs, length, str, attrs);
      //      consoleTextPane.setCaretPosition(offs + str.length());
    }

    /**
     * Replaces current command on script.
     * 
     * @param newCommand
     *        new command value
     * @param isError
     *        true to set error color ends with #??
     * 
     * @throws BadLocationException
     */
    void replaceCommand(String newCommand, boolean isError)
        throws BadLocationException {
      if (positionAfterPrompt == positionBeforePrompt)
        return;
      replace(offsetAfterPrompt, getLength() - offsetAfterPrompt, newCommand,
          isError ? attError : attUserInput);
    }

    void colorCommand(SimpleAttributeSet att) {
      if (positionAfterPrompt == positionBeforePrompt)
        return;
      //System.out.println(offsetAfterPrompt + "  " + getLength() + " att is " + att);
      setCharacterAttributes(offsetAfterPrompt, getLength() - offsetAfterPrompt,
          att, true);
    }
  }

}

interface EnterListener {
  public void enterPressed();
}
