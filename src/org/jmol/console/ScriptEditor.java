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

import javajs.util.PT;

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

public final class ScriptEditor extends JDialog implements JmolScriptEditorInterface, ActionListener, WindowListener {

  protected EditorTextPane editor;
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
  
  protected JButton stepButton;
  protected JButton resumeButton;

  private Viewer vwr;

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
  
  private JPanel buttonPanel = new JPanel();

  void layoutWindow(Container container) {
    editor = new EditorTextPane();
    editor.setDragEnabled(true);
    editor.setDropTarget(new DropTarget(editor, new FileDropper(null, vwr, this)));
    JScrollPane editorPane = new JScrollPane(editor);
    updateFontSize();

    consoleButton = setButton(GT.$("Console"));
    if (!vwr.isApplet || vwr.getBooleanProperty("_signedApplet"))
      openButton = setButton(GT.$("Open"));
    fontButton = setButton(GT.$("Font"));
    loadButton = setButton(GT.$("Script"));
    checkButton = setButton(GT.$("Check"));
    topButton = setButton(PT.split(GT.$("Top[as in \"go to the top\" - (translators: remove this bracketed part]"), "[")[0]);
    stepButton = setButton(GT.$("Step"));
    runButton = setButton(GT.$("Run"));
    pauseButton = setButton(GT.$("Pause"));
    pauseButton.setEnabled(true);
    resumeButton = setButton(GT.$("Resume"));
    resumeButton.setEnabled(false);
    haltButton = setButton(GT.$("Halt"));
    haltButton.setEnabled(false);
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
  
  @Override
  public void notifyScriptStart() {
    runButton.setEnabled(false);
    resumeButton.setEnabled(false);
    haltButton.setEnabled(true);
    pauseButton.setEnabled(true);
  }

  @Override
  public void notifyScriptTermination() {
    runButton.setEnabled(true);
    pauseButton.setEnabled(false);
    resumeButton.setEnabled(false);
    haltButton.setEnabled(false);
    editor.editorDoc.clearHighlight();
    editor.setCaretPosition(editor.editorDoc.getLength());
  }

  @Override
  public void setVisible(boolean b) {
    super.setVisible(b);
    vwr.getProperty("DATA_API", "scriptEditorState", b ? Boolean.TRUE : Boolean.FALSE);
    if (b)
      editor.grabFocus();
  }
  
  @Override
  public String getText() {
    return editor.getText();
  }

  
  @Override
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

  /*
   * (non-Javadoc)
   * @see org.jmol.api.JmolScriptEditorInterface#notifyContext(org.jmol.script.ScriptContext, java.lang.Object[])
   * 
   * from org.jmol.viewer.StatusManager:
   * 
      if (isScriptCompletion && vwr.getMessageStyleChime()
          && vwr.getDebugScript()) {
        jmolCallbackListener.notifyCallback(EnumCallback.SCRIPT,
            new Object[] { null, "script <exiting>", statusMessage,
                Integer.valueOf(-1), strErrorMessageUntranslated });
        strStatus = "Jmol script completed.";
      }
      jmolCallbackListener.notifyCallback(EnumCallback.SCRIPT,
          new Object[] { sJmol, strStatus, statusMessage,
              Integer.valueOf(isScriptCompletion ? -1 : msWalltime),
              strErrorMessageUntranslated });
   */
  
  @Override
  public void notifyContext(ScriptContext context, Object[] data) {
    haltButton.setEnabled(context.errorMessage == null);
    pauseButton.setEnabled(context.errorMessage == null);
    resumeButton.setEnabled(false);
    if (context.errorMessage == null)
      setContext(context); 
  }

  protected String filename;
  private Map<String, Object> map;
  
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
    boolean isPaused = context.executionPaused || context.executionStepping;
    pauseButton.setEnabled(!isPaused && vwr.isScriptExecuting());
    resumeButton.setEnabled(isPaused);
    gotoCommand(context.pc, isPaused, attHighlight);
  }
  
