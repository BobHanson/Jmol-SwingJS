package org.jmol.awtjs.swing;

import javajs.util.SB;

public class JComboBox<T>  extends AbstractButton {

	private String[] info;
	private int selectedIndex;

	public JComboBox(String[] info){
		super("cmbJCB");
		this.info = info;
	}

	public void setSelectedIndex(int i) {
		selectedIndex = i;
		/**
		 * @j2sNative
		 * 
		 * SwingController.setSelectedIndex(this);
		 * 
		 */
		{
		}
	}

	public int getSelectedIndex() {
		return selectedIndex;
	}

	public Object getSelectedItem() {
		return (selectedIndex < 0 ? null : info[selectedIndex]);
	}

	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("\n<select id='" + id + "' class='JComboBox' onchange='SwingController.click(this)'>\n");		
		for (int i = 0; i < info.length; i++)
			sb.append("\n<option class='JComboBox_option'" + (i == selectedIndex ? "selected":"") + ">" + info[i] + "</option>");
		sb.append("\n</select>\n");
		return sb.toString();
	}



}
