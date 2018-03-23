package org.jmol.viewer.binding;

import javajs.awt.event.Event;
import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;

import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;


abstract public class Binding {

  // 2            1           0
  // 0 9876 5432 1098 7654 3210
  //                          x shift
  //                         x  ctrl
  //                       x    alt
  //                    x       wheel
  //                     x      left
  //                       x    middle (same as alt)
  //                        x   right
  //                     x  x   left-right (Apple COMMAND)
  //                     x xx   button_mask
  //                    xx xxxx button_modifier_mask
  
  // 2            1           0
  // 0 9876 5432 1098 7654 3210
  //                x           single
  //               x            double
  //               xx           click_mask
  
  // 2            1           0
  // 0 9876 5432 1098 7654 3210
  //           x                down       
  //          x                 drag
  //         x                  up
  //        x                   click
  //        xxxx                mode_mask
  
  
  public final static int LEFT = Event.MOUSE_LEFT;
  public final static int MIDDLE = Event.MOUSE_MIDDLE;
  public final static int RIGHT = Event.MOUSE_RIGHT;
  public final static int WHEEL = Event.MOUSE_WHEEL; 
  public final static int ALT = Event.ALT_MASK; // MIDDLE and ALT are the same
  public final static int CTRL = Event.CTRL_MASK;
  public final static int SHIFT = Event.SHIFT_MASK;
  public final static int CTRL_ALT = Event.CTRL_ALT;
  public final static int CTRL_SHIFT = Event.CTRL_SHIFT;
  public final static int MAC_COMMAND = Event.MAC_COMMAND;
  public final static int BUTTON_MASK = Event.BUTTON_MASK;
  public final static int BUTTON_MODIFIER_MASK = CTRL | SHIFT | LEFT | MIDDLE | RIGHT | WHEEL;

  public final static int SINGLE = 1 << 8;
  public final static int DOUBLE = 2 << 8;
  public final static int COUNT_MASK = SINGLE | DOUBLE;
  
  public final static int DOWN  = 1 << 12;
  public final static int DRAG  = 2 << 12;
  public final static int UP    = 4 << 12;
  public final static int CLICK = 8 << 12;

  public final static int MODE_MASK = DOWN | DRAG | UP | CLICK;
  
  public static int getMouseAction(int clickCount, int modifiers, int mode) {
    if (clickCount > 2)
      clickCount = 2;
    switch (clickCount) {
    case 0:
      break;
    case 1:
      clickCount = SINGLE;
      break;
    default:
      clickCount = DOUBLE;
    }
    switch (mode) {
    case Event.PRESSED:
      mode = DOWN;
      break;
    case Event.DRAGGED:
      mode = DRAG;
      break;
    case Event.RELEASED:
      mode = UP;
      break;
    case Event.CLICKED:
      mode = CLICK;
      break;
    case Event.WHEELED:
      mode = WHEEL;
    }
    return (modifiers & BUTTON_MODIFIER_MASK) | clickCount | mode;   
  }

  /**
   * create an action code from a string such as "CTRL-LEFT-double click"
   * 
   * @param desc
   * @return action code
   */
  public static int getMouseActionStr(String desc) {
    if (desc == null)
      return 0;
    int mouseAction = 0;
    desc = desc.toUpperCase();

    if (desc.indexOf("MIDDLE") >= 0)
      mouseAction = MIDDLE;
    else if (desc.indexOf("RIGHT") >= 0)
      mouseAction = RIGHT;
    else if (desc.indexOf("WHEEL") >= 0)
      mouseAction = WHEEL;
    else
      mouseAction = LEFT;

    if (desc.indexOf("DOWN") >= 0)
      mouseAction |= DOWN;
    else if (desc.indexOf("DRAG") >= 0)
      mouseAction |= DRAG;
    else if (desc.indexOf("UP") >= 0)
      mouseAction |= UP;
    else if (mouseAction != WHEEL)
      mouseAction |= CLICK;

      if (mouseAction != WHEEL && desc.indexOf("DOUBLE") >= 0)
        mouseAction |= DOUBLE;
      else if (mouseAction > 0)
        mouseAction |= SINGLE;

      if (mouseAction != (SINGLE | WHEEL) && desc.indexOf("ALT") >= 0)
        mouseAction |= ALT;

    if (desc.indexOf("CTRL") >= 0)
      mouseAction |= CTRL;
    if (desc.indexOf("SHIFT") >= 0)
      mouseAction |= SHIFT;

    return mouseAction;
  }

