package org.jmol.api;

import java.util.Map;

import org.jmol.viewer.Viewer;

public interface JmolDialogInterface {

  public abstract void setupUI(boolean forceNewTranslation);

  /**
   * @return The file type which contains the user's choice
   */
  public abstract String getType();

  /**
   * @param sType JPG or PNG
   * @return The quality (on a scale from 0 to 100) of the JPEG. 0 to 9 for PNG
   *         image that is to be generated.  Returns -1 if choice was not JPEG or PNG.
   */
  public abstract int getQuality(String sType);

  public abstract String getOpenFileNameFromDialog(Map<String, Object> vwrOptions,
                                                   Viewer vwr,
                                                   String fileName,
                                                   JmolAppAPI historyFile,
                                                   String windowName,
                                                   boolean allowAppend);

  public abstract String getSaveFileNameFromDialog(Viewer vwr,
                                                   String data, String type);

  public abstract String getImageFileNameFromDialog(Viewer vwr,
                                                    String fileName,
                                                    String type,
                                                    String[] imageChoices,
                                                    String[] imageExtensions,
                                                    int qualityJPG,
                                                    int qualityPNG);

  public abstract void setImageInfo(int qualityJPG, int qualityPNG,
                                    String imageType);

  public abstract String getFileNameFromDialog(Viewer vwr, String dialogType,
                                               String inputFileName);

}
