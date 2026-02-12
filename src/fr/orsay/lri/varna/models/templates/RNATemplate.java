package fr.orsay.lri.varna.models.templates;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.orsay.lri.varna.exceptions.ExceptionEdgeEndpointAlreadyConnected;
import fr.orsay.lri.varna.exceptions.ExceptionFileFormatOrSyntax;
import fr.orsay.lri.varna.exceptions.ExceptionInvalidRNATemplate;
import fr.orsay.lri.varna.exceptions.ExceptionXMLGeneration;
import fr.orsay.lri.varna.exceptions.ExceptionXmlLoading;
import fr.orsay.lri.varna.models.rna.RNA;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateElement.EdgeEndPoint;
import fr.orsay.lri.varna.models.treealign.Tree;


/**
 * A model for RNA templates.
 * A template is a way to display an RNA secondary structure.
 * 
 * @author Raphael Champeimont
 */
public class RNATemplate {
	/**
	 * The list of template elements.
	 */
	private Collection<RNATemplateElement> elements = new ArrayList<RNATemplateElement>();

	/**
	 * Tells whether the template contains elements.
	 */
	public boolean isEmpty() {
		return elements.isEmpty();
	}
	
	/**
	 * The first endpoint (in sequence order) of the template.
	 * If there are multiple connected components, the first elements of one
	 * connected component will be returned.
	 * If the template contains no elements, null is returned.
	 * If there is a cycle, an arbitrary endpoint will be returned
	 * (as it then does not make sense to define the first endpoint).
	 * Time: O(n)
	 */
	public RNATemplateElement getFirst() {
		return getFirstEndPoint().getElement();
	}
	
	/**
	 * The first endpoint edge endpoint (in sequence order) of the template.
	 * If there are multiple connected components, the first elements of one
	 * connected component will be returned.
	 * If the template contains no elements, null is returned.
	 * If there is a cycle, an arbitrary endpoint will be returned
	 * (as it then does not make sense to define the first endpoint).
	 * Time: O(n)
	 */
	public EdgeEndPoint getFirstEndPoint() {
		if (elements.isEmpty()) {
			return null;
		} else {
			Set<EdgeEndPoint> knownEndPoints = new HashSet<EdgeEndPoint>();
			EdgeEndPoint currentEndPoint = getAnyEndPoint();
			while (true) {
				if (knownEndPoints.contains(currentEndPoint)) {
					// There is a cycle in the template, so we stop there
					// to avoid looping infinitely.
					return currentEndPoint;
				}
				knownEndPoints.add(currentEndPoint);
				EdgeEndPoint previousEndPoint = currentEndPoint.getPreviousEndPoint();
				if (previousEndPoint == null) {
					return currentEndPoint;
				} else {
					currentEndPoint = previousEndPoint;
				}
			}
		}
	}
	
	/**
	 * Return an arbitrary element of the template,
	 * null if empty.
	 * Time: O(1)
	 */
	public RNATemplateElement getAny() {
		if (elements.isEmpty()) {
			return null;
		} else {
			return elements.iterator().next();
		}
	}
	
	/**
	 * Return an arbitrary endpoint of the template,
	 * null if empty.
	 * Time: O(1)
	 */
	public EdgeEndPoint getAnyEndPoint() {
		if (isEmpty()) {
			return null;
		} else {
			return getAny().getIn1EndPoint();
		}
	}
	
	/**
	 * Variable containing "this", used by the internal class
	 * to access this object.
	 */
	private final RNATemplate template = this;
	

	/**
	 * To get an iterator of this class, use rnaIterator().
	 * See rnaIterator() for documentation.
	 */
	private class RNAIterator implements Iterator<RNATemplateElement> {
		private Iterator<EdgeEndPoint> iter = vertexIterator();

		public boolean hasNext() {
			return iter.hasNext();
		}

		public RNATemplateElement next() {
			if (! hasNext()) {
				throw (new NoSuchElementException());
			}
			
			EdgeEndPoint currentEndPoint = iter.next();
			switch (currentEndPoint.getPosition()) {
			// We skip "IN" endpoints, so that we don't return elements twice
			case IN1:
			case IN2:
				// We get the corresponding "OUT" endpoint
				currentEndPoint = iter.next();
				break;
			}
			
			return currentEndPoint.getElement();
		}

		public void remove() {
			throw (new UnsupportedOperationException());
		}
		
	}
	
	/**
	 * Iterates over the elements of the template, in the sequence order.
	 * Helixes will be given twice.
	 * Only one connected component will be iterated on.
	 * Note that if there is a cycle, the iterator may return a infinite
	 * number of elements.
	 */
	public Iterator<RNATemplateElement> rnaIterator() {
		return new RNAIterator();
	}
	
	/**
	 * Iterates over all elements (each endpoint is given only once)
	 * in an arbitrary order.
	 */
	public Iterator<RNATemplateElement> classicIterator() {
		return elements.iterator();
	}
	
	private class VertexIterator implements Iterator<EdgeEndPoint> {
		private EdgeEndPoint endpoint = getFirstEndPoint();

		public boolean hasNext() {
			return (endpoint != null);
		}

		public EdgeEndPoint next() {
			if (endpoint == null) {
				throw (new NoSuchElementException());
			}
			
			EdgeEndPoint currentEndPoint = endpoint;
			endpoint = currentEndPoint.getNextEndPoint();
			return currentEndPoint;
		}

		public void remove() {
			throw (new UnsupportedOperationException());
		}
	}
	
	/**
	 * Iterates over the elements edge endpoints of the template,
	 * in the sequence order.
	 * Only one connected component will be iterated on.
	 * Note that if there is a cycle, the iterator may return a infinite
	 * number of elements.
	 */
	public Iterator<EdgeEndPoint> vertexIterator() {
		return new VertexIterator();
	}
	
	
	
	private class MakeEdgeList {
		List<EdgeEndPoint> list = new LinkedList<EdgeEndPoint>();
		
		private void addEdgeIfNecessary(EdgeEndPoint endPoint) {
			if (endPoint.isConnected()) {
				list.add(endPoint);
			}
		}
		
		public List<EdgeEndPoint> make() {
			for (RNATemplateElement element: elements) {
				if (element instanceof RNATemplateHelix) {
					RNATemplateHelix helix = (RNATemplateHelix) element;
					addEdgeIfNecessary(helix.getIn1());
					addEdgeIfNecessary(helix.getIn2());
				} else if (element instanceof RNATemplateUnpairedSequence) {
					RNATemplateUnpairedSequence sequence = (RNATemplateUnpairedSequence) element;
					addEdgeIfNecessary(sequence.getIn());
				}
			}
			return list;
		}
	}
	
	/**
	 * Return over all edges in an arbitrary order.
	 * For each edge, the first (5' side) endpoint will be given. 
	 */
	public List<EdgeEndPoint> makeEdgeList() {
		MakeEdgeList listMaker = new MakeEdgeList();
		return listMaker.make();
	}
	
	
	
	

	private class RemovePseudoKnots {
		/**
		 * The elements of the template as an array, in the order of the
		 * RNA sequence. Note that helixes will appear twice,
		 * and non-paired sequences don't appear.
		 */
		private ArrayList<RNATemplateHelix> helixesSeq;
		
		/**
		 * For any i,
		 * j = helixesStruct[i] is the index in helixesSeq
		 * where the same helix also appears.
		 * It means we have for all i:
		 * helixesSeq[helixesStruct[i]] == helixesSeq[i]
		 */
		private ArrayList<Integer> helixesStruct;
		
