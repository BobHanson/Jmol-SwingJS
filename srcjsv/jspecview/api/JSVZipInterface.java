package jspecview.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

public interface JSVZipInterface {

	public abstract InputStream newGZIPInputStream(InputStream bis)
			throws IOException;

	public abstract BufferedReader newJSVZipFileSequentialReader(InputStream in,
			String[] subFileList, String startCode);

}