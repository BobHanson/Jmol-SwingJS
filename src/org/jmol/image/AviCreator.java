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

package org.jmol.image;

//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;

import org.jmol.api.JmolMovieCreatorInterface;
//import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

/*
 * by Bob Hanson, hansonr@stolaf.edu  9/26/2008
 * 
 * status: useless, not well tested. Experimental. Probably to be removed.
 * 
 * Create a movie from a set of JPG images.
 * http://msdn.microsoft.com/en-us/library/ms779631(VS.85).aspx

 * unfortunately, I think the JPEGs that Jmol creates are
 * not appropriate for AVI insertion. Still, this class
 * does appear to creat genuine AVI files.
 * 
 * What we could do is use the pixelGrabber to get the pixels from 
 * the JPG or other image source (see ImageJ http://rsbweb.nih.gov/ij/
 * and AviReader http://rsbweb.nih.gov/ij/plugins/avi-reader.html)
 * 
 * 
 * (http://www.digitalpreservation.gov/formats/fdd/fdd000063.shtml)
 *  <quote>
 *  Avery Lee, writing in the rec.video.desktop 
 *  newsgroup in 2001, commented that "MJPEG, or at 
 *  least the MJPEG in AVIs having the MJPG fourcc, 
 *  is restricted JPEG with a fixed -- and *omitted* 
 *  -- Huffman table. The JPEG must be YCbCr 
 *  colorspace, it must be 4:2:2, and it must use 
 *  basic Huffman encoding, not arithmetic or 
 *  progressive. . . . You can indeed extract the 
 *  MJPEG frames and decode them with a regular 
 *  JPEG decoder, but you have to prepend the DHT 
 *  segment to them, or else the decoder won't have 
 *  any idea how to decompress the data. The exact 
 *  table necessary is given in the OpenDML spec."
 *  </quote>

 RIFF ('AVI '
      LIST ('hdrl'
            'avih'(<Main AVI Header>)
            LIST ('strl'
                  'strh'(<Stream header>)
                  'strf'(<Stream format>)
                  [ 'strd'(<Additional header data>) ]
                  [ 'strn'(<Stream name>) ]
                  ...
                 )
             ...
           )
      LIST ('movi'
            {SubChunk | LIST ('rec '
                              SubChunk1
                              SubChunk2
                              ...
                             )
               ...
            }
            ...
           )
      ['idx1' (<AVI Index>) ]
     )
 * 
 */

