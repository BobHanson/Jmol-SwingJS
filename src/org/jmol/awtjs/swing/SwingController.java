package org.jmol.awtjs.swing;

/**
 * SwingController is an interface that org.jmol.awtjs.swing classes will need.
 * It must be implemented as a JavaScript object PRIOR to 
 * any calls to create any components.
 * 
 * In JSmol it is Jmol.Swing (see JsmolCore.js)
 * 
 * There should be one and only one SwingController on a page. 
 * It is called by its class name "SwingController" directly. 
 * 
 * @author hansonr
 * 
 */
public interface SwingController {
  
  /**
   * Fired from clicking an element such as a button or 
   * check box or table entry, or from entering text in a text box.
   * 
   * SwingController should make the changes in the underlying 
   * "Java" object directly, then send notification of the event to the manager.
   * For instance:
   * 
   *   var component = Jmol.Swing.htDialogs[element.id];
   *   var info = component.toString();
   *   
   * if (info.indexOf("JCheck") >= 0)
   *     component.selected = element.checked;
   * var id = $("div.JDialog:has(#" + element.id + ")")[0].id
   * var dialog = Jmol.Swing.htDialogs[id];
   * dialog.manager.actionPerformed(component ? component.name :  dialog.registryKey + "/" + element.id);
   * 
   * @param element
   * @param event 
   */
  void click(HTMLElement element, HTMLWindowEvent event);
  

  /**
   * Remove this component's HTML5 equivalent and clear references to it.
   * 
   * @param dialog
   */
  void dispose(Component dialog);
  
  /**
   * Return the width and height of the window in d.
   * For example:
   * 
   * d.width = $(window).width();
   * d.height = $(window).height();
   *
   * @param d
   */
  void getScreenDimensions(Dimension d);
  
  /**
   * Set c's id to a unique identifier
   * and add it to an associative array that will
   * associate that id with c.
   * 
   * @param c
   * @param type
   */
  void register(Component c, String type);
  
  /**
   * The HTML for this dialog has been generated.
   * Now create the HTML on the page for this dialog
   * based on dialog.html and wrap it appropriately.
   * 
   * @param dialog
   */
  void setDialog(Component dialog);
  
  /**
   * Convey to the HTML object that this check box's selection
   * has been changed.
   * 
   *  $("#" + chk.id).prop('checked', !!chk.selected);
   *  
   * @param chk
   */
  void setSelected(Component chk);
  
  /**
   * Convey to the HTML object that this combo box's selected item
   * has been changed.
   * 
   *  $("#" + cmb.id).prop('selectedIndex', cmb.selectedIndex);
   *  
   * @param cmb
   */
  void setSelectedIndex(Component cmb);
  
  /**
   * Convey to the HTML object that this component's text
   * has been changed.
   * 
   *  $("#" + btn.id).prop('value', btn.text);
   *  
   * @param text
   */
  void setText(String text);
  
  /**
   * Convey to the HTML object that this component's text
   * has been changed.
   * 
   *   if (c._visible)
   *     $("#" + c.id).show();
   *   else
   *     $("#" + c.id).hide();  
   *
   * @param c
   */  
  void setVisible(Component c);
  
  /**
   * Called by clicking the [x] in the corner of the dialog;
   * send a notification back to the manager via processWindowClosing(key)
   * 
   *   var id = $("div.JDialog:has(#" + element.id + ")")[0].id
   *   var dialog = Jmol.Swing.htDialogs[id];
   *   dialog.manager.processWindowClosing(dialog.registryKey);
   * 
   * @param element
   */
  void windowClosing(HTMLElement element);
 
}
