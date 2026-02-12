/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Universit� Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.utils;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.orsay.lri.varna.models.rna.ModeleBP;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.ModeleBP.Edge;
import fr.orsay.lri.varna.models.rna.ModeleBP.Stericity;

public class RNAMLParser extends DefaultHandler {
	public class HelixTemp {
		public int pos5, pos3, length;
		public String name;

		public HelixTemp(int pos5, int pos3, int length, String name) {
			this.pos3 = pos3;
			this.pos5 = pos5;
			this.length = length;
			this.name = name;
		}

		public String toString() {
			return ("[" + name + "," + pos5 + "," + pos3 + "," + length + "]");
		}

	}

	public class BPTemp {
		public int pos5, pos3;
		public String edge5, edge3, orientation;

		public BPTemp(int pos5, int pos3, String edge5, String edge3,
				String orientation) {
			if (edge3 == null) {
				edge3 = "+";
			}
			if (edge5 == null) {
				edge5 = "+";
			}
			if (orientation == null) {
				orientation = "c";
			}
			this.pos5 = pos5;
			this.pos3 = pos3;
			this.edge5 = edge5;
			this.edge3 = edge3;
			this.orientation = orientation;
		}

		public ModeleBP createBPStyle(ModeleBase mb5, ModeleBase mb3) {
			ModeleBP.Edge e5, e3;
			@SuppressWarnings("unused")
			boolean isCanonical = false;
			if (edge5.equals("W")) {
				e5 = ModeleBP.Edge.WC;
			} else if (edge5.equals("H")) {
				e5 = ModeleBP.Edge.HOOGSTEEN;
			} else if (edge5.equals("S")) {
				e5 = ModeleBP.Edge.SUGAR;
			} else {
				e5 = ModeleBP.Edge.WC;
			}

			if (edge3.equals("W")) {
				e3 = ModeleBP.Edge.WC;
			} else if (edge3.equals("H")) {
				e3 = ModeleBP.Edge.HOOGSTEEN;
			} else if (edge3.equals("S")) {
				e3 = ModeleBP.Edge.SUGAR;
			} else {
				e3 = ModeleBP.Edge.WC;
			}

			if ((edge5.equals("+") && edge3.equals("+"))
					|| (edge5.equals("-") && edge3.equals("-"))) {
				e3 = ModeleBP.Edge.WC;
				e5 = ModeleBP.Edge.WC;
			}

			ModeleBP.Stericity ster;

			if (orientation.equals("c")) {
				ster = ModeleBP.Stericity.CIS;
			} else if (orientation.equals("t")) {
				ster = ModeleBP.Stericity.TRANS;
			} else {
				ster = ModeleBP.Stericity.CIS;
			}

			return (new ModeleBP(mb5, mb3, e5, e3, ster));
		}

		public String toString() {
			return ("[" + pos5 + "," + pos3 + "," + edge5 + "," + edge3 + ","
					+ orientation + "]");
		}
	}

	public class RNATmp {
		public ArrayList<String> _sequence = new ArrayList<String>();
		public Vector<Integer> _sequenceIDs = new Vector<Integer>();
		public Vector<BPTemp> _structure = new Vector<BPTemp>();
		public Vector<HelixTemp> _helices = new Vector<HelixTemp>();

		public ArrayList<String> getSequence() {
			return _sequence;
		}

		public Vector<BPTemp> getStructure() {
			return _structure;
		}
	};

	private Hashtable<String, RNATmp> _molecules = new Hashtable<String, RNATmp>();

	private boolean _inSequenceIDs, _inLength, _inSequence, _inHelix,
			_inStrAnnotation, _inBP, _inBP5, _inBP3, _inEdge5, _inEdge3,
			_inPosition, _inBondOrientation, _inMolecule;
	private StringBuffer _buffer;
	private String _currentModel = "";
	private int _id5, _id3, _length;
	String _edge5, _edge3, _orientation, _helixID;

