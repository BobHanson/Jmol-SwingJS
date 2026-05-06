/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Université Paris-Sud 91405 Orsay Cedex France

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

import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;

import fr.orsay.lri.varna.components.VARNAPanel;
import fr.orsay.lri.varna.views.VueUI;


/**
 * VARNAPanel Shortcuts Controller
 * 
 * @author darty
 * 
 */
public class ControleurVARNAPanelKeys implements KeyListener, FocusListener {

	private VARNAPanel _vp;
	

	public ControleurVARNAPanelKeys(VARNAPanel vp) {
		_vp = vp;
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
		// prise du focus
		//_vp.requestFocus();
		
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void keyPressed(KeyEvent e) {
		boolean controlDown = (e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK)) == InputEvent.CTRL_DOWN_MASK;
		boolean shiftDown = (e.getModifiersEx() & (InputEvent.SHIFT_DOWN_MASK)) == InputEvent.SHIFT_DOWN_MASK;
		boolean altDown = (e.getModifiersEx() & (InputEvent.ALT_DOWN_MASK)) == InputEvent.ALT_DOWN_MASK;
		VueUI ui = _vp.getVARNAUI();
		try {
			switch (e.getKeyCode()) {
			case (KeyEvent.VK_A):
				if (controlDown) {
					ui.about();
				}
				break;
			case (KeyEvent.VK_B):
				if (controlDown) {
					ui.setBorder();
				}
				else
					if (altDown) {
						ui.toggleDrawBackbone();
					}
				break;
			case (KeyEvent.VK_C):
				if (shiftDown && controlDown) {
					ui.setColorMapCaption();
				}
				break;
			case (KeyEvent.VK_D):
				if (controlDown) {
					if (shiftDown) {
						ui.pickGapsBasesColor();
					} else {
						ui.toggleColorGapsBases();
					}
				}
				break;
			case (KeyEvent.VK_E):
				if (controlDown) {
					ui.toggleShowNonPlanar();
				}
				break;
			case (KeyEvent.VK_F):
				if (controlDown) {
					ui.toggleFlatExteriorLoop();
				}
				break;
			case (KeyEvent.VK_G):
				if (controlDown) {
					ui.setBackground();
				}
				else if (!shiftDown && altDown) {
				  ui.toggleGaspinMode();
				}
				break;
			case (KeyEvent.VK_H):
				if (controlDown && !shiftDown) {
					ui.setBPHeightIncrement();
				}
				else if (controlDown && shiftDown)
				{
					Point2D.Double p = _vp.getLastSelectedPosition();
					ui.annotationsAddPosition((int)p.x,(int)p.y);
				}
				break;
			case (KeyEvent.VK_J):
				if (controlDown) {
					if (shiftDown) {
						ui.pickSpecialBasesColor();
					} else {
						ui.toggleColorSpecialBases();
					}
				}
				break;
			case (KeyEvent.VK_K):
				if (controlDown && shiftDown) {
					ui.loadColorMapValues();
				}
				else if (controlDown) {
					ui.setBackboneColor();
				}
				break;
			case (KeyEvent.VK_L):
				if (shiftDown && controlDown) {
					ui.toggleColorMap();
				} else if (controlDown)
				{
					ui.setColorMapStyle();
				} else if (shiftDown)
				{
					ui.setColorMapValues();
				} 
				break;
			case (KeyEvent.VK_M):
				if (controlDown) {
					ui.setNumPeriod();
				} else if (shiftDown && altDown) {
					ui.toggleModifiable();
				}
				break;
			case (KeyEvent.VK_N):
				if (controlDown) {
					ui.manualInput();
				}
				break;
			case (KeyEvent.VK_O):
				if (controlDown) {
					ui.openFileAsync();
				}
				break;
			case (KeyEvent.VK_P):
				if (controlDown && shiftDown) {
					ui.setBPStyle();
				}
				else if (controlDown && !shiftDown) {
					ui.print();
				}
				break;
			case (KeyEvent.VK_Q):
				if      (controlDown && !shiftDown && !altDown) {
					_vp.getVARNAUI().autoAnnotateHelices();
				}
				else if (controlDown && shiftDown && !altDown) {
					_vp.getVARNAUI().autoAnnotateTerminalLoops();					
				}
				else if (!controlDown && shiftDown && altDown) {
					_vp.getVARNAUI().autoAnnotateInteriorLoops();					
				}
				else if (controlDown && !shiftDown && altDown) {
					_vp.getVARNAUI().autoAnnotateStrandEnds();					
				}
				break;
			case (KeyEvent.VK_R):
				if (controlDown) {
					if (shiftDown) {
						ui.reset();
					} else {
						ui.globalRotation();
					}
				}
				break;
			case (KeyEvent.VK_S):
				if (controlDown) {
					if (shiftDown) {
						ui.setSpaceBetweenBases();
					} else {
						ui.saveAs();
					}
				}
				break;
			case (KeyEvent.VK_T):
				if (controlDown) {
					if (shiftDown) {
						ui.setTitleFont();
					} else if (altDown) {
						ui.setTitleColor();
					} else {
						ui.setTitle();
					}
				}
				break;
			case (KeyEvent.VK_U):
				if (controlDown && !shiftDown && !altDown) {
					_vp.getVARNAUI().baseTypeColor();
				} else if (!controlDown && shiftDown && !altDown) {
					_vp.getVARNAUI().basePairTypeColor();
				} else if (!controlDown && !shiftDown && altDown) {
					_vp.getVARNAUI().baseAllColor();
				}
				break;
			case (KeyEvent.VK_W):
				if (controlDown) {
					ui.toggleShowNCBP();
				}
				break;
			case (KeyEvent.VK_X):
				if (controlDown) {
					ui.export();
				}
				break;
			case (KeyEvent.VK_Y):
				if (controlDown) {
					ui.redo();
				}
			break;

			case (KeyEvent.VK_Z):
				if (controlDown && !shiftDown) {
					ui.undo();
				}
				else if (controlDown && shiftDown) {
					ui.redo();
				}
				else if (!controlDown && !shiftDown) {
					ui.customZoom();
				}
				break;
			case (KeyEvent.VK_1):
				if (controlDown) {
					ui.line();
				}
				break;
			case (KeyEvent.VK_2):
				if (controlDown) {
					ui.circular();
				}
				break;
			case (KeyEvent.VK_3):
				if (controlDown) {
					ui.radiate();
				}
				break;
			case (KeyEvent.VK_4):
				if (controlDown) {
					ui.aNAView();
				}
				break;
			case (KeyEvent.VK_5):
				if (controlDown) {
					ui.varnaView();
				}
				break;
			case (KeyEvent.VK_6):
				if (controlDown) {
					ui.motifView();
				}
				break;

			// Navigation control keys (Zoom in/out, arrow keys ...)
			case (KeyEvent.VK_DOWN):
				if (_vp.getZoom() > 1) {
					_vp.setTranslation(new Point(_vp.getTranslation().x,_vp.getTranslation().y-5));
					_vp.checkTranslation();
				}
				break;
			case (KeyEvent.VK_UP):
				if (_vp.getZoom() > 1) {
					_vp.setTranslation(new Point(_vp.getTranslation().x,_vp.getTranslation().y+5));
					_vp.checkTranslation();
				}
				break;
			case (KeyEvent.VK_LEFT):
				if (_vp.getZoom() > 1) {
					_vp.setTranslation(new Point(_vp.getTranslation().x+5,_vp.getTranslation().y));
					_vp.checkTranslation();
				}
				break;
			case (KeyEvent.VK_RIGHT):
				if (_vp.getZoom() > 1) {
					_vp.setTranslation(new Point(_vp.getTranslation().x-5,_vp.getTranslation().y));
					_vp.checkTranslation();
				}
				break;
			case (KeyEvent.VK_EQUALS):
			case (KeyEvent.VK_PLUS):
				ui.zoomIn();
				break;
			case (KeyEvent.VK_MINUS):
				ui.zoomOut();
				break;
			}
		} catch (Exception e1) {
			_vp.errorDialog(e1);
		}
		_vp.repaint();
	}

