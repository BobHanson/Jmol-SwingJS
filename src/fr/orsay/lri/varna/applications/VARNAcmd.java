/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Université Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.applications;


import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Vector;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JFrame;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.exceptions.ExceptionExportFailed;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionJPEGEncoding;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.exceptions.ExceptionModeleStyleBaseSyntaxError;
import fr.orsay.lri.varna.exceptions.ExceptionNonEqualLength;
import fr.orsay.lri.varna.exceptions.ExceptionParameterError;
import fr.orsay.lri.varna.exceptions.ExceptionPermissionDenied;
import fr.orsay.lri.varna.exceptions.ExceptionUnmatchedClosingParentheses;
import fr.orsay.lri.varna.exceptions.ExceptionWritingForbidden;
import fr.orsay.lri.varna.factories.RNAFactory;
import fr.orsay.lri.varna.interfaces.InterfaceParameterLoader;
import fr.orsay.lri.varna.models.FullBackup;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.VARNAConfigLoader;
import fr.orsay.lri.varna.models.rna.RNA;

public class VARNAcmd implements InterfaceParameterLoader {
	
	public class ExitCode extends Exception{
		/**
		 * 
		 */
		private static final long serialVersionUID = -3011196062868355584L;
		private int _c;
		private String _msg;
		public ExitCode(int c,String msg){
			_c = c;
			_msg = msg;
		}
		public int getExitCode(){
			return _c;
		}
		public String getExitMessage(){
			return _msg;
		}
	}
	
	
	private Hashtable<String, String> _optsValues = new Hashtable<String, String>();
	private Hashtable<String, String> _basicOptsInv = new Hashtable<String, String>();
	private String _inFile = "";
	private String _outFile = "";
	int _baseWidth = 400;
	double _scale = 1.0;
	float _quality = 0.9f;

	private String[] _basicOptions = { VARNAConfigLoader.algoOpt,
			VARNAConfigLoader.bpStyleOpt, VARNAConfigLoader.bondColorOpt,
			VARNAConfigLoader.backboneColorOpt, VARNAConfigLoader.periodNumOpt,
			VARNAConfigLoader.baseInnerColorOpt,
			VARNAConfigLoader.baseOutlineColorOpt,

	};

	public VARNAcmd(Vector<String> args) throws ExitCode {
		for (int j = 0; j < _basicOptions.length; j++) {
			_basicOptsInv.put(_basicOptions[j], _basicOptions[j]);
		}
		int i = 0;
		while (i < args.size()) {
			String opt = args.elementAt(i);
			if (opt.charAt(0) != '-') {
				errorExit("Missing or unknown option \"" + opt + "\"");
			}
			if (opt.equals("-h")) {
				displayLightHelpExit();
			}
			if (opt.equals("-x")) {
				displayDetailledHelpExit();
			} else {
				if (i + 1 >= args.size()) {
					errorExit("Missing argument for option \"" + opt + "\"");
				}
				String val = args.get(i + 1);
				if (opt.equals("-i")) {
					_inFile = val;
				} else if (opt.equals("-o")) {
					_outFile = val;
				} else if (opt.equals("-quality")) {
					_quality = Float.parseFloat(val);
				} else if (opt.equals("-resolution")) {
					_scale = Float.parseFloat(val);
				} else {
					addOption(opt, val);
				}
			}
			i += 2;
		}
	}

	public void addOption(String key, String value) {
		if (key.equals("-i")) {
			_inFile = value;
		} else if (key.equals("-o")) {
			_outFile = value;
		} else {
			_optsValues.put(key.substring(1), value);
		}
	}

	private String getDescription() {
		return "VARNA v"
				+ VARNAConfig.MAJOR_VERSION
				+ "."
				+ VARNAConfig.MINOR_VERSION
				+ " Assisted drawing of RNA secondary structure (Command Line version)";
	}

	private String indent(int k) {
		String result = "";
		for (int i = 0; i < k; i++) {
			result += "  ";
		}
		return result;
	}

