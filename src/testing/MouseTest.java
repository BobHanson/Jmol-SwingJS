package testing;


/*
 * Simplified MouseMotionEventDemo.java
 *
 */
 
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseMotionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.GridLayout;
 
import javax.swing.*;

public class MouseTest extends JPanel implements MouseMotionListener {
    BlankArea blankArea;
    JTextArea textArea;
    static final String NEWLINE = System.getProperty("line.separator");
     
    public static void main(String[] args) {
         
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
     
    private static Font f = new Font(null, Font.PLAIN, 48);
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("MouseTest for swingjs");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         
        //Create and set up the content pane.
        JComponent newContentPane = new MouseTest();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
         
        //Display the window.
        frame.setFont(f);
        frame.pack();
        frame.setVisible(true);
    }
     
    public MouseTest() {
        super(new GridLayout(0,1));
        blankArea = new BlankArea(Color.YELLOW);
        add(blankArea);
         
        textArea = new JTextArea();
        textArea.setFont(f);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(200, 75));
         
        add(scrollPane);
        
        JButton clearBtn=new JButton("Clear Messages");
        clearBtn.setSize(100, 30);
        clearBtn.addActionListener(
            new ActionListener() {
                @Override
								public void actionPerformed(ActionEvent e) {
                	textArea.setText("");
                }
            }
        );
        blankArea.add(clearBtn);
         
        //Register for mouse events on blankArea and panel.
        blankArea.addMouseMotionListener(this);
        addMouseMotionListener(this);
        blankArea.addMouseListener(new MouseListener() {

          @Override
          public void mouseClicked(MouseEvent e) {
            System.out.println("mouseClicked " + e);
          }

          @Override
          public void mousePressed(MouseEvent e) {
            System.out.println("mousePressed " + e);
          }

          @Override
          public void mouseReleased(MouseEvent e) {
            System.out.println("mouseReleased " + e);
          }

          @Override
          public void mouseEntered(MouseEvent e) {
            System.out.println("mouseEntered " + e);
          }

          @Override
          public void mouseExited(MouseEvent e) {
            System.out.println("mouseExited " + e);
          }
          
        });
         
        setPreferredSize(new Dimension(750, 750));
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
    }
     
    void eventOutput(String eventDescription, MouseEvent e) {
        textArea.append(eventDescription
                + " (" + e.getX() + "," + e.getY() + ")"
                + " detected on "
                + e.getComponent().getClass().getName()
                + NEWLINE);
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
     
    public void mouseMoved(MouseEvent e) {
        eventOutput("Mouse moved", e);
    }
     
    public void mouseDragged(MouseEvent e) {
      System.out.println("mouseDragged " + e);
        eventOutput("Mouse dragged", e);
    }
    
    // inner class to test mouse actions
    public class BlankArea extends JLabel {
      Dimension minSize = new Dimension(100, 50);
   
      public BlankArea(Color color) {
          setBackground(color);
          setOpaque(true);
          setBorder(BorderFactory.createLineBorder(Color.black));
      }
   
      public Dimension getMinimumSize() {
          return minSize;
      }
   
      public Dimension getPreferredSize() {
          return minSize;
      }
  }
}
