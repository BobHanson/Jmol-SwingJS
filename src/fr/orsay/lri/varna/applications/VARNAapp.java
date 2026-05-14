package fr.orsay.lri.varna.applications;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;

import fr.orsay.lri.varna.components.VARNAPanel;
import fr.orsay.lri.varna.components.ZoomPanel;
import fr.orsay.lri.varna.exceptions.ExceptionExportFailed;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import fr.orsay.lri.varna.exceptions.ExceptionParameterError;
import fr.orsay.lri.varna.exceptions.ExceptionUnmatchedClosingParentheses;
import fr.orsay.lri.varna.exceptions.ExceptionWritingForbidden;
import fr.orsay.lri.varna.factories.RNAFactory;
import fr.orsay.lri.varna.interfaces.InterfaceParameterLoader;
import fr.orsay.lri.varna.interfaces.InterfaceVARNASelectionListener;
import fr.orsay.lri.varna.interfaces.VARNAViewerI;
import fr.orsay.lri.varna.models.BaseSet;
import fr.orsay.lri.varna.models.FullBackup;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.VARNAConfigLoader;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.RNA;
import fr.orsay.lri.varna.utils.VARNASessionParser;
import fr.orsay.lri.varna.utils.XMLUtils;
import javajs.api.Interface;

/**
 * A generalized VARNA application interface that includes one or more
 * VARNAPanel instances (as for VARNAEditor and VARNAGUI and VARNAApplet) as
 * well as maintaining options for a command-line or "headless" library-based
 * interface
 * 
 * There are many changes here to the original VARNA 3.9 code.
 * 
 * @author Bob Hanson 2026.04.20
 */
public class VARNAapp implements InterfaceParameterLoader {

  public static String NO_RNA_TEXT = "No RNA here";

  private static final String DEFAULT_SEQUENCE = "CAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAGUCAGUGUCAGACUGCAIA";

  private static final String DEFAULT_STRUCTURE1 = "..(((((...(((((...(((((...(((((.....)))))...))))).....(((((...(((((.....)))))...))))).....)))))...)))))..";
  private static final String DEFAULT_STRUCTURE2 = "..(((((...(((((...(((((........(((((...(((((.....)))))...)))))..................))))).....)))))...)))))..";

  // BH: These are not necessary. It is enough to just have a lighlighter for individual residues
  //  private class HoverTextHighlight extends DefaultHighlightPainter {
  //
  //    public HoverTextHighlight(Color c) {
  //      super(c);
  //    }
  //    
  //    @Override
  //    public Color getColor() {
  //      return super.getColor();
  //    }
  //    
  //
  //  }
  //
  //  private class SelectionTextHighlight extends DefaultHighlightPainter {
  //
  //    public SelectionTextHighlight() {
  //      super(null);
  //    }
  //
  //    @Override
  //    public Color getColor() {
  //      return _vp.getSelectColor(Color.ORANGE);
  //    }
  //  }

  private class RNATextHighlight extends DefaultHighlightPainter {

    private Color myColor;
    private VARNAConfig config;
    private int index;
    private RNA rna;
    private boolean enabled = true;

    public RNATextHighlight(RNA rna, VARNAConfig config) {
      super(null);
      this.rna = rna;
      this.config = config;
    }

    private void setMyColor() {
      if (hoverBaseIndex == index) {
        myColor = hoverColor;
      } else if (rna.isSelected(index)) {
        myColor = selectionColor;
      } else if (_vp.selectionColored) {
        myColor = _vp.getUnselectedColor(null);
      } else {
        myColor = rna.getBaseInnerColor(index, config);
      }
    }

    @Override
    public Color getColor() {
      return myColor;
    }

    @Override
    public Shape paintLayer(Graphics g, int p0, int p1, Shape viewBounds,
                            JTextComponent editor, View view) {
      index = rna.getCaretToIndex(p0);
      if (index < 0 || !enabled
      //|| hovering 
          || textDragging) {
        return null;
      }
      setMyColor();
      return (myColor == null ? null
          : super.paintLayer(g, p0, p1, viewBounds, editor, view));
    }

    public void setEnabled(boolean b) {
      enabled = b;
    }

  }

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

    public void colorBasesByGroupID(List<Color> colors,
                                    Map<Integer, Map<String, List<?>>> map) {
      for (RNA rna : _rnas) {
        Map<String, List<?>> modelData = map.get(rna.modelID);
        if (modelData != null) {
          List<?> GroupIDs = modelData.get(VARNAViewerI.PROPERTY_GROUPIDS);
          List<?> colorIndexes = modelData
              .get(VARNAViewerI.PROPERTY_COLOR_INDEXES);
          rna.colorResidues(GroupIDs, colorIndexes, colors);
        }
      }
    }

