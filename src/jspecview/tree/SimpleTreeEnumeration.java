package jspecview.tree;

import java.util.Enumeration;

import jspecview.api.JSVTreeNode;

public class SimpleTreeEnumeration implements Enumeration<JSVTreeNode> {

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
	public JSVTreeNode nextElement() {
		return node.children.get(pt++);
	}

}
