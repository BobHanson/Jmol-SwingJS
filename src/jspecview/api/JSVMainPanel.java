package jspecview.api;

import javajs.util.Lst;
import jspecview.common.JSViewer;
import jspecview.common.PanelNode;


public interface JSVMainPanel extends JSVViewPanel {

	int getCurrentPanelIndex();
	void setSelectedPanel(JSViewer viewer, JSVPanel jsvp, Lst<PanelNode> panelNodes);

}