    public void selectBasesByGroupID(Map<Integer, Map<String, List<?>>> map) {
      for (RNA rna : _rnas) {
        Map<String, List<?>> modelData = (map == null ? null
            : map.get(rna.modelID));
        rna.selectBasesByGroupID(modelData == null ? null
            : modelData.get(VARNAViewerI.PROPERTY_GROUPIDS));
      }

    }

    public RNA getRNAForModelID(int modelID) {
      if (modelID > 0) {
        for (RNA rna : _rnas) {
          if (rna.modelID.intValue() == modelID)
            return rna;
        }
      }
      return null;
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

  boolean editable = true;

  /**
   * Where to get parameter values from.
   * 
   * generally a JApplet
   */
  private InterfaceParameterLoader parameterSource;

  public JPanel _tools = new JPanel();

  private Color _backgroundColor = Color.white;

  protected VARNAPanel _vp;
  protected final JTextField _str, _seq;

  private JLabel _info = new JLabel();

  private RNATextHighlight rnaHighlight;

  // BH removing old system of highlighting; unnecessarily complex
  //  private HoverTextHighlight hoverHighlight;
  //  private SelectionTextHighlight selectionHighlight;
  //  private Object _hoverHighlightStr;
  //  private Object _hoverHighlightSeq;
  //  private ArrayList<?> _selectionHighlightStr;
  //  private ArrayList<?> _selectionHighlightSeq;

  /**
   * turns off rna highlighting
   */
  protected boolean hovering, textDragging;

  public void setHovering(boolean b) {
    hovering = b;
    repaintText();
  }

  private JButton _deleteButton, _duplicateButton;

  protected JList<FullBackup> _sideList;

  JPanel _listPanel;
  ZoomPanel _zoomPanel;

  private BackupHolder _rnaList;

  static String errorOpt = "error";
  boolean _error;

  protected Color selectionColor;

  private JPanel input;

  private JFrame frame;

  public void setFrame(JFrame frame) {
    this.frame = frame;
    if (frame == null) {
      headless = true;
    } else {
      frame.addComponentListener(new ComponentAdapter() {

        @Override
        public void componentResized(ComponentEvent e) {
        }

      });
    }
  }

  private static int _nextID = 1;

  private MouseAdapter textMouseListener = new MouseAdapter() {

    @Override
    public void mousePressed(MouseEvent e) {
      textDragging = true;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      textDragging = false;
      repaintText();
    }

  };

  public final static Color hoverColor = XMLUtils.colorFromHTML("#A3FFA3");//("#FFFFC3");

  protected int hoverBaseIndex;

  private ButtonSlider sequenceSlider;

  private int width = -1;

  private int height = -1;

  public VARNAapp(boolean enableEditing) {
    this.editable = enableEditing;
    _seq = newJTextField("", "seq");
    _str = newJTextField("", "str");
    sequenceSlider = new ButtonSlider();
    sequenceSlider.setPreferredSize(new Dimension(-1, 10));
  }

  private JTextField newJTextField(String def, String name) {
    JTextField f = new JTextField(def);
    f.setName(name);
    DefaultCaret caret = (DefaultCaret) f.getCaret();
    caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    f.addMouseListener(textMouseListener);
    BoundedRangeModel model = f.getHorizontalVisibility();
    model.addChangeListener(e -> {
      if (!sequenceSlider.isAdjusting())
        SwingUtilities.invokeLater(() -> {
          doCheckTextOverflow(name == "seq");
        });
    });
    f.setMargin(new Insets(1, 2, 1, 2));
    return f;
  }

  protected void doCheckTextOverflow(boolean isSequence) {
    JTextField f = (isSequence ? _seq : _str);
    int offset = f.getScrollOffset();
    sequenceSlider.setText(f, offset);
    JTextField f2 = (f == _seq ? _str : _seq);
    f2.setScrollOffset(offset);

    //    FontMetrics fm = _seq.getFontMetrics(_seq.getFont());
    //    double x =fm.stringWidth("X");
    //    String text = _seq.getText();
    //    double ntext = fm.stringWidth(text) / x;
    //    double offLeft = _seq.getScrollOffset() / x;
    //    double offRight = (int)(_seq.getWidth()/x - 0.72 + offLeft);
    //    double left = (int) offLeft;
    //    double right = ntext - offRight; 
    //    int nhidden = (int) (left + right);
    //    int nshown = (int) (ntext - nhidden);
    //    System.out.println("nshown=" + nshown + " nhidden=" + nhidden 
    //        + " x=" + x + " n=" + ntext + " nt=" + text.length() + " left=" + left + " right=" + right);
    //    System.out.println("slider " + sequenceSlider.getValue() 
    //     + " " + _seq.getScrollOffset());  

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

  public JTextField getSeq() {
    return _seq;
  }

  public ButtonSlider getSequenceSlider() {
    return sequenceSlider;
  }

  public RNA selectOrAddSequenceAndStructure(String name, Integer modelID,
                                             String sequence,
                                             String structure) {
    return doCreate(sequence, structure, " Model " + name, modelID);
  }

  public JLabel getInfo() {
    return _info;
  }

  public void set_info(JLabel info) {
    this._info = info;
  }

  public void setPanels(JPanel goPanel, JPanel opsPanel, String listTitle,
                        boolean allowDeleteDuplicate) {
    Font textFieldsFont = Font.decode("MonoSpaced-PLAIN-12");
    _seq.setFont(textFieldsFont);
    _seq.setEditable(editable);
    _str.setFont(textFieldsFont);
    _str.setEditable(editable);

    // VARNAViewer
    _seq.addCaretListener(new CaretListener() {

      @Override
      public void caretUpdate(CaretEvent e) {
        // fired upon mouse release
        updateTextSelection(e);
      }

    });
    _str.addCaretListener(new CaretListener() {

      @Override
      public void caretUpdate(CaretEvent e) {
        // fired upon mouse release
        updateTextSelection(e);
      }

    });

    input = new JPanel();
    input.setMaximumSize(new Dimension(4000, 55));
    input.setPreferredSize(new Dimension(-1, 55));
    input.setLayout(new BoxLayout(input, BoxLayout.Y_AXIS));
    input.add(newInput(" Seq:", _seq));
    input.add(newInput(" Str:", _str));
    input.add(newInput(null, sequenceSlider));

    _tools.setLayout(new BorderLayout());
    _tools.setMaximumSize(new Dimension(-1, 30));
    _tools.add(input, BorderLayout.CENTER);
    _tools.add(_info, BorderLayout.SOUTH);
    if (opsPanel != null) {
      if (allowDeleteDuplicate) {
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
      }
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

  private JPanel newInput(String label, JComponent c) {
    JPanel p = new JPanel();
    p.setLayout(new BorderLayout());
    int margin = 40;
    Component l;
    if (label == null) {
      l = Box.createHorizontalStrut(margin);
      p.setPreferredSize(new Dimension(-1, 10));
      p.setMaximumSize(new Dimension(100000, 10));
    } else {
      JLabel jl = new JLabel(label);
      jl.setHorizontalTextPosition(SwingConstants.LEFT);
      l = jl;
    }
    l.setPreferredSize(new Dimension(margin, 1));
    p.add(l, BorderLayout.WEST);
    p.add(c, BorderLayout.CENTER);
    return p;
  }

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
          // no, don't do this
          //SwingUtilities.invokeLater(()->{
          doSelectModel();
          //});
        }
      }
    });
    _rnaList = new BackupHolder(dlm, _sideList);
  }

  public void setupDefault() {
    try {
      newVARNAPanel(_seq.getText(), _str.getText(), width, height);
    } catch (Exception e) {

    }
  }

  void setupTwoRNADemo() {
    // for VARNAEditoreditor
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
      _vp.setRNA(sequence, structure);

    }
    _vp.setPreferredSize(
        new Dimension(width <= 0 ? VARNAPanel.DEFAULT_WIDTH : width,
            height <= 0 ? VARNAPanel.DEFAULT_HEIGHT : height));
  }

  public void setupOneRNA() {
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
   * @param sequence
   * @param structure
   * @param name
   *        " MODEL 1.1" for example
   * @param modelID
   *        Jmol modelFileNumber
   * @return RNA that is created, or null if it is already present in the given
   *         model
   */
  public RNA doCreate(String sequence, String structure, String name,
                      Integer modelID) {
    try {
      //      setHighlighters(null);
      RNA rna = new RNA(null, modelID);
      if (sequence == null) {
        rna.setRNA(_seq.getText(), _str.getText());
      } else {
        rna.setRNA(sequence, structure);
      }

      if (sequence != null) {
        int i = _rnaList.getIndexFor(rna);
        if (i >= 0) {
          _sideList.setSelectedIndex(i);
          return null;
        }
        if (!_seq.getText().equals(sequence)) {
          _seq.setText(sequence);
          _str.setText(structure);
        }
      }
      rna.setTextIndexes();
      setHighlighters(rna);
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

  /**
   * user has dragged the mouse over text, or clicked
   * 
   * @param e
   */
  protected void updateTextSelection(CaretEvent e) {
    int[] map = _vp.getRNA().caretToIndex;
    if (map == null)
      return;
    JTextField f = (JTextField) e.getSource();
    int len = f.getText().length();
    int start = f.getSelectionStart();
    int end = f.getSelectionEnd();
    if (end == start)
      end = start + 1;
    if (start >= len) {
      // caret off to side
      return;
    }
    doTextSelection(map[start], map[--end]);
  }

  protected void doSelectModel() {
    _vp.getRNA().setTextOffset(_seq.getScrollOffset());
    FullBackup sel = _sideList.getSelectedValue();
    if (sel.name != null && frame != null)
      frame.setTitle("VARNA " + sel.name);
    _vp.setConfig(sel.config);
    installModelRNA(sel.rna);
    if (actionListener != null) {
      actionListener.actionPerformed(
          new ActionEvent(sel, 0, VARNAViewerI.ACTION_SELECT_MODEL));
    }
  }

  private void installModelRNA(RNA rna) {
    showRNA(rna);
    if (rna != _vp.getRNA()) {
      // just checking -- interpolation does not immediately set _RNA to rna
    }
    _seq.setText(rna.getSeq());
    _str.setText(rna.getStructDBN(true));
    _seq.setScrollOffset(rna.getTextOffset());
    setHighlighters(rna);
  }

  private void doTextSelection(int i0, int i1) {
    _vp.setSelection(i0, i1);
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
   * From console and applet
   * 
   * @param vp
   * @param str
   * @param seq
   */
  public void updateVP(VARNAPanel vp, String seq, String str) {
    if (vp == null)
      vp = _vp;
    try {
      vp.drawRNAInterpolated(seq == null ? _seq.getText() : seq,
          str == null ? _str.getText() : str);
    } catch (ExceptionNonEqualLength e) {
      e.printStackTrace();
    }
    vp.repaint();
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
      public void onHoverChanged(ModeleBase oldBase, ModeleBase newBase,
                                 boolean doNotify) {
        doHoverChange(oldBase, newBase, doNotify);
      }

      @Override
      public void onSelectionChanged(int selectMode, BaseSet selection,
                                     BaseSet addedBases, BaseSet removedBases) {
        setHovering(false);
        doSelectionChanged(selectMode, selection);
      }

    });

  }

  /**
   * @param oldBase
   * @param newBase
   * @param doNotify
   */
  protected void doHoverChange(ModeleBase oldBase, ModeleBase newBase,
                               boolean doNotify) {
    if (doNotify && actionListener != null) {
      actionListener.actionPerformed(new ActionEvent(
          new ModeleBase[] { newBase }, 1, VARNAViewerI.ACTION_HOVER));
    }
    setHovering(newBase != null);
    setHoverBaseIndex(newBase == null ? -1 : newBase.getIndex());
    repaintText();
  }

  private void setHoverBaseIndex(int i) {
    hoverBaseIndex = i;
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

  public boolean doMouseClicked(Window w, Point point) {
    int index = _sideList.locationToIndex(point);
    ListModel<FullBackup> dlm = _sideList.getModel();
    FullBackup item = dlm.getElementAt(index);
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

  protected Hashtable<String, String> _optsValues = new Hashtable<>();

  private String _inFile = "";
  private String _outFile = "";
  int _baseWidth = 400;
  double _scale = 1.0;
  float _quality = 0.9f;

  ArrayList<VARNAPanel> appletPanels;

  boolean headless;

  public void setHeadless() {
    headless = true;
    _vp.setHeadless();
  }

  public final static String VARNA_SESSION_EXTENSION = "varna";

  void setupDemo() {
    addOptionWithValue("-sequenceDBN", DEFAULT_SEQUENCE);
    addOptionWithValue("-structureDBN", DEFAULT_STRUCTURE1);
  }

  public String addOptionWithValue(String key, String val) {
    switch (key) {
    case "-i":
    case "-infile":
      _inFile = val;
      break;
    case "-o":
    case "-outfile":
      if (_inFile == null) {
        return "Outfile with no infile indicated";
      }
      _outFile = val;
      setHeadless();
      break;
    case "-width":
      width = Math.max(300, Integer.parseInt(val));
      break;
    case "-height":
      height = Math.max(300, Integer.parseInt(val));
      break;
    case "-quality":
      _quality = Float.parseFloat(val);
      break;
    case "-resolution":
      _scale = Float.parseFloat(val);
      break;
    default:
      _optsValues.put(key.substring(1), val);
      break;
    }
    return null;
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
        String seq = getParameterValue("sequenceDBN", "");
        String str = getParameterValue("structureDBN", "");
        _seq.setText(seq);
        _str.setText(str);
      } else if (_inFile.toLowerCase().endsWith(".varna")) {
        confs.add(VARNASessionParser.importSession(_inFile));
      } else {
        Collection<RNA> rnas = RNAFactory.loadSecStr(_inFile);
        if (rnas.isEmpty()) {
          FullBackup bck = null;
          try {
            // just in case this is a VARNA session file, but isn't xxx.varna
            bck = VARNASessionParser.importSession(_inFile);
          } catch (Exception e) {
            e.printStackTrace();
          }
          if (bck == null) {
            throw new ExceptionFileFormatOrSyntax(
                "No RNA could be parsed from file '" + _inFile + "'.");
          }
          confs.add(bck);
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
        | ExceptionExportFailed | ExceptionLoadingFailed e) {
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
    int n = confs.size();
    for (FullBackup r : confs) {
      String err = write(index++, n, loader, r, outFile, quality, scale,
          baseWidth);
      if (err != null)
        return err;
    }
    return null;
  }

  private static String write(int index, int n, VARNAConfigLoader loader,
                              FullBackup r, String outFile, float quality,
                              double scale, double baseWidth)
      throws ExceptionParameterError, ExceptionWritingForbidden,
      ExceptionExportFailed {
    loader.setRNA(r.rna);
    ArrayList<VARNAPanel> vpl = loader.createVARNAPanels();
    if (vpl.size() == 0)
      return null;
    VARNAPanel vp = vpl.get(0);
    if (r.hasConfig()) {
      vp.setConfig(r.config);
    }
    RNA rna = vp.getRNA();
    Rectangle2D.Double bbox = vp.getRNA().getBBox();
    int pt = outFile.lastIndexOf(".");
    String ext = (pt > 0 ? outFile.toLowerCase().substring(pt) : "");
    switch (ext) {
    case ".jpeg":
    case ".jpg":
    case ".png":
      vp.setTitleFontSize((int) (scale * vp.getTitleFont().getSize()));
      vp.setSize((int) (baseWidth * scale),
          (int) ((scale * baseWidth * bbox.height) / bbox.width));
      break;
    }
    switch (ext) {
    case ".eps":
      rna.saveRNAEPS(formatOutputPath(outFile, index, n), vp.getConfig());
      break;
    case ".xfig":
    case ".fig":
      rna.saveRNAXFIG(formatOutputPath(outFile, index, n), vp.getConfig());
      break;
    case ".svg":
      rna.saveRNASVG(formatOutputPath(outFile, index, n), vp.getConfig());
      break;
    case ".jpeg":
    case ".jpg":
      saveToJPEG(formatOutputPath(outFile, index, n), quality, vp);
      break;
    case ".png":
      saveToPNG(formatOutputPath(outFile, index, n), vp);
      break;
    case ".varna":
      vp.saveSession(formatOutputPath(outFile, index, n));
      break;
    default:
      return "Unknown extension for output file \"" + outFile + "\"";
    }
    return null;
  }

  private static void openGUI(ArrayList<FullBackup> confs,
                              VARNAConfigLoader loader)
      throws ExceptionParameterError {
    if (confs.size() == 0) {
      return;
    }
    VARNA d = new VARNA();
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
      _vp.setRNA(_seq.getText(), _str.getText(), _algoCode);
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
    _zoomPanel = ((ZoomPanel) Interface
        .getInterface("fr.orsay.lri.varna.components.ZoomPanel")).setVP(_vp);
  }

  public void zap() {
    if (_sideList == null)
      return;
    _sideList.removeAll();
    _rnaList.removeAll();
    _seq.setText("");
    _str.setText("");

    _vp.zap();
  }

  public void setSequenceText(String seq) {
    _seq.setText(seq);
  }

  public void setStructureText(String struc) {
    _str.setText(struc);
  }

  /**
   * from VARNAViewer
   * 
   * @param map
   */
  public void selectBasesByGroupID(Map<Integer, Map<String, List<?>>> map) {
    if (_rnaList == null)
      return;
    _rnaList.selectBasesByGroupID(map);
    _vp.setBlinkActive(true);
    repaintText();
  }

  public void colorBasesByGroupID(List<Color> colors,
                                  Map<Integer, Map<String, List<?>>> map) {
    _rnaList.colorBasesByGroupID(colors, map);
    repaintText();
  }

  public ArrayList<VARNAPanel> setAppletPanels(ArrayList<VARNAPanel> panels) {
    _vp = panels.get(0);
    _backgroundColor = _vp.getBackboneColor();
    return this.appletPanels = panels;
  }

  public ArrayList<VARNAPanel> getAppletPanels() {
    return appletPanels;
  }

  public Color getBackgroundColor() {
    return _backgroundColor;
  }

  protected void doSelectionChanged(int selMode, BaseSet selection) {
    //clearSelectionHighlights();
    selectionColor = _vp.getSelectColor(Color.ORANGE);
    //    if (getRNAHighlight() != null) {
    _vp.getRNA().setSelections(selection.getBaseList());
    //    } else {
    //      if (selectionHighlight == null) {
    //        selectionHighlight = new SelectionTextHighlight();
    //        _selectionHighlightStr = new ArrayList<?>();
    //        _selectionHighlightSeq = new ArrayList<?>();
    //      }
    //
    //      BitSet bsSelect = new BitSet();
    //      for (ModeleBase m : selection.getBaseList()) {
    //        _vp.getRNA().setSelected(m);
    //        bsSelect.set(m.getTextIndex());
    //      }
    //      for (int i = bsSelect.nextSetBit(0); i >= 0; i = bsSelect
    //          .nextSetBit(i + 1)) {
    //        int j = bsSelect.nextClearBit(i + 1);
    //        try {
    //          _selectionHighlightSeq.add(
    //              _seq.getHighlighter().addHighlight(i, j, selectionHighlight));
    //          _selectionHighlightStr.add(
    //              _str.getHighlighter().addHighlight(i, j, selectionHighlight));
    //        } catch (BadLocationException e) {
    //          e.printStackTrace();
    //        }
    //      }
    //    }
    if (actionListener != null) {
      actionListener.actionPerformed(new ActionEvent(selection, selMode,
          VARNAViewerI.ACTION_SELECT_BASES));
    }
    repaintText();
  }

  // no longer necessary  
  //  /**
  //   * selectionHighlight has been abandonded as unnecessary, since now we have
  //   * rnaHighlight working, but leaving it here just in case.
  //   */
  //  private void clearSelectionHighlights() {
  ////    if (rnaHighlight != null || _selectionHighlightSeq == null) {
  ////      //_vp.getRNA().clearSelections();
  ////      // this will take care of itself
  ////      return;
  ////    }
  ////    for (Object tag : _selectionHighlightSeq) {
  ////      _seq.getHighlighter().removeHighlight(tag);
  ////    }
  ////    _selectionHighlightSeq.clear();
  ////    for (Object tag : _selectionHighlightStr) {
  ////      _str.getHighlighter().removeHighlight(tag);
  ////    }
  ////    _selectionHighlightStr.clear();
  //  }

  public void setHighlighters(RNA rna) {
    if (rnaHighlight != null)
      rnaHighlight.setEnabled(false);
    Highlighter seqh = _seq.getHighlighter();
    Highlighter strh = _str.getHighlighter();
    seqh.removeAllHighlights();
    strh.removeAllHighlights();
    rnaHighlight = new RNATextHighlight(rna, _vp.getConfig());
    if (rna == null)
      return;
    ArrayList<ModeleBase> bases = rna.getListeBases();
    if (bases.size() < 2)
      return;
    try {
      for (int i = bases.size(); --i >= 0;) {
        ModeleBase base = bases.get(i);
        int itext = base.getTextIndex();
        seqh.addHighlight(itext, itext + 1, rnaHighlight);
        strh.addHighlight(itext, itext + 1, rnaHighlight);
      }
    } catch (BadLocationException e) {
    }
    repaintText();
  }

  public RNATextHighlight getRNAHighlight() {
    return rnaHighlight;
  }

  public void repaintText() {
    if (input != null) {
      _seq.paintImmediately(
          new Rectangle(0, 0, _seq.getWidth(), _seq.getHeight()));
      _str.paintImmediately(
          new Rectangle(0, 0, _str.getWidth(), _str.getHeight()));
    }
  }

  public void destroy() {
    frame = null;
    _vp = null;
    parameterSource = null;
  }

  public void startZoomThread() {
    if (_zoomPanel != null)
      new Thread(_zoomPanel).start();
  }

  /////////////////

  /**
   * <code>
  |----------------------textWidth----------------------------|  
  CAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAAGGGAGGUCA
  |-left--|----------------width---------------------|--right-|
         |----xThumb-----[thumbWidth]---------------|
         
         |----------------------textWidth----------------------------|  
         CAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAAGGGAGGUCA
         |----------------width---------------------|--right=hidden--|         
         [thumb]--------------range-----------------|
         
         
         |----------------------textWidth----------------------------|  
         CAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAAGGGAGGUCA
         |--left=hidden---|------------------width-------------------|         
                          |--------------range-----------------[thumb]
                          
         |----------------------textWidth----------------------------|  
         CAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAAGGGAGGUCA
         |--left--|------------------width-------------------|-right-|         
                  |----------thumbX--[thumb]-----------------|
                                       ^xPressed
  
         |----------------------textWidth----------------------------|  
         CAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAAGGGAGGUCA
         |--left---|------------------width-------------------|-right|         
                  |----------thumbX--[thumb]-----------------|
                                         ^xDragged
  
         |----------------------textWidth----------------------------|  
         CAGCACGACACUAGCAGUCAGUGUCAGACUGCAIACAGCACGACACUAGCAAGGGAGGUCA
         |--left---|------------------width-------------------|-right|         
                   |----------thumbX--[thumb]-----------------|
                                        ^xDragged
  
                          
                          
    
    hidden = textWidth - width
    hidden = left + right
    thumbWidth = width - range
    range/width = 1 - thumbWidth/width
  
    thumbWidth / width = width / textWidth
  ** thumbWidth = width * width / textWidth
  
    thumbX = left / hidden * range   
           = left / (textWidth - width) * (width - thumbWidth)   
    
    and 
    
  ** left = thumbX * (textWidth - width) / (width - thumbWidth)
    
    thumbX1  = thumbX0 + dx
    
    
   </code>
   */
  private class ButtonSlider extends JPanel {

    protected JComponent thumb;

    protected int x0;
    protected int thumbX, thumbX0;

    private JTextField text;

    private boolean adjusting;

    private int textWidth;

    ButtonSlider() {
      super(null);
      createSlider();
    }

    public void setAdjusting(boolean b) {
      adjusting = b;
    }

    public boolean isAdjusting() {
      return adjusting;
    }

    private void createSlider() {
      setBackground(Color.LIGHT_GRAY);
      setMaximumSize(new Dimension(-1, 10));

      thumb = new JPanel();
      thumb.setOpaque(true);
      thumb.setBackground(Color.GRAY);
      thumb.setBounds(0, 0, 0, 0); // x, y, width, height
      //      thumb.setMaximumSize(new Dimension(-1,10));
      add(thumb);
      MouseListener listener = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          int x = e.getX();
          if (e.getSource() == thumb) {
            setX0(thumb.getX() + x);
          } else {
            setX0(x);
          }
        }
      };
      addMouseListener(listener);
      thumb.addMouseListener(listener);

      MouseMotionListener l = new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
          int x = e.getX();
          if (e.getSource() == thumb) {
            moveThumb(thumb.getX() + x);
          } else {
            moveThumb(x);
          }
        }
      };

      addMouseMotionListener(l);
      thumb.addMouseMotionListener(l);

    }

    public void setText(JTextField text, int left) {
      if (isAdjusting())
        return;
      this.text = text;
      FontMetrics fm = text.getFontMetrics(text.getFont());
      String stext = text.getText();
      int textWidth = fm.stringWidth(stext) + 5;// adds just a bit of buffer
      this.textWidth = textWidth;
      if (textWidth == 0)
        return;
      if (left == Integer.MIN_VALUE)
        left = text.getScrollOffset();
      setThumbPosition(left, textWidth);
    }

    private void setThumbPosition(int left, int textWidth) {
      int width = getWidth();
      int thumbWidth = width * width / textWidth - 2;
      if (thumbWidth < width) {
        int range = width - thumbWidth;
        thumbX = (int) (1d * left / (textWidth - width) * range);
        thumb.setBounds(thumbX, 1, thumbWidth, 8);
        thumb.setVisible(true);
      } else {
        thumb.setVisible(false);
      }
    }

    protected void setX0(int mouseX) {
      x0 = mouseX;
      thumbX0 = thumb.getX();
    }

    protected void moveThumb(int mouseX) {
      // Keep the button centered on the mouse click

      int trackWidth = getWidth();
      int thumbWidth = thumb.getWidth();
      int range = trackWidth - thumbWidth;
      thumbX = Math.max(0, Math.min(range, thumbX0 + (mouseX - x0)));
      thumb.setLocation(thumbX, thumb.getY());
      int left = (int) (1d * thumbX * (textWidth - trackWidth)
          / (trackWidth - thumbWidth));
      setScrollOffset(text, left);
    }

    protected void setScrollOffset(JTextField t, int left) {
      setAdjusting(true);
      t.setScrollOffset(left);
      JTextField t2 = (t == _seq ? _str : _seq);
      t2.setScrollOffset(left);
      setAdjusting(false);
    }
  }

  public void setHoverFor(Integer modelID, String groupID) {
    RNA rna = _vp.getRNA();
    int index = (modelID == null || !rna.modelID.equals(modelID) ? -1
        : rna.getIndexFromGroupID(groupID));
    if (index == hoverBaseIndex)
      return;
    setHoverBaseIndex(index);
    _vp.setHoverBase(index < 0 ? null : rna.getBaseAt(index), false);
  }

  public RNA getRNAForModelID(int modelID) {
    return _rnaList.getRNAForModelID(modelID);
  }

  /**
   * Load one or more VARNA session files or one RNA-type files. This method is
   * designed to be asynchronous so
   * 
   * @param list
   * @param onError
   *        asynchronous method to deliver error message to
   */
  public void loadFileListAsync(List<File> list, Consumer<String> onError) {
    // first check for Jmol-readable structure files. 
    // only the first will be processed.
    for (int j = 0; j < list.size(); j++) {
      File f = list.get(j);
      if (checkJmolFileLoad(f))
        return;
    }
    // now asynchronously get multiple files
    nextFile(0, list, onError);
  }

  private void nextFile(int j, List<File> list, Consumer<String> onError) {
    if (j < list.size()) {
      SwingUtilities.invokeLater(() -> {
        getListFileAsync(list, j, onError);
      });
    }
  }

  private void getListFileAsync(List<File> list, int j,
                                Consumer<String> onError) {
    String err = null;
    File f = list.get(j);
    try {
      FullBackup bck = VARNASessionParser.importSession(f); // BH SwingJS File Object here
      if (bck == null) {
        ArrayList<RNA> rnas = null;
        try {
          rnas = RNAFactory.loadSecStr(f);
        } catch (ExceptionFileFormatOrSyntax e) {
          err = e.getMessage();
        }
        if (rnas == null || rnas.isEmpty()) {
          err = "No RNA could be parsed from " + f
              + (err == null ? "" : "" + err);
        } else {
          getVARNAPanel().getVARNAUI().chooseRNAsAsync(rnas, () -> {
            nextFile(j + 1, list, onError);
          }, onError);
        }
      } else {
        addRNA(bck.rna, bck.config, bck.name, true);
      }
    } catch (ExceptionLoadingFailed e3) {
      err = e3.getMessage();
    }
    if (err != null)
      onError.accept(err);
    nextFile(j + 1, list, onError);
  }

  private boolean checkJmolFileLoad(File f) {
    if (actionListener == null)
      return false;
    ActionEvent e = new ActionEvent(f, 0, VARNAViewerI.ACTION_FILE_DROPPED);
    actionListener.actionPerformed(e);
    return (e.getSource() == Boolean.TRUE);
  }

  public void getSelectPanel(JPanel selectPanel) {
    JLabel label = new JLabel(" Select: ");
    selectPanel.add(label, BorderLayout.WEST);
    JTextField select = new JTextField();
    select.setMargin(new Insets(0,2,0,2));
    selectPanel.add(select, BorderLayout.CENTER);
    JPanel p = new JPanel();
    JRadioButton bJmol = new JRadioButton("Jmol");
    JRadioButton bText = new JRadioButton("Text");
    ActionListener doSelect = (e) -> {
      doSelectText(select.getText(), bJmol.isSelected());
    };
    select.addActionListener(doSelect);
    bJmol.addActionListener(doSelect);    
    bText.addActionListener(doSelect);
    bText.setSelected(true);
    ButtonGroup g = new ButtonGroup();
    g.add(bText);
    g.add(bJmol);
    p.add(bText);
    p.add(bJmol);
    selectPanel.add(p, BorderLayout.EAST);
  }

  private void doSelectText(String text, boolean isJmol) {
    if (actionListener != null)
      actionListener.actionPerformed(new ActionEvent(text, 
          (isJmol ? VARNAViewerI.ACTION_ID_JMOL_SELECT : VARNAViewerI.ACTION_ID_TEXT_SELECT), 
          VARNAViewerI.ACTION_SELECT_TEXT));
  }

}