	private String complete(String s, int k) {
		String result = s;
		while (result.length() < k) {
			result += " ";
		}
		return result;
	}

	Vector<String[]> matrix = new Vector<String[]>();

	private void addLine(String opt, String val) {
		String[] line = { opt, val };
		matrix.add(line);
	}

	private static int MAX_WIDTH = 100;

	@SuppressWarnings("unchecked")
	private void printMatrix(int ind) {
		String[][] values = new String[matrix.size()][];
		matrix.toArray(values);
		Arrays.sort(values, new Comparator() {
			public int compare(Object o1, Object o2) {
				String[] tab1 = (String[]) o1;
				String[] tab2 = (String[]) o2;
				return tab1[0].compareTo(tab2[0]);
			}
		});

		int maxSize = 0;
		for (int i = 0; i < values.length; i++) {
			String[] elem = values[i];
			maxSize = Math.max(maxSize, elem[0].length());
		}
		maxSize += ind + 2;
		for (int i = 0; i < values.length; i++) {
			String[] elem = values[i];
			String opt = elem[0];
			String msg = elem[1];
			opt = complete("", ind) + "-" + complete(opt, maxSize - ind);
			System.out.println(opt	+ msg.substring(0, Math.min(MAX_WIDTH - opt.length(), msg.length())));
			if (opt.length() + msg.length() >= MAX_WIDTH) {
				int off = MAX_WIDTH - opt.length();
				while (off < msg.length()) {
					String nmsg = msg.substring(off, Math.min(off + MAX_WIDTH
							- opt.length(), msg.length()));
					System.out.println(complete("", opt.length())+nmsg);
					off += MAX_WIDTH - opt.length();
				}
			}
		} 
		matrix = new Vector<String[]>();
	}

	private void printUsage() {
		System.out
				.println("Usage: java -cp . [-i InFile|-sequenceDBN XXX -structureDBN YYY] -o OutFile [Options]");
		System.out.println("Where:");
		System.out.println(indent(1)
				+ "OutFile\tSupported formats: {JPEG,PNG,EPS,XFIG,SVG}");
		System.out
				.println(indent(1)
						+ "InFile\tSecondary structure file: Supported formats: {BPSEQ,CT,RNAML,DBN}");

	}

	private void printHelpOptions() {
		System.out.println("\nMain options:");
		addLine("h", "Displays a short description of main options and exits");
		addLine("x", "Displays a detailled description of all options");
		printMatrix(2);
	}

	private void printMainOptions(String[][] info) {
		System.out.println("\nMain options:");
		addLine("h", "Displays a short description of main options and exits");
		addLine("x", "Displays a detailled description of all options");
		for (int i = 0; i < info.length; i++) {
			String key = info[i][0];
			if (_basicOptsInv.containsKey(key)) {
				addLine(key, info[i][2]);
			}
		}
		printMatrix(2);
	}

	private void printAdvancedOptions(String[][] info) {
		System.out.println("\nAdvanced options:");
		for (int i = 0; i < info.length; i++) {
			String key = info[i][0];
			if (!_basicOptsInv.containsKey(key)) {
				addLine(key, info[i][2]);
			}
		}
		addLine("quality", "Sets quality (non-vector file formats only)");
		addLine("resolution", "Sets resolution (non-vector file formats only)");
		printMatrix(2);
	}

	private void displayLightHelpExit() throws ExitCode {
		String[][] info = VARNAConfigLoader.getParameterInfo();
		System.out.println(getDescription());
		printUsage();
		printMainOptions(info);
		throw(new ExitCode(1,""));
	}

	private void displayDetailledHelpExit() throws ExitCode {
		String[][] info = VARNAConfigLoader.getParameterInfo();
		System.out.println(getDescription());
		printUsage();
		printMainOptions(info);
		printAdvancedOptions(info);
		throw(new ExitCode(1,""));
	}

	private void errorExit(String msg) throws ExitCode {
		System.out.println(getDescription());
		System.out.println("Error: " + msg + "\n");
		printUsage();
		printHelpOptions();
		throw(new ExitCode(1,""));
	}

