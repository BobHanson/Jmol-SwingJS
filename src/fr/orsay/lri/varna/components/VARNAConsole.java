package fr.orsay.lri.varna.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.controlers.ControleurScriptParser;
import fr.orsay.lri.varna.models.VARNAConfig;

public class VARNAConsole extends JFrame implements ActionListener, FocusListener, KeyListener {

  private VARNAPanel _vp;
	
  private JButton _quitButton; 
  private JPanel _contentPanel; 
  private JPanel _quitPanel; 
  private JTextField _input; 
  private JEditorPane _output; 
  private JScrollPane _scrolls; 
	
  public VARNAConsole(VARNAPanel vp)
  {
	  _vp = vp;
	  init();
  }
  
  private void init()
  {
	  _quitButton = new JButton("Exit");
	  _quitPanel = new JPanel();
	  _contentPanel = new JPanel();
	  _input = new JTextField("Your command here...");
	  _output = new JEditorPane();
	  _scrolls = new JScrollPane(_output);

	  _input.addFocusListener(this);
	  _input.addKeyListener(this);
	  
	  _output.setText(VARNAConfig.getFullName()+" console\n");
	  _output.setPreferredSize(new Dimension(500,300));
	  _output.setEditable(false);
	  
	  _quitPanel.add(_quitButton);
	  
	  _quitButton.addActionListener(this);
	  
	  _contentPanel.setLayout(new BorderLayout());
	  _contentPanel.add(_scrolls,BorderLayout.CENTER);
	  _contentPanel.add(_input,BorderLayout.SOUTH);

	  getContentPane().setLayout(new BorderLayout());
	  getContentPane().add(_contentPanel,BorderLayout.CENTER);
	  getContentPane().add(_quitPanel,BorderLayout.SOUTH);
	  
	  pack();
  }

public void actionPerformed(ActionEvent arg0) {
	setVisible(false);
}

private boolean _firstFocus = true;

public void focusGained(FocusEvent arg0) {
	if (_firstFocus)
	{
		_input.setSelectionStart(0);
		_input.setSelectionEnd(_input.getText().length());
		_firstFocus = false;
	}
}

public void focusLost(FocusEvent arg0) {
	// TODO Auto-generated method stub
	
}

public void keyPressed(KeyEvent arg0) {
	// TODO Auto-generated method stub
	
}

public void keyReleased(KeyEvent arg0) {
	// TODO Auto-generated method stub
	
}

public void keyTyped(KeyEvent arg0) {
	// TODO Auto-generated method stub
	char c = arg0.getKeyChar();
	if (c=='\n')
	{
		try {
			ControleurScriptParser.executeScript(_vp,_input.getText());
		} catch (Exception e) {
			_output.setText(_output.getText()+e.getMessage()+'\n');
			e.printStackTrace();
		}
	}
}
  
  
  
}
