/**
 * File written by Raphael Champeimont
 * UMR 7238 Genomique des Microorganismes
 */
package fr.orsay.lri.varna.models.templates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import fr.orsay.lri.varna.exceptions.ExceptionExportFailed;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionLoadingFailed;
import fr.orsay.lri.varna.exceptions.ExceptionNAViewAlgorithm;
import fr.orsay.lri.varna.exceptions.ExceptionPermissionDenied;
import fr.orsay.lri.varna.exceptions.ExceptionUnmatchedClosingParentheses;
import fr.orsay.lri.varna.exceptions.ExceptionXmlLoading;
import fr.orsay.lri.varna.factories.RNAFactory;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.rna.RNA;


public class BatchBenchmark {
	private VARNAConfig conf = new VARNAConfig();
	
	final boolean DEFAULT_STRAIGHT_BULGES = false;
	
	public static RNA loadRNA(File file) throws ExceptionFileFormatOrSyntax, ExceptionUnmatchedClosingParentheses, FileNotFoundException, ExceptionExportFailed, ExceptionPermissionDenied, ExceptionLoadingFailed {
		Collection<RNA> rnas = RNAFactory.loadSecStr(file.getPath());
		if (rnas.isEmpty()) {
			throw new ExceptionFileFormatOrSyntax(
					"No RNA could be parsed from that source.");
		}
		return rnas.iterator().next();
	}
	
	public void benchmarkRNA(File templatePath, File rnaPath, BufferedWriter outbuf) throws ExceptionXmlLoading, RNATemplateDrawingAlgorithmException, ExceptionFileFormatOrSyntax, ExceptionUnmatchedClosingParentheses, ExceptionExportFailed, ExceptionPermissionDenied, ExceptionLoadingFailed, ExceptionNAViewAlgorithm, IOException {
		// load template
		RNATemplate template = RNATemplate.fromXMLFile(templatePath);
		
		// load RNA
		RNA rna = loadRNA(rnaPath);
		
		for (int algo=0; algo<=100; algo++) {
			String algoname = "";
		
			// draw RNA
			switch (algo) {
			//case 0:
			//	rna.drawRNALine(conf);
			//	algoname = "Linear";
			//	break;
			//case 1:
			//	rna.drawRNACircle(conf);
			//	algoname = "Circular";
			//	break;
			case 2:
				rna.drawRNARadiate(conf);
				algoname = "Radiate";
				break;
			case 3:
				rna.drawRNANAView(conf);
				algoname = "NAView";
				break;
			case 10:
				algoname = "Template/noadj";
				rna.drawRNATemplate(template, conf, DrawRNATemplateMethod.NOADJUST, DrawRNATemplateCurveMethod.EXACTLY_AS_IN_TEMPLATE, DEFAULT_STRAIGHT_BULGES);
				break;
			case 11:
				algoname = "Template/noadj/ellipses";
				rna.drawRNATemplate(template, conf, DrawRNATemplateMethod.NOADJUST, DrawRNATemplateCurveMethod.ALWAYS_REPLACE_BY_ELLIPSES, DEFAULT_STRAIGHT_BULGES);
				break;
			case 12:
				algoname = "Template/noadj/smart";
				rna.drawRNATemplate(template, conf, DrawRNATemplateMethod.NOADJUST, DrawRNATemplateCurveMethod.SMART, DEFAULT_STRAIGHT_BULGES);
				break;
				/*
			case 5:
				algoname = "Template/maxfactor";
				rna.drawRNATemplate(template, conf, DrawRNATemplateMethod.MAXSCALINGFACTOR, DrawRNATemplateCurveMethod.EXACTLY_AS_IN_TEMPLATE, DEFAULT_STRAIGHT_BULGES);
				break;
				*/
			case 6:
				algoname = "Template/mininter";
				rna.drawRNATemplate(template, conf, DrawRNATemplateMethod.NOINTERSECT, DrawRNATemplateCurveMethod.EXACTLY_AS_IN_TEMPLATE, DEFAULT_STRAIGHT_BULGES);
				break;
			case 30:
				algoname = "Template/translate";
				rna.drawRNATemplate(template, conf, DrawRNATemplateMethod.HELIXTRANSLATE, DrawRNATemplateCurveMethod.EXACTLY_AS_IN_TEMPLATE, DEFAULT_STRAIGHT_BULGES);
				break;
			case 31:
				algoname = "Template/translate/ellipses";
				rna.drawRNATemplate(template, conf, DrawRNATemplateMethod.HELIXTRANSLATE, DrawRNATemplateCurveMethod.ALWAYS_REPLACE_BY_ELLIPSES, DEFAULT_STRAIGHT_BULGES);
				break;
			case 32:
				algoname = "Template/translate/smart";
				rna.drawRNATemplate(template, conf, DrawRNATemplateMethod.HELIXTRANSLATE, DrawRNATemplateCurveMethod.SMART, DEFAULT_STRAIGHT_BULGES);
				break;
			default:
				continue;
			}
		
			// benchmark
			Benchmark benchmark = new Benchmark(rna);
			
			// print results
			outbuf.write(
					removeExt(rnaPath.getName())
					+ "\t" + algoname
					+ "\t" + benchmark.backboneCrossings
					// averageUnpairedDistance % -> best is 100
					+ "\t" + (benchmark.averageUnpairedDistance / benchmark.targetConsecutiveBaseDistance *100)
					+ "\t" + benchmark.tooNearConsecutiveBases
					+ "\t" + benchmark.tooFarConsecutiveBases
					+ "\n");
		}
		
	}
	
