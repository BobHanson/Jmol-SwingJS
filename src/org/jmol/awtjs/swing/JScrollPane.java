package org.jmol.awtjs.swing;

import javajs.util.SB;

public class JScrollPane extends JComponent {

	public JScrollPane(JComponent component) {
		super("JScP");
		add(component);
	}

	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("\n<div id='" + id + "' class='JScrollPane' style='" + getCSSstyle(98, 98) + "overflow:auto'>\n");
		if (list != null) {
		  Component c = list.get(0);
		  sb.append(c.toHTML());
		}
		sb.append("\n</div>\n");
		return sb.toString();
	}
	
  @Override
  public void setMinimumSize(Dimension dimension) {
		// TODO Auto-generated method stub
		
	}

}