		/**
		 * The same as helixesStruct, but without the pseudoknots,
		 * ie. helixesStructWithoutPseudoKnots[i] may be -1
		 * even though helixesStruct[i] != -1 .
		 */
		private int[] helixesStructWithoutPseudoKnots;

		
		private void initArrays() throws ExceptionInvalidRNATemplate {
			helixesSeq = new ArrayList<RNATemplateHelix>();
			helixesStruct = new ArrayList<Integer>();
			Map<RNATemplateHelix,Integer> knownHelixes = new Hashtable<RNATemplateHelix,Integer>();
			Iterator<RNATemplateElement> iter = rnaIterator();
			while (iter.hasNext()) {
				RNATemplateElement element = iter.next();
				if (element instanceof RNATemplateHelix) {
					helixesSeq.add((RNATemplateHelix) element);
					int index = helixesSeq.size() - 1;
					if (knownHelixes.containsKey(element)) {
						// This is the second time we meet this helix.
						int otherOccurenceIndex = knownHelixes.get(element);
						helixesStruct.add(otherOccurenceIndex);
						if (helixesStruct.get(otherOccurenceIndex) != -1) {
							throw (new ExceptionInvalidRNATemplate("We met an helix 3 times. Is there a cycle?"));
						}
						// Now we know the partner of the other part of
						// the helix.
						helixesStruct.set(otherOccurenceIndex, index);
					} else {
						// This is the first time we meet this helix.
						// Remember its index
						knownHelixes.put((RNATemplateHelix) element, index);
						// For the moment we don't know where the other part
						// of the helix is, but this will be found later.
						helixesStruct.add(-1);
					}
				}
			}
			
		}
		
