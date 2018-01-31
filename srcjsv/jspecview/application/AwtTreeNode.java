package jspecview.application;

import javax.swing.tree.DefaultMutableTreeNode;

import jspecview.api.JSVTreeNode;
import jspecview.common.PanelNode;

@SuppressWarnings("unchecked")
public class AwtTreeNode extends
	DefaultMutableTreeNode implements JSVTreeNode {


	  private static final long serialVersionUID = 1L;
		
	  public PanelNode panelNode;
		public int index;

	  public AwtTreeNode(String text, PanelNode panelNode) {
	    super(text);
	    this.panelNode = panelNode;
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


}
