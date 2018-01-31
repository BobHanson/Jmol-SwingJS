package jspecview.api;

import java.io.BufferedReader;
import java.io.InputStream;

public interface JSVZipReader {

	BufferedReader set(InputStream in, String[] subFileList, String startCode);

}