	public String getParameterValue(String key, String def) {
		if (_optsValues.containsKey(key)) {
			return _optsValues.get(key);
		}
		return def;
	}

	public String formatOutputPath(String base,int index, int total)
	{
		String result = base;
		
		if (total>1)
		{
			int indexDot = base.lastIndexOf('.');
			String pref;
			String ext;
			if (indexDot!=-1)
			{
			  pref = base.substring(0,indexDot);
			  ext = base.substring(indexDot);
			}
			else{
				pref=base;
				ext="";
			}
			result = pref+"-"+index+ext;
		}
		System.err.println("Output file: "+result);
		return result;
	}
	
	public void run() throws IOException, ExitCode {
		VARNAConfigLoader VARNAcfg = new VARNAConfigLoader(this);
		ArrayList<VARNAPanel> vpl;
		ArrayList<FullBackup> confs = new ArrayList<FullBackup>();
		try {
			if (!_inFile.equals("")) {
				if (!_inFile.toLowerCase().endsWith(".varna")) {
					Collection<RNA> rnas = RNAFactory.loadSecStr(_inFile);
					 if (rnas.isEmpty())
					 {
						 FullBackup f = null;
								try{
								f = VARNAPanel.importSession(new FileInputStream(_inFile), _inFile);
								confs.add(f);
								}
								catch(Exception e)
								{
									e.printStackTrace();
								}
						if (f==null)
						{
						 throw new ExceptionFileFormatOrSyntax("No RNA could be parsed from file '"+_inFile+"'.");
						}
					 }
					 else{
						 for (RNA r: rnas)
						 {
							 confs.add(new FullBackup(r,_inFile));
						 }
					 }
				}
				else{
					confs.add(VARNAPanel.importSession(_inFile));
				}
			} else {
				RNA r = new RNA();
				r.setRNA(this.getParameterValue("sequenceDBN",
						""), this.getParameterValue(
						"structureDBN", ""));
				confs.add(new FullBackup(r,"From Params"));
			}
			if (!_outFile.equals(""))
			{
			int index = 1;
			for (FullBackup r: confs)
			{
				VARNAcfg.setRNA(r.rna);
				vpl = VARNAcfg.createVARNAPanels();
				if (vpl.size() > 0) {
				VARNAPanel _vp = vpl.get(0);
				if (r.hasConfig())
				{
					_vp.setConfig(r.config);
				}
				RNA _rna = _vp.getRNA();
				Rectangle2D.Double bbox = _vp.getRNA().getBBox();
				//System.out.println(_vp.getRNA().getBBox());
				
				if (_outFile.toLowerCase().endsWith(".jpeg")
						|| _outFile.toLowerCase().endsWith(".jpg")
						|| _outFile.toLowerCase().endsWith(".png"))
				{ 
					_vp.setTitleFontSize((int)(_scale*_vp.getTitleFont().getSize())); 
				    _vp.setSize((int)(_baseWidth*_scale), (int)((_scale*_baseWidth*bbox.height)/((double)bbox.width)));
				}
				
				if (_outFile.toLowerCase().endsWith(".eps")) {
					_rna.saveRNAEPS(formatOutputPath(_outFile,index, confs.size()), _vp.getConfig());
				} else if (_outFile.toLowerCase().endsWith(".xfig")
						|| _outFile.toLowerCase().endsWith(".fig")) {
					_rna.saveRNAXFIG(formatOutputPath(_outFile,index, confs.size()), _vp.getConfig());
				} else if (_outFile.toLowerCase().endsWith(".svg")) {
					_rna.saveRNASVG(formatOutputPath(_outFile,index, confs.size()), _vp.getConfig());
				} else if (_outFile.toLowerCase().endsWith(".jpeg")
						|| _outFile.toLowerCase().endsWith(".jpg")) {
					this.saveToJPEG(formatOutputPath(_outFile,index, confs.size()), _vp);
				} else if (_outFile.toLowerCase().endsWith(".png")) {
					this.saveToPNG(formatOutputPath(_outFile,index, confs.size()), _vp);
				} else if (_outFile.toLowerCase().endsWith(".varna")) {
					_vp.saveSession(formatOutputPath(_outFile,index, confs.size()));
				} else {
					errorExit("Unknown extension for output file \"" + _outFile
							+ "\"");
				}
			}
			index++;
			}
			}
			// No output file => Open GUI
			else
			{
				VARNAGUI d = new VARNAGUI();
				d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				d.pack();
				d.setVisible(true);
				for (FullBackup b: confs)
				{
					RNA r = b.rna;
					VARNAcfg.setRNA(r);
					vpl = VARNAcfg.createVARNAPanels();
					if (vpl.size() > 0) {
						VARNAPanel _vp = vpl.get(0);
						VARNAConfig cfg = _vp.getConfig();
						if (b.hasConfig())
						{
							cfg = b.config;
						}
						RNA rna = _vp.getRNA();
						d.addRNA(rna, cfg);
						
					}
				}
			}
		} catch (ExceptionWritingForbidden e) {
			e.printStackTrace();
			throw(new ExitCode(1,""));
		} catch (ExceptionJPEGEncoding e) {
			e.printStackTrace();
			throw(new ExitCode(1,""));
		} catch (ExceptionParameterError e) {
			e.printStackTrace();
			throw(new ExitCode(1,""));
		} catch (ExceptionModeleStyleBaseSyntaxError e) {
			e.printStackTrace();
			throw(new ExitCode(1,""));
		} catch (ExceptionNonEqualLength e) {
			e.printStackTrace();
			throw(new ExitCode(1,""));
		} catch (ExceptionUnmatchedClosingParentheses e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ExceptionExportFailed e) {
			e.printStackTrace();
			throw(new ExitCode(1,""));
		} catch (ExceptionPermissionDenied e) {
			e.printStackTrace();
			throw(new ExitCode(1,""));
		} catch (ExceptionLoadingFailed e) {
			e.printStackTrace();
			throw(new ExitCode(1,""));
		} catch (ExceptionFileFormatOrSyntax e) {
			e.setPath(_inFile);
			e.printStackTrace();
			throw(new ExitCode(1,""));
		} catch (FileNotFoundException e) {
			throw(new ExitCode(1,"Error: Missing input file \""+_inFile+"\"."));
		}
		
		if (!_outFile.equals(""))
			throw(new ExitCode(0,""));
		
		
	
	}

