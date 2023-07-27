/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2018-07-22 20:29:48 -0500 (Sun, 22 Jul 2018) $
 * $Revision: 21922 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package jme;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.Resolver;
import org.jmol.adapter.writers.CDXMLWriter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAdapterAtomIterator;
import org.jmol.api.JmolAdapterBondIterator;
import org.jmol.smiles.SmilesMatcher;
import org.jmol.util.Edge;
import org.jmol.util.Elements;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import javajs.util.P3d;
import javajs.util.PT;
import jme.core.Atom;
import jme.core.Bond;
import jme.core.JMECore;
import jme.io.FileDropper;
import jme.util.JMEUtil;

/**
 * An extension of JME that adds features of Jmol, such as many more file types for reading, 
 * writing of various formats, and substructure searching.  
 * 
 * @author hansonr
 *
 */
@SuppressWarnings("serial")
public class JMEJmol extends JME implements WindowListener {

	Viewer vwr;
	private Container parentWindow;
	private String fileName;

	/**
	 * debug flag; set to false to see an actual 2D structure before SMILES-based
	 * cleaning.
	 */
	private boolean allowClean = true;
	private SmilesMatcher smilesMatcher;
	String[] args = new String[0];
 private boolean cleaning = false;
  
  public JMEJmol() {
    super(null, true, new String[0]);
  }
	public JMEJmol(String[] args) {
		super(null, true, args);
		this.args = args;
		setRemoveHsC();
	}

  public void setViewer(JFrame frame, Viewer vwr, Container parent,
                        String frameType) {
    // from JmolPanel
    parentWindow = parent;
    this.vwr = vwr;
    if (parent == null && frame == null && !"search".equals(frameType))
      headless = vwr.headless;
    if (!headless) {
      if (frame == null) {
        frame = getJmolFrame(frameType, (parent == null));
      }
      setFrame(frame);
    }
    initialize(args);
    if (parent != null) {
      if (vwr != null)
        vwr.getInchi(null, null, null); // initialize InChI
      SwingUtilities.invokeLater(() -> {
        start(new String[0]);
      });

    }
  }

