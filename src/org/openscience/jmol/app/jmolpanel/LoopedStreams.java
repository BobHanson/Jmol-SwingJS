/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2013-11-24 15:00:42 -0600 (Sun, 24 Nov 2013) $
 * $Revision: 19010 $
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
package org.openscience.jmol.app.jmolpanel;

import java.io.PipedOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.OutputStream;
import java.io.IOException;

public class LoopedStreams {

  PipedOutputStream pipedOS = new PipedOutputStream();
  boolean keepRunning = true;
  ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream() {

    @Override
    public void close() {

      keepRunning = false;
      try {
        super.close();
        pipedOS.close();
      } catch (IOException e) {

        // Do something to log the error -- perhaps invoke a 
        // Runnable to log the error. For now we simply exit.
        System.exit(1);
      }
    }
  };

  private PipedInputStream pipedIS = new PipedInputStream() {

    @Override
    public void close() {

      keepRunning = false;
      try {
        super.close();
      } catch (IOException e) {

        // Do something to log the error -- perhaps invoke a 
        // Runnable to log the error. For now we simply exit.
        System.exit(1);
      }
    }
  };

  public LoopedStreams() throws IOException {
    pipedOS.connect(pipedIS);
    startByteArrayReaderThread();
  }    // LoopedStreams()

  public InputStream getInputStream() {
    return pipedIS;
  }    // getInputStream()

  public OutputStream getOutputStream() {
    return byteArrayOS;
  }    // getOutputStream()

  private void startByteArrayReaderThread() {

    new Thread(new Runnable() {

      @Override
      public void run() {

        while (keepRunning) {

          // Check for bytes in the stream.
          if (byteArrayOS.size() > 0) {
            byte[] buffer = null;
            synchronized (byteArrayOS) {
              buffer = byteArrayOS.toByteArray();
              byteArrayOS.reset();    // Clear the buffer.
            }
            try {

              // Send the extracted data to
              // the PipedOutputStream.
              pipedOS.write(buffer, 0, buffer.length);
            } catch (IOException e) {

              // Do something to log the error -- perhaps 
              // invoke a Runnable. For now we simply exit.
              System.exit(1);
            }
          } else {                    // No data available, go to sleep.
            try {

              // Check the ByteArrayOutputStream every
              // 1 second for new data.
              Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
          }
        }
      }
    }).start();
  }    // startByteArrayReaderThread()
}      // LoopedStreams




