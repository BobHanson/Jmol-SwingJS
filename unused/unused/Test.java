package jspecview.unused;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.Inflater;

import jspecview.common.JSVFileManager;

public class Test {

	public Test() {
		try {
//			// Encode a String into bytes
//			String inputString = "blahblahblah??";
//			byte[] input = inputString.getBytes("UTF-8");
//
//			// Compress the bytes
//			byte[] output = new byte[100];
//			Deflater compresser = new Deflater();
//			compresser.setInput(input);
//			compresser.finish();
//			int compressedDataLength = compresser.deflate(output);
//
//			// Decompress the bytes
//			Inflater decompresser = new Inflater();
//			decompresser.setInput(output, 0, compressedDataLength);
//			byte[] result = new byte[100];
//			int resultLength = decompresser.inflate(result);
//			decompresser.end();
//
//			// Decode the bytes into a String
//			String outputString = new String(result, 0, resultLength, "UTF-8");
//			System.out.println(outputString);

			//checkStream("c.pdf", 785, 22927);
			//checkStream("cholesterol.pdf", 57, 44);
			//checkStream("tp.pdf", 57, 29);
			//checkStream("b.pdf", 633, 123744);
			//checkStream("tn-peaks.pdf", 785, 49067);
			checkStream("tn-peaks.pdf", 57, 44);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void checkStream(String fname, int ptStream, int len) throws Exception {
		fname = "C:/temp/" + fname;
		InputStream is = JSVFileManager.getInputStream(fname, false, null);
		is.skip(ptStream + 6);
		byte[] b = new byte[len];
		is.read(b);
		Inflater d = new Inflater();
		d.setInput(b, 0, b.length);
		byte[] b2 = new byte[b.length * 10];
		len = d.inflate(b2);
		System.out.println(fname + "\t ptStream=" + ptStream + "\t len=" + len);
		d.end();
		FileOutputStream fos = new FileOutputStream(new File(fname + ".txt"));
		fos.write(b2, 0, len);
		fos.flush();
		fos.close();		
	}
}
