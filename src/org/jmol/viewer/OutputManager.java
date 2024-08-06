package org.jmol.viewer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.i18n.GT;
import org.jmol.script.T;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer.ACCESS;

import javajs.api.GenericImageEncoder;
import javajs.util.AU;
import javajs.util.BS;
import javajs.util.Lst;
import javajs.util.OC;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;
import javajs.util.ZipTools;

abstract class OutputManager {

  abstract protected String getLogPath(String fileName);

  abstract String clipImageOrPasteText(String text);

  abstract String getClipboardText();

  abstract OC openOutputChannel(double privateKey, String fileName,
                                boolean asWriter, boolean asAppend)
      throws IOException;

  abstract protected String createSceneSet(String sceneFile, String type,
                                           int width, int height);

  protected Viewer vwr;
  protected double privateKey;

  OutputManager setViewer(Viewer vwr, double privateKey) {
    this.vwr = vwr;
    this.privateKey = privateKey;
    return this;
  }

  /**
   * From handleOutputToFile, write text, byte[], or image data to a file;
   * 
   * @param params
   * @return null (canceled) or byte[] or String message starting with OK or an
   *         error message; in the case of params.image != null, return the
   *         fileName
   */

  private String writeToOutputChannel(Map<String, Object> params) {
    String type = (String) params.get("type");
    String fileName = (String) params.get("fileName");
    String text = (String) params.get("text");
    byte[] bytes = (byte[]) params.get("bytes");
    int quality = getInt(params, "quality", Integer.MIN_VALUE);
    OC out = (OC) params.get("outputChannel");
    boolean closeStream = (out == null);
    int len = -1;
    String ret = null;
    try {
      if (!vwr.checkPrivateKey(privateKey))
        return "ERROR: SECURITY";
      if (bytes != null) {
        if (out == null)
          out = openOutputChannel(privateKey, fileName, false, false);
        out.write(bytes, 0, bytes.length);
      } else if (text != null && !type.equals("ZIPDATA")
          && !type.equals("BINARY")) {
        if (out == null)
          out = openOutputChannel(privateKey, fileName, true, false);
        out.append(text);
      } else {
        String errMsg = (String) getOrSaveImage(params);
        if (errMsg != null)
          return errMsg;
        len = ((Integer) params.get("byteCount")).intValue();
      }
    } catch (Exception exc) {
      Logger.errorEx("IO Exception", exc);
      return exc.toString();
    } finally {
      if (out != null) {
        if (closeStream)
          ret = out.closeChannel();
        len = out.getByteCount();
      }
    }
    int pt = fileName.indexOf("?POST?");
    if (pt >= 0)
      fileName = fileName.substring(0, pt);
    return (len < 0
        ? "Creation of " + fileName + " failed: "
            + (ret == null ? vwr.getErrorMessageUn() : ret)
        : "OK " + type + " " + (len > 0 ? len + " " : "") + fileName
            + (quality == Integer.MIN_VALUE ? "" : "; quality=" + quality));
  }

  /**
   * 
   * Creates an image of params.type form -- PNG, PNGJ, PNGT, JPG, JPG64, PDF,
   * PPM, GIF, GIFT.
   * 
   * From createImage and getImageAsBytes
   * 
   * @param params
   *        include fileName, type, text, bytes, image, scripts, appendix,
   *        quality, outputStream, and type-specific parameters. If
   *        params.outputChannel != null, then we are passing back the data, and
   *        the channel will not be closed.
   * 
   * @return bytes[] if params.fileName==null and params.outputChannel==null
   *         otherwise, return a message string or null
   * @throws Exception
   * 
   */