	public RNAMLParser() {
		super();
		_inSequenceIDs = false;
		_inSequence = false;
		_inStrAnnotation = false;
		_inBP = false;
		_inBP5 = false;
		_inBP3 = false;
		_inPosition = false;
		_inEdge5 = false;
		_inEdge3 = false;
		_inBondOrientation = false;
		_inHelix = false;
		_inMolecule = false;
	}

	public InputSource createSourceFromURL(String path) {
		URL url = null;
		try {
			url = new URL(path);
			URLConnection connexion = url.openConnection();
			connexion.setUseCaches(false);
			InputStream r = connexion.getInputStream();
			InputStreamReader inr = new InputStreamReader(r);
			return new InputSource(inr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new InputSource(new StringReader(""));
	}

	public InputSource resolveEntity(String publicId, String systemId) {
		// System.out.println("[crade]");
		if (systemId.endsWith("rnaml.dtd"))
		{
			String resourceName = "/rnaml.dtd";
			URL url = ClassLoader.getSystemResource(resourceName);
			if (url!=null)
			{
				try {
					InputStream stream = url.openStream();
					if (stream != null)
					{
						return new InputSource(stream );
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return new InputSource(new StringReader(""));
	}

	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (qName.equals("numbering-table")) {
			_inSequenceIDs = true;
			_buffer = new StringBuffer();
		} else if (qName.equals("helix")) {
			_inHelix = true;
			_buffer = new StringBuffer();
			_helixID = attributes.getValue("id");
		} else if (qName.equals("seq-data")) {
			_inSequence = true;
			_buffer = new StringBuffer();
		} else if (qName.equals("length")) {
			_inLength = true;
			_buffer = new StringBuffer();
		} else if (qName.equals("str-annotation")) {
			_inStrAnnotation = true;
		} else if (qName.equals("base-pair")) {
			_inBP = true;
		} else if (qName.equals("base-id-5p")) {
			if (_inBP || _inHelix) {
				_inBP5 = true;
			}
		} else if (qName.equals("base-id-3p")) {
			if (_inBP || _inHelix) {
				_inBP3 = true;
			}
		} else if (qName.equals("edge-5p")) {
			_inEdge5 = true;
			_buffer = new StringBuffer();
		} else if (qName.equals("edge-3p")) {
			_inEdge3 = true;
			_buffer = new StringBuffer();
		} else if (qName.equals("position")) {
			_inPosition = true;
			_buffer = new StringBuffer();
		} else if (qName.equals("bond-orientation")) {
			_inBondOrientation = true;
			_buffer = new StringBuffer();
		} else if (qName.equals("molecule")) {
			_inMolecule = true;
			String id = (attributes.getValue("id"));
			// System.err.println("Molecule#"+id);
			_molecules.put(id, new RNATmp());
			_currentModel = id;
		} else {
			// We don't care too much about the rest ...
		}
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equals("numbering-table")) {
			_inSequenceIDs = false;
			String content = _buffer.toString();
			content = content.trim();
			String[] tokens = content.split("\\s+");
			Vector<Integer> results = new Vector<Integer>();
			for (int i = 0; i < tokens.length; i++) {
				try {
					results.add(new Integer(Integer.parseInt(tokens[i])));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
			_molecules.get(_currentModel)._sequenceIDs = results;
			_buffer = null;
		} else if (qName.equals("seq-data")) {
			_inSequence = false;
			String content = _buffer.toString();
			content = content.trim();
			String[] tokens = content.split("\\s+");
			ArrayList<String> results = new ArrayList<String>();
			for (int i = 0; i < tokens.length; i++) {
				for (int j = 0; j < tokens[i].length(); j++)
					results.add("" + tokens[i].charAt(j));
			}
			// System.err.println("  Seq: "+results);
			_molecules.get(_currentModel)._sequence = results;
			_buffer = null;
		} else if (qName.equals("bond-orientation")) {
			_inBondOrientation = false;
			String content = _buffer.toString();
			content = content.trim();
			_orientation = content;
			_buffer = null;
		} else if (qName.equals("str-annotation")) {
			_inStrAnnotation = false;
		} else if (qName.equals("base-pair")) {
			if (_inMolecule) {
				_inBP = false;
				BPTemp bp = new BPTemp(_id5, _id3, _edge5, _edge3, _orientation);
				_molecules.get(_currentModel)._structure.add(bp);
				// System.err.println("  "+bp);
			}
		} else if (qName.equals("helix")) {
			_inHelix = false;
			if (_inMolecule) {
				HelixTemp h = new HelixTemp(_id5, _id3, _length, _helixID);
				_molecules.get(_currentModel)._helices.add(h);
			}
		} else if (qName.equals("base-id-5p")) {
			_inBP5 = false;
		} else if (qName.equals("base-id-3p")) {
			_inBP3 = false;
		} else if (qName.equals("length")) {
			_inLength = false;
			String content = _buffer.toString();
			content = content.trim();
			_length = Integer.parseInt(content);
			_buffer = null;
		} else if (qName.equals("position")) {
			String content = _buffer.toString();
			content = content.trim();
			int pos = Integer.parseInt(content);
			if (_inBP5) {
				_id5 = pos;
			}
			if (_inBP3) {
				_id3 = pos;
			}
			_buffer = null;
		} else if (qName.equals("edge-5p")) {
			_inEdge5 = false;
			String content = _buffer.toString();
			content = content.trim();
			_edge5 = content;
			_buffer = null;
		} else if (qName.equals("edge-3p")) {
			_inEdge3 = false;
			String content = _buffer.toString();
			content = content.trim();
			_edge3 = content;
			_buffer = null;
		} else if (qName.equals("molecule")) {
			_inMolecule = false;
		} else {
			// We don't care too much about the rest ...
		}
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		String lecture = new String(ch, start, length);
		if (_buffer != null)
			_buffer.append(lecture);
	}

	public void startDocument() throws SAXException {
	}

	public void endDocument() throws SAXException {
		postProcess();
	}

	// Discarding stacking interactions...
	private void discardStacking() {
		Vector<BPTemp> result = new Vector<BPTemp>();
		for (int i = 0; i < _molecules.get(_currentModel)._structure.size(); i++) {
			BPTemp bp = _molecules.get(_currentModel)._structure.get(i);
			if (bp.orientation.equals("c") || bp.orientation.equals("t")) {
				result.add(bp);
			}
		}
		_molecules.get(_currentModel)._structure = result;
	}

	public static boolean isSelfCrossing(int[] str) {
		Stack<Point> intervals = new Stack<Point>();
		intervals.add(new Point(0, str.length - 1));
		while (!intervals.empty()) {
			Point p = intervals.pop();
			if (p.x <= p.y) {
				if (str[p.x] == -1) {
					intervals.push(new Point(p.x + 1, p.y));
				} else {
					int i = p.x;
					int j = p.y;
					int k = str[i];
					if ((k <= i) || (k > j)) {
						return true;
					} else {
						intervals.push(new Point(i + 1, k - 1));
						intervals.push(new Point(k + 1, j));
					}
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	private void debugPrintArray(Object[] str) {
		StringBuffer s = new StringBuffer("[");
		for (int i = 0; i < str.length; i++) {
			if (i != 0) {
				s.append(",");
			}
			s.append(str[i]);

		}
		s.append("]");
		System.out.println(s.toString());
	}

	/**
	 * Computes and returns a maximal planar subset of the current structure.
	 * 
	 * @param str
	 *            A sequence of base-pairing positions
	 * @return A sequence of non-crossing base-pairing positions
	 */

	public static int[] planarize(int[] str) {
		if (!isSelfCrossing(str)) {
			return str;
		}

		int length = str.length;

		int[] result = new int[length];
		for (int i = 0; i < result.length; i++) {
			result[i] = -1;
		}

		short[][] tab = new short[length][length];
		short[][] backtrack = new short[length][length];
		int theta = 3;

		for (int i = 0; i < result.length; i++) {
			for (int j = i; j < Math.min(i + theta, result.length); j++) {
				tab[i][j] = 0;
				backtrack[i][j] = -1;
			}
		}
		for (int n = theta; n < length; n++) {
			for (int i = 0; i < length - n; i++) {
				int j = i + n;
				tab[i][j] = tab[i + 1][j];
				backtrack[i][j] = -1;
				int k = str[i];
				if ((k != -1) && (k <= j) && (i < k)) {
					int tmp = 1;
					if (i + 1 <= k - 1) {
						tmp += tab[i + 1][k - 1];
					}
					if (k + 1 <= j) {
						tmp += tab[k + 1][j];
					}
					if (tmp > tab[i][j]) {
						tab[i][j] = (short) tmp;
						backtrack[i][j] = (short) k;
					}
				}
			}
		}
		Stack<Point> intervals = new Stack<Point>();
		intervals.add(new Point(0, length - 1));
		while (!intervals.empty()) {
			Point p = intervals.pop();
			if (p.x <= p.y) {
				if (backtrack[p.x][p.y] == -1) {
					result[p.x] = -1;
					intervals.push(new Point(p.x + 1, p.y));
				} else {
					int i = p.x;
					int j = p.y;
					int k = backtrack[p.x][p.y];
					result[i] = k;
					result[k] = i;
					intervals.push(new Point(i + 1, k - 1));
					intervals.push(new Point(k + 1, j));
				}
			}
		}
		return result;
	}

	public static void planarize(ArrayList<ModeleBP> input,
			ArrayList<ModeleBP> planar, ArrayList<ModeleBP> others, int length) {
		// System.err.println("Planarize: Length:"+length);
		Hashtable<Integer, ArrayList<ModeleBP>> index2BPs = new Hashtable<Integer, ArrayList<ModeleBP>>();
		for (ModeleBP msbp : input) {
			int i = msbp.getPartner5().getIndex();
			if (!index2BPs.containsKey(i)) {
				index2BPs.put(i, new ArrayList<ModeleBP>());
			}
			index2BPs.get(i).add(msbp);
		}
		// System.err.println(index2BPs);

		short[][] tab = new short[length][length];
		short[][] backtrack = new short[length][length];
		int theta = 3;

		for (int i = 0; i < length; i++) {
			for (int j = i; j < Math.min(i + theta, length); j++) {
				tab[i][j] = 0;
				backtrack[i][j] = -1;
			}
		}
		for (int n = theta; n < length; n++) {
			for (int i = 0; i < length - n; i++) {
				int j = i + n;
				tab[i][j] = tab[i + 1][j];
				backtrack[i][j] = -1;
				if (index2BPs.containsKey(i)) {
					ArrayList<ModeleBP> vi = index2BPs.get(i);
					// System.err.print(".");
					for (int numBP = 0; numBP < vi.size(); numBP++) {
						ModeleBP mb = vi.get(numBP);
						int k = mb.getPartner3().getIndex();
						if ((k != -1) && (k <= j) && (i < k)) {
							int tmp = 1;
							if (i + 1 <= k - 1) {
								tmp += tab[i + 1][k - 1];
							}
							if (k + 1 <= j) {
								tmp += tab[k + 1][j];
							}
							if (tmp > tab[i][j]) {
								tab[i][j] = (short) tmp;
								backtrack[i][j] = (short) numBP;
							}
						}
					}
				}
			}
		}
		// System.err.println("DP table: "+tab[0][length-1]);

		// Backtracking
		Stack<Point> intervals = new Stack<Point>();
		intervals.add(new Point(0, length - 1));
		while (!intervals.empty()) {
			Point p = intervals.pop();
			if (p.x <= p.y) {
				if (backtrack[p.x][p.y] == -1) {
					intervals.push(new Point(p.x + 1, p.y));
				} else {
					int i = p.x;
					int j = p.y;
					int nb = backtrack[p.x][p.y];
					ModeleBP mb = index2BPs.get(i).get(nb);
					int k = mb.getPartner3().getIndex();
					planar.add(mb);
					intervals.push(new Point(i + 1, k - 1));
					intervals.push(new Point(k + 1, j));
				}
			}
		}

		// Remaining base pairs
		for (int i : index2BPs.keySet()) {
			ArrayList<ModeleBP> vi = index2BPs.get(i);
			for (ModeleBP mb : vi) {
				if (!planar.contains(mb)) {
					others.add(mb);
				}
			}
		}
	}

	private void postProcess() {
		for (RNATmp r : _molecules.values()) {
			// First, check if base numbers were specified
			if (r._sequenceIDs.size() == 0) {
				Vector<Integer> results = new Vector<Integer>();
				for (int i = 0; i < r._sequence.size(); i++) {
					results.add(new Integer(i + 1));
				}
				r._sequenceIDs = results;
			}
			// System.err.println("IDs: "+_sequenceIDs);
			// System.err.println("Before remapping: "+_structure);

			// Then, build inverse mapping ID => index
			Hashtable<Integer, Integer> ID2Index = new Hashtable<Integer, Integer>();
			for (int i = 0; i < r._sequenceIDs.size(); i++) {
				ID2Index.put(r._sequenceIDs.get(i), i);
			}

			// Translate BP coordinates into indices
			for (BPTemp bp : r._structure) {
				bp.pos3 = bp.pos3 - 1;
				bp.pos5 = bp.pos5 - 1;
			}
			// System.err.println("After remapping: "+_structure);

			discardStacking();
			// System.err.println("  Discard stacking (length="+r._sequence.size()+") => "+r._structure);

			// Eliminate redundancy
			Hashtable<Integer, Hashtable<Integer,BPTemp>> index2BPs = new Hashtable<Integer, Hashtable<Integer,BPTemp>>();
			for (BPTemp msbp : r._structure) {
				int i = msbp.pos5;
				if (!index2BPs.containsKey(i)) {
					index2BPs.put(i, new Hashtable<Integer,BPTemp>());
				}
				if (!index2BPs.get(i).contains(msbp.pos3)) {
					index2BPs.get(i).put(msbp.pos3,msbp);
				}
			}
			
			// Adding helices...
			for (int i = 0; i < r._helices.size(); i++) {
				HelixTemp h = r._helices.get(i);
				for (int j = 0; j < h.length; j++) {
					// System.err.println("Looking for residues: "+(h.pos5+j-1)+" and "+(h.pos3-j-1));
					int a = (h.pos5 + j - 1);
					int b = (h.pos3 - j - 1);
					BPTemp bp = new BPTemp(a, (b), "+", "+", "c");
					if (!index2BPs.containsKey(a)) {
						index2BPs.put(a, new Hashtable<Integer,BPTemp>());
					}
					if (!index2BPs.get(a).contains(b)) {
						index2BPs.get(a).put(b,bp);
					}
				}
			}

			Vector<BPTemp> newStructure = new Vector<BPTemp>();
			for (int i : index2BPs.keySet()) {
				for (int j : index2BPs.get(i).keySet()) {
					BPTemp bp = index2BPs.get(i).get(j);
					newStructure.add(bp);
				}
			}
			r._structure = newStructure;

			// System.err.println("After Helices => "+_structure);


			// System.err.println("After Postprocess => "+_structure);
		}
	}

	public ArrayList<RNATmp> getMolecules() {
		return new ArrayList<RNATmp>(_molecules.values());
	}
}