/**
 * File written by Raphael Champeimont
 * UMR 7238 Genomique des Microorganismes
 */
package fr.orsay.lri.varna.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TranslateFormatRNaseP {
	public static void main(String[] args) throws Exception {
		File templatesDir = new File(new File(System.getProperty("user.dir")), "templates");
		File infile = new File(new File(templatesDir, "RNaseP_bact_a"), "a_bacterial_rnas.gb");
		File outfile = new File(new File(templatesDir, "RNaseP_bact_a"), "alignment.fasta");
		
		BufferedReader inbuf = new BufferedReader(new FileReader(infile));
		String line = inbuf.readLine();
		String seqname;
		List<String> seqnames = new ArrayList<String>();
		List<String> sequences = new ArrayList<String>();
		while (line != null) {
			if (line.length() != 0) {
				if (line.startsWith("LOCUS")) {
					String parts[] = line.split("\\s+");
					seqname = parts[1];
					seqnames.add(seqname);
					sequences.add("");
				}
				if (line.startsWith(" ")) {
					String parts[] = line.split("\\s+");
					for (int i=2; i<parts.length; i++) {
						sequences.set(sequences.size()-1, sequences.get(sequences.size()-1) + parts[i]);
					}
				}
			}
			line = inbuf.readLine();
		}
		inbuf.close();
		
		BufferedWriter outbuf = new BufferedWriter(new FileWriter(outfile));
		for (int i=2; i<seqnames.size(); i++) {
			outbuf.write(">" + seqnames.get(i) + "\n");
			outbuf.write(sequences.get(i) + "\n");
		}
		outbuf.close();
	}
}
