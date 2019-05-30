package org.jmol.awtjs.swing;

import org.jmol.awtjs.swing.Color;
import org.jmol.awtjs.swing.Container;
import javajs.util.SB;


/**
 * There is really no need here for awt.Dialog.
 * We would not use FileDialog in an HTML5 context anyway.
 * 
 */
public class JDialog extends Container {

  private static final int headerHeight = 25;
  private int defaultWidth = 600;
  protected int defaultHeight = 300;
  
  private JContentPane contentPane;
  private String title;
  private String html;
  private int zIndex = 9000;
  
  public void setZIndex(int zIndex) {
    this.zIndex = zIndex;
  }
  
  int[] loc;

  public JDialog() {
    super("JD");
    add(contentPane = new JContentPane());
    setBackground(Color.get3(210, 210, 240));
    contentPane.setBackground(Color.get3(230, 230, 230));
  }
  
  public void setLocation(int[] loc) {
    this.loc = loc;
  }
  
  public JContentPane getContentPane() {
    return contentPane;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void pack() {
    html = null;
  }

  public void validate() {
    html = null;
  }

  @Override
  public void setVisible(boolean tf) {
    if (tf && html == null)
      setDialog();
    super.setVisible(tf);
    if (tf)
    	toFront();
  }

  public void dispose() {
    {
      
      /**
       * @j2sNative
       * 
       * SwingController.dispose(this);
       * 
       */
      {
      }
      
    }
  }

  @Override
  public void repaint() {
    setDialog();
  }
  
  /**
   * Set it into DOM, but don't show it yet.
   * this.loc, this.manager, this.id, etc.
   * 
   */
  private void setDialog() {
    html = toHTML();
    /**
     * @j2sNative
     * 
     * SwingController.setDialog(this);
     * 
     * 
     */
    {
      System.out.println(html);
    }
  }
  
  @Override
  public String toHTML() {
    renderWidth = Math.max(width, getSubcomponentWidth());
    if (renderWidth == 0)
      renderWidth = defaultWidth;
    renderHeight = Math.max(height, contentPane.getSubcomponentHeight());
    if (renderHeight == 0)
      renderHeight = defaultHeight;
    int h = renderHeight - headerHeight;
    SB sb = new SB();
    sb.append("\n<div id='" + id + "' class='JDialog' style='" + getCSSstyle(0, 0) + "z-index:" + zIndex + ";position:relative;top:0px;left:0px;reize:both;'>\n");
    sb.append("\n<div id='" + id + "_title' class='JDialogTitle' style='width:100%;height:25px;padding:5px 5px 5px 5px;height:"+headerHeight+"px'>"
        +"<span style='text-align:center;'>" + title + "</span><span style='position:absolute;text-align:right;right:1px;'>"
        + "<input type=button id='" + id + "_closer' onclick='SwingController.windowClosing(this)' value='x' /></span></div>\n");
    sb.append("\n<div id='" + id + "_body' class='JDialogBody' style='width:100%;height:"+h+"px;"
        +"position: relative;left:0px;top:0px'>\n");
    sb.append(contentPane.toHTML());
    sb.append("\n</div></div>\n");
    return sb.toString();
  }

	public void toFront() {
		/**
		 * @j2sNative
		 * 
		 * SwingController.setFront(this);
		 * 
		 */
		{}
	}


}
