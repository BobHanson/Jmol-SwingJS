package fr.orsay.lri.varna.applications.fragseq;

import javax.swing.tree.DefaultMutableTreeNode;

public class FragSeqNode extends DefaultMutableTreeNode
{
	
	public FragSeqNode(Object o)
	{
		super(o);
	}
	
	public boolean isLeaf()
	{
		return (this.getUserObject() instanceof FragSeqModel);
	}
}