  private Object getOrSaveImage(Map<String, Object> params) throws Exception {
    byte[] bytes = null;
    String errMsg = null;
    String type = ((String) params.get("type")).toUpperCase();
    String fileName = (String) params.get("fileName");
    String[] scripts = (String[]) params.get("scripts");
    Object objImage = params.get("image");
    int[] rgbbuf = (int[]) params.get("rgbbuf"); // only from OBJ exporter creating PNG output
    OC out = (OC) params.get("outputChannel");
    boolean asBytes = (out == null && fileName == null);
    boolean closeChannel = (out == null && fileName != null);
    boolean releaseImage = (objImage == null);
    Object image = (type.equals("BINARY") || type.equals("ZIPDATA") ? ""
        : rgbbuf != null ? rgbbuf
            : objImage != null ? objImage : vwr.getScreenImage());
    boolean isOK = false;
    try {
      if (image == null)
        return errMsg = vwr.getErrorMessage();
      if (fileName != null && fileName.startsWith("\1")) {
        isOK = true;
        Map<String, Object> info = new Hashtable<String, Object>();
        info.put("_IMAGE_", image);
        vwr.fm.loadImage(info, fileName, false);
        return errMsg = "OK - viewing " + fileName.substring(1);
      }
      boolean isPngj = type.equals("PNGJ");
      if (!isPngj) {
        if (out == null && (out = openOutputChannel(privateKey, fileName, false,
            false)) == null)
          return errMsg = "ERROR: canceled";
        fileName = out.getFileName();
      }
      String comment = null;
      Object stateData = null;
      params.put("date", vwr.apiPlatform.getDateFormat("8601"));
      if (type.startsWith("JP")) {
        type = PT.rep(type, "E", "");
        if (type.equals("JPG64")) {
          params.put("outputChannelTemp", getOutputChannel(null, null));
          comment = "";
        } else {
          comment = (!asBytes
              ? (String) getWrappedState(null, null, image, null)
              : "");
        }
        params.put("jpgAppTag", FileManager.JPEG_CONTINUE_STRING);
      } else if (type.equals("PDF")) {
        comment = "";
      } else if (type.startsWith("PNG")) {
        comment = "";
        if (isPngj) {// get zip file data
          OC outTemp = getOutputChannel(null, null);
          getWrappedState(fileName, scripts, image, outTemp);
          stateData = outTemp.toByteArray();
          if (out == null && (out = openOutputChannel(privateKey, fileName,
              false, false)) == null)
            return errMsg = "ERROR: canceled";
        } else if (rgbbuf == null && !asBytes
            && !params.containsKey("captureMode")) {
          stateData = ((String) getWrappedState(null, scripts, image, null))
              .getBytes();
        }
        if (stateData != null) {
          params.put("pngAppData", stateData);
          params.put("pngAppPrefix", "Jmol Type");
        }
      }
      if (type.equals("PNGT") || type.equals("GIFT"))
        params.put("transparentColor",
            Integer.valueOf(vwr.getBackgroundArgb()));
      if (type.length() == 4) // PNGT PNGJ GIFT
        type = type.substring(0, 3);
      if (comment != null)
        params.put("comment",
            comment.length() == 0 ? Viewer.getJmolVersion() : comment);
      String[] errRet = new String[1];
      isOK = createTheImage(image, type, out, params, errRet);
      if (isOK && errRet[0] == "async") {
        errRet[0] = "OK: bytes written asynchronously";
        closeChannel = false;
      }
      if (closeChannel)
        out.closeChannel();
      if (isOK) {
        if (params.containsKey("captureMsg")
            && !params.containsKey("captureSilent"))
          vwr.prompt((String) params.get("captureMsg"), "OK", null, true);
        if (asBytes)
          bytes = out.toByteArray();
        else if (params.containsKey("captureByteCount"))
          errMsg = "OK: " + params.get("captureByteCount").toString()
              + " bytes";
      } else {
        errMsg = errRet[0];
      }
    } finally {
      if (releaseImage)
        vwr.releaseScreenImage();
      if (bytes != null || out != null)
        params.put("byteCount", Integer.valueOf(
            bytes != null ? bytes.length : isOK ? out.getByteCount() : -1));
      if (objImage != null) {
        // _ObjExport is saving the texture file -- just return file name, regardless of whether there is an error
        return fileName;
      }
    }
    return (errMsg == null ? bytes : errMsg);
  }

  /**
   * @param pngjName
   * @param scripts
   * @param objImage
   * @param pgjOut
   * @return either byte[] (a full ZIP file) or String (just an embedded state
   *         script)
   * 
   */

  Object getWrappedState(String pngjName, String[] scripts, Object objImage,
                         OC pgjOut) {
    int width = vwr.apiPlatform.getImageWidth(objImage);
    int height = vwr.apiPlatform.getImageHeight(objImage);
    if (width > 0 && !vwr.g.imageState && pgjOut == null
        || !vwr.g.preserveState)
      return "";
    String s = vwr.getStateInfo3(null, width, height);
    if (pgjOut != null) {
      // when writing a file, we need to make sure
      // the pngj cache for that file is updated
      return createZipSet(s, scripts, true, pgjOut, pngjName);
    }
    // we remove local file references in the embedded states for images
    try {
      s = JC
          .embedScript(FileManager.setScriptFileReferences(s, ".", null, null));
    } catch (Throwable e) {
      // ignore if this uses too much memory
      Logger.error("state could not be saved: " + e.toString());
      s = "Jmol " + Viewer.getJmolVersion();
    }
    return s;
  }

  /**
   * @param objImage
   * @param type
   * @param out
   * @param params
   * @param errRet
   * @return byte array if needed
   * @throws Exception
   */
  private boolean createTheImage(Object objImage, String type, OC out,
                                 Map<String, Object> params, String[] errRet)
      throws Exception {
    type = type.substring(0, 1) + type.substring(1).toLowerCase();
    boolean isZipData = type.equals("Zipdata");
    if (isZipData || type.equals("Binary")) {
      @SuppressWarnings("unchecked")
      Lst<Object> v = (Lst<Object>) params.get("imageData");
      if (v.size() >= 2 && v.get(0).equals("_IMAGE_")) {
        if (isZipData) {
          errRet[0] = writeZipFile(out, v, "OK JMOL", null);
          return true;
        }
        objImage = null;
        v.removeItemAt(0);
        v.removeItemAt(0); // also "_IMAGE_"
        byte[] bytes = (byte[]) v.removeItemAt(0);
        OC oz = getOutputChannel(null, null);
        errRet[0] = writeZipFile(oz, v, "OK JMOL", null);
        params.put("pngAppData", oz.toByteArray());
        params.put("type", "PNGJ");
        type = "Png";
        params.put("pngAppPrefix", "Jmol Type");

        if (Rdr.isPngZipB(bytes)) {
          params.put("pngImgData", bytes);
        } else {
          // bytes will not be ready yet in JavaScript
          Object image = vwr.fm.getImage(bytes, null, true);
          Runnable r = new Runnable() {

            @Override
            public void run() {
              try {
                getImagePixels(image, params);
              } catch (Exception e) {
                e.printStackTrace();
              }
              finishImage(errRet, "Png", out, null, params);
              out.closeChannel();
            }

          };
          if (Viewer.isJS) {
            errRet[0] = "async";
            new Thread(r).start();
            return true;
          }
          r.run();
          return errRet[0] == null;
        }
      } else if (v.size() == 1) {
        byte[] b = (byte[]) v.removeItemAt(0);
        out.write(b, 0, b.length);
        return true;
      } else {
        errRet[0] = writeZipFile(out, v, "OK JMOL", null);
        return true;
      }
    }
    finishImage(errRet, type, out, objImage, params);
    return errRet[0] == null;
  }

