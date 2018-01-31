/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-09-05 23:09:52 -0500 (Sat, 05 Sep 2015) $
 * $Revision: 20757 $
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
package org.openscience.jmol.app.jmolpanel.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import javax.swing.JTextArea;

import org.openscience.jmol.app.jmolpanel.LoopedStreams;

public class ConsoleTextArea extends JTextArea {

  public ConsoleTextArea(InputStream[] inStreams) {
    for (int i = 0; i < inStreams.length; ++i) {
      startConsoleReaderThread(inStreams[i]);
    }
  }    // ConsoleTextArea()


  public ConsoleTextArea(boolean doRedirect) throws IOException {

    final LoopedStreams ls = new LoopedStreams();

    String redirect = (doRedirect ? System.getProperty("JmolConsole") : "false");
    if (redirect == null || redirect.equals("true")) {
        // Redirect System.out & System.err.        
        PrintStream ps = new PrintStream(ls.getOutputStream());
        System.setOut(ps);
        System.setErr(ps);
    }
    startConsoleReaderThread(ls.getInputStream());
  }    // ConsoleTextArea()


  private void startConsoleReaderThread(InputStream inStream) {

    final BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
    new Thread(new Runnable() {
      @Override
      public void run() {
        Thread.currentThread().setName("ConsoleReaderThread");
        try {
          String s;
          //Document doc = getDocument();
          while ((s = br.readLine()) != null) {
            //boolean caretAtEnd = false;
            //caretAtEnd = (getCaretPosition() == doc.getLength());
            append(s + "\n");
            //if (caretAtEnd) {
              //setCaretPosition(doc.getLength());
           //}
          }
        } catch (Exception e) {
          //
        }
      }
    }).start();
  }
}
