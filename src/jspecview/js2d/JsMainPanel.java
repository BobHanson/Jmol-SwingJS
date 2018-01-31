/**
 * 
 */
package jspecview.js2d;

import javajs.util.Lst;

import jspecview.api.JSVMainPanel;
import jspecview.api.JSVPanel;
import jspecview.common.JSViewer;
import jspecview.common.PanelNode;

public class JsMainPanel implements JSVMainPanel {

	private JSVPanel selectedPanel;
	private int currentPanelIndex;
	private String title;
	private boolean visible;
	private boolean focusable;
	private boolean enabled;
	@Override
	public int getCurrentPanelIndex() {
		return currentPanelIndex;
	}

	@Override
	public void dispose() {
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public void setSelectedPanel(JSViewer viewer, JSVPanel jsvp, Lst<PanelNode> panelNodes) {
		if (jsvp != selectedPanel)
			selectedPanel = jsvp;
    int i = viewer.selectPanel(jsvp, panelNodes);
    if (i >= 0)
      currentPanelIndex = i;    
		visible = true;
	}

	@Override
	public int getHeight() {
		return (selectedPanel == null ? 0 : selectedPanel.getHeight());
	}

	@Override
	public int getWidth() {
		return (selectedPanel == null ? 0 : selectedPanel.getWidth());
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean isFocusable() {
		return focusable;
	}

	@Override
	public boolean isVisible() {
		return visible;
	}

	@Override
	public void setEnabled(boolean b) {
		enabled = b;
	}

	@Override
	public void setFocusable(boolean b) {
		focusable = b;
	}

}