package fr.orsay.lri.varna.applications;

/*
VARNA is a Java library for quick automated drawings RNA secondary structure 
Copyright (C) 2007  Yann Ponty

This program is free software:you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.components.VARNAConsole;
import fr.orsay.lri.varna.interfaces.InterfaceParameterLoader;

/**
* An RNA 2d Panel demo applet.
* 
* Generalized to work with VARNAapp for Jmol-SwingJS by Bob Hanson
* 
* @author Yann Ponty & Darty Kévin
* 
*/

public class VARNAConsoleDemo extends JApplet implements InterfaceParameterLoader, ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -790155708306987257L;

  private VARNAapp app;
  private VARNAConsole _console;

	private JButton _go;

	public VARNAConsoleDemo() {
		super();
		app = new VARNAapp(true)
		    .setParameterSource(this);
		RNAPanelDemoInit();
	}

	private void RNAPanelDemoInit() {
		app.setupOneRNA();
		VARNAPanel vp = app.getVARNAPanel();
    _console = new VARNAConsole(vp); 
    setBackground(app._backgroundColor);

    _go = new JButton("Go");
    _go.addActionListener(this);
    JPanel goPanel = new JPanel();
    goPanel.setLayout(new BorderLayout());
    goPanel.add(_go, BorderLayout.CENTER);
    
    app.setPanels(goPanel, null, null);
    
    getContentPane().setLayout(new BorderLayout());
		getContentPane().add(vp, BorderLayout.CENTER);
		getContentPane().add(app._tools, BorderLayout.SOUTH);
		vp.getVARNAUI().UIRadiate();
		setPreferredSize(new Dimension(400,400));
		setVisible(true);
		_console.setVisible(true);	
	}

	@Override
  public String[][] getParameterInfo() {
	  return app.getParameterInfo();
	}

	@Override
  public void init() {
	  // from applet loader
		app.retrieveAppletParametersValues();
	}

  @Override
  public String getParameterValue(String key, String def) {
    String tmp = getParameter(key);
    return (tmp == null ? def : tmp);
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    app.updateVP();
  }
  
    public VARNAPanel getVarnaPanel() {
      return app.getVARNAPanel();
	  }

	  public JTextField getStruct() {
	    return app.getStruct();
	  }

	 	public JTextField getSeq() {
	 	  return app.getSeq();
	  }

	  public JLabel getInfo() {
	    return app.getInfo();
	  }

}
