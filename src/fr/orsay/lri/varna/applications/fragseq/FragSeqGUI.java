/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Université Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.applications.fragseq;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.applications.BasicINI;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import fr.orsay.lri.varna.models.FullBackup;


public class FragSeqGUI extends JFrame implements TreeModelListener, MouseListener,DropTargetListener, WindowListener, ComponentListener, ActionListener, TreeSelectionListener {

	private enum Commands
	{
		NEW_FOLDER,
		ADD_PANEL_UP,
		ADD_PANEL_DOWN,
		REMOVE_PANEL_UP,
		REMOVE_PANEL_DOWN,
		SORT_ID,
		SORT_FILENAME,
		REFRESH_ALL,
		CHANGE_LNF,
		TEST_XML,
	};
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -790155708306987257L;


		
	private String _INIFilename = "FragSeqUI.ini";
	private boolean redrawOnSlide = false;
	private int dividerWidth = 5;
	
	private JPanel _varnaUpperPanels = new JPanel();
	private JPanel _varnaLowerPanels = new JPanel();

	private JPanel _listPanel = new JPanel();
	private JPanel _infoPanel = new JPanel();
	private FragSeqTree _sideList = null;

	
	private FragSeqTreeModel _treeModel;
	private JToolBar _toolbar = new JToolBar();
	private JFileChooser _choice = new JFileChooser();
	
	private JScrollPane _listScroller;

	private JList _selectedElems;
	private JSplitPane _splitLeft;
	private JSplitPane _splitRight;
	private JSplitPane _splitVARNA;
	
	private JComboBox _lnf;	


