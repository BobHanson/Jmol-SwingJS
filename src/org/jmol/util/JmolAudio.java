/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-26 23:35:44 -0500 (Fri, 26 Jun 2009) $
 * $Revision: 11131 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published byopst the Free Software Foundation; either
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
package org.jmol.util;

import java.util.Map;

import javajs.J2SIgnoreImport;
import javajs.util.AU;

import org.jmol.api.JmolAudioPlayer;
import org.jmol.viewer.Viewer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import sun.audio.AudioData;
import sun.audio.AudioDataStream;

@J2SIgnoreImport({ javax.sound.sampled.AudioInputStream.class,
    javax.sound.sampled.AudioSystem.class, javax.sound.sampled.Clip.class,
    javax.sound.sampled.Line.class, javax.sound.sampled.LineEvent.class,
    javax.sound.sampled.LineListener.class, sun.audio.AudioData.class,
    sun.audio.AudioDataStream.class })
public class JmolAudio implements javax.sound.sampled.LineListener, JmolAudioPlayer {

  public JmolAudio() {
  }

  private final static int MAX_LOOP = 10;
  private Map<String, Object> params;
  private Clip myClip;
  private String fileName;
  private Viewer vwr;
  private String id;
  private static int idCount;
  private boolean autoClose;

  /**
   * WAV only for application
   * 
   * @param vwr
   * @param htParams
   */
  public void playAudio(Viewer vwr, Map<String, Object> htParams) {
    try {
      id = (String) htParams.get("id");
      if (id == null || id.length() == 0) {
        autoClose = true;
        htParams.put("id", id = "audio" + ++idCount);
      }
      this.vwr = vwr;
      params = htParams;
      params.put("audioPlayer", this);
      fileName = (String) htParams.get("audioFile");
      vwr.sm.registerAudio(id, htParams);
      /**
       * @j2sNative
       * 
       * 
       *            Jmol._playAudio(vwr.html5Applet, htParams);
       */
      {
        getClip();
      }
      if (myClip == null)
        return;
      if (htParams.containsKey("action"))
        action((String) htParams.get("action"));
      else if (htParams.containsKey("loop")) {
        action("loop");
      } else {
        autoClose = true;
        action("start");
      }
    } catch (Exception e) {
      Logger.info("File " + fileName + " could not be opened as an audio file");
    }
  }

  /**
   * @j2sIgnore
   * 
   * @throws Exception
   * 
   */
  private void getClip() throws Exception {
    Object data = vwr.fm.getFileAsBytes(fileName, null);
    if (!AU.isAB(data)) {
      Logger.info("File " + fileName + " " + data);
      return;
    }
    myClip = (Clip) AudioSystem.getLine(new Line.Info(Clip.class));
    myClip.addLineListener(this);
    AudioInputStream ais = AudioSystem.getAudioInputStream(new AudioDataStream(
        new AudioData((byte[]) data)));
    myClip.open(ais);
  }

  @Override
  /**
   * @j2sIgnore
   */
  public void update(LineEvent le) {
    processUpdate(le.getType().toString());
  }

  public void processUpdate(String type) {
    Logger.info("audio id " + id + " " + fileName + " " + type);
    if (type == "open" || type == "Open") {
      params.put("status", "open");
    } else if (type == "play" || type == "Start") {
      params.put("status", "play");
    } else if (type == "pause" || type == "Stop") {
      params.put("status", "pause");
      if (autoClose) {
        myClip.close();
      }
    } else if (type == "ended" || type == "Close") {
      params.put("status", "ended");
    } else {
      params.put("status", type);
    }
    vwr.sm.notifyAudioStatus(params);
  }

  /**
   * Execute an action from load audio "xxxxx.wav" filter "id=a1 action=xxx"
   * 
   * 
   * 
   * @param action
   * 
   *        start -- back to the beginning and olay
   * 
   *        loop -- loop indefinitely (JavaScript) or up to 10 times (Java) and play
   * 
   *        play -- start from current position; opposite of PAUSE
   * 
   *        pause -- opposite of PLAY
   * 
   *        (not implemented: position-nn  reposition to nnn ms)  
   *        
   *        close -- close player
   * 
   * 
   */
  @Override
  public void action(String action) {
    if (myClip == null) {
      if (action == "kill")
        return;
      params.put("status", "ended");
      vwr.sm.notifyAudioStatus(params);
      return;
    }
    try {
      if ("start".equals(action)) {
        myClip.setMicrosecondPosition(0);
        myClip.loop(0);
        myClip.start();
        
      } else if ("loop".equals(action)) {
        myClip.setMicrosecondPosition(0);
        myClip.loop(MAX_LOOP);
        myClip.start();
        
      } else if ("pause".equals(action)) {
        if (myClip != null)
          myClip.stop();
        
      } else if ("play".equals(action)) {
        myClip.stop();
        myClip.start();
        
      } else if ("close".equals(action)) {
        myClip.close();
        
// did not work in Java.
//      } else if (action.startsWith("position-")) {
//        int n = Integer.parseInt(action.substring(9));
//        if (n >= 0)
//          clip.setMicrosecondPosition(n * 1000);
      }
    } catch (Throwable t) {
      // ignore
    }
  }
}
