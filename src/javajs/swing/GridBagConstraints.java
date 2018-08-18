package javajs.swing;

public class GridBagConstraints {

	public static final int NONE = 0;
	public static final int CENTER = 10;
	public static final int WEST = 17;
	public static final int EAST = 13;

	public int gridx;
	public int gridy;
	public int gridwidth;
	public int gridheight;
	double weightx;
	double weighty;
	public int anchor;
	public int fill;
	Insets insets;
	public int ipadx;
	public int ipady;
	
	public GridBagConstraints(int gridx, int gridy, int gridwidth,
			int gridheight, double weightx, double weighty, int anchor, int fill,
			Insets insets, int ipadx, int ipady) {
		this.gridx = gridx;
		this.gridy = gridy;
		this.gridwidth = gridwidth;
		this.gridheight = gridheight;
		this.weightx = weightx;
		this.weighty = weighty;
		this.anchor = anchor;
		this.fill = fill;
		if (insets == null)
			insets = new Insets(0, 0, 0, 0);
		this.insets = insets;
		this.ipadx = ipadx;
		this.ipady = ipady;
	}

	String getStyle(boolean margins) {
		return "style='" + (margins ? 
				"margin:" + insets.top + "px " + (ipady + insets.right) + "px "
				+ insets.bottom + "px " + (ipadx + insets.left) + "px;"
				: "text-align:" + (anchor == EAST ? "right" : anchor == WEST? "left" : "center")) + "'";
	}

}
