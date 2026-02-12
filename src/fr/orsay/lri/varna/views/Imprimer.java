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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;

public class Imprimer implements Printable {
	private String phrase;

	public Imprimer(String phrase) {
		this.phrase = phrase;
	}

	public int print(Graphics g, PageFormat pf, int indexPage) {
		if (indexPage > 0)
			return NO_SUCH_PAGE;
		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(Color.blue);
		g2.setFont(new Font("Serif", Font.PLAIN, 64));
		g2.drawString(phrase, 96, 144);
		return PAGE_EXISTS;
	}

	public static void main(String args[]) {
		PrinterJob tache = PrinterJob.getPrinterJob();
		tache.setPrintable(new Imprimer(
				"Ceci est un teste d'impression en java!"));

		// printDialog affiche la fenetre de sélection de l’imprimante,
		// etc...

		// il retourne true si on clique sur "Ok", false si on clique sur
		// "Annuler"
		//System.out.println(PrinterJob.getPrinterJob());
		if (!tache.printDialog())
			return;
		try {
			tache.print();
		} catch (Exception e) {
			System.err.println("impossible d'imprimer");
		}
	}
}