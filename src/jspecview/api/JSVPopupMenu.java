package jspecview.api;


import javajs.api.GenericMenuInterface;
import javajs.util.Lst;
import jspecview.common.PanelNode;


public interface JSVPopupMenu extends GenericMenuInterface {

	@Override
	void jpiShow(int x, int y);

	void setSelected(String key, boolean b);

	boolean getSelected(String key);

	void setCompoundMenu(Lst<PanelNode> panelNodes,
			boolean allowCompoundMenu);

	void setEnabled(boolean allowMenu, boolean zoomEnabled);

}
