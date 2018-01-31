package org.jmol.quantum;


/**
 * Allows modification of the planes prior to isosurface creation
 * Used by Noncovalent Interaction Calculation for progressive readers
 */

public abstract class QuantumPlaneCalculation extends QuantumCalculation {
  
  /**
   * Planes to use for holding raw file data. These will be managed by
   * VolumeFileReader, but they will be needed by the calculation.
   * 
   * @param planes   a set of four planes that shifts as the progressive
   *                 Marching Cubes process moves along
   */
  public abstract void setPlanes(float[][] planes);
  
  /**
   * Fill this plane with data based on the current set of raw data planes. 
   * Really there are just two planes that are managed by VolumeFileReader
   * and are interchanged as the Marching Cubes process moves along.
   * @param x 
   * 
   * @param plane
   */
  
  public abstract void calcPlane(int x, float[] plane);
  
  /**
   * 
   * Data mapping function to radically increase speed and reduce
   * memory requirements of mapping data when the mapping comes from
   * the same data set as the points, so isosurface creation and 
   * data mapping can be carried out both in the first (and only) pass. 
   * 
   * 
   * @param vA  absolute pointer to vertex A on grid 
   * @param vB  absolute pointer to vertex B on grid
   * @param f   fractional way from A to B
   * @return    computed value
   */
  public abstract float process(int vA, int vB, float f);
  
  /**
   * Get that value that represents "no value" so that it can be
   * disregarded in terms of recording and reporting the min/max/mean.
   *         
   * @return NO_VALUE
   */
  public abstract float getNoValue();

  public abstract void getPlane(int x, float[] yzPlane);

}