  public static int getButtonMods(int mouseAction) {
    return mouseAction & BUTTON_MODIFIER_MASK;
  }
  
  public static int getClickCount(int mouseAction) {
    return (mouseAction & COUNT_MASK) >> 8;
  }

  public static String getMouseActionName(int mouseAction, boolean addSortCode) {
    SB sb = new SB();
    if (mouseAction == 0)
      return "";
    boolean isMiddle = (includes(mouseAction, MIDDLE)
        && !includes(mouseAction, LEFT) && !includes(mouseAction, RIGHT));
    char[] code = "      ".toCharArray();
    if (includes(mouseAction, CTRL)) {
      sb.append("CTRL+");
      code[5] = 'C';
    }
    if (!isMiddle && includes(mouseAction, ALT)) {
      sb.append("ALT+");
      code[4] = 'A';
    }
    if (includes(mouseAction, SHIFT)) {
      sb.append("SHIFT+");
      code[3] = 'S';
    }

    if (includes(mouseAction, LEFT)) {
      code[2] = 'L';
      sb.append("LEFT");
    } else if (includes(mouseAction, RIGHT)) {
      code[2] = 'R';
      sb.append("RIGHT");
    } else if (isMiddle) {
      code[2] = 'M';
      sb.append("MIDDLE");
    } else if (includes(mouseAction, WHEEL)) {
      code[2] = 'W';
      sb.append("WHEEL");
    }
    if (includes(mouseAction, DOUBLE)) {
      sb.append("+double");
      code[1] = '2';
    }
    if (includes(mouseAction, DOWN)) {
      sb.append("+down");
      code[0] = '1';
    } else if (includes(mouseAction, DRAG)) {
      sb.append("+drag");
      code[0] = '2';
    } else if (includes(mouseAction, UP)) {
      sb.append("+up");
      code[0] = '3';
    } else if (includes(mouseAction, CLICK)) {
      sb.append("+click");
      code[0] = '4';
    }

    return (addSortCode ? new String(code) + ":" + sb.toString() : sb
        .toString());
  }

  public String name;
  private Map<String, Object> bindings = new Hashtable<String, Object>();
  public Map<String, Object> getBindings() {
    return bindings;
  }
    
  public Binding() {
  }
  
  public final void bindAction(int mouseAction, int jmolAction) {
    //System.out.println("binding " + mouseAction + " " + getMouseActionName(mouseAction, false) + "\t" + jmolAction);
    addBinding(mouseAction + "\t" + jmolAction, new int[] {mouseAction, jmolAction});
  }
  
  public void bindName(int mouseAction, String name) {
    addBinding(mouseAction + "\t", Boolean.TRUE);
    addBinding(mouseAction + "\t" + name, new String[] { getMouseActionName(mouseAction, false), name });
  }


  public final void unbindAction(int mouseAction, int jmolAction) {
    if (mouseAction == 0)
      unbindJmolAction(jmolAction);
    else
      removeBinding(null, mouseAction + "\t" + jmolAction);
  }
  
  public final void unbindName(int mouseAction, String name) {
    if (name == null)
      unbindMouseAction(mouseAction);
    else
      removeBinding(null, mouseAction + "\t" + name);
  }
  
  public final void unbindJmolAction(int jmolAction) {
    Iterator<String> e = bindings.keySet().iterator();
    String skey = "\t" + jmolAction;
    while (e.hasNext()) {
      String key = e.next();
      if (key.endsWith(skey))
        removeBinding(e, key);
    }
  }
  
