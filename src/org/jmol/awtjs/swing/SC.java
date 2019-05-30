package org.jmol.awtjs.swing;

/**
 * SwingComponent interface common to javax.swing and org.jmol.awtjs.swing
 * 
 * Can be augmented as needed, provided classes of org.jmol.awtjs.swing are also
 * updated. (SwingComponents in javajs are subclasses of AbstractButton.)
 * 
 */

public interface SC {

  void add(SC item);

  void addActionListener(Object owner);

  void addItemListener(Object owner);

  void addMouseListener(Object owner);

  String getActionCommand();

  Object getComponent(int i);

  int getComponentCount();

  Object[] getComponents();

  String getName();

  Object getParent();

  Object getPopupMenu();

  Object getIcon();

  String getText();

  void init(String text, Object icon, String actionCommand, SC popupMenu);

  void insert(SC subMenu, int index);

  boolean isEnabled();

  boolean isSelected();

  void remove(int i);

  void removeAll();

  void setActionCommand(String script);

  void setAutoscrolls(boolean b);

  void setEnabled(boolean enable);

  void setIcon(Object icon);

  void setName(String string);

  void setSelected(boolean state);

  void setText(String entry);

}