  void finishImage(String[] errRet, String type, OC out,
                           Object objImage, Map<String, Object> params) {
    GenericImageEncoder ie = (GenericImageEncoder) Interface
        .getInterface("javajs.img." + type + "Encoder", vwr, "file");
    if (ie == null) {
      errRet[0] = "Image encoder type " + type + " not available";
      return;
    }
    boolean doClose = true;
    try {
      if (type.equals("Gif") && vwr.getBoolean(T.testflag2))
        params.put("reducedColors", Boolean.TRUE);
      if (params.get("imagePixels") == null)
        getImagePixels(objImage, params);
      params.put("logging", Boolean.valueOf(Logger.debugging));
      // GIF capture may not close output channel
      doClose = ie.createImage(type, out, params);
    } catch (Exception e) {
      errRet[0] = e.toString();
      out.cancel();
      doClose = true;
    } finally {
      if (doClose)
        out.closeChannel();
    }
  }

  void getImagePixels(Object objImage, Map<String, Object> params)
      throws Exception {
    int w = objImage == null ? -1
        : AU.isAI(objImage) ? ((Integer) params.get("width")).intValue()
            : vwr.apiPlatform.getImageWidth(objImage);
    int h = objImage == null ? -1
        : AU.isAI(objImage) ? ((Integer) params.get("height")).intValue()
            : vwr.apiPlatform.getImageHeight(objImage);
    params.put("imageWidth", Integer.valueOf(w));
    params.put("imageHeight", Integer.valueOf(h));
    int[] pixels = encodeImage(w, h, objImage);
    if (pixels != null)
      params.put("imagePixels", pixels);
  }

  /**
   * general image encoder, allows for BufferedImage, int[], or HTML5 2D canvas
   * 
   * @param w
   * @param h
   * @param objImage
   * @return linear int[] array of ARGB values
   * @throws Exception
   */
  private int[] encodeImage(int w, int h, Object objImage) throws Exception {
    return (w < 0 ? null
        : (AU.isAI(objImage) ? (int[]) objImage
            : vwr.apiPlatform.grabPixels(objImage, w, h,
                (!Viewer.isJS || Viewer.isSwingJS ? new int[w * h] : null))));
  }

  /////////////////////// general output including logging //////////////////////

  String outputToFile(Map<String, Object> params) {
    return handleOutputToFile(params, true);
  }

  OC getOutputChannel(String fileName, String[] fullPath) {
    if (!vwr.haveAccess(ACCESS.ALL))
      return null;
    boolean isRemote = OC.isRemote(fileName);
    if (fileName != null && !isRemote && !fileName.startsWith("cache://")) {
      fileName = getOutputFileNameFromDialog(fileName, Integer.MIN_VALUE, null);
      if (fileName == null)
        return null;
    }
    if (fullPath != null)
      fullPath[0] = fileName;
    try {
      return openOutputChannel(privateKey, fileName, false, false);
    } catch (IOException e) {
      Logger.info(e.toString());
      return null;
    }
  }

  /////////////////////// WRITE and CAPTURE command processing /////////////

  /**
   * 
   * @param params
   *        include fileName, type, text, bytes, scripts, quality, width,
   *        height, bsFrames, nVibes, fullPath
   * @return message
   */

  String processWriteOrCapture(Map<String, Object> params) {
    String fileName = (String) params.get("fileName");
    if (fileName == null)
      return vwr.clipImageOrPasteText((String) params.get("text"));
    BS bsFrames = (BS) params.get("bsFrames");
    int nVibes = getInt(params, "nVibes", 0);
    return (bsFrames != null || nVibes != 0
        ? processMultiFrameOutput(fileName, bsFrames, nVibes, params)
        : handleOutputToFile(params, true));
  }

  private static int getInt(Map<String, Object> params, String key, int def) {
    Integer p = (Integer) params.get(key);
    return (p == null ? def : p.intValue());
  }

  private String processMultiFrameOutput(String fileName, BS bsFrames,
                                         int nVibes,
                                         Map<String, Object> params) {
    String info = "";
    int n = 0;
    int quality = getInt(params, "quality", -1);
    fileName = setFullPath(params,
        getOutputFileNameFromDialog(fileName, quality, null));
    if (fileName == null)
      return null;
    String[] rootExt = new String[2];
    getRootExt(fileName, rootExt, 0);
    SB sb = new SB();
    if (bsFrames == null) {
      vwr.tm.vibrationOn = true;
      sb = new SB();
      for (int i = 0; i < nVibes; i++) {
        for (int j = 0; j < 20; j++) {
          vwr.tm.setVibrationT(j / 20d + 0.25d);
          if (!writeFrame(++n, rootExt, params, sb))
            return "ERROR WRITING FILE SET: \n" + info;
        }
      }
      vwr.tm.setVibrationPeriod(0);
    } else {
      for (int i = bsFrames.nextSetBit(0); i >= 0; i = bsFrames
          .nextSetBit(i + 1)) {
        vwr.setCurrentModelIndex(i);
        if (!writeFrame(++n, rootExt, params, sb))
          return "ERROR WRITING FILE SET: \n" + info;
      }
    }
    if (info.length() == 0)
      info = "OK\n";
    return info + "\n" + n + " files created";
  }