  private void addBinding(String key, Object value) {
    if (Logger.debugging)
      Logger.debug("adding binding " + key + "\t==\t" + Escape.e(value));
    bindings.put(key, value);
  }
  private void removeBinding(Iterator<String> e, String key) {
    if (Logger.debugging)
      Logger.debug("removing binding " + key);
    if (e == null)
      bindings.remove(key); 
    else
      e.remove();
  }
  
  public final void unbindUserAction(String script) {
    Iterator<String> e = bindings.keySet().iterator();
    String skey = "\t" + script;
    while (e.hasNext()) {
      String key = e.next();
      if (key.endsWith(skey))
        removeBinding(e, key);
    }
  }
  
  public final void unbindMouseAction(int mouseAction) {
    Iterator<String> e = bindings.keySet().iterator();
    String skey = mouseAction + "\t";
    while (e.hasNext()) {
      String key = e.next();
      if (key.startsWith(skey))
        removeBinding(e, key);
    }
  }
  
  public final boolean isBound(int mouseAction, int jmolAction) {
    //System.out.println("checking isBound " + mouseAction + " " + getMouseActionName(mouseAction, false) + "\t" + jmolAction);
    return bindings.containsKey(mouseAction + "\t" + jmolAction);
  }
  
  public final boolean isUserAction(int mouseAction) {
    return bindings.containsKey(mouseAction + "\t");
  }

  @SuppressWarnings("unchecked")
  public String getBindingInfo(String[] actionInfo, String[] actionNames,
                               String qualifiers) {
    SB sb = new SB();
    String qlow = (qualifiers == null || qualifiers.equalsIgnoreCase("all") ? null
        : qualifiers.toLowerCase());
    Lst<String>[] names = new Lst[actionInfo.length];
    Lst<String[]> user = new Lst<String[]>();
    for (Object obj : bindings.values()) {
      if (obj instanceof Boolean) {
        // ignore
      } else if (AU.isAS(obj)) {
        String action = ((String[]) obj)[0];
        String script = ((String[]) obj)[1];
        if (qlow == null || qlow.indexOf("user") >= 0 || action.indexOf(qlow) >= 0 || script.indexOf(qlow) >= 0)
          user.addLast((String[]) obj);
      } else {
        
        // we cannot test using AU.isAI() here because of a Java2Script compiler error
        // in relation to new int[] {.....}  BH  2015.02.03
        
        int[] info = (int[]) obj;
        int i = info[1];
        if (names[i] == null)
          names[i] = new Lst<String>();
        String name = getMouseActionName(info[0], true);
        if (qlow == null
          || (actionNames[i] + ";" + actionInfo[i] + ";" + name).toLowerCase().indexOf(qlow) >= 0)
        names[i].addLast(name);
      }
    }
    for (int i = 0; i < actionInfo.length; i++) {
      int n;
      if (names[i] == null || (n = names[i].size()) == 0)
        continue;
      addInfo(sb, names[i].toArray(new String[n]), actionNames[i], actionInfo[i]);
    }
    for (int i = 0; i < user.size(); i++) {
      String[] info = user.get(i);
      addInfo(sb, new String[] {"USER:::" + info[0]}, "user-defined", info[1]);
    }
    return sb.toString();
  }
  
  private void addInfo(SB sb, String[] list, String name, String info) {
    Arrays.sort(list);
    PT.leftJustify(sb, "                      ", name);
    sb.append("\t");
    String sep = "";
    int len = sb.length();
    for (int j = 0; j < list.length; j++) {
      sb.append(sep).append(list[j].substring(7));
      sep = ", ";
    }
    len = sb.length() - len;
    if (len < 20)
      sb.append("                 ".substring(0, 20 - len));
    sb.append("\t").append(info).appendC('\n');
  }


  private static boolean includes(int mouseAction, int mod) {
    return ((mouseAction & mod) == mod);
  }
  
  public static Binding newBinding(Viewer vwr, String name) {
    return (Binding) Interface.getInterface("org.jmol.viewer.binding." + name + "Binding", vwr, "script");
  }
}
