package jspecview.tree;

import java.util.Enumeration;

import javax.swing.tree.TreeNode;

import jspecview.api.JSVTreeNode;

public class SimpleTreeEnumeration implements Enumeration<TreeNode> {

	SimpleTreeNode node;
	int pt;
	
	public SimpleTreeEnumeration(SimpleTreeNode jsTreeNode) {
		node = jsTreeNode;
	}

	@Override
	public boolean hasMoreElements() {
		return (pt < node.children.size());
	}

	@Override
	public TreeNode nextElement() {
		return node.children.get(pt++);
	}

}
