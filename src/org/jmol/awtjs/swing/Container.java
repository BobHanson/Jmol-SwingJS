package org.jmol.awtjs.swing;

import javajs.util.Lst;
  
abstract public class Container extends Component {
  
  protected Lst<Component> list;
  
  private Component[] cList;

  protected Container(String type) {
    super(type);
  }
  
  public Component getComponent(int i) {
    return list.get(i);
  }
  
  public int getComponentCount() {
    return (list == null ? 0 : list.size());
  }

  public Component[] getComponents() {
    if (cList == null) {
      if (list == null)
        return new Component[0];
      cList = (Component[]) list.toArray();
    }
    return cList;
  }

  public Component add(Component component) {
    return addComponent(component);
  }

  protected Component addComponent(Component component) {
    if (list == null)
      list = new Lst<Component>();
    list.addLast(component);
    cList = null;
    component.parent = this;
    return component;
  }

  protected Component insertComponent(Component component, int index) {
    if (list == null)
      return addComponent(component);
    list.add(index, component);
    cList = null;
    component.parent = this;
    return component;
  }

  public void remove(int i) {
    Component c = list.removeItemAt(i);
    c.parent = null;
    cList = null;
  }
  
  public void removeAll() {
    if (list != null) {
      for (int i = list.size(); --i >= 0;)
        list.get(i).parent = null;
      list.clear();
    }
    cList = null;
  }

  @Override
  public int getSubcomponentWidth() {
    return (list != null && list.size() == 1 ? list.get(0).getSubcomponentWidth() : 0);
  }
  
  @Override
  public int getSubcomponentHeight() {
    return (list != null && list.size() == 1 ? list.get(0).getSubcomponentHeight() : 0);
  }

}
