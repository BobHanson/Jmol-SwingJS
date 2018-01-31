/**
 * 
 */
package jspecview.java;

import java.awt.BorderLayout;


import javajs.util.Lst;

import javax.swing.JPanel;


import jspecview.api.JSVMainPanel;
import jspecview.api.JSVPanel;
import jspecview.common.JSViewer;
import jspecview.common.PanelNode;

public class AwtMainPanel extends JPanel implements JSVMainPanel {

	private static final long serialVersionUID = 1L;
	private JSVPanel selectedPanel;
	private int currentPanelIndex;
	@Override
	public int getCurrentPanelIndex() {
		return currentPanelIndex;
	}

	public AwtMainPanel(BorderLayout borderLayout) {
		super(borderLayout);
	}

	@Override
	public void dispose() {
	}

	@Override
	public String getTitle() {
		return null;
	}

	@Override
	public void setTitle(String title) {
	}

	@Override
	public void setSelectedPanel(JSViewer viewer, JSVPanel jsvp, Lst<PanelNode> panelNodes) {
		if (jsvp != selectedPanel) {
			if (selectedPanel != null)
				remove((AwtPanel) selectedPanel);
			if (jsvp != null)
				add((AwtPanel) jsvp, BorderLayout.CENTER);
			selectedPanel = jsvp;
		}
		int i = viewer.selectPanel(jsvp, panelNodes);
		if (i >= 0)
			currentPanelIndex = i;		
		setVisible(true);
	}


}