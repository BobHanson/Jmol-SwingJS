/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Universit� Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.UIResource;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.applications.FileNameExtensionFilter;
import fr.orsay.lri.varna.applications.VARNAPrinter;
import fr.orsay.lri.varna.exceptions.ExceptionExportFailed;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionJPEGEncoding;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.exceptions.ExceptionNAViewAlgorithm;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import fr.orsay.lri.varna.exceptions.ExceptionPermissionDenied;
import fr.orsay.lri.varna.exceptions.ExceptionUnmatchedClosingParentheses;
import fr.orsay.lri.varna.exceptions.ExceptionWritingForbidden;
import fr.orsay.lri.varna.factories.RNAFactory;
import fr.orsay.lri.varna.models.FullBackup;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.VARNAEdits;
import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation;
import fr.orsay.lri.varna.models.annotations.HighlightRegionAnnotation;
import fr.orsay.lri.varna.models.annotations.TextAnnotation;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModeleBaseNucleotide;
import fr.orsay.lri.varna.models.rna.ModeleBasesComparison;
import fr.orsay.lri.varna.models.rna.RNA;


public class VueUI {
	
	/**
	 *
	 * BH SwingJS
	 * 
	 *      JavaScript cannot wait on a thread and so cannot wait for the result
	 *      of a customized modal JOptionPane.
	 * 
	 *      Instead, we make any JPanel that is placed in the JOptionPane
	 *      implement Runnable. In Java, we simply run that runnable here when an OK
	 *      response is delivered; in JavaScript, we run the runnable within the
	 *      JavaScript version of JOptionPane as an asynchronous callback.
	 *      
	 *      Same for a CANCEL, CLOSED, ERROR, or FINAL response.
	 *      
	 *      Note that for simple error or warning messages, this is not required; they will use simple HTML5 messages.
	 * 
	 */
   public Runnable okBtnCallback, cancelBtnCallback, closeBtnCallback, errorCallback, finalCallback, noBtnCallback, objectCallback;

   public Object dialogReturnValue;
	
   public Object getDialogReturnValue() {
		return dialogReturnValue;
	}

	public Throwable dialogError;
	
	/**
	 * BH SwingJS
	 * 
	 * 
	 * @return
	 */
	public Throwable getDialogError() {
		return dialogError;
	}


	/**
	 * BH SwingJS
	 * 
	 * Initiate a message dialog with callback for OK and close.
	 * 
	 * @param messagePanel
	 * @param title
	 * @param ok
	 * @param close
	 */
	private void showMessageDialog(Object messagePanel, String title, int messageType, Runnable ok, Runnable close) {
		okBtnCallback = ok;
		closeBtnCallback = close;
		JOptionPane.showMessageDialog(_vp, messagePanel, title, messageType);
	}




	/**
	 * BH SwingJS
	 * 
	 * Initiate a confirm dialog with callbacks.
	 * 
	 * @param optionPanel
	 * @param title
	 * @param ok
	 * @param cancel
	 * @param close_final_error optional close,finally,error
	 */
	public void showConfirmDialog(JPanel optionPanel, String title, Runnable ok, Runnable cancel, Runnable... close_final_error) {
		okBtnCallback = ok;
		cancelBtnCallback = cancel;
		closeBtnCallback = (close_final_error.length > 0 ? close_final_error[0] : null);
		finalCallback = (close_final_error.length > 1 ? close_final_error[1] : null);
		errorCallback = (close_final_error.length > 2 ? close_final_error[2] : null);		
		onDialogReturn(JOptionPane.showConfirmDialog(_vp, optionPanel, title, JOptionPane.OK_CANCEL_OPTION));
	}

	/**
	 * BH SwingJS
	 * 
	 * Initiate an input dialog with callbacks.
	 * 
	 * @param message
	 * @param initialValue
	 * @param input
	 * @param close_final_error optional [close,finally,error]
	 */
	public void showInputDialog(String message, Object initialValue, Runnable input, Runnable... close_final_error) {
		objectCallback = input;
		closeBtnCallback = (close_final_error.length > 0 ? close_final_error[0] : null);
		finalCallback = (close_final_error.length > 1 ? close_final_error[1] : null);
		errorCallback = (close_final_error.length > 2 ? close_final_error[2] : null);		
		onDialogReturn(JOptionPane.showInputDialog(_vp, message, initialValue));
	}

	/**
	 * BH SwingJS
	 * 
	 * Initiate an color chooser dialog with callbacks.
	 * 
	 * @param message
	 * @param initialValue
	 * @param input
	 * @param close_final_error optional [close,finally,error]
	 */
	public void showColorDialog(String message, Object initialValue, Runnable ret) {
		objectCallback = ret;
		onDialogReturn(JColorChooser.showDialog(_vp, message, (Color) initialValue));
	}

	
	/**
	 * BH SwingJS
	 * 
	 * A general method to handle all the showInputDialog, JFileChooser, and JColorChooser callbacks from all the VueXXX classes.
	 * 
	 * The initial return to be ignored is an object that is an instanceof UIResource.
	 * 
	 */
	public void onDialogReturn(Object value) {
		dialogReturnValue = value;
		if (objectCallback != null && !(value instanceof UIResource))
			objectCallback.run();
	}
	
	/**
	 * BH SwingJS
	 * 
	 * A general method to handle all the showConfirmDialog callbacks from all
	 * the VueXXX classes.
	 * 
	 * The initial return to be ignored is NaN, testable as 
	 * value != Math.floor(value).
	 * 
	 */
	public void onDialogReturn(int value) {
		try {
			switch (value) {
			case JOptionPane.OK_OPTION | JOptionPane.YES_OPTION:
				if (okBtnCallback != null)
					okBtnCallback.run();
				break;
			case JOptionPane.NO_OPTION:
				if (noBtnCallback != null)
					noBtnCallback.run();
				break;
			case JOptionPane.CANCEL_OPTION:
				if (cancelBtnCallback != null)
					cancelBtnCallback.run();
				break;
			case JOptionPane.CLOSED_OPTION:
				if (closeBtnCallback != null)
					closeBtnCallback.run();
				break;
			}
		} catch (Throwable e) {
			dialogError = e;
			if (errorCallback != null)
				errorCallback.run();
		} finally {
			if (value != Math.floor(value)) {
				// asynchronous deferred
				return;
			}
			if (finalCallback != null)
				finalCallback.run();
			okBtnCallback = noBtnCallback = cancelBtnCallback = closeBtnCallback = errorCallback = objectCallback = null;
			dialogError = null;
		}
	}
	
	
    protected VARNAPanel _vp;
	private File _fileChooserDirectory = null;
	private UndoableEditSupport _undoableEditSupport;

	public VueUI(VARNAPanel vp) {
		_vp = vp;
		_undoableEditSupport = new UndoableEditSupport(_vp);
	}

	public void addUndoableEditListener(UndoManager manager) {
		_undoableEditSupport.addUndoableEditListener(manager);
	}

	public void UIToggleColorMap() {
		if (_vp.isModifiable()) {
			_vp.setColorMapVisible(!_vp.getColorMapVisible());
			_vp.repaint();
		}
	}

	public void UIToggleDrawBackbone() {
		if (_vp.isModifiable()) {
			_vp.setDrawBackbone(!_vp.getDrawBackbone());
			_vp.repaint();
		}
	}

	public Hashtable<Integer, Point2D.Double> backupAllCoords() {
		Hashtable<Integer, Point2D.Double> tmp = new Hashtable<Integer, Point2D.Double>();
		for (int i = 0; i < _vp.getRNA().getSize(); i++) {
			tmp.put(i, _vp.getRNA().getCoords(i));
		}
		return tmp;
	}

	public void UIToggleFlatExteriorLoop() {
		if (_vp.isModifiable()
				&& _vp.getRNA().get_drawMode() == RNA.DRAW_MODE_RADIATE) {
			Hashtable<Integer, Point2D.Double> bck = backupAllCoords();
			_undoableEditSupport.postEdit(new VARNAEdits.RedrawEdit(
					RNA.DRAW_MODE_RADIATE, _vp, !_vp.getFlatExteriorLoop()));
			_vp.setFlatExteriorLoop(!_vp.getFlatExteriorLoop());
			_vp.reset();
			_vp.drawRNA(_vp.getRNA(), RNA.DRAW_MODE_RADIATE);
			_vp.repaint();
			_vp.fireLayoutChanged(bck);
		}
	}

