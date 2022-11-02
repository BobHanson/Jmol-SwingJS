/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-24 10:35:00 -0500 (Wed, 24 Jun 2009) $
 * $Revision: 11106 $
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
package org.jmol.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.undo.UndoManager;

import org.jmol.api.JmolScriptEditorInterface;
import org.jmol.awt.FileDropper;
import org.jmol.i18n.GT;
import org.jmol.script.ScriptContext;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.PreferencesDialog;

import javajs.util.PT;

public final class ScriptEditor extends JDialog implements JmolScriptEditorInterface, ActionListener, WindowListener {

  protected EditorTextPane editor;
  private JPanel buttonPanel;
  private JButton openButton;
  private JButton closeButton;
  private JButton loadButton;
  private JButton topButton;
  private JButton fontButton;
  private JButton checkButton;
  private JButton runButton;
  private JButton pauseButton;
  private JButton saveButton;
  private JButton saveAsButton;
  private JButton haltButton;
  private JButton clearButton;
  private JButton stateButton;
  private JButton consoleButton;
  private JButton stepButton;
  private JButton resumeButton;

  private Viewer vwr;

  private int state;

  private final static int STATE_EDITING = 1;
  private final static int STATE_RUNNING = 2;
  private final static int STATE_PAUSED  = 3;
  
  /*
   * methods sendeditorEcho, sendeditorMessage(strStatus), notifyScriptStart(),
   * notifyScriptTermination() are public in case developers want to use
   * ScriptWindow separate from the Jmol application.
   */

  public ScriptEditor() { 
  }

  private JmolConsole jmolConsole;

  protected String title;
  protected String parsedData = "";
  protected ScriptContext parsedContext;
  
  protected SimpleAttributeSet attHighlight;
  protected SimpleAttributeSet attEcho;
  protected SimpleAttributeSet attError;

  ScriptEditor(Viewer vwr, JFrame frame, JmolConsole jmolConsole) {
    super(frame, null, false);
    // from appConsole only;
    setAttributes();
    setTitle(title = GT.$("Jmol Script Editor"));
    this.vwr = vwr;
    this.jmolConsole = jmolConsole;
    layoutWindow(getContentPane());
    setSize(745, 400);
    if (frame != null)
      setLocationRelativeTo(frame);
  }

  private void setAttributes() {
    attHighlight = new SimpleAttributeSet();
    StyleConstants.setBackground(attHighlight, Color.LIGHT_GRAY);
    StyleConstants.setForeground(attHighlight, Color.blue);
    StyleConstants.setBold(attHighlight, true);

    attEcho = new SimpleAttributeSet();
    StyleConstants.setForeground(attEcho, Color.blue);
    StyleConstants.setBold(attEcho, true);

    attError = new SimpleAttributeSet();
    StyleConstants.setForeground(attError, Color.red);
    StyleConstants.setBold(attError, true);

  }
  
