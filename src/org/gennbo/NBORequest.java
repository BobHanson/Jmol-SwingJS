package org.gennbo;

import javajs.util.PT;

/**
 * A class to manage NBOServer requests. Manages file data that needs to 
 * be put in place prior to running the command and also keeps a handle 
 * to a Runnable that is to be run when the job is complete.
 * 
 */
class NBORequest {

//  Guiding logic:
//    
//
//    -- User attempts to switch tab model-run-view etc. while a request to NBOServe is pending.
//
//   Option 1: Jmol silently kills the pending request.
//
//   Option 2: Jmol notifies user that a request is in process and offers to NOT change modules.
//   "A request is already processing. Do you want to cancel that request and switch modules?"
//
//   Option 3: Jmol refuses request, notifying user that a request is in process, and we must wait for that to complete.
//   "A request is already processing. Please wait."
//
//
//   -- User makes second request in module while request is already pending.
//
//   Option 1: Jmol silently kills the pending request.
//
//   Option 2: Jmol notifies user that a request is in process and offers to NOT carry out this request.
//   "A request is already processing. Do you want to cancel that request?
//
//   Option 3: Jmol refuses request, notifying user that a request is in process, and we must wait for that to complete.
//   "A request is already processing. Please wait."
//
//   Option 4: Jmol queues request notifying user that a request is in  process, and this request will be processed as soon as the earlier request is completed.
//   "A request is already processing. Your request has been queued."
//
//   Option 5: Jmol asks the user what to do:
//
//   "A request is already processing. What would you like to do?"
//
//   [cancel pending request] [queue this request] [cancel this request]
//
//
//   - User attempts to close the NBO plug-in to return to Jmol proper.
//
//   Option 1: Jmol silently kills the pending request.
//
//   Option 2: Jmol notifies user that a request is in process and offers to NOT to quit NBO.
//
//   Option 3: NBO continues to process, just does so in the background.
//
//
//   - User attempts to close Jmol entirely.
//
//   Option 1: Jmol silently kills the pending request.
//
//   Option 2: Jmol notifies user that a request is in process and offers to NOT to quit Jmol.
//
//
//   I see how Jmol can kill NBOServe. But what if NBOServe is blocked at that  moment, waiting for a ray-tracer or gennbo job?
//
//   Q: Does the ray-tracer or gennbo job also die?
//   Q: If it does, won't we be left with incomplete or garbage files? What do we do about that?
//   Q: If it does not, what happens when, a few seconds later, we spawn a new one? Sounds dangerous to me.
//
//    Q: How do we determine easily exactly what the status of NBOServe is at any time? Where that status includes:
//
//   1 - initializing
//   2 - waiting for request
//   3 - processing request (expect *start*/*end* sequence soon)
//   4 - stopping (finalizing; cleaning up)
//   5 - dead
  
  
  
  
  
  
  
  /**
   * File data that needs to be written to disk at the time the 
   * job is run. An even-length array with odd entries file names (no directory)
   * and even entries the data: [filename,data,filename,data,...]
   * 
   * element 0 is the command file name sent to NBO as <xxxxx.xxx>
   * 
   * element 1 is the set of metacommands to be put in that file
   * 
   */
  String[] fileData;  
  
  /**
   * A few words that will be flashed on the bottom line of the nboOutput
   * panel while the request is being processed.
   * 
   */
  String statusInfo;
  
  /**
   * A method to be called when the reply is complete.
   */
  Runnable callbackMethod;
  
  /**
   * The reply from NBOServe
   * 
   */
  private String reply;
  
  /**
   * Just an ad hoc flag that indicates that we might expect some garbage 
   * before the ***start*** flag; used to disregard this preliminary info.
   * 
   */
  boolean isMessy;

  public long timeStamp;

  int dialogMode;

  NBORequest() {}
  
  void set(int dialogMode, Runnable returnMethod, boolean isMessy, String statusInfo, String... fileData) {
    this.dialogMode = dialogMode;
    this.isMessy = isMessy;
    this.fileData = fileData;
    this.statusInfo = statusInfo;
    this.callbackMethod = returnMethod;
    // need to flag this so that not all of sysout is returned
    // from either RUN or SEARCH
    //this.isMessy = isMessy;
  }

  /**
   * Set the reply and notify originator that it can pick it up.
   * 
   * @param reply
   */
  void sendReply(String reply) {
    
    System.out.println("CMD IS >>>>" + fileData[1] + "<<<<<");
    System.out.println("REPLY IS >>>>\n" + reply + "<<<<<");
    this.reply = reply;
    if (callbackMethod != null)
      callbackMethod.run();
  }
  
  String getReply() {
    return reply;
  }
  /**
   * 
   * @return reply lines separated into lines and trimmed of 
   * first and last blank lines.
   */
  public String[] getReplyLines() {
    int pt = reply.lastIndexOf("\n");
    if (pt > 0 &&  reply.lastIndexOf(" ") > pt)
      reply = reply.substring(0, pt + 1);
    return PT.trim(reply, "\n").split("\n");
  }
  
  @Override
  public String toString() {
    return "request was " + (fileData == null ? null : fileData[1]) + " reply was " + reply;
  }

}