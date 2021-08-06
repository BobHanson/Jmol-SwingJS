package jspecview.application;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import jspecview.api.JSVPanel;
import jspecview.common.PanelNode;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.common.ScriptToken;

public class AppToolBar extends JToolBar {

  private static final long serialVersionUID = 1L;
  protected MainFrame mainFrame;
  protected JSViewer vwr;

  JToggleButton gridToggleButton, coordsToggleButton, revPlotToggleButton;
  
  private JButton spectraButton, errorLogButton;
	private ImageIcon errorLogIcon, errorLogYellowIcon, errorLogRedIcon;

  public AppToolBar(MainFrame mainFrame) {
    this.mainFrame = mainFrame;
    this.vwr = mainFrame.vwr;
    jbInit();
  }

  void setSelections(JSVPanel jsvp) {
    if (jsvp != null) {   
      PanelData pd = jsvp.getPanelData();
      gridToggleButton.setSelected(pd.getBoolean(ScriptToken.GRIDON));
      coordsToggleButton.setSelected(pd.getBoolean(ScriptToken.COORDINATESON));
      revPlotToggleButton.setSelected(pd.getBoolean(ScriptToken.REVERSEPLOT));
    }
  }

  void setMenuEnables(PanelNode node) {
    if (node == null)
      return;
    setSelections(node.jsvp);
    spectraButton.setToolTipText("View Spectra");
  }   
  
  void setError(boolean isError, boolean isWarningOnly) {
    errorLogButton.setIcon(isWarningOnly ? errorLogYellowIcon
        : isError ? errorLogRedIcon : errorLogIcon);
    errorLogButton.setEnabled(isError || isWarningOnly);
  }

	private void jbInit() {

		addButton(null, "Open", "open24", "open");
		addButton(null, "Print", "print24", "print");

		addSeparator();

		addButton(gridToggleButton = new JToggleButton(), "Toggle Grid",
				"grid24", "GRIDON TOGGLE");
		addButton(coordsToggleButton = new JToggleButton(), "Toggle Coordinates",
				"coords24", "COORDINATESON TOGGLE");
		addButton(revPlotToggleButton = new JToggleButton(), "Reverse Plot",
				"reverse24", "REVERSEPLOT TOGGLE");

		addSeparator();

		addButton(null, "Previous View", "previous24", "zoom previous");
		addButton(null, "Next View", "next24", "zoom next");
		addButton(null, "Reset", "reset24", "zoom out");
		addButton(null, "Clear Views", "clear24", "zoom clear");

		addSeparator();

		addButton(spectraButton = new JButton(), "Overlay Display",
				"overlay24", "view");
		addButton(null, "Display Key for Overlaid Spectra",
				"overlayKey24", "showKey TOGGLE");

		addSeparator();

    errorLogIcon = getIcon("errorLog24");
    errorLogRedIcon = getIcon("errorLogRed24");
    errorLogYellowIcon = getIcon("errorLogYellow24");

		addButton(null, "Properties", "information24", "showProperties");
		addButton(errorLogButton = new JButton(), "Error Log", errorLogIcon,
				"SHOWERRORS");

		addSeparator();

		addButton(null, "About JSpecView", "about24", "about");
	}

	private static ImageIcon getIcon(Object name) {
		return new ImageIcon(AppToolBar.class.getResource(
				"icons/" + name + ".gif"));
	}
	
  private AbstractButton addButton(AbstractButton button, String tip,
			Object icon, final String script) {
  	if (button == null)
  		button = new JButton();
  	if (icon instanceof String)
  		icon = getIcon(icon);
    button.setBorder(null);
    button.setToolTipText(tip);
    button.setIcon((ImageIcon) icon);
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (script.equals("open"))
	        vwr.openFileFromDialog(false, false, null, null);
				else if (script.equals("about"))
	        new AboutDialog(mainFrame);
				else
					vwr.runScript(script);
			}
    });
    add(button, null);
    return button;
	}

}
