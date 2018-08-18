package jspecview.java;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlException;

import javajs.api.GenericFileInterface;


/**
 * a subclass of File allowing extension to JavaScript
 * 
 * private to jspecview.java
 * 
 */

class AwtFile extends File implements GenericFileInterface {

	AwtFile(String name) {
		super(name);
	}

	@Override
	public GenericFileInterface getParentAsFile() {
		// used in printPDF only
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
			return e.toString();// e.getMessage();
		}
	}

	static Object getURLContents(URL url, byte[] outputBytes,
			String post) {
		try {
			URLConnection conn = url.openConnection();
			String type = null;
			if (outputBytes != null) {
				type = "application/octet-stream;";
			} else if (post != null) {
				type = "application/x-www-form-urlencoded";
			}
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
			return e.getMessage();
		}
	}

	@Override
	public String getFullPath() {
    try {
      return getAbsolutePath();
    } catch (Exception e) {
      // Unsigned applet cannot do this.
    	return null;
    }
	}

}
