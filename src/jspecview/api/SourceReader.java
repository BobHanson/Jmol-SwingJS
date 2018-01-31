package jspecview.api;

import java.io.BufferedReader;

import jspecview.source.JDXSource;

public interface SourceReader {

	JDXSource getSource(String filePath, BufferedReader br);

}
