package org.jmol.export;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javajs.util.A4d;
import javajs.util.AU;
import javajs.util.CU;
import javajs.util.Lst;
import javajs.util.M4d;
import javajs.util.OC;
import javajs.util.P3d;
import javajs.util.PT;
import javajs.util.Qd;
import javajs.util.SB;
import javajs.util.T3d;
import javajs.util.V3d;

import javajs.util.BS;
import org.jmol.modelset.Atom;
import org.jmol.util.Escape;
import org.jmol.util.GData;
import org.jmol.util.Logger;
import org.jmol.util.MeshSurface;
import org.jmol.viewer.Viewer;

/**
 * Class to export Wavefront OBJ files. The format is described at<br>
 * <br>
 * <a href="http://en.wikipedia.org/wiki/Wavefront_.obj_file">
 * http://en.wikipedia.org/wiki/Wavefront_.obj_file</a><br>
 * and<br>
 * <a href="http://www.martinreddy.net/gfx/3d/OBJ.spec">
 * http://www.martinreddy.net/gfx/3d/OBJ.spec</a><br>
 * <br>
 * At least two files are produced: the object in the .obj file and the
 * materials in the .mtl file. An additional image file is produced for each
 * surface. All should be kept in the same directory.<br>
 * <br>
 * The exporter has been tested for ball and stick models, but not for:
 * <ul>
 * <li>outputFace (not used)</li>
 * <li>outputCone</li>
 * <li>outputTextPixel</li>
 * <li>outputTriangle</li>
 * <li>outputSurface (only some possibilities tested)</li>
 * </ul>
 * 
 * Adapted for JavaScript by Bob Hanson 2/2014
 * using javajs.util.OC OutputChannel instead of File.
 * This is way more flexible.
 * 
 * @author ken@kenevans.net
 * 
 */
public class _ObjExporter extends __CartesianExporter {
  /** Flag to cause debugging output to stdout. */
  private static final boolean debug = false;
  /** Flag to cause only surfaces to be output. */
  private boolean surfacesOnly = false;
  /**
   * Whether to normalize UV texture coordinates. (Many applications expect them
   * to be normalized.)
   */
  private boolean normalizeUV = true;
  /** BufferedWriter for the .mtl file. */
  private OC mtlout;
  /** Path of the OBJ file without the extension. */
  String objFileRootName;
  /** File for the .mtl file. */
  // this may be a web socket even. // OC mtlFile;
  /** Bytes written to the .mtl file. */
  private int nMtlBytes;
  /** HashSet for textures. */
  Set<Short> textures = new HashSet<Short>();
  /** List of texture files created. */
  Lst<String> textureFiles;

  /** Number for the next mesh of this type. */
  private int sphereNum = 1;
  /** Number for the next mesh of this type. */
  private int cylinderNum = 1;
  /** Number for the next mesh of this type. */
  private int ellipseNum = 1;
  /** Number for the next mesh of this type. */
  private int circleNum = 1;
  /** Number for the next mesh of this type. */
  private int ellipsoidNum = 1;
  /** Number for the next mesh of this type. */
  private int coneNum = 1;
  /** Number for the next mesh of this type. */
  private int triangleNum = 1;
  /** Number for the next mesh of this type. */
  private int surfaceNum = 1;

  /**
   * Wavefront OBJ refers to vertices and normals and textures by their location
   * in the file. This keeps track of where the latest vertex set starts.
   */
  private int currentVertexOrigin = 1;
  /**
   * Wavefront OBJ refers to vertices and normals and textures by their location
   * in the file. This keeps track of where the latest normal set starts.
   */
  private int currentNormalOrigin = 1;
  /**
   * Wavefront OBJ refers to vertices and normals and textures by their location
   * in the file. This keeps track of where the latest texture set starts.
   */
  private int currentTextureOrigin = 1;

//  /**
//   * The size of a pixel based on some assumptions about screen size
//   */
//  private double pixelSize;
  
  public _ObjExporter() {
    debugPrint("_WavefrontObjExporter CTOR");
    commentChar = "# ";
  }

  /**
   * Debug print utility. Only prints if debug is true.
   * 
   * @param string
   */
  protected void debugPrint(final String string) {
    if (debug) {
      Logger.debug(string);
    }
  }

