package org.jmol.awtjs.swing;

import javajs.util.SB;

public class JButton extends AbstractButton {

  public JButton() {
    super("btnJB");
  }
	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("<input type=button id='" + id + "' class='JButton' style='" + getCSSstyle(80, 0) + "' onclick='SwingController.click(this)' value='"+ text + "'/>");
		return sb.toString();
	}
}