	public FragSeqGUI() {
		super("VARNA Explorer");
		RNAPanelDemoInit();
	}

	
	private void RNAPanelDemoInit() 
	{
		JFrame.setDefaultLookAndFeelDecorated(true);
		this.addWindowListener(this);
	    
		_selectedElems = new JList();

		_lnf = new JComboBox(UIManager.getInstalledLookAndFeels());
		
		// Initializing Custom Tree Model
		_treeModel = new FragSeqTreeModel();
		_treeModel.addTreeModelListener(this);
		
		_sideList = new FragSeqTree(_treeModel);
		_sideList.addMouseListener(this);
		_sideList.setLargeModel(true);
		_sideList.setEditable(true);
		_sideList.addTreeWillExpandListener(_treeModel);
		FragSeqCellRenderer renderer = new FragSeqCellRenderer(_sideList,_treeModel);
		//_sideList.setUI(new CustomTreeUI());
		_sideList.setCellRenderer(renderer);
		_sideList.setCellEditor(new FragSeqCellEditor(_sideList,renderer,_treeModel));
		TreeSelectionModel m = _sideList.getSelectionModel();
		m.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		_sideList.setSelectionModel(m);
		m.addTreeSelectionListener(this);
		_sideList.setShowsRootHandles(true);
		_sideList.setDragEnabled(true);
		_sideList.setRootVisible(false);
		_sideList.setTransferHandler(new TransferHandler(null) 
		{
			public int getSourceActions(JComponent c) {
				return COPY_OR_MOVE;
			}
			protected Transferable createTransferable(JComponent c) {
				JTree tree = (JTree) c;
				TreePath tp =tree.getSelectionPath();
				if (tp!=null)
				{
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp.getLastPathComponent();
					if (node.getUserObject() instanceof FragSeqRNASecStrModel) {
						return new Transferable(){
							public DataFlavor[] getTransferDataFlavors() {
								DataFlavor[] dt = {FragSeqRNASecStrModel.Flavor};
								return dt;
							}
							public Object getTransferData(DataFlavor df)
							throws UnsupportedFlavorException, IOException {
								if (!isDataFlavorSupported(df))
									throw new UnsupportedFlavorException(df);
								DefaultMutableTreeNode node = (DefaultMutableTreeNode) _sideList.getSelectionPath().getLastPathComponent();
								return node.getUserObject();
							}
							public boolean isDataFlavorSupported(DataFlavor df) {
								return FragSeqRNASecStrModel.Flavor.equals(df);
							}
						};
					} else	if (node.getUserObject() instanceof FragSeqAnnotationDataModel) {
							return new Transferable(){
								public DataFlavor[] getTransferDataFlavors() {
									DataFlavor[] dt = { FragSeqAnnotationDataModel.Flavor};
									return dt;
								}
								public Object getTransferData(DataFlavor df)
								throws UnsupportedFlavorException, IOException {
									if (!isDataFlavorSupported(df))
										throw new UnsupportedFlavorException(df);
									DefaultMutableTreeNode node = (DefaultMutableTreeNode) _sideList.getSelectionPath().getLastPathComponent();
									return node.getUserObject();
								}
								public boolean isDataFlavorSupported(DataFlavor df) {
									return FragSeqAnnotationDataModel.Flavor.equals(df);
								}
							};
						} else {
						return null;
					}
				}
				return null;
			}
		});

		// Various buttons
		JButton refreshAllFoldersButton = new JButton("Refresh All");
		refreshAllFoldersButton.setActionCommand(""+Commands.REFRESH_ALL);
		refreshAllFoldersButton.addActionListener(this);
		
		JButton watchFolderButton = new JButton("Add folder");
		watchFolderButton.setActionCommand("" +Commands.NEW_FOLDER);
		watchFolderButton.addActionListener(this);
		
		JButton addUpperButton = new JButton("+Up");
		addUpperButton.setActionCommand(""+Commands.ADD_PANEL_UP);
		addUpperButton.addActionListener(this);
		
		JButton removeUpperButton = new JButton("-Up");
		removeUpperButton.setActionCommand(""+Commands.REMOVE_PANEL_UP);
		removeUpperButton.addActionListener(this);
		
		JButton addLowerButton = new JButton("+Down");
		addLowerButton.setActionCommand(""+Commands.ADD_PANEL_DOWN);
		addLowerButton.addActionListener(this);
		
		JButton removeLowerButton = new JButton("-Down");
		removeLowerButton.setActionCommand(""+Commands.REMOVE_PANEL_DOWN);
		removeLowerButton.addActionListener(this);
		
		JButton changeLNFButton = new JButton("Change");
		changeLNFButton.setActionCommand(""+Commands.CHANGE_LNF);
		changeLNFButton.addActionListener(this);

		JButton XMLButton = new JButton("Test XML");
		XMLButton.setActionCommand(""+Commands.TEST_XML);
		XMLButton.addActionListener(this);

		_toolbar.setFloatable(false);
		_toolbar.add(refreshAllFoldersButton);
		_toolbar.addSeparator();
		_toolbar.add(addUpperButton);
		_toolbar.add(removeUpperButton);
		_toolbar.add(addLowerButton);
		_toolbar.add(removeLowerButton);
		_toolbar.addSeparator();
		_toolbar.add(XMLButton);
		_toolbar.addSeparator();
		_toolbar.add(_lnf);
		_toolbar.add(changeLNFButton);
		
		// Scroller for File tree
	    _listScroller = new JScrollPane(_sideList,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	    _listScroller.setPreferredSize(new Dimension(300, 200));
	    _listScroller.addComponentListener(this);

		
		_listPanel.setLayout(new BorderLayout());
		
		_listPanel.add(_listScroller,BorderLayout.CENTER);
		_listPanel.add(_selectedElems,BorderLayout.SOUTH);
		_listPanel.setBorder(BorderFactory.createTitledBorder("Structures"));
		_listPanel.setPreferredSize(new Dimension(300, 0));
		
		_varnaUpperPanels.setLayout(new GridLayout());
		_varnaUpperPanels.setPreferredSize(new Dimension(800, 600));
		
		_varnaLowerPanels.setLayout(new GridLayout());
		_varnaLowerPanels.setPreferredSize(new Dimension(800, 000));
		
	    JRadioButton sortFileName = new JRadioButton("Directory");
	    sortFileName.setActionCommand("sortfilename");
	    sortFileName.setSelected(true);
	    sortFileName.setOpaque(false);
	    sortFileName.setActionCommand(""+Commands.SORT_FILENAME);
	    sortFileName.addActionListener(this);
	    JRadioButton sortID = new JRadioButton("ID");
	    sortID.setActionCommand("sortid");
	    sortID.setOpaque(false);
	    sortID.setActionCommand(""+Commands.SORT_ID);
	    sortID.addActionListener(this);

	    ButtonGroup group = new ButtonGroup();
	    group.add(sortFileName);
	    group.add(sortID);
		
		JToolBar listTools = new JToolBar();
		listTools.setFloatable(false);
		listTools.add(watchFolderButton);
		listTools.addSeparator();
	    listTools.add(new JLabel("Sort by"));
		listTools.add(sortFileName);
		listTools.add(sortID);

		JPanel sidePanel = new JPanel();
		sidePanel.setLayout(new BorderLayout());
		sidePanel.add(listTools,BorderLayout.NORTH);
		sidePanel.add(_listPanel,BorderLayout.CENTER);
		
		
		JPanel mainVARNAPanel = new JPanel();
		mainVARNAPanel.setLayout(new BorderLayout());
		_splitVARNA = new JSplitPane(JSplitPane.VERTICAL_SPLIT,redrawOnSlide,_varnaUpperPanels,_varnaLowerPanels);
		_splitVARNA.setDividerSize(dividerWidth);
		_splitVARNA.setResizeWeight(1.0);
		_splitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,redrawOnSlide,sidePanel,_splitVARNA);
		_splitLeft.setResizeWeight(0.1);
		_splitLeft.setDividerSize(dividerWidth);
		_splitRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,redrawOnSlide,_splitLeft,_infoPanel);
		_splitRight.setResizeWeight(0.85);
		_splitRight.setDividerSize(dividerWidth);
		
