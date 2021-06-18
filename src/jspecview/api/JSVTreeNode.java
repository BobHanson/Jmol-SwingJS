/**
 * 
 */
package jspecview.api;

import java.util.Enumeration;

import javax.swing.tree.TreeNode;

import jspecview.common.PanelNode;

public interface JSVTreeNode extends TreeNode {

	int getChildCount();

	Object[] getPath();

	boolean isLeaf();

	PanelNode getPanelNode();

	Enumeration<TreeNode> children();

	int getIndex();

	void setIndex(int index);

}