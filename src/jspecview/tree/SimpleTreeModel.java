package jspecview.tree;

import jspecview.api.JSVTreeNode;

public class SimpleTreeModel {
	JSVTreeNode rootNode;

	public SimpleTreeModel(JSVTreeNode rootNode) {
		this.rootNode = rootNode;
	}

	public void insertNodeInto(JSVTreeNode fileNode, JSVTreeNode rootNode,
			int i) {
		SimpleTreeNode node = (SimpleTreeNode) rootNode;
		node.children.add(i, (SimpleTreeNode) fileNode);
		((SimpleTreeNode) fileNode).prevNode = node;
	}

	public void removeNodeFromParent(JSVTreeNode node) {
		((SimpleTreeNode) node).prevNode.children.removeObj(node);
	}

}
