package fr.orsay.lri.varna.applications;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.components.ZoomPanel;
import fr.orsay.lri.varna.exceptions.ExceptionExportFailed;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import fr.orsay.lri.varna.exceptions.ExceptionParameterError;
import fr.orsay.lri.varna.exceptions.ExceptionPermissionDenied;
import fr.orsay.lri.varna.exceptions.ExceptionUnmatchedClosingParentheses;
import fr.orsay.lri.varna.exceptions.ExceptionWritingForbidden;
import fr.orsay.lri.varna.factories.RNAFactory;
import fr.orsay.lri.varna.interfaces.InterfaceParameterLoader;
import fr.orsay.lri.varna.interfaces.InterfaceVARNASelectionListener;
import fr.orsay.lri.varna.models.BaseList;
import fr.orsay.lri.varna.models.FullBackup;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.VARNAConfigLoader;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.RNA;

/**
 * A generalized VARNA application interface that includes one or more
 * VARNAPanel instances (as for VARNAEditor and VARNAGUI) as well as maintaining
 * options for a command-line or library-based interface
 */
public class VARNAapp implements InterfaceParameterLoader {

  private static final String DEFAULT_SEQUENCE = "CAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAGUCAGUGUCAGACUGCAIA";

  private static final String DEFAULT_STRUCTURE1 = "..(((((...(((((...(((((...(((((.....)))))...))))).....(((((...(((((.....)))))...))))).....)))))...)))))..";
  private static final String DEFAULT_STRUCTURE2 = "..(((((...(((((...(((((........(((((...(((((.....)))))...)))))..................))))).....)))))...)))))..";

  /**
   * A class to hold the current list model for the sidelist.
   * 
   */
  class BackupHolder {
    private DefaultListModel<FullBackup> model;
    private ArrayList<RNA> _rnas = new ArrayList<RNA>();
    JList<FullBackup> jlist;

    public BackupHolder(DefaultListModel<FullBackup> model,
        JList<FullBackup> l) {
      this.model = model;
      jlist = l;
    }

    public void add(VARNAConfig c, RNA r) {
      add(c, r, r.getName(), false);
    }

    public void add(VARNAConfig c, RNA r, boolean select) {
      add(c, r, r.getName(), select);
    }

    public void add(VARNAConfig c, RNA r, String name) {
      add(c, r, name, false);
    }

    public void add(VARNAConfig c, RNA r, String name, boolean select) {
      if (_rnas.contains(r))
        return;
      if (select) {
        jlist.removeSelectionInterval(0, model.getSize());
      }
      if (name.equals("")) {
        name = generateDefaultName();
      }
      FullBackup bck = new FullBackup(c, r, name);
      _rnas.add(r);
      model.add(model.getSize(), bck);
      jlist.doLayout();
      if (select) {
        jlist.setSelectedIndex(getJlistIndexFor(bck));
      }
    }

    private int getJlistIndexFor(FullBackup bck) {
      for (int i = model.getSize(); --i >= 0;) {
        if (model.get(i).equals(bck)) {
          return i;
        }        
      }
      return -1;
    }

    public void remove(int i) {
      _rnas.remove(i);
      model.remove(i);

    }

    public DefaultListModel<FullBackup> getModel() {
      return model;
    }

    public boolean contains(RNA r) {
      return _rnas.contains(r);
    }

    /*public int getSize()
    {
      return _rnaList.getSize();
    }*/
    public FullBackup getElementAt(int i) {
      return model.getElementAt(i);
    }

    public void removeSelected() {
      int i = jlist.getSelectedIndex();
      if (i != -1) {
        if (model.getSize() == 1) {
          RNA r = new RNA();
          try {
            r.setRNA(" ", ".");
          } catch (ExceptionUnmatchedClosingParentheses e1) {
          }
          showRNA(r);
        } else {
          int newi = i + 1;
          if (newi == model.getSize()) {
            newi = model.getSize() - 2;
          }
          FullBackup bck = model.getElementAt(newi);
          jlist.setSelectedValue(bck, true);
        }
        model.remove(i);
      }

    }

    public int getIndexFor(RNA rna) {
      return _rnas.indexOf(rna);
    }

    public void removeAll() {
      _rnas.clear();
      model.clear();
    }
  }

  private ActionListener actionListener;

  public void setActionListener(ActionListener listener) {
    actionListener = listener;
  }

  private boolean doInterpolate = true;