	public void saveToJPEG(String filename, VARNAPanel vp)
			throws ExceptionJPEGEncoding, ExceptionExportFailed {
		
		BufferedImage myImage = new BufferedImage((int) Math.round(vp
				.getWidth()
				), (int) Math.round(vp.getHeight() ),
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = myImage.createGraphics();
		vp.paintComponent(g2);
		try {
			FileImageOutputStream out = new FileImageOutputStream(new File(filename));
			ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
			ImageWriteParam params = writer.getDefaultWriteParam();
			params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			params.setCompressionQuality(_quality);
			writer.setOutput(out);
			IIOImage myIIOImage = new IIOImage(myImage, null, null);
			writer.write(null, myIIOImage, params);
			out.close();
		} catch (IOException e) {
			throw new ExceptionExportFailed(e.getMessage(), filename);
		}

	}

	public void saveToPNG(String filename, VARNAPanel vp)
			throws ExceptionExportFailed {
		BufferedImage myImage = new BufferedImage((int) Math.round(vp
				.getWidth()), (int) Math.round(vp.getHeight() ),
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = myImage.createGraphics();
		vp.paintComponent(g2);
		g2.dispose();
		try {
			ImageIO.write(myImage, "PNG", new File(filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] argv) {
		Vector<String> opts = new Vector<String>();
		for (int i = 0; i < argv.length; i++) {
			opts.add(argv[i]);
		}
		try {
			VARNAcmd app = new VARNAcmd(opts);
			app.run();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ExitCode e) {
			System.err.println(e.getExitMessage());
			System.exit(e.getExitCode());
		}
	}

}