		/**
		 * Tells whether there is a pseudoknot.
		 * Adapted from RNAMLParser.isSelfCrossing()
		 */
		private boolean isSelfCrossing() {
			Stack<Point> intervals = new Stack<Point>();
			intervals.add(new Point(0, helixesStruct.size() - 1));
			while (!intervals.empty()) {
				Point p = intervals.pop();
				if (p.x <= p.y) {
					if (helixesStruct.get(p.x) == -1) {
						intervals.push(new Point(p.x + 1, p.y));
					} else {
						int i = p.x;
						int j = p.y;
						int k = helixesStruct.get(i);
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
		
		/**
		 * We compute helixesStructWithoutPseudoKnots
		 * from helixesStruct by replacing values by -1
		 * for the helixes we cut (the bases are become non-paired).
		 * We try to cut as few base pairs as possible.
		 */
		private void removePseudoKnotsAux() {
			if (!isSelfCrossing()) {
				helixesStructWithoutPseudoKnots = new int[helixesStruct.size()];
				for (int i=0; i<helixesStructWithoutPseudoKnots.length; i++) {
					helixesStructWithoutPseudoKnots[i] = helixesStruct.get(i);
				}
			} else {
				
				// We need to get rid of pseudoknots
				// This code was adapted from RNAMLParser.planarize()
				int length = helixesStruct.size();
	
				int[] result = new int[length];
				for (int i = 0; i < result.length; i++) {
					result[i] = -1;
				}
	
				short[][] tab = new short[length][length];
				short[][] backtrack = new short[length][length];
	
				// On the diagonal we have intervals containing only
				// one endpoint. Therefore there can be no helix
				// (because an helix consists of 2 elements).
				for (int i = 0; i < result.length; i++) {
					// tab[i][j] = 0;
					backtrack[i][i] = -1;
				}
				
				for (int n = 1; n < length; n++) {
					for (int i = 0; i < length - n; i++) {
						int j = i + n;
						tab[i][j] = tab[i + 1][j];
						backtrack[i][j] = -1;
						int k = helixesStruct.get(i);
						assert k != -1;
						if ((k <= j) && (i < k)) {
							int tmp = helixesSeq.get(i).getLength();
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
				
				// debug
				//RNATemplateTests.printShortMatrix(tab);
				
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
							int k = backtrack[i][j];
							result[i] = k;
							result[k] = i;
							intervals.push(new Point(i + 1, k - 1));
							intervals.push(new Point(k + 1, j));
						}
					}
				}
				
				helixesStructWithoutPseudoKnots = result;
			}
		}
		
		private Set<RNATemplateHelix> makeSet() {
			Set<RNATemplateHelix> removedHelixes = new HashSet<RNATemplateHelix>();
			
			for (int i=0; i<helixesStructWithoutPseudoKnots.length; i++) {
				if (helixesStructWithoutPseudoKnots[i] < 0) {
					removedHelixes.add(helixesSeq.get(i));
				}
			}
			
			return removedHelixes;
		}
		
		public Set<RNATemplateHelix> removePseudoKnots() throws ExceptionInvalidRNATemplate {
			initArrays();
			
			removePseudoKnotsAux();
			// debug
			//printIntArrayList(helixesStruct);
			//printIntArray(helixesStructWithoutPseudoKnots);
			
			return makeSet();
		}
	}
	
	private class ConvertToTree {
		private Set<RNATemplateHelix> removedHelixes;
		
		public ConvertToTree(Set<RNATemplateHelix> removedHelixes) {
			this.removedHelixes = removedHelixes;
		}
		
		private Iterator<RNATemplateElement> iter = template.rnaIterator();
		private Set<RNATemplateHelix> knownHelixes = new HashSet<RNATemplateHelix>();
		
		public Tree<RNANodeValueTemplate> convert() throws ExceptionInvalidRNATemplate {
			Tree<RNANodeValueTemplate> root = new Tree<RNANodeValueTemplate>();
			 // No value, this is a fake node because we need a root.
			root.setValue(null);
			makeChildren(root);
			return root;
		}
		
		private void makeChildren(Tree<RNANodeValueTemplate> father) throws ExceptionInvalidRNATemplate {
			List<Tree<RNANodeValueTemplate>> children = father.getChildren();
			while (true) {
				try {
					RNATemplateElement element = iter.next();
					if (element instanceof RNATemplateHelix) {
						RNATemplateHelix helix = (RNATemplateHelix) element;
						if (removedHelixes.contains(helix)) {
							// Helix was removed
							
							boolean firstPartOfHelix;
							if (knownHelixes.contains(helix)) {
								firstPartOfHelix = false;
							} else {
								knownHelixes.add(helix);
								firstPartOfHelix = true;
							}
							
							int helixLength = helix.getLength();
							// Maybe we could allow helixes of length 0?
							// If we want to then this code can be changed in the future.
							if (helixLength < 1) {
								throw (new ExceptionInvalidRNATemplate("Helix length < 1"));
							}
							int firstPosition = firstPartOfHelix ? 0 : helixLength;
							int afterLastPosition = firstPartOfHelix ? helixLength : 2*helixLength;
							for (int i=firstPosition; i<afterLastPosition; i++) {
								RNANodeValueTemplateBrokenBasePair value = new RNANodeValueTemplateBrokenBasePair();
								value.setHelix(helix);
								value.setPositionInHelix(i);
								Tree<RNANodeValueTemplate> child = new Tree<RNANodeValueTemplate>();
								child.setValue(value);
								father.getChildren().add(child);
							}
							
						} else {
							// We have an non-removed helix
							
							if (knownHelixes.contains(helix)) {
								if ((! (father.getValue() instanceof RNANodeValueTemplateBasePair))
										|| ((RNANodeValueTemplateBasePair) father.getValue()).getHelix() != helix) {
									// We have already met this helix, so unless it is our father,
									// we have a pseudoknot (didn't we remove them???).
									throw (new ExceptionInvalidRNATemplate("Unexpected helix. Looks like there still are pseudoknots even after we removed them so something is wrong about the template."));
								} else {
									// As we have found the father, we have finished our work
									// with the children.
									return;
								}
							} else {
								knownHelixes.add(helix);
								
								int helixLength = helix.getLength();
								// Maybe we could allow helixes of length 0?
								// If we want to then this code can be changed in the future.
								if (helixLength < 1) {
									throw (new ExceptionInvalidRNATemplate("Helix length < 1"));
								}
								Tree<RNANodeValueTemplate> lastChild = father;
								for (int i=0; i<helixLength; i++) {
									RNANodeValueTemplateBasePair value = new RNANodeValueTemplateBasePair();
									value.setHelix(helix);
									value.setPositionInHelix(i);
									Tree<RNANodeValueTemplate> child = new Tree<RNANodeValueTemplate>();
									child.setValue(value);
									lastChild.getChildren().add(child);
									lastChild = child;
								}
								// Now we put what follows as children of lastChild
								makeChildren(lastChild);
							}
							
							
						}
					} else if (element instanceof RNATemplateUnpairedSequence) {
						RNATemplateUnpairedSequence sequence = (RNATemplateUnpairedSequence) element;
						int seqLength = sequence.getLength();
						
						// Maybe we could allow sequences of length 0?
						// If we want to then this code can be changed in the future.
						if (seqLength < 1) {
							throw (new ExceptionInvalidRNATemplate("Non-paired sequence length < 1"));
						}
						
						RNANodeValueTemplateSequence value = new RNANodeValueTemplateSequence();
						value.setSequence(sequence);
						Tree<RNANodeValueTemplate> child = new Tree<RNANodeValueTemplate>();
						child.setValue(value);
						children.add(child);
					} else {
						throw (new ExceptionInvalidRNATemplate("We have an endpoint which is neither an helix nor a sequence. What is that?"));
					}
					
				} catch (NoSuchElementException e) {
					// We are at the end of elements so if everything is ok
					// the father must be the root.
					if (father.getValue() == null) {
						return; // Work finished.
					} else {
						throw (new ExceptionInvalidRNATemplate("Unexpected end of template endpoint list."));
					}
				}
			}
		}
	
	}
	
	/**
	 * Tells whether the connected component to which endPoint belongs to
	 * is cyclic.
	 */
	public boolean connectedComponentIsCyclic(EdgeEndPoint endPoint) {
		Set<EdgeEndPoint> knownEndPoints = new HashSet<EdgeEndPoint>();
		EdgeEndPoint currentEndPoint = endPoint;
		while (true) {
			if (knownEndPoints.contains(currentEndPoint)) {
				return true;
			}
			knownEndPoints.add(currentEndPoint);
			EdgeEndPoint previousEndPoint = currentEndPoint.getPreviousEndPoint();
			if (previousEndPoint == null) {
				return false;
			} else {
				currentEndPoint = previousEndPoint;
			}
		}
	}
	
	/**
	 * Tells whether the template elements are all connected, ie. if the
	 * graph (edge endpoints being vertices) is connected. 
	 */
	public boolean isConnected() {
		if (isEmpty()) {
			return true;
		}
		
		// Count all vertices
		int n = 0;
		for (RNATemplateElement element: elements) {
			if (element instanceof RNATemplateHelix) {
				n += 4;
			} else if (element instanceof RNATemplateUnpairedSequence) {
				n += 2;
			}
		}
		
		// Now try reaching all vertices
		Set<EdgeEndPoint> knownEndPoints = new HashSet<EdgeEndPoint>();
		EdgeEndPoint currentEndPoint = getFirstEndPoint();
		while (true) {
			if (knownEndPoints.contains(currentEndPoint)) {
				// We are back to our original endpoint, so stop
				break;
			}
			knownEndPoints.add(currentEndPoint);
			EdgeEndPoint nextEndPoint = currentEndPoint.getNextEndPoint();
			if (nextEndPoint == null) {
				break;
			} else {
				currentEndPoint = nextEndPoint;
			}
		}
		
		// The graph is connected iff the number of reached vertices
		// is equal to the total number of vertices.
		return (knownEndPoints.size() == n);

	}

	/**
	 * Checks whether this template is a valid RNA template, ie.
	 * it is non-empty, it does not contain a cycle and all elements are in one
	 * connected component.
	 */
	public void checkIsValidTemplate() throws ExceptionInvalidRNATemplate {
		if (isEmpty()) {
			throw (new ExceptionInvalidRNATemplate("The template is empty."));
		}
		
		if (! isConnected()) {
			throw (new ExceptionInvalidRNATemplate("The template is a non-connected graph."));
		}
		
		// Now we know the graph is connected.
		
		if (connectedComponentIsCyclic(getAnyEndPoint())) {
			throw (new ExceptionInvalidRNATemplate("The template is cyclic."));
		}
	}
	
	/**
	 * Make a tree of the template. For this, we will remove pseudoknots,
	 * taking care to remove as few base pair links as possible.
	 * Requires the template to be valid and will check the validity
	 * (will call checkIsValidTemplate()).
	 * Calling this method will automatically call computeIn1Is().
	 */
	public Tree<RNANodeValueTemplate> toTree() throws ExceptionInvalidRNATemplate {
		// Compute the helix in1Is fields.
		// We also rely on computeIn1Is() for checking the template validity.
		computeIn1Is();
		
		// Remove pseudoknots
		RemovePseudoKnots pseudoKnotKiller = new RemovePseudoKnots();
		Set<RNATemplateHelix> removedHelixes = pseudoKnotKiller.removePseudoKnots();
		
		// Convert to tree
		ConvertToTree converter = new ConvertToTree(removedHelixes);
		return converter.convert();
	}
	
	
	/**
	 * Generate an RNA sequence that exactly matches the template.
	 * Requires the template to be valid and will check the validity
	 * (will call checkIsValidTemplate()).
	 */
	public RNA toRNA() throws ExceptionInvalidRNATemplate {
		// First, we check this is a valid template
		checkIsValidTemplate();
		
		ArrayList<Integer> str = new ArrayList<Integer>();
		Map<RNATemplateHelix, ArrayList<Integer>> helixes = new HashMap<RNATemplateHelix, ArrayList<Integer>>();
		Iterator<RNATemplateElement> iter = rnaIterator();
		while (iter.hasNext()) {
			RNATemplateElement element = iter.next();
			if (element instanceof RNATemplateHelix) {
				RNATemplateHelix helix = (RNATemplateHelix) element;
				int n = helix.getLength();
				if (helixes.containsKey(helix)) {
					int firstBase = str.size();
					ArrayList<Integer> helixMembers = helixes.get(helix);
					for (int i = 0; i < n; i++) {
						int indexOfAssociatedBase = helixMembers.get(n-1-i);
						str.set(indexOfAssociatedBase, firstBase + i);
						str.add(indexOfAssociatedBase);
					}
				} else {
					int firstBase = str.size();
					ArrayList<Integer> helixMembers = new ArrayList<Integer>();
					for (int i = 0; i < n; i++) {
						// We don't known yet where the associated base is
						str.add(-1);
						helixMembers.add(firstBase + i);
					}
					helixes.put(helix, helixMembers);
				}
			} else if (element instanceof RNATemplateUnpairedSequence) {
				RNATemplateUnpairedSequence sequence = (RNATemplateUnpairedSequence) element;
				int n = sequence.getLength();
				for (int i=0; i<n; i++) {
					str.add(-1);
				}
			} else {
				throw (new ExceptionInvalidRNATemplate("We have an endpoint which is neither an helix nor a sequence. What is that?"));
			}
		}
		
		int[] strAsArray = RNATemplateAlign.intArrayFromList(str);
		String[] seqAsArray = new String[strAsArray.length];
		Arrays.fill(seqAsArray, " ");
		RNA rna = new RNA();
		try {
			rna.setRNA(seqAsArray, strAsArray);
		} catch (ExceptionFileFormatOrSyntax e) {
			throw (new RuntimeException("Bug in toRNA(): setRNA() threw an ExceptionFileFormatOrSyntax exception."));
		}
		return rna;
	}
	
	
	private class ConvertToXml {
		private Map<RNATemplateElement, String> elementNames = new HashMap<RNATemplateElement, String>();
		private Element connectionsXmlElement;
		private Document document;
		
		private void addConnectionIfNecessary(EdgeEndPoint endPoint) {
			if (endPoint != null && endPoint.isConnected()) {
				RNATemplateElement e1 = endPoint.getElement();
				EdgeEndPointPosition p1 = endPoint.getPosition();
				RNATemplateElement e2 = endPoint.getOtherElement();
				EdgeEndPointPosition p2 = endPoint.getOtherEndPoint().getPosition();
				Element xmlElement = document.createElement("edge");
				{
					Element fromXmlElement = document.createElement("from");
					fromXmlElement.setAttribute("endpoint", elementNames.get(e1));
					fromXmlElement.setAttribute("position", p1.toString());
					xmlElement.appendChild(fromXmlElement);
				}
				{
					Element toXmlElement = document.createElement("to");
					toXmlElement.setAttribute("endpoint", elementNames.get(e2));
					toXmlElement.setAttribute("position", p2.toString());
					xmlElement.appendChild(toXmlElement);
				}
				connectionsXmlElement.appendChild(xmlElement);
			}
		}
		
		public Document toXMLDocument() throws ExceptionXMLGeneration, ExceptionInvalidRNATemplate {
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				document = builder.newDocument();
				Element root = document.createElement("RNATemplate");
				document.appendChild(root);
				Element elementsXmlElement = document.createElement("elements");
				root.appendChild(elementsXmlElement);
				connectionsXmlElement = document.createElement("edges");
				root.appendChild(connectionsXmlElement);
				
				// First pass, we create a mapping between java references and names (strings)
				{
					int nextHelix = 1;
					int nextNonPairedSequence = 1;
					for (RNATemplateElement templateElement: elements) {
						if (templateElement instanceof RNATemplateHelix) {
							RNATemplateHelix helix = (RNATemplateHelix) templateElement;
							if (! elementNames.containsKey(helix)) {
								elementNames.put(helix, "H ID " + nextHelix);
								nextHelix++;
							}
						} else if (templateElement instanceof RNATemplateUnpairedSequence) {
							RNATemplateUnpairedSequence sequence = (RNATemplateUnpairedSequence) templateElement;
							if (! elementNames.containsKey(sequence)) {
								elementNames.put(sequence, "S ID " + nextNonPairedSequence);
								nextNonPairedSequence++;
							}
						} else {
							throw (new ExceptionInvalidRNATemplate("We have an endpoint which is neither an helix nor a sequence. What is that?"));
						}
					}
				}
				
				// Now we generate the XML document
				for (RNATemplateElement templateElement: elements) {
					String elementXmlName = elementNames.get(templateElement);
					Element xmlElement;
					if (templateElement instanceof RNATemplateHelix) {
						RNATemplateHelix helix = (RNATemplateHelix) templateElement;
						xmlElement = document.createElement("helix");
						xmlElement.setAttribute("id", elementXmlName);
						xmlElement.setAttribute("length", Integer.toString(helix.getLength()));
						xmlElement.setAttribute("flipped", Boolean.toString(helix.isFlipped()));
						if (helix.hasCaption()) {
							xmlElement.setAttribute("caption", helix.getCaption());
						}
						{
							Element startPositionXmlElement = document.createElement("startPosition");
							startPositionXmlElement.setAttribute("x", Double.toString(helix.getStartPosition().x));
							startPositionXmlElement.setAttribute("y", Double.toString(helix.getStartPosition().y));
							xmlElement.appendChild(startPositionXmlElement);
						}
						{
							Element endPositionXmlElement = document.createElement("endPosition");
							endPositionXmlElement.setAttribute("x", Double.toString(helix.getEndPosition().x));
							endPositionXmlElement.setAttribute("y", Double.toString(helix.getEndPosition().y));
							xmlElement.appendChild(endPositionXmlElement);
						}
						addConnectionIfNecessary(helix.getOut1());
						addConnectionIfNecessary(helix.getOut2());
					} else if (templateElement instanceof RNATemplateUnpairedSequence) {
						RNATemplateUnpairedSequence sequence = (RNATemplateUnpairedSequence) templateElement;
						xmlElement = document.createElement("sequence");
						xmlElement.setAttribute("id", elementXmlName);
						xmlElement.setAttribute("length", Integer.toString(sequence.getLength()));
						{
							Element vertex5XmlElement = document.createElement("vertex5");
							vertex5XmlElement.setAttribute("x", Double.toString(sequence.getVertex5().x));
							vertex5XmlElement.setAttribute("y", Double.toString(sequence.getVertex5().y));
							xmlElement.appendChild(vertex5XmlElement);
						}
						{
							Element vertex3XmlElement = document.createElement("vertex3");
							vertex3XmlElement.setAttribute("x", Double.toString(sequence.getVertex3().x));
							vertex3XmlElement.setAttribute("y", Double.toString(sequence.getVertex3().y));
							xmlElement.appendChild(vertex3XmlElement);
						}
						{
							Element inTangentVectorXmlElement = document.createElement("inTangentVector");
							inTangentVectorXmlElement.setAttribute("angle", Double.toString(sequence.getInTangentVectorAngle()));
							inTangentVectorXmlElement.setAttribute("length", Double.toString(sequence.getInTangentVectorLength()));
							xmlElement.appendChild(inTangentVectorXmlElement);
						}
						{
							Element outTangentVectorXmlElement = document.createElement("outTangentVector");
							outTangentVectorXmlElement.setAttribute("angle", Double.toString(sequence.getOutTangentVectorAngle()));
							outTangentVectorXmlElement.setAttribute("length", Double.toString(sequence.getOutTangentVectorLength()));
							xmlElement.appendChild(outTangentVectorXmlElement);
						}
						addConnectionIfNecessary(sequence.getOut());
					} else {
						throw (new ExceptionInvalidRNATemplate("We have an endpoint which is neither an helix nor a sequence. What is that?"));
					}
					elementsXmlElement.appendChild(xmlElement);
				}
				
				return document;
			} catch (ParserConfigurationException e) {
				throw (new ExceptionXMLGeneration("ParserConfigurationException: " + e.getMessage()));
			}
		}
	}
	
	
	
	public void toXMLFile(File file) throws ExceptionXMLGeneration, ExceptionInvalidRNATemplate {
		try {
			Document xmlDocument = toXMLDocument();
			Source source = new DOMSource(xmlDocument);
			Result result = new StreamResult(file);
			Transformer transformer;
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(source, result); 
		} catch (TransformerConfigurationException e) {
			throw (new ExceptionXMLGeneration("TransformerConfigurationException: " + e.getMessage()));
		} catch (TransformerFactoryConfigurationError e) {
			throw (new ExceptionXMLGeneration("TransformerFactoryConfigurationError: " + e.getMessage()));
		} catch (TransformerException e) {
			throw (new ExceptionXMLGeneration("TransformerException: " + e.getMessage()));
		}
	}

	
	public Document toXMLDocument() throws ExceptionXMLGeneration, ExceptionInvalidRNATemplate {
		ConvertToXml converter = new ConvertToXml();
		return converter.toXMLDocument();
	}
	
	
	private class LoadFromXml {
		private Document xmlDocument;
		private Map<String, RNATemplateElement> elementNames = new HashMap<String, RNATemplateElement>();
		
		public LoadFromXml(Document xmlDocument) {
			this.xmlDocument = xmlDocument;
		}
		
		private Point2D.Double pointFromXml(Element xmlPoint) {
			Point2D.Double point = new Point2D.Double();
			point.x = Double.parseDouble(xmlPoint.getAttribute("x"));
			point.y = Double.parseDouble(xmlPoint.getAttribute("y"));
			return point;
		}
		
		private double vectorLengthFromXml(Element xmlVector) {
			return Double.parseDouble(xmlVector.getAttribute("length"));
		}
		
		private double vectorAngleFromXml(Element xmlVector) {
			return Double.parseDouble(xmlVector.getAttribute("angle"));
		}
		
		/**
		 * Takes an element of the form:
		 * <anything endpoint="an element id" position="a valid EdgeEndPointPosition"/>
		 * and returns the corresponding EdgeEndPoint object. 
		 */
		private EdgeEndPoint endPointFromXml(Element xmlEdgeEndPoint) throws ExceptionXmlLoading {
			String elementId = xmlEdgeEndPoint.getAttribute("endpoint");
			if (elementId == null || elementId == "")
				throw (new ExceptionXmlLoading ("Missing endpoint attribute on " + xmlEdgeEndPoint));
			String positionOnElement = xmlEdgeEndPoint.getAttribute("position");
			if (positionOnElement == null || positionOnElement == "")
				throw (new ExceptionXmlLoading ("Missing position attribute on " + xmlEdgeEndPoint));
			if (elementNames.containsKey(elementId)) {
				RNATemplateElement templateElement = elementNames.get(elementId);
				EdgeEndPointPosition relativePosition = EdgeEndPointPosition.valueOf(positionOnElement);
				if (relativePosition == null)
					throw (new ExceptionXmlLoading ("Could not compute relativePosition"));
				return templateElement.getEndPointFromPosition(relativePosition);
			} else {
				throw (new ExceptionXmlLoading("Edge is connected on unkown element: " + elementId));
			}
		}
		
		private String connectErrMsg(EdgeEndPoint v1, EdgeEndPoint v2, String reason) {
			return "Error while connecting\n"
			+ v1.toString() + " to\n"
			+ v2.toString() + " because:\n"
			+ reason;
		}
		
		private void connect(EdgeEndPoint v1, EdgeEndPoint v2) throws ExceptionXmlLoading {
			if (v1 == null || v2 == null) {
				throw (new ExceptionXmlLoading("Invalid edge: missing endpoint\n v1 = " + v1 + "\n v2 = " + v2));
			}
			if (v2.isConnected()) {
				throw (new ExceptionXmlLoading(connectErrMsg(v1, v2, "Second vertex is already connected to " + v2.getOtherElement().toString())));
			}
			if (v1.isConnected()) {
				throw (new ExceptionXmlLoading(connectErrMsg(v1, v2, "First vertex is already connected to " + v1.getOtherElement().toString())));
			}
			
			try {
				v1.connectTo(v2);
			} catch (ExceptionEdgeEndpointAlreadyConnected e) {
				throw (new ExceptionXmlLoading("A vertex is on two edges at the same time: " + e.getMessage()));
			} catch (ExceptionInvalidRNATemplate e) {
				throw (new ExceptionXmlLoading("ExceptionInvalidRNATemplate: " + e.getMessage()));
			}
		}
		
		public void load() throws ExceptionXmlLoading {
			// First part: we load all elements from the XML tree
			Element xmlElements = (Element) xmlDocument.getElementsByTagName("elements").item(0);
			{
				NodeList xmlElementsChildren = xmlElements.getChildNodes();
				for (int i=0; i<xmlElementsChildren.getLength(); i++) {
					Node xmlElementsChild = xmlElementsChildren.item(i);
					if (xmlElementsChild instanceof Element) {
						Element xmlTemplateElement = (Element) xmlElementsChild;
						String tagName = xmlTemplateElement.getTagName();
						if (tagName == "helix") {
							RNATemplateHelix helix = new RNATemplateHelix(xmlTemplateElement.getAttribute("id"));
							helix.setFlipped(Boolean.parseBoolean(xmlTemplateElement.getAttribute("flipped")));
							helix.setLength(Integer.parseInt(xmlTemplateElement.getAttribute("length")));
							if (xmlTemplateElement.hasAttribute("caption")) {
								helix.setCaption(xmlTemplateElement.getAttribute("caption"));
							}
							elementNames.put(xmlTemplateElement.getAttribute("id"), helix);
							NodeList xmlHelixChildren = xmlTemplateElement.getChildNodes();
							for (int j=0; j<xmlHelixChildren.getLength(); j++) {
								Node xmlHelixChild = xmlHelixChildren.item(j);
								if (xmlHelixChild instanceof Element) {
									Element xmlHelixChildElement = (Element) xmlHelixChild;
									String helixChildTagName = xmlHelixChildElement.getTagName();
									if (helixChildTagName == "startPosition") {
										helix.setStartPosition(pointFromXml(xmlHelixChildElement));
									} else if (helixChildTagName == "endPosition") {
										helix.setEndPosition(pointFromXml(xmlHelixChildElement));
									}
								}
							}
						} else if (tagName == "sequence") {
							RNATemplateUnpairedSequence sequence = new RNATemplateUnpairedSequence(xmlTemplateElement.getAttribute("id"));
							sequence.setLength(Integer.parseInt(xmlTemplateElement.getAttribute("length")));
							elementNames.put(xmlTemplateElement.getAttribute("id"), sequence);
							NodeList xmlSequenceChildren = xmlTemplateElement.getChildNodes();
							for (int j=0; j<xmlSequenceChildren.getLength(); j++) {
								Node xmlSequenceChild = xmlSequenceChildren.item(j);
								if (xmlSequenceChild instanceof Element) {
									Element xmlSequenceChildElement = (Element) xmlSequenceChild;
									String sequenceChildTagName = xmlSequenceChildElement.getTagName();
									if (sequenceChildTagName == "inTangentVector") {
										sequence.setInTangentVectorLength(vectorLengthFromXml(xmlSequenceChildElement));
										sequence.setInTangentVectorAngle(vectorAngleFromXml(xmlSequenceChildElement));
									} else if (sequenceChildTagName == "outTangentVector") {
										sequence.setOutTangentVectorLength(vectorLengthFromXml(xmlSequenceChildElement));
										sequence.setOutTangentVectorAngle(vectorAngleFromXml(xmlSequenceChildElement));
									} else if (sequenceChildTagName == "vertex5") {
										sequence.setVertex5(pointFromXml(xmlSequenceChildElement));
									} else if (sequenceChildTagName == "vertex3") {
										sequence.setVertex3(pointFromXml(xmlSequenceChildElement));
									}
								}
							}
						}
					}
				}
			}
			
			// Second part: We read the edges from the XML tree
			Element xmlEdges = (Element) xmlDocument.getElementsByTagName("edges").item(0);
			{
				NodeList xmlEdgesChildren = xmlEdges.getChildNodes();
				for (int i=0; i<xmlEdgesChildren.getLength(); i++) {
					Node xmlEdgesChild = xmlEdgesChildren.item(i);
					if (xmlEdgesChild instanceof Element) {
						Element xmlTemplateEdge = (Element) xmlEdgesChild;
						if (xmlTemplateEdge.getTagName() == "edge") {
							EdgeEndPoint v1 = null, v2 = null;
							NodeList xmlEdgeChildren = xmlTemplateEdge.getChildNodes();
							for (int j=0; j<xmlEdgeChildren.getLength(); j++) {
								Node xmlEdgeChild = xmlEdgeChildren.item(j);
								if (xmlEdgeChild instanceof Element) {
									Element xmlEdgeChildElement = (Element) xmlEdgeChild;
									String edgeChildTagName = xmlEdgeChildElement.getTagName();
									if (edgeChildTagName == "from") {
										v1 = endPointFromXml(xmlEdgeChildElement);
									} else if (edgeChildTagName == "to") {
										v2 = endPointFromXml(xmlEdgeChildElement);
									}
								}
							}
							if (v1 == null)
								throw (new ExceptionXmlLoading("Invalid edge: missing \"from\" declaration"));
							if (v2 == null)
								throw (new ExceptionXmlLoading("Invalid edge: missing \"to\" declaration"));
							connect(v1, v2);
						}
					}
				}
			
			}
		}
	}
	
	
	public static RNATemplate fromXMLFile(File file) throws ExceptionXmlLoading {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document xmlDocument = builder.parse(file);
			return fromXMLDocument(xmlDocument);
		} catch (ParserConfigurationException e) {
			throw (new ExceptionXmlLoading("ParserConfigurationException: " + e.getMessage()));
		} catch (SAXException e) {
			throw (new ExceptionXmlLoading("SAXException: " + e.getMessage()));
		} catch (IOException e) {
			throw (new ExceptionXmlLoading("IOException: " + e.getMessage()));
		}
	}
	
	
	public static RNATemplate fromXMLDocument(Document xmlDocument) throws ExceptionXmlLoading {
		RNATemplate template = new RNATemplate();
		LoadFromXml loader = template.new LoadFromXml(xmlDocument);
		loader.load();
		return template;
	}
	
	
	
	/**
	 * For an helix, tells us whether IN1/OUT1 is the 5' strand
	 * (the first strand we meet if we follow the RNA sequence)
	 * or the 3' strand (the second we meet if we follow the RNA sequence).
	 */
	public enum In1Is {
		IN1_IS_5PRIME, IN1_IS_3PRIME
	}
	
	
	/**
	 * For each helix, compute the in1Is field.
	 * If helices connections are changed, the value may become obsolete,
	 * so you need to call this method again before accessing the in1Is
	 * fields if you have modified connections in the template.
	 * Requires the template to be valid and will check the validity
	 * (will call checkIsValidTemplate()).
	 */
	public void computeIn1Is() throws ExceptionInvalidRNATemplate {
		checkIsValidTemplate();
		
		Iterator<EdgeEndPoint> iter = vertexIterator();
		Set<RNATemplateHelix> knownHelices = new HashSet<RNATemplateHelix>();
		while (iter.hasNext()) {
			EdgeEndPoint endPoint = iter.next();
			RNATemplateElement templateElement = endPoint.getElement();
			if (templateElement instanceof RNATemplateHelix) {
				RNATemplateHelix helix = (RNATemplateHelix) templateElement;
				if (! knownHelices.contains(helix)) {
					// first time we meet this helix
					switch (endPoint.getPosition()) {
					case IN1:
					case OUT1:
						helix.setIn1Is(In1Is.IN1_IS_5PRIME);
						break;
					case IN2:
					case OUT2:
						helix.setIn1Is(In1Is.IN1_IS_3PRIME);
						break;
					}
					knownHelices.add(helix);
				}
			}
		}
	}
	
	
	
	/**
	 * Remove the element from the template.
	 * The element is automatically disconnected from any other element.
	 * Returns true if and only if the element was present in the template,
	 * otherwise nothing was done.
	 */
	public boolean removeElement(RNATemplateElement element) throws ExceptionInvalidRNATemplate {
		if (elements.contains(element)) {
			element.disconnectFromAny();
			elements.remove(element);
			return true;
		} else {
			return false;
		}
	}
	
	
	/**
	 * Position of an endpoint on an endpoint.
	 * Not all values make sense for any endpoint.
	 * For an helix, all four make sense, but for a non-paired
	 * sequence, only IN1 and OUT1 make sense.
	 */
	public enum EdgeEndPointPosition {
		IN1, IN2, OUT1, OUT2;
	}
	
	
	private static int NEXT_ID = 1;
	
	/**
	 * An endpoint of an RNA template,
	 * it can be an helix or a sequence of non-paired bases.
	 * 
	 * You cannot create an object of this class directly,
	 * use RNATemplateHelix or RNATemplateUnpairedSequence instead.
	 * 
	 * @author Raphael Champeimont
	 */
	public abstract class RNATemplateElement {
		
		public int _id = NEXT_ID++;
		
		public String getName()
		{return "RNATemplate"+_id; }
		
		
		/**
		 * This variable is just there so that "this" can be accessed by a name
		 * from the internal class EdgeEndPoint.
		 */
		private final RNATemplateElement element = this;
		
		/**
		 * When the endpoint is created, it is added to the list of elements
		 * in this template. To remove it, call RNATemplate.removeElement().
		 */
		public RNATemplateElement() {
			elements.add(this);
		}
		
		/**
		 * Disconnect this endpoint from any other elements it may be connected to.
		 */
		public abstract void disconnectFromAny();
		
		/**
		 * Get the the IN endpoint in the case of a sequence
		 * and the IN1 endpoint in the case of an helix.
		 */
		public abstract EdgeEndPoint getIn1EndPoint();

		/**
		 * Returns the template to which this endpoint belongs.
		 */
		public RNATemplate getParentTemplate() {
			return template;
		}
		
		/**
		 * Provided endpoint is an endpoint of this endpoint, get the next
		 * endpoint, either on this same endpoint, or or the connected endpoint.
		 * Note that you should use the getNextEndPoint() method of the endpoint
		 * itself directly.
		 */
		protected abstract EdgeEndPoint getNextEndPoint(EdgeEndPoint endpoint);
		
		/**
		 * Provided endpoint is an endpoint of this endpoint, get the previous
		 * endpoint, either on this same endpoint, or or the connected endpoint.
		 * Note that you should use the getPreviousEndPoint() method of the endpoint
		 * itself directly.
		 */
		protected abstract EdgeEndPoint getPreviousEndPoint(EdgeEndPoint endpoint);

		
		/**
		 * An edge endpoint is where an edge can connect.
		 */
		public class EdgeEndPoint {
			private EdgeEndPoint() {
			}
			
			/**
			 * Get the next endpoint. If this endpoint is an "in" endpoint,
			 * returns the corresponding "out" endpoint. If this endpoint
			 * is an "out" endpoint, return the connected endpoint if there is
			 * one, otherwise return null.
			 */
			public EdgeEndPoint getNextEndPoint() {
				return element.getNextEndPoint(this);
			}
			
			
			/**
			 * Same as getNextEndPoint(), but with the previous endpoint.
			 */
			public EdgeEndPoint getPreviousEndPoint() {
				return element.getPreviousEndPoint(this);
			}
			
			
			/**
			 * Get the position on the endpoint where this endpoint is.
			 */
			public EdgeEndPointPosition getPosition() {
				return element.getPositionFromEndPoint(this);
			}
			
			
			private EdgeEndPoint otherEndPoint;
			
			/**
			 * Returns the endpoint on which this edge endpoint is.
			 */
			public RNATemplateElement getElement() {
				return element;
			}
			
			/**
			 * Returns the other endpoint of the edge.
			 * Will be null if there is no edge connecter to this endpoint.
			 */
			public EdgeEndPoint getOtherEndPoint() {
				return otherEndPoint;
			}
			/**
			 * Returns the endpoint at the other endpoint of the edge.
			 * Will be null if there is no edge connecter to this endpoint.
			 */
			public RNATemplateElement getOtherElement() {
				return (otherEndPoint != null) ? otherEndPoint.getElement() : null;
			}
			
			/**
			 * Disconnect this endpoint from the other, ie. delete the edge
			 * between them. Note that this will modify both endpoints, and that 
			 * x.disconnect() is equivalent to x.getOtherEndPoint().disconnect().
			 * If this endpoint is not connected, does nothing.
			 */
			public void disconnect() {
				if (otherEndPoint != null) {
					otherEndPoint.otherEndPoint = null;
					otherEndPoint = null;
				}
			}
			
			/**
			 * Tells whether this endpoint is connected with an edge to
			 * an other endpoint.
			 */
			public boolean isConnected() {
				return (otherEndPoint != null);
			}


			/**
			 * Create an edge between two edge endpoints.
			 * This is a symmetric operation and it will modify both endpoints.
			 * It means x.connectTo(y) is equivalent to y.connectTo(x).
			 * The edge endpoint must be free (ie. not yet connected).
			 * Also, elements connected together must belong to the same template.
			 */
			public void connectTo(EdgeEndPoint otherEndPoint) throws ExceptionEdgeEndpointAlreadyConnected, ExceptionInvalidRNATemplate {
				if (this.otherEndPoint != null || otherEndPoint.otherEndPoint != null) {
					throw (new ExceptionEdgeEndpointAlreadyConnected());
				}
				if (template != otherEndPoint.getElement().getParentTemplate()) {
					throw (new ExceptionInvalidRNATemplate("Elements from different templates cannot be connected with each other."));
				}
				this.otherEndPoint = otherEndPoint;
				otherEndPoint.otherEndPoint = this;
			}
			

			public String toString() {
				return "Edge endpoint on element " + element.toString() + " at position " + getPosition().toString();
			}
		}
		
		
		/**
		 * Get the EdgeEndPoint object corresponding to the the given
		 * position on this endpoint.
		 */
		public abstract EdgeEndPoint getEndPointFromPosition(EdgeEndPointPosition position);
		
		
		/**
		 * The inverse of getEndPointFromPosition.
		 */
		public abstract EdgeEndPointPosition getPositionFromEndPoint(EdgeEndPoint endPoint);
		
		
		/**
		 * Connect the endpoint at position positionHere of this endpoint
		 * to the otherEndPoint.
		 */
		public void connectTo(
				EdgeEndPointPosition positionHere,
				EdgeEndPoint otherEndPoint)
		throws ExceptionEdgeEndpointAlreadyConnected, ExceptionInvalidRNATemplate {
			EdgeEndPoint endPointHere = getEndPointFromPosition(positionHere);
			endPointHere.connectTo(otherEndPoint);
		}
		
		/**
		 * Connect the endpoint at position positionHere of this endpoint
		 * to the endpoint of otherElement at position positionOnOtherElement.
		 * @throws ExceptionInvalidRNATemplate 
		 * @throws ExceptionEdgeEndpointAlreadyConnected, ExceptionEdgeEndpointAlreadyConnected 
		 */
		public void connectTo(
				EdgeEndPointPosition positionHere,
				RNATemplateElement otherElement,
				EdgeEndPointPosition positionOnOtherElement)
		throws ExceptionEdgeEndpointAlreadyConnected, ExceptionEdgeEndpointAlreadyConnected, ExceptionInvalidRNATemplate {
			EdgeEndPoint otherEndPoint = otherElement.getEndPointFromPosition(positionOnOtherElement);
			connectTo(positionHere, otherEndPoint);
		}
	
	
	}
	

	/**
	 * An helix in an RNA template.
	 * 
	 * @author Raphael Champeimont
	 */
	public class RNATemplateHelix extends RNATemplateElement {
		/**
		 * Number of base pairs in the helix.
		 */
		private int length;
		
		/**
		 * Position of the helix start point,
		 * ie. the middle in the line [x,y] where (x,y)
		 * x is the base at the IN1 edge endpoint and
		 * y is the base at the OUT2 edge endpoint.
		 */
		private Point2D.Double startPosition;
		
		/**
		 * Position of the helix end point,
		 * ie. the middle in the line [x,y] where (x,y)
		 * x is the base at the OUT1 edge endpoint and
		 * y is the base at the IN2 edge endpoint.
		 */
		private Point2D.Double endPosition;
		
		
		/**
		 * Tells whether the helix is flipped.
		 */
		private boolean flipped = false;
		
		public boolean isFlipped() {
			return flipped;
		}

		public void setFlipped(boolean flipped) {
			this.flipped = flipped;
		}
		
		/**
		 * For an helix, tells us whether IN1/OUT1 is the 5' strand
		 * (the first strand we meet if we follow the RNA sequence)
		 * or the 3' strand (the second we meet if we follow the RNA sequence).
		 * This information cannot be known locally, we need the complete
		 * template to compute it, see RNATemplate.computeIn1Is().
		 */
		private In1Is in1Is = null;
		
		public In1Is getIn1Is() {
			return in1Is;
		}

		public void setIn1Is(In1Is in1Is) {
			this.in1Is = in1Is;
		}
		
		
		
		/**
		 * A string displayed on the helix.
		 */
		private String caption = null;
		
		public String getCaption() {
			return caption;
		}

		public void setCaption(String caption) {
			this.caption = caption;
		}
		
		public boolean hasCaption() {
			return caption != null;
		}

		
		
		
		/**
		 * If we go through all bases of the RNA from first to last,
		 * we will pass twice through this helix. On time, we arrive
		 * from in1, and leave by out2, and the other time we arrive from
		 * in2 and leave by out2.
		 * Whether we go through in1/out1 or in2/out2 the first time
		 * is written in the in1Is field.
		 */
		private final EdgeEndPoint in1, out1, in2, out2;
		private String _name;
		
		public RNATemplateHelix(String name) {
			in1 = new EdgeEndPoint();
			out1 = new EdgeEndPoint();
			in2 = new EdgeEndPoint();
			out2 = new EdgeEndPoint();
			_name = name;
		}
		
		

		
		
		public String toString() {
			return "Helix    @" + Integer.toHexString(hashCode()) + " len=" + length + " caption=" + caption;
		}
		
		public String getName()
		{return ""+_name; }
		
		

		public int getLength() {
			return length;
		}

		public void setLength(int length) {
			this.length = length;
		}

		public Point2D.Double getStartPosition() {
			return startPosition;
		}

		public void setStartPosition(Point2D.Double startPosition) {
			this.startPosition = startPosition;
		}

		public Point2D.Double getEndPosition() {
			return endPosition;
		}

		public void setEndPosition(Point2D.Double endPosition) {
			this.endPosition = endPosition;
		}

		public EdgeEndPoint getIn1() {
			return in1;
		}

		public EdgeEndPoint getOut1() {
			return out1;
		}

		public EdgeEndPoint getIn2() {
			return in2;
		}

		public EdgeEndPoint getOut2() {
			return out2;
		}

		public void disconnectFromAny() {
			getIn1().disconnect();
			getIn2().disconnect();
			getOut1().disconnect();
			getOut2().disconnect();
		}


		protected EdgeEndPoint getNextEndPoint(EdgeEndPoint endpoint) {
			if (endpoint == in1) {
				return out1;
			} else if (endpoint == in2) {
				return out2;
			} else {
				return endpoint.getOtherEndPoint();
			}
		}


		protected EdgeEndPoint getPreviousEndPoint(EdgeEndPoint endpoint) {
			if (endpoint == out1) {
				return in1;
			} else if (endpoint == out2) {
				return in2;
			} else {
				return endpoint.getOtherEndPoint();
			}
		}


		public EdgeEndPoint getIn1EndPoint() {
			return in1;
		}

		public EdgeEndPoint getEndPointFromPosition(
				EdgeEndPointPosition position) {
			switch (position) {
			case IN1:
				return getIn1();
			case IN2:
				return getIn2();
			case OUT1:
				return getOut1();
			case OUT2:
				return getOut2();
			default:
				return null;
			}
		}

		public EdgeEndPointPosition getPositionFromEndPoint(
				EdgeEndPoint endPoint) {
			if (endPoint == in1) {
				return EdgeEndPointPosition.IN1;
			} else if (endPoint == in2) {
				return EdgeEndPointPosition.IN2;
			} else if (endPoint == out1) {
				return EdgeEndPointPosition.OUT1;
			} else if (endPoint == out2) {
				return EdgeEndPointPosition.OUT2;
			} else {
				return null;
			}
		}


	}



	/**
	 * A sequence of non-paired bases in an RNA template.
	 * 
	 * @author Raphael Champeimont
	 */
	public class RNATemplateUnpairedSequence extends RNATemplateElement {
		/**
		 * Number of (non-paired) bases. 
		 */
		private int length;
		
		private static final double defaultTangentVectorAngle  = Math.PI / 2;
		private static final double defaultTangentVectorLength = 100;
		
		/**
		 * The sequence is drawn along a cubic Bezier curve.
		 * The curve can be defined by 2 vectors, one for the start of the line
		 * and the other for the end. They are the tangents to the line at
		 * the beginning and the end of the line.
		 * Each vector can be defined by its length and its absolute angle.
		 * The angles are given in radians.
		 */
		private double inTangentVectorAngle   = defaultTangentVectorAngle,
		               inTangentVectorLength  = defaultTangentVectorLength,
		               outTangentVectorAngle  = defaultTangentVectorAngle,
		               outTangentVectorLength = defaultTangentVectorLength;
		
		
		
		
		/**
		 * Position of the begginning (at the "in" endpoint) of the line.
		 * It is only useful when the sequence is not yet connected to an helix.
		 * (Otherwise we can deduce it from this helix position).
		 */
		private Point2D.Double vertex5;
		
		/**
		 * Position of the end (at the "out" endpoint) of the line.
		 * It is only useful when the sequence is not yet connected to an helix.
		 * (Otherwise we can deduce it from this helix position).
		 */
		private Point2D.Double vertex3;
		
		
		public Point2D.Double getVertex5() {
			return vertex5;
		}

		public void setVertex5(Point2D.Double vertex5) {
			this.vertex5 = vertex5;
		}

		public Point2D.Double getVertex3() {
			return vertex3;
		}

		public void setVertex3(Point2D.Double vertex3) {
			this.vertex3 = vertex3;
		}


		
		
		/**
		 * The helixes connected on both sides.
		 * They must be helixes because only helixes have absolute positions,
		 * and the positions of the starting and ending points of the sequence
		 * are those stored in the helixes.
		 */
		private final EdgeEndPoint in, out;
		
		private String _name;
		
		public RNATemplateUnpairedSequence(String name) {
			in = new EdgeEndPoint();
			out = new EdgeEndPoint();
			_name = name;
		}

		
		public String toString() {
			return "Sequence @" + Integer.toHexString(hashCode()) + " len=" + length;
		}
		
		public String getName()
		{return ""+_name; }
		
		
		
		public int getLength() {
			return length;
		}

		public void setLength(int length) {
			this.length = length;
		}

		public double getInTangentVectorAngle() {
			return inTangentVectorAngle;
		}

		public void setInTangentVectorAngle(double inTangentVectorAngle) {
			this.inTangentVectorAngle = inTangentVectorAngle;
		}

		public double getInTangentVectorLength() {
			return inTangentVectorLength;
		}

		public void setInTangentVectorLength(double inTangentVectorLength) {
			this.inTangentVectorLength = inTangentVectorLength;
		}

		public double getOutTangentVectorAngle() {
			return outTangentVectorAngle;
		}

		public void setOutTangentVectorAngle(double outTangentVectorAngle) {
			this.outTangentVectorAngle = outTangentVectorAngle;
		}

		public double getOutTangentVectorLength() {
			return outTangentVectorLength;
		}

		public void setOutTangentVectorLength(double outTangentVectorLength) {
			this.outTangentVectorLength = outTangentVectorLength;
		}

		public EdgeEndPoint getIn() {
			return in;
		}

		public EdgeEndPoint getOut() {
			return out;
		}

		public void disconnectFromAny() {
			getIn().disconnect();
			getOut().disconnect();
		}


		protected EdgeEndPoint getNextEndPoint(EdgeEndPoint endpoint) {
			if (endpoint == in) {
				return out;
			} else {
				return endpoint.getOtherEndPoint();
			}
		}

		protected EdgeEndPoint getPreviousEndPoint(EdgeEndPoint endpoint) {
			if (endpoint == out) {
				return in;
			} else {
				return endpoint.getOtherEndPoint();
			}
		}
		
		public EdgeEndPoint getIn1EndPoint() {
			return in;
		}


		public EdgeEndPoint getEndPointFromPosition(
				EdgeEndPointPosition position) {
			switch (position) {
			case IN1:
				return getIn();
			case OUT1:
				return getOut();
			default:
				return null;
			}
		}


		public EdgeEndPointPosition getPositionFromEndPoint(
				EdgeEndPoint endPoint) {
			if (endPoint == in) {
				return EdgeEndPointPosition.IN1;
			} else if (endPoint == out) {
				return EdgeEndPointPosition.OUT1;
			} else {
				return null;
			}
		}

		
	}

	
}
