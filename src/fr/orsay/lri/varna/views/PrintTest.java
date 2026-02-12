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
package fr.orsay.lri.varna.views;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * This program demonstrates how to print 2D graphics
 */
public class PrintTest {
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		JFrame frame = new PrintTestFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.show();
	}
}

/**
 * This frame shows a panel with 2D graphics and buttons to print the graphics
 * and to set up the page format.
 */

@SuppressWarnings("serial")
class PrintTestFrame extends JFrame {
	public PrintTestFrame() {
		setTitle("PrintTest");
		setSize(WIDTH, HEIGHT);

		Container contentPane = getContentPane();
		canvas = new PrintPanel();
		contentPane.add(canvas, BorderLayout.CENTER);

		attributes = new HashPrintRequestAttributeSet();

		JPanel buttonPanel = new JPanel();
		JButton printButton = new JButton("Print");
		buttonPanel.add(printButton);
		printButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				try {
					PrinterJob job = PrinterJob.getPrinterJob();
					job.setPrintable(canvas);
					if (job.printDialog(attributes)) {
						job.print(attributes);
					}
				} catch (PrinterException exception) {
					JOptionPane.showMessageDialog(PrintTestFrame.this,
							exception);
				}
			}
		});

		JButton pageSetupButton = new JButton("Page setup");
		buttonPanel.add(pageSetupButton);
		pageSetupButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				PrinterJob job = PrinterJob.getPrinterJob();
				job.pageDialog(attributes);
			}
		});

		contentPane.add(buttonPanel, BorderLayout.NORTH);
	}

	private PrintPanel canvas;

	private PrintRequestAttributeSet attributes;

	private static final int WIDTH = 300;

	private static final int HEIGHT = 300;
}

/**
 * This panel generates a 2D graphics image for screen display and printing.
 */

@SuppressWarnings("serial")
class PrintPanel extends JPanel implements Printable {

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		drawPage(g2);
	}

	public int print(Graphics g, PageFormat pf, int page)
			throws PrinterException {
		if (page >= 1)
			return Printable.NO_SUCH_PAGE;
		Graphics2D g2 = (Graphics2D) g;
		g2.translate(pf.getImageableX(), pf.getImageableY());
		g2.draw(new Rectangle2D.Double(0, 0, pf.getImageableWidth(), pf
				.getImageableHeight()));

		drawPage(g2);
		return Printable.PAGE_EXISTS;
	}

	/**
	 * This method draws the page both on the screen and the printer graphics
	 * context.
	 * 
	 * @param g2
	 *            the graphics context
	 */
	public void drawPage(Graphics2D g2) {
		FontRenderContext context = g2.getFontRenderContext();
		Font f = new Font("Serif", Font.PLAIN, 72);

		boolean drawOutline = true;
		/**
		 * textLayout is not implemented 
		 * 
		 * @j2sNative 
		 * 
		 *            drawOutline = false;
		 */
		{}
		if (drawOutline) {
			// BH: SwingJS HTML5 would have to use a different method for this

			GeneralPath clipShape = new GeneralPath();

			TextLayout layout = new TextLayout("Hello", f, context);
			AffineTransform transform = AffineTransform.getTranslateInstance(0, 72);
			Shape outline = layout.getOutline(transform);
			clipShape.append(outline, false);

			layout = new TextLayout("World", f, context);
			transform = AffineTransform.getTranslateInstance(0, 144);
			outline = layout.getOutline(transform);
			clipShape.append(outline, false);

			g2.draw(clipShape);
			g2.clip(clipShape);
		} else {
			g2.setFont(f);
			g2.drawString("Hello", 0, 72);
			g2.drawString("World", 0, 144);
		}

		final int NLINES = 50;
		Point2D p = new Point2D.Double(0, 0);
		for (int i = 0; i < NLINES; i++) {
			double x = (2 * getWidth() * i) / NLINES;
			double y = (2 * getHeight() * (NLINES - 1 - i)) / NLINES;
			Point2D q = new Point2D.Double(x, y);
			g2.draw(new Line2D.Double(p, q));
		}
	}
}