  private static Object getRootExt(String fileName, String[] rootExt, int n) {
    if (fileName == null) {
      fileName = "0000" + n;
      return rootExt[0] + fileName.substring(fileName.length() - 4)
          + rootExt[1];
    }
    int ptDot = fileName.lastIndexOf(".");
    if (ptDot < 0)
      ptDot = fileName.length();
    String froot = fileName.substring(0, ptDot);
    if (froot.endsWith("0"))
      froot = PT.trim(froot, "0");
    rootExt[0] = froot;
    rootExt[1] = fileName.substring(ptDot);
    return rootExt;
  }

  private String setFullPath(Map<String, Object> params, String fileName) {
    String[] fullPath = (String[]) params.get("fullPath");
    if (fullPath != null)
      fullPath[0] = fileName;
    if (fileName == null)
      return null;
    params.put("fileName", fileName);
    return fileName;
  }

  String getOutputFromExport(Map<String, Object> params) {
    int width = getInt(params, "width", 0);
    int height = getInt(params, "height", 0);
    String fileName = (String) params.get("fileName");
    if (fileName != null) {
      fileName = setFullPath(params,
          getOutputFileNameFromDialog(fileName, Integer.MIN_VALUE, null));
      if (fileName == null)
        return null;
    }
    vwr.mustRender = true;
    int saveWidth = vwr.dimScreen.width;
    int saveHeight = vwr.dimScreen.height;
    vwr.resizeImage(width, height, true, true, false);
    vwr.setModelVisibility();
    String data = vwr.rm.renderExport(vwr.gdata, vwr.ms, params);
    vwr.resizeImage(saveWidth, saveHeight, true, true, true);
    return data;
  }

  /**
   * Called when a simple image is required -- from x=getProperty("image") or
   * for a simple preview PNG image for inclusion in a ZIP file from write
   * xxx.zip or xxx.jmol, or for a PNGJ or PNG image that is being posted
   * because of a URL that contains "?POST?_PNG_" or ?POST?_PNGJ_" or
   * ?POST?_PNGJBIN_".
   * 
   * @param type
   * @param width
   * @param height
   * @param quality
   * @param errMsg
   * @return image bytes or, if an error, null and an error message
   */

  byte[] getImageAsBytes(String type, int width, int height, int quality,
                         String[] errMsg) {
    int saveWidth = vwr.dimScreen.width;
    int saveHeight = vwr.dimScreen.height;
    vwr.mustRender = true;
    vwr.resizeImage(width, height, true, false, false);
    vwr.setModelVisibility();
    vwr.creatingImage = true;
    byte[] bytes = null;
    try {
      Map<String, Object> params = new Hashtable<String, Object>();
      params.put("type", type);
      if (quality > 0)
        params.put("quality", Integer.valueOf(quality));
      Object bytesOrError = getOrSaveImage(params);
      if (bytesOrError instanceof String)
        errMsg[0] = (String) bytesOrError;
      else
        bytes = (byte[]) bytesOrError;
    } catch (Exception e) {
      errMsg[0] = e.toString();
      vwr.setErrorMessage("Error creating image: " + e, null);
    } catch (Error er) {
      vwr.handleError(er, false);
      vwr.setErrorMessage("Error creating image: " + er, null);
      errMsg[0] = vwr.getErrorMessage();
    }
    vwr.creatingImage = false;
    vwr.resizeImage(saveWidth, saveHeight, true, false, true);
    return bytes;
  }

  /**
   * Generates file data and passes it on either to a FileOuputStream (Java) or
   * via POSTing to a url using a ByteOutputStream (JavaScript)
   * 
   * @param fileName
   * @param type
   *        one of: PDB PQR FILE PLOT
   * @param modelIndex for PLOT or PDB or PQR only
   * @param plotParameters
   * @return "OK..." or "" or null
   * 
   */

  String writeFileData(String fileName, String type, int modelIndex,
                       Object[] plotParameters) {
    String[] fullPath = new String[1];
    OC out = getOutputChannel(fileName, fullPath);
    if (out == null)
      return "";
    fileName = fullPath[0];
    String pathName = (type.equals("FILE") ? (String) vwr.getParameter("_modelFile")
        : null);
    boolean getStringData = (pathName != null && (pathName.equals("string")
        || pathName.equals("String[]") || pathName.equals("JSNode")));
    boolean asBytes = (pathName != null && !getStringData);
    if (asBytes) {
      if (vwr.getModelSetPathName() == null)
        return null; // zapped
    }
    // The OutputStringBuilder allows us to create strings or byte arrays
    // of a given type, passing just one parameter and maintaining an 
    // output stream all along. For JavaScript, this will be a ByteArrayOutputStream
    // which will then be posted to a server for a return that allows saving.
    out.setType(type);
    String msg = (type.startsWith("PDB")
        ? vwr.getPdbAtomData(null, out, false, false)
        : type.startsWith("PLOT")
            ? vwr.getPdbData(modelIndex, type.substring(5), null,
                plotParameters, out, true)
            : getStringData
                ? out.append(vwr.getCurrentFileAsString("write")).toString()
                : (String) vwr.fm.getFileAsBytes(pathName, out));
    out.closeChannel();
    if (msg != null)
      msg = "OK " + msg + " " + fileName;
    return msg;
  }

