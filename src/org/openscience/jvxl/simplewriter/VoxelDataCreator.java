package org.openscience.jvxl.simplewriter;


import org.jmol.jvxl.data.VolumeData;
import javajs.util.P3;

public class VoxelDataCreator {

  VolumeData volumeData;
  
  VoxelDataCreator(VolumeData volumeData) {
    this.volumeData = volumeData;
  }
  
    /**
     * Developer must customize this method
     * 
     */
    
    void createVoxelData() {

      int[] counts = volumeData.getVoxelCounts();
      int nX = counts[0];
      int nY = counts[1];
      int nZ = counts[2];
      float[][][] voxelData = new float[nX][nY][nZ];
      volumeData.setVoxelDataAsArray(voxelData);
      // whatever method here that is desired;
      // it is not necessary to create a whole block.
      // you can set volumeData.voxelData = null, in
      // which case the Marching Cube method will 
      // query getValue(x, y, z) itself.
      
      for (int x = 0; x < nX; ++x)
        for (int y = 0; y < nY; ++y)
          for (int z = 0; z < nZ; ++z) {
            voxelData[x][y][z] = getValue(x, y, z);
            //System.out.println("draw a" + x + "" + y + "" + z + " {" + x + " " + y + " " + z + "} \"" + voxelData[x][y][z] + "\"");
          }
    }    

    P3 pt = new P3();

    public float getValue(int x, int y, int z) {
      volumeData.voxelPtToXYZ(x, y, z, pt);
      return pt.x * pt.x + pt.y * pt.y - pt.z * pt.z;  // for instance
    }


}