public class AviCreator implements JmolMovieCreatorInterface {
/*
  final int AVIF_HASINDEX = 0x10;
  final int AVIF_MUSTUSEINDEX = 0x20;
  final int AVIF_ISINTERLEAVED = 0x100;
  final int AVIF_TRUSTCKTYPE = 0x800;
  final int AVIF_WASCAPTUREFILE = 0x10000;
  final int AVIF_COPYRIGHTED = 0x20000;
  final int AVIIF_KEYFRAME = 0x10;
  FileOutputStream os;
*/
  String errorMsg;

 
  @Override
  public String createMovie(Viewer vwr, String[] files, int width,
                            int height, int fps, String fileName) {
    /*
    this.vwr = vwr;
    int frames = files.length;
    JpgData[] jpgData = new JpgData[frames];
    int nBytesJpg = setJpgData(files, jpgData);
    AviHeader avih = new AviHeader(width, height, frames, fps, nBytesJpg);
    AviStrh strh = new AviStrh(frames, fps);
    AviStrf strf = new AviStrf(width, height);
    ListStrl strl = new ListStrl(strh, strf, frames);
    ListHdrl hdrl = new ListHdrl(avih, strl);
    ListMovi movi = new ListMovi(jpgData, nBytesJpg);
    RiffFile rf = new RiffFile(hdrl, movi);
    errorMsg = null;
    try {
      os = new FileOutputStream(fileName);
      rf.write();
      os.flush();
    } catch (Exception exc) {
      if (exc != null) {
        Logger.error("IO Exception", exc);
        errorMsg = exc.toString();
      }
    } finally {
      try {
        os.close();
      } catch (IOException e) {
      }
    }
    */
    return errorMsg;
  }

/*  void putInt(int i) throws IOException {
    putByte((byte)(i % 0x100));  i /= 0x100;
    putByte((byte)(i % 0x100));  i /= 0x100;
    putByte((byte)(i % 0x100));  i /= 0x100;
    putByte((byte)(i % 0x100));
  }

  void putByte(byte b) throws IOException {
    os.write(b);
  }

  void putBytes(byte[] bytes) throws IOException {
    os.write(bytes);
  }

  static int toInt(String s) {
    return 0 + s.charAt(0) 
        + (((int) s.charAt(1)) << 8)
        + (((int) s.charAt(2)) << 16) 
        + (((int) s.charAt(3)) << 24);
  }

  private class JpgData {
    File file;
    int length;
    int offset;
    int paddedLength;
    int nExtra;
    
    JpgData(File file) {
      this.file = file;
      length = (int) file.length();
      nExtra =  (4 - (length % 4)) % 4;
      paddedLength = length + nExtra;
    } 
    
    int getLength() {
      return length;
    }
    
    void setOffset(int offset) {
      this.offset = offset;
    }
  }

  private int setJpgData(String[] files, JpgData[] jpgData) {
    int nBytes = 0;
    for (int i = 0; i < files.length; i++) {
      jpgData[i] = new JpgData(new File(files[i]));
      nBytes += jpgData[i].paddedLength;
    }
    return nBytes;
  }
  
  static int HDR_SIZE = 8;
  private class Hdr {
    
    int type;
    int size;

    Hdr(String type, int size) {
      this.type = toInt(type);
      this.size = size;
    }

    void write()  throws IOException {
      putInt(type);
      putInt(size);
    }
  }

  static int LIST_HDR_SIZE = HDR_SIZE + 4;
  private class ListHdr {
    
    Hdr hdr;
    int type;

    ListHdr(String type, int size) {
      hdr = new Hdr("LIST", size + 4);
      this.type = toInt(type);
    }

    void write()  throws IOException {
      hdr.write();
      putInt(type);
    }
  }

  private class ListMovi {
    
    JpgData[] jpgData;
    int nBytesJpg;
    int frames;
    
    ListMovi(JpgData[] jpgData, int nBytesJpg) {
      this.jpgData = jpgData;
      this.nBytesJpg = nBytesJpg;
      frames = jpgData.length;
      listHdr = new ListHdr("movi", getLength(false) - LIST_HDR_SIZE);
    }
    
    int getLength(boolean addIndex) {
      return LIST_HDR_SIZE + 8 * frames + nBytesJpg + 
        (addIndex ? HDR_SIZE + 16 * frames : 0);
    }
    
    ListHdr listHdr;

    boolean write()  throws IOException {
      listHdr.write();
      int offset = HDR_SIZE + LIST_HDRL_SIZE + HDR_SIZE + HDR_SIZE;
      for (int i = 0; i < frames; i++) {
        putInt(toInt("00db"));
        JpgData d = jpgData[i];
        d.offset = offset;
        offset += d.paddedLength;
        putInt(d.paddedLength);
        Object ret = vwr.getFileAsBytes(d.file.getAbsolutePath());
        if (ret instanceof String) {
          errorMsg = (String) ret;
          return false;
        }
        byte[] bytes = (byte[]) ret;
        if (bytes.length > 10) {
          bytes[6] = (byte) 'a';
          bytes[7] = (byte) 'v';
          bytes[8] = (byte) 'i';
          bytes[9] = (byte) '1';
        }
        putBytes(bytes);
        for (int j = 0; j < d.nExtra; j++)
          putByte((byte) 0);
      }
      (new Hdr("idx1", 16 * frames)).write();
      for (int i = 0; i < jpgData.length; i++) {
        (new Hdr("00db", AVIIF_KEYFRAME)).write();
        JpgData d = jpgData[i];
        putInt(d.offset);
        putInt(d.length);
      }
      return true;
    }
  }
  
  private class RiffFile {
    
    RiffFile(ListHdrl hdrl, ListMovi movi) {
      int fileSize = 4 + LIST_HDRL_SIZE + movi.getLength(true);
      hdr = new Hdr("RIFF", fileSize);
      this.hdrl = hdrl;
      this.movi = movi;
    }

    Hdr hdr;
    int fileType = toInt("AVI ");
    ListHdrl hdrl;
    ListMovi movi;

    void write()  throws IOException {
      hdr.write();
      putInt(fileType);
      hdrl.write();
      movi.write();
    }
  }

  final int AVI_HEADER_SIZE = HDR_SIZE + 14 * 4;
  private class AviHeader {

    AviHeader(int width, int height, int frames, int fps, int nBytes) {
      dwWidth = width;
      dwHeight = height;
      dwTotalFrames = frames;
      dwMicroSecPerFrame = 1000000 / fps;
      dwMaxBytesPerSec = nBytes / frames  * fps;
    }
    
    Hdr hdr = new Hdr("avih", AVI_HEADER_SIZE - HDR_SIZE);

    int dwMicroSecPerFrame;
    int dwMaxBytesPerSec;
    int dwPaddingGranularity = 0;
    int dwFlags = 0;//AVIF_HASINDEX;
    int dwTotalFrames;
    int dwInitialFrames = 0;
    int dwStreams = 1;
    int dwSuggestedBufferSize = 0;
    int dwWidth;
    int dwHeight;
    int dwReserved1 = 0;
    int dwReserved2 = 0;
    int dwReserved3 = 0;
    int dwReserved4 = 0;

    void write()  throws IOException {
      hdr.write();
      putInt(dwMicroSecPerFrame);
      putInt(dwMaxBytesPerSec);
      putInt(dwPaddingGranularity);
      putInt(dwFlags);
      putInt(dwTotalFrames);
      putInt(dwInitialFrames);
      putInt(dwStreams);
      putInt(dwSuggestedBufferSize);
      putInt(dwWidth);
      putInt(dwHeight);
      putInt(dwReserved1);
      putInt(dwReserved2);
      putInt(dwReserved3);
      putInt(dwReserved4);
    }
  }

  final int AVI_STRH_SIZE = HDR_SIZE + 14 * 4;
  private class AviStrh {

    AviStrh(int frames, int fps) {
      length = frames;
      this.rate = fps * scale;
    }
    
    Hdr hdr = new Hdr("strh", AVI_STRH_SIZE - HDR_SIZE); 
    int type = toInt("vids");
    int handler = toInt("MJPG");
    int flags = 0;
    int priorityLanguage = 0;
    int init_frames = 0;
    int scale = 1000000;
    int rate;
    int start = 0;
    int length;
    int buff_sz = 0;
    int quality = 0;
    int sample_sz = 0;
    int leftTop = 0;
    int rightBottom = 0;

    void write()  throws IOException {
      hdr.write();
      putInt(type);
      putInt(handler);
      putInt(flags);
      putInt(priorityLanguage);
      putInt(init_frames);
      putInt(scale);
      putInt(rate);
      putInt(start);
      putInt(length);
      putInt(buff_sz);
      putInt(quality);
      putInt(sample_sz);
      putInt(leftTop);
      putInt(rightBottom);
    }

  }

  final int AVI_STRF_SIZE = HDR_SIZE + 16 * 4;
  private class AviStrf {

    AviStrf(int width, int height) {
      this.width = width;
      this.height = height;
      image_sz = width * height * 6; // maybe twice this?
    }

    int size = AVI_STRF_SIZE - HDR_SIZE;
    Hdr hdr = new Hdr("strf", size);
    int width;
    int height;
    int planes_bit_cnt = 1 + 24 * 256 * 256; // 0x  
    int compression = toInt("MJPG");
    int image_sz;
    int xpels_meter;
    int ypels_meter;
    int num_colors;
    int imp_colors;
    int x1 = 18;
    int x2 = 0;
    int x3 = 2;
    int x4 = 8;
    int x5 = 2;
    int x6 = 2;
    
    void write()  throws IOException {
      hdr.write();
      putInt(size);
      putInt(width);
      putInt(height);
      putInt(planes_bit_cnt);
      putInt(compression);
      putInt(image_sz);
      putInt(xpels_meter);
      putInt(ypels_meter);
      putInt(num_colors);
      putInt(imp_colors);
      putInt(x1);
      putInt(x2);
      putInt(x3);
      putInt(x4);
      putInt(x5);
      putInt(x6);
    }
  }

  final int LIST_ODML_SIZE = LIST_HDR_SIZE + HDR_SIZE + 4;
  private class ListOdml {

    ListOdml(int frames) {
      this.frames = frames;
    }
    
    ListHdr listHdr = new ListHdr("odml", LIST_ODML_SIZE - LIST_HDR_SIZE);
    Hdr hdr = new Hdr("dmlh", 4);
    int frames;
    
    void write()  throws IOException {
      listHdr.write();
      hdr.write();
      putInt(frames);
    }
  }

  final int LIST_STRL_SIZE = LIST_HDR_SIZE + AVI_STRH_SIZE + AVI_STRF_SIZE + LIST_ODML_SIZE;  
  private class ListStrl {

    ListStrl(AviStrh strh, AviStrf strf, int frames) {
      this.strh = strh;
      this.strf = strf;
      listOdml = new ListOdml(frames);
    }
    
    ListHdr listHdr = new ListHdr("strl", LIST_STRL_SIZE);
    AviStrh strh;
    AviStrf strf;
    ListOdml listOdml;

    void write()  throws IOException {
      listHdr.write();
      strh.write();
      strf.write();
      listOdml.write();
    }
  }

  
  LIST ('hdrl'
      'avih'(<Main AVI Header>)
      LIST ('strl'
            'strh'(<Stream header>)
            'strf'(<Stream format>)
            [ 'strd'(<Additional header data>) ]
            [ 'strn'(<Stream name>) ]
            ...
           )
       ...
     )
  
  final int LIST_HDRL_SIZE = LIST_HDR_SIZE + AVI_HEADER_SIZE + LIST_STRL_SIZE;
  private class ListHdrl {
    
    
    ListHdrl(AviHeader avih, ListStrl strl) {
      this.avih = avih;
      this.strl = strl;
    }

    ListHdr listHdr = new ListHdr("hdrl", LIST_HDRL_SIZE - LIST_HDR_SIZE);
    AviHeader avih;
    ListStrl strl;

    void write()  throws IOException {
      System.out.println(LIST_HDR_SIZE + " " + AVI_HEADER_SIZE + " " + LIST_STRL_SIZE);
      listHdr.write();
      avih.write();
      strl.write();
    }
  }
*/
}