  private boolean writeFrame(int n, String[] rootExt,
                             Map<String, Object> params, SB sb) {
    String fileName = (String) getRootExt(null, rootExt, n);
    fileName = setFullPath(params, fileName);
    if (fileName == null)
      return false;
    String msg = handleOutputToFile(params, false);
    vwr.scriptEcho(msg);
    sb.append(msg).append("\n");
    return msg.startsWith("OK");
  }

  private String getOutputFileNameFromDialog(String fileName, int quality,
                                             Map<String, Object> params) {
    if (fileName == null || vwr.isKiosk)
      return null;
    boolean useDialog = fileName.startsWith("?");
    if (useDialog)
      fileName = fileName.substring(1);
    useDialog |= (vwr.isApplet && fileName.indexOf("http:") != 0
        && fileName.indexOf("https:") != 0);
    fileName = FileManager.getLocalPathForWritingFile(vwr, fileName, useDialog);
    if (useDialog)
      fileName = vwr.dialogAsk(
          quality == Integer.MIN_VALUE ? "Save" : "Save Image", fileName,
          params);
    return fileName;
  }

  /**
   * general routine for creating an image or writing data to a file
   * 
   * passes request to statusManager to pass along to app or applet
   * jmolStatusListener interface
   * 
   * @param params
   *        include: fileName: starts with ? --> use file dialog; type: PNG,
   *        JPG, etc.; text: String to output; bytes: byte[] or null if an
   *        image; scripts for scenes; quality: for JPG and PNG; width: image
   *        width; height: image height; fullPath: String[] return
   * 
   * @param doCheck
   * @return null (canceled) or a message starting with OK or an error message
   */
  protected String handleOutputToFile(Map<String, Object> params,
                                      boolean doCheck) {

    // org.jmol.image.AviCreator does create AVI animations from JPEGs
    //but these aren't read by standard readers, so that's pretty much useless.

    String fileName = (String) params.get("fileName");
    OC out = (OC) params.get("outputChannel");
    if (fileName == null && out == null)
      return null;
    String sret = null;
    String type = (String) params.get("type");
    String text = (String) params.get("text");
    int width = getInt(params, "width", 0);
    int height = getInt(params, "height", 0);
    int saveWidth = 0, saveHeight = 0;
    int quality = getInt(params, "quality", Integer.MIN_VALUE);
    String captureMode = (String) params.get("captureMode");
    boolean is2D = params.get("is2D") == Boolean.TRUE;
    String localName = null;
    if (captureMode != null && !vwr.allowCapture())
      return "ERROR: Cannot capture on this platform.";
    boolean mustRender = (!is2D && quality != Integer.MIN_VALUE);
    // localName will be fileName only if we are able to write to disk.
    if (captureMode != null) {
      doCheck = false; // will be checked later
      mustRender = false;
    }
    if (out == null) {
      if (!fileName.startsWith("\1")) {
        if (doCheck)
          fileName = getOutputFileNameFromDialog(fileName, quality, params);
        fileName = setFullPath(params, fileName);
      }
      if (fileName == null)
        return null;
      params.put("fileName", fileName);
      // JSmol/HTML5 WILL produce a localName now
      if (OC.isLocal(fileName))
        localName = fileName;
      saveWidth = vwr.dimScreen.width;
      saveHeight = vwr.dimScreen.height;
      vwr.creatingImage = true;
      if (mustRender) {
        vwr.mustRender = true;
        vwr.resizeImage(width, height, true, false, false);
        vwr.setModelVisibility();
      }
    }
    try {
      if (type.equals("JMOL"))
        type = "ZIPALL";
      if (type.equals("ZIP") || type.equals("ZIPALL")) {
        String[] scripts = (String[]) params.get("scripts");
        if (scripts != null && type.equals("ZIP"))
          type = "ZIPALL";
        sret = createZipSet(text, scripts, type.equals("ZIPALL"),
            out == null ? getOutputChannel(fileName, null) : out, null);
      } else if (type.equals("SCENE")) {
        sret = createSceneSet(fileName, text, width, height);
      } else {
        // see if application wants to do it (returns non-null String)
        // both Jmol application and applet return null
        byte[] bytes = (byte[]) params.get("bytes");
        // String return here
        sret = vwr.sm.createImage(fileName, type, text, bytes, quality);
        if (sret == null) {
          boolean createImage = true;
          // allow Jmol to do it            
          String captureMsg = null;
          if (captureMode != null) {
            out = null;
            Map<String, Object> cparams = vwr.captureParams;
            int imode = "ad on of en ca mo "
                .indexOf(captureMode.substring(0, 2));
            //           0  3  6  9  12 15
            String[] rootExt;
            if (imode == 15) {// movie -- start up
              if (cparams != null && cparams.containsKey("outputChannel"))
                ((OC) cparams.get("outputChannel")).closeChannel();
              boolean streaming = params.containsKey("streaming");
              if (streaming
                  && (out = getOutputChannel(localName, null)) == null) {
                sret = captureMsg = "ERROR: capture canceled";
                vwr.captureParams = null;
              } else {
                vwr.captureParams = params;
                if (params.containsKey("captureRootExt")) {
                  imode = 0; // add
                } else {
                  if (out != null)
                    localName = out.getFileName();
                  params.put("captureFileName", localName);
                  if (streaming) {
                    captureMsg = type + "_STREAM_OPEN " + localName;
                    params.put("captureMode", "movie");
                  } else {
                    rootExt = new String[2];
                    params.put("captureRootExt",
                        getRootExt(localName, rootExt, 0));
                    localName = (String) getRootExt(null, rootExt, 1);
                    imode = -1; // ignore
                    cparams = params;
                    createImage = false;
                  }
                }
                if (!params.containsKey("captureCount"))
                  params.put("captureCount", Integer.valueOf(0));
              }
            }
            if (imode >= 0 && imode != 15) {
              if (cparams == null) {
                sret = captureMsg = "ERROR: capture not active";
              } else {
                params = cparams;
                switch (imode) {
                default:
                  sret = captureMsg = "ERROR: CAPTURE MODE=" + captureMode
                      + "?";
                  break;
                case 0: //add:
                  if (Boolean.FALSE == params.get("captureEnabled")) {
                    sret = captureMsg = "capturing OFF; use CAPTURE ON/END/CANCEL to continue";
                  } else {
                    int count = getInt(params, "captureCount", 0);
                    params.put("captureCount", Integer.valueOf(++count));
                    if ((rootExt = (String[]) params
                        .get("captureRootExt")) != null) {
                      localName = (String) getRootExt(null, rootExt, count);
                      captureMsg = null;
                      createImage = true;
                      //out = (OC) params.get("outputChannel");
                      //if (out != null)
                      //  out.closeChannel();
                      //out = getOutputChannel(localName, null);
                      //out = null;
                    } else {
                      captureMsg = type + "_STREAM_ADD " + count;
                    }
                  }
                  break;
                case 3: //on:
                case 6: //off:
                  params = cparams;
                  params.put("captureEnabled",
                      (captureMode.equals("on") ? Boolean.TRUE
                          : Boolean.FALSE));
                  sret = type + "_STREAM_"
                      + (captureMode.equals("on") ? "ON" : "OFF");
                  params.put("captureMode", "add");
                  break;
                case 9:// end:
                case 12:// cancel:
                  params = cparams;
                  params.put("captureMode", captureMode);
                  fileName = (String) params.get("captureFileName");
                  captureMsg = type + "_STREAM_"
                      + (captureMode.equals("end") ? "CLOSE " : "CANCEL ")
                      + fileName;
                  vwr.captureParams = null;
                  params.put("captureMsg",
                      GT.$("Capture") + ": "
                          + (captureMode.equals("cancel") ? GT.$("canceled")
                              : GT.o(GT.$("{0} saved"), fileName)));
                  if (params.containsKey("captureRootExt"))
                    createImage = false;
                  break;
                }
              }
            }
            if (createImage && out != null)
              params.put("outputChannel", out);
          }
          if (createImage) {
            if (localName != null)
              params.put("fileName", localName);
            if (sret == null)
              sret = writeToOutputChannel(params);
            if (!is2D) {
              vwr.sm.createImage(sret, type, null, null, quality);
              if (captureMode != null) {
                if (captureMsg == null)
                  captureMsg = sret;
                else
                  captureMsg += " ("
                      + params.get(params.containsKey("captureByteCount")
                          ? "captureByteCount"
                          : "byteCount")
                      + " bytes)";
              }
              if (captureMsg != null) {
                vwr.showString(captureMsg, false);
              }
            }
          }
        }
      }
    } catch (Throwable er) {
      er.printStackTrace();
      Logger.error(
          vwr.setErrorMessage(sret = "ERROR creating image??: " + er, null));
    } finally {
      vwr.creatingImage = false;
      if (quality != Integer.MIN_VALUE && saveWidth > 0)
        vwr.resizeImage(saveWidth, saveHeight, true, false, true);
    }
    return sret;
  }

