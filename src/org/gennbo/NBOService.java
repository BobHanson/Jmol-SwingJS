/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-12-13 22:43:17 -0600 (Sat, 13 Dec 2014) $
 * $Revision: 20162 $
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
package org.gennbo;

import java.awt.Cursor;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Queue;

import javajs.util.AU;
import javajs.util.PT;

import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

/**
 * A service for interacting with NBOServe.
 * 
 * This class maintains information about the connection to NBOServe. 
 * 
 * 
 * 
 * TODO: figure out how to manage time-consuming asynchronous requests
 * 
 * 
 */
public class NBOService {

  
  
  
  
  /// BH: observations: 
  // 
  // 1) NBOServe creates jmol_infile.txt, jmol_molfile.txt, and jmol_outfile.txt. 
  // 
  // 2) Of these three, jmol_outfile is locked for writing during NBOServe sessions 
  //    and stays locked until the program finishes. 
  //    
  // 3) When View starts up, a new jmol_outfile is created, and it can be deleted.
  // 
  

  // modes of operation

  static final int MODE_ERROR          = -1;
  static final int MODE_RAW            = 0;// leave this 0; it is referred to that in StatusListener

  private Viewer vwr;
  private Object lock;
  private String serverPath;
  private boolean doConnect;
  
  protected NBODialog dialog;
  protected String exeName = "NBOServe.exe";
  private String gennboPath = "gennbo.bat";
  
  protected Queue<NBORequest> requestQueue;  
  protected NBORequest currentRequest;
  protected boolean cantStartServer;  
  protected boolean haveLicense;

  boolean isReady() {
    return nboListener != null && nboListener.isReady;
  }
  
  /**
   * A class to manage communication between Jmol and NBOServe.
   * 
   * @param nboDialog 
   * 
   * @param vwr
   *        The interacting display we are reproducing (source of view angle
   *        info etc)
   * @param doConnect 
   */
  public NBOService(NBODialog nboDialog, Viewer vwr, boolean doConnect) {
    this.dialog = nboDialog;
    this.vwr = vwr;
    this.doConnect = doConnect;
    setServerPath(nboDialog.nboPlugin.getNBOProperty("serverPath", System.getProperty("user.home")
        + "/NBOServe"));
    requestQueue = new ArrayDeque<NBORequest>();
    lock = new Object();
  }

  ////////////////////// Initialization //////////////////////////

  /**
   * Return path to NBOServe directory.
   * 
   * @param fileName
   *        or null for path itself, without slash
   * 
   * @return path
   */
  String getServerPath(String fileName) {
    return (fileName == null ? serverPath : serverPath + "/" + fileName);
  }

  /**
   * Set path to NBOServe directory
   * 
   * @param path
   */
  protected void setServerPath(String path) {
    serverPath = path.replace('\\',  '/');
    dialog.nboPlugin.setNBOProperty("serverPath", path);
  }

  /**
   * Check to see that the service has been initialized.
   * 
   * @return true if the path to the server has been set.
   */
  protected boolean isEnabled() {
    return serverPath != null;
  }

  /**
   * Check to see if there is a current request running
   * 
   * @return true if there is a current request
   * 
   */
  public int getWorkingMode() {
    return (currentRequest == null ? NBODialog.DIALOG_HOME : currentRequest.dialogMode);
  }  
  
  ////////////////////// NBOServe Process //////////////////////////
  
  private NBOListener nboListener;
  private boolean allowRequestQueing = true;

