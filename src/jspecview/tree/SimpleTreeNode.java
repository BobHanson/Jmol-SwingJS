package jspecview.tree;

import java.util.Enumeration;

import javajs.util.Lst;


import jspecview.api.JSVTreeNode;
import jspecview.common.PanelNode;

public class SimpleTreeNode implements JSVTreeNode {


	  public PanelNode panelNode;
		public int index;
	  SimpleTreeNode prevNode;
	  Lst<SimpleTreeNode> children;

		private String text;

	  public SimpleTreeNode(String text, PanelNode panelNode) {
	  	this.text = text;
	    this.panelNode = panelNode;
	    children = new Lst<SimpleTreeNode>();
	   // System.out.println("adding " + text);
	  }

		@Override
		public PanelNode getPanelNode() {
			return panelNode;
		}
		
		@Override
		public int getIndex() {
			return index;
		}
		
		@Override
		public void setIndex(int index) {
			this.index = index;
		}

		@Override
		public Enumeration<JSVTreeNode> children() {
			return new SimpleTreeEnumeration(this);
		}

		@Override
		public int getChildCount() {
			return children.size();
		}

		@Override
		public Object[] getPath() {
			Lst<Object> o = new Lst<Object>();
			SimpleTreeNode node = this;
			o.addLast(node);
			while ((node = node.prevNode) != null)
				o.add(0, node);
			return o.toArray();//new Object[o.size()]);
		}

		@Override
		public boolean isLeaf() {
			return (prevNode != null && prevNode.prevNode != null);
		}

		@Override
		public String toString() {
			return text;
		}

}
