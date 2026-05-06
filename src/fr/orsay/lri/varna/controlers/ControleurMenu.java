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
package fr.orsay.lri.varna.controlers;


import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;

import fr.orsay.lri.varna.components.VARNAPanel;
import fr.orsay.lri.varna.exceptions.ExceptionExportFailed;
import fr.orsay.lri.varna.exceptions.ExceptionWritingForbidden;
import fr.orsay.lri.varna.interfaces.InterfaceVARNAListener;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.annotations.TextAnnotation;
import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.RNA;
import fr.orsay.lri.varna.views.VueBPThickness;
import fr.orsay.lri.varna.views.VueMenu;

/**
 * This listener controls menu items
 * 
 * @author darty
 * 
 */
public class ControleurMenu implements InterfaceVARNAListener,
		ActionListener {

	private VARNAPanel _vp;
	@SuppressWarnings("unused")
	private VueMenu _vm;
	private String _type;
	private String _color;
	private Object _source;

	/**
	 * Creates the menu listener
	 * 
	 * @param varnaPanel
	 *            The VARNAPanel
	 * @param vueMenu 
	 */
	public ControleurMenu(VARNAPanel varnaPanel, VueMenu vueMenu) {
		_vp = varnaPanel;
		_vm = vueMenu;
	  _vp.getRNA().addVARNAListener(this);
	}

	@Override
  public void actionPerformed(ActionEvent e) {
		String[] temp = e.getActionCommand().split(",");
		_source = e.getSource();
		_type = temp[0];
		if (temp.length > 1)
			_color = temp[1];
		else
			_color = "";
		// selon l'option choisie dans le menu:
		if (!optionRedraw())
			if (!optionExport())
				if (!optionImport())
					if (!optionRNADisplay())
						if (!optionTitle())
							if (!optionColorMap())
								if (!optionView())
									if (!optionBase())
										if (!optionBasePair())
											if (!optionLoop())
												if (!option3prime())
													if (!option5prime())
														if (!optionHelix())
															if (!optionStem())
																if (!optionBulge())
																	if (!optionAnnotation())
																		if (!optionEditRNA())
																			_vp.errorDialog(new Exception("Uknown action command '"+_type+"'"));
	}
	
	private boolean optionEditRNA()
	{
		if (_type.equals("editallbps")) {
			_vp.getVARNAUI().editAllBasePairs();
		}
		else if (_type.equals("editallbases")) {
			_vp.getVARNAUI().editAllBases();
		}
		else return false;
		return true;
	}

  private boolean optionAnnotation() {
    if (!_type.contains("annotation"))
      return false;
    // a partir du menu principale (gestion des annotations)
    if (_type.equals("annotationsaddPosition")) {
      _vp.getVARNAUI().annotationsAddPosition(
          _vp.getPopup().getSpawnPoint().x, _vp.getPopup().getSpawnPoint().y);
    } else if (_type.equals("annotationsaddBase")) {
      _vp.getVARNAUI().annotationsAddBase(_vp.getPopup().getSpawnPoint().x,
          _vp.getPopup().getSpawnPoint().y);
    } else if (_type.equals("annotationsaddLoop")) {
      _vp.getVARNAUI().annotationsAddLoop(_vp.getPopup().getSpawnPoint().x,
          _vp.getPopup().getSpawnPoint().y);
    } else if (_type.equals("annotationsaddChemProb")) {
      _vp.getVARNAUI().annotationsAddChemProb(
          _vp.getPopup().getSpawnPoint().x, _vp.getPopup().getSpawnPoint().y);
    } else if (_type.equals("annotationsaddRegion")) {
      _vp.getVARNAUI().annotationsAddRegion(_vp.getPopup().getSpawnPoint().x,
          _vp.getPopup().getSpawnPoint().y);
    } else if (_type.equals("annotationsaddHelix")) {
      _vp.getVARNAUI().annotationsAddHelix(_vp.getPopup().getSpawnPoint().x,
          _vp.getPopup().getSpawnPoint().y);
    } else if (_type.equals("annotationsautohelices")) {
      _vp.getVARNAUI().autoAnnotateHelices();
    } else if (_type.equals("annotationsautointerior")) {
      _vp.getVARNAUI().autoAnnotateInteriorLoops();
    } else if (_type.equals("annotationsautoterminal")) {
      _vp.getVARNAUI().autoAnnotateTerminalLoops();
    } else if (_type.equals("annotationsautohelices")) {
      _vp.getVARNAUI().autoAnnotateHelices();
    } else if (_type.equals("annotationsremove")) {
      _vp.getVARNAUI().annotationsRemove();
    } else if (_type.equals("annotationsautoextremites")) {
      _vp.getVARNAUI().autoAnnotateStrandEnds();
    } else if (_type.equals("annotationsedit")) {
      _vp.getVARNAUI().annotationsEdit();
      // a partir du menu selection (annotation la plus proche)
    } else if (_type.equals("Selectionannotationremove")) {
      _vp.getVARNAUI()
          .annotationRemoveFromAnnotation(_vp.getSelectedAnnotation());
    } else if (_type.equals("Selectionannotationedit")) {
      _vp.getVARNAUI()
          .annotationEditFromAnnotation(_vp.getSelectedAnnotation());

      // a partir d'une structure(base, loop, helix) dans l'arn
      // (annotation li� a la structure)
    } else if (_type.endsWith("annotationadd")
        || _type.contains("annotationremove")
        || _type.contains("annotationedit")) {
      try {
        TextAnnotation.AnchorType type = trouverAncrage();
        ArrayList<Integer> listeIndex = new ArrayList<Integer>();
        switch (type) {
        case BASE:
          listeIndex.add(Integer.valueOf(_vp.getNearestBase()));
          break;/// BH CHECK was missing break
        case LOOP:
          if (_type.startsWith("loop1"))
            listeIndex = _vp.getRNA().findLoopForward(_vp.getNearestBase());
          else if (_type.startsWith("loop2"))
            listeIndex = _vp.getRNA().findLoopBackward(_vp.getNearestBase());
          else
            listeIndex = _vp.getRNA().findLoop(_vp.getNearestBase());
          break;
        case HELIX:
          listeIndex = _vp.getRNA().findHelix(_vp.getNearestBase());
          break;
        case POSITION:
          break;
        }
        if (_type.endsWith("annotationadd")) {
          _vp.getVARNAUI().annotationAddFromStructure(type, listeIndex);
        } else if (_type.contains("annotationremove")) {
          _vp.getVARNAUI().annotationRemoveFromStructure(trouverAncrage(),
              listeIndex);
        } else if (_type.contains("annotationedit")) {
          _vp.getVARNAUI().annotationEditFromStructure(trouverAncrage(),
              listeIndex);
        }

      } catch (Exception e2) {
        e2.printStackTrace();
      }
    } else
      return false;
    return true;
  }

	private TextAnnotation.AnchorType trouverAncrage() {
		if (_type.contains("loop"))
			return TextAnnotation.AnchorType.LOOP;
		if (_type.contains("helix"))
			return TextAnnotation.AnchorType.HELIX;
		if (_type.contains("base"))
			return TextAnnotation.AnchorType.BASE;
		errorDialog(new Exception("probleme d'identification de l'ancrage"));
		return TextAnnotation.AnchorType.POSITION;
	}

	private boolean option5prime() {
		return colorBases();
	}

	private boolean option3prime() {
		return colorBases();
	}

	private boolean optionBulge() {
		return colorBases();
	}

	private boolean optionStem() {
		return colorBases();
	}

	private boolean optionHelix() {
		return colorBases();
	}

	private boolean colorBases() {
		// System.out.println(_type);
		ArrayList<Integer> listBase = new ArrayList<Integer>();
		String phrase = "Choose new " + _type;
		if (_color.equals("InnerColor")) {
			phrase += " inner color";
			Color c = JColorChooser.showDialog(_vp, phrase,
					VARNAConfig.BASE_INNER_COLOR_DEFAULT);
			if (c != null) {
				listBase = listSwitchType(_type);
				for (int i = 0; i < listBase.size(); i++) {
					_vp.getRNA().get_listeBases().get(listBase.get(i).intValue())
							.getStyleBase().setBaseInnerColor(c);
				}
				_vp.repaint();
			}
		} else if (_color.equals("OutlineColor")) {
			phrase += " outline color";
			Color c = JColorChooser.showDialog(_vp, phrase,
					VARNAConfig.BASE_OUTLINE_COLOR_DEFAULT);
			if (c != null) {
				listBase = listSwitchType(_type);
				for (int i = 0; i < listBase.size(); i++) {
					_vp.getRNA().get_listeBases().get(listBase.get(i).intValue())
							.getStyleBase().setBaseOutlineColor(c);
				}
				_vp.repaint();
			}
		} else if (_color.equals("NameColor")) {
			phrase += " name color";
			Color c = JColorChooser.showDialog(_vp, phrase,
					VARNAConfig.BASE_NAME_COLOR_DEFAULT);
			if (c != null) {
				listBase = listSwitchType(_type);
				for (int i = 0; i < listBase.size(); i++) {
					_vp.getRNA().get_listeBases().get(listBase.get(i).intValue())
							.getStyleBase().setBaseNameColor(c);
				}
				_vp.repaint();
			}
		} else if (_color.equals("NumberColor")) {
			phrase += " number color";
			Color c = JColorChooser.showDialog(_vp, phrase,
					VARNAConfig.BASE_NUMBER_COLOR_DEFAULT);
			if (c != null) {
				listBase = listSwitchType(_type);
				for (int i = 0; i < listBase.size(); i++) {
					_vp.getRNA().get_listeBases().get(listBase.get(i).intValue())
							.getStyleBase().setBaseNumberColor(c);
				}
				_vp.repaint();
			}
		} else if (_color.equals("BPColor")) {
			phrase += " base-pair color";
			Color c = JColorChooser.showDialog(_vp, phrase,
					VARNAConfig.BASE_NUMBER_COLOR_DEFAULT);
			if (c != null) {
				listBase = listSwitchType(_type);
				for (int i = 0; i < listBase.size(); i++) 
				{
					for (ModeleBP msbp:_vp.getRNA().getBPsAt(listBase.get(i).intValue()))
					{
						if (msbp!=null) {
							msbp.getStyle().setCustomColor(c);
						}	
					}
				}
				_vp.repaint();
			}
		} else if (_color.equals("BPColor")) {
			phrase += " base-pair color";
			Color c = JColorChooser.showDialog(_vp, phrase,
					VARNAConfig.BASE_NUMBER_COLOR_DEFAULT);
			if (c != null) {
				listBase = listSwitchType(_type);
				for (int i = 0; i < listBase.size(); i++) {
					ModeleBase mb = _vp.getRNA().get_listeBases().get(
							listBase.get(i).intValue());
					if (mb.getElementStructure() != -1) {
						mb.getStyleBP().getStyle().setCustomColor(c);
					}
				}
				_vp.repaint();
			}
		} else if (_color.equals("BPThickness")) {
			listBase = listSwitchType(_type);
			// System.out.println(listBase.size());
			ArrayList<ModeleBP> styleBPs = new ArrayList<ModeleBP>();
			for (int i = 0; i < listBase.size(); i++) {
				ModeleBase mb = _vp.getRNA().get_listeBases().get(
						listBase.get(i).intValue());
				if (mb.getElementStructure() != -1) {
					styleBPs.add(mb.getStyleBP());
				}
			}
			VueBPThickness vbpt = new VueBPThickness(_vp, styleBPs);
			if (JOptionPane.showConfirmDialog(_vp, vbpt.getPanel(),
					"Set base pair(s) thickness", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
				vbpt.restoreThicknesses();
				_vp.repaint();
			}
		} else
			return false;
		return true;
	}

	private ArrayList<Integer> listSwitchType(String _type) {
		if (_type.equals("helix"))
			return _vp.getRNA().findHelix(_vp.getNearestBase());
		if (_type.equals("current")) {
			return _vp.getSelectionIndices();
		}
		if (_type.equals("allBases")) {
			return _vp.getRNA().findAll();
		}
		if (_type.equals("loop1")) {
			return _vp.getRNA().findLoopForward(_vp.getNearestBase());
		}
		if (_type.equals("loop2")) {
			return _vp.getRNA().findLoopBackward(_vp.getNearestBase());
		}
		if (_type.equals("stem"))
			return _vp.getRNA().findStem(_vp.getNearestBase());
		if (_type.equals("base")) {
			ArrayList<Integer> list = new ArrayList<Integer>();
			list.add(Integer.valueOf(_vp.getNearestBase()));
			return list;
		}
		if (_type.equals("basepair") || _type.equals("bpcolor")
				|| _type.equals("bp")) {
			ArrayList<Integer> list = new ArrayList<Integer>();
			int i = _vp.getNearestBase();
			list.add(Integer.valueOf(i));
			ModeleBase mb = _vp.getRNA().get_listeBases().get(i);
			int j = mb.getElementStructure();
			if (mb.getElementStructure() != -1) {
				list.add(Integer.valueOf(i));
				list.add(Integer.valueOf(j));
			}
			return list;
		}
		switch(_type) {
		case "5'":
      return _vp.getRNA().findNonPairedBaseGroup(_vp.getNearestBase());
		case "3'":
      return _vp.getRNA().findNonPairedBaseGroup(_vp.getNearestBase());
		case "bulge":
      return _vp.getRNA().findNonPairedBaseGroup(_vp.getNearestBase());
		case "all":
      return _vp.getRNA().findAll();		
		}
		return new ArrayList<Integer>();
	}

	private boolean optionLoop() {
		return colorBases();
	}

	private boolean optionBase() {
		if (_type.equals("baseChar")) {
			_vp.getVARNAUI().setBaseCharacter();
			return true;
		}
		return colorBases();
	}

	private boolean optionBasePair() {
		if (_type.equals("basepair")) {
			_vp.getVARNAUI().editBasePair();
			return true;
		} else if (_type.equals("bpcolor")) {
			_vp.getVARNAUI().colorBasePair();
			return true;
		} else if (_type.equals("thickness")) {
			_vp.getVARNAUI().thicknessBasePair();
			return true;
		}
		return false;
	}

	
	
	private boolean optionView() {
		if (_type.equals("background")) {
			_vp.getVARNAUI().setBackground();
		} else if (_type.equals("shownc")) {
			_vp.getVARNAUI().toggleShowNCBP();
		} else if (_type.equals("showbackbone")) {
			_vp.getVARNAUI().toggleDrawBackbone();
		} else if (_type.equals("shownp")) {
			_vp.getVARNAUI().toggleShowNonPlanar();
		} else if (_type.equals("spaceBetweenBases")) {
			_vp.getVARNAUI().setSpaceBetweenBases();
		} else if (_type.equals("bpheightincrement")) {
			_vp.getVARNAUI().setBPHeightIncrement();
		} else if (_type.equals("borderSize")) {
			_vp.getVARNAUI().setBorder();
		} else if (_type.startsWith("zoom")) {
			if (_type.equals("zoom")) {
				_vp.getVARNAUI().customZoom();
			} else {
				String factor = _type.substring("zoom".length());
				double pc = Integer.parseInt(factor);
				pc /= 100.0;
				_vp.setZoom(pc);
				_vp.repaint();
			}
		} else if (_type.equals("rotation")) {
			_vp.getVARNAUI().globalRotation();
		} else if (_type.equals("rescale")) {
			_vp.getVARNAUI().globalRescale();
		} else
			return false;
		return true;
	}

	
	private boolean optionTitle() {
		if (_type.equals("titleDisplay")) {
			_vp.getVARNAUI().setTitleFont();
		} else if (_type.equals("setTitle")) {
			_vp.getVARNAUI().setTitle();
		} else if (_type.equals("titleColor")) {
			_vp.getVARNAUI().setTitleColor();
		} else
			return false;
		return true;
	}

		
	private boolean optionColorMap() {
		if (_type.equals("toggleshowcolormap")) {
			_vp.getVARNAUI().toggleColorMap();
		} else 	if (_type.equals("colormapcaption")) {
			_vp.getVARNAUI().setColorMapCaption();
		} else 	if (_type.equals("colormapstyle")) {
			_vp.getVARNAUI().setColorMapStyle();
		} else 	if (_type.equals("colormaploadvalues")) {
			_vp.getVARNAUI().loadColorMapValues();
		} else 	if (_type.equals("colormapvalues")) {
			_vp.getVARNAUI().setColorMapValues();
		} else
			return false;
		return true;
	}

	private boolean optionRNADisplay() {
		// les options d'affichages generales
		if (_type.equals("gaspin")) {
			_vp.getVARNAUI().toggleGaspinMode();
		} else if (_type.equals("backbone")) {
			_vp.getVARNAUI().setBackboneColor();
		} else if (_type.equals("bonds")) {
			Color c = JColorChooser.showDialog(_vp, "Choose new bonds color",
					_vp.getBackground());
			if (c != null) {
				_vp.setDefaultBPColor(c);
				_vp.repaint();
			}
		} else if (_type.equals("basecolorforBP")) {
			if (_source != null) {
				if (_source instanceof JCheckBoxMenuItem) {
					JCheckBoxMenuItem check = (JCheckBoxMenuItem) _source;
					_vp.setUseBaseColorsForBPs(check.getState());
					_vp.repaint();
				}
			}
		} else if (_type.equals("bpstyle")) {
			_vp.getVARNAUI().setBPStyle();
		} else if (_type.equals("specialbasecolored")) {
			_vp.getVARNAUI().toggleColorSpecialBases();
		} else if (_type.equals("showwarnings")) {
			_vp.getVARNAUI().toggleShowWarnings();
		} else if (_type.equals("dashbasecolored")) {
			_vp.getVARNAUI().toggleColorGapsBases();
		} else if (_type.equals("numPeriod")) {
			_vp.getVARNAUI().setNumPeriod();
		} else if (_type.equals("eachKind")) {
			if (_vp.getRNA().get_listeBases() != null) {
				_vp.getVARNAUI().baseTypeColor();
			} else {
				_vp.emitWarning("No base");
			}
		} else if (_type.equals("eachCouple")) {
			if (_vp.getRNA().get_listeBases() != null
					&& _vp.getRNA().get_listeBases().size() != 0) {
				_vp.getVARNAUI().basePairTypeColor();
			} else {
				_vp.emitWarning("No base");
			}
		} else if (_type.equals("eachBase")) {
			if (_vp.getRNA().get_listeBases() != null
					&& _vp.getRNA().get_listeBases().size() != 0) {
				_vp.getVARNAUI().baseAllColor();
			} else {
				_vp.emitWarning("No base");
			}
		} else if (_type.equals("specialBasesColor")) {
			_vp.getVARNAUI().pickSpecialBasesColor();
		} else if (_type.equals("dashBasesColor")) {
			_vp.getVARNAUI().pickGapsBasesColor();
		} else
			return colorBases();
		return true;
	}

	private boolean optionImport() {
		if (_type.equals("userInput")) {
				_vp.getVARNAUI().manualInput();
		} else if (_type.equals("file")) {
				_vp.getVARNAUI().openFileAsync();
		} else if (_type.equals("print")) {
			_vp.getVARNAUI().print();
		} else if (_type.equals("about")) {
			_vp.getVARNAUI().about();
		} else
			return false;
		return true;
	}

	private boolean optionRedraw() {
		if (_type.equals("reset")) {
			_vp.getVARNAUI().reset();
		} else if (_type.equals("circular")) {
			_vp.getVARNAUI().circular();
		} else if (_type.equals("radiate")) {
			_vp.getVARNAUI().radiate();
		} else if (_type.equals("naview")) {
			_vp.getVARNAUI().aNAView();
		} else if (_type.equals("varnaview")) {
			_vp.getVARNAUI().varnaView();
		} else if (_type.equals("motifview")) {
			_vp.getVARNAUI().motifView();
		} else if (_type.equals("line")) {
			_vp.getVARNAUI().line();
		} else if (_type.equals("flat")) {
			_vp.getVARNAUI().toggleFlatExteriorLoop();
		} else
			return false;
		return true;
	}

	private boolean optionExport() {
		if (_type.equals("saveas")) {
			try {
				_vp.getVARNAUI().saveAs();
			} catch (ExceptionExportFailed e1) {
				errorDialog(e1);
			}
		} else if (_type.equals("dbn")) {
			try {
				_vp.getVARNAUI().saveAsDBN();
			} catch (ExceptionExportFailed e) {
				errorDialog(e);
			}
		} else if (_type.equals("bpseq")) {
			try {
				_vp.getVARNAUI().saveAsBPSEQ();
			} catch (ExceptionExportFailed e) {
				errorDialog(e);
			}
		} else if (_type.equals("ct")) {
			try {
				_vp.getVARNAUI().saveAsCT();
			} catch (ExceptionExportFailed e) {
				errorDialog(e);
			}
		} else if (_type.equals("eps")) {
			try {
				_vp.getVARNAUI().exportEPS();
			} catch (ExceptionWritingForbidden e1) {
				errorDialog(e1);
			}
		} else if (_type.equals("tikz")) {
			try {
				_vp.getVARNAUI().exportTIKZ();
			} catch (ExceptionWritingForbidden e1) {
				errorDialog(e1);
			}
		} else if (_type.equals("xfig")) {
			try {
				_vp.getVARNAUI().exportXFIG();
			} catch (ExceptionWritingForbidden e1) {
				errorDialog(e1);
			}
		} else if (_type.equals("svg")) {
			try {
				_vp.getVARNAUI().exportSVG();
			} catch (ExceptionWritingForbidden e1) {
				errorDialog(e1);
			}
		} else if (_type.equals("jpeg")) {
				_vp.getVARNAUI().exportJPEG();
		} else if (_type.equals("png")) {
				_vp.getVARNAUI().exportPNG();
		} else
			return false;
		return true;
	}

	/**
	 * Return the extension of a file, it means the string after the last dot of
	 * the file name
	 * 
	 * @param f
	 *            The file
	 * @return <code>null</code> if the file name have no dot<br>
	 *         <code>ext</code> if the file name contains a dot
	 */
	public String getExtension(File f) {
		String s = f.getName();
		return getExtension(s);
	}

	/**
	 * Return the extension of a string, it means the string after the last dot
	 * of the path
	 * 
	 * @param s
	 *            The strnig o the path
	 * @return <code>null</code> if the path have no dot<br>
	 *         <code>ext</code> if the path contains a dot
	 */
	public String getExtension(String s) {
		String ext = null;
		int i = s.lastIndexOf('.');

		if (i > 0 && i < s.length() - 1) {
			ext = s.substring(i + 1).toLowerCase();
		}
		return ext;
	}

	/**
	 * Open an error message dialog with the exception message
	 * 
	 * @param e1
	 *            The <code>Exception</code>
	 */
	public void errorDialog(Exception e1) {
		if (_vp.isErrorsOn())
			JOptionPane.showMessageDialog(_vp, e1.getMessage(), "VARNA Error",
					JOptionPane.ERROR_MESSAGE);
	}

	@Override
  public void onStructureRedrawn() {
		
		
	}

	@Override
  public void onWarningEmitted(String s) {
		if (_vp.isErrorsOn())
			JOptionPane.showMessageDialog(_vp,s, "VARNA Warning",
					JOptionPane.ERROR_MESSAGE);
	}

	public void onLoad(@SuppressWarnings("unused") String path) {
		
		
	}

	public void onLoaded() {
		
		
	}

	@Override
  public void onUINewStructure(VARNAConfig v, RNA r) {
		
		
	}

	@Override
  public void onZoomLevelChanged() {
		
		
	}

	@Override
  public void onZoomTranslationChanged() {
		
		
	}


}
