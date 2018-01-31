package javajs.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javajs.api.JmolObjectInterface;

/**
 * 
 * A method to allow a JavaScript Ajax 
 * 
 */
public class AjaxURLConnection extends URLConnection {

  protected AjaxURLConnection(URL url) {
    super(url);
  }

  byte[] bytesOut;
  String postOut = "";

  /**
   * 
   * doAjax() is where the synchronous call to AJAX is to happen. or at least
   * where we wait for the asynchronous call to return. This method should fill
   * the dataIn field with either a string or byte array, or null if you want to
   * throw an error.
   * 
   * url, bytesOut, and postOut are all available for use
   * 
   * the method is "private", but in JavaScript that can still be overloaded.
   * Just set something to org.jmol.awtjs.JmolURLConnection.prototype.doAjax
   * 
   * @return file data as a javajs.util.SB or byte[] depending upon the file
   *         type.
   * 
   * 
   */
  @SuppressWarnings("null")
  private Object doAjax() {
    JmolObjectInterface jmol = null;
    /**
     * @j2sNative
     * 
     *            jmol = Jmol;
     * 
     */
    {
    }
    return jmol._doAjax(url, postOut, bytesOut);
  }

  @Override
  public void connect() throws IOException {
    // not expected to be used. 
  }

  public void outputBytes(byte[] bytes) {
    //      type = "application/octet-stream;";
    bytesOut = bytes;
  }

  public void outputString(String post) {
    postOut = post;
    //     type = "application/x-www-form-urlencoded";
  }

  @Override
  public InputStream getInputStream() {
    InputStream is = null;
    Object o = doAjax();
    if (AU.isAB(o))
      is = Rdr.getBIS((byte[]) o);
    else if (o instanceof SB) 
      is = Rdr.getBIS(Rdr.getBytesFromSB((SB)o));
    else if (o instanceof String)
      is = Rdr.getBIS(((String) o).getBytes());
    return is;
  }
  /**
   * @return javajs.util.SB or byte[], depending upon the file type
   */
  public Object getContents() {
    return doAjax();
  }

}
