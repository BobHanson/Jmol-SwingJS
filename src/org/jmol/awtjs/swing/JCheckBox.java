package org.jmol.awtjs.swing;

public class JCheckBox extends AbstractButton {

	public JCheckBox() {
		super("chkJCB");
	}

  @Override
  public String toHTML() {
    String s = "<label><input type=checkbox id='" + id
        + "' class='JCheckBox' style='" + getCSSstyle(0, 0) + "' "
        + (selected ? "checked='checked' " : "")
        + "onclick='SwingController.click(this)'>" + text + "</label>";
    return s;
  }


}