  String setLogFile(String value) {
    String path = null;
    /**
     * @j2sNative
     * 
     *            if (typeof value == "function") path = value;
     * 
     */
    if (vwr.logFilePath == null || value.indexOf("\\") >= 0) {
      value = null;
    } else if (value.startsWith("http://") || value.startsWith("https://")) {
      // allow for remote logging
      path = value;
    } else if (value.indexOf("/") >= 0) {
      value = null;
    } else if (value.length() > 0) {
      if (!value.startsWith("JmolLog_"))
        value = "JmolLog_" + value;
      path = getLogPath(vwr.logFilePath + value);
    }
    if (path == null)
      value = null;
    else
      Logger.info(GT.o(GT.$("Setting log file to {0}"), path));
    if (value == null || !vwr.haveAccess(ACCESS.ALL)) {
      Logger.info(GT.$("Cannot set log file path."));
      value = null;
    } else {
      vwr.logFileName = path;
      vwr.g.setO("_logFile", vwr.isApplet ? value : path);
    }
    return value;
  }

  void logToFile(String data) {
    try {
      boolean doClear = (data.equals("$CLEAR$"));
      if (data.indexOf("$NOW$") >= 0)
        data = PT.rep(data, "$NOW$", vwr.apiPlatform.getDateFormat(null));
      if (vwr.logFileName == null) {
        Logger.info(data);
        return;
      }
      @SuppressWarnings("resource")
      OC out = (vwr.haveAccess(ACCESS.ALL)
          ? openOutputChannel(privateKey, vwr.logFileName, true, !doClear)
          : null);
      if (!doClear) {
        int ptEnd = data.indexOf('\0');
        if (ptEnd >= 0)
          data = data.substring(0, ptEnd);
        out.append(data);
        if (ptEnd < 0)
          out.append("\n");
      }
      String s = out.closeChannel();
      Logger.info(s);
    } catch (Exception e) {
      if (Logger.debugging)
        Logger.debug("cannot log " + data);
    }
  }

