package org.jmol.awt;

import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;

import org.jmol.api.SC;

/**
 * A javax.swing implementation of SwingComponent 
 * that mirrors org.jmol.awtjs.swing for compatibility with 
 * compilation with java2script.
 * 
 * 
 * @author Bob Hanson, hansonr
 * 
 */
public class AwtSwingComponent implements SC {

  // jc, ab, jmi, and jm are all aliases for the same object.
  // We do this so that we can use a generic "SwingComponent"
  // for both Java and JavaScript, reducing code duplication.
  
  public JComponent jc;
  AbstractButton ab;
  JMenuItem jmi;
  JMenu jm;
  private SC parent; 

  static SC getPopup(String title, Map<Object, SC> htSources) {
    AwtSwingComponent c = new AwtSwingComponent();
    c.jc = new JPopupMenu(title);
    htSources.put(c.jc, c);    
    return c;
  }
  
  static SC getMenu(String name, Map<Object, SC> htSources) {
    AwtSwingComponent c = new AwtSwingComponent();
    c.jc = c.ab = c.jmi = c.jm = new JMenu(name);
    c.jc.setName(name);
    htSources.put(c.jc, c);    
    return c;
  }

  static SC getMenuItem(final AwtPopupHelper helper, String name, Map<Object, SC> htSources) {
    AwtSwingComponent c = new AwtSwingComponent();
    c.jc = c.ab = c.jmi = new JMenuItem(name) {
      private MenuElement[] path;
      
      @Override
      public void setArmed(boolean b) {
        super.setArmed(b);
        if (b)
          path = MenuSelectionManager.defaultManager().getSelectedPath();
      }

      @Override
      public void doClick(int n) {
        super.doClick(n);
          helper.reinstateMenu(this, path);
      }


    };
    htSources.put(c.jc, c);    
    return c;
  }
  
  static SC getRadio(final AwtPopupHelper helper, String name, Map<Object, SC> htSources) {
    AwtSwingComponent c = new AwtSwingComponent();
    c.jc = c.ab = c.jmi = new JRadioButtonMenuItem(name) {
      private MenuElement[] path;
      
      @Override
      public void setArmed(boolean b) {
        super.setArmed(b);
        if (b)
          path = MenuSelectionManager.defaultManager().getSelectedPath();
      }

      @Override
      public void doClick(int n) {
        super.doClick(n);
          helper.reinstateMenu(this, path);
      }


    };

    htSources.put(c.jc, c);
    return c;
  }

  static SC getCheckBox(final AwtPopupHelper helper, String name, Map<Object, SC> htSources) {
    AwtSwingComponent c = new AwtSwingComponent();
    c.jc = c.ab = c.jmi = new JCheckBoxMenuItem(name) {
      private MenuElement[] path;
      
      @Override
      public void setArmed(boolean b) {
        super.setArmed(b);
        if (b)
          path = MenuSelectionManager.defaultManager().getSelectedPath();
      }

      @Override
      public void doClick(int n) {
        super.doClick(n);
          helper.reinstateMenu(this, path);
      }


    };

    htSources.put(c.jc, c);    
    return c;
  }

  @Override
  public void add(SC item) {
    if (item == null || item.getIcon() == null
        && (item.getText() == null || item.getText().length() == 0)) {
      if (jm == null)
        ((JPopupMenu) jc).addSeparator();
      else
        jm.addSeparator();
      return;
    }

    AwtSwingComponent c = (AwtSwingComponent) item;
    jc.add(c.jc);

    // in Java, a menuItem gets one and only one parent.
    // This is never changed, even if the item is moved 
    // to another menu. So we set this exactly ONCE.

    if (c.parent == null)
      c.parent = this;
  }

  @Override
  public void addActionListener(Object owner) {
    ab.addActionListener((ActionListener) owner);
  }

  @Override
  public void addItemListener(Object owner) {
    ab.addItemListener((ItemListener) owner);
  }

  @Override
  public void addMouseListener(Object owner) {
    jc.addMouseListener((MouseListener) owner);
  }

  @Override
  public String getActionCommand() {
    return ab.getActionCommand();
  }

  @Override
  public Object getComponent(int i) {
    return (i < 0 ? jc : jm == null ? jc.getComponent(i) : jm.getItem(i));
  }

  @Override
  public int getComponentCount() {
    return (jm == null ? jc.getComponentCount() : jm.getItemCount());
  }

  @Override
  public Object[] getComponents() {
    return jc.getComponents();
  }

  @Override
  public String getName() {
    return jc.getName();
  }

  @Override
  public Object getParent() {
    return parent; 
    // note that in this case we could be getting a JPopupMenu, I think.
    
  }

  @Override
  public Object getPopupMenu() {
    return jm.getPopupMenu();
  }

  @Override
  public String getText() {
    return ab.getText();
  }

  @Override
  public Object getIcon() {
    return ab.getIcon();
  }

  @Override
  public void init(String text, Object icon, String actionCommand,
                   SC popupMenu) {
    setText(text);
    setIcon(icon);
    setActionCommand(actionCommand);
  }

  @Override
  public void insert(SC subMenu, int index) {
    if (jc instanceof JPopupMenu)
      ((JPopupMenu)jc).insert(((AwtSwingComponent) subMenu).jm, index);
    else
      ((JMenu) jc).insert(((AwtSwingComponent)subMenu).jm, index);
 }

  @Override
  public boolean isEnabled() {
    return jc.isEnabled();
  }

  @Override
  public boolean isSelected() {
    return ab.isSelected();
  }

  @Override
  public void remove(int i) {
    jc.remove(i);
  }

  @Override
  public void removeAll() {
    jc.removeAll();
  }

  @Override
  public void setActionCommand(String script) {
    ab.setActionCommand(script);
  }

  @Override
  public void setAutoscrolls(boolean b) {
    jc.setAutoscrolls(b);
  }

  @Override
  public void setEnabled(boolean enabled) {
    jc.setEnabled(enabled);
  }

  @Override
  public void setName(String name) {
    jc.setName(name);
  }

  @Override
  public void setSelected(boolean b) {
    ab.setSelected(b);
  }

  @Override
  public void setText(String text) {
    ab.setText(text);
  }

  @Override
  public void setIcon(Object icon) {
    ab.setIcon((Icon) icon);
  }

}