	public void UIRadiate() {
		if (_vp.isModifiable()) {
			Hashtable<Integer, Point2D.Double> bck = backupAllCoords();
			_undoableEditSupport.postEdit(new VARNAEdits.RedrawEdit(
					RNA.DRAW_MODE_RADIATE, _vp));
			_vp.reset();
			_vp.drawRNA(_vp.getRNA(), RNA.DRAW_MODE_RADIATE);
			_vp.repaint();
			_vp.fireLayoutChanged(bck);
		}
	}

	public void UIMOTIFView() {
		if (_vp.isModifiable()) {
			Hashtable<Integer, Point2D.Double> bck = backupAllCoords();
			_undoableEditSupport.postEdit(new VARNAEdits.RedrawEdit(
					RNA.DRAW_MODE_MOTIFVIEW, _vp));
			_vp.reset();
			_vp.drawRNA(_vp.getRNA(), RNA.DRAW_MODE_MOTIFVIEW);
			_vp.repaint();
			_vp.fireLayoutChanged(bck);
		}
	}

	public void UILine() {
		if (_vp.isModifiable()) {
			Hashtable<Integer, Point2D.Double> bck = backupAllCoords();
			_undoableEditSupport.postEdit(new VARNAEdits.RedrawEdit(
					RNA.DRAW_MODE_LINEAR, _vp));
			_vp.reset();
			_vp.drawRNA(_vp.getRNA(), RNA.DRAW_MODE_LINEAR);
			_vp.repaint();
			_vp.fireLayoutChanged(bck);
		}
	}

	public void UICircular() {
		if (_vp.isModifiable()) {
			Hashtable<Integer, Point2D.Double> bck = backupAllCoords();
			_undoableEditSupport.postEdit(new VARNAEdits.RedrawEdit(
					RNA.DRAW_MODE_CIRCULAR, _vp));
			_vp.reset();
			_vp.drawRNA(_vp.getRNA(), RNA.DRAW_MODE_CIRCULAR);
			_vp.repaint();
			_vp.fireLayoutChanged(bck);
		}
	}

	public void UINAView() {
		if (_vp.isModifiable()) {
			Hashtable<Integer, Point2D.Double> bck = backupAllCoords();
			_undoableEditSupport.postEdit(new VARNAEdits.RedrawEdit(
					RNA.DRAW_MODE_NAVIEW, _vp));
			_vp.reset();
			_vp.drawRNA(_vp.getRNA(), RNA.DRAW_MODE_NAVIEW);
			_vp.repaint();
			_vp.fireLayoutChanged(bck);
		}
	}

	public void UIVARNAView() {
		if (_vp.isModifiable()) {
			Hashtable<Integer, Point2D.Double> bck = backupAllCoords();
			_undoableEditSupport.postEdit(new VARNAEdits.RedrawEdit(
					RNA.DRAW_MODE_VARNA_VIEW, _vp));
			_vp.reset();
			_vp.drawRNA(_vp.getRNA(), RNA.DRAW_MODE_VARNA_VIEW);
			_vp.repaint();
			_vp.fireLayoutChanged(bck);
		}
	}

	public void UIReset() {
		if (_vp.isModifiable()) {
			Hashtable<Integer, Point2D.Double> bck = backupAllCoords();
			_undoableEditSupport.postEdit(new VARNAEdits.RedrawEdit(_vp
					.getRNA().get_drawMode(), _vp));
			_vp.reset();
			_vp.drawRNA(_vp.getRNA(), _vp.getRNA().get_drawMode());
			_vp.repaint();
			_vp.fireLayoutChanged(bck);
		}
	}

	private void savePath(JFileChooser jfc) {
		_fileChooserDirectory = jfc.getCurrentDirectory();
	}

	private void loadPath(JFileChooser jfc) {
		if (_fileChooserDirectory != null) {
			jfc.setCurrentDirectory(_fileChooserDirectory);
		}
	}

	public void UIChooseRNAs(ArrayList<RNA> rnas) {
		if (rnas.size() > 5) {
			final VueRNAList vrna = new VueRNAList(rnas);
			Runnable ok = new Runnable() {

				@Override
				public void run() {
					for (RNA r : vrna.getSelectedRNAs()) {
						try {
							r.drawRNA(_vp.getConfig());
						} catch (ExceptionNAViewAlgorithm e) {
							e.printStackTrace();
						}
						_vp.showRNA(r);
					}
					_vp.repaint();
				}

			};
			showConfirmDialog(vrna, "Select imported sequence/structures", ok, null);
		} else {
			for (RNA r : rnas) {
				try {
					r.drawRNA(_vp.getConfig());
				} catch (ExceptionNAViewAlgorithm e) {
					e.printStackTrace();
				}
				_vp.showRNA(r);
			}
			_vp.repaint();
		}
	}

	public void UIFile() throws ExceptionNonEqualLength {
		if (_vp.isModifiable()) {
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
			fc.setDialogTitle("Open...");
			loadPath(fc);
			if (fc.showOpenDialog(_vp) == JFileChooser.APPROVE_OPTION) {
				try {
					savePath(fc);
					String path = fc.getSelectedFile().getAbsolutePath();
					if (!path.toLowerCase().endsWith(".varna")) {
						ArrayList<RNA> rnas = RNAFactory.loadSecStr(path);
						if (rnas.isEmpty()) {
							throw new ExceptionFileFormatOrSyntax("No RNA could be parsed from that source.");
						} else {
							UIChooseRNAs(rnas);
						}
					} else {
						FullBackup bck = _vp.loadSession(fc.getSelectedFile()); // was path
					}
				} catch (ExceptionExportFailed e1) {
					_vp.errorDialog(e1);
				} catch (ExceptionPermissionDenied e1) {
					_vp.errorDialog(e1);
				} catch (ExceptionLoadingFailed e1) {
					_vp.errorDialog(e1);
				} catch (ExceptionFileFormatOrSyntax e1) {
					_vp.errorDialog(e1);
				} catch (ExceptionUnmatchedClosingParentheses e1) {
					_vp.errorDialog(e1);
				} catch (FileNotFoundException e) {
					_vp.errorDialog(e);
				}
			}
		}
	}

	public void UISetColorMapStyle() {
		final VueColorMapStyle vcms = new VueColorMapStyle(_vp);
		Runnable ok = new Runnable() {

			@Override
			public void run() {
				_vp.setColorMap(vcms.getColorMap());
			}
			
		};
		Runnable cancel = new Runnable() {

			@Override
			public void run() {
				vcms.cancelChanges();
			}
						
		};
		showConfirmDialog(vcms, "Choose color map style", ok, cancel, cancel, null, null);
	}

	public void UILoadColorMapValues() {
		final VueLoadColorMapValues vcmv = new VueLoadColorMapValues(_vp);
		Runnable ok = new Runnable(){
			
			@Override
			public void run() {
				_vp.setColorMapVisible(true);
				try {
					_vp.readValues(vcmv.getReader());
				} catch (IOException e) {
					_vp.errorDialog((Exception) getDialogError());
				}
			}
			
		};
		showConfirmDialog(vcmv, "Load base values", ok, null);
	}

	public void UISetColorMapValues() {
		final VueBaseValues vbv = new VueBaseValues(_vp);
		Runnable cancel = new Runnable() {

			@Override
			public void run() {
				vbv.cancelChanges();
			}
						
		};
		showConfirmDialog(vbv, "Choose base values", null, cancel);
	}