  protected final static String SCENE_TAG = "###scene.spt###";

  private String createZipSet(String script, String[] scripts,
                              boolean includeRemoteFiles, OC out,
                              String pngjName) {
    Lst<Object> v = new Lst<Object>();
    FileManager fm = vwr.fm;
    Lst<String> fileNamesEscaped = new Lst<String>();
    Lst<String> fileNamesUTF = new Lst<String>();
    Hashtable<Object, String> crcMap = new Hashtable<Object, String>();
    boolean haveSceneScript = (scripts != null && scripts.length == 3
        && scripts[1].startsWith(SCENE_TAG));
    boolean sceneScriptOnly = (haveSceneScript && scripts[2].equals("min"));
    if (!sceneScriptOnly) {
      FileManager.getFileReferences(script, fileNamesEscaped, fileNamesUTF);
      if (haveSceneScript)
        FileManager.getFileReferences(scripts[1], fileNamesEscaped,
            fileNamesUTF);
    }
    boolean haveScripts = (!haveSceneScript && scripts != null
        && scripts.length > 0);
    if (haveScripts) {
      script = wrapPathForAllFiles("script " + PT.esc(scripts[0]), "");
      for (int i = 0; i < scripts.length; i++)
        fileNamesEscaped.addLast(scripts[i]);
    }
    int nFiles = fileNamesEscaped.size();
    Lst<String> newFileNames = new Lst<String>();
    for (int iFile = 0; iFile < nFiles; iFile++) {
      String name = fileNamesUTF.get(iFile);
      int pt = name.indexOf("::");
      String type = "";
      if (pt >= 0) {
        type = name.substring(0, pt + 2);
        name = name.substring(pt + 2);
      }
        
      boolean isLocal = OC.isLocal(name);
      String newName = name;
      // also check that somehow we don't have a local file with the same name as
      // a fixed remote file name (because someone extracted the files and then used them)
      if (isLocal || includeRemoteFiles) {
        int ptSlash = name.lastIndexOf("/");
        newName = (name.indexOf("?") > 0 && name.indexOf("|") < 0
            ? PT.replaceAllCharacters(name, "/:?\"'=&", "_")
            : FileManager.stripPath(name));
        newName = PT.replaceAllCharacters(newName, "[]", "_");
        newName = PT.rep(newName, "#_DOCACHE_", "");
        newName = PT.rep(newName, "localLOAD_", "");
        newName = PT.rep(newName, "DROP_", "");
        boolean isSparDir = (fm.spardirCache != null
            && fm.spardirCache.containsKey(name));
        if (isLocal && name.indexOf("|") < 0 && !isSparDir) {
          v.addLast(name);
          v.addLast(newName);
          v.addLast(null); // data will be gotten from disk
        } else {
          // all remote files, and any file that was opened from a ZIP collection
          Object ret = (isSparDir ? fm.spardirCache.get(name)
              : fm.getFileAsBytes(name, null));
          if (!AU.isAB(ret))
            return "ERROR: " + (String) ret;
          newName = addPngFileBytes(name, (byte[]) ret, iFile, crcMap,
              isSparDir, newName, ptSlash, v);
        }
        name = type + "$SCRIPT_PATH$" + newName;
      }
      crcMap.put(newName, newName);
      newFileNames.addLast(PT.escUnicode(name));
    }
    if (!sceneScriptOnly) {
      script = PT.replaceQuotedStrings(script, fileNamesEscaped, newFileNames);
      v.addLast("state.spt");
      v.addLast(null);
      v.addLast(script.getBytes());
    }
    if (haveSceneScript) {
      if (scripts[0] != null) {
        v.addLast("animate.spt");
        v.addLast(null);
        v.addLast(scripts[0].getBytes());
      }
      v.addLast("scene.spt");
      v.addLast(null);
      script = PT.replaceQuotedStrings(scripts[1], fileNamesEscaped,
          newFileNames);
      v.addLast(script.getBytes());
    }
    String sname = (haveSceneScript ? "scene.spt" : "state.spt");
    v.addLast("JmolManifest.txt");
    v.addLast(null);
    String sinfo = "# Jmol Manifest Zip Format 1.1\n" + "# Created "
        + (new Date()) + "\n" + "# JmolVersion " + Viewer.getJmolVersion()
        + "\n" + sname;
    v.addLast(sinfo.getBytes());
    v.addLast("Jmol_version_"
        + Viewer.getJmolVersion().replace(' ', '_').replace(':', '.'));
    v.addLast(null);
    v.addLast(new byte[0]);
    if (out.getFileName() != null) {
      byte[] bytes = vwr.getImageAsBytes("PNG", 0, 0, -1, null);
      if (bytes != null) {
        v.addLast("preview.png");
        v.addLast(null);
        v.addLast(bytes);
      }
    }
    return writeZipFile(out, v, "OK JMOL", pngjName);
  }

