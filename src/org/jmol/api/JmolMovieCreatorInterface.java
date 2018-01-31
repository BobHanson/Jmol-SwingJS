package org.jmol.api;

import org.jmol.viewer.Viewer;

public interface JmolMovieCreatorInterface {

  // implemented by org.jmol.image.AviCreator.java
  // status: not useful yet, 9/2008
  
  /**
   * create a movie from a set of image files 
   * @param vwr 
   * @param files 
   * @param width 
   * @param height 
   * @param fps 
   * @param fileName 
   * @return null if no error, errorMsg if an error occurs
   * 
   */
  abstract public String createMovie(Viewer vwr, String[] files, int width,
                                     int height, int fps, String fileName);
}
