package org.jmol;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;

public class Test2 extends JPanel {

  // Main application
  public static void main(String[] args) {
    new Test2(strXyzHOH);
  }

  public Test2() {
    adapter = new SmarterJmolAdapter();
    viewer = JmolViewer.allocateViewer(this, adapter);
    JFrame newFrame = new JFrame();
    newFrame.getContentPane().add(this);
    newFrame.setSize(300, 300);
    newFrame.setVisible(true);
  }

  public Test2(String model) {
    adapter = new SmarterJmolAdapter();
    viewer = JmolViewer.allocateViewer(this, adapter);
    JFrame newFrame = new JFrame();
    newFrame.getContentPane().add(this);
    newFrame.setSize(300, 300);
    newFrame.setVisible(true);
    newFrame.addWindowListener(new AppCloser());
    viewer.loadInline(model);
  }

  private final static String strXyzHOH = 
      "3\n" +
      "water\n" +
  		"O  0.0 0.0 0.0\n" +
  		"H  0.76923955 -0.59357141 0.0\n" +
  		"H -0.76923955 -0.59357141 0.0\n";

  private JmolViewer viewer;
  private JmolAdapter adapter;
  private Dimension currentSize = new Dimension();

  @Override
  public void paint(Graphics g) {
    getSize(currentSize);
    viewer.renderScreenImage(g, currentSize.width, currentSize.height);
  }

  /**
   * To shutdown when run as an application.  This is a
   * fairly lame implementation.   A more self-respecting
   * implementation would at least check to see if a save
   * was needed.
   */
  protected final class AppCloser extends WindowAdapter {

    @Override
    public void windowClosing(WindowEvent e) {
      System.exit(0);
    }
  }

}