	public void runBenchmark(List<File> templates, List<File> rnas, File outfile) throws Exception {
		if (templates.size() != rnas.size()) {
			throw new Error("templates and rnas list size differ");
		}
		
		BufferedWriter outbuf = new BufferedWriter(new FileWriter(outfile));
		
		outbuf.write("RNA\tAlgorithm\tBackbone crossings\tAverage unpaired distance %\tToo near\tToo far\n");
		
		for (int i=0; i<templates.size(); i++) {
			System.out.println("Benchmarking for RNA " + removeExt(rnas.get(i).getName()));
			benchmarkRNA(templates.get(i), rnas.get(i), outbuf);
		}
		
		outbuf.close();
		
		System.out.println("******* Benchmark finished. *******");
	}
	
	public void runExamples() throws Exception {
		File templatesDir = new File("templates");
		File root = new File(templatesDir, "examples");
		File outfile = new File(new File(templatesDir, "benchmark"), "benchmark.txt");
		
		String seqlist[] = {"RNase P E Coli.ct", "RNase P Synechocystis-PCC6803.ct", "RNase P M Musculus.ct"};
		
		List<File> templates = new ArrayList<File>();
		List<File> rnas = new ArrayList<File>();
		
		for (String seq: seqlist) {
			templates.add(new File(root, "RNase P E Coli.xml"));
			rnas.add(new File(root, seq));
		}
		
		runBenchmark(templates, rnas, outfile);
	}
	
	public static void readFASTA(File file, List<String> seqnames, List<String> sequences) throws IOException {
		BufferedReader buf = new BufferedReader(new FileReader(file));
		String line = buf.readLine();
		while (line != null) {
			if (line.length() != 0) {
				if (line.charAt(0) == '>') {
					String id = line.substring(1); // remove the >
					seqnames.add(id);
					sequences.add("");
				} else {
					sequences.set(sequences.size()-1, sequences.get(sequences.size()-1) + line);
				}
			}
			line = buf.readLine();
		}
		buf.close();
	}
	
	
	/**
	 * We assume given directory contains a alignemnt.fasta file,
	 * of which the first sequence is the consensus structure,
	 * and the other sequences are aligned nucleotides. 
	 * The principle is to convert it to a set of secondary structure,
	 * using the following rule:
	 * - keep the same nucleotides as in original sequence
	 * - keep base pairs where both bases of the pair are non-gaps in our sequence
	 */
	public void benchmarkAllDir(File rootdir) throws Exception {
		File seqdir = new File(rootdir, "sequences");
		File templateFile = new File(rootdir, "template.xml");
		File sequenceFiles[] = seqdir.listFiles();
		Arrays.sort(sequenceFiles);
		
		List<File> templates = new ArrayList<File>();
		List<File> rnas = new ArrayList<File>();
		for (File seq: sequenceFiles) {
			if (!seq.getPath().endsWith(".dbn")) continue;
			rnas.add(seq);
			templates.add(templateFile);
		}
		
		File outfile = new File(rootdir, "benchmark.txt");
		runBenchmark(templates, rnas, outfile);
		
	}
	
	
	public static void main(String[] args) throws Exception {
		File templatesDir = new File("templates");
		if (args.length < 1) {
			System.out.println("Command-line argument required: RNA");
			System.out.println("Example: RNaseP_bact_a");
			System.exit(1);
		}
		//new BatchBenchmark().runExamples();
		for (String arg: args) {
			new BatchBenchmark().benchmarkAllDir(new File(templatesDir, arg));
		}
	}
	
	/**
	 * Return the given file path without the (last) extension. 
	 */
	public static String removeExt(String path) {
		return path.substring(0, path.lastIndexOf('.'));
	}
	
	public static File removeExt(File path) {
		return new File(removeExt(path.getPath()));
	}
}