  public VARNAapp setDoInterpolate(boolean b) {
    doInterpolate = b;
    return this;
  }

  boolean enableEditing;

  /**
   * Where to get parameter values from.
   * 
   * generally a JApplet
   */
  private InterfaceParameterLoader parameterSource;

  
  JPanel _tools = new JPanel();
  JPanel _input = new JPanel();

  Color _backgroundColor = Color.white;
  
  private VARNAPanel _vp;
  private JTextField _str = new JTextField(DEFAULT_STRUCTURE1);
  private JTextField _seq = new JTextField(DEFAULT_SEQUENCE);

  JPanel _seqPanel = new JPanel();
  JPanel _strPanel = new JPanel();
  JLabel _info = new JLabel();

  Object _hoverHighlightStr = null;
  ArrayList<Object> _selectionHighlightStr = new ArrayList<Object>();

  Object _hoverHighlightSeq = null;
  ArrayList<Object> _selectionHighlightSeq = new ArrayList<Object>();

  JLabel _strLabel = new JLabel(" Str:");
  JLabel _seqLabel = new JLabel(" Seq:");
  JButton _deleteButton, _duplicateButton;
  JPanel _listPanel;
  JList<FullBackup> _sideList;
  ZoomPanel _zoomPanel;

  protected BackupHolder _rnaList;

  static String errorOpt = "error";
  boolean _error;

  private static int _nextID = 1;

  VARNAapp(boolean enableEditing) {
    this.enableEditing = enableEditing;
  }

  public VARNAPanel getVARNAPanel() {
    return _vp;
  }

  public void setVarnaPanel(VARNAPanel vp) {
    _vp = vp;
  }

  public JTextField getStruct() {
    return _str;
  }

  public void setStructure(JTextField struct) {
    this._str = struct;
  }

  public JTextField getSeq() {
    return _seq;
  }

  public RNA selectOrAddSequenceAndStructure(String name, String sequence,
                                              String structure) {
    _seq.setText(sequence == null ? DEFAULT_SEQUENCE : sequence);
    _str.setText(structure == null ? DEFAULT_STRUCTURE1 : structure);
    RNA rna = doCreate(" Model " + name);
//    updateVP();
    return rna;
  }

  public JLabel getInfo() {
    return _info;
  }

  public void set_info(JLabel info) {
    this._info = info;
  }

