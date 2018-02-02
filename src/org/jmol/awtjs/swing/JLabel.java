package org.jmol.awtjs.swing;

import javajs.util.SB;

public class JLabel extends JComponent {

	public JLabel(String text) {
		super("lblJL");
		this.text = text;
	}

	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("<span id='" + id + "' class='JLabel' style='" + getCSSstyle(0, 0) + "'>");
		sb.append(text);
		sb.append("</span>");
		return sb.toString();
	}


}
