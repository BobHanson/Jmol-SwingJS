package org.jmol.awtjs.swing;

import org.jmol.awtjs.swing.Container;
import javajs.util.SB;

public class JSplitPane extends JComponent {

	public static final int HORIZONTAL_SPLIT = 1;
	boolean isH = true;
	private int split = 1;
	private Container right;
	private Container left;

	public JSplitPane(int split) {
		super("JSpP");
		this.split = split;
		isH = (split == HORIZONTAL_SPLIT);
	}
	
	public void setRightComponent(JComponent r) {
		right = new JComponentImp(null);
		right.add(r); 
	}

	public void setLeftComponent(JComponent l) {
		left = new JComponentImp(null);
		left.add(l);
	}

	@Override
	public int getSubcomponentWidth() {
		int w = this.width;
		if (w == 0) {
			int wleft = left.getSubcomponentWidth();
			int wright = right.getSubcomponentWidth();
			if (wleft > 0 && wright > 0) {
				if (isH)
					w = wleft + wright;
				else
					w = Math.max(wleft, wright);
			}
		}
		return w;
	}
	
	@Override
	public int getSubcomponentHeight() {
		int h = this.height;
		if (h == 0) {
			int hleft = left.getSubcomponentHeight();
			int hright = right.getSubcomponentHeight();
			if (hleft > 0 && hright > 0) {
				if (isH)
					h = Math.max(hleft, hright);
				else
					h = hleft + hright;
			}
		}
		return h;
	}
	
	@Override
	public String toHTML() {
		if (left == null || right == null)
			return "";
		boolean isH = (split == HORIZONTAL_SPLIT);
		if (width == 0)
		  width = getSubcomponentWidth();
		if (height == 0)
		  height = getSubcomponentHeight();
		SB sb = new SB();
		sb.append("<div id='" + id + "' class='JSplitPane' style='" + getCSSstyle(100, 100) + "'>");
		if (isH) 
			sb.append("<div id='" + id + "_left' style='width:50%;height:100%;position:absolute;top:0%;left:0%'>");
		else
			sb.append("<div id='" + id + "_top' style='width:100%;height:50%;position:absolute;top:0%;left:0%'>");
		sb.append(left.getComponents()[0].toHTML());
		if (isH) 
			sb.append("</div><div id='" + id + "_right' style='width:50%;height:100%;position:absolute;top:0%;left:50%'>");
		else
			sb.append("</div><div id='" + id + "_bottom' style='width:100%;height:50%;position:absolute;top:50%;left:0%'>");
		sb.append(right.getComponents()[0].toHTML());
		sb.append("</div></div>\n");
		return sb.toString();
	}


}
