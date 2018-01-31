package jspecview.tree;

import javajs.util.Lst;

import jspecview.api.JSVPanel;
import jspecview.api.JSVTree;
import jspecview.api.JSVTreeNode;
import jspecview.api.JSVTreePath;
import jspecview.api.ScriptInterface;
import jspecview.common.JSVFileManager;
import jspecview.common.PanelNode;
import jspecview.common.JSViewer;
import jspecview.source.JDXSource;

public class SimpleTree implements JSVTree {

	protected ScriptInterface si;
  private JSVTreeNode rootNode;
  private SimpleTreeModel spectraTreeModel;
	protected JSViewer vwr;
	private SimpleTreePath selectedPath;

	@Override
	public JSVTreeNode getRootNode() {
		return rootNode;
	}

	public SimpleTree(JSViewer viewer) {
		this.vwr = viewer;
    rootNode = new SimpleTreeNode("Spectra", null);
    spectraTreeModel = new SimpleTreeModel(rootNode);
	}
	
  public void valueChanged() {
  	vwr.selectedTreeNode(getLastSelectedPathComponent());
  }

  private JSVTreeNode getLastSelectedPathComponent() {
  	return (JSVTreeNode) (selectedPath == null ? null : selectedPath.getLastPathComponent());
	}

	@Override
	public void setSelectedPanel(ScriptInterface si, JSVPanel jsvp) {
		if (jsvp != null) {
			JSVTreeNode treeNode = PanelNode.findNode(jsvp, vwr.panelNodes).treeNode;
			setSelectionPath(vwr.spectraTree.newTreePath(treeNode.getPath()));
		}
	}
	
	private void setSelectionPath(JSVTreePath newTreePath) {
		selectedPath = (SimpleTreePath) newTreePath;
		valueChanged();
	}

	@Override
	public JSVTreeNode createTree(int fileCount, JDXSource source, JSVPanel[] panels) {
		SimpleTree tree = (SimpleTree) vwr.spectraTree;
		JSVTreeNode rootNode = tree.getRootNode();
		Lst<PanelNode> panelNodes = vwr.panelNodes;

		String fileName = JSVFileManager.getTagName(source.getFilePath());
		PanelNode panelNode = new PanelNode(null, fileName, source, null);
		JSVTreeNode fileNode = new SimpleTreeNode(fileName, panelNode);
		panelNode.setTreeNode(fileNode);
		tree.spectraTreeModel.insertNodeInto(fileNode, rootNode,
				rootNode.getChildCount());
		// tree.scrollPathToVisible(new JsTreePath(fileNode.getPath()));

		for (int i = 0; i < panels.length; i++) {
			JSVPanel jsvp = panels[i];
			String id = fileCount + "." + (i + 1);
      panelNode = new PanelNode(id, fileName, source, jsvp);
			JSVTreeNode treeNode = new SimpleTreeNode(panelNode.toString(), panelNode);
			panelNode.setTreeNode(treeNode);
			panelNodes.addLast(panelNode);
			tree.spectraTreeModel.insertNodeInto(treeNode, fileNode,
					fileNode.getChildCount());
			// tree.scrollPathToVisible(new TreePath(treeNode.getPath()));
		}
		vwr.selectFrameNode(panels[0]);
		return fileNode;
	}

	@Override
	public void setPath(JSVTreePath path) {
		setSelectionPath(path);
	}

	@Override
	public JSVTreePath newTreePath(Object[] path) {
		return new SimpleTreePath(path);
	}

	@Override
	public void deleteNodes(Lst<JSVTreeNode> toDelete) {
	  for (int i = 0; i < toDelete.size(); i++) {
	  	spectraTreeModel.removeNodeFromParent(toDelete.get(i));
	  }
	
	}

}
