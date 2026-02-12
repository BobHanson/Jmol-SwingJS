/**
 * File written by Raphael Champeimont
 * UMR 7238 Genomique des Microorganismes
 */
package fr.orsay.lri.varna.models.templates;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import fr.orsay.lri.varna.factories.RNAFactory;

public class BatchBenchmarkPrepare {

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
		if (!seqdir.exists()) {
			seqdir.mkdir();
		}
		
		File templateFile = new File(rootdir, "template.xml");
		
		ArrayList<String> seqnames = new ArrayList<String>();
		ArrayList<String> sequences = new ArrayList<String>();
		BatchBenchmark.readFASTA(new File(rootdir, "alignment.fasta"), seqnames, sequences);
		
		BufferedWriter outbufASS = new BufferedWriter(new FileWriter(new File(rootdir, "all_secondary_structures.fasta")));
		
		String consensusSecStr = sequences.get(0);
		int[] consensusSecStrInt = RNAFactory.parseSecStr(consensusSecStr);
		
		List<File> templates = new ArrayList<File>();
		for (int i=1; i<seqnames.size(); i++) {
			String seqname = seqnames.get(i);
			String sequence = sequences.get(i);
			String sequenceUngapped = sequence.replaceAll("[\\.-]", "");
			System.out.println(seqname);
			String ss = "";
			String nt = "";
			for (int j=0; j<sequence.length(); j++) {
				if (sequence.charAt(j) != '.' && sequence.charAt(j) != '-') {
					if (consensusSecStr.charAt(j) == '-' || consensusSecStr.charAt(j) == '.') {
						nt += sequence.charAt(j);
						ss += '.';
					} else {
						int k = consensusSecStrInt[j];
						// k is the matching base, is it aligned to a base in our sequence?
						if (sequence.charAt(k) != '.' && sequence.charAt(k) != '-') {
							nt += sequence.charAt(j);
							ss += consensusSecStr.charAt(j);
						} else {
							nt += sequence.charAt(j);
							ss += '.';
						}
					}
				}
			}
			
			if (!sequenceUngapped.equals(nt)) {
				System.out.println(sequenceUngapped);
				System.out.println(nt);
				throw new Error("bug");
			}
			
			// We now have the sequence with its secondary structure.
			File outfile = new File(seqdir, seqname + ".dbn");
			BufferedWriter outbuf = new BufferedWriter(new FileWriter(outfile));
			outbuf.write(">" + seqname + "\n");
			outbuf.write(nt + "\n");
			outbuf.write(ss + "\n");
			outbuf.close();
			
			outbufASS.write(">" + seqname + "\n");
			outbufASS.write(ss + "\n");
			
			templates.add(templateFile);
		}
		
		outbufASS.close();
		
	}
	
	public static void main(String[] args) throws Exception {
		new BatchBenchmarkPrepare().benchmarkAllDir(new File(new File("templates"), "RNaseP_bact_a"));
	}
	
}
