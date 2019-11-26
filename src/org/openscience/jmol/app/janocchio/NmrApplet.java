/*  
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 * 
 *  It is derived in part from Jmol 
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.openscience.jmol.app.janocchio;

public class NmrApplet {//extends JApplet {
//    
//    String[] backupFileName = new String[2];
//    Nmr nmr;
//    boolean detached = false;
//    JFrame myFrame;
//    Container myContainer;
//    Dimension origSize;
//    JPanel loadPanel;
//    
//    /** Creates a new instance of NmrApplet */
//    public NmrApplet() {
//        // retain ability to find old .jnc files
//        backupFileName[0] = System.getProperty("user.home") + System.getProperty("file.separator") + ".last.jsn";
//        backupFileName[1] = System.getProperty("user.home") + System.getProperty("file.separator") + ".last.jnc";
//    }
//
//    public void init() {
//        loadPanel = new JPanel() {
//            
//            public void paintComponent(Graphics g) {
//                super.paintComponent(g);
//                g.setFont(new Font("sansserif", Font.PLAIN, 18));
//                g.setColor(Color.black);
//                g.fillRect(0,0,getWidth()-1, getHeight()-1);
//                g.setColor(Color.white);
//                g.drawString("Janocchio is loading....",40,40);
//                
//            }
//        } ;   
//        getContentPane().add(loadPanel);
//    }
//    
//    
//    public void detach() {
//        
//	if (!detached) {
//	    detached = true;
//	    myContainer = getParent();
//            origSize = new Dimension(getSize());
//	    myFrame = new SeparateFrame("Janocchio");
//	    myFrame.getContentPane().add(this);
//	    myFrame.pack();
//	    myFrame.setVisible(true);
//            
//            JMenu menu = nmr.getMenuBar().getMenu(5);
//            menu.getItem(0).setEnabled(false);
//            menu.getItem(1).setEnabled(true);
//	}
//    }
//
//
//    public void reattach() {
//	detached = false;
//	myFrame.getContentPane().remove(this);
//	myFrame.dispose();
//	myFrame = null;
//	setSize(origSize);
//	myContainer.add(this);
//        
//        JMenu menu = nmr.getMenuBar().getMenu(5);
//         
//        menu.getItem(0).setEnabled(true);
//        menu.getItem(1).setEnabled(false);
//    }
//    
//    public void start() {
//        setVisible(true);
//        Splash splash = null;
//        
//        JFrame frame = new JFrame();
//        nmr = new Nmr(splash, frame, null, 500, 500, "");
//        
//        File file0 = new File(backupFileName[0]);
//        File file1 = new File(backupFileName[1]);
//        if (file1.exists() && ! file0.exists()) {
//            loadFile(file1,backupFileName[1],false);            
//        }
//        else if (file0.exists()) {
//            loadFile(file0,backupFileName[0],true);
//        }
//        
//        getContentPane().remove(loadPanel);
//        getContentPane().add(nmr.getMainSplitPane());
//        nmr.addApplet(this);
//    
//        super.start();
//    }
//    
//    private void loadFile(File file, String fileName, boolean ljson) {
//        int opt = JOptionPane.showConfirmDialog(this,"NMR Data File "+ fileName + " found.\n Do you want to load this data?","Previous NMR Data",JOptionPane.YES_NO_OPTION);
//            if (opt == JOptionPane.YES_OPTION) {
//                try {
//                    if (ljson) {
//                        nmr.readNmrDataJSON(file);
//                    }
//                    else {
//                        nmr.readNmrData(file);
//                    }
//                }
//                catch (Exception e) {
//                    //
//                }
//            }
//    }
//    
//    public void stop() {
//      
//        if (nmr.getCurrentStructureFile() != null) {
//            // always write .jsn files now
//            File file = new File(backupFileName[0]);
//            try {
//               nmr.writeNmrDataJSON(file);
//            }
//            catch (Exception e) {
//               //          
//            }
//        }
//
//        super.stop();
//    }
//    
//    class SeparateFrame extends JFrame {
//        
//        SeparateFrame(String title) {
//            setTitle(title);
//            addWindowListener(
//                        new WindowAdapter() {
//                             public void windowClosing(WindowEvent e) {
//                                    reattach();
//                             }
//                        });
//            
//        }
//    }
}
