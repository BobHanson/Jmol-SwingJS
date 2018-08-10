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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javajs.util.AU;
import javajs.util.PT;

import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

/**
 * A service for interacting with NBOServe (experimental)
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
  //    
  // 3) When View starts up, a new jmol_outfile is created, and it can be deleted.
  // 

  // modes of operation

  static final int MODE_ERROR          = -1;
  static final int MODE_RAW            = 0;// leave this 0; it is referred to that in StatusListener

  // these are for postToNBO; n * 10 + module value(s)
  

  protected Viewer vwr;
  protected Process nboServer;
  protected Thread nboListener;
  protected NBODialog dialog;
  protected NBORequest currentRequest;
  protected Object lock;
  protected Queue<NBORequest> requestQueue;  
  
  private PrintWriter nboIn;
  protected BufferedInputStream nboOut;

  private boolean cantStartServer;  
  private String serverPath;

  private String exeName = "NBOServe";
  private boolean doConnect;
  
  private boolean isReady;

  protected void setReady(boolean tf) {
    //System.out.println("isready = " + tf);
    isReady = tf;
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
   * Check to see if we have tried and are not able to make contact with NBOServe at the designated location.
   * 
   * @return true if we hvae found that we cannot start the server.
   * 
   */
  boolean isOffLine() {
    return cantStartServer;
  }

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
  public boolean isWorking() {
    return (currentRequest != null);
  }  
  
  ////////////////////// NBOServe Process //////////////////////////
  
  byte[] buffer = new byte[1024];
  String cachedReply = "";

  private NBORunnable nboRunnable;
  private boolean haveLicense;

  /**
   * Start the ProcessBuilder for NBOServe and listen to its stdout (Fortran LFN
   * 6, a Java BufferedInputStream). We simply look for available bytes and listen
   * for a 10-ms gap, which should be sufficient, since all these are done via a 
   * single flush;
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
  String startProcess() {
    try {
      cantStartServer = true;
      if (!doConnect)
        return null;
      nboListener = null;
      String path = getServerPath(exeName);
      
      System.out.println("startProcess: " + path);

      ProcessBuilder builder = new ProcessBuilder(path);
      
      builder.directory(new File(new File(path).getParent())); // root folder for executable 
      builder.redirectErrorStream(true);
      nboServer = builder.start();
      
      // pick up the NBO stdout
      
      nboOut = (BufferedInputStream) nboServer.getInputStream();
      System.out.println("startProcess:" + nboServer);

      // start a listener
      
      nboListener = new Thread(nboRunnable = new NBORunnable());
      nboListener.setName("NBOServiceThread" + System.currentTimeMillis());
      nboListener.start();
      
      // open a channel to NBOServe's stdin.
      
      nboIn = new PrintWriter(nboServer.getOutputStream());
      
    } catch (IOException e) {
      String s = e.getMessage();
      System.out.println(s);
      if (s.contains("error=1455"))
         s = "Jmol can't do that - low on memory";
      dialog.logError(s);
      return s;
    }
    cantStartServer = false;
    return null;
  }

  /**
   * Close the process and all channels associated with it. 
   * @param andPause 
   */
  public void closeProcess(boolean andPause) {
    
    if (nboRunnable != null) {
      nboRunnable.destroyed = true;
      andPause = true;
    }
    if (andPause) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e1) {
      }
      

    }
    setReady(false);
    nboOut = null;
    
    try {
      nboIn.close();
    } catch (Exception e) {
    }
    nboIn = null;
    
    try {
      nboListener.interrupt();
    } catch (Exception e) {
      System.out.println("can't interrupt");
    }
    nboListener = null;
    
    try {
      nboServer.destroy();
    } catch (Exception e) {
      // we don't care
    }
    nboServer = null;
    currentRequest = null;
  }

  /**
   * Restart the process from scratch.
   * 
   * @return null or an error message
   * 
   */
  String restart() {
    closeProcess(true);
    return startProcess();
  }

  /**
   * Restart the processor only if necessary
   * 
   * @return true if successful
   */
  public boolean restartIfNecessary() {
    if (nboServer == null)
      startProcess();
    return (nboServer != null);
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
    File f = new File(getServerPath("gennbo.bat"));
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
      if (isReady && requestQueue.isEmpty() && currentRequest == null) {
        currentRequest = request;
        requestQueue.add(currentRequest);
        startRequest(currentRequest);
      } else {
        requestQueue.add(request);
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
   * Start the current request by writing its metacommands to disk and sending a
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
    if (request == null || nboRunnable.destroyed)
      return;

    System.out.println("starting request for " + request.statusInfo + " " + request);
    if (request.timeStamp != 0) {
      System.out.println("SENDING TWICE?");
      nboRunnable.destroyed = true;
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

    if (nboIn == null)
      restart();
    nboIn.println(cmd);
    nboIn.flush();
  }

  //////////////////////////// Process NBO's Reply ///////////////////////////

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
      restart();
      return true;
    }
    
    //newly added by professor Frank. NBOServe should restart whenever this message is received and the 
    //souce of error sent by NBOServe should be printed to session dialog.
    if(s.indexOf("**NBOServe fatal error**") >=0)
    {
      int i;
      int index=s.indexOf("**NBOServe fatal error**");
      s=s.substring(index);
      String errorLines[]=s.split("\n");
      for(i=0;i<errorLines.length;i++)
      {
        logServerLine(errorLines[i],Logger.LEVEL_ERROR);
      }
      dialog.alertError("NBOServe has stopped working - restarting");
      currentRequest = null;
      clearQueue();
      restart();
      return true;
    }
    
    if (isFortranError(s) || s.indexOf("missing or invalid") >= 0) {
      if (!s.contains("end of file")) {
        dialog.alertError(s);
      }
      currentRequest = null;
      clearQueue();
      restart();
      return true;
    }

    int pt;

    // We don't always remove the request, because in the case of RUN, 
    // we are prone to get additional sysout message from other processes,
    // particularly gennbo.bat. These will come BEFORE the start message.

    boolean removeRequest = true;

    try { // with finally clause to remove request from queue

      if ((pt = s.indexOf("***errmess***")) >= 0) {
        System.out.println(s);
        try {
          s = PT.split(s, "\n")[2];
          logServerLine(s.substring(s.indexOf("\n") + 1), Logger.LEVEL_ERROR);
        } catch (Exception e) {
          // ignore
        }
        logServerLine("NBOPro can't do that.", Logger.LEVEL_WARN);
        return true;
      }
      
      
      if ((pt = s.indexOf("**NBOServe warning**")) >= 0) {
        try {
          int i;
          int index=s.indexOf("**NBOServe warning**");
          s=s.substring(index);
          String errorLines[]=s.split("\n");
          for(i=0;i<errorLines.length;i++)
          {
            logServerLine(errorLines[i],Logger.LEVEL_ERROR);
          }
        } catch (Exception e) {
          // ignore
        }
        logServerLine("NBOPro can't do that.", Logger.LEVEL_WARN);
        return true;
      }

      if ((pt = s.lastIndexOf("*start*")) < 0) {

        // Note that RUN can dump all kinds of things to SYSOUT prior to completion.

        if (currentRequest == null)
          return (removeRequest = true);
        logServerLine(s, (currentRequest.isMessy ? Logger.LEVEL_DEBUG
            : Logger.LEVEL_ERROR));
        return (removeRequest = !currentRequest.isMessy);
      }
      s = s.substring(pt + 8); // includes \n
      pt = s.indexOf("*end*");
      if (pt < 0) {
        System.out.println("...listening...");
        //System.out.println("bad start/end packet from NBOServe: !!!!!!!>>" + s + "<<!!!!!!!");
        removeRequest = false;
        return false;
      }

      // standard expectation
      //System.out.println("*start*\n" + s);
      if (currentRequest == null) {
        if (haveLicense) {
          System.out.println("TRANSMISSION ERROR: UNSOLICITED!");
        } else {
          haveLicense = true;
          dialog.setLicense(s);
        }
      } else {
        currentRequest.sendReply(s.substring(0, pt));
      }
      
      if(currentRequest!=null && currentRequest.isVideoCreate)
        dialog.logValue("Video Create has been completed.");
      
      return true;
    } finally {
      if (currentRequest != null && removeRequest) {
        requestQueue.remove();
        currentRequest = null;
        dialog.setStatus("");
      }
    }
  }

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

  class NBORunnable implements Runnable {

    protected boolean destroyed;

    @Override
    public void run() {

      String s;

      while (!destroyed && !Thread.currentThread().isInterrupted()) {
        try {
          if(currentRequest!=null && currentRequest.isVideoCreate)
          {
            
          }
          else
          {
            Thread.sleep(10);
          }

          // Get and process the return, continuing if incomplete or no activity.

          if (destroyed || (s = getNBOMessage()) == null || !processServerReturn(s))
            continue;

          // Success!

          cachedReply = "";

          // Get the next request. Note that we leave the currentRequest on 
          // the Queue because that indicates we are still working. 
          // Note that we FIRST check for messages, as NBOServe will have an
          // unsolicited startup message for us providing license information.
          if (destroyed)
            continue;
          currentRequest = requestQueue.peek();
          startRequest(currentRequest);

        } catch (Throwable e1) {
          clearQueue();
          e1.printStackTrace();
          dialog.setStatus(e1.getMessage());
          continue;
          // includes thread death
        }

      }
      if (destroyed)
        closeProcess(false);
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
    protected synchronized String getNBOMessage() throws IOException, InterruptedException {
      
      // 1. Check for available bytes.
      
      int n = (nboOut == null ? 0 : nboOut.available());
      if (n <= 0) {
        return null;
      }
      setReady(true);
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
      //Added by fzy
      if(currentRequest!=null && currentRequest.isVideoCreate)
        displayCreateVideoProgress(s);
      System.out.println(">> " + s + "<<");
      return cachedReply = cachedReply + s;
    }
    
    /*
     * Display the progress for create video in VIEW module
     */
    private void displayCreateVideoProgress(String s)
    {
      s=s.trim();
      s=s.replace("\\","/");
      if(s.equals("*start*")||s.equals("*end*"))
        return;
      
      Pattern pattern=Pattern.compile("(.*/)(\\w+_?\\d*\\.bmp$)");
      Matcher matcher=pattern.matcher(s);
      if(matcher.matches())
      {
        dialog.setStatus(matcher.group(2)+" has been created.");
      }
    }

  }
}



