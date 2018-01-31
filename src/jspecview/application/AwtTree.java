package jspecview.application;



import javajs.util.Lst;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;


import jspecview.api.JSVPanel;
import jspecview.api.JSVTree;
import jspecview.api.JSVTreeNode;
import jspecview.api.JSVTreePath;
import jspecview.api.ScriptInterface;
import jspecview.common.JSVFileManager;
import jspecview.common.PanelNode;
import jspecview.common.JSViewer;
import jspecview.source.JDXSource;

public class AwtTree extends JTree implements JSVTree {

  private static final long serialVersionUID = 1L;
	protected ScriptInterface si;
  private JSVTreeNode rootNode;
  private DefaultTreeModel spectraTreeModel;
	protected JSViewer vwr;

	@Override
	public JSVTreeNode getRootNode() {
		return rootNode;
	}

	public AwtTree(JSViewer viewer) {
		super();
		final JSViewer v = this.vwr = viewer;
    rootNode = new AwtTreeNode("Spectra", null);
    spectraTreeModel = new DefaultTreeModel((TreeNode) rootNode);
    setModel(spectraTreeModel);
    getSelectionModel().setSelectionMode(
        TreeSelectionModel.SINGLE_TREE_SELECTION);
    addTreeSelectionListener(new TreeSelectionListener() {
      @Override
			public void valueChanged(TreeSelectionEvent e) {
      	v.selectedTreeNode((JSVTreeNode) getLastSelectedPathComponent());
      }
    });
    setRootVisible(false);

	}
	
	@Override
	public void setSelectedPanel(ScriptInterface si, JSVPanel jsvp) {
		if (jsvp != null) {
			JSVTreeNode treeNode = PanelNode.findNode(jsvp, vwr.panelNodes).treeNode;
			scrollPathToVisible((TreePath) vwr.spectraTree.newTreePath(treeNode.getPath()));
			setSelectionPath((TreePath) vwr.spectraTree.newTreePath(treeNode.getPath()));
		}
	}
	
	@Override
	public JSVTreeNode createTree(int fileCount,
			JDXSource source, JSVPanel[] panels) {
  	AwtTree tree = (AwtTree) vwr.spectraTree;
		JSVTreeNode rootNode = tree.getRootNode();
    Lst<PanelNode> panelNodes = vwr.panelNodes;

    String fileName = JSVFileManager.getTagName(source.getFilePath());
    PanelNode panelNode = new PanelNode(null, fileName, source, null);
    JSVTreeNode fileNode = new AwtTreeNode(fileName, panelNode);
    panelNode.setTreeNode(fileNode);
		tree.spectraTreeModel.insertNodeInto((MutableTreeNode) fileNode, (MutableTreeNode) rootNode, rootNode
        .getChildCount());
		tree.scrollPathToVisible(new TreePath(fileNode.getPath()));

    for (int i = 0; i < panels.length; i++) {
      JSVPanel jsvp = panels[i];
      String id = fileCount + "." + (i + 1);
      panelNode = new PanelNode(id, fileName, source, jsvp);
      JSVTreeNode treeNode = new AwtTreeNode(panelNode.toString(), panelNode);
      panelNode.setTreeNode(treeNode);
			panelNodes.addLast(panelNode);
      tree.spectraTreeModel.insertNodeInto((MutableTreeNode) treeNode, (MutableTreeNode) fileNode, fileNode
          .getChildCount());
      tree.scrollPathToVisible(new TreePath(treeNode.getPath()));
    }
    vwr.selectFrameNode(panels[0]);
    return fileNode;
	}

	@Override
	public void setPath(JSVTreePath path) {
		setSelectionPath((TreePath) path);
	}

	@Override
	public JSVTreePath newTreePath(Object[] path) {
		return new AwtTreePath(path);
	}

	@Override
	public void deleteNodes(Lst<JSVTreeNode> toDelete) {
	  for (int i = 0; i < toDelete.size(); i++) {
	  	spectraTreeModel.removeNodeFromParent((MutableTreeNode) toDelete.get(i));
	  }
	
	}

}