  void layoutWindow(Container container) {
    editor = new EditorTextPane();
    editor.setDragEnabled(true);
    editor.setDropTarget(new DropTarget(editor, new FileDropper(null, vwr, this)));
    editor.setEnabled(true);
    editor.setDisabledTextColor(Color.DARK_GRAY);
    buttonPanel = new JPanel();
    JScrollPane editorPane = new JScrollPane(editor);
    updateFontSize();

    consoleButton = setButton(GT.$("Console"));
    if (!vwr.isApplet || vwr.getBooleanProperty("_signedApplet"))
      openButton = setButton(GT.$("Open"));
    fontButton = setButton(GT.$("Font"));
    loadButton = setButton(GT.$("Script"));
    checkButton = setButton(GT.$("Check"));
    topButton = setButton(PT.split(GT.$("Top[as in \"go to the top\" - (translators: remove this bracketed part]"), "[")[0]);
    runButton = setButton(GT.$("Run"));
    pauseButton = setButton(GT.$("Pause"));
    stepButton = setButton(GT.$("Step"));
    resumeButton = setButton(GT.$("Resume"));
    haltButton = setButton(GT.$("Halt"));
    clearButton = setButton(GT.$("Clear"));
    closeButton = setButton(GT.$("Close"));
    saveButton = setButton(GT.$("Save"));
    saveButton.setEnabled(false);
    saveAsButton = setButton(PT.rep(GT.$("&Save As..."), "&", ""));
    saveAsButton.setEnabled(false);
    // container.setLayout(new BorderLayout());
    // container.add(editorPane, BorderLayout.CENTER);
    JPanel buttonPanelWrapper = new JPanel();
    buttonPanelWrapper.setLayout(new BorderLayout());
    buttonPanelWrapper.add(buttonPanel, BorderLayout.CENTER);

    JSplitPane spane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPane,
        buttonPanelWrapper);
    editorPane.setMinimumSize(new Dimension(300, 300));
    editorPane.setPreferredSize(new Dimension(5000, 5000));
    buttonPanelWrapper.setMinimumSize(new Dimension(60, 60));
    buttonPanelWrapper.setMaximumSize(new Dimension(1000, 60));
    buttonPanelWrapper.setPreferredSize(new Dimension(60, 60));
    spane.setDividerSize(0);
    spane.setResizeWeight(0.95);
    container.add(spane);
    // container.setLayout(new BorderLayout());
    // container.add(editorPane,BorderLayout.CENTER);
    // container.add(buttonPanelWrapper,BorderLayout.SOUTH);

  }

  private JButton setButton(String s) {
    JButton b = new JButton(s);
    b.addActionListener(this);
    buttonPanel.add(b);
    return b;
  }
  
  public void notifyScriptStart() {
    setState();
  }

  private void setState() {
    if (vwr.eval.isPaused()) {
      state = STATE_PAUSED;
    } else if (vwr.eval.isExecuting()) {
      state = STATE_RUNNING;
    } else {
      state = STATE_EDITING;
    }
    setEnables();
  }

  private void setEnables() {
    editor.setEnabled(state == STATE_EDITING);
    openButton.setEnabled(state == STATE_EDITING);
    closeButton.setEnabled(state == STATE_EDITING);
    loadButton.setEnabled(state == STATE_EDITING);
    topButton.setEnabled(state == STATE_EDITING);
    fontButton.setEnabled(state == STATE_EDITING);
    checkButton.setEnabled(state == STATE_EDITING);
    runButton.setEnabled(state == STATE_EDITING);
    pauseButton.setEnabled(state == STATE_RUNNING);
    //saveButton.setEnabled(state == STATE_EDIT);
    //saveAsButton.setEnabled(state == STATE_EDIT);
    haltButton.setEnabled(state == STATE_RUNNING || state == STATE_PAUSED);
    clearButton.setEnabled(state == STATE_EDITING);
    if (stateButton != null)
      stateButton.setEnabled(state == STATE_EDITING);
    consoleButton.setEnabled(true);
    stepButton.setEnabled(state == STATE_EDITING || state == STATE_PAUSED);
    resumeButton.setEnabled(state == STATE_PAUSED);
  }

  public synchronized void notifyScriptTermination(@SuppressWarnings("unused") String msg) {
    setState();
    String err = vwr.eval.getErrorMessage();
    if (err == null) {
      editor.editorDoc.clearHighlight();
      editor.setCaretPosition(editor.editorDoc.getLength());
      //vwr.alert(msg);
    } else {
      editor.editorDoc.doHighlight(lastIndices >> 16, lastIndices & 0xFFFF,
          attError);
//      if (hasFocus())
//        vwr.alert(err);
    }

  }
  
  @Override
  public void setVisible(boolean b) {
    super.setVisible(b);
    vwr.getProperty("DATA_API", "scriptEditorState", b ? Boolean.TRUE : Boolean.FALSE);
    if (b)
      editor.grabFocus();
  }
  
  public String getText() {
    return editor.getText();
  }

  
  public void output(String message) {
    setSaveEnabled(null);
    editor.clearContent(message);
  }

  private void setSaveEnabled(String zipName) {
    saveButton.setEnabled(zipName != null);
    saveAsButton.setEnabled(zipName != null);
    zipFileName = zipName;    
  }

  @Override
  public void dispose() {
    super.dispose();
  }

  @Override
  public void notifyContext(ScriptContext context, Object[] data) {
    boolean isOK = (context.errorMessage == null);
    haltButton.setEnabled(isOK);
    pauseButton.setEnabled(isOK);
    resumeButton.setEnabled(false);
    setState();
    if (isOK)
      setContext(context); 
  }

  protected String filename;
  private Map<String, Object> map;
  private boolean noPrefs;
  
  private synchronized void setContext(ScriptContext context) {
    pauseButton.setEnabled(vwr.isScriptExecuting());
    if (context.script.indexOf(JC.SCRIPT_EDITOR_IGNORE) >= 0)
      return; 
    parsedContext = context;
    filename = context.scriptFileName;
    setTitle(title + parsedContext.contextPath);
    if (filename == null && context.functionName != null)
      filename = "function " + context.functionName; 
    //pcLast = context.pc;
    parsedData = editor.editorDoc.outputEcho(context.script);
    setState();
  }

  private void gotoCommand(int pt, SimpleAttributeSet attr) {
    ScriptContext context = parsedContext;
    try {
      try {
        setVisible(true);
        System.out.println(editor.getText());
        int pt2;
        int pt1;
        if (pt < 0) {
          pt1 = 0;
          pt2 = editor.getDocument().getLength();
        } else if (context == null || context.getTokenCount() < 0) {
          pt1 = pt2 = 0;
        } else if (pt < context.getTokenCount()) {
          pt1 = context.lineIndices[pt][0];
          pt2 = context.lineIndices[pt][1];
          if (pt1 == pt2 && pt1 >= 2)
            pt1 -= 2;
        } else {
          pt1 = pt2 = editor.getDocument().getLength();
        }
        editor.setCaretPosition(pt1);
        editor.editorDoc.doHighlight(pt1, pt2, attr);
      } catch (Exception e) {
        //editor.grabFocus();
        editor.setCaretPosition(0);
        // do we care?
      }
    } catch (Error er) {
      // no. We don't.
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    checkAction(e);
  }
  
  private synchronized void checkAction(ActionEvent e) {
    Object source = e.getSource();
    if (source == consoleButton) {
      jmolConsole.setVisible(true);
      return;
    }
    if (source == openButton) {
      doOpen();
      return;
    }
    if (source == closeButton) {
      setVisible(false);
      return;
    }
    if (source == loadButton) {
      setContext(vwr.getScriptContext("SE loadButton"));
      return;
    }
    if (source == topButton) {
      gotoTop();
      return;
    }
    if (source == fontButton) {
      doFont();
      return;
    }
    if (source == checkButton) {
      checkScript(0);
      return;
    }
    if (source == runButton) {
      doRun();
      return;
    }
    if (source == pauseButton) {
      doPause();
      return;
    }
    if (source == resumeButton) {
      doResume();
      return;
    }
    if (source == stepButton) {
      doStep();
      return;
    }
    if (source == clearButton) {
      editor.clearContent();
      return;
    }
    if (source == stateButton) {
      editor.clearContent(vwr.getStateInfo());
      return;
    }
    if (source == haltButton) {
      doHalt();
      return;
    }
    if (source == saveButton) {
      saveZip(false);
      return;
    }
    if (source == saveAsButton) {
      saveZip(true);
      return;
    }

  }
 
  private void doFont() {
    if (!Viewer.isJS && !noPrefs) {
      if (updateFont())
        return;
    }
    vwr.setConsoleFontScale((vwr.getConsoleFontScale() + 1) % 5);
    updateFontSize();
  }

  private void doRun() {
    notifyScriptStart();
    String s = editor.getText();
    jmolConsole.execute(s + JC.SCRIPT_ISEDITOR);
  }

  private void doPause() {
    jmolConsole.execute("!pause" + JC.SCRIPT_EXT);
    state = STATE_PAUSED;
    setEnables();
  }

  public void doStep() {
    boolean isPaused = vwr.eval.isPaused();
    int pos = editor.getCaretPosition();
    int len = editor.getText().length();
    if (pos == len)
      pos = 0;
    String s = (isPaused ? "!step" + JC.SCRIPT_EXT 
        : editor.getText()  + JC.SCRIPT_ISEDITOR + JC.SCRIPT_STEP + JC.SCRIPT_START +  pos);
    System.out.println(s);
    jmolConsole.execute(s);
    state = STATE_RUNNING;
    setEnables();
  }

  protected void doResume() {
    //editor.clearContent();
    jmolConsole.execute("!resume" + JC.SCRIPT_EXT);
    setState();
  }

  private void gotoPosition(int i, int j) {
    editor.scrollRectToVisible(new Rectangle(i, j));
  }

  private void doHalt() {
    vwr.haltScriptExecution();
    setState();
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

  @SuppressWarnings("unchecked")
  private void saveZip(boolean isAs) {
    if (isAs) {
    // TODO
    } else {
      String script = editor.getText().trim();
      if (script.startsWith("load "))
        script = script.substring(script.indexOf(";") +  1);
      map = (Map<String, Object>) vwr.fm.getFileAsMap(zipFileName, null, false);
      if (map == null)
        return;
      map.put("movie.spt", script);
    }
  }

  private int fontSize;
  private String zipFileName;

  public void updateFontSize() {
    int scale = (Viewer.isJS || noPrefs ? vwr.getConsoleFontScale()
        : PT.parseInt("" + (String) vwr.getProperty("DATA_API", "getPreference",
            "consoleFontScale")));
    scale = (scale < 0 ? 1 : scale) % 5;
    fontSize = scale * 4 + 12;
    if (editor != null)
      editor.setFont(new Font("dialog", Font.PLAIN, fontSize));
  }

  private static String[] lastOpened = {"?.spt", null} ;
  private void doOpen() {
    new Thread() {
      @Override
      public void run() {
        open();
      }
    }.start();
  }

  protected void open() {
    vwr.fm.getFileDataAsString(lastOpened, -1, false, false, true);
    editor.clearContent(lastOpened[1]);
    lastOpened[1] = null;
  }

  public void gotoTop() {
    editor.setCaretPosition(0);
    editor.grabFocus();
    editor.editorDoc.clearHighlight();
    gotoPosition(0, 0);
  }

  public void checkScript(int i) {
    if (i == 0)
    parsedContext = null;
    parseScript(editor.getText(), i);
  }
  
  protected void parseScript(String text, int i) {
    if (text == null || text.length() == 0) {
      parsedContext = null;
      parsedData = "";
      setTitle(title);
      return;
    }
    if (parsedContext == null || !text.equals(parsedData)) {
      parsedData = text;
      parsedContext = (ScriptContext) vwr.getProperty("DATA_API","scriptCheck", text);
    }
    gotoParsedLine(i);
  }

  private void gotoParsedLine(int i) {
    setTitle(title + " " + parsedContext.contextPath 
        + " -- " + (parsedContext.getTokenCount() < 0 ? "" : parsedContext.getTokenCount() + " commands ") 
        + (parsedContext.iCommandError < 0 ? "" : " ERROR: " + parsedContext.errorType));
    boolean isError = (i == 0 && parsedContext.iCommandError >= 0);
    gotoCommand(isError ? parsedContext.iCommandError : i, isError ? attError : attHighlight);
  }

  class EditorTextPane extends JTextPane {

    EditorDocument editorDoc;
    //JmolViewer vwr;

    boolean checking = false;

    EditorTextPane() {
      super(new EditorDocument());
      editorDoc = (EditorDocument) getDocument();
      editorDoc.setEditorTextPane(this);
    }

    public void clearContent() {
      filename = null;
      clearContent(null);
    }

    public synchronized void clearContent(String text) {
      editorDoc.outputEcho(text);
      parseScript(text, 0);
    }
    
    @Override
    protected void processKeyEvent(KeyEvent ke) {
      int kcode = ke.getKeyCode();
      int kid = ke.getID();
      if (kid == KeyEvent.KEY_PRESSED) {
        switch (kcode) {
        case KeyEvent.VK_Z:
          if (ke.isControlDown()) {
            if (ke.isShiftDown())
              editor.editorDoc.redo();
            else
              editor.editorDoc.undo();
            return;
          }
          break;
        case KeyEvent.VK_Y:
          if (ke.isControlDown()) {
            editor.editorDoc.redo();
            return;
          }
          break;
        case KeyEvent.VK_F5:
//          if (stepButton.isEnabled())
//            doStep();
          return;
        case KeyEvent.VK_F8:
//          if (resumeButton.isEnabled())
//            doResume();
          return;
        }
      }
      super.processKeyEvent(ke);
    }
  }

  class EditorDocument extends DefaultStyledDocument {

    EditorTextPane EditorTextPane;

    EditorDocument() {
      super();
      putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
      addUndoableEditListener(new MyUndoableEditListener());
    }

    void setEditorTextPane(EditorTextPane EditorTextPane) {
      this.EditorTextPane = EditorTextPane;
    }

    
    
    void doHighlight(int from, int to, SimpleAttributeSet attr) {
      clearHighlight();
      if (from >= to)
        return;
      setCharacterAttributes(from, to - from, attr, true);
      editor.select(from, to);
      editor.setSelectedTextColor(attr == attError ? Color.RED : Color.black);

    }

    void clearHighlight() {
      setCharacterAttributes(0, editor.editorDoc.getLength(), attEcho, true);
    }

    protected UndoManager undo = new UndoManager();

    protected class MyUndoableEditListener implements UndoableEditListener {
      @Override
      public void undoableEditHappened(UndoableEditEvent e) {
        // Remember the edit and update the menus
        undo.addEdit(e.getEdit());
        // undoAction.updateUndoState();
        // redoAction.updateRedoState();
      }
    }  

    protected void undo() {
      try {
        undo.undo();
      } catch (Exception e) {
        //
      }
    }
    
    protected void redo() {
      try {
        undo.redo();
      } catch (Exception e) {
        //
      }
    }
    
    /**
     * Removes all content of the script window, and add a new prompt.
     */
    void clearContent() {
      try {
        super.remove(0, getLength());
      } catch (Exception exception) {
        //Logger.error("Could not clear script window content", exception);
      }
    }

    String outputEcho(String text) {
      clearContent();
      if (text == null)
        return "";
      int pt = text.indexOf('\1');
      if (pt >= 0)
        text = text.substring(0, pt).trim();
      if (!text.endsWith("\n"))
        text += "\n";
      try {
        super.insertString(0, text, attEcho);
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
      return text;
    }
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  @Override
  public void show(String[] fileText) {
    if (fileText != null) {
      if (fileText[1] == null)
        fileText[1] = "#no data#";
      String filename = fileText[0];
      String msg = fileText[1];
      if (msg != null) {
        setFilename(filename);
        output(FileManager.getEmbeddedScript(msg));
      }
    }
    setVisible(true);
  }

  @Override
  public void windowOpened(WindowEvent e) {
  }

  @Override
  public void windowClosing(WindowEvent e) {
  }

  @Override
  public void windowClosed(WindowEvent e) {
  }

  @Override
  public void windowIconified(WindowEvent e) {
  }

  @Override
  public void windowDeiconified(WindowEvent e) {
  }

  @Override
  public void windowActivated(WindowEvent e) {
    updateFontSize();
  }

  @Override
  public void windowDeactivated(WindowEvent e) {
  }

  @Override
  public void loadContent(String script) {
    System.out.println("SCRIPT is " + script);
  }

  @Override
  public void loadFile(String fileName) {
    if (FileManager.isEmbeddable(fileName)) {
      try {
        setSaveEnabled(fileName);
        output(vwr.fm.getEmbeddedFileState(fileName, false, "movie.spt"));
      } catch (Throwable e) {
        // ignore
      }

      try {
        String data = vwr.fm.getEmbeddedFileState(fileName, false, "state.spt");
        if (data.indexOf("preferredWidthHeight") >= 0)
          vwr.sm.resizeInnerPanelString(data);
        return;
      } catch (Throwable e) {
        // ignore
      }
    } else if (fileName.endsWith("spt")) {
      output(vwr.getAsciiFileOrNull(fileName));
      return;
    }
  }

  @Override
  public void notify(int msWalltime, Object[] data) {
     if (data == null) {
      notifyThisCommand(msWalltime, attHighlight);
      return;
    }
    if (msWalltime == Integer.MAX_VALUE) {
      notifyFont(((Integer)data[0]).intValue());
    } else if (msWalltime == 0) {
      notifyDone();
    } else if (msWalltime > 0) {
      // termination -- button legacy
      notifyScriptTermination((String) data[1]);
    } else if (msWalltime < 0) {
      //if (msWalltime == -2)
      if (msWalltime == -1) {
        //notifyScriptTermination();
      } else {
        notifyScriptStart();
      }
    } else if (isVisible()
        && ((String) data[2]).length() > 0) {
      notifyContext(vwr.getScriptContext("SE notify"), data);
    }
    setState();
 }

  
  private void notifyFont(int fontSize) {
    editor.setFont(new Font("dialog", Font.PLAIN, fontSize));
  }

  private void notifyDone() {
    state = STATE_EDITING;
    setState();
  }

  int lastIndices;
  
  private void notifyThisCommand(int indices, SimpleAttributeSet att) {
    try {
      lastIndices = indices;
      setState();
      editor.editorDoc.clearHighlight();
      editor.editorDoc.doHighlight(indices >> 16, indices & 0xFFFF, att);
    } catch (Exception e) {
      
    }
  }
}
