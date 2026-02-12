package fr.orsay.lri.varna.views;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import fr.orsay.lri.varna.VARNAPanel;

public class VueLoadColorMapValues extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1648400107478203724L;
	VARNAPanel _vp;
	
  public VueLoadColorMapValues(VARNAPanel vp)
  {
	  _vp = vp;
	  init();
  }
  JRadioButton urlCB = new JRadioButton("URL"); 
  JRadioButton fileCB = new JRadioButton("File");
  JPanel urlAux = new JPanel();
  JPanel fileAux = new JPanel();
  CardLayout l = new CardLayout();
  JPanel input = new JPanel();
  JTextField urlTxt = new JTextField(); 
  JTextField fileTxt = new JTextField(); 
  JButton load = new JButton("Choose file");
  
  private void init()
  {
	  setLayout(new GridLayout(2,1));
	  JPanel choice = new JPanel();
	  urlCB.addActionListener(this);
	  fileCB.addActionListener(this);
	  ButtonGroup group = new ButtonGroup();
	  group.add(urlCB);
	  group.add(fileCB);
	  choice.add(new JLabel("Choose input source:"));
	  choice.add(urlCB);
	  choice.add(fileCB);
	  input.setLayout(l);
	  urlTxt.setPreferredSize(new Dimension(300,30));
	  fileTxt.setPreferredSize(new Dimension(300,30));
	  urlAux.add(urlTxt);
	  fileAux.add(fileTxt);
	  fileAux.add(load);
	  input.add(fileAux,"file");
	  input.add(urlAux,"url");
	  group.setSelected(fileCB.getModel(), true);
	  load.addActionListener(this);
	  this.add(choice);
	  this.add(input);
  }

public void actionPerformed(ActionEvent e) {
	if (e.getSource() instanceof JRadioButton)
	{
		if (urlCB.isSelected())
		{
			l.show(input, "url");
		}
		else
		{
			l.show(input, "file");
		}
	}
	else if (e.getSource() instanceof JButton)
	{
		JFileChooser fc = new JFileChooser();
		if (fc.showOpenDialog(_vp) == JFileChooser.APPROVE_OPTION)
		{
			this.fileTxt.setText(fc.getSelectedFile().getAbsolutePath());
		}
	}
}

public Reader getReader() throws IOException
{
	if (urlCB.isSelected())
	{
		URL url = new URL(urlTxt.getText());
		URLConnection connexion = url.openConnection();
		connexion.setUseCaches(false);
		InputStream r = connexion.getInputStream();
		return new InputStreamReader(r);
	}
	else
	{
	  return new FileReader(fileTxt.getText());
	}
	
}

}
