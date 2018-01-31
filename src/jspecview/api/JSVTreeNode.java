/**
 * 
 */
package jspecview.api;

import java.util.Enumeration;

import jspecview.common.PanelNode;

public interface JSVTreeNode {

	int getChildCount();

	Object[] getPath();

	boolean isLeaf();

	PanelNode getPanelNode();

	Enumeration<JSVTreeNode> children();

	int getIndex();

	void setIndex(int index);

}