  private void gotoCommand(int pt, boolean isPaused, SimpleAttributeSet attr) {    
    ScriptContext context = parsedContext;
    try {
      try {
        setVisible(true);
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
          //System.out.println ("cursor set to " + pt + " ispaused " + isPaused + " " + context.pc + " " + context.lineIndices.length);
        } else {
          pt1 = pt2 = editor.getDocument().getLength();
        }
        if (isPaused) {
          editor.setCaretPosition(pt1);
          editor.editorDoc.doHighlight(pt1, pt2, attr);
          //System.out.println("highlighting /" + editor.editorDoc.getText(pt1,pt2 - pt1) + "/");
        }
        //editor.grabFocus();
      } catch (Exception e) {
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
      PreferencesDialog d = (PreferencesDialog) vwr.getProperty("DATA_API", "getPreference", null);
      if (d != null)
        d.setFontScale(-1);
      updateFontSize();
      return;
    }
    if (source == checkButton) {
      checkScript();
      return;
    }
    if (source == runButton) {
      notifyScriptStart();
      String s = editor.getText();
      jmolConsole.execute(s + "\1##");
      return;
    }
    if (source == pauseButton) {
      jmolConsole.execute("!pause\1##");
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
      vwr.haltScriptExecution();
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
 
  private void saveZip(boolean isAs) {
    if (isAs) {
    // TODO
    } else {
      String script = editor.getText().trim();
      if (script.startsWith("load "))
        script = script.substring(script.indexOf(";") +  1);
      map = vwr.fm.getFileAsMap(zipFileName, null);
      if (map == null)
        return;
      map.put("movie.spt", script);
    }
  }

  private int fontSize;
  private String zipFileName;

  public void updateFontSize() {
    int scale = PT.parseInt("" + (String) vwr.getProperty("DATA_API", "getPreference", "consoleFontScale"));
    scale = (scale < 0 ? 1 : scale) % 5;
    fontSize = scale * 4 + 12;
    if (editor != null)
      editor.setFont(new Font("dialog", Font.PLAIN, fontSize));
  }

  private static String[] lastOpened = {"?.spt", null} ;
  private void doOpen() {
    vwr.fm.getFileDataAsString(lastOpened, -1, false, false, true);
    editor.clearContent(lastOpened[1]);
    lastOpened[1] = null;
  }

  public void gotoTop() {
    editor.setCaretPosition(0);
    editor.grabFocus();
    gotoPosition(0, 0);
  }

  public void checkScript() {
    parsedContext = null;
    parseScript(editor.getText());
  }
  
  protected void parseScript(String text) {
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
    gotoParsedLine();
  }

  private void gotoParsedLine() {
    setTitle(title + " " + parsedContext.contextPath 
        + " -- " + (parsedContext.getTokenCount() < 0 ? "" : parsedContext.getTokenCount() + " commands ") 
        + (parsedContext.iCommandError < 0 ? "" : " ERROR: " + parsedContext.errorType));
    boolean isError = (parsedContext.iCommandError >= 0);
    gotoCommand(isError ? parsedContext.iCommandError : 0, true, isError ? attError : attHighlight);
  }

  public void doStep() {
    boolean isPaused = vwr.getBooleanProperty("executionPaused");
    jmolConsole.execute(isPaused ? "!step\1##" 
        : editor.getText() + "\1##SCRIPT_STEP\n##SCRIPT_START=" +  editor.getCaretPosition());
  }

  protected void doResume() {
    editor.clearContent();
    jmolConsole.execute("!resume\1##");
  }
  private void gotoPosition(int i, int j) {
    editor.scrollRectToVisible(new Rectangle(i, j));
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
      parseScript(text);
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
          if (stepButton.isEnabled())
            doStep();
          return;
        case KeyEvent.VK_F8:
          if (resumeButton.isEnabled())
            doResume();
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

  @Override
  public void setFilename(String filename) {
    this.filename = filename;
  }

  @Override
  public void show(String[] fileText) {
    if (fileText == null)
      fileText = new String[] { null, null };
    if (fileText[1] == null)
      fileText[1] = "#no data#";
    String filename = fileText[0];
    String msg = fileText[1];
    if (msg != null) {
      setFilename(filename);
      output(FileManager.getEmbeddedScript(msg));
    }
    setVisible(true);
  }

  @Override
  public void windowOpened(WindowEvent e) {
    // TODO
    
  }

  @Override
  public void windowClosing(WindowEvent e) {
    // TODO
    
  }

  @Override
  public void windowClosed(WindowEvent e) {
    // TODO
    
  }

  @Override
  public void windowIconified(WindowEvent e) {
    // TODO
    
  }

  @Override
  public void windowDeiconified(WindowEvent e) {
    // TODO
    
  }

  @Override
  public void windowActivated(WindowEvent e) {
    updateFontSize();
  }

  @Override
  public void windowDeactivated(WindowEvent e) {
    // TODO
    
  }

//  @Override
//  public void dragOver(DropTargetDragEvent dtde) {
//    if (Logger.debugging)
//      Logger.debug("DropOver detected...");
//    if (processDragDrop(dtde, null))
//      dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
//  }
//
//  @Override
//  public void dragEnter(DropTargetDragEvent dtde) {
//    if (Logger.debugging)
//      Logger.debug("DropEnter detected...");
//    if (processDragDrop(dtde, null))
//      dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
//  }
//

  @Override
  public void loadContent(String script) {
    System.out.println("SCRIPT is " + script);
  }

  @Override
  public void loadFile(String fileName) {
    System.out.println(fileName);
    if (!fileName.endsWith("png") && !fileName.endsWith("pngj")
        && !fileName.endsWith("jmol") && !fileName.endsWith("zip"))
      return;
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
    } catch (Throwable e) {
      // ignore
    }
    vwr.openFileAsyncSpecial(fileName, 11);
  }

  @Override
  public void notify(int msWalltime, Object[] data) {
    if (msWalltime > 0) {
      // termination -- button legacy
      notifyScriptTermination();
    } else if (msWalltime < 0) {
      if (msWalltime == -2)
        notifyScriptStart();
    } else if (isVisible()
        && ((String) data[2]).length() > 0) {
      notifyContext(vwr.getScriptContext("SE notify"), data);
    }
  }
}