  /**
   * Start the ProcessBuilder for NBOServe and listen to its stdout (Fortran LFN
   * 6, a Java BufferedInputStream). We simply look for available bytes and
   * listen for a 10-ms gap, which should be sufficient, since all these are
   * done via a single flush;
   * 
   * 
   * Expected packets are of one of the following three forms:
   * 
   * *start*
   * 
   * [one or more lines]
   * 
   * *end*
   * 
   * or
   * 
   * ***errmess***
   * 
   * [one error message line]
   * 
   * or
   * 
   * [some sort of identifiable Fortran or system error message most likely
   * indicating NBO has died]
   * 
   * 
   * @return a caught exception message, or null if we are not connected or we
   *         are successful
   */
  boolean startListener() {
    cantStartServer = true;
    if (!doConnect)
      return false;
    if (nboListener != null)
      nboListener.disconnect();
    nboListener = null;
    boolean trying = false;
    for (int i = 0; i < 3; i++) {
      if (nboListener != null)
        nboListener.disconnect();
      nboListener = new NBOListener();
      boolean connected = nboListener.connect();
      if (connected) {
        if (trying)
          dialog.logError("Connection successful");
        cantStartServer = false;
        return true;
      }
      trying = true;
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
      }
    }
    dialog.logError("Cannot start NBOServe process");
    nboListener = null;
    return false;
  }
  

  /**
   * Close the process and all channels associated with it. 
   * @param andPause 
   */
  public synchronized void closeProcess(boolean andPause) {
    if (nboListener != null) {
      System.out.flush();
      System.out.println("CLOSING PROCESS " +Thread.currentThread());    
      nboListener.disconnect();
      nboListener = null;
      if (andPause) {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e1) {
        }
      }
    }
    currentRequest = null;
    haveLicense = false;
    dialog.setLicense("\n\n");
  }

  /**
   * Restart the process from scratch.
   * 
   * @return null or an error message
   * 
   */
  boolean restart() {
    closeProcess(false);
    System.out.println("NBO restart");
    return startListener();
  }

  /**
   * Restart the processor only if necessary
   * 
   * @return true if successful
   */
  public boolean restartIfNecessary() {
    return (nboListener != null || startListener());
  }

  /**
   * Take a quick look to see that gennbo.bat is present and notify the user if it is not.
   * 
   * Note that this is the only method that is marginally Windows-dependent.
   * 
   * @return true if gennbo.bat exists.
   * 
   */
  public boolean haveGenNBO() {
    if (!doConnect)
      return true;
    File f = new File(getServerPath(gennboPath));
    if (!f.exists()) {
      vwr.alert(f + " not found, make sure gennbo.bat is in same directory as "
          + exeName);
      return false;
    }
    return true;
  }

  ////////////////////// Request Queue //////////////////////////
  
  /**
   * The interface for ALL communication with NBOServe from NBODialog.
   * 
   * @param request
   * 
   */
  protected void postToNBO(NBORequest request) {
    synchronized (lock) {
      restartIfNecessary();
      if (nboListener != null && nboListener.isReady && requestQueue.isEmpty() && currentRequest == null) {
        currentRequest = request;
        requestQueue.add(currentRequest);
        startRequest(currentRequest);
      } else if (allowRequestQueing) {
        requestQueue.add(request);
        System.out.println("NBOService request queued " + requestQueue.size());
      } else {
        System.out.println("request denied " + request);
      }
    }
  }
  
  /**
   * Clear the request queue
   * 
   */
  public void clearQueue() {
    requestQueue.clear();
  }


  //////////////////////////// Send Request to NBOServe ///////////////////////////

  /**
   * Start the current request by writing its meta-commands to disk and sending a
   * command to NBOServe directing it to that file via its stdin.
   * 
   * We allow the Jmol command
   * 
   * set TESTFLAG2 TRUE
   * 
   * to give us an alert just prior to sending the command to NBOServe. This
   * allows us to edit these files in PSPad (which will scan for changed files
   * and load them again if requested) and thus send any modification we want to
   * NBOServe for testing.
   * 
   * @param request
   */
  protected void startRequest(NBORequest request) {
    if (request == null)
      return;
    try {
      Thread.sleep(30);
    } catch (InterruptedException e) {
    }
    if (nboListener == null && !startListener() || nboListener.destroyed)
      return;

    System.out.println("starting request for " + request.statusInfo + " " + request);
    if (request.timeStamp != 0) {
      System.out.println("SENDING TWICE?");
      nboListener.disconnect();
      nboListener = null;
      clearQueue();
      return;
    }
    request.timeStamp = System.currentTimeMillis();
    currentRequest = request;

    String cmdFileName = null, data = null;
    for (int i = 2, n = request.fileData.length; i < n + 2; i += 2) {

      cmdFileName = request.fileData[i % n];
      data = request.fileData[(i + 1) % n];
      if (cmdFileName != null) {
        dialog.inputFileHandler.writeToFile(getServerPath(cmdFileName), data);
        System.out.println("saved file " + cmdFileName + "\n" + data + "\n");
      }
    }
    dialog.setStatus(request.statusInfo);
    String cmd = "<" + cmdFileName + ">";

    System.out.println("sending " + cmd);
    nboListener.submitRequest(cmd);
  }

  //////////////////////////// Process NBO's Reply ///////////////////////////

  /**
   * Log a message to NBODialog; probably an error.
   * 
   * @param line
   * @param level
   */
  protected void logServerLine(String line, int level) {
    dialog.logInfo(line, level);
    if (level == Logger.LEVEL_ERROR)
      dialog.setStatus("");
  }

  /**
   * Check for known errors; PG is the Portland Group Compiler 
   * @param line
   * @return true if a recognized error has been found.
   */
  protected boolean isFortranError(String line) {
    return line.indexOf("Permission denied") >= 0
        || line.indexOf("PGFIO-F") >= 0 
        || line.indexOf("Invalid command") >= 0;
  }

  class NBOListener extends Thread {

    
    final public static int NBO_STATUS_UNINITIALIZED = 0;
    final public static int NBO_STATUS_INITIALIZING = 1;
    final public static int NBO_STATUS_READY = 2;
    final public static int NBO_STATUS_LISTENING = 3;
    final public static int NBO_STATUS_WORKING = 4;
    final public static int NBO_STATUS_REPLYING = 5;
    final public static int NBO_STATUS_BAD_REPLY = 6;
    final public static int NBO_STATUS_DEAD = 7;
    
    public int nboStatus = NBO_STATUS_UNINITIALIZED;
    
    protected boolean isReady;
    protected boolean destroyed;
    
    private byte[] buffer = new byte[1024];
    private String cachedReply = "";

    private long lastTime;
    private BufferedInputStream nboOut;
    private Process nboProcess;
    private PrintWriter nboIn;
//    private String thisReply;
    
    @Override
    public void run() {
      System.out.println("NBORunnable run()");
      String s;

      while (!destroyed && !Thread.currentThread().isInterrupted()) {
    
        if ((NBOConfig.debugVerbose) && System.currentTimeMillis() - lastTime > 1000) {
          lastTime = System.currentTimeMillis();
          dialog.logStatus("NBORunnable looping");
        }
        try {
          Thread.sleep(10);

          // Get and process the return, continuing if incomplete or no activity.

          if (nboOut == null || destroyed || (s = getNBOMessage()) == null || !processServerReturn(s))
            continue;

          // Success!

          cachedReply = "";

          // Get the next request. Note that we leave the currentRequest on 
          // the Queue because that indicates we are still working. 
          // Note that we FIRST check for messages, as NBOServe will have an
          // unsolicited startup message for us providing license information.
          if (destroyed || !haveLicense)
            continue;
            
          currentRequest = requestQueue.peek();
          startRequest(currentRequest);

        } catch (Throwable e1) {
          clearQueue();
          if (("" + e1.getMessage()).indexOf("sleep") < 0)
            e1.printStackTrace();
          dialog.setStatus(e1.getMessage());
          continue;
          // includes thread death
        }

      }
      System.out.println("NBORunnable done " + destroyed);
      if (destroyed) {
        closeProcess(false);
      }
      System.out.println("NBORunnable exiting run()");      
    }
    
    public void submitRequest(String cmd) {
      nboStatus = NBO_STATUS_WORKING;
      nboIn.println(cmd);
      nboIn.flush();
    }

    /**
     * Process the return from NBOServe.
     * 
     * @param s
     * @return true if we are done
     */
    protected boolean processServerReturn(String s) {

      // Check for the worst

      if (s.indexOf("FORTRAN STOP") >= 0) {
        System.out.println(s);
        dialog.alertError("NBOServe has stopped working - restarting");
        currentRequest = null;
        clearQueue();
        nboStatus = NBO_STATUS_DEAD;
        return restart();
      }

      if (isFortranError(s) || s.indexOf("missing or invalid") >= 0) {
        if (!s.contains("end of file")) {
          dialog.alertError(s);
        }
        boolean wasWorking = (getWorkingMode() == NBODialog.DIALOG_RUN);
        currentRequest = null;
        clearQueue();
        nboStatus = NBO_STATUS_DEAD;
        if (wasWorking)
          dialog.inputFileHandler.checkNBOComplete(true);
        dialog.setStatus("");
        return restart();
      }

      int pt;

      // We don't always remove the request, because in the case of RUN, 
      // we are prone to get additional sysout message from other processes,
      // particularly gennbo.bat. These will come BEFORE the start message.

      boolean removeRequest = true;

      try { // with finally clause to remove request from queue

        if ((pt = s.lastIndexOf("*start*")) < 0) {
          //          logServerLine(s, Logger.LEVEL_ERROR);
          //System.out.println(thisReply);
          if (!currentRequest.isMessy) {
            nboStatus = NBO_STATUS_BAD_REPLY;
          }
          return (removeRequest = !currentRequest.isMessy);
        }
        s = s.substring(pt + 8); // includes \n
        pt = s.indexOf("*end*");
        if (pt < 0) {
          nboStatus = NBO_STATUS_REPLYING;
          System.out.println("...listening...");
          //System.out.println("bad start/end packet from NBOServe: !!!!!!!>>" + s + "<<!!!!!!!");
          removeRequest = false;
          return false;
        }

        // standard expectation
        if (currentRequest == null) {
          if (haveLicense) {
            nboStatus = NBO_STATUS_BAD_REPLY;
            System.out.println("TRANSMISSION ERROR: UNSOLICITED!>>>>" + s
                + "<<<<<");
          } else {
            System.out.println("license found:" + s);
            haveLicense = true;
            dialog.setLicense(s);
            nboStatus = NBO_STATUS_READY;
          }
          return true;
        }
        
        if (s.indexOf("***errmess***") >= 0) {
          try {
            s = PT.split(s, "\n")[1];
            logServerLine(s.substring(s.indexOf("\n") + 1), Logger.LEVEL_ERROR);
          } catch (Exception e) {
            // ignore
          }
          logServerLine("NBOPro can't do that.", Logger.LEVEL_WARN);
          return true;
        }

        nboStatus = NBO_STATUS_LISTENING;
        currentRequest.sendReply(s.substring(0, pt));
        return true;
      } finally {
        if (currentRequest != null && removeRequest) {
          try {
            requestQueue.remove();
          } catch (Exception e) {
            System.out.println("NBOService requestQueue empty");
          }
          currentRequest = null;
          dialog.setStatus("");
        }
      }
    }

    protected boolean connect() {
      try {
        nboStatus = NBO_STATUS_INITIALIZING;
        String path = getServerPath(exeName);
        System.out.flush();
        System.out.println("startProcess: " + path + " "
            + Thread.currentThread());
        ProcessBuilder builder = new ProcessBuilder(path);

        builder.directory(new File(new File(path).getParent())); // root folder for executable 
        builder.redirectErrorStream(true);
        // start a listener

        System.out.println("creating new runnable");
        setName("NBOServiceThread" + System.currentTimeMillis());
        nboOut = null;
        System.out.println("creating new nboOut");
        nboOut = (BufferedInputStream) (nboProcess = builder.start())
            .getInputStream();
        System.out.println("creating new nboIn");
        nboIn = new PrintWriter(nboProcess.getOutputStream());
        start();
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          System.err.println(e);
        }
        System.out.println("checking nboOut");
        if (nboOut.available() > 0)
          System.out.println("OK!");
        System.out.println("startProcess:" + nboProcess + " "
            + nboOut.available());
        nboStatus = NBO_STATUS_LISTENING;
        return true;
      } catch (IOException e) {
        String s = e.getMessage();
        System.out.println(s);
        if (s.contains("error=1455"))
          s = "low on memory -- retrying";
        dialog.logError(s);
        closeProcess(false);
        return false;
      }
    }

    public void taskKillNBO() {
      String cmd = "taskKill /im " + exeName + " /t /f  ";
      try {
        Runtime.getRuntime().exec(cmd);
      } catch (IOException e) {
        System.out.println("NBOService cannot taskKill " + cmd);
      }
    }
    
    protected void disconnect() {
      destroyed = true;
      try {
        interrupt();
      } catch (Exception e) {

      }
      try {
        nboProcess = null;
        taskKillNBO();
        Thread.sleep(100);
        //        nboProcess.destroy();
      } catch (Exception e) {
        System.out.println("??" + e);
      }
    }


    /**
     * Retrieve a message from NBOServe by monitoring its sysout (a
     * BufferedInputStream for us).
     * 
     * @return new NBO output, or null if no activity
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    private synchronized String getNBOMessage() throws IOException, InterruptedException {
      
      // 1. Check for available bytes.
      
      int n = (nboOut == null ? 0 : nboOut.available());
      if (n <= 0) {
        return null;
      }
      isReady = true;
      while (n > buffer.length) {
        buffer = AU.doubleLengthByte(buffer);
      }
      
      // 3. Read the bytes into a single string and deliver that. 
      //    No need for a line buffer here.
      
      // (The problem we were having originally is that we were using 
      //  a BufferedReader, and it was blocking.)

      // 4. Deliver standard Java format without carriage return.
      
      n = nboOut.read(buffer, 0, n);    
      String s = PT.rep(new String(buffer, 0, n), "\r", "");
      System.out.println(">> " + s + "<<");
      //thisReply = s;
      return cachedReply = cachedReply + s;
    }
  }

}