	/**
	 * if ((e.getKeyCode() == KeyEvent.VK_PLUS)||(e.getKeyChar() == '+')) {
	 * _vp.getVARNAUI().UIZoomIn(); } else if (e.getKeyCode() ==
	 * KeyEvent.VK_MINUS) { _vp.getVARNAUI().UIZoomOut(); } // 1 pour Redraw
	 * Radiate else if (e.getKeyChar() == KeyEvent.VK_1) {
	 * _vp.getVARNAUI().UIRadiate(); } // 2 pour Redraw Circular else if
	 * (e.getKeyChar() == KeyEvent.VK_2) { _vp.getVARNAUI().UICircular(); } // 3
	 * pour Redraw NAView else if (e.getKeyChar() == KeyEvent.VK_3) {
	 * _vp.getVARNAUI().UINAView(); }
	 * 
	 * // 4 for RNA on a line else if (e.getKeyChar() == KeyEvent.VK_4) {
	 * _vp.getVARNAUI().UILine(); } // 5 fun arn random coord else if
	 * (e.isControlDown() && e.getKeyChar() == KeyEvent.VK_9) { for (int i = 0;
	 * i < _vp.getRNA().get_listeBases().size(); i++) {
	 * _vp.getRNA().get_listeBases().get(i).set_coords( new
	 * Point2D.Double(_vp.getWidth() * Math.random(), _vp.getHeight() *
	 * Math.random())); _vp.getRNA().get_listeBases().get(i).set_center( new
	 * Point2D.Double(_vp.getWidth() / 2 Math.random(), _vp.getHeight() / 2
	 * Math.random())); } } // 6 fun random arn structure else if
	 * (e.isControlDown() & e.getKeyChar() == KeyEvent.VK_8) { try {
	 * _vp.drawRNA(_vp.getRNA().getListeBasesToString(), getRandomRNA(), _vp
	 * .getRNA().get_drawMode()); } catch (ExceptionNonEqualLength e1) {
	 * _vp.errorDialog(e1); } } _vp.repaint(); }
	 **/

	public String getRandomRNA() {
		int pile = 0, j, i = 0;
		double l;
		String fun = "";
		while (i < 2000) {
			if (Math.random() > 0.5) {
				j = 0;
				l = Math.random() * 10;
				while (j < l) {
					fun += '.';
					i++;
					j++;
				}
			} else {
				if (Math.random() > 0.5 && pile > 0) {
					j = 0;
					l = Math.random() * 5;
					while (j < l && pile > 0) {
						fun += ')';
						pile--;
						j++;
						i++;
					}
				} else {
					j = 0;
					l = Math.random() * 5;
					while (j < l) {
						fun += '(';
						pile++;
						j++;
						i++;
					}

				}
			}
		}
		while (pile > 0) {
			fun += ')';
			pile--;
		}
		return fun;
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}

	public void focusGained(FocusEvent arg0) {
		_vp.repaint();
	}

	public void focusLost(FocusEvent arg0) {
		_vp.repaint();
	}
}