  private String addPngFileBytes(String name, byte[] ret, int iFile,
                                 Hashtable<Object, String> crcMap,
                                 boolean isSparDir, String newName, int ptSlash,
                                 Lst<Object> v) {
    Integer crcValue = Integer.valueOf(ZipTools.getCrcValue(ret));
    // only add to the data list v when the data in the file is new
    if (crcMap.containsKey(crcValue)) {
      // let newName point to the already added data
      newName = crcMap.get(crcValue);
    } else {
      if (isSparDir)
        newName = newName.replace('.', '_');
      if (crcMap.containsKey(newName)) {
        // now we have a conflict. Two different files with the same name
        // append "[iFile]" to the new file name to ensure it's unique
        int pt = newName.lastIndexOf(".");
        if (pt > ptSlash) // is a file extension, probably
          newName = newName.substring(0, pt) + "[" + iFile + "]"
              + newName.substring(pt);
        else
          newName = newName + "[" + iFile + "]";
      }
      v.addLast(name);
      v.addLast(newName);
      v.addLast(ret);
      crcMap.put(crcValue, newName);
    }
    return newName;
  }

  /**
   * generic method to create a zip file based on
   * http://www.exampledepot.com/egs/java.util.zip/CreateZip.html
   * 
   * @param out
   * @param fileNamesAndByteArrays
   *        Vector of [filename1, bytes|null, filename2, bytes|null, ...]
   * @param msg
   * @param pngjName
   *        TODO
   * @return msg bytes filename or errorMessage or byte[]
   */

  private String writeZipFile(OC out, Lst<Object> fileNamesAndByteArrays,
                              String msg, String pngjName) {
    byte[] buf = new byte[1024];
    long nBytesOut = 0;
    long nBytes = 0;
    String outFileName = out.getFileName();
    if (pngjName != null && pngjName.startsWith("//"))
      pngjName = "file:" + pngjName.substring(1); // a bug?

    Logger.info("creating zip file " + (outFileName == null ? "" : outFileName)
        + "...");
    String fileList = "";
    try {
      OutputStream bos;
      /**
       * 
       * no need for buffering here
       * 
       * @j2sNative
       * 
       *            bos = out;
       * 
       */
      {
        bos = new BufferedOutputStream(out);
      }
      FileManager fm = vwr.fm;
      OutputStream zos = (OutputStream) ZipTools.getZipOutputStream(bos);
      for (int i = 0; i < fileNamesAndByteArrays.size(); i += 3) {
        String fname = (String) fileNamesAndByteArrays.get(i);
        String fnameShort = (String) fileNamesAndByteArrays.get(i + 1);
        byte[] bytes = (byte[]) fileNamesAndByteArrays.get(i + 2);
        Object data = (bytes == null ? fm.cacheGet(fname, false) : null);
        if (data instanceof Map<?, ?>)
          continue;
        if (fname.indexOf("file:/") == 0) {
          fname = fname.substring(5);
          if (fname.length() > 2 && fname.charAt(2) == ':') // "/C:..." DOS/Windows
            fname = fname.substring(1);
        } else if (fname.indexOf("cache://") == 0) {
          fname = fname.substring(8);
        }
        if (fnameShort == null)
          fnameShort = fname;
        if (data != null)
          bytes = (AU.isAB(data) ? (byte[]) data : ((String) data).getBytes());
        String key = ";" + fnameShort + ";";
        if (fileList.indexOf(key) >= 0) {
          Logger.info("duplicate entry");
          continue;
        }
        fileList += key;
        ZipTools.addZipEntry(zos, fnameShort);
        int nOut = 0;
        if (bytes == null) {
          // get data from disk
          BufferedInputStream in = vwr.getBufferedInputStream(fname);
          int len;
          if (in != null) {
            while ((len = in.read(buf, 0, 1024)) > 0) {
              zos.write(buf, 0, len);
              nOut += len;
            }
            in.close();
          }
        } else {
          // data are already in byte form
          zos.write(bytes, 0, bytes.length);
          if (pngjName != null)
            vwr.fm.recachePngjBytes(pngjName + "|" + fnameShort, bytes);
          nOut += bytes.length;
        }
        nBytesOut += nOut;
        ZipTools.closeZipEntry(zos);
        Logger.info("...added " + fname + " (" + nOut + " bytes)");
      }
      zos.flush();
      zos.close();
      Logger.info(nBytesOut + " bytes prior to compression");
      String ret = out.closeChannel();
      if (ret != null) {
        if (ret.indexOf("Exception") >= 0)
          return ret;
        msg += " " + ret;
      }
      nBytes = out.getByteCount();
    } catch (IOException e) {
      Logger.info(e.toString());
      return e.toString();
    }
    String fileName = out.getFileName();
    return (fileName == null ? null : msg + " " + nBytes + " " + fileName);
  }

  protected String wrapPathForAllFiles(String cmd, String strCatch) {
    String vname = "v__" + ("" + Math.random()).substring(3);
    return "# Jmol script\n{\n\tVar " + vname
        + " = pathForAllFiles\n\tpathForAllFiles=\"$SCRIPT_PATH$\"\n\ttry{\n\t\t"
        + cmd + "\n\t}catch(e){" + strCatch + "}\n\tpathForAllFiles = " + vname
        + "\n}\n";
  }

}
