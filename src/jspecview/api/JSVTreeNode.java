/**
 * 
 */
package jspecview.api;

import java.util.Enumeration;

import javax.swing.tree.TreeNode;

import jspecview.common.PanelNode;

public interface JSVTreeNode extends TreeNode {

	@Override
  int getChildCount();

	Object[] getPath();

	@Override
  boolean isLeaf();

	PanelNode getPanelNode();

	@Override
  Enumeration<JSVTreeNode> children();

	int getIndex();

	void setIndex(int index);

}