  // Abstract methods

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputFace(int[], int[], int)
   */
  @Override
  protected void outputFace(int[] face, int[] map, int faceVertexMax) {
    // not used
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputCircle(javax.vecmath.Point3f, javax.vecmath.Point3f, double, short, boolean)
   */
  @Override
  protected void outputCircle(P3d pt1, P3d pt2, double radius,
                              short colix, boolean doFill) {
    debugPrint("outputCircle");
    if (surfacesOnly) {
      debugPrint("  Not done owing to surfacesOnly");
      return;
    }
    if (doFill) {
      outputCircle1(pt1, pt2, colix, radius);
    }
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputCone(javax.vecmath.Point3f, javax.vecmath.Point3f, double, short)
   */
  @Override
  protected void outputCone(P3d ptBase, P3d ptTip, double radius,
                            short colix) {
    debugPrint("outputCone");
    if (surfacesOnly) {
      debugPrint("  Not done owing to surfacesOnly");
      return;
    }

    outputCone1(ptBase, ptTip, radius, colix);
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputCylinder(javax.vecmath.Point3f, javax.vecmath.Point3f, javax.vecmath.Point3f, short, byte, double, javax.vecmath.Point3f, javax.vecmath.Point3f)
   */
  @Override
  protected boolean outputCylinder(P3d ptCenter, P3d pt1, P3d pt2,
                                   short colix, byte endcaps, double radius,
                                   P3d ptX, P3d ptY, boolean checkRadius) {
    // Ignore ptX and pyY as they are passed null from __CartesianExporter.draw
    if (debug) {
      debugPrint("outputCylinder: colix="
          + String.format("%04x", Short.valueOf(colix)));
      debugPrint("  ptCenter=" + ptCenter);
      debugPrint("  pt1=" + pt1);
      debugPrint("  endcaps=" + endcaps + " NONE=" + GData.ENDCAPS_NONE
          + " FLAT=" + GData.ENDCAPS_FLAT + " SPHERICAL="
          + GData.ENDCAPS_SPHERICAL);
      debugPrint("  radius=" + radius);
      debugPrint("  pt2=" + pt2);
      debugPrint("  ptX=" + ptX);
      debugPrint("  ptY=" + ptY);
    }
    if (surfacesOnly) {
      debugPrint("  Not done owing to surfacesOnly");
      return true;
    }

    if (ptX != null) {
      if (endcaps == GData.ENDCAPS_FLAT) {
        outputEllipse1(ptCenter, pt1, ptX, ptY, colix);
        tempP3.add2(ptCenter, ptCenter);
        tempP3.sub(ptX);
        outputEllipse1(ptCenter, pt2, tempP3, ptY, colix);
      }

    } else if (endcaps == GData.ENDCAPS_SPHERICAL) {
      outputSphere(pt1, radius * 1.01f, colix, true);
      outputSphere(pt2, radius * 1.01f, colix, true);
    } else if (endcaps == GData.ENDCAPS_FLAT) {
      outputCircle1(pt1, pt2, colix, radius);
      outputCircle1(pt2, pt1, colix, radius);
    }
    outputCylinder1(ptCenter, pt1, pt2, colix, endcaps, radius, ptX, ptY);

    return true;
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputEllipsoid(javax.vecmath.Point3f, javax.vecmath.Point3f[], short)
   */
  @Override
  protected void outputEllipsoid(P3d center, P3d[] points, short colix) {
    if (debug) {
      debugPrint("outputEllipsoid: colix="
          + String.format("%04x", Short.valueOf(colix)));
      debugPrint("  center=" + center);
      debugPrint("  points[0]=" + points[0]);
      debugPrint("  points[1]=" + points[1]);
      debugPrint("  points[2]=" + points[2]);
    }
    if (surfacesOnly) {
      debugPrint("  Not done owing to surfacesOnly");
      return;
    }

    A4d a = Qd.getQuaternionFrame(center, points[1], points[3])
        .toA4d();
    double sx = points[1].distance(center);
    double sy = points[3].distance(center);
    double sz = points[5].distance(center);
    outputEllipsoid1(center, sx, sy, sz, a, colix);
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputSphere(javax.vecmath.Point3f, double, short)
   */
  @Override
  protected void outputSphere(P3d center, double radius, short colix, boolean checkRadius) {
    // Note center is called ptAtom2 in the _CartesianExporter superclass
    // Note radius is called f in the _CartesianExporter superclass
    // Atom extends Point3fi extends Point3f, so this may be passed an Atom
    if (debug) {
      debugPrint("outputSphere: colix="
          + String.format("%04x", Short.valueOf(colix)));
      debugPrint("  center.getClass().getName()=" + center.getClass().getName());
      debugPrint("  center=" + center);
      debugPrint("  center.x=" + center.x);
      debugPrint("  center.y=" + center.y);
      debugPrint("  center.z=" + center.z);
      debugPrint("  radius=" + radius);
    }
    if (surfacesOnly) {
      debugPrint("  Not done owing to surfacesOnly");
      return;
    }

    // Treat as a special case of ellipsoid
    outputEllipsoid1(center, radius, radius, radius, null, colix);
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputTextPixel(javax.vecmath.Point3f, int)
   */
  @Override
  protected void outputTextPixel(P3d pt, int argb) {
//    if (surfacesOnly) {
//      return;
//    }
//    short colix = C.getColix(argb);
//    outputSphere(pt, pixelSize, colix, true);
  }

  /* (non-Javadoc)
   * @see org.jmol.export.__CartesianExporter#outputTriangle(javax.vecmath.Point3f, javax.vecmath.Point3f, javax.vecmath.Point3f, short)
   */
  @Override
  protected void outputTriangle(T3d pt1, T3d pt2, T3d pt3,
                                short colix) {
    if (surfacesOnly) {
      return;
    }
    outputTriangle1(pt1, pt2, pt3, colix);
  }

  /* (non-Javadoc)
   * @see org.jmol.export.___Exporter#outputHeader()
   */
  @Override
  protected void outputHeader() {
    debugPrint("outputHeader");
    output("#obj Created by Jmol " + Viewer.getJmolVersion() + "\n");
  }

  /* (non-Javadoc)
   * @see org.jmol.export.___Exporter#output(javax.vecmath.Tuple3f)
   */
  @Override
  protected void output(T3d pt) {
    debugPrint("output");
  }

  /* (non-Javadoc)
   * @see org.jmol.export.___Exporter#drawSurface(MeshSurface meshSurface)
   */
  @Override
  protected void drawSurface(MeshSurface meshSurface, short colix) {
    if (Logger.debugging) {
      debugPrint("outputSurface");
      debugPrint("  nVertices=" + meshSurface.vc);
      if (meshSurface.normals == null) {
        debugPrint("  no vertex normals");
      } else {
        debugPrint("  nNormals=" + meshSurface.vc);
      }
      if (meshSurface.vcs == null) {
        debugPrint("  no vertex colors");
      } else {
        debugPrint("  nColixes=" + meshSurface.vc);
      }
      debugPrint("  number of triangles or quads=" + meshSurface.pc);
      if (meshSurface.pcs == null) {
        debugPrint("  no face colors");
      } else {
        debugPrint("  nPolygonColixes=" + meshSurface.pc);
      }
      if (meshSurface.bsPolygons == null) {
        debugPrint("  all polygons used");
      } else {
        debugPrint("  number of polygons used=" + meshSurface.bsPolygons.cardinality());
      }
      debugPrint("  solid color=" + gdata.getColorArgbOrGray(colix));
    }

    // Create reduced face set
    
    BS bsPolygons = meshSurface.bsPolygons;
    int nPolygons = meshSurface.pc;
    if (meshSurface.normals != null)
      meshSurface.normalCount = meshSurface.vc;
    boolean isAll = (bsPolygons == null);
    int[][] faces = AU.newInt2(isAll ? nPolygons : bsPolygons.cardinality());
    int i0 = (isAll ? nPolygons - 1 : bsPolygons.nextSetBit(0));
    for (int i = i0, ipt = 0; i >= 0; i = isAll ? i - 1 : bsPolygons
        .nextSetBit(i + 1)) {
      int[] polygon = meshSurface.pis[i];
      faces[ipt++] = (meshSurface.haveQuads ? polygon : new int[] {
          polygon[0], polygon[1], polygon[2] });
    }
    MeshSurface data = MeshSurface.newMesh(false, meshSurface.vs, meshSurface.vc, faces,
        meshSurface.normals, 0);
    data.vcs = meshSurface.vcs;
    // Do the texture
    String name = "Surface" + surfaceNum++;
    boolean isSolidColor = (colix != 0);
    addTexture(colix, isSolidColor ? null : name);

    // Create a Point with the image file dimensions
    // If it remains null, then it is a flag that a texture file and
    // texture coordinates are not used
    int[] dim = null;

    // Make a texture file if colixes is defined
    if (isSolidColor) {
      debugPrint("outputSurface: coloring solid");
      debugPrint("  Omitting texture map");
    } else {
      int nFaces = faces.length;
      // Determine the height and width of an image file that is as close to
      // square as possible 
      int width = (int) Math.ceil(Math.sqrt(nFaces));
      int height = nFaces / width;
      if (nFaces % width != 0) {
        height++;
      }
      dim = new int[] { width, height };
      debugPrint("  width=" + width + " height=" + height + " size = "
          + (width * height));
      OC file = createTextureFile(name, data, dim);
      if (file == null || file.getByteCount() == 0) {
        System.out.println("Error creating texture file: " + name);
        textureFiles.addLast("Error creating texture file: " + name);
        return;
      }
//      String error = "";
//      if (!file.exists()) {
//        error = " [Does not exist]";
//      } else if (file.length() == 0) {
//        error = " [Empty]";
//      }
      
      textureFiles.addLast(file.getByteCount() + " (" + width + "x" + height + ") "
          + name);
      // Add the texture file to the material
      String shortName = file.getName();
      outputMtl(" map_Kd " + shortName + "\n");
      // TODO Check this is wise
      outputMtl(" map_Ka " + shortName + "\n");
    }

    M4d matrix = M4d.newM4(null);
    matrix.setTranslation(V3d.newV(meshSurface.offset));
    BS bsValid = new BS();
    addMesh(name, data, matrix, null, colix, dim, bsValid);
  }

  // Non-abstract overrides from _Exporter

  /* (non-Javadoc)
   * @see org.jmol.export.___Exporter#initializeOutput(org.jmol.viewer.Viewer, org.jmol.g3d.Graphics3D, java.lang.Object)
   */
  @Override
  boolean initializeOutput(Viewer vwr, double privateKey, GData gdata, Map<String, Object> params) {
    debugPrint("initializeOutput: + output");
    // Call the super method
    boolean retVal = initOutput(vwr, privateKey, gdata, params);
    if (!retVal) {
      debugPrint("End initializeOutput (error in super):");
      return false;
    }

//    pixelSize = 0.5d / scalePixelsPerAngstrom;

    // Get the root path
    int dot = fileName.lastIndexOf(".");
    if (dot < 0) {
      debugPrint("End initializeOutput (Error creating .mtl file):");
      return false;
    }
    objFileRootName = fileName.substring(0, dot);

    // Open stream and writer for the .mtl file
    try {
      String mtlFileName = objFileRootName + ".mtl";
      mtlout = vwr.openExportChannel(privateKey, mtlFileName, true);
    } catch (Exception ex) {
      debugPrint("End initializeOutput (" + ex.getMessage() + "):");
      return false;
    }
    outputMtl("# Created by Jmol " + Viewer.getJmolVersion() + "\n");
    output("\nmtllib " + mtlout.getName() + "\n");

    // Keep a list of texture files created
    textureFiles = new  Lst<String>();
    debugPrint("End initializeOutput:");
    return true;
  }

  /* (non-Javadoc)
   * @see org.jmol.export.___Exporter#finalizeOutput()
   */
  @Override
  protected String finalizeOutput() {
    debugPrint("finalizeOutput");
    String retVal = finalizeOutput2();

    // Close the writer and stream
    String ret = mtlout.closeChannel();
    if (ret != null) {
      Logger.info(ret);
      ret = "ERROR EXPORTING MTL FILE: " + ret;
      if (retVal.startsWith("OK"))
        return ret;
      return retVal + " and " + ret;
    }

    retVal += ", " + nMtlBytes + " " + mtlout.getFileName();
    for (String string : textureFiles) {
      retVal += ", " + string;
    }
    debugPrint(retVal);
    debugPrint("End finalizeOutput:");
    return retVal;
  }

  // Added methods

  /**
   * Write to the .mtl file and keep track of the bytes written.
   * 
   * @param data
   */
  private void outputMtl(String data) {
    nMtlBytes += data.length();
    mtlout.append(data);
  }

  /**
   * Returns the name to be used for the texture associated with the given
   * colix. Jmol reading of the file without additional resources requires a
   * color name here in the form: kRRGGBB
   * 
   * @param colix
   *          The value of colix.
   * @return The name for the structure.
   */
  private String getTextureName(short colix) {
    return "k" + Escape.getHexColorFromRGB(gdata.getColorArgbOrGray(colix));
  }

  /**
   * Local implementation of outputCircle.
   * 
   * @param ptCenter
   * @param ptPerp
   * @param colix
   * @param radius
   */
  private void outputCircle1(P3d ptCenter, P3d ptPerp, short colix,
                             double radius) {
    MeshSurface data = MeshData.getCircleData();
    M4d matrix = new M4d();
    addTexture(colix, null);
    String name = "Circle" + circleNum++;
    matrix.setToM3(getRotationMatrix(ptCenter, ptPerp, radius));
    matrix.m03 = ptCenter.x;
    matrix.m13 = ptCenter.y;
    matrix.m23 = ptCenter.z;
    matrix.m33 = 1;
    addMesh(name, data, matrix, matrix, colix, null, null);
  }

  /**
   * Local implementation of outputCone.
   * 
   * @param ptBase
   * @param ptTip
   * @param radius
   * @param colix
   */
  private void outputCone1(P3d ptBase, P3d ptTip, double radius,
                           short colix) {
    MeshSurface data = MeshData.getConeData();
    M4d matrix = new M4d();
    addTexture(colix, null);
    String name = "Cone" + coneNum++;
    matrix.setToM3(getRotationMatrix(ptBase, ptTip, radius));
    matrix.m03 = ptBase.x;
    matrix.m13 = ptBase.y;
    matrix.m23 = ptBase.z;
    matrix.m33 = 1;
    addMesh(name, data, matrix, matrix, colix, null, null);
  }

  /**
   * Local implementation of outputEllipse.
   * 
   * @param ptCenter
   * @param ptZ
   * @param ptX
   * @param ptY
   * @param colix
   * @return Always returns true.
   */
  private boolean outputEllipse1(P3d ptCenter, P3d ptZ, P3d ptX,
                                 P3d ptY, short colix) {
    MeshSurface data = MeshData.getCircleData();
    M4d matrix = new M4d();
    addTexture(colix, null);
    String name = "Ellipse" + ellipseNum++;
    matrix.setToM3(getRotationMatrix(ptCenter, ptZ, 1, ptX, ptY));
    matrix.m03 = ptZ.x;
    matrix.m13 = ptZ.y;
    matrix.m23 = ptZ.z;
    matrix.m33 = 1;
    addMesh(name, data, matrix, matrix, colix, null, null);
    return true;
  }

  /**
   * Local implementation of outputEllipsoid.
   * 
   * @param center
   * @param rx
   * @param ry
   * @param rz
   * @param a
   * @param colix
   */
  private void outputEllipsoid1(T3d center, double rx, double ry, double rz,
                                A4d a, short colix) {
    MeshSurface data = MeshSurface.getSphereData(3);
    addTexture(colix, null);  
    String name;
    if (center instanceof Atom) {
      Atom atom = (Atom) center;
      name = PT.replaceAllCharacters(atom.getAtomName(), " \t", "") + "_Atom";
    } else if (rx == ry && rx == rz) {
      // Is a sphere
      name = "Sphere" + sphereNum++;
    } else {
      name = "Ellipsoid" + ellipsoidNum++;
    }
    setSphereMatrix(center, rx, ry, rz, a, sphereMatrix);
    addMesh(name, data, sphereMatrix, sphereMatrix, colix, null, null);
  }

  /**
   * Local implementation of outputCylinder.
   * 
   * @param ptCenter
   * @param pt1
   * @param pt2
   * @param colix
   * @param endcaps
   * @param radius
   * @param ptX
   * @param ptY
   */
  private void outputCylinder1(P3d ptCenter, P3d pt1, P3d pt2,
                               short colix, byte endcaps, double radius,
                               P3d ptX, P3d ptY) {
    MeshSurface data = MeshData.getCylinderData(false);
    M4d matrix = new M4d();
    addTexture(colix, null);
    String name = "Cylinder" + cylinderNum++;
    int n = (ptX != null && endcaps == GData.ENDCAPS_NONE ? 2 : 1);
    for (int i = 0; i < n; i++) {
      if (ptX == null)
        matrix.setToM3(getRotationMatrix(pt1, pt2, radius));
      else
        matrix.setToM3(getRotationMatrix(ptCenter, pt2, radius, ptX, ptY));
      matrix.m03 = pt1.x;
      matrix.m13 = pt1.y;
      matrix.m23 = pt1.z;
      matrix.m33 = 1;
    }
    addMesh(name, data, matrix, matrix, colix, null, null);
  }

  /**
   * Local implementation of outputCylinder.
   * 
   * @param pt1
   *          Vertex 1.
   * @param pt2
   *          Vertex 2.
   * @param pt3
   *          Vertex 3.
   * @param colix
   *          The colix.
   */
  private void outputTriangle1(T3d pt1, T3d pt2, T3d pt3,
                               short colix) {
    MeshSurface data = MeshData.getTriangleData(pt1, pt2, pt3);
    addTexture(colix, null);
    String name = "Triangle" + triangleNum++;
    M4d matrix = M4d.newM4(null);
    addMesh(name, data, matrix, matrix, colix, null, null);
  }

  /**
   * Adds a texture to the .mtl file if it is a new texture. Some of the
   * parameter choices are arbitrarily chosen. The .mtl file can be easily
   * edited if it is desired to change things.
   * 
   * @param colix
   * @param name TODO
   */
  private void addTexture(short colix, String name) {
    Short scolix = Short.valueOf(colix);
    if (name == null && textures.contains(scolix)) {
      return;
    }
    textures.add(scolix);
    SB sb = new SB();
    sb.append("\nnewmtl " + (name == null ? getTextureName(colix) : name) + "\n");
    // Highlight exponent (0-1000) High is a tight, concentrated highlight
    sb.append(" Ns 163\n");
    // Opacity (Sometimes d is used, sometimes Tr)
    //    sb.append(" d " + opacityFractionalFromColix(colix) + "\n");
    sb.append(" Tr " + opacityFractionalFromColix(colix) + "\n");
    // Index of refraction (.0001-10) 1.0 passes through
    sb.append(" Ni 0.001\n");
    // Illumination model (2 = highlight on)
    sb.append(" illum 2\n");
    // Ambient
    //    sb.append(" Ka " + rgbFractionalFromColix(colix, ' ') + "\n");
    sb.append(" Ka 0.20 0.20 0.20\n");
    // Diffuse
    sb.append(" Kd " + rgbFractionalFromColix(colix) + "\n");
    // Specular
    sb.append(" Ks 0.25 0.25 0.25\n");

    outputMtl(sb.toString());
  }

  /**
   * Adds a new mesh using the given data (faces, vertices, and normals) and
   * colix after transforming it via the given affine transform matrix.
   * 
   * @param name
   *          The name to be used for the mesh.
   * @param data
   *          Where the data are located.
   * @param matrix
   *          Transformation to transform the base mesh.
   * @param matrix1 
   *          Transformation for normals
   * @param colix
   *          Colix associated with the mesh.
   * @param dim
   *          The width, height of the associated image for UV texture
   *          coordinates. If null no UV coordinates are used.
   * @param bsValid TODO
   */
  private void addMesh(String name, MeshSurface data, M4d matrix, M4d matrix1,
                       short colix, int[] dim, BS bsValid) {
    // Use to only get surfaces in the output
    if (surfacesOnly) {
      if (name == null || !name.startsWith("Surface")) {
        return;
      }
    }
    // Note: No texture coordinates (vt) are used
    // The group (g) is probably not needed, but makes the file easier to read
    // Vertices and normals are numbered sequentially throughout the OBJ file
    //   (Why the currentVertexOrigin, etc. are needed)
    // currentNormalOrigin is the same as currentVertexOrigin since the
    //   normals and vertices are in 1-1 correspondence for our meshes
    output("\n" + "g " + name + "\n");
    output("usemtl " + (dim == null ? getTextureName(colix) : name) + "\n");

    int[][] faces = data.getFaces();
    int nFaces = faces.length;
    
    if (bsValid != null)
      for (int[] face : faces)
        for (int i : face)
          bsValid.set(i);
    
    // vertices
    
    T3d[] vertices = data.getVertices();
    int nVertices = data.vc;
    int[] map = new int[nVertices];
    int nCoord = getCoordinateMap(vertices, map, bsValid);
    output("# Number of vertices: " + nCoord + "\n");
    outputList(vertices, nVertices, matrix, "v ", bsValid);
    nVertices = nCoord;

    // normals
    
    T3d[] normals = data.normals;
    int nNormals = data.normalCount;
    int[] map2 = null;
    Lst<String> vNormals = null;
    if (normals != null) {
      vNormals = new  Lst<String>();
      map2 = getNormalMap(normals, nNormals, bsValid, vNormals);
      nNormals = vNormals.size();
      output("# Number of normals: " + nNormals + "\n");
      for (int i = 0; i < nNormals; i++)
        output("vn " + vNormals.get(i));    
    }

    if (dim != null) {
      // This needs to be kept correlated with what createTextureFile does
      output("# Number of texture coordinates: " + nFaces + "\n");
      int width = dim[0];
      int height = dim[1];
      double u, v;
      for (int row = 0, iFace = 0; row < height; row++) {
        v = row + .5d;
        if (normalizeUV)
          v /= height;
        for (int col = 0; col < width; col++) {
          u = col + .5d;
          if (normalizeUV)
            u /= width;
          output("vt " + u + " " + v + "\n");
          if (++iFace == nFaces)
            break;
        }
      }
      if (!normalizeUV) {
        // Be sure there are values to denote the limits of the UV mesh
        output("vt 0.0 0.0\n");
        output("vt " + (double) width + " " + (double) height + "\n");
      }
    }
    output("# Number of faces: " + nFaces + "\n");
    for (int i = 0; i < nFaces; i++)
      if (dim != null)
        outputFace2(faces[i], i, map, map2);
      else
        outputFace1(faces[i], map, map2);

    // Increase the the current numbering start points for the vertices,
    // textures, and normals
    if (dim != null)
      currentTextureOrigin += nFaces;
    currentVertexOrigin += nVertices;
    currentNormalOrigin += nNormals;
  }
  
  private final P3d ptTemp = new P3d();

  /**
   * create the v or vn list
   * 
   * @param pts  
   * @param nPts
   * @param m
   * @param prefix
   * @param bsValid TODO
   */
  private void outputList(T3d[] pts, int nPts, M4d m, String prefix, BS bsValid) {
    for (int i = 0; i < nPts; i++) {
      if (bsValid != null && !bsValid.get(i))
        continue;
      ptTemp.setT(pts[i]);
      if (m != null)
        m.rotTrans(ptTemp);
      output(prefix + ptTemp.x + " " + ptTemp.y + " " + ptTemp.z + "\n");
    }
  }

  /**
   * Local implementation of outputFace used for no texture coordinates.
   * 
   * @param face
   * @param map
   *          Map of data vertex indexes to output indexes 
   * @param map2
   *          Map of data normal indexes to output indexes 
   */
  private void outputFace1(int[] face, int[] map, int[] map2) {
    output("f");
    for (int i : face)
      output(" " + ((map == null ? i : map[i]) + currentVertexOrigin) + "//"
          + ((map2 == null ? i : map2[i]) + currentNormalOrigin));
    output("\n");
  }

  /**
   * Local implementation of outputFace used with texture coordinates.
   * 
   * @param face
   *          Array of vertices for the face.
   * @param vt
   *          Number of the vt texture coordinate.
   * @param map
   *          Map of data vertex indexes to output indexes 
   * @param map2
   *          Map of data normal indexes to output indexes 
   */
  private void outputFace2(int[] face, int vt, int[] map, int[] map2) {
    output("f");
    for (int i : face) {
      output(" " + ((map == null ? i : map[i]) + currentVertexOrigin) + "/"
          + (currentTextureOrigin + vt) + "/"
          + ((map2 == null ? i : map2[i]) + currentNormalOrigin));
    }
    output("\n");
  }
  
  /**
   * Writes a texture file with the colors in the colixes array in a way that it
   * can be mapped by the texture coordinates vt.
   * 
   * @param name
   *        The name of the file without the path or ext. This will be added to
   *        the root name of the OBJ file along with the image suffix. The value
   *        should be the name given to the surface.
   * @param data
   * @param dim
   *        A Point representing the width, height of the image.
   * @return The File created or null on failure.
   */
  private OC createTextureFile(String name, MeshSurface data, int[] dim) {
    // This needs to be kept correlated with what doUV in addMesh does
    debugPrint("createTextureFile: " + name);
    // Do nothing unless all the arrays exist and have the same size
    //    if (debug) {
    //      debugPrint("  ImageIO Avaliable Formats:");
    //      String[] formats = ImageIO.getWriterFormatNames();
    //      for (String format : formats) {
    //        debugPrint("    " + format);
    //      }
    //    }

    // Create the File before checking the input, deleting any existing file
    // of the same name (silently) so we will not have an inconsistent set of
    // files.  Create the file even if it will be empty so we can denote that
    // in the console output.
    // Check input 
    short[] colixes = (data.pcs == null ? data.vcs
        : data.pcs);
    if (colixes == null || colixes.length == 0) {
      debugPrint("createTextureFile: Array problem");
      debugPrint("  colixes=" + colixes + " data=" + data);
      if (colixes != null) {
        debugPrint("  colixes.length=" + colixes.length);
      }
      return null;
    }

    int nUsed = data.pis.length;
    if (nUsed <= 0) {
      debugPrint("createTextureFile: nFaces = 0");
      return null;
    }
    // Make a BufferedImage and set the pixels in it
    int width = dim[0];
    int height = dim[1];
    // We write a 3x3 block of pixels for each color 
    // point so as to avoid antialiasing by vwr

    String textureType = "png";
    // tga is not enabled
    // Write it bottom to top to match direction of UV coordinate v
    int row = height - 1;
    int col = 0;
    P3d sum = new P3d();
    int w = width * 3;
    int h = height * 3;
    byte[][] bytes = (textureType.equals("tga") ? new byte[h][w * 3] : null);
    int[] rgbbuf = (bytes == null ? new int[h * w] : null);
    P3d ptTemp = new P3d();
    for (int i = 0; i < data.pis.length; i++) {
      int rgb;
      if (data.pcs == null) {
        int[] face = data.pis[i];
        // Get the vertex colors and average them
        sum.set(0, 0, 0);
        for (int iVertex : face)
          sum.add(CU.colorPtFromInt(gdata.getColorArgbOrGray(colixes[iVertex]), ptTemp));
        sum.scale(1.0d / face.length);
        rgb = CU.colorPtToFFRGB(sum);
      } else {
        rgb = gdata.getColorArgbOrGray(colixes[i]);
      }
      if (bytes == null) {
        // just create an int32 rgb buffer. That's all any of the encoders need.
        for (int j = 0; j < 3; j++)
          for (int k = 0; k < 3; k++)
            rgbbuf[(row * 3 + k) * w + col * 3 + j] = rgb;//image.setRGB(col * 3 + j, row * 3 + k, rgb);
      } else {
        /*  TGA test -- not worth it 
        byte r = (byte) ((rgb >> 16) & 0xFF);
        byte g = (byte) ((rgb >> 8) & 0xFF);
        byte b = (byte) (rgb & 0xFF);
        for (int j = 0; j < 3; j++) {
          int x = col * 9 + j * 3;
          for (int k = 0; k < 3; k++) {
             int y = height * 3 - 1 - (row * 3 + k);
             bytes[y][x] = b;
             bytes[y][x + 1] = g;
             bytes[y][x + 2] = r;
          }
        }
        */
      }
      if ((col = (col + 1) % width) == 0)
        row--;
    }

    // Write the file
    try {
      // in the applet, we must allow the user to use a dialog, which can change the file name
      Map<String, Object> params = new Hashtable<String, Object>();
      String fname = fileName;
      if (rgbbuf != null) {
        params.put("rgbbuf", rgbbuf);
        params
            .put("fileName", objFileRootName + "_" + name + "." + textureType);
        params.put("type", textureType);
        params.put("width", Integer.valueOf(w));
        params.put("height", Integer.valueOf(h));
        fname = fileName = vwr.outputToFile(params);
      }
      debugPrint("End createTextureFile: " + fname);
      return (OC) params.get("outputChannel");
    } catch (Exception ex) {
      debugPrint("End createTextureFile (" + ex.getMessage() + "):");
      return null;
    }
  }

//  private Object createImage(Map<String, Object> params) throws Exception {
    /*  TGA test -- not worth it 
    // write simple TGA file
    // see http://www.organicbit.com/closecombat/formats/tga.html
    // no point in this, and it is much larger than png
    OutputStream os = new FileOutputStream(fileName);
    width *= 3;
    height *= 3;
    byte[] header = new byte[18];
    header[2] = 2; // rbg image
    header[7] = 32;
    header[12] = (byte) (width & 0xFF);
    header[13] = (byte) ((width >> 8) & 0xFF);
    header[14] = (byte) (height & 0xFF);
    header[15] = (byte) ((height >> 8) & 0xFF);
    header[16] = 24;
    os.write(header);
    byte[][] bytes = (byte[][]) image;
    for (int i = 0; i < bytes.length; i++)
      os.write(bytes[i]);
    os.flush();
    os.close();
    */
//  }


}
  