	private JFrame getJmolFrame(String type, boolean exit0) {

		JFrame frame = new JFrame(type == "search" ? "Substructure search" : getTitle());
		JPanel pp = new JPanel();
		JPanel p = new JPanel();
		pp.add("Center", p);
		p.setLayout(new javax.swing.BoxLayout(p, javax.swing.BoxLayout.X_AXIS));
		JButton b;

		if ("search".equals(type)) {
			p.add(b = new JButton("search"));
			b.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					@SuppressWarnings("unused")
					String smiles = smiles();
					@SuppressWarnings("unused")
					Object f = options.getInfo("searchCallback");
					/**
					 * @j2sNative
					 * 
					 *   f && f(smiles);
					 */
				}

			});
		} else {
			if (exit0) {
			frame.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent evt) {
						System.exit(0);
					}
			});
			}
			p.add(b = new JButton("clean"));
			b.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					doClean();
				}

			});
			p.add(b = new JButton("from 3D"));
			b.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					from3D();
				}

			});
			p.add(Box.createHorizontalStrut(5));
			p.add(b = new JButton("replace 3D"));
			b.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					to3D(false);
				}

			});
			p.add(b = new JButton("add 3D"));
			b.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					to3D(true);
				}

			});
			p.add(Box.createHorizontalStrut(5));
			p.add(b = new JButton("to MOL"));
			b.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					toMOL("?jme.mol");
				}

			});
			p.add(b = new JButton("to CDXML"));
			b.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					toCDXML("?jme.cdxml");
				}

			});
			p.add(b = new JButton("to PNG"));
			b.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					toBorderedPNG("?", 10, 10);
				}

			});
      p.add(b = new JButton("InChI"));
      b.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          toInChI();
        }

      });
		}
		frame.add("South", pp);
		frame.setBounds(300, 300, 700, 400);

		frame.addWindowListener(this);
		return frame;
	}

	protected void toInChI() {
	  String mol = molFile();
	  String inchi = vwr.getInchi(null, mol, null);
	  JOptionPane.showInputDialog(this, "Standard InChI", null, JOptionPane.INFORMATION_MESSAGE, null, null, inchi);
  }
  /**
	 * Check for smarts in this structure.
	 * 
	 * For this search we want the most general (c1ccccn1) as the target, 
	 * which is this structure. So we use JME's native code, which 
	 * tends to be most inclusive on aromaticity, thus super.smiles();
	 * 
	 * 
	 * @param pattern
   * @param isSmarts 
	 * @return true if viewer has structure
	 */
	public boolean hasStructure(String pattern, boolean isSmarts) {
		String smiles = super.smiles();
		return vwr.hasStructure(pattern, smiles, isSmarts);
	}


	/**
	 * See if we can match this or the given smarts to any structure in an array of
	 * structures.
	 * 
	 * For this search we want the most general (c1ccccn1) as the target, which in
	 * this case are the SMILES in the array. So we use Jmol's SMILES generator
	 * code, which tends to be moderately inclusive.
	 * 
	 * NCI/CADD would be highly Kekule.
	 * 
	 * 
	 * 
	 * 
	 * Return null for some sort of SMILES initialization error
	 * @param smarts 
	 * 
	 * @param smilesSet
	 * @param isSmarts 
	 * @return int array of 1 or 0
	 */
	public int[] findMatchingStructures(String smarts, String[] smilesSet, boolean isSmarts) {
		try {
			return vwr.getSmilesMatcher().hasStructure(smarts == null ? smilesFromJmol() : smarts, smilesSet, 0);
		} catch (Exception e) {
			say("there was a problem with matching the SMILES " + e);
			e.printStackTrace();
		}
		return null;
	}
	
	
	  public SmilesMatcher getSmilesMatcher() {
		    return (smilesMatcher == null
		        ? (smilesMatcher = new SmilesMatcher()) : smilesMatcher);
		  }




	public byte[] toPNG(String filename) {
		return toBorderedPNG(filename, 10, 10);
	}

	public Dimension getImageSize() {
		Rectangle2D.Double coordBox = activeMol.computeBoundingBoxWithAtomLabels(null);
		double f = molecularAreaScalePixelsPerCoord;
		return new Dimension((int) (coordBox.width * f), (int) (coordBox.height * f));
	}
	
	public String toHtmlDataURI() {
		return "data:image/png;base64," + javajs.util.Base64.getBase64(toBorderedPNG(null, 10, 10)); 
	}

	public byte[] toBorderedPNG(String filename, int marginX, int marginY) {
		try {
			BufferedImage img = getBufferedImage(marginX, marginY);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ImageIO.write(img, "PNG", bos);
			if (filename == null)
				return bos.toByteArray();
			toFile(headless && filename.indexOf("?") >= 0 ? "jmol.png" : filename, bos.toByteArray(), "png");
		} catch (IOException e1) {
			sorry("Something went wrong: " + e1);
		}
		return null;
	}

	public BufferedImage getBufferedImage(int marginX, int marginY) {
		return drawMolecularArea(null, new Point(marginX, marginY));
	}

	public void from3D() {
		if (vwr.getFrameAtoms().isEmpty())
			return;
		Map<String, Object> info = vwr.getCurrentModelAuxInfo();
		if (info == null) {
			sorry("More than one model is visible in Jmol.");
			return;
		}
		boolean is2D = "2D".equals(info.get("dimension"));
		String mol = null;
		try {
			if (is2D) {
				mol = vwr.getModelExtract("thisModel", false, false, "MOL");
			} else {
				String smiles = vwr.getSmiles(vwr.getFrameAtoms());
				mol = getMolFromSmiles(smiles, false);
			}
			if (mol == null) {
				sorry("Something went wrong.");
			}
			clear();
			readMolFile(mol);
		} catch (Exception e) {
			sorry(e.toString());
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
  @Override
	public String smiles() {
		if (activeMol.natoms == 0)
			return "";
		if (/** @j2sNative true ||*/ false) {
			return super.smiles();
		} 
		String mol = molFile();
		if (mol.length() == 0)
			return "";
		return vwr.getInchi(null, mol, "smiles");
	}
	
	public String smilesFromJmol() {
		if (activeMol.natoms == 0)
			return "";
		if (JMEUtil.isSwingJS) {
		    return getSmilesMatcher().getSmilesFromJME(jmeFile());
		} 
		// Java only -- do we need this at all?
		String mol = molFile();
		if (mol.length() == 0)
			return "";
		return vwr.getInchi(null, mol, "smiles");		
	}	
	
	public String inchi() {
		return inchi("standard");
	}
	
	public String inchiFixedH() {
		return inchi("fixedH");
	}
	
	public String inchi(String options) {
		return vwr.getInchi(null, molFile(), (options == null ? "standard" : options));
	}

	/**
	 * Resolve a SMILES using the NCI/CADD Chemical Identifier Resolver
	 * 
	 * @param smiles
	 * @param is3D
	 * @return SMILES
	 */
	private String getMolFromSmiles(String smiles, boolean is3D) {
		System.out.println("JmolJME using SMILES " + smiles);
		String url = JC.resolveDataBase("smiles" + (is3D ? "3D" : "2D"), PT.escapeUrl(smiles), null);

		// (String) vwr.setLoadFormat((is3D ? "$" : "$$") + smiles, '$', false);
		return vwr.getFileAsString(url);
	}

	void sorry(String msg) {
		System.err.println(msg);
		if (!headless)
			JOptionPane.showMessageDialog(this, msg, "Sorry, can't do that.", JOptionPane.INFORMATION_MESSAGE);
	}

	void say(String msg) {
		System.out.println(msg);
		infoText = msg;
//    if (!headless)
//    JOptionPane.showMessageDialog(this, msg, "Hmm.", JOptionPane.INFORMATION_MESSAGE);
	}

	public void to3D(boolean isAppend) {
		String smiles = smiles();
		if (smiles == null || smiles.length() == 0) {
			sorry("There was a problem generating the SMILES from the InChI");
			return;
		}
		System.out.println("using smiles from InChI: " + smiles);
		String mol = getMolFromSmiles(smiles, true);
		Map<String, Object> htParams = new Hashtable<String, Object>();
		vwr.openStringInlineParamsAppend(mol, htParams, isAppend);
		if (!headless) {
			parentWindow.requestFocus();
			vwr.refresh(Viewer.REFRESH_REPAINT, "JmolJME");
		}

	}

	public void setFrameVisible(boolean b) {
		if (myFrame != null)
			myFrame.setVisible(b);
	}


	protected String toMOL(String filename) {
		String mol = molFile();
		if (filename == null) 
			return mol;
		toFile(fixOutFilename(filename), mol, "txt");
		return null;
	}

	/**
	 * Just making sure we do not try to open a dialog if headless.
	 * 
	 * @param filename
	 * @return filename with "_" if headless
	 */
	private String fixOutFilename(String filename) {
		return (!headless ? filename : filename.replace('?', '_'));
	}

	public String toSVG(String filename) {
		String svg = super.getOclSVG();
		if (filename == null) 
			return svg;
		toFile(fixOutFilename(filename), svg, "txt");
		return null;
	}

	protected String toCDXML(String filename) {
		String mol = molFile();
		String xml = CDXMLWriter.fromString(vwr, "Mol", mol);
		if (filename == null)
			return xml;
		toFile(fixOutFilename(filename), xml, "txt");
		return null;
	}

	private void setFileName(String fname) {
		// TODO
		this.fileName = fname;
	}

	private void read2D(String fname, AtomSetCollection asc) {
		this.fileName = fname;
		options.reaction = false;
		JmolAdapter a = vwr.getModelAdapter();
		readAtomSet(a.getAtomIterator(asc), a.getBondIterator(asc));
	}

	protected void openMolByName(String name) {
		try {
			String mol = (String) vwr.setLoadFormat("$$" + name, '$', true);
			mol = vwr.getFileAsString(mol);
			readMolFile(mol);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void readAtomSet(JmolAdapterAtomIterator atomIterator, JmolAdapterBondIterator bondIterator) {
		// duplicated code, a pointer to a function would solve the problem?
		JMEmolList inputMolList = new JMEmolList();
		try {
			JMEmol mol = new JMEmol(null, params);
			createJMEFromJmolAdapter(mol, atomIterator, bondIterator);
			inputMolList.add(mol);
			if (!inputMolList.isReallyEmpty()) {
				processIncomingMolecules(inputMolList, true);
			}
			if (activeMol.checkNeedsCleaning()) {
				say("Close atoms found; cleaning");
				doClean();
				repaint();
			}
		} catch (Exception e) {
			info(makeErrorMessage(e));
		}

	}

	public static void createJMEFromJmolAdapter(JMECore mol, JmolAdapterAtomIterator atomIterator, JmolAdapterBondIterator bondIterator) {
		Map<Object, Integer> atomMap = new Hashtable<Object, Integer>();
		while (atomIterator.hasNext()) {
			String sym = Elements.elementSymbolFromNumber(atomIterator.getElementNumber());
			// from Jmol -- could be 13C;
			Atom a = mol.createAtom(sym);
			atomMap.put(atomIterator.getUniqueID(), Integer.valueOf(mol.natoms));
			P3d pt = atomIterator.getXYZ();
			a.x = pt.x;
			a.y = -pt.y;
			a.q = atomIterator.getFormalCharge();
			mol.setAtom(mol.natoms, JmolAdapter.getElementSymbol(atomIterator.getElement()));
		}
		while (bondIterator.hasNext()) {
			Bond b = mol.createAndAddBondFromOther(null);
			b.va = atomMap.get(bondIterator.getAtomUniqueID1()).intValue();
			b.vb = atomMap.get(bondIterator.getAtomUniqueID2()).intValue();
			int bo = bondIterator.getEncodedOrder();
			switch (bo) {
			case Edge.BOND_STEREO_NEAR:
				b.bondType = Bond.SINGLE;
				b.stereo = Bond.STEREO_UP;
				break;
			case Edge.BOND_STEREO_FAR:
				b.bondType = Bond.SINGLE;
				b.stereo = Bond.STEREO_DOWN;
				break;
			case Edge.BOND_COVALENT_SINGLE:
			case Edge.BOND_AROMATIC_SINGLE:
				b.bondType = Bond.SINGLE;
				break;
			case Edge.BOND_COVALENT_DOUBLE:
			case Edge.BOND_AROMATIC_DOUBLE:
				b.bondType = Bond.DOUBLE;
				break;
			case Edge.BOND_COVALENT_TRIPLE:
				b.bondType = Bond.TRIPLE;
				break;
			case Edge.BOND_AROMATIC:
			case Edge.BOND_STEREO_EITHER:
			default:
				if ((bo & 0x07) != 0)
					b.bondType = (bo & 0x07);
				break;
			}
		}

		mol.finalizeMolecule();
	}


	private void read3D(String fname) {
		String mol = vwr.getFileAsString(fname);
		loadSmilesCleanly(getSmiles(mol));
	}

	private String getSmiles(String mol) {
		return vwr.getInchi(null, mol, "smiles");
	}

	
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String name = evt.getPropertyName();
		Object val = evt.getNewValue();
		if (val == null)
			return;
		try {
			if (name == FileDropper.PROPERTY_FILEDROPPER_FILE) {
				readFile((String) val);
			} else if (name == FileDropper.PROPERTY_FILEDROPPER_INLINE) {
				readDroppedData(val);
			}
		} catch (Throwable t) {
			System.err.println("JME couldn't load data for drop " + name);
		}
	}

	@Override
	protected void readDroppedData(Object newValue) {
		String data = newValue.toString();
		String trimmed = data.trim();
		// BH 2023.1.18 Allowing for copying with a bit of whitespace for SMILES
		try {
			if (trimmed.indexOf("\n") >= 0)
				readMolFile(data);
			else if (trimmed.indexOf(" ") >= 0)
				readMolecule(data);
			else
				readSmiles(trimmed);
			activeMol.center();
		} catch (Exception e) {
			System.err.println("JME error reading data starting with " + data.substring(Math.min(data.length(), 100)));
		}

	}

	@Override
	protected void readSmiles(String data) {
		try {
			readMolFile(getOclAdapter().SMILEStoMOL(data));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void readDroppedTextFile(String fileName) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		InputStream fis = null;
		try {
			fis = (fileName.indexOf("://") >= 0 ? new URL(fileName).openStream() : new FileInputStream(fileName));
			byte[] bytes = new byte[0x1000];
			int n;
			while ((n = fis.read(bytes)) > 0) {
				bos.write(bytes, 0, n);
			}
			fis.close();
		} catch (Exception e) {
			System.err.println("JME error reading file " + fileName);
		}
		readDroppedData(new String(bos.toByteArray()));
	}
	
	void doClean() {
		if (!allowClean)
			return;
		String smiles = vwr.getInchi(null, molFile(), "smiles");
		loadSmilesCleanly(smiles);
	}

	/**
	 * SMILES to InChI to MOL
	 * 
	 * @param smiles
	 */
	private void loadSmilesCleanly(String smiles) {
		if (smiles == null || smiles.length() == 0 || cleaning)
			return;
		System.out.println("using smiles from InChI: " + smiles);
		String mol = null;
		try {
			cleaning = true;
			mol = getMolFromSmiles(smiles, false);
			if (mol == null) {
				sorry("Something went wrong.");
			} else {
				readMolFile(mol);
			}
		} catch (Exception e) {
			sorry(e.toString());
			e.printStackTrace();
		} finally {
			cleaning = false;
		}
	}

	protected Object readFile(String fname) {
		try {
			setFileName(fname);
			File f = new File(fname);
		    System.out.println("JmolJME reading file " + f.getAbsolutePath());

			// from file dropper
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fname));
			boolean isBinary = (Resolver.getBinaryType(bis) != null);
			String type = vwr.getModelAdapter().getFileTypeName(bis);
			bis.close();
			if ("Jme".equals(type)) {
				clear();
				readMolecule(vwr.getFileAsString(fname));
				activeMol.center();
				return null;
			}
			Map<String, Object> htParams = new Hashtable<String, Object>();
			htParams.put("filter", "NOH;NO3D;fileType=" + type);
			htParams.put("binary", Boolean.valueOf(isBinary));
			vwr.setLoadParameters(htParams, false);
			bis = new BufferedInputStream(new FileInputStream(fname));
			Object ret = vwr.getModelAdapter().getAtomSetCollectionFromReader(fname, bis, htParams);
			if (ret instanceof AtomSetCollection) {
				AtomSetCollection asc = (AtomSetCollection) ret;
				Map<String, Object> info = asc.getAtomSetAuxiliaryInfo(0);
				boolean is2D = "2D".equals(info.get("dimension"));
				clear();
				if (is2D) {
					read2D(fname, asc);
				} else {
					read3D(fname);
				}
//				activeMol.center();
			} else {
				sorry(ret.toString());
				return ret.toString();
			}
		} catch (Exception e) {
			sorry(e.toString());
			e.printStackTrace();
			return e;
		}
		repaint();
		System.out.println("JJME " + fname);
		return null;
	}

	private void toFile(String name, final Object bytesOrString, final String type) {
		boolean useThread = (name.indexOf("?") >= 0);
		if (useThread && headless) {
			sorry("Filenames must not contain '?' in headless mode - '?' replaced with '_'");
			name = name.replace('?', '_');
			useThread = false;
		}
		final String finalName = name;
		Runnable r = new Runnable() {

			@Override
			public void run() {
				System.out.println("JmolJME writing file " + finalName);
				boolean haveDialog = (finalName.startsWith("?"));
				String f = vwr.writeFile(finalName, bytesOrString, type);
				if (haveDialog && f != null && f.indexOf("OK") == 0) {
					int pt = f.indexOf(" ", 3);
					f = f.substring(f.indexOf(" ", pt + 1)).trim();
					pt = f.lastIndexOf("/");
					if (pt <= 0)
						return;
					f = f.substring(0, pt + 1);
					vwr.setStringProperty("currentLocalPath", f);
				}
				System.out.println(f);
			}
		};
		if (useThread) {
			new Thread(r).start();
		} else {
			r.run();
		}
	}

	@Override
	protected void subclassAddToCopyMenu(JPopupMenu popup, boolean hasAtom) {
		addMenuItem(popup,hasAtom, "Save " + "as " + "SVG graphic", "Jmol-saveSVG");
		addMenuItem(popup,hasAtom, "Save " + "as " + "PNG image", "Jmol-savePNG");
		addMenuItem(popup,hasAtom, "Save " + "as " + "MOL file", "Jmol-saveMOL");
		addMenuItem(popup,hasAtom, "Save " + "as " + "CDXML file", "Jmol-saveCDXML");
	}

	@Override
	protected boolean subclassHandleMenuAction(String cmd) {
		switch (cmd) {
		case "Jmol-savePNG":
			toBorderedPNG("?jme.png", 10, 10);
			return true;
		case "Jmol-saveSVG":
			toSVG("?jme.svg");
			return true;
		case "Jmol-saveMOL":
			toMOL("?jme.mol");
			return true;
		case "Jmol-saveCDXML":
			toCDXML("?jme.cdxml");
			return true;
		}
		return false;
	}
	private String getTitle() {
		return "JME-SwingJS 2D Molecular Editor" + (fileName == null ? "" : " " + fileName);
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// 

	}

	@Override
	public void windowClosing(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
		if (myFrame != null)
			myFrame.setVisible(false);
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// 

	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// 

	}

	@Override
	public void windowActivated(WindowEvent e) {
		// 

	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// 

	}

	public void dispose() {
		// dereference externals
		vwr = null;
		if (myFrame != null)
			myFrame.dispose();
		myFrame = null;
		parentWindow = null;
	}

	@Override
	protected void handleAdditionalParameters() {

	}

	
	public static void main(String[] args) {
		JFrame frame = null;
	    Map<String, Object> info = new Hashtable<String, Object>();
	    info.put("isApp",Boolean.TRUE);
	    info.put("headless",Boolean.TRUE);
	    info.put("noscripting", Boolean.TRUE);
	    info.put("noDisplay", Boolean.TRUE);
	    info.put("repaintManager", "NONE");
		Viewer vwr = new Viewer(info);
    JMEJmol jjme = new JMEJmol(new String[] { JME.NO_INIT });
		jjme.vwr = vwr;

		String type = null;
		
		boolean dostart = true;
		if (args.length > 0) {
			if (args[0].equals("headless")) {
        jjme.initialize(args);
				jjme.options("headless");
				dostart = false;
			} else if (args[0].equals("search")) {
        jjme.initialize(args);
				type = "search";
			} else if (args[0].equals("test")) {
				switch (args[1]) {
				case "headless":
					jjme.initialize(args);
					testJMEHeadless(jjme);
					dostart = false;
					break;
				case "data":
					jjme.initialize(args);
					testJmolData(jjme, args);
					dostart = false;
					break;
				case "jmol":
					type = "jmol";
					break;
				}
			}
		}
		if (dostart)
		  startJmolJME(frame, jjme, type);
    /**
     * @j2sNative
     * 
     * return jjme;
     */

		
		
		// testJmolData(args);
		// testJMEHeadless(jjme);
	}

	private static void startJmolJME(JFrame frame, JMEJmol jjme, String type) {
		JFrame embeddingFrame = null;
		if ("jmol".equals(type)) {
			embeddingFrame = new JFrame();
		} else if ("search".equals(type)) {
		  // don't create frame
		} else if (frame == null) {
			frame = new JFrame("JmolJME Molecular Editor");
			frame.setName("JME"); // for embedding in <div id="testApplet-JME-div">
			frame.setBounds(300, 200, 24 * 18, 24 * 16); // urcuje dimensions pre
			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent evt) {
					System.exit(0);
				}
			});
		}

		jjme.setViewer(frame, jjme.vwr, embeddingFrame, type);
		jjme.vwr.getInchi(null, null, null); // initialize InChI

		// jjme.openMolByName( "cholesterol");
		// jjme.openMolByName("morphine");
		// jjme.read2Dor3DFile("c:/temp/jmetest.mol");
		// jjme.read2Dor3DFile("c:/temp/cdx/t2.cdxml");
		// jjme.read2Dor3DFile("c:/temp/t.cdx");

		SwingUtilities.invokeLater(() -> {
			jjme.myFrame.setVisible(true);
			jjme.start(new String[0]);
		});
	}

	private static void testJMEHeadless(JMEJmol jjme) {
		jjme.options("headless");
		//jjme.openMolByName("cholesterol");
		jjme.openMolByName("morphine");
//		jjme.readFile("data/jmol.mol");

//    jjme.readMolFile(vwr.getFileAsString("c:/temp/jmetest.mol"));
//    jjme.readSmiles("CCCCCCOCC");

		jjme.toBorderedPNG("c:/temp/test.png", 10, 10);
	}

	private static void testJmolData(JMEJmol jjme, String[] args) {
		JFrame frame = new JFrame("JmolJME Molecular Editor");
		frame.setName("JME"); // for embedding in <div id="testApplet-JME-div">

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evt) {
				System.exit(0);
			}
		});
		frame.setBounds(300, 200, 24 * 18, 24 * 16); // urcuje dimensions pre
		jjme.vwr.getInchi(null, null, null); // initialize InChI
		jjme.setViewer(frame, jjme.vwr, null, null);

		//jjme.openMolByName( "cholesterol");
		//jjme.openMolByName("morphine");
		//jjme.read2Dor3DFile("c:/temp/jmetest.mol");
		//jjme.read2Dor3DFile("c:/temp/cdx/t2.cdxml");
		//jjme.read2Dor3DFile("c:/temp/t.cdx");
		
		SwingUtilities.invokeLater(() -> {
			jjme.start(args);
			jjme.readFile("data/3af.cdxml");
		});
		
	  }


}