  /**
   * Editor and GUI only
   * 
   * @param w
   */
  @SuppressWarnings("unchecked")
  void setSideList(MouseListener w) {
    DefaultListModel<FullBackup> dlm = new DefaultListModel<>();
    DefaultListSelectionModel m = new DefaultListSelectionModel();
    m.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    m.setLeadAnchorNotificationEnabled(false);

    _sideList = new JList<>();
    _sideList.setModel(dlm);
    _sideList.addMouseListener(w);
    _sideList.setSelectionModel(m);
    _sideList.setPreferredSize(null);
    _sideList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!_sideList.isSelectionEmpty() && !e.getValueIsAdjusting()) {          
          SwingUtilities.invokeLater(()->{
            doSelectModel();
          });
        }
      }
    });
    _rnaList = new BackupHolder(dlm, _sideList);
  }

  protected void doSelectModel() {
    FullBackup sel = _sideList.getSelectedValue();
    _vp.setConfig(sel.config);
    showRNA(sel.rna);
    _seq.setText(sel.rna.getSeq());
    _str.setText(sel.rna.getStructDBN(true));
    if (actionListener != null) {
      actionListener.actionPerformed(new ActionEvent(sel, 0, "selectModel"));
    }
  }

  public void setupNoDemo() {
    try {
      newVARNAPanel("0", ".", -1, -1);
    } catch (Exception e) {

    }
  }

  void setupTwoRNADemo() {
    RNA _RNA1 = new RNA("User defined 1");
    RNA _RNA2 = new RNA("User defined 2");
    try {
      newVARNAPanel("0", ".", -1, -1);

      // _vp.setPreferredSize(new Dimension(width, height));
      _RNA1.setRNA(DEFAULT_SEQUENCE, DEFAULT_STRUCTURE1);
      _RNA1.drawRNARadiate(_vp.getConfig());
      _RNA2.setRNA(DEFAULT_SEQUENCE, DEFAULT_STRUCTURE2);
      _RNA2.drawRNARadiate(_vp.getConfig());
    } catch (ExceptionNonEqualLength e) {
      _vp.errorDialog(e);
    } catch (ExceptionUnmatchedClosingParentheses e2) {
      e2.printStackTrace();
    }
    _rnaList.add(_vp.getConfig().clone(), _RNA1, generateDefaultName(), true);
    _rnaList.add(_vp.getConfig().clone(), _RNA2, generateDefaultName(), false);
  }

  private void newVARNAPanel(String sequence, String structure, int width,
                             int height)
      throws ExceptionNonEqualLength {
    if (_vp == null) {
      _vp = new VARNAPanel(sequence, structure);
      _vp.setBackground(_backgroundColor);
    } else {
      _vp.drawRNA(sequence, structure);
    }
    _vp.setPreferredSize(
        new Dimension(width <= 0 ? VARNAPanel.DEFAULT_WIDTH : width,
            height <= 0 ? VARNAPanel.DEFAULT_HEIGHT : height));
  }

  public VARNAPanel setupVarnaPanel(String sequence, String structure) {
    _seq.setText(sequence);
    _str.setText(structure);
    setupOneRNA();
    return _vp;
  }

  void setupOneRNA() {
    try {
      newVARNAPanel(_seq.getText(), _str.getText(), -1, -1);
    } catch (ExceptionNonEqualLength e) {
      _vp.errorDialog(e);
    }
    _vp.setBackground(_backgroundColor);
    _vp.setErrorsOn(false);
  }

  /**
   * 
   * @param name
   * @return RNA that is created
   */
  public RNA doCreate(String name) {
    try {
      RNA rna = new RNA((String) null);
      rna.setRNA(_seq.getText(), _str.getText());
      if (name != null) {
        int i = _rnaList.getIndexFor(rna);
        if (i >= 0) {
          _sideList.setSelectedIndex(i);
          return null;
        }
      }
      rna.setName(name == null ? generateDefaultName() : name);
      rna.drawRNARadiate(_vp.getConfig());
      _rnaList.add(_vp.getConfig().clone(), rna, true);
      return rna;
    } catch (ExceptionUnmatchedClosingParentheses e1) {
      JOptionPane.showMessageDialog(_vp, e1.getMessage(), "Error",
          JOptionPane.ERROR_MESSAGE);
      return null;
    }
  }

  void setPanels(JPanel goPanel, JPanel opsPanel, String listTitle) {
    Font textFieldsFont = Font.decode("MonoSpaced-PLAIN-12");
    int marginTools = 40;
    _seqLabel.setHorizontalTextPosition(SwingConstants.LEFT);
    _seqLabel.setPreferredSize(new Dimension(marginTools, 15));
    _seq.setFont(textFieldsFont);
    _seq.setText(DEFAULT_SEQUENCE);
    _seq.setEditable(enableEditing);

    _seqPanel.setLayout(new BorderLayout());
    _seqPanel.add(_seqLabel, BorderLayout.WEST);
    _seqPanel.add(_seq, BorderLayout.CENTER);

    _strLabel.setPreferredSize(new Dimension(marginTools, 15));
    _strLabel.setHorizontalTextPosition(SwingConstants.LEFT);
    _str.setFont(textFieldsFont);
    _str.setEditable(enableEditing);
    _strPanel.setLayout(new BorderLayout());
    _strPanel.add(_strLabel, BorderLayout.WEST);
    _strPanel.add(_str, BorderLayout.CENTER);

    _input.setLayout(new GridLayout(2, 0));
    _input.add(_seqPanel);
    _input.add(_strPanel);

    _tools.setLayout(new BorderLayout());
    _tools.add(_input, BorderLayout.CENTER);
    _tools.add(_info, BorderLayout.SOUTH);

    if (opsPanel != null) {

      _deleteButton = new JButton("Delete");
      _duplicateButton = new JButton("Duplicate");

      _deleteButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          doDelete();
        }
      });
      _duplicateButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          doDuplicate();
        }
      });

      JPanel ops = new JPanel();
      ops.setLayout(new GridLayout(1, 2));
      ops.add(_deleteButton);
      ops.add(_duplicateButton);
      opsPanel.add(ops, BorderLayout.NORTH);
      if (_zoomPanel != null)
        opsPanel.add(_zoomPanel, BorderLayout.SOUTH);

    }
    if (goPanel != null)
      _tools.add(goPanel, BorderLayout.EAST);

    JLabel titleLabel = new JLabel(listTitle, SwingConstants.CENTER);
    _listPanel = new JPanel();
    _listPanel.setLayout(new BorderLayout());
    if (opsPanel != null)
      _listPanel.add(opsPanel, BorderLayout.SOUTH);
    _listPanel.add(titleLabel, BorderLayout.NORTH);
    JScrollPane listScroller = new JScrollPane(_sideList);
    listScroller.setPreferredSize(new Dimension(150, 0));
    _listPanel.add(listScroller, BorderLayout.CENTER);
  }

  protected void doDelete() {
    _rnaList.removeSelected();
  }

  protected void doDuplicate() {
    _rnaList.add(_vp.getConfig().clone(), _vp.getRNA().clone(),
        _vp.getRNA().getName() + "-"
            + DateFormat.getTimeInstance(DateFormat.LONG).format(new Date()),
        true);
  }

  /**
   * From console action listener
   */
  public void updateVP() {
    try {
      _vp.drawRNAInterpolated(_seq.getText(), _str.getText());
    } catch (ExceptionNonEqualLength e) {
      e.printStackTrace();
    }
    repaint();
  }

  //  protected void showRNA(RNA rna) {
  //    showRNA(rna, false);
  //  }
  //
  protected void showRNA(RNA rna) {
    if (doInterpolate)
      _vp.showRNAInterpolated(rna);
    else
      _vp.showRNA(rna);
    _vp.repaint();
    if (_zoomPanel != null)
      _zoomPanel.repaint();
  }

  void addRNA(RNA r, VARNAConfig cfg) {
    _rnaList.add(cfg, r);
  }

  void addRNA(RNA r, VARNAConfig v, String title, boolean select) {
    _rnaList.add(v, r, title, select);
  }

  void addSelectionListener() {
    _vp.addSelectionListener(new InterfaceVARNASelectionListener() {

      @Override
      public void onHoverChanged(ModeleBase oldBase, ModeleBase newBase) {
        doHoverChange(oldBase, newBase);
      }

      @Override
      public void onSelectionChanged(int sel_ID, BaseList selection,
                                     BaseList addedBases,
                                     BaseList removedBases) {
        doSelectionChanged(sel_ID, selection);
      }

    });

  }

  protected void doSelectionChanged(int selID, BaseList selection) {
    for (Object tag : _selectionHighlightSeq) {
      _seq.getHighlighter().removeHighlight(tag);
    }
    _selectionHighlightSeq.clear();
    for (Object tag : _selectionHighlightStr) {
      _str.getHighlighter().removeHighlight(tag);
    }
    _selectionHighlightStr.clear();

    int[] shifts = (enableEditing ? _vp.getRNA().getStrandShifts() : null);
    Color selectColor = _vp.getSelectColor(Color.ORANGE);
    DefaultHighlightPainter highlighter = new DefaultHighlighter.DefaultHighlightPainter(
        selectColor);

    for (ModeleBase m : selection.getBases()) {
      try {
        int i = m.getIndex();
        int i0 = m.getIndex() + (shifts == null ? 0 : shifts[i]);
        _selectionHighlightSeq
            .add(_seq.getHighlighter().addHighlight(i0, i0 + 1, highlighter));
        _selectionHighlightStr
            .add(_str.getHighlighter().addHighlight(i0, i0 + 1, highlighter));
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    }
    if (actionListener != null) {
      actionListener
          .actionPerformed(new ActionEvent(selection, selID, "selectBases"));
    }

  }

  /**
   * @param oldBase
   * @param newBase
   */
  protected void doHoverChange(ModeleBase oldBase, ModeleBase newBase) {
    if (_hoverHighlightSeq != null) {
      _seq.getHighlighter().removeHighlight(_hoverHighlightSeq);
      _hoverHighlightSeq = null;
    }
    if (_hoverHighlightStr != null) {
      _str.getHighlighter().removeHighlight(_hoverHighlightStr);
      _hoverHighlightStr = null;
    }
    if (newBase != null) {
      try {
        int i = newBase.getIndex();
        int[] shifts = _vp.getRNA().getStrandShifts();
        _hoverHighlightSeq = _seq.getHighlighter().addHighlight(i + shifts[i],
            i + shifts[i] + 1,
            new DefaultHighlighter.DefaultHighlightPainter(Color.green));
        _hoverHighlightStr = _str.getHighlighter().addHighlight(i + shifts[i],
            i + shifts[i] + 1,
            new DefaultHighlighter.DefaultHighlightPainter(Color.green));
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    }
  }

  RNA getRNA() {
    return _sideList.getSelectedValue().rna;
  }

  public String[][] getParameterInfo() {
    String[][] info = {
        // Parameter Name Kind of Value Description,
        { "sequenceDBN", "String", "A raw RNA sequence" },
        { "structureDBN", "String",
            "An RNA structure in dot bracket notation (DBN)" },
        { errorOpt, "boolean", "To show errors" }, };
    return info;
  }

  public void init() {
    // TODO

  }

  @SuppressWarnings({ "cast", "unchecked" })
  public boolean doMouseClicked(Window w, Point point) {
    int index = _sideList.locationToIndex(point);
    ListModel<FullBackup> dlm = (ListModel<FullBackup>) _sideList.getModel();
    FullBackup item = (FullBackup) dlm.getElementAt(index);
    _sideList.ensureIndexIsVisible(index);
    Object newName = JOptionPane.showInputDialog(w,
        "Specify a new name for this RNA", "Rename RNA",
        JOptionPane.QUESTION_MESSAGE, (Icon) null, null, item.toString());
    if (newName != null) {
      item.name = newName.toString();
      return true;
    }
    return false;
  }

  ////// CLI options, from VARNAcmd //////

  private Hashtable<String, String> _optsValues = new Hashtable<String, String>();
  Hashtable<String, String> _basicOptsInv = new Hashtable<String, String>();
  private String _inFile = "";
  private String _outFile = "";
  int _baseWidth = 400;
  double _scale = 1.0;
  float _quality = 0.9f;

  private String[] _basicOptions = { VARNAConfigLoader.algoOpt,
      VARNAConfigLoader.bpStyleOpt, VARNAConfigLoader.bondColorOpt,
      VARNAConfigLoader.backboneColorOpt, VARNAConfigLoader.periodNumOpt,
      VARNAConfigLoader.baseInnerColorOpt,
      VARNAConfigLoader.baseOutlineColorOpt,

  };

  public static final int SEL_COMPLETE = 105;

  public static final int SEL_CLEAR = 104;

  public static final int SEL_REMOVE = 103;

  public static final int SEL_ADD = 102;

  public static final int SEL_SET = 101;

  public String setCLIOptions(Vector<String> args, String[] err) {

    for (int j = 0; j < _basicOptions.length; j++) {
      _basicOptsInv.put(_basicOptions[j], _basicOptions[j]);
    }
    int i = 0;
    while (i < args.size()) {
      String opt = args.elementAt(i);
      if (opt.charAt(0) != '-') {
        err[0] = "Missing or unknown option \"" + opt + "\"";
        return null;
      }
      if (opt.equals("-h")) {
        return "-h";
      }
      if (opt.equals("-x")) {
        return "-x";
      }
      if (i + 1 >= args.size()) {
        err[0] = "Missing argument for option \"" + opt + "\"";
        return null;
      }
      String val = args.get(i + 1);
      if (opt.equals("-i")) {
        _inFile = val;
      } else if (opt.equals("-o")) {
        _outFile = val;
      } else if (opt.equals("-quality")) {
        _quality = Float.parseFloat(val);
      } else if (opt.equals("-resolution")) {
        _scale = Float.parseFloat(val);
      } else {
        addOption(opt, val);
      }
      i += 2;
    }
    return null;
  }

  public void addOption(String key, String value) {
    if (key.equals("-i")) {
      _inFile = value;
    } else if (key.equals("-o")) {
      _outFile = value;
    } else {
      _optsValues.put(key.substring(1), value);
    }
  }

  @Override
  public String getParameterValue(String key, String def) {
    if (parameterSource != null)
      return parameterSource.getParameterValue(key, def);
    if (_optsValues.containsKey(key)) {
      return _optsValues.get(key);
    }
    return def;
  }

  /**
   * Process CLI parameters. May exit or open a GUI or just return
   * 
   * @return "exit0" (output handled), "exit1" (output not handled), an error
   *         message, or null (no exit)
   */
  public String processCLI() {
    try {
      VARNAConfigLoader varnaCFG = new VARNAConfigLoader(this);
      ArrayList<FullBackup> confs = new ArrayList<FullBackup>();
      if (_inFile.equals("")) {
        RNA r = new RNA();
        r.setRNA(getParameterValue("sequenceDBN", ""),
            getParameterValue("structureDBN", ""));
        confs.add(new FullBackup(r, "From Params"));
      } else if (_inFile.toLowerCase().endsWith(".varna")) {
        confs.add(VARNAPanel.importSession(_inFile));
      } else {
        Collection<RNA> rnas = RNAFactory.loadSecStr(_inFile);
        if (rnas.isEmpty()) {
          FullBackup f = null;
          try {
            f = VARNAPanel.importSession(new FileInputStream(_inFile), _inFile);
            confs.add(f);
          } catch (Exception e) {
            e.printStackTrace();
          }
          if (f == null) {
            throw new ExceptionFileFormatOrSyntax(
                "No RNA could be parsed from file '" + _inFile + "'.");
          }
        } else {
          for (RNA r : rnas) {
            confs.add(new FullBackup(r, _inFile));
          }
        }
      }
      if (_outFile.equals("")) {
        openGUI(confs, varnaCFG);
      } else {
        return writeFiles(_outFile, _quality, _scale, _baseWidth, confs,
            varnaCFG);
      }
    } catch (ExceptionWritingForbidden | ExceptionParameterError
        | ExceptionUnmatchedClosingParentheses | ExceptionExportFailed
        | ExceptionPermissionDenied | ExceptionLoadingFailed e) {
      e.printStackTrace();
      return "exit1";
    } catch (ExceptionFileFormatOrSyntax e) {
      e.setPath(_inFile);
      e.printStackTrace();
      return "exit1";
    } catch (FileNotFoundException e) {
      return "Error: Missing input file \"" + _inFile + "\".";
    }
    return (_outFile.equals("") ? null : "exit0");
  }

  private static String writeFiles(String outFile, float quality, double scale,
                                   double baseWidth,
                                   ArrayList<FullBackup> confs,
                                   VARNAConfigLoader loader)
      throws ExceptionParameterError, ExceptionWritingForbidden,
      ExceptionExportFailed {
    int index = 1;
    for (FullBackup r : confs) {
      loader.setRNA(r.rna);
      ArrayList<VARNAPanel> vpl = loader.createVARNAPanels();
      if (vpl.size() > 0) {
        VARNAPanel vp = vpl.get(0);
        if (r.hasConfig()) {
          vp.setConfig(r.config);
        }
        RNA rna = vp.getRNA();
        Rectangle2D.Double bbox = vp.getRNA().getBBox();

        if (outFile.toLowerCase().endsWith(".jpeg")
            || outFile.toLowerCase().endsWith(".jpg")
            || outFile.toLowerCase().endsWith(".png")) {
          vp.setTitleFontSize((int) (scale * vp.getTitleFont().getSize()));
          vp.setSize((int) (baseWidth * scale),
              (int) ((scale * baseWidth * bbox.height) / bbox.width));
        }

        if (outFile.toLowerCase().endsWith(".eps")) {
          rna.saveRNAEPS(formatOutputPath(outFile, index, confs.size()),
              vp.getConfig());
        } else if (outFile.toLowerCase().endsWith(".xfig")
            || outFile.toLowerCase().endsWith(".fig")) {
          rna.saveRNAXFIG(formatOutputPath(outFile, index, confs.size()),
              vp.getConfig());
        } else if (outFile.toLowerCase().endsWith(".svg")) {
          rna.saveRNASVG(formatOutputPath(outFile, index, confs.size()),
              vp.getConfig());
        } else if (outFile.toLowerCase().endsWith(".jpeg")
            || outFile.toLowerCase().endsWith(".jpg")) {
          saveToJPEG(formatOutputPath(outFile, index, confs.size()), quality,
              vp);
        } else if (outFile.toLowerCase().endsWith(".png")) {
          saveToPNG(formatOutputPath(outFile, index, confs.size()), vp);
        } else if (outFile.toLowerCase().endsWith(".varna")) {
          vp.saveSession(formatOutputPath(outFile, index, confs.size()));
        } else {
          return "Unknown extension for output file \"" + outFile + "\"";
        }
      }
      index++;
    }
    return null;
  }

  private static void openGUI(ArrayList<FullBackup> confs,
                              VARNAConfigLoader loader)
      throws ExceptionParameterError {
    VARNAGUI d = new VARNAGUI();
    d.pack();
    for (FullBackup b : confs) {
      RNA r = b.rna;
      loader.setRNA(r);
      ArrayList<VARNAPanel> vpl = loader.createVARNAPanels();
      if (vpl.size() > 0) {
        VARNAPanel vp = vpl.get(0);
        VARNAConfig cfg = vp.getConfig();
        if (b.hasConfig()) {
          cfg = b.config;
        }
        RNA rna = vp.getRNA();
        d.addRNA(rna, cfg);
      }
    }
  }

  public static void saveToJPEG(String filename, float quality, VARNAPanel vp)
      throws ExceptionExportFailed {

    BufferedImage myImage = new BufferedImage(vp.getWidth(), vp.getHeight(),
        BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = myImage.createGraphics();
    vp.paintComponent(g2);
    try {
      FileImageOutputStream out = new FileImageOutputStream(new File(filename));
      ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
      ImageWriteParam params = writer.getDefaultWriteParam();
      params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      params.setCompressionQuality(quality);
      writer.setOutput(out);
      IIOImage myIIOImage = new IIOImage(myImage, null, null);
      writer.write(null, myIIOImage, params);
      out.close();
    } catch (IOException e) {
      throw new ExceptionExportFailed(e.getMessage(), filename);
    }
  }

  public static void saveToPNG(String filename, VARNAPanel vp) {
    BufferedImage myImage = new BufferedImage(vp.getWidth(), vp.getHeight(),
        BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = myImage.createGraphics();
    vp.paintComponent(g2);
    g2.dispose();
    try {
      ImageIO.write(myImage, "PNG", new File(filename));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * formulates a path from a base path name or name.ext and an index by
   * inserting the index into the name as name-index or name-index.ext. If total
   * is less than 1, then the fileName is just returned.
   * 
   * @param fileName
   * @param index
   * @param total
   * @return indexed path
   */
  public static String formatOutputPath(String fileName, int index, int total) {
    if (total <= 1) {
      return fileName;
    }
    int indexDot = fileName.lastIndexOf('.');
    String name;
    String ext;
    if (indexDot != -1) {
      name = fileName.substring(0, indexDot);
      ext = fileName.substring(indexDot);
    } else {
      name = fileName;
      ext = "";
    }
    return name + "-" + index + ext;
  }

  public static String generateDefaultName() {
    return "User file #" + _nextID++;
  }

  public void retrieveAppletParametersValues() {
    _error = Boolean.parseBoolean(getParameterValue(errorOpt, "false"));
    _vp.setErrorsOn(_error);
    _backgroundColor = getSafeColor(
        getParameterValue("background", _backgroundColor.toString()),
        _backgroundColor);
    _vp.setBackground(_backgroundColor);
    _seq.setText(getParameterValue("sequenceDBN", ""));
    _str.setText(getParameterValue("structureDBN", ""));
    int _algoCode = 0;
    String _algo = getParameterValue("algorithm", "radiate");
    if (_algo.equals("circular"))
      _algoCode = RNA.DRAW_MODE_CIRCULAR;
    else if (_algo.equals("naview"))
      _algoCode = RNA.DRAW_MODE_NAVIEW;
    else if (_algo.equals("line"))
      _algoCode = RNA.DRAW_MODE_LINEAR;
    else
      _algoCode = RNA.DRAW_MODE_RADIATE;
    if (_seq.getText().equals("") && _str.getText().equals("")) {
      _seq.setText(DEFAULT_SEQUENCE);
      _str.setText(DEFAULT_STRUCTURE1);
    }
    try {
      _vp.drawRNA(_seq.getText(), _str.getText(), _algoCode);
    } catch (ExceptionNonEqualLength e) {
      e.printStackTrace();
    }

  }

  private Color getSafeColor(String col, Color def) {
    Color result;
    try {
      result = Color.decode(col);
    } catch (Exception e) {
      try {
        result = Color.getColor(col, def);
      } catch (Exception e2) {
        return def;
      }
    }
    return result;
  }

  public VARNAapp setParameterSource(InterfaceParameterLoader applet) {
    parameterSource = applet;
    return this;
  }

  public void repaint() {
    _vp.repaint();
  }

  public void addZoomWindow() {
    _zoomPanel = new ZoomPanel(_vp);
    _zoomPanel.setPreferredSize(new Dimension(-1, 200));
  }

  public void zap() {
    _sideList.removeAll();
    _rnaList.removeAll();
    _vp.zap();
  }

  public void setSequenceText(String seq) {
    _seq.setText(seq);
  }

  public void setStructureText(String struc) {
    _str.setText(struc);
  }

}