		_infoPanel.setLayout(new GridLayout(0,1));
		
		this.restoreConfig();		
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(_splitRight, BorderLayout.CENTER);
		this.getContentPane().add(_toolbar, BorderLayout.NORTH);
		addUpperPanel();
		addUpperPanel();
		this.setVisible(true);
	}
	
	public FragSeqGUI getSelf()
	{
		return this;
	}
	
	public VARNAHolder createIntegratedPanel(int height)
	{
		VARNAHolder vh = new VARNAHolder(this);
		_varnaPanels.add(vh);
		return vh;
	}
	
	
	public void removeUpperPanel()
	{
		if (_varnaUpperPanels.getComponentCount()>1)
		{
			VARNAHolder vh = (VARNAHolder) _varnaUpperPanels.getComponent(_varnaUpperPanels.getComponentCount()-1);
			_infoPanel.remove(vh.getInfoPane());
			_varnaUpperPanels.remove(vh);
			_splitLeft.validate();
			_splitRight.validate();
		}
	}

	public void addUpperPanel()
	{
		VARNAHolder vh = createIntegratedPanel(100);
		_varnaUpperPanels.add(vh);
		_infoPanel.add(vh.getInfoPane());
		_splitRight.validate();
		_splitLeft.validate();
	}

	
	public void removeLowerPanel()
	{
		if (_varnaLowerPanels.getComponentCount()>0)
		{
			_varnaLowerPanels.remove(_varnaLowerPanels.getComponentCount()-1);
			if (_varnaLowerPanels.getComponentCount()==0)
			{
				_splitVARNA.setDividerLocation(1.0);
				_splitVARNA.validate();
				_splitVARNA.repaint();
			}
			_splitLeft.validate();
		}
	}

	public void addLowerPanel()
	{
		if (_varnaLowerPanels.getComponentCount()==0)
		{
			_splitVARNA.setDividerLocation(0.7);
			_splitVARNA.validate();
		}
		_varnaLowerPanels.add(createIntegratedPanel(400));
		_splitLeft.validate();
	}


	public void treeNodesChanged(TreeModelEvent e) {
	       DefaultMutableTreeNode node;
	        node = (DefaultMutableTreeNode)
	                 (e.getTreePath().getLastPathComponent());

	        /*
	         * If the event lists children, then the changed
	         * node is the child of the node we have already
	         * gotten.  Otherwise, the changed node and the
	         * specified node are the same.
	         */
	        try {
	            int index = e.getChildIndices()[0];
	            node = (DefaultMutableTreeNode)
	                   (node.getChildAt(index));
	        } catch (NullPointerException exc) {}

	}
	
	


	public void addFolder(String path) {
		addFolder( path, true); 
	}

    public void addFolder(String path,
            boolean shouldBeVisible) 
    {
    	DefaultMutableTreeNode childNode = _treeModel.addFolder(path);
    	
		if ((childNode!=null) && shouldBeVisible ) {
			  System.out.println("  Expanding: "+childNode.getUserObject());
			  TreePath tp = new TreePath(childNode.getPath());
			_sideList.scrollPathToVisible(tp);			
			_sideList.expandRow(_sideList.getRowForPath(tp));
			_sideList.updateUI();
			_sideList.validate();
		}
    }

	public void treeNodesInserted(TreeModelEvent e) {
		System.out.println(e);
		
	}

	public void treeNodesRemoved(TreeModelEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void treeStructureChanged(TreeModelEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent e) {
		
	}

	int index = 0;
	
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource() == this._sideList)
		{
			if (e.getClickCount() == 1)
			{
				/*TreePath t = _sideList.getSelectionPath();
				if (t!=null)
				{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) t.getLastPathComponent();
				if (node.getUserObject() instanceof FragSeqFileModel)
				{
					int row = _sideList.getRowForPath(t);
					System.out.println("[A]"+row);
					if (!_sideList.isExpanded(row))
					{
						try {
							_sideList.fireTreeWillExpand(t);
							_sideList.expandPath(t);
						} catch (ExpandVetoException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					else 
					{
						try {
							_sideList.fireTreeWillCollapse(t);
							_sideList.collapsePath(t);
						} catch (ExpandVetoException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				}*/
			}
			else if (e.getClickCount() == 2)
			{
				TreePath t = _sideList.getSelectionPath();
				if (t!= null)
				{
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) t.getLastPathComponent();
					if (node.getUserObject() instanceof FragSeqFileModel)
					{
						if (!_sideList.isExpanded(t))
						{
							try {
								_sideList.fireTreeWillExpand(t);
								_sideList.expandPath(t);
							} catch (ExpandVetoException e1) {
								e1.printStackTrace();
							}
						}
						else 
						{
							try {
								_sideList.fireTreeWillCollapse(t);
								_sideList.collapsePath(t);
							} catch (ExpandVetoException e1) {
								e1.printStackTrace();
							}
						}
					}
					else if (node.getUserObject() instanceof FragSeqModel)
					{
						FragSeqModel model = (FragSeqModel) node.getUserObject();
						
						// Figuring out which panel to add object to...
						int res;
						if (model instanceof FragSeqRNASecStrModel)
						{
							res = index % (_varnaUpperPanels.getComponentCount()+_varnaLowerPanels.getComponentCount());
						}
						else 
						{
							res = (index+_varnaUpperPanels.getComponentCount()+_varnaLowerPanels.getComponentCount()-1) % (_varnaUpperPanels.getComponentCount()+_varnaLowerPanels.getComponentCount());
						}
						Component c = null;
						if (res<_varnaUpperPanels.getComponentCount())
						{
							c = (VARNAHolder)_varnaUpperPanels.getComponent(res);
						}
						else
						{
							res -= _varnaUpperPanels.getComponentCount();
							c = (VARNAHolder)_varnaLowerPanels.getComponent(res);
						}
						
						if (c instanceof VARNAHolder)
						{
							VARNAHolder h = (VARNAHolder) c;
							if (model instanceof FragSeqRNASecStrModel)
							{
								h.setSecStrModel((FragSeqRNASecStrModel)model);
								index ++;
							}
							else if (model instanceof FragSeqAnnotationDataModel)
							{
								h.setDataModel((FragSeqAnnotationDataModel)model);
							}
						}
					}
				}
			}
		}
	}

	
	
	
	class VARNAHolder extends JPanel
	{
		VARNAPanel vp;
		FragSeqRNASecStrModel _m;
		FragSeqAnnotationDataModel _data;
		JPanel _infoPanel;
		JTextPane _infoTxt;
		
		
		public VARNAHolder(DropTargetListener f)
		{
			super();
			vp = new VARNAPanel();
			vp.addFocusListener(new FocusListener(){
				public void focusGained(FocusEvent e) {
					//focus(_m);
				}
				public void focusLost(FocusEvent e) {
				}});
			vp.setPreferredSize(new Dimension(800, 400));

			_infoTxt = new JTextPane(); 
			_infoTxt.setPreferredSize(new Dimension(200,0));
			_infoTxt.setContentType("text/html");

			JScrollPane scroll = new JScrollPane(_infoTxt,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS); 

			_infoPanel = new JPanel();
			_infoPanel.setLayout(new BorderLayout());
			_infoPanel.setPreferredSize(new Dimension(200,0));
			_infoPanel.setBorder(BorderFactory.createTitledBorder("Info"));
			_infoPanel.add(scroll,BorderLayout.CENTER);
			_infoPanel.validate();

			this.setLayout(new BorderLayout());
			this.setPreferredSize(new Dimension(300,600));
			this.setBorder(BorderFactory.createTitledBorder("None"));			
			this.add(vp, BorderLayout.CENTER);
			
			DropTarget dt = new DropTarget(vp, f);
		}
		
		VARNAPanel getVARNAPanel()
		{
			return vp;
		}
		void setSecStrModel(FragSeqRNASecStrModel m)
		{
			_m = m;
			vp.showRNAInterpolated(m.getRNA());
			setBorder(BorderFactory.createTitledBorder(m.toString()));
			_infoTxt.setText(m.getRNA().getHTMLDescription());
			_infoPanel.setBorder(BorderFactory.createTitledBorder("Info ("+_m+")"));
			vp.requestFocus();
		}
		void setDataModel(FragSeqAnnotationDataModel data)
		{
			_data = data;
			data.applyTo(vp.getRNA());
			vp.repaint();
			vp.requestFocus();
		}
		FragSeqModel getModel()
		{
			setBorder(BorderFactory.createTitledBorder(_m.toString()));
			return _m;
		}
		public void setInfoTxt(String s)
		{
			_infoTxt.setText(vp.getRNA().getHTMLDescription());
			_infoTxt.validate();
		}
		public JPanel getInfoPane()
		{
			return _infoPanel;
		}
		
	}
	

	private ArrayList<VARNAHolder> _varnaPanels = new ArrayList<VARNAHolder>(); 
	
	private VARNAHolder getHolder(Component vp)
	{
		if (vp instanceof VARNAHolder)
		{
			int i= _varnaPanels.indexOf(vp);
			if (i!=-1)
			{
				return _varnaPanels.get(i);
			}
		}
		if (vp instanceof VARNAPanel)
		{
			for (VARNAHolder vh: _varnaPanels)
			{
				if (vh.getVARNAPanel()==vp)
				return vh;
			}
		}
		return null;
	}


	
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void dragEnter(DropTargetDragEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void dragExit(DropTargetEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void dragOver(DropTargetDragEvent arg0) {
		
	}

	public void drop(DropTargetDropEvent arg0) {
			try {
				
				DropTarget o = (DropTarget)arg0.getSource();
				if (o.getComponent() instanceof VARNAPanel)
				{
					VARNAHolder h = getHolder(o.getComponent());
					if (h!=null)
					{
						System.out.println("[X]");
						Transferable t = arg0.getTransferable();
						if (t.isDataFlavorSupported(FragSeqRNASecStrModel.Flavor))
						{
							Object data = t.getTransferData(FragSeqRNASecStrModel.Flavor);
							if (data instanceof FragSeqRNASecStrModel)
							{
								h.setSecStrModel((FragSeqRNASecStrModel) data);
							}
						}
						else if (t.isDataFlavorSupported(FragSeqAnnotationDataModel.Flavor))
						{							
							System.out.println("[Y]");
							Object data = t.getTransferData(FragSeqAnnotationDataModel.Flavor);
							if (data instanceof FragSeqAnnotationDataModel)
							{
								FragSeqAnnotationDataModel d = (FragSeqAnnotationDataModel) data;
								h.setDataModel(d);
							}
						}
					}
				}
			} catch (UnsupportedFlavorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
	}

	public void dropActionChanged(DropTargetDragEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowClosing(WindowEvent e) {
		saveConfig();
		System.exit(0);
	}

	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	private void restoreConfig()
	{
		BasicINI config = BasicINI.loadINI(_INIFilename);
    	ArrayList<String> vals = config.getItemList("folders");
		System.out.print("[C]"+vals);
    	
	    for(String path:vals)
		{
	    	System.out.println("Loading folder "+path);
		  addFolder(path);
		}
	    _sideList.validate();
	    _listScroller.validate();
	}

	private void saveConfig()
	{
		BasicINI data = new BasicINI();
		int i=0;
		for (String folderPath: _treeModel.getFolders())
		{
		  data.addItem("folders", "val"+i, folderPath);
		  i++;
		}
		BasicINI.saveINI(data, _INIFilename);
	}


	public void componentResized(ComponentEvent e) {
		_sideList.validate();
	}


	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}


	public void componentShown(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}


	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}


	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		System.out.println(cmd);
		if (cmd.equals(""+Commands.NEW_FOLDER))
		{
			_choice.setDialogTitle("Watch new folder...");
			_choice.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			_choice.setAcceptAllFileFilterUsed(false);
			try {
				if (_choice.showOpenDialog(getSelf()) == JFileChooser.APPROVE_OPTION) { 
					addFolder(_choice.getSelectedFile().getCanonicalPath());
				}
				else {
					System.out.println("No Selection ");
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		else if (cmd.equals(""+Commands.ADD_PANEL_DOWN))
		{
			addLowerPanel();
		}
		else if (cmd.equals(""+Commands.ADD_PANEL_UP))
		{
			addUpperPanel();
		}
		else if (cmd.equals(""+Commands.REMOVE_PANEL_DOWN))
		{
			removeLowerPanel();
		}
		else if (cmd.equals(""+Commands.REMOVE_PANEL_UP))
		{
			removeUpperPanel();
		}
		else if (cmd.equals(""+Commands.SORT_FILENAME))
		{
			_sideList.switchToPath();
		}
		else if (cmd.equals(""+Commands.SORT_ID))
		{
			_sideList.switchToID();
		}
		else if (cmd.equals(""+Commands.TEST_XML))
		{
			String path = "temp.xml";
			VARNAHolder vh = (VARNAHolder) _varnaUpperPanels.getComponent(0);
			vh.vp.toXML(path);
			try {
				FullBackup b = vh.vp.importSession(path);
				VARNAHolder vh2 = (VARNAHolder) _varnaUpperPanels.getComponent(1);
				vh2.vp.setConfig(b.config);
				vh2.vp.showRNAInterpolated(b.rna);
				vh2.vp.repaint();
			} catch (ExceptionLoadingFailed e1) {
				e1.printStackTrace();
			}
		}
		else if (cmd.equals(""+Commands.CHANGE_LNF))
		{
			try {
				
				Object o  = _lnf.getModel().getSelectedItem();
				System.out.println(o);
				UIManager.setLookAndFeel(((LookAndFeelInfo)_lnf.getModel().getSelectedItem()).getClassName());
				SwingUtilities.updateComponentTreeUI(this);
				this.pack();
			} catch (UnsupportedLookAndFeelException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ClassNotFoundException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (InstantiationException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			} catch (IllegalAccessException e4) {
				// TODO Auto-generated catch block
				e4.printStackTrace();
			}
		}
		else
		{
			JOptionPane.showMessageDialog(this, "Command '"+cmd+"' not implemented yet.");
		}
	}


	public void valueChanged(TreeSelectionEvent e) {
		int[] t = _sideList.getSelectionRows();
		if (t==null)
		{
			System.out.print("null");
		}
		else
		{
			System.out.print("[");
			for(int i=0;i<t.length;i++)
			{
				System.out.print(t[i]+",");
			}
			System.out.println("]");
		}
	}

	
	public static void main(String[] args) {
		FragSeqGUI d = new FragSeqGUI();
		d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		d.pack();
		d.setVisible(true);
	}


}

