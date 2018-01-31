package org.jmol.jvxl.readers;

public abstract class PeriodicVolumeFileReader extends VolumeFileReader {

  @Override
  protected void readSurfaceData(boolean isMapData) throws Exception {
    initializeSurfaceData();
    newVoxelDataCube();
    getPeriodicVoxels();
    if (params.extendGrid != 0) {
      int[] n = new int[3];
      int nx = nPointsX - 1;
      int ny = nPointsY - 1;
      int nz = nPointsZ - 1;
      for (int i = 0; i < 3; i++) {
        int vi = voxelCounts[i] - 1;
        n[i] = (int) (vi * params.extendGrid);
        volumetricOrigin.scaleAdd2(-n[i], volumetricVectors[i],
            volumetricOrigin);
        vi += 2 * n[i];
        while (n[i] > 0)
          n[i] -= voxelCounts[i] - 1;
        voxelCounts[i] = vi + 1;
      }
      nPointsX = voxelCounts[0];
      nPointsY = voxelCounts[1];
      nPointsZ = voxelCounts[2];
      float[][][] vd = new float[nPointsX][nPointsY][nPointsZ];
      for (int i = nPointsX; --i >= 0;)
        for (int j = nPointsY; --j >= 0;)
          for (int k = nPointsZ; --k >= 0;) {
            vd[i][j][k] = voxelData[(i - n[0]) % nx][(j - n[1]) % ny][(k - n[2]) % nz];
          }
      voxelData = vd;
    } else {

      // add in periodic face data

      int n;
      n = nPointsX - 1;
      for (int i = 0; i < nPointsY; ++i)
        for (int j = 0; j < nPointsZ; ++j)
          voxelData[n][i][j] = voxelData[0][i][j];
      n = nPointsY - 1;
      for (int i = 0; i < nPointsX; ++i)
        for (int j = 0; j < nPointsZ; ++j)
          voxelData[i][n][j] = voxelData[i][0][j];
      n = nPointsZ - 1;
      for (int i = 0; i < nPointsX; ++i)
        for (int j = 0; j < nPointsY; ++j)
          voxelData[i][j][n] = voxelData[i][j][0];
    }
    // for map data, just pick out near points and get rid of voxelData

    if (isMapData && volumeData.hasPlane()) {
      volumeData.setVoxelMap();
      for (int x = 0; x < nPointsX; ++x) {
        for (int y = 0; y < nPointsY; ++y) {
          for (int z = 0; z < nPointsZ; ++z) {
            float f = volumeData.getToPlaneParameter();
            if (volumeData.isNearPlane(x, y, z, f))
              volumeData.setVoxelMapValue(x, y, z, voxelData[x][y][z]);
          }
        }
      }
      voxelData = null;
    }
    volumeData.setVoxelDataAsArray(voxelData);

    if (dataMin > params.cutoff)
      params.cutoff = 2 * dataMin;
  }

  protected abstract void getPeriodicVoxels() throws Exception;

}
