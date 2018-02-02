package org.jmol.awtjs.swing;

import javajs.util.SB;

/**
 * A simple implementation of a Swing JTextPane. 
 * Operates as its own Document; no attributes
 * 
 * @author hansonr
 *
 */
public class JEditorPane extends JComponent {

	public JEditorPane() {
		super("txtJEP");
		text = "";
	}
	
	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("<textarea type=text id='" + id + "' class='JEditorPane' style='" + getCSSstyle(98, 98) + "'>"+ text + "</textarea>");
		return sb.toString();
	}

}