	public void UIManualInput() throws ParseException, ExceptionNonEqualLength {
		if (_vp.isModifiable()) {
			final VueManualInput manualInput = new VueManualInput(_vp);
			Runnable ok = new Runnable() {

				@Override
				public void run() {
					if (_vp.getRNA().getSize() == 0) {

					}
					try {
						RNA r = new RNA();
						VARNAConfig cfg = new VARNAConfig();
						r.setRNA(manualInput.getTseq().getText(), manualInput.getTstr().getText());
						r.drawRNA(_vp.getRNA().get_drawMode(), cfg);
						_vp.drawRNAInterpolated(r);
						_vp.repaint();
					} catch (ExceptionFileFormatOrSyntax e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ExceptionNAViewAlgorithm e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ExceptionUnmatchedClosingParentheses e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			};
			showConfirmDialog(manualInput.getPanel(), "Input sequence/structure", ok, null);
		}
	}

	public void UISetTitle() {
		if (_vp.isModifiable()) {
			Runnable input = new Runnable() {

				@Override
				public void run() {
					String res = (String) getDialogReturnValue();
					if (res != null) {
				_vp.setTitle(res);
						_vp.repaint();
					}
				}
				
			};
			
			showInputDialog("Input title", _vp.getTitle(), input);
		}
	}

	public void UISetColorMapCaption() {
		if (_vp.isModifiable()) {
			Runnable input = new Runnable() {

				@Override
				public void run() {
					String res = (String) getDialogReturnValue();						
					if (res != null) {
						_vp.setColorMapCaption(res);
						_vp.repaint();
					}
				}
				
			};
			showInputDialog("Input new color map caption", _vp.getColorMapCaption(), input);
		}
	}

	public void UISetBaseCharacter() {
		if (_vp.isModifiable()) {
			final int i = _vp.getNearestBase();

			if (_vp.isComparisonMode()) {

				Runnable input = new Runnable() {

					@Override
					public void run() {
						String res = (String) getDialogReturnValue();
						if (res != null) {
							ModeleBasesComparison mb = (ModeleBasesComparison) _vp.getRNA().get_listeBases().get(i);
							String bck = mb.getBase1() + "|" + mb.getBase2();
							mb.setBase1(((res.length() > 0) ? res.charAt(0) : ' '));
							mb.setBase2(((res.length() > 1) ? res.charAt(1) : ' '));
							_vp.repaint();
							_vp.fireSequenceChanged(i, bck, res);
						}
					}

				};

				
				
				showInputDialog("Input base", ((ModeleBasesComparison) _vp.getRNA().get_listeBases().get(i)).getBases(),  input);

			} else {

				Runnable input = new Runnable() {

					@Override
					public void run() {
						String res = (String) getDialogReturnValue();
						if (res != null) {
							ModeleBaseNucleotide mb = (ModeleBaseNucleotide) _vp.getRNA().get_listeBases().get(i);
							String bck = mb.getBase();
							mb.setBase(res);
							_vp.repaint();
							_vp.fireSequenceChanged(i, bck, res);
						}
					}

				};
				showInputDialog("Input base", ((ModeleBaseNucleotide) _vp.getRNA().get_listeBases().get(i)).getBase(), input);
			}
		}
	}

	FileNameExtensionFilter _varnaFilter = new FileNameExtensionFilter(
			"VARNA Session File", "varna", "VARNA");
	FileNameExtensionFilter _bpseqFilter = new FileNameExtensionFilter(
			"BPSeq (CRW) File", "bpseq", "BPSEQ");
	FileNameExtensionFilter _ctFilter = new FileNameExtensionFilter(
			"Connect (MFold) File", "ct", "CT");
	FileNameExtensionFilter _dbnFilter = new FileNameExtensionFilter(
			"Dot-bracket notation (Vienna) File", "dbn", "DBN", "faa", "FAA");

	FileNameExtensionFilter _jpgFilter = new FileNameExtensionFilter(
			"JPEG Picture", "jpeg", "jpg", "JPG", "JPEG");
	FileNameExtensionFilter _pngFilter = new FileNameExtensionFilter(
			"PNG Picture", "png", "PNG");
	FileNameExtensionFilter _epsFilter = new FileNameExtensionFilter(
			"EPS File", "eps", "EPS");
	FileNameExtensionFilter _svgFilter = new FileNameExtensionFilter(
			"SVG Picture", "svg", "SVG");
	FileNameExtensionFilter _xfigFilter = new FileNameExtensionFilter(
			"XFig Diagram", "fig", "xfig", "FIG", "XFIG");
	FileNameExtensionFilter _tikzFilter = new FileNameExtensionFilter(
			"PGF/Tikz diagram", "tex", "pgf");

	public void UIExport() throws ExceptionExportFailed,
			ExceptionPermissionDenied, ExceptionWritingForbidden,
			ExceptionJPEGEncoding {
		ArrayList<FileNameExtensionFilter> v = new ArrayList<FileNameExtensionFilter>();
		v.add(_epsFilter);
		v.add(_svgFilter);
		v.add(_tikzFilter);
		v.add(_xfigFilter);
		v.add(_jpgFilter);
		v.add(_pngFilter);
		String dest = UIChooseOutputFile(v);
		if (dest != null) {
			String extLower = dest.substring(dest.lastIndexOf('.'))
					.toLowerCase();
			// System.out.println(extLower);
			if (extLower.equals(".eps")) {
				_vp.getRNA().saveRNAEPS(dest, _vp.getConfig());
			} else if (extLower.equals(".svg")) {
				_vp.getRNA().saveRNASVG(dest, _vp.getConfig());
			} else if (extLower.equals(".fig") || extLower.equals(".xfig")) {
				_vp.getRNA().saveRNAXFIG(dest, _vp.getConfig());
			} else if (extLower.equals(".pgf") || extLower.equals(".tex")) {
				_vp.getRNA().saveRNATIKZ(dest, _vp.getConfig());
			} else if (extLower.equals(".png")) {
				saveToPNG(dest);
			} else if (extLower.equals(".jpg") || extLower.equals(".jpeg")) {
				saveToJPEG(dest);
			}
		}
	}

	public void UIExportJPEG() throws ExceptionJPEGEncoding,
			ExceptionExportFailed {
		String dest = UIChooseOutputFile(_jpgFilter);
		if (dest != null) {
			saveToJPEG(dest);
		}
	}

	public void UIPrint() {
		VARNAPrinter.printComponent(_vp);
	}

	public void UIExportPNG() throws ExceptionExportFailed {
		String dest = UIChooseOutputFile(_pngFilter);
		if (dest != null) {
			saveToPNG(dest);
		}
	}

	public void UIExportXFIG() throws ExceptionExportFailed,
			ExceptionWritingForbidden {
		String dest = UIChooseOutputFile(_xfigFilter);
		if (dest != null) {
			_vp.getRNA().saveRNAXFIG(dest, _vp.getConfig());
		}
	}

	public void UIExportTIKZ() throws ExceptionExportFailed,
			ExceptionWritingForbidden {
		String dest = UIChooseOutputFile(_tikzFilter);
		if (dest != null) {
			_vp.getRNA().saveRNATIKZ(dest, _vp.getConfig());
		}
	}

	public void UIExportEPS() throws ExceptionExportFailed,
			ExceptionWritingForbidden {
		String dest = UIChooseOutputFile(_epsFilter);
		if (dest != null) {
			_vp.getRNA().saveRNAEPS(dest, _vp.getConfig());
		}
	}

	public void UIExportSVG() throws ExceptionExportFailed,
			ExceptionWritingForbidden {
		String dest = UIChooseOutputFile(_svgFilter);
		if (dest != null) {
			_vp.getRNA().saveRNASVG(dest, _vp.getConfig());
		}
	}

	public void UISaveAsDBN() throws ExceptionExportFailed,
			ExceptionPermissionDenied {
		String name = _vp.getVARNAUI().UIChooseOutputFile(_dbnFilter);
		if (name != null)
			_vp.getRNA().saveAsDBN(name, _vp.getTitle());
	}

	public void UISaveAsCT() throws ExceptionExportFailed,
			ExceptionPermissionDenied {
		String name = _vp.getVARNAUI().UIChooseOutputFile(_ctFilter);
		if (name != null)
			_vp.getRNA().saveAsCT(name, _vp.getTitle());
	}

	public void UISaveAsBPSEQ() throws ExceptionExportFailed,
			ExceptionPermissionDenied {
		String name = _vp.getVARNAUI().UIChooseOutputFile(_bpseqFilter);
		if (name != null)
			_vp.getRNA().saveAsBPSEQ(name, _vp.getTitle());
	}

	public void UISaveAs() throws ExceptionExportFailed,
			ExceptionPermissionDenied {
		ArrayList<FileNameExtensionFilter> v = new ArrayList<FileNameExtensionFilter>();
		v.add(_bpseqFilter);
		v.add(_dbnFilter);
		v.add(_ctFilter);
		v.add(_varnaFilter);
		String dest = UIChooseOutputFile(v);
		if (dest != null) {
			String extLower = dest.substring(dest.lastIndexOf('.'))
					.toLowerCase();
			if (extLower.endsWith("bpseq")) {
				_vp.getRNA().saveAsBPSEQ(dest, _vp.getTitle());
			} else if (extLower.endsWith("ct")) {
				_vp.getRNA().saveAsCT(dest, _vp.getTitle());
			} else if (extLower.endsWith("dbn") || extLower.endsWith("faa")) {
				_vp.getRNA().saveAsDBN(dest, _vp.getTitle());
			} else if (extLower.endsWith("varna")) {
				_vp.saveSession(dest);
			}
		}
	}

	public String UIChooseOutputFile(FileNameExtensionFilter filtre) {
		ArrayList<FileNameExtensionFilter> v = new ArrayList<FileNameExtensionFilter>();
		v.add(filtre);
		return UIChooseOutputFile(v);
	}

	/**
	 * Opens a save dialog with right extensions and return the absolute path
	 * 
	 * @param filtre
	 *            Allowed extensions
	 * @return <code>null</code> if the user doesn't approve the save dialog,<br>
	 *         <code>absolutePath</code> if the user approve the save dialog
	 */
	public String UIChooseOutputFile(ArrayList<FileNameExtensionFilter> filtre) {
		JFileChooser fc = new JFileChooser();
		loadPath(fc);
		String absolutePath = null;
		// applique le filtre
		for (int i = 0; i < filtre.size(); i++) {
			fc.addChoosableFileFilter(filtre.get(i));
		}
		// en mode open dialog pour voir les autres fichiers avec la meme
		// extension
		fc.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
		fc.setDialogTitle("Save...");
		// Si l'utilisateur a valider
		if (fc.showSaveDialog(_vp) == JFileChooser.APPROVE_OPTION) {
			savePath(fc);
			absolutePath = fc.getSelectedFile().getAbsolutePath();
			String extension = _vp.getPopupMenu().get_controleurMenu()
					.getExtension(fc.getSelectedFile());
			FileFilter f = fc.getFileFilter();
			if (f instanceof FileNameExtensionFilter) {
				ArrayList<String> listeExtension = new ArrayList<String>();
				listeExtension.addAll(Arrays
						.asList(((FileNameExtensionFilter) f).getExtensions()));
				// si l'extension du fichier ne fait pas partie de la liste
				// d'extensions acceptées
				if (!listeExtension.contains(extension)) {
					absolutePath += "." + listeExtension.get(0);
				}
			}
		}
		return absolutePath;
	}

	public void UISetBorder() {
		VueBorder border = new VueBorder(_vp);
		final Dimension oldBorder = _vp.getBorderSize();
		_vp.drawBBox(true);
		_vp.drawBorder(true);
		_vp.repaint();
		Runnable cancel = new Runnable () {

			@Override
			public void run() {
				_vp.setBorderSize(oldBorder);
			}
			
		};
		Runnable final_ = new Runnable () {

			@Override
			public void run() {
				_vp.drawBorder(false);
				_vp.drawBBox(false);
				_vp.repaint();
			}
			
		};
		
		showConfirmDialog(border.getPanel(), "Set new border size", null, cancel, cancel, final_);
	}

	public void UISetBackground() {
		showColorDialog("Choose new background color", _vp.getBackground(), new Runnable() {

			@Override
			public void run() {
				if (dialogReturnValue != null) {
					_vp.setBackground((Color) dialogReturnValue);
					_vp.repaint();
				}
			}
			
		});
	}

	public void UIZoomIn() {
		double _actualZoom = _vp.getZoom();
		double _actualAmount = _vp.getZoomIncrement();
		Point _actualTranslation = _vp.getTranslation();
		double newZoom = Math.min(VARNAConfig.MAX_ZOOM, _actualZoom
				* _actualAmount);
		double ratio = newZoom / _actualZoom;
		Point newTrans = new Point((int) (_actualTranslation.x * ratio),
				(int) (_actualTranslation.y * ratio));
		_vp.setZoom(newZoom);
		_vp.setTranslation(newTrans);
		// verification que la translation ne pose pas de problemes
		_vp.checkTranslation();
		// System.out.println("Zoom in");
		_vp.repaint();
	}

	public void UIZoomOut() {
		double _actualZoom = _vp.getZoom();
		double _actualAmount = _vp.getZoomIncrement();
		Point _actualTranslation = _vp.getTranslation();
		double newZoom = Math.max(_actualZoom / _actualAmount,
				VARNAConfig.MIN_ZOOM);
		double ratio = newZoom / _actualZoom;
		Point newTrans = new Point((int) (_actualTranslation.x * ratio),
				(int) (_actualTranslation.y * ratio));
		_vp.setZoom(newZoom);
		_vp.setTranslation(newTrans);
		// verification que la translation ne pose pas de problemes
		_vp.checkTranslation();
		_vp.repaint();
	}

	public void UICustomZoom() {
		VueZoom zoom = new VueZoom(_vp);
		final double oldZoom = _vp.getZoom();
		final double oldZoomAmount = _vp.getZoomIncrement();
		_vp.drawBBox(true);
		_vp.repaint();
		Runnable cancel = new Runnable() {

			@Override
			public void run() {
				_vp.setZoom(oldZoom);
				_vp.setZoomIncrement(oldZoomAmount);
			}
			
		};
		Runnable final_ = new Runnable() {

			@Override
			public void run() {
				_vp.drawBBox(false);
				_vp.repaint();
			}
			
		};
		showConfirmDialog(zoom.getPanel(), "Set zoom", null, cancel, cancel, final_);
	}

	public void UIGlobalRescale() {
		if (_vp.isModifiable()) {
			if (_vp.getRNA().get_listeBases().size() > 0) {
				final VueGlobalRescale rescale = new VueGlobalRescale(_vp);
				Runnable cancel = new Runnable() {

					@Override
					public void run() {
						UIGlobalRescale(1. / rescale.getScale());
					}
					
				};
				Runnable final_ = new Runnable() {

					@Override
					public void run() {
						_vp.drawBBox(false);
						_vp.repaint();
					}
					
				};
				showConfirmDialog(rescale.getPanel(), "Rescales the whole RNA (No redraw)", null, cancel, cancel, final_);
			}
		}
	}

	public void UIGlobalRescale(double d) {
		if (_vp.isModifiable()) {
			if (_vp.getRNA().get_listeBases().size() > 0) {
				_vp.globalRescale(d);
				_undoableEditSupport.postEdit(new VARNAEdits.RescaleRNAEdit(d,
						_vp));
			}
		}
	}

	public void UIGlobalRotation() {
		if (_vp.isModifiable()) {
			if (_vp.getRNA().get_listeBases().size() > 0) {
				_vp.drawBBox(true);
				_vp.repaint();
				final VueGlobalRotation rotation = new VueGlobalRotation(_vp);
				Runnable cancel = new Runnable() {

					@Override
					public void run() {
						UIGlobalRotation(-rotation.getAngle());
					}
					
				};
				Runnable final_ = new Runnable() {

					@Override
					public void run() {
						_vp.drawBBox(false);
						_vp.repaint();
					}
					
				};
				showConfirmDialog(rotation.getPanel(), "Rotates the whole RNA", null, cancel, cancel, final_, null);
			}
		}
	}

	public void UIGlobalRotation(double d) {
		if (_vp.isModifiable()) {
			if (_vp.getRNA().get_listeBases().size() > 0) {
				_vp.globalRotation(d);
				_undoableEditSupport.postEdit(new VARNAEdits.RotateRNAEdit(d,
						_vp));
			}
		}
	}

	public void UISetBPStyle() {
		if (_vp.getRNA().get_listeBases().size() > 0) {
			VueStyleBP bpstyle = new VueStyleBP(_vp);
			final VARNAConfig.BP_STYLE bck = _vp.getBPStyle();
			Runnable cancel = new Runnable() {

				@Override
				public void run() {
					_vp.setBPStyle(bck);
					_vp.repaint();
				}
				
			};
			showConfirmDialog(bpstyle.getPanel(), "Set main base pair style", null, cancel, cancel);
		}
	}

	public void UISetTitleColor() {
		if (_vp.isModifiable()) {
			showColorDialog("Choose new title color", _vp.getTitleColor(), new Runnable() {

				@Override
				public void run() {
					if (dialogReturnValue != null) {
						_vp.setTitleColor((Color) dialogReturnValue);
						_vp.repaint();
					}
				}
				
			});
		}
	}

	public void UISetBackboneColor() {
		if (_vp.isModifiable()) {
			showColorDialog("Choose new backbone color", _vp.getBackboneColor(), new Runnable() {

				@Override
				public void run() {
					if (dialogReturnValue != null) {
						_vp.setBackboneColor((Color) dialogReturnValue);
						_vp.repaint();
					}
				}
				
			});
		}
	}

	public void UISetTitleFont() {
		if (_vp.isModifiable()) {
			final VueFont font = new VueFont(_vp);
			Runnable ok = new Runnable() {

				@Override
				public void run() {
					_vp.setTitleFont(font.getFont());
					_vp.repaint();
				}

			};
			showConfirmDialog(font.getPanel(), "New Title font", ok, null);
		}
	}

	public void UISetSpaceBetweenBases() {
		if (_vp.isModifiable()) {

			final VueSpaceBetweenBases vsbb = new VueSpaceBetweenBases(_vp);
			final Double oldSpace = _vp.getSpaceBetweenBases();
			Runnable cancel = new Runnable () {

				@Override
				public void run() {
					_vp.setSpaceBetweenBases(oldSpace);
					_vp.drawRNA(_vp.getRNA());
					_vp.repaint();
				}
				
			};
			showConfirmDialog(vsbb.getPanel(), "Set the space between each base", null, cancel, cancel);			
		}
	}

	public void UISetBPHeightIncrement() {
		if (_vp.isModifiable()) {

			VueBPHeightIncrement v = new VueBPHeightIncrement(_vp);
			final Double oldSpace = _vp.getBPHeightIncrement();
			Runnable cancel = new Runnable() {

				@Override
				public void run() {
					_vp.setBPHeightIncrement(oldSpace);
					_vp.drawRNA(_vp.getRNA());
					_vp.repaint();
				}
				
			};
			showConfirmDialog(v.getPanel(), "Set the vertical increment in linear mode", null, cancel, cancel);
		}
	}

	public void UISetNumPeriod() {
		if (_vp.getRNA().get_listeBases().size() != 0) {
			final int oldNumPeriod = _vp.getNumPeriod();
			VueNumPeriod vnp = new VueNumPeriod(_vp);
			Runnable cancel = new Runnable() {

				@Override
				public void run() {
					_vp.setNumPeriod(oldNumPeriod);
					_vp.repaint();
				}
				
			};
			showConfirmDialog(vnp.getPanel(), "Set new numbering period", null, cancel, cancel);
		}
	}

	public void UIEditBasePair() {
		if (_vp.isModifiable()) {
			ModeleBase mb = _vp.getRNA().get_listeBases().get(_vp.getNearestBase());
			if (mb.getElementStructure() != -1) {
				final ModeleBP msbp = mb.getStyleBP();
				final ModeleBP.Edge bck5 = msbp.getEdgePartner5();
				final ModeleBP.Edge bck3 = msbp.getEdgePartner3();
				final ModeleBP.Stericity bcks = msbp.getStericity();

				VueBPType vbpt = new VueBPType(_vp, msbp);
				Runnable cancel = new Runnable() {

					@Override
					public void run() {
						msbp.setEdge5(bck5);
						msbp.setEdge3(bck3);
						msbp.setStericity(bcks);
						_vp.repaint();
					}
					
				};
				showConfirmDialog(vbpt.getPanel(), "Set base pair L/W type", null, cancel, cancel);
			}
		}
	}

	public void UIColorBasePair() {
		if (_vp.isModifiable()) {
			ModeleBase mb = _vp.getRNA().get_listeBases().get(_vp.getNearestBase());
			if (mb.getElementStructure() != -1) {
				final ModeleBP msbp = mb.getStyleBP();
				showColorDialog("Choose custom base pair color",
						msbp.getStyle().getColor(_vp.getConfig()._bondColor), new Runnable() {

					@Override
					public void run() {
						if (dialogReturnValue != null) {
							msbp.getStyle().setCustomColor((Color) dialogReturnValue);
							_vp.repaint();
						}
					}
					
				});
			}
		}
	}

	public void UIThicknessBasePair() {
		if (_vp.isModifiable()) {
			ModeleBase mb = _vp.getRNA().get_listeBases()
					.get(_vp.getNearestBase());
			if (mb.getElementStructure() != -1) {
				ModeleBP msbp = mb.getStyleBP();
				ArrayList<ModeleBP> bases = new ArrayList<ModeleBP>();
				bases.add(msbp);
				UIThicknessBasePairs(bases);
			}
		}
	}

	public void saveToPNG(final String filename) {
		final VueJPEG jpeg = new VueJPEG(true, false);
		Runnable ok = new Runnable() {

			@Override
			public void run() {
				Double scale = jpeg.getScaleSlider().getValue() / 100.0;
				BufferedImage myImage = new BufferedImage((int) Math.round(_vp.getWidth() * scale),
						(int) Math.round(_vp.getHeight() * scale), BufferedImage.TYPE_INT_ARGB);
				// BH j2s SwingJS: was BufferedImage.TRANSLUCENT, which is TYPE_INT_ARGB_PRE ?? no transparent background?
				Graphics2D g2 = myImage.createGraphics();
				AffineTransform AF = new AffineTransform();
				AF.setToScale(scale, scale);
				g2.setTransform(AF);
				_vp.paintComponent(g2, !_vp.getConfig()._drawBackground);
				g2.dispose();
				try {
					ImageIO.write(myImage, "PNG", new File(filename));
				} catch (IOException e) {
					// cannot throw an exception from Runnable.run()
					_vp.errorDialog(new ExceptionExportFailed(e.getMessage(), filename));
				}
			}

		};
		showConfirmDialog(jpeg.getPanel(), "Set resolution", ok, null);
	}

	public void saveToJPEG(final String filename) {
		final VueJPEG jpeg = new VueJPEG(true, true);
		Runnable ok = new Runnable() {

			@Override
			public void run() {
				Double scale;
				if (jpeg.getScaleSlider().getValue() == 0)
					scale = 1. / 100.;
				else
					scale = jpeg.getScaleSlider().getValue() / 100.;
			BufferedImage myImage = new BufferedImage((int) Math.round(_vp
					.getWidth() * scale), (int) Math.round(_vp.getHeight()
					* scale), BufferedImage.TYPE_INT_RGB);
				Graphics2D g2 = myImage.createGraphics();
				AffineTransform AF = new AffineTransform();
				AF.setToScale(scale, scale);
				g2.setTransform(AF);
				_vp.paintComponent(g2);
				try {
				FileImageOutputStream out = new FileImageOutputStream(new File(
						filename));
				ImageWriter writer = ImageIO
						.getImageWritersByFormatName("jpeg").next();
					ImageWriteParam params = writer.getDefaultWriteParam();
					params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					params.setCompressionQuality(jpeg.getQualitySlider().getValue() / 100.0f);
					writer.setOutput(out);
					IIOImage myIIOImage = new IIOImage(myImage, null, null);
					writer.write(null, myIIOImage, params);
					out.close();
				} catch (IOException e) {
					// cannot throw an exception from Runnable.run()
					_vp.errorDialog(new ExceptionExportFailed(e.getMessage(), filename));
				}
			}

		};
		showConfirmDialog(jpeg.getPanel(), "Set resolution/quality", ok, null);
	}

	public void UIToggleShowNCBP() {
		if (_vp.isModifiable()) {
			_vp.setShowNonCanonicalBP(!_vp.getShowNonCanonicalBP());
			_vp.repaint();
		}
	}

	public void UIToggleColorSpecialBases() {
		_vp.setColorNonStandardBases(!_vp.getColorSpecialBases());
		_vp.repaint();
	}

	public void UIToggleColorGapsBases() {
		_vp.setColorGapsBases(!_vp.getColorGapsBases());
		_vp.repaint();
	}

	public void UIToggleShowNonPlanar() {
		if (_vp.isModifiable()) {
			_vp.setShowNonPlanarBP(!_vp.getShowNonPlanarBP());
			_vp.repaint();
		}
	}

	public void UIToggleShowWarnings() {
		_vp.setShowWarnings(!_vp.getShowWarnings());
		_vp.repaint();
	}

	public void UIPickSpecialBasesColor() {
		showColorDialog("Choose new special bases color", _vp.getNonStandardBasesColor(), new Runnable() {

			@Override
			public void run() {
				if (dialogReturnValue != null) {
					_vp.setNonStandardBasesColor((Color) dialogReturnValue);
					_vp.setColorNonStandardBases(true);
					_vp.repaint();
				}
			}
			
		});
	}

	public void UIPickGapsBasesColor() {
		showColorDialog("Choose new gaps bases color", _vp.getGapsBasesColor(), new Runnable() {

			@Override
			public void run() {
				if (dialogReturnValue != null) {
					_vp.setGapsBasesColor((Color) dialogReturnValue);
					_vp.setColorGapsBases(true);
					_vp.repaint();
				}
			}
			
		});
	}

	public void UIBaseTypeColor() {
		if (_vp.isModifiable()) {
			new VueBases(_vp, VueBases.KIND_MODE);
		}
	}

	public void UIToggleModifiable() {
		_vp.setModifiable(!_vp.isModifiable());
	}

	public void UIBasePairTypeColor() {
		if (_vp.isModifiable()) {
			new VueBases(_vp, VueBases.COUPLE_MODE);
		}
	}

	public void UIBaseAllColor() {
		if (_vp.isModifiable()) {
			new VueBases(_vp, VueBases.ALL_MODE);
		}
	}

	public void UIAbout() {
		final VueAboutPanel about = new VueAboutPanel();
		Runnable ok = new Runnable() {

			@Override
			public void run() {
				about.gracefulStop();
			}
			
		};
		showMessageDialog(about, "About VARNA " + VARNAConfig.MAJOR_VERSION + "." + VARNAConfig.MINOR_VERSION,
				JOptionPane.PLAIN_MESSAGE, ok, ok);
	}

	public void UIAutoAnnotateHelices() {
		if (_vp.isModifiable()) {
			_vp.getRNA().autoAnnotateHelices();
			_vp.repaint();
		}
	}

	public void UIAutoAnnotateStrandEnds() {
		if (_vp.isModifiable()) {
			_vp.getRNA().autoAnnotateStrandEnds();
			_vp.repaint();
		}
	}

	public void UIAutoAnnotateInteriorLoops() {
		if (_vp.isModifiable()) {
			_vp.getRNA().autoAnnotateInteriorLoops();
			_vp.repaint();
		}
	}

	public void UIAutoAnnotateTerminalLoops() {
		if (_vp.isModifiable()) {
			_vp.getRNA().autoAnnotateTerminalLoops();
			_vp.repaint();
		}
	}

	public void UIAnnotationRemoveFromAnnotation(TextAnnotation textAnnotation) {
		if (_vp.isModifiable()) {
			_vp.set_selectedAnnotation(null);
			_vp.getListeAnnotations().remove(textAnnotation);
			_vp.repaint();
		}
	}

	public void UIAnnotationEditFromAnnotation(TextAnnotation textAnnotation) {
		VueAnnotation vue;
		if (textAnnotation.getType() == TextAnnotation.AnchorType.POSITION)
			vue = new VueAnnotation(_vp, textAnnotation, false);
		else
			vue = new VueAnnotation(_vp, textAnnotation, true, false);
		vue.show();
	}

	public void UIAnnotationAddFromStructure(TextAnnotation.AnchorType type, ArrayList<Integer> listeIndex)
			throws Exception {
		TextAnnotation textAnnot;
		ArrayList<ModeleBase> listeBase;
		VueAnnotation vue;
		switch (type) {
		case BASE:
			textAnnot = new TextAnnotation("", _vp.getRNA().get_listeBases()
					.get(listeIndex.get(0)));
			vue = new VueAnnotation(_vp, textAnnot, true);
			vue.show();
			break;
		case LOOP:
			listeBase = new ArrayList<ModeleBase>();
			for (Integer i : listeIndex) {
				listeBase.add(_vp.getRNA().get_listeBases().get(i));
			}
			textAnnot = new TextAnnotation("", listeBase, type);
			vue = new VueAnnotation(_vp, textAnnot, true);
			vue.show();
			break;
		case HELIX:
			listeBase = new ArrayList<ModeleBase>();
			for (Integer i : listeIndex) {
				listeBase.add(_vp.getRNA().get_listeBases().get(i));
			}
			textAnnot = new TextAnnotation("", listeBase, type);
			vue = new VueAnnotation(_vp, textAnnot, true);
			vue.show();
			break;
		default:
			_vp.errorDialog(new Exception("Unknown structure type"));
			break;
		}
	}

	public void UIAnnotationEditFromStructure(TextAnnotation.AnchorType type,
			ArrayList<Integer> listeIndex) {
		if (_vp.isModifiable()) {
			ModeleBase mb = _vp.getRNA().get_listeBases()
					.get(listeIndex.get(0));
			TextAnnotation ta = _vp.getRNA().getAnnotation(type, mb);
			if (ta != null)
				UIAnnotationEditFromAnnotation(ta);
		}
	}

	public void UIAnnotationRemoveFromStructure(TextAnnotation.AnchorType type,
			ArrayList<Integer> listeIndex) {
		if (_vp.isModifiable()) {
			ModeleBase mb = _vp.getRNA().get_listeBases()
					.get(listeIndex.get(0));
			TextAnnotation ta = _vp.getRNA().getAnnotation(type, mb);
			if (ta != null)
				UIAnnotationRemoveFromAnnotation(ta);
		}
	}

	public void UIAnnotationsAddPosition(int x, int y) {
		if (_vp.isModifiable()) {
			Point2D.Double p = _vp.panelToLogicPoint(new Point2D.Double(x, y));
			VueAnnotation annotationAdd = new VueAnnotation(_vp, (int) p.x,
					(int) p.y);
			annotationAdd.show();
		}
	}

	public void UIAnnotationsAddBase(int x, int y) {
		if (_vp.isModifiable()) {
			ModeleBase mb = _vp.getBaseAt(new Point2D.Double(x, y));
			if (mb != null) {
				_vp.highlightSelectedBase(mb);
				TextAnnotation textAnnot = new TextAnnotation("", mb);
				VueAnnotation annotationAdd = new VueAnnotation(_vp, textAnnot,
						true);
				annotationAdd.show();
			}
		}
	}

	public void UIAnnotationsAddLoop(int x, int y) {
		if (_vp.isModifiable()) {
			try {
				ModeleBase mb = _vp.getBaseAt(new Point2D.Double(x, y));
				if (mb != null) {
					Vector<Integer> v = _vp.getRNA()
							.getLoopBases(mb.getIndex());
					ArrayList<ModeleBase> mbs = _vp.getRNA().getBasesAt(v);
					TextAnnotation textAnnot;
					textAnnot = new TextAnnotation("", mbs,
							TextAnnotation.AnchorType.LOOP);
					_vp.setSelection(mbs);
					VueAnnotation annotationAdd = new VueAnnotation(_vp,
							textAnnot, true);
					annotationAdd.show();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private ArrayList<ModeleBase> extractMaxContiguousPortion(
			ArrayList<ModeleBase> m) {
		ModeleBase[] tab = new ModeleBase[_vp.getRNA().getSize()];
		for (int i = 0; i < tab.length; i++) {
			tab[i] = null;
		}
		for (ModeleBase mb : m) {
			tab[mb.getIndex()] = mb;
		}
		ArrayList<ModeleBase> best = new ArrayList<ModeleBase>();
		ArrayList<ModeleBase> current = new ArrayList<ModeleBase>();
		for (int i = 0; i < tab.length; i++) {
			if (tab[i] != null) {
				current.add(tab[i]);
			} else {
				if (current.size() > best.size())
					best = current;
				current = new ArrayList<ModeleBase>();
			}
		}
		if (current.size() > best.size()) {
			best = current;
		}
		return best;
	}

	public void UIAnnotationsAddRegion(int x, int y) {
		if (_vp.isModifiable()) {
			ArrayList<ModeleBase> mb = _vp.getSelection().getBases();
			if (mb.size() == 0) {
				ModeleBase m = _vp.getBaseAt(new Point2D.Double(x, y));
				mb.add(m);
			}
			mb = extractMaxContiguousPortion(extractMaxContiguousPortion(mb));
			_vp.setSelection(mb);
			HighlightRegionAnnotation regionAnnot = new HighlightRegionAnnotation(
					mb);
			_vp.addHighlightRegion(regionAnnot);
			VueHighlightRegionEdit annotationAdd = new VueHighlightRegionEdit(
					_vp, regionAnnot);
			if (!annotationAdd.show()) {
				_vp.removeHighlightRegion(regionAnnot);
			}
			_vp.clearSelection();
		}
	}

	public void UIAnnotationsAddChemProb(int x, int y) {
		if (_vp.isModifiable() && _vp.getRNA().getSize() > 1) {
			Point2D.Double p = _vp.panelToLogicPoint(new Point2D.Double(x, y));
			ModeleBase m1 = _vp.getBaseAt(new Point2D.Double(x, y));
			ModeleBase best = null;
			if (m1.getIndex() - 1 >= 0) {
				best = _vp.getRNA().getBaseAt(m1.getIndex() - 1);
			}
			if (m1.getIndex() + 1 < _vp.getRNA().getSize()) {
				ModeleBase m2 = _vp.getRNA().getBaseAt(m1.getIndex() + 1);
				if (best == null) {
					best = m2;
				} else {
					if (best.getCoords().distance(p) > m2.getCoords().distance(
							p)) {
						best = m2;
					}
				}
			}
			ArrayList<ModeleBase> tab = new ArrayList<ModeleBase>();
			tab.add(m1);
			tab.add(best);
			_vp.setSelection(tab);
			ChemProbAnnotation regionAnnot = new ChemProbAnnotation(m1, best);
			_vp.getRNA().addChemProbAnnotation(regionAnnot);
			VueChemProbAnnotation annotationAdd = new VueChemProbAnnotation(
					_vp, regionAnnot);
			if (!annotationAdd.show()) {
				_vp.getRNA().removeChemProbAnnotation(regionAnnot);
			}
			_vp.clearSelection();
		}
	}

	public void UIAnnotationsAddHelix(int x, int y) {
		if (_vp.isModifiable()) {
			try {
				ModeleBase mb = _vp.getBaseAt(new Point2D.Double(x, y));
				if (mb != null) {
					ArrayList<Integer> v = _vp.getRNA().findHelix(mb.getIndex());
					ArrayList<ModeleBase> mbs = _vp.getRNA().getBasesAt(v);
					TextAnnotation textAnnot;
					textAnnot = new TextAnnotation("", mbs,
							TextAnnotation.AnchorType.HELIX);
					_vp.setSelection(mbs);
					VueAnnotation annotationAdd = new VueAnnotation(_vp,
							textAnnot, true);
					annotationAdd.show();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void UIToggleGaspinMode() {
		if (_vp.isModifiable()) {
			_vp.toggleDrawOutlineBases();
			_vp.toggleFillBases();
			_vp.repaint();
		}
	}

	public void UIAnnotationsAdd() {
		if (_vp.isModifiable()) {
			VueAnnotation annotationAdd = new VueAnnotation(_vp);
			annotationAdd.show();
		}
	}

	public void UIEditAllBasePairs() {
		if (_vp.isModifiable()) {
			new VueBPList(_vp);
		}
	}

	public void UIEditAllBases() {
		if (_vp.isModifiable()) {
			new VueBases(_vp, VueBases.ALL_MODE);
		}
	}

	public void UIAnnotationsRemove() {
		if (_vp.isModifiable()) {
			new VueListeAnnotations(_vp, VueListeAnnotations.REMOVE);
		}
	}

	public void UIAnnotationsEdit() {
		if (_vp.isModifiable()) {
			new VueListeAnnotations(_vp, VueListeAnnotations.EDIT);
		}
	}

	public void UIAddBP(int i, int j, ModeleBP ms) {
		if (_vp.isModifiable()) {
			_vp.getRNA().addBP(i, j, ms);
			_undoableEditSupport.postEdit(new VARNAEdits.AddBPEdit(i, j, ms,
					_vp));
			_vp.repaint();

			HashSet<ModeleBP> tmp = new HashSet<ModeleBP>();
			tmp.add(ms);
			_vp.fireStructureChanged(new HashSet<ModeleBP>(_vp.getRNA()
					.getAllBPs()), tmp, new HashSet<ModeleBP>());
		}
	}

	public void UIRemoveBP(ModeleBP ms) {
		if (_vp.isModifiable()) {
			_undoableEditSupport.postEdit(new VARNAEdits.RemoveBPEdit(ms
					.getIndex5(), ms.getIndex3(), ms, _vp));
			_vp.getRNA().removeBP(ms);
			_vp.repaint();

			HashSet<ModeleBP> tmp = new HashSet<ModeleBP>();
			tmp.add(ms);
			_vp.fireStructureChanged(new HashSet<ModeleBP>(_vp.getRNA()
					.getAllBPs()), new HashSet<ModeleBP>(), tmp);
		}
	}

	public void UIShiftBaseCoord(ArrayList<Integer> indices, double dx, double dy) {
		if (_vp.isModifiable()) {
			Hashtable<Integer, Point2D.Double> backupPos = new Hashtable<Integer, Point2D.Double>();

			for (int index : indices) {
				ModeleBase mb = _vp.getRNA().getBaseAt(index);
				Point2D.Double d = mb.getCoords();
				backupPos.put(index, d);
				_vp.getRNA().setCoord(index, d.x + dx, d.y + dy);
				_vp.getRNA().setCenter(index, mb.getCenter().x + dx, mb.getCenter().y + dy);
			}
			_undoableEditSupport.postEdit(new VARNAEdits.BasesShiftEdit(
					indices, dx, dy, _vp));
			_vp.repaint();
			_vp.fireLayoutChanged(backupPos);
		}
	}

	public void UIShiftBaseCoord(ArrayList<Integer> indices, Point2D.Double dv) {
		UIShiftBaseCoord(indices, dv.x, dv.y);
	}

	public void UIMoveSingleBase(int index, double nx, double ny) {
		if (_vp.isModifiable()) {
			ModeleBase mb = _vp.getRNA().getBaseAt(index);
			Point2D.Double d = mb.getCoords();
			Hashtable<Integer, Point2D.Double> backupPos = new Hashtable<Integer, Point2D.Double>();
			backupPos.put(index, d);
			_undoableEditSupport.postEdit(new VARNAEdits.SingleBaseMoveEdit(
					index, nx, ny, _vp));
			_vp.getRNA().setCoord(index, nx, ny);
			_vp.repaint();
			_vp.fireLayoutChanged(backupPos);
		}
	}

	public void UIMoveSingleBase(int index, Point2D.Double dv) {
		UIMoveSingleBase(index, dv.x, dv.y);
	}

	public void UISetBaseCenter(int index, double x, double y) {
		UISetBaseCenter(index, new Point2D.Double(x, y));
	}

	public void UISetBaseCenter(int index, Point2D.Double p) {
		if (_vp.isModifiable()) {
			_vp.getRNA().setCenter(index, p);
		}
	}

	public void UIUndo() {
		_vp.undo();
	}

	public void UIRedo() {
		_vp.redo();
	}

	/**
	 * Move a helix of the rna
	 * 
	 * @param index
	 *            :the index of the selected base
	 * @param newPos
	 *            :the new xy coordinate, within the logical system of
	 *            coordinates
	 */
	public void UIMoveHelixAtom(int index, Point2D.Double newPos) {
		if (_vp.isModifiable() && (index >= 0)
				&& (index < _vp.getRNA().get_listeBases().size())) {
			int indexTo = _vp.getRNA().get_listeBases().get(index)
					.getElementStructure();
			Point h = _vp.getRNA().getHelixInterval(index);
			Point ml = _vp.getRNA().getMultiLoop(h.x);
			int i = ml.x;
			if (indexTo != -1) {
				if (i == 0) {
					if (shouldFlip(index, newPos)) {
						UIFlipHelix(h);
						_undoableEditSupport
								.postEdit(new VARNAEdits.HelixFlipEdit(h, _vp));
					}
				} else {
					UIRotateHelixAtom(index, newPos);
				}

			}
			_vp.fireLayoutChanged();
		}
	}

	/**
	 * Flip an helix around its supporting base
	 */
	public void UIFlipHelix(Point h) {
		int hBeg = h.x;
		int hEnd = h.y;
		Point2D.Double A = _vp.getRNA().getCoords(hBeg);
		Point2D.Double B = _vp.getRNA().getCoords(hEnd);
		Point2D.Double AB = new Point2D.Double(B.x - A.x, B.y - A.y);
		double normAB = Math.sqrt(AB.x * AB.x + AB.y * AB.y);
		// Creating a coordinate system centered on A and having
		// unit x-vector Ox.
		Point2D.Double O = A;
		Point2D.Double Ox = new Point2D.Double(AB.x / normAB, AB.y / normAB);
		Hashtable<Integer, Point2D.Double> old = new Hashtable<Integer, Point2D.Double>();
		for (int i = hBeg + 1; i < hEnd; i++) {
			Point2D.Double P = _vp.getRNA().getCoords(i);
			Point2D.Double nP = RNA.project(O, Ox, P);
			old.put(i, nP);
		}
		_vp.getRNA().flipHelix(h);
		_vp.fireLayoutChanged(old);
	}

	/**
	 * Tests if an helix needs to be flipped.
	 */
	boolean shouldFlip(int index, Point2D.Double P) {
		Point h = _vp.getRNA().getHelixInterval(index);

		Point2D.Double A = _vp.getRNA().getCoords(h.x);
		Point2D.Double B = _vp.getRNA().getCoords(h.y);
		Point2D.Double C = _vp.getRNA().getCoords(h.x + 1);
		// Creating a vector that is orthogonal to AB
		Point2D.Double hAB = new Point2D.Double(B.y - A.y, -(B.x - A.x));
		Point2D.Double AC = new Point2D.Double(C.x - A.x, C.y - A.y);
		Point2D.Double AP = new Point2D.Double(P.x - A.x, P.y - A.y);
		double signC = (hAB.x * AC.x + hAB.y * AC.y);
		double signP = (hAB.x * AP.x + hAB.y * AP.y);
		// Now, the product signC*signP is negative iff the mouse and the first
		// base inside
		// the helix are on different sides of the end of the helix => Flip the
		// helix!
		return (signC * signP < 0.0);
	}

	public void UIRotateHelixAtom(int index, Point2D.Double newPos) {
		Point h = _vp.getRNA().getHelixInterval(index);
		Point ml = _vp.getRNA().getMultiLoop(h.x);
		int i = ml.x;
		int prevIndex = h.x;
		int nextIndex = h.y;
		while (i <= ml.y) {
			int j = _vp.getRNA().get_listeBases().get(i).getElementStructure();
			if ((j != -1) && (i < h.x)) {
				prevIndex = i;
			}
			if ((j != -1) && (i > h.y) && (nextIndex == h.y)) {
				nextIndex = i;
			}
			if ((j > i) && (j < ml.y)) {
				i = _vp.getRNA().get_listeBases().get(i).getElementStructure();
			} else {
				i++;
			}
		}
		Point2D.Double oldPos = _vp.getRNA().getCoords(index);
		Point2D.Double limitLoopLeft, limitLoopRight, limitLeft, limitRight, helixStart, helixStop;
		boolean isDirect = _vp.getRNA().testDirectionality(ml.x, ml.y, h.x);
		if (isDirect) {
			limitLoopLeft = _vp.getRNA().getCoords(ml.y);
			limitLoopRight = _vp.getRNA().getCoords(ml.x);
			limitLeft = _vp.getRNA().getCoords(prevIndex);
			limitRight = _vp.getRNA().getCoords(nextIndex);
			helixStart = _vp.getRNA().getCoords(h.x);
			helixStop = _vp.getRNA().getCoords(h.y);
		} else {
			limitLoopLeft = _vp.getRNA().getCoords(ml.x);
			limitLoopRight = _vp.getRNA().getCoords(ml.y);
			limitLeft = _vp.getRNA().getCoords(nextIndex);
			limitRight = _vp.getRNA().getCoords(prevIndex);
			helixStart = _vp.getRNA().getCoords(h.y);
			helixStop = _vp.getRNA().getCoords(h.x);
		}

		Point2D.Double center = _vp.getRNA().get_listeBases().get(h.x)
				.getCenter();
		double base = (RNA.computeAngle(center, limitLoopRight) + RNA
				.computeAngle(center, limitLoopLeft)) / 2.0;
		double pLimR = RNA.computeAngle(center, limitLeft) - base;
		double pHelR = RNA.computeAngle(center, helixStart) - base;
		double pNew = RNA.computeAngle(center, newPos) - base;
		double pOld = RNA.computeAngle(center, oldPos) - base;
		double pHelL = RNA.computeAngle(center, helixStop) - base;
		double pLimL = RNA.computeAngle(center, limitRight) - base;

		while (pLimR < 0.0)
			pLimR += 2.0 * Math.PI;
		while (pHelR < pLimR)
			pHelR += 2.0 * Math.PI;
		while ((pNew < pHelR))
			pNew += 2.0 * Math.PI;
		while ((pOld < pHelR))
			pOld += 2.0 * Math.PI;
		while ((pHelL < pOld))
			pHelL += 2.0 * Math.PI;
		while ((pLimL < pHelL))
			pLimL += 2.0 * Math.PI;

		double minDelta = normalizeAngle((pLimR - pHelR) + 0.25);
		double maxDelta = normalizeAngle((pLimL - pHelL) - 0.25);
		while (maxDelta < minDelta)
			maxDelta += 2.0 * Math.PI;
		double delta = normalizeAngle(pNew - pOld);
		while (delta < minDelta)
			delta += 2.0 * Math.PI;

		if (delta > maxDelta) {
			double distanceMax = delta - maxDelta;
			double distanceMin = minDelta - (delta - 2.0 * Math.PI);
			if (distanceMin < distanceMax) {
				delta = minDelta;
			} else {
				delta = maxDelta;
			}
		}
		double corrected = RNA
				.correctHysteresis((delta + base + (pHelR + pHelL) / 2.));
		delta = corrected - (base + (pHelR + pHelL) / 2.);
		_undoableEditSupport.postEdit(new VARNAEdits.HelixRotateEdit(delta,
				base, pLimL, pLimR, h, ml, _vp));
		UIRotateEverything(delta, base, pLimL, pLimR, h, ml);
	}

	public void UIRotateEverything(double delta, double base, double pLimL,
			double pLimR, Point h, Point ml) {
		Hashtable<Integer, Point2D.Double> backupPos = new Hashtable<Integer, Point2D.Double>();
		_vp.getRNA().rotateEverything(delta, base, pLimL, pLimR, h, ml,
				backupPos);
		_vp.fireLayoutChanged(backupPos);
	}

	private double normalizeAngle(double angle) {
		return normalizeAngle(angle, 0.0);
	}

	private double normalizeAngle(double angle, double base) {
		while (angle < base) {
			angle += 2.0 * Math.PI;
		}
		while (angle >= (2.0 * Math.PI) - base) {
			angle -= 2.0 * Math.PI;
		}
		return angle;
	}

	public void UIThicknessBasePairs(ArrayList<ModeleBP> bases) {
		final VueBPThickness vbpt = new VueBPThickness(_vp, bases);
		Runnable cancel = new Runnable() {

			@Override
			public void run() {
				vbpt.restoreThicknesses();
				_vp.repaint();
			}
			
		};
		showConfirmDialog(vbpt.getPanel(), "Set base pair(s) thickness", null, cancel, cancel);
	}
	
	

}
