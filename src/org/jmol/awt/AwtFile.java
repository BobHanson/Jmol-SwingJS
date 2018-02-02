package org.jmol.awt;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlException;

import org.jmol.api.GenericFileInterface;
import org.jmol.viewer.Viewer;

import javajs.util.PT;


/**
 * a subclass of File allowing extension to JavaScript
 * 
 * private to org.jmol.awt
 * 
 */

class AwtFile extends File implements GenericFileInterface {

  AwtFile(String name) {
    super(name);
  }

  @Override
  public GenericFileInterface getParentAsFile() {
    AwtFile f = null;
    try {
      File file = getParentFile();
      f = new AwtFile(file.getAbsolutePath());
    } catch (AccessControlException e) {
      //
    }
    return f;
  }

  static Object getBufferedFileInputStream(String name) {
    File file = new File(name);
    try {
      return new BufferedInputStream(new FileInputStream(file));
    } catch (IOException e) {
      return e.toString();//e.getMessage();
    }
  }

  static Object getURLContents(URL url, byte[] outputBytes,
                                          String post) {
    URLConnection conn = null;
    try {
      conn = url.openConnection();
      String type = null;
      //conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
      //Map<String, List<String>> x = conn.getRequestProperties();
      if (outputBytes != null) {
        type = "application/octet-stream;";
      } else if (post != null) {
        type = "application/x-www-form-urlencoded";
      }
      conn.setRequestProperty("User-Agent", Viewer.getJmolVersion());
      if (type != null) {
        conn.setRequestProperty("Content-Type", type);
        conn.setDoOutput(true);
        if (outputBytes == null) {
          OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
          wr.write(post);
          wr.flush();
        } else {
          conn.getOutputStream().write(outputBytes);
          conn.getOutputStream().flush();
        }
      }
      return new BufferedInputStream(conn.getInputStream());
    } catch (IOException e) {
      return e.toString();
    }
  }

  @Override
  public String getFullPath() {
    try {
     return getAbsolutePath();
    } catch (Exception e) {
     return null;
      // Unsigned applet cannot do this.
    }
  }
  
  private final static String[] urlPrefixPairs = { "http:", "http://", "www.",
    "http://www.", "https:", "https://", "ftp:", "ftp://", "file:",
    "file:///" };

  static String getLocalUrl(GenericFileInterface file) {
    // entering a url on a file input box will be accepted,
    // but cause an error later. We can fix that...
    // return null if there is no problem, the real url if there is
    if (file.getName().startsWith("="))
      return file.getName();
    String path = file.getFullPath();
    if (path == null)
      return null;
      path = path.replace('\\', '/');
    for (int i = 0; i < urlPrefixPairs.length; i++)
      if (path.indexOf(urlPrefixPairs[i]) == 0)
        return null;
    // looking for /xxx/xxxx/file://...
    for (int i = 0; i < urlPrefixPairs.length; i += 2)
      if (path.indexOf(urlPrefixPairs[i]) > 0)
        return urlPrefixPairs[i + 1]
            + PT.trim(path.substring(path.indexOf(urlPrefixPairs[i])
                + urlPrefixPairs[i].length()), "/");
    return null;
  }

}
