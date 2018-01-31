package org.jmol.thread;

import org.jmol.api.JmolScriptEvaluator;
import org.jmol.script.ScriptContext;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

abstract public class JmolThread extends Thread {
  
  public String name = "JmolThread";
  
  private static int threadIndex;
  
  protected static final int INIT = -1;
  protected static final int MAIN = 0;
  protected static final int FINISH = -2;
  protected static final int CHECK1 = 1;
  protected static final int CHECK2 = 2;
  protected static final int CHECK3 = 3;
  
  protected Viewer vwr;
  protected JmolScriptEvaluator eval;
  protected ScriptContext sc;
  protected boolean haveReference;

  protected boolean hoverEnabled;

  protected long startTime;
  protected long targetTime;
  protected long lastRepaintTime;
  protected long currentTime;
  protected int sleepTime;
  
  protected boolean isJS;
  protected boolean stopped = false;
  protected boolean isReset;

  private boolean useTimeout = true;

  /**
   * @param manager  
   * @param vwr 
   * @param params 
   * @return TODO
   */
  public int setManager(Object manager, Viewer vwr, Object params){return 0;}
 
  public void setViewer(Viewer vwr, String name) {
    setName(name);
    this.name = name + "_" + (++threadIndex);
    this.vwr = vwr;
    isJS = vwr.isSingleThreaded;
  }
  
  abstract protected void run1(int mode) throws InterruptedException;

  /**
   * JavaScript only --
   * -- scriptDelay, moveTo, spin
   * -- save context for restoration later
   * -- move program counter forward one command
   * 
   * @param eval
   */
  public void setEval(JmolScriptEvaluator eval) {
    this.eval = eval;
    sc = vwr.getEvalContextAndHoldQueue(eval);
    if (sc != null)
      useTimeout = eval.getAllowJSThreads();
  }

  public void resumeEval() {
    if (eval == null || !isJS && !vwr.testAsync || !useTimeout)
      return;
    sc.mustResumeEval = !stopped;
    JmolScriptEvaluator eval = this.eval;
    ScriptContext sc = this.sc;
    this.eval = null;
    this.sc = null;
    /**
     * @j2sNative
     * 
     * setTimeout(function() { eval.resumeEval(sc); }, 1);
     * 
     */
    {
      eval.resumeEval(sc);
    }
  }
  
  @Override
  public synchronized void start() {
    if (isJS) {
      //Logger.info("starting " + name);
      run();
    } else {
      super.start();
    }
  }

  @Override
  public void run() {
    startTime = System.currentTimeMillis();
    try {
      run1(INIT); 
    } catch (InterruptedException e) {
      if (Logger.debugging  && !(this instanceof HoverWatcherThread))
        oops(e);
    } catch (Exception e) {
      oops(e);
    }
  }
  
  protected void oops(Exception e) {
    Logger.debug(name + " exception " + e);
    if (!vwr.isJS)
      e.printStackTrace();
    vwr.queueOnHold = false;
  }

  double junk;
  
  /**
   * 
   * @param millis  
   * @param runPtr
   * @return true if we can continue on with this thread (Java, not JavaScript)
   * @throws InterruptedException 
   *  
   */
  
  protected boolean runSleep(int millis, int runPtr) throws InterruptedException {
    if (isJS && !useTimeout) {
      //...but nothing will be shown anyway until the thread is finished.
      //long targetTime = System.currentTimeMillis() + millis; 
      //while(System.currentTimeMillis() < targetTime){
      //  junk = Math.random();
      //}      
      return true;
    }
    
    
   /**
    * @j2sNative
    * 
    * var me = this;
    * setTimeout(function(){me.run1(runPtr)}, Math.max(millis, 0));
    * return false;
    *  
    */
    {
      if (millis > 0) {
        try {
        Thread.sleep(millis);
        } catch (Exception e) {
          // ignore interruption
        }
      }
      return true;
    }
  }
  
  @Override
  public void interrupt() {
    stopped = true;
    vwr.startHoverWatcher(true);
    if (!isJS)
      super.interrupt();
  }
  
  protected boolean checkInterrupted(JmolThread ref) {
    if (haveReference && (ref == null || !ref.name.equals(name)))
      return true;
    /**
     * @j2sNative
     * 
     *            return this.stopped;
     */
    {
      return super.isInterrupted();
    }
  }
  
  public void reset() {
    isReset = true;
    interrupt();
  }

}
