package org.openscience.jmol.app;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Scanner;

import javax.swing.Timer;

import org.jmol.api.JmolViewer;
import org.jmol.script.ScriptContext;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public class InputScannerThread extends Thread {
 
  private JmolViewer vwr;
  private Scanner scanner;
  private boolean isSilent;

  InputScannerThread(JmolViewer vwr, boolean isSilent) {
    this.vwr = vwr;
    this.isSilent = isSilent;
    start();
  }

  private StringBuilder buffer = new StringBuilder();
  @Override
  public synchronized void start() {
    scanner = new Scanner(System.in);
    scanner.useDelimiter(System.lineSeparator());
    super.start();
  }

  @Override
  public void run() {
    try {
      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      say(null);
      while (true) {
        if (!mainLoop())
          break;        
      }
    } catch (InterruptedException e) {
      System.exit(1);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

 
  private Timer t;
  
  boolean mainLoop() throws InterruptedException {

    if (Viewer.isSwingJS) {
      if (t == null) {
        t = new Timer(100, new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            scan();
          }

        });
        t.setRepeats(false);
        t.start();
      } else {
        t.restart();
      }
      return false;
    }
    Thread.sleep(100);
    scan();
    return true;
  }

  void scan() {
    while (scanner.hasNext()) {
      String s = scanner.next();
      if (s.toLowerCase().equals("exitjmol"))
        System.exit(0);
      if (vwr.checkHalt(s, false)) {
        buffer = new StringBuilder();
        s = "";
      }
      buffer.append(s).append('\n');
      if (!checkCommand() && buffer.length() == 1) {
        say(null);
      }
    }
  }

  private void say(String msg) {
    if (isSilent)
      return;
    if (msg == null)
      msg = "Enter: \nquit     to stop processing and re-initialize input\nexit     to stop all script processing\nexitJmol to exit Jmol\nJmol> ";
    System.out.print(msg);
    System.out.flush();
  }

  private boolean checkCommand() {
    String s = buffer.toString();
    if (s.length() == 1)
      return false;
    Object ret = vwr.scriptCheck(s);
    if (ret instanceof String) {
      s = (String) ret;
      if (s.indexOf("missing END") >= 0)
        return true;
      say(s);
      return false;
    }
    if (ret instanceof ScriptContext) {
      ScriptContext c = (ScriptContext) ret;
      if (!c.isComplete) {
        return true;
      }
    }
    buffer = new StringBuilder();
    s += JC.SCRIPT_NOENDCHECK;
    if (isSilent)
      vwr.evalStringQuiet(s);
    else
      vwr.evalString(s);
    return true;
  }
}
