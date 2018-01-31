/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-06-02 12:14:13 -0500 (Sat, 02 Jun 2007) $
 * $Revision: 7831 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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

package org.jmol.viewer;

import java.awt.Image;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.awt.AwtClipboard;

import javajs.util.OC;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.util.Logger;
import org.jmol.viewer.Viewer.ACCESS;

final public class OutputManagerAwt extends OutputManager {

  public OutputManagerAwt() {
    // by reflection only
  }

  @Override
  protected String getLogPath(String fileName) {
    return (vwr.isApplet ? fileName
        : (new File(fileName).getAbsolutePath()));
  }

  @Override
  String clipImageOrPasteText(String text) {
    String msg;
    try {
      if (text == null) {
        Image image = (Image) vwr.getScreenImageBuffer(null, true);
        AwtClipboard.setClipboard(image);
        msg = "OK image to clipboard: "
            + (image.getWidth(null) * image.getHeight(null));
      } else {
        AwtClipboard.setClipboard(text);
        msg = "OK text to clipboard: " + text.length();
      }
    } catch (Error er) {
      msg = vwr.getErrorMessage();
    } finally {
      if (text == null)
        vwr.releaseScreenImage();
    }
    return msg;
  }

  @Override
  String getClipboardText() {
    return AwtClipboard.getClipboardText();
  }

  @SuppressWarnings("resource")
  @Override
  OC openOutputChannel(double privateKey, String fileName,
                                      boolean asWriter, boolean asAppend)
      throws IOException {
    boolean isLocal = OC.isLocal(fileName);
    if (asAppend && isLocal && fileName.indexOf("JmolLog_") < 0)
      asAppend = false;
    return (fileName != null && !vwr.haveAccess(ACCESS.ALL)
        || !vwr.checkPrivateKey(privateKey) ? null
        : (new OC()).setParams(vwr.fm, fileName, asWriter,
            (isLocal ? new FileOutputStream(fileName, asAppend) : null)));
  }

  @Override
  protected String createSceneSet(String sceneFile, String type, int width,
                                int height) {
    String script0 = vwr.getFileAsString3(sceneFile, false, null);
    if (script0 == null)
      return "no such file: " + sceneFile;
    sceneFile = PT.rep(sceneFile, ".spt", "");
    String fileRoot = sceneFile;
    String fileExt = type.toLowerCase();
    String[] scenes = PT.split(script0, "pause scene ");
    Map<String, String> htScenes = new Hashtable<String, String>();
    Lst<Integer> list = new Lst<Integer>();
    String script = getSceneScript(scenes, htScenes, list);
    if (Logger.debugging)
      Logger.debug(script);
    script0 = PT.rep(script0, "pause scene", "delay "
        + vwr.am.lastFrameDelay + " # scene");
    String[] str = new String[] { script0, script, null };
    vwr.stm.saveState("_scene0");
    int nFiles = 0;
    if (scenes[0] != "")
      vwr.zap(true, true, false);
    int iSceneLast = -1;
    for (int i = 0; i < scenes.length - 1; i++) {
      try {
        int iScene = list.get(i).intValue();
        if (iScene > iSceneLast)
          vwr.showString("Creating Scene " + iScene, false);
        vwr.eval.runScript(scenes[i]);
        if (iScene <= iSceneLast)
          continue;
        iSceneLast = iScene;
        str[2] = "all"; // full PNGJ
        String fileName = fileRoot + "_scene_" + iScene + ".all." + fileExt;
        Map<String, Object> params = new Hashtable<String, Object>();
        params.put("fileName", fileName);
        params.put("type", "PNGJ");
        params.put("scripts", str);
        params.put("width", Integer.valueOf(width));
        params.put("height", Integer.valueOf(height));
        String msg = handleOutputToFile(params, false);
        str[0] = null; // script0 only saved in first file
        str[2] = "min"; // script only -- for fast loading
        fileName = fileRoot + "_scene_" + iScene + ".min." + fileExt;
        params.put("fileName", fileName);
        params.put("width", Integer.valueOf(Math.min(width, 200)));
        params.put("height", Integer.valueOf(Math.min(height, 200)));
        msg += "\n" + handleOutputToFile(params, false);
        vwr.showString(msg, false);
        nFiles += 2;
      } catch (Exception e) {
        return "script error " + e.toString();
      }
    }
    try {
      vwr.eval.runScript(vwr.stm.getSavedState("_scene0"));
    } catch (Exception e) {
      // ignore
    }
    return "OK " + nFiles + " files created";
  }

  private String getSceneScript(String[] scenes, Map<String, String> htScenes,
                                Lst<Integer> list) {
    // no ".spt.png" -- that's for the sceneScript-only version
    // that we will create here.
    // extract scenes based on "pause scene ..." commands
    int iSceneLast = 0;
    int iScene = 0;
    SB sceneScript = new SB().append(SCENE_TAG).append(
        " Jmol ").append(Viewer.getJmolVersion()).append(
        "\n{\nsceneScripts={");
    for (int i = 1; i < scenes.length; i++) {
      scenes[i - 1] = PT.trim(scenes[i - 1], "\t\n\r ");
      int[] pt = new int[1];
      iScene = PT.parseIntNext(scenes[i], pt);
      if (iScene == Integer.MIN_VALUE)
        return "bad scene ID: " + iScene;
      scenes[i] = scenes[i].substring(pt[0]);
      list.addLast(Integer.valueOf(iScene));
      String key = iSceneLast + "-" + iScene;
      htScenes.put(key, scenes[i - 1]);
      if (i > 1)
        sceneScript.append(",");
      sceneScript.appendC('\n').append(PT.esc(key)).append(": ")
          .append(PT.esc(scenes[i - 1]));
      iSceneLast = iScene;
    }
    sceneScript.append("\n}\n");
    if (list.size() == 0)
      return "no lines 'pause scene n'";
    sceneScript
         .append("\nthisSceneRoot = '$SCRIPT_PATH$'.split('_scene_')[1];\n")
         .append(
             "thisSceneID = 0 + ('$SCRIPT_PATH$'.split('_scene_')[2]).split('.')[1];\n")
         .append(
             "var thisSceneState = '$SCRIPT_PATH$'.replace('.min.png','.all.png') + 'state.spt';\n")
         .append("var spath = ''+currentSceneID+'-'+thisSceneID;\n")
         .append("print thisSceneRoot + ' ' + spath;\n")
         .append("var sscript = sceneScripts[spath];\n")
         .append("var isOK = true;\n")
         .append("try{\n")
         .append("if (thisSceneRoot != currentSceneRoot){\n")
         .append(" isOK = false;\n")
         .append("} else if (sscript != '') {\n")
         .append(" isOK = true;\n")
         .append("} else if (thisSceneID <= currentSceneID){\n")
         .append(" isOK = false;\n")
         .append("} else {\n")
         .append(" sscript = '';\n")
         .append(" for (var i = currentSceneID; i < thisSceneID; i++){\n")
         .append(
             "  var key = ''+i+'-'+(i + 1); var script = sceneScripts[key];\n")
         .append("  if (script = '') {isOK = false;break;}\n")
         .append("  sscript += ';'+script;\n")
         .append(" }\n")
         .append("}\n}catch(e){print e;isOK = false}\n")
         .append(
             "if (isOK) {"
                 + wrapPathForAllFiles("script inline @sscript",
                     "print e;isOK = false") + "}\n")
         .append("if (!isOK){script @thisSceneState}\n")
         .append(
             "currentSceneRoot = thisSceneRoot; currentSceneID = thisSceneID;\n}\n");
    return sceneScript.toString();
  }
}
