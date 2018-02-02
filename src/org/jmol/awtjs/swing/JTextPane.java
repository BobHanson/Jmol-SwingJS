package org.jmol.awtjs.swing;

import javajs.util.SB;

/**
 * A simple implementation of a Swing JTextPane. 
 * Operates as its own Document; no attributes
 * 
 * @author hansonr
 *
 */
public class JTextPane extends JComponent implements Document {

	public JTextPane() {
		super("txtJTP");
		text = "";
	}
	
	public Document getDocument() {
		return this;
	}

	@Override
  public void insertString(int i, String s, Object object) {
		i = Math.min(i, text.length());
		text = text.substring(0, i) + s + text.substring(i);
	}

	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("<textarea type=text id='" + id + "' class='JTextPane' style='" + getCSSstyle(98, 98) + "'>"+ text + "</textarea>");
		return sb.toString();
	}

}
