/**
 * File written by Raphael Champeimont
 * UMR 7238 Genomique des Microorganismes
 */
package fr.orsay.lri.varna.models.rna;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import fr.orsay.lri.varna.exceptions.ExceptionInvalidRNATemplate;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.geom.ComputeArcCenter;
import fr.orsay.lri.varna.models.geom.ComputeEllipseAxis;
import fr.orsay.lri.varna.models.geom.CubicBezierCurve;
import fr.orsay.lri.varna.models.geom.HalfEllipse;
import fr.orsay.lri.varna.models.geom.LinesIntersect;
import fr.orsay.lri.varna.models.geom.MiscGeom;
import fr.orsay.lri.varna.models.templates.DrawRNATemplateCurveMethod;
import fr.orsay.lri.varna.models.templates.DrawRNATemplateMethod;
import fr.orsay.lri.varna.models.templates.RNANodeValueTemplate;
import fr.orsay.lri.varna.models.templates.RNANodeValueTemplateBasePair;
import fr.orsay.lri.varna.models.templates.RNATemplate;
import fr.orsay.lri.varna.models.templates.RNATemplateAlign;
import fr.orsay.lri.varna.models.templates.RNATemplateDrawingAlgorithmException;
import fr.orsay.lri.varna.models.templates.RNATemplateMapping;
import fr.orsay.lri.varna.models.templates.RNATemplate.EdgeEndPointPosition;
import fr.orsay.lri.varna.models.templates.RNATemplate.In1Is;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateElement;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateHelix;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateUnpairedSequence;
import fr.orsay.lri.varna.models.templates.RNATemplate.RNATemplateElement.EdgeEndPoint;
import fr.orsay.lri.varna.models.treealign.Tree;

public class DrawRNATemplate {
	private RNA rna;
	private RNATemplateMapping mapping;
	private ArrayList<ModeleBase> _listeBases;
	
	
	public DrawRNATemplate(RNA rna) {
		this.rna = rna;
		this._listeBases = rna.getListeBases();
	}
	
	public RNATemplateMapping getMapping() {
		return mapping;
	}
	
	
	
	/**
	 * Draw this RNA like the given template.
	 * The helixLengthAdjustmentMethod argument tells what to do in case
	 * some helices are of a different length in the template and the
	 * actual helix. See class DrawRNATemplateMethod above for possible values.
	 * @param straightBulges 
	 */
	public void drawRNATemplate(
			RNATemplate template,
			VARNAConfig conf,
			DrawRNATemplateMethod helixLengthAdjustmentMethod,
			DrawRNATemplateCurveMethod curveMethod, boolean straightBulges)
	throws RNATemplateDrawingAlgorithmException {
		
		// debug
//		try {
//			RNA perfectMatchingRNA = template.toRNA();
//			System.out.println("An RNA that would perfectly match this template would be:");
//			System.out.println(perfectMatchingRNA.getStructDBN());
//		} catch (ExceptionInvalidRNATemplate e) {
//			e.printStackTrace();
//		}
		
		mapping = RNATemplateAlign.mapRNAWithTemplate(rna, template);
		//System.out.println(mapping.showCompact(this));
		
		// debug
//			RNATemplateAlign.printMapping(mapping, template, getSeq());
//			try {
//				TreeGraphviz.treeToGraphvizPostscript(alignment, "alignment_graphviz.ps");
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		
		

		Iterator<RNATemplateElement> iter;
		double globalIncreaseFactor = 1;
		Map<RNATemplateHelix, Point2D.Double> translateVectors = null;
		if (helixLengthAdjustmentMethod == DrawRNATemplateMethod.MAXSCALINGFACTOR) {
			// Compute increase factors for helices.
			Map<RNATemplateHelix, Double> lengthIncreaseFactor = new HashMap<RNATemplateHelix, Double>();
			double maxLengthIncreaseFactor = Double.NEGATIVE_INFINITY;
			//RNATemplateHelix maxIncreaseHelix = null;
			iter = template.rnaIterator();
			while (iter.hasNext()) {
				RNATemplateElement element = iter.next();
				if (element instanceof RNATemplateHelix
						&& mapping.getAncestor(element) != null
						&& !lengthIncreaseFactor.containsKey(element)) {
					RNATemplateHelix helix = (RNATemplateHelix) element;
					int[] basesInHelixArray = RNATemplateAlign.intArrayFromList(mapping.getAncestor(helix));
					Arrays.sort(basesInHelixArray);
					double l = computeLengthIncreaseFactor(basesInHelixArray, helix, straightBulges);
					lengthIncreaseFactor.put(helix, l);
					if (l > maxLengthIncreaseFactor) {
						maxLengthIncreaseFactor = l;
						//maxIncreaseHelix = helix;
					}
				}
			}
			
			// debug
			//System.out.println("Max helix length increase factor = " + maxLengthIncreaseFactor + " reached with helix " + maxIncreaseHelix);;
			
			globalIncreaseFactor = Math.max(1, maxLengthIncreaseFactor);
			
		} else if (helixLengthAdjustmentMethod == DrawRNATemplateMethod.HELIXTRANSLATE) {
			try {
				// Now we need to propagate this helices translations
				Tree<RNANodeValueTemplate> templateAsTree = template.toTree();
				translateVectors = computeHelixTranslations(templateAsTree, mapping, straightBulges);
			
			} catch (ExceptionInvalidRNATemplate e) {
				throw (new RNATemplateDrawingAlgorithmException("ExceptionInvalidRNATemplate: " + e.getMessage()));
			}
		}
		
		// Allocate the coords and centers arrays
		// We create Point2D.Double objects in it but the algorithms
		// we use may choose to create new Point2D.Double objects or to
		// modify those created here.
		Point2D.Double[] coords = new Point2D.Double[_listeBases.size()];
		Point2D.Double[] centers = new Point2D.Double[_listeBases.size()];
		double[] angles = new double[_listeBases.size()];
		for (int i = 0; i < _listeBases.size(); i++) {
			coords[i] = new Point2D.Double(0, 0);
			centers[i] = new Point2D.Double(0, 0);
		}
		
		boolean computeCoords = true;
		while (computeCoords) {
			computeCoords = false;
			// Compute coords and centers
			Set<RNATemplateHelix> alreadyDrawnHelixes = new HashSet<RNATemplateHelix>();
			RNATemplateHelix lastMappedHelix = null;
			EdgeEndPoint howWeGotOutOfLastHelix = null;
			int howWeGotOutOfLastHelixBaseIndex = 0;
			iter = template.rnaIterator();
			RNATemplateElement element = null;
			while (iter.hasNext()) {
				element = iter.next();
				if (element instanceof RNATemplateHelix
						&& mapping.getAncestor(element) != null) {
					// We have a mapping between an helix in the RNA sequence
					// and an helix in the template.
					
					RNATemplateHelix helix = (RNATemplateHelix) element;
					boolean firstTimeWeMeetThisHelix;
					int[] basesInHelixArray = RNATemplateAlign.intArrayFromList(mapping.getAncestor(helix));
					Arrays.sort(basesInHelixArray);
					
					// Draw this helix if it has not already been done
					if (!alreadyDrawnHelixes.contains(helix)) {
						firstTimeWeMeetThisHelix = true;
						drawHelixLikeTemplateHelix(basesInHelixArray, helix, coords, centers,angles, globalIncreaseFactor, translateVectors, straightBulges);
						alreadyDrawnHelixes.add(helix);
					} else {
						firstTimeWeMeetThisHelix = false;
					}
					
					EdgeEndPoint howWeGetInCurrentHelix;
					if (firstTimeWeMeetThisHelix) {
						if (helix.getIn1Is() == In1Is.IN1_IS_5PRIME) {
							howWeGetInCurrentHelix = helix.getIn1();
						} else {
							howWeGetInCurrentHelix = helix.getIn2();
						}
					} else {
						if (helix.getIn1Is() == In1Is.IN1_IS_5PRIME) {
							howWeGetInCurrentHelix = helix.getIn2();
						} else {
							howWeGetInCurrentHelix = helix.getIn1();
						}
					}
					
					Point2D.Double P0 = new Point2D.Double();
					Point2D.Double P3 = new Point2D.Double();
					
					if (lastMappedHelix != null) {
						// Now draw the RNA sequence (possibly containing helixes)
						// between the last template drawn helix and this one.
	
						if (lastMappedHelix == helix) {
							// Last helix is the same as the current one so
							// nothing matched (or at best a single
							// non-paired sequence) so we will just
							// use the Radiate algorithm
							
							Point2D.Double helixVector = new Point2D.Double();
							computeHelixEndPointDirections(howWeGotOutOfLastHelix, helixVector, new Point2D.Double());
							
							double angle = MiscGeom.angleFromVector(helixVector);
							int b1 = basesInHelixArray[basesInHelixArray.length/2 - 1];
							P0.setLocation(coords[b1]);
							int b2 = basesInHelixArray[basesInHelixArray.length/2];
							P3.setLocation(coords[b2]);
							Point2D.Double loopCenter = new Point2D.Double((P0.x + P3.x)/2, (P0.y + P3.y)/2);
							rna.drawLoop(b1,
									 b2,
									 loopCenter.x,
									 loopCenter.y,
									 angle,
									 coords,
									 centers,
									 angles,
									 straightBulges);
							// If the helix is flipped, we need to compute the symmetric
							// of the whole loop.
							if (helix.isFlipped()) {
								symmetric(loopCenter, helixVector, coords, b1, b2);
								symmetric(loopCenter, helixVector, centers, b1, b2);
							}
						} else {
							// No helices matched between the last helix and
							// the current one, so we draw what is between
							// using the radiate algorithm but on the Bezier curve.
							
							int b1 = howWeGotOutOfLastHelixBaseIndex;
							int b2 = firstTimeWeMeetThisHelix ? basesInHelixArray[0] : basesInHelixArray[basesInHelixArray.length/2];
							P0.setLocation(coords[b1]);
							P3.setLocation(coords[b2]);
							
							Point2D.Double P1, P2;
							
							if (howWeGotOutOfLastHelix.getOtherElement() instanceof RNATemplateUnpairedSequence
									&& howWeGetInCurrentHelix.getOtherElement() instanceof RNATemplateUnpairedSequence) {
								// We will draw the bases on a Bezier curve
								P1 = new Point2D.Double();
								computeBezierTangentVectorTarget(howWeGotOutOfLastHelix, P0, P1);
								
								P2 = new Point2D.Double();
								computeBezierTangentVectorTarget(howWeGetInCurrentHelix, P3, P2);
							} else {
								// We will draw the bases on a straight line between P0 and P3
								P1 = null;
								P2 = null;
							}
							
							drawAlongCurve(b1, b2, P0, P1, P2, P3, coords, centers, angles, curveMethod, lastMappedHelix.isFlipped(), straightBulges);
						}
						
					} else if (basesInHelixArray[0] > 0) {
						// Here we draw what is before the first mapped helix.
	
						RNATemplateUnpairedSequence templateSequence;
						// Try to find our template sequence as the mapped element of base 0
						RNATemplateElement templateSequenceCandidate = mapping.getPartner(0);
						if (templateSequenceCandidate != null
								&& templateSequenceCandidate instanceof RNATemplateUnpairedSequence) {
							templateSequence = (RNATemplateUnpairedSequence) templateSequenceCandidate;
						} else {
							// Try other idea: first template element if it is a sequence
							templateSequenceCandidate = template.getFirst();
							if (templateSequenceCandidate != null
									&& templateSequenceCandidate instanceof RNATemplateUnpairedSequence) {
								templateSequence = (RNATemplateUnpairedSequence) templateSequenceCandidate;
							} else {
								// We don't know where to start
								templateSequence = null;
							}
						}
						
						int b1 = 0;
						int b2 = firstTimeWeMeetThisHelix ? basesInHelixArray[0] : basesInHelixArray[basesInHelixArray.length/2];
						P3.setLocation(coords[b2]);
						
						if (templateSequence != null) {
							coords[0].setLocation(templateSequence.getVertex5());
							coords[0].x *= globalIncreaseFactor;
							coords[0].y *= globalIncreaseFactor;
						} else {
							// Put b1 at an ideal distance from b2, using the "flat exterior loop" method
							double idealLength = computeStraightLineIdealLength(b1, b2);
							Point2D.Double j = new Point2D.Double();
							if (howWeGetInCurrentHelix != null) {
								computeHelixEndPointDirections(howWeGetInCurrentHelix, new Point2D.Double(), j);
							} else {
								j.setLocation(1, 0);
							}
							coords[b1].setLocation(coords[b2].x + j.x*idealLength, coords[b2].y + j.y*idealLength);
						}
						P0.setLocation(coords[0]);
						
						Point2D.Double P1, P2;
						
						if (howWeGetInCurrentHelix.getOtherElement() instanceof RNATemplateUnpairedSequence
								&& templateSequence != null) {
							// We will draw the bases on a Bezier curve
							P1 = new Point2D.Double();
							computeBezierTangentVectorTarget(templateSequence.getIn(), P0, P1);
							
							P2 = new Point2D.Double();
							computeBezierTangentVectorTarget(howWeGetInCurrentHelix, P3, P2);
						} else {
							// We will draw the bases on a straight line between P0 and P3
							P1 = null;
							P2 = null;
						}
						
						drawAlongCurve(b1, b2, P0, P1, P2, P3, coords, centers, angles, curveMethod, false, straightBulges);
					}
					
					lastMappedHelix = helix;
					howWeGotOutOfLastHelix = howWeGetInCurrentHelix.getNextEndPoint();
					if (firstTimeWeMeetThisHelix) {
						howWeGotOutOfLastHelixBaseIndex = basesInHelixArray[basesInHelixArray.length/2-1];
					} else {
						howWeGotOutOfLastHelixBaseIndex = basesInHelixArray[basesInHelixArray.length-1];
					}
				}
			} // end template iteration
			
			
			// Now we need to draw what is after the last mapped helix.
			if (howWeGotOutOfLastHelixBaseIndex < coords.length-1
					&& element != null
					&& coords.length > 1) {
				
				RNATemplateUnpairedSequence beginTemplateSequence = null;
				if (lastMappedHelix == null) {
					// No helix at all matched between the template and RNA!
					// So the sequence we want to draw is the full RNA.
					
					// Try to find our template sequence as the mapped element of base 0
					RNATemplateElement templateSequenceCandidate = mapping.getPartner(0);
					if (templateSequenceCandidate != null
							&& templateSequenceCandidate instanceof RNATemplateUnpairedSequence) {
						beginTemplateSequence = (RNATemplateUnpairedSequence) templateSequenceCandidate;
					} else {
						// Try other idea: first template element if it is a sequence
						templateSequenceCandidate = template.getFirst();
						if (templateSequenceCandidate != null
								&& templateSequenceCandidate instanceof RNATemplateUnpairedSequence) {
							beginTemplateSequence = (RNATemplateUnpairedSequence) templateSequenceCandidate;
						} else {
							// We don't know where to start
							beginTemplateSequence = null;
						}
					}
					
					if (beginTemplateSequence != null) {
						coords[0].setLocation(beginTemplateSequence.getVertex5());
						coords[0].x *= globalIncreaseFactor;
						coords[0].y *= globalIncreaseFactor;
					}
					
				}
				
				RNATemplateUnpairedSequence endTemplateSequence;
				// Try to find our template sequence as the mapped element of last base
				RNATemplateElement templateSequenceCandidate = mapping.getPartner(coords.length-1);
				if (templateSequenceCandidate != null
						&& templateSequenceCandidate instanceof RNATemplateUnpairedSequence) {
					endTemplateSequence = (RNATemplateUnpairedSequence) templateSequenceCandidate;
				} else {
					// Try other idea: last template element if it is a sequence
					templateSequenceCandidate = element;
					if (templateSequenceCandidate != null
							&& templateSequenceCandidate instanceof RNATemplateUnpairedSequence) {
						endTemplateSequence = (RNATemplateUnpairedSequence) templateSequenceCandidate;
					} else {
						// We don't know where to end
						endTemplateSequence = null;
					}
				}
				
				int b1 = howWeGotOutOfLastHelixBaseIndex;
				int b2 = coords.length - 1;
				
				if (endTemplateSequence != null) {
					coords[b2].setLocation(endTemplateSequence.getVertex3());
					coords[b2].x *= globalIncreaseFactor;
					coords[b2].y *= globalIncreaseFactor;
				} else {
					// Put b2 at an ideal distance from b1, using the "flat exterior loop" method
					double idealLength = computeStraightLineIdealLength(b1, b2);
					Point2D.Double j = new Point2D.Double();
					if (howWeGotOutOfLastHelix != null) {
						computeHelixEndPointDirections(howWeGotOutOfLastHelix, new Point2D.Double(), j);
					} else {
						j.setLocation(1, 0);
					}
					coords[b2].setLocation(coords[b1].x + j.x*idealLength, coords[b1].y + j.y*idealLength);
				}

				
				Point2D.Double P0 = new Point2D.Double();
				Point2D.Double P3 = new Point2D.Double();
				
				P0.setLocation(coords[b1]);
				P3.setLocation(coords[b2]);
				
				Point2D.Double P1, P2;
				
				if (howWeGotOutOfLastHelix != null
						&& howWeGotOutOfLastHelix.getOtherElement() instanceof RNATemplateUnpairedSequence
						&& endTemplateSequence != null) {
					// We will draw the bases on a Bezier curve
					P1 = new Point2D.Double();
					computeBezierTangentVectorTarget(howWeGotOutOfLastHelix, P0, P1);
					
					P2 = new Point2D.Double();
					computeBezierTangentVectorTarget(endTemplateSequence.getOut(), P3, P2);
				} else if (lastMappedHelix == null
						&& beginTemplateSequence != null
						&& endTemplateSequence != null) {
					// We will draw the bases on a Bezier curve
					P1 = new Point2D.Double();
					computeBezierTangentVectorTarget(beginTemplateSequence.getIn(), P0, P1);
					
					P2 = new Point2D.Double();
					computeBezierTangentVectorTarget(endTemplateSequence.getOut(), P3, P2);
				} else {
					// We will draw the bases on a straight line between P0 and P3
					P1 = null;
					P2 = null;
				}
				
				drawAlongCurve(b1, b2, P0, P1, P2, P3, coords, centers, angles, curveMethod, lastMappedHelix != null ? lastMappedHelix.isFlipped() : false, straightBulges);
			
			}
			
			
			if (helixLengthAdjustmentMethod == DrawRNATemplateMethod.NOINTERSECT && coords.length > 3) {
				// Are we happy with this value of globalIncreaseFactor?
				Line2D.Double[] lines = new Line2D.Double[coords.length-1];
				for (int i=0; i<coords.length-1; i++) {
					lines[i] = new Line2D.Double(coords[i], coords[i+1]);
				}
				int intersectLines = 0;
				for (int i=0; i<lines.length; i++) {
					for (int j=i+2; j<lines.length; j++) {
						if (LinesIntersect.linesIntersect(lines[i], lines[j])) {
							intersectLines++;
						}
					}
				}
				// If no intersection we keep this globalIncreaseFactor value
				if (intersectLines > 0) {
					// Don't increase more than a maximum value
					if (globalIncreaseFactor < 3) {
						globalIncreaseFactor += 0.1;
						//System.out.println("globalIncreaseFactor increased to " + globalIncreaseFactor);
						// Compute the drawing again
						computeCoords = true;
					}
				}
			}
			
		}
		
		// debug
		if (helixLengthAdjustmentMethod == DrawRNATemplateMethod.MAXSCALINGFACTOR
				|| helixLengthAdjustmentMethod == DrawRNATemplateMethod.NOINTERSECT) {
			//System.out.println("globalIncreaseFactor = " + globalIncreaseFactor);
		}
		
		// Now we actually move the bases, according to arrays coords and centers
		// and taking in account the space between bases parameter.
		for (int i = 0; i < _listeBases.size(); i++) {
			_listeBases.get(i).setCoords(
					new Point2D.Double(coords[i].x * conf._spaceBetweenBases,
							coords[i].y * conf._spaceBetweenBases));
			_listeBases.get(i).setCenter(
					new Point2D.Double(centers[i].x * conf._spaceBetweenBases,
							centers[i].y * conf._spaceBetweenBases));
		}
	
	}
	
	
	
	
	
	
	/**
	 * IN: Argument helixEndPoint is an IN argument (will be read),
	 * and must contain an helix edge endpoint.
	 * 
	 * The other arguments are OUT arguments
	 * (must be existing objects, content will be overwritten).
	 * 
	 * OUT: The i argument will contain a vector colinear to the vector
	 * from the helix startPosition to endPosition or the opposite
	 * depending on there the endpoint is (the endpoint will be on the
	 * destination side of the vector). ||i|| = 1
	 * 
	 * OUT: The j vector will contain an vector that is colinear
	 * to the last/first base pair connection on the side of this endpoint.
	 * The vector will be oriented to the side of the given endpoint.
	 * ||j|| = 1
	 */
	private void computeHelixEndPointDirections(
			EdgeEndPoint helixEndPoint, // IN
			Point2D.Double i, // OUT
			Point2D.Double j // OUT
			) {
		RNATemplateHelix helix = (RNATemplateHelix) helixEndPoint.getElement();
		Point2D.Double startpos = helix.getStartPosition();
		Point2D.Double endpos = helix.getEndPosition();
		Point2D.Double helixVector = new Point2D.Double();
		switch (helixEndPoint.getPosition()) {
		case IN1:
		case OUT2:
			helixVector.x = startpos.x - endpos.x;
			helixVector.y = startpos.y - endpos.y;
			break;
		case IN2:
		case OUT1:
			helixVector.x = endpos.x - startpos.x;
			helixVector.y = endpos.y - startpos.y;
			break;
		}
		double helixVectorLength = Math.hypot(helixVector.x, helixVector.y);
		// i is the vector which is colinear to helixVector and such that ||i|| = 1
		i.x = helixVector.x / helixVectorLength;
		i.y = helixVector.y / helixVectorLength;
		// Find j such that it is orthogonal to i, ||j|| = 1
		// and j goes to the side where the sequence will be connected
		switch (helixEndPoint.getPosition()) {
		case IN1:
		case IN2:
			// rotation of +pi/2
			j.x = - i.y;
			j.y =   i.x;
			break;
		case OUT1:
		case OUT2:
			// rotation of -pi/2
			j.x =   i.y;
			j.y = - i.x;
			break;
		}
		if (helix.isFlipped()) {
			j.x = - j.x;
			j.y = - j.y;
		}

	}
	
	/**
	 * A cubic Bezier curve can be defined by 4 points,
	 * see http://en.wikipedia.org/wiki/Bezier_curve#Cubic_B.C3.A9zier_curves
	 * For each of the curve end points, there is the last/first point of the
	 * curve and a point which gives the direction and length of the tangent
	 * vector on that side. This two points are respectively curveEndPoint
	 * and curveVectorOtherPoint.
	 * IN:  Argument helixVector is the vector formed by the helix,
	 *      in the right direction for our sequence.
	 * IN:  Argument curveEndPoint is the position of the endpoint on the helix.
	 * OUT: Argument curveVectorOtherPoint must be allocated
	 *      and the values will be modified.
	 */
	private void computeBezierTangentVectorTarget(
			EdgeEndPoint endPoint,
			Point2D.Double curveEndPoint,
			Point2D.Double curveVectorOtherPoint)
			throws RNATemplateDrawingAlgorithmException {
		
		boolean sequenceEndPointIsIn;
		RNATemplateUnpairedSequence sequence;
		
		if (endPoint.getElement() instanceof RNATemplateHelix) {
			sequence = (RNATemplateUnpairedSequence) endPoint.getOtherElement();
			EdgeEndPointPosition endPointPositionOnHelix = endPoint.getPosition();
			switch (endPointPositionOnHelix) {
			case IN1:
			case IN2:
				sequenceEndPointIsIn = false;
				break;
			default:
				sequenceEndPointIsIn = true;
			}
			
			EdgeEndPoint endPointOnHelix =
				sequenceEndPointIsIn ?
						sequence.getIn().getOtherEndPoint() :
						sequence.getOut().getOtherEndPoint();
			if (endPointOnHelix == null) {
				throw (new RNATemplateDrawingAlgorithmException("Sequence is not connected to an helix."));
			}
		} else {
			// The endpoint is on an unpaired sequence.
			sequence = (RNATemplateUnpairedSequence) endPoint.getElement();
			if (endPoint == sequence.getIn()) {
				// endpoint is 5'
				sequenceEndPointIsIn = true;
			} else {
				sequenceEndPointIsIn = false;
			}
		}

		double l =
			sequenceEndPointIsIn ?
				sequence.getInTangentVectorLength() :
				sequence.getOutTangentVectorLength();
		
		// Compute the absolute angle our line makes to the helix
		double theta =
			sequenceEndPointIsIn ?
				sequence.getInTangentVectorAngle() :
				sequence.getOutTangentVectorAngle();
		
		// Compute v, the tangent vector of the Bezier curve
		Point2D.Double v = new Point2D.Double();
		v.x = l * Math.cos(theta);
		v.y = l * Math.sin(theta);
		curveVectorOtherPoint.x = curveEndPoint.x + v.x;
		curveVectorOtherPoint.y = curveEndPoint.y + v.y;
	}
	

	/**
	 * Compute (actual helix length / helix length in template).
	 * @param straightBulges 
	 */
	private double computeLengthIncreaseFactor(
			int[] basesInHelixArray,  // IN
			RNATemplateHelix helix,    // IN
			boolean straightBulges
			) {
		double templateLength = computeHelixTemplateLength(helix);
		double realLength = computeHelixRealLength(basesInHelixArray,straightBulges);
		return realLength / templateLength;
	}
	
	/**
	 * Compute (actual helix vector - helix vector in template).
	 */
	private Point2D.Double computeLengthIncreaseDelta(
			int[] basesInHelixArray,  // IN
			RNATemplateHelix helix,   // IN
			boolean straightBulges
			) {
		double templateLength = computeHelixTemplateLength(helix);
		double realLength = computeHelixRealLength(basesInHelixArray,straightBulges);
		Point2D.Double i = new Point2D.Double();
		computeTemplateHelixVectors(helix, null, i, null);
		return new Point2D.Double(i.x*(realLength-templateLength), i.y*(realLength-templateLength));
	}
	
	/**
	 * Compute helix interesting vectors from template helix.
	 * @param helix The template helix you want to compute the vectors from.
	 * @param o This point coordinates will be set the origin of the helix (or not if null),
	 *          ie. the point in the middle of the base pair with the two most extreme bases.
	 * @param i Will be set to the normalized helix vector. (nothing done if null)
	 * @param j Will be set to the normalized helix base pair vector (5' -> 3'). (nothing done if null)
	 */
	private void computeTemplateHelixVectors(
			RNATemplateHelix helix,  // IN
			Point2D.Double o,        // OUT
			Point2D.Double i,        // OUT
			Point2D.Double j         // OUT
			) {
		Point2D.Double startpos, endpos;
		if (helix.getIn1Is() == In1Is.IN1_IS_5PRIME) {
			startpos = helix.getStartPosition();
			endpos = helix.getEndPosition();
		} else {
			endpos = helix.getStartPosition();
			startpos = helix.getEndPosition();
		}
		if (o != null) {
			o.x = startpos.x;
			o.y = startpos.y;
		}
		if (i != null || j != null) {
			// (i_x,i_y) is the vector between two consecutive bases of the same side of an helix
			if (i == null)
				i = new Point2D.Double();
			i.x = (endpos.x - startpos.x);
			i.y = (endpos.y - startpos.y);
			double i_original_norm = Math.hypot(i.x, i.y);
			// change its norm to 1
			i.x = i.x / i_original_norm;
			i.y = i.y / i_original_norm;
			if (j != null) {
				j.x = - i.y;
				j.y =   i.x;
				if (helix.isFlipped()) {
					j.x = - j.x;
					j.y = - j.y;
				}
				double j_original_norm = Math.hypot(j.x, j.y);
				// change (j_x,j_y) so that its norm is 1
				j.x = j.x / j_original_norm;
				j.y = j.y / j_original_norm;
			}
		}
	}
	
	
	/**
	 * Estimate bulge arc length.
	 */
	private double estimateBulgeArcLength(int firstBase, int lastBase) {
		if (firstBase + 1 == lastBase)
			return RNA.LOOP_DISTANCE; // there is actually no bulge
		double len = 0.0;
		int k = firstBase;
		while (k < lastBase) {
			int l = _listeBases.get(k).getElementStructure();
			if (k < l && l < lastBase) {
				len += RNA.BASE_PAIR_DISTANCE;
				k = l;
			} else {
				len += RNA.LOOP_DISTANCE;
				k++;
			}
		}
		return len;
	}
	
	
	/**
	 * Estimate bulge width, the given first and last bases must be those in the helix.
	 */
	private double estimateBulgeWidth(int firstBase, int lastBase) {
		double len = estimateBulgeArcLength(firstBase, lastBase);
		return 2 * (len / Math.PI);
	}
	
	
	/**
	 * Get helix length in template.
	 */
	private double computeHelixTemplateLength(RNATemplateHelix helix) {
		return Math.hypot(helix.getStartPosition().x - helix.getEndPosition().x,
				helix.getStartPosition().y - helix.getEndPosition().y);
	}
	
	
	/**
	 * Compute helix actual length (as drawHelixLikeTemplateHelix() would draw it).
	 */
	private double computeHelixRealLength(int[] basesInHelixArray, boolean straightBulges) {
		return drawHelixLikeTemplateHelix(basesInHelixArray, null, null, null, null, 0, null,straightBulges);
	}
	
	
	/**
	 * Result type of countUnpairedLine(), see below.
	 */
	private static class UnpairedLineCounts {
		public int nBP, nLD, total;
	}
	/**
	 * If we are drawing an unpaired region that may contains helices,
	 * and we are drawing it on a line (curve or straight, doesn't matter),
	 * how many intervals should have a base-pair length (start of an helix)
	 * and how many should have a consecutive unpaired bases length?
	 * Returns an array with three elements:
	 * - answer to first question
	 * - answer to second question
	 * - sum of both, ie. total number of intervals
	 *   (this is NOT lastBase-firstBase because bases deep in helices do not count)
	 */
	private UnpairedLineCounts countUnpairedLine(int firstBase, int lastBase) {
		UnpairedLineCounts counts = new UnpairedLineCounts();
		int nBP = 0;
		int nLD = 0;
		{
			int b = firstBase;
			while (b < lastBase) {
				int l = _listeBases.get(b).getElementStructure();
				if (b < l && l < lastBase) {
					nBP++;
					b = l;
				} else {
					nLD++;
					b++;
				}
			}
		}
		counts.nBP = nBP;
		counts.nLD = nLD;
		counts.total = nBP + nLD;
		return counts;
	}
	
	
	/**
	 * Draw the given helix (given as a *SORTED* array of indexes)
	 * like defined in the given template helix.
	 * OUT: The bases positions are not changed in fact,
	 *      instead the coords and centers arrays are modified.
	 * IN:  The helix origin position is multiplied by scaleHelixOrigin
	 *      and translateVectors.get(helix) is added.
	 * RETURN VALUE:
	 *      The length of the drawn helix.
	 * @param straightBulges 
	 * 
	 */
	private double drawHelixLikeTemplateHelix(
			int[] basesInHelixArray,  // IN
			RNATemplateHelix helix,   // IN  (optional, ie. may be null)
			Point2D.Double[] coords,  // OUT (optional, ie. may be null)
			Point2D.Double[] centers, // OUT (optional, ie. may be null)
			double[] angles,  // OUT 
			double scaleHelixOrigin,  // IN
			Map<RNATemplateHelix, Point2D.Double> translateVectors // IN (optional, ie. may be null)
, boolean straightBulges
			) {
		int n = basesInHelixArray.length / 2;
		if (n == 0)
			return 0;
		 // Default values when not template helix is provided:
		Point2D.Double o = new Point2D.Double(0, 0);
		Point2D.Double i = new Point2D.Double(1, 0);
		Point2D.Double j = new Point2D.Double(0, 1);
		boolean flipped = false;
		if (helix != null) {
			computeTemplateHelixVectors(helix, o, i, j);
			flipped = helix.isFlipped();
		}
		Point2D.Double li = new Point2D.Double(i.x*RNA.LOOP_DISTANCE, i.y*RNA.LOOP_DISTANCE);
		// We want o to be the point where the first base (5' end) is
		o.x = (o.x - j.x * RNA.BASE_PAIR_DISTANCE / 2) * scaleHelixOrigin;
		o.y = (o.y - j.y * RNA.BASE_PAIR_DISTANCE / 2) * scaleHelixOrigin;
		if (translateVectors != null && translateVectors.containsKey(helix)) {
			Point2D.Double v = translateVectors.get(helix);
			o.x = o.x + v.x;
			o.y = o.y + v.y;
		}
		
		// We need this array so that we can store positions even if coords == null
		Point2D.Double[] helixBasesPositions = new Point2D.Double[basesInHelixArray.length];
		for (int k=0; k<helixBasesPositions.length; k++) {
			helixBasesPositions[k] = new Point2D.Double();	
		}
		Point2D.Double accDelta = new Point2D.Double(0, 0);
		for (int k=0; k<n; k++) {
			int kp = 2*n-k-1;
			Point2D.Double p1 = helixBasesPositions[k]; // we assign the point *reference*
			Point2D.Double p2 = helixBasesPositions[kp];
			// Do we have a bulge between previous base pair and this one?
			boolean bulge = k >= 1 && (basesInHelixArray[k] != basesInHelixArray[k-1] + 1
					                   || basesInHelixArray[kp+1] != basesInHelixArray[kp] + 1);
			if (k >= 1) {
				if (basesInHelixArray[k] < basesInHelixArray[k-1]
				    || basesInHelixArray[kp+1] < basesInHelixArray[kp]) {
					throw new Error("Internal bug: basesInHelixArray must be sorted");
				}
				if (bulge) {
					// Estimate a good distance (delta) between the previous base pair and this one
					double delta1 = estimateBulgeWidth(basesInHelixArray[k-1], basesInHelixArray[k]);
					double delta2 = estimateBulgeWidth(basesInHelixArray[kp], basesInHelixArray[kp+1]);
					// The biggest bulge defines the width
					double delta = Math.max(delta1, delta2);

					if (coords != null) {
						// Now, where do we put the bases that are part of the bulge?
						for (int side=0; side<2; side++) {
							Point2D.Double pstart = new Point2D.Double();
							Point2D.Double pend = new Point2D.Double();
							Point2D.Double bisectVect = new Point2D.Double();
							Point2D.Double is = new Point2D.Double();
							int firstBase, lastBase;
							double alphasign = flipped ? -1 : 1;
							if (side == 0) {
								firstBase = basesInHelixArray[k-1];
								lastBase  = basesInHelixArray[k];
								pstart.setLocation(o.x + accDelta.x,
								                   o.y + accDelta.y);
								pend.setLocation(o.x + accDelta.x + i.x*delta,
								                 o.y + accDelta.y + i.y*delta);
								bisectVect.setLocation(-j.x, -j.y);
								is.setLocation(i);
							} else {
								firstBase = basesInHelixArray[kp];
								lastBase  = basesInHelixArray[kp+1];
								pstart.setLocation(o.x + accDelta.x + i.x*delta + j.x*RNA.BASE_PAIR_DISTANCE,
						                           o.y + accDelta.y + i.y*delta + j.y*RNA.BASE_PAIR_DISTANCE);
								pend.setLocation(o.x + accDelta.x + j.x*RNA.BASE_PAIR_DISTANCE,
								                 o.y + accDelta.y + j.y*RNA.BASE_PAIR_DISTANCE);

								bisectVect.setLocation(j);
								is.setLocation(-i.x, -i.y);
							}
							double arclen = estimateBulgeArcLength(firstBase, lastBase);
							double centerOnBisect = ComputeArcCenter.computeArcCenter(delta, arclen);
							
							// Should we draw the base on an arc or simply use a line?
							if (centerOnBisect > -1000) {
								Point2D.Double center = new Point2D.Double(pstart.x + is.x*delta/2 + bisectVect.x*centerOnBisect,
								                                           pstart.y + is.y*delta/2 + bisectVect.y*centerOnBisect);
								int b = firstBase;
								double len = 0;
								double r = Math.hypot(pstart.x - center.x, pstart.y - center.y);
								double alpha0 = MiscGeom.angleFromVector(pstart.x - center.x, pstart.y - center.y);
								while (b < lastBase) {
									int l = _listeBases.get(b).getElementStructure();
									if (b < l && l < lastBase) {
										len += RNA.BASE_PAIR_DISTANCE;
										b = l;
									} else {
										len += RNA.LOOP_DISTANCE;
										b++;
									}
									if (b < lastBase) {
										coords[b].x = center.x + r*Math.cos(alpha0 + alphasign*len/r);
										coords[b].y = center.y + r*Math.sin(alpha0 + alphasign*len/r);
									}
								}
							} else {
								// Draw on a line
								
								// Distance between paired bases cannot be changed
								// (imposed by helix width) but distance between other
								// bases can be adjusted.
								UnpairedLineCounts counts = countUnpairedLine(firstBase, lastBase);
								double LD = Math.max((delta - counts.nBP*RNA.BASE_PAIR_DISTANCE) / (double) counts.nLD, 0);
								//System.out.println("nBP=" + nBP + " nLD=" + nLD);
								double len = 0;
								{
									int b = firstBase;
									while (b < lastBase) {
										int l = _listeBases.get(b).getElementStructure();
										if (b < l && l < lastBase) {
											len += RNA.BASE_PAIR_DISTANCE;
											b = l;
										} else {
											len += LD;
											b++;
										}
										if (b < lastBase) {
											coords[b].x = pstart.x + is.x*len;
											coords[b].y = pstart.y + is.y*len;
										}
									}
								}
								//System.out.println("len=" + len + " delta=" + delta + " d(pstart,pend)=" + Math.hypot(pend.x-pstart.x, pend.y-pstart.y));
							}
							
							// Does the bulge contain an helix?
							// If so, use drawLoop() to draw it.
							{
								int b = firstBase;
								while (b < lastBase) {
									int l = _listeBases.get(b).getElementStructure();
									if (b < l && l < lastBase) {
										// Helix present in bulge
										Point2D.Double b1pos = coords[b];
										Point2D.Double b2pos = coords[l];
										double beta = MiscGeom.angleFromVector(b2pos.x - b1pos.x, b2pos.y - b1pos.y) - Math.PI / 2 + (flipped ? Math.PI : 0);
										Point2D.Double loopCenter = new Point2D.Double((b1pos.x + b2pos.x)/2, (b1pos.y + b2pos.y)/2);
										rna.drawLoop(b,
												 l,
												 loopCenter.x,
												 loopCenter.y,
												 beta,
												 coords,
												 centers,
												 angles,
												 straightBulges);
										// If the helix is flipped, we need to compute the symmetric
										// of the whole loop.
										if (helix.isFlipped()) {
											Point2D.Double v = new Point2D.Double(Math.cos(beta), Math.sin(beta));
											symmetric(loopCenter, v, coords, b, l);
											symmetric(loopCenter, v, centers, b, l);
										}
										// Continue
										b = l;
									} else {
										b++;
									}
								}
							}
						}
					}
					
					accDelta.x += i.x*delta;
					accDelta.y += i.y*delta;
					p1.x = o.x + accDelta.x;
					p1.y = o.y + accDelta.y;
					p2.x = p1.x + j.x*RNA.BASE_PAIR_DISTANCE;
					p2.y = p1.y + j.y*RNA.BASE_PAIR_DISTANCE;
					
				} else {
					accDelta.x += li.x;
					accDelta.y += li.y;
					p1.x = o.x + accDelta.x;
					p1.y = o.y + accDelta.y;
					p2.x = p1.x + j.x*RNA.BASE_PAIR_DISTANCE;
					p2.y = p1.y + j.y*RNA.BASE_PAIR_DISTANCE;
				}
			} else {
				// First base pair
				p1.x = o.x;
				p1.y = o.y;
				p2.x = p1.x + j.x*RNA.BASE_PAIR_DISTANCE;
				p2.y = p1.y + j.y*RNA.BASE_PAIR_DISTANCE;
			}
		}
		
		Point2D.Double p1 = helixBasesPositions[0];
		Point2D.Double p2 = helixBasesPositions[n-1];
		
		if (coords != null) {
			for (int k=0; k<helixBasesPositions.length; k++) {
				coords[basesInHelixArray[k]] = helixBasesPositions[k];
			}
		}
		
		return Math.hypot(p2.x-p1.x, p2.y-p1.y);
	}
	
	
	

	/**
	 * Return ideal length to draw unpaired region from firstBase to lastBase.
	 */
	private double computeStraightLineIdealLength(int firstBase, int lastBase) {
		UnpairedLineCounts counts = countUnpairedLine(firstBase, lastBase);
		return RNA.BASE_PAIR_DISTANCE*counts.nBP + RNA.LOOP_DISTANCE*counts.nLD;
	}
	
	/**
	 * Like drawOnBezierCurve(), but on a straight line.
	 */
	private boolean drawOnStraightLine(
			int firstBase,
			int lastBase,
			Point2D.Double P0,
			Point2D.Double P3,
			Point2D.Double[] coords,
			Point2D.Double[] centers,
			boolean cancelIfNotGood) {
		
		Point2D.Double vector = new Point2D.Double(P3.x - P0.x, P3.y - P0.y); 
		double vectorNorm = Math.hypot(vector.x, vector.y);

		UnpairedLineCounts counts = countUnpairedLine(firstBase, lastBase);
		double LD = Math.max((vectorNorm - counts.nBP*RNA.BASE_PAIR_DISTANCE) / (double) counts.nLD, 0);
		
		if (cancelIfNotGood && LD < RNA.LOOP_DISTANCE*0.75) {
			// Bases would be drawn "too" near (0.75 is heuristic)
			return false;
		}
		
		double len = 0;
		{
			int b = firstBase;
			while (b <= lastBase) {
				coords[b].x = P0.x + vector.x*len/vectorNorm;
				coords[b].y = P0.y + vector.y*len/vectorNorm;
				int l = _listeBases.get(b).getElementStructure();
				if (b < l && l < lastBase) {
					len += RNA.BASE_PAIR_DISTANCE;
					b = l;
				} else {
					len += LD;
					b++;
				}
			}
		}
		
		return true;
	}
	
	


	/**
	 * A Bezier curve can be defined by four points,
	 * see http://en.wikipedia.org/wiki/Bezier_curve#Cubic_B.C3.A9zier_curves
	 * Here we give this four points and an array of bases indexes
	 * (which must be indexes in this RNA sequence) which will be moved
	 * to be on the Bezier curve.
	 * The bases positions are not changed in fact, instead the coords and
	 * centers arrays are modified.
	 * If cancelIfNotGood, draw only if it would look good,
	 * otherwise just return false.
	 */
	private boolean drawOnBezierCurve(
			int firstBase,
			int lastBase,
			Point2D.Double P0,
			Point2D.Double P1,
			Point2D.Double P2,
			Point2D.Double P3,
			Point2D.Double[] coords,
			Point2D.Double[] centers,
			boolean cancelIfNotGood) {
		
		UnpairedLineCounts counts = countUnpairedLine(firstBase, lastBase);
		
		// Draw the bases of the sequence along a Bezier curve
		int n = counts.total;
		
		// We choose to approximate the Bezier curve by 10*n straight lines.
		CubicBezierCurve bezier = new CubicBezierCurve(P0, P1, P2, P3, 10*n);
		double curveLength = bezier.getApproxCurveLength();
		
		
		double LD = Math.max((curveLength - counts.nBP*RNA.BASE_PAIR_DISTANCE) / (double) counts.nLD, 0);
		
		if (cancelIfNotGood && LD < RNA.LOOP_DISTANCE*0.75) {
			// Bases would be drawn "too" near (0.75 is heuristic)
			return false;
		}
		
		double[] t = new double[n+1];

		{
			double len = 0;
			int k = 0;
			int b = firstBase;
			while (b <= lastBase) {
				t[k] = len;
				k++;
				
				int l = _listeBases.get(b).getElementStructure();
				if (b < l && l < lastBase) {
					len += RNA.BASE_PAIR_DISTANCE;
					b = l;
				} else {
					len += LD;
					b++;
				}
			}
		}
		
		Point2D.Double[] sequenceBasesCoords = bezier.uniformParam(t);
		
		{
			int k = 0;
			int b = firstBase;
			while (b <= lastBase) {
				coords[b] = sequenceBasesCoords[k];
				int l = _listeBases.get(b).getElementStructure();
				if (b < l && l < lastBase) {
					b = l;
				} else {
					b++;
				}
				k++;
			}
		}

		return true;
	}
	
	
	
	private void drawOnEllipse(
			int firstBase,
			int lastBase,
			Point2D.Double P0,
			Point2D.Double P3,
			Point2D.Double[] coords,
			Point2D.Double[] centers,
			boolean reverse) {		

		UnpairedLineCounts counts = countUnpairedLine(firstBase, lastBase);
		double halfEllipseLength = RNA.BASE_PAIR_DISTANCE*counts.nBP + RNA.LOOP_DISTANCE*counts.nLD;
		double fullEllipseLength = halfEllipseLength*2;
		
		double axisA = P0.distance(P3)/2;
		double axisB = ComputeEllipseAxis.computeEllipseAxis(axisA, fullEllipseLength);
		
		if (axisB == 0) {
			// Ellipse is in fact a line, and it is much faster to draw it as such.
			drawOnStraightLine(firstBase, lastBase, P0, P3, coords, centers, false);
			return;
		}
		
		//System.out.println("Ellipse a=" + axisA + " b=" + axisB);
		
		int n = counts.total;
		
		// We choose to approximate the Bezier curve by 10*n straight lines.
		HalfEllipse curve = new HalfEllipse(axisA, axisB, 10*n);
		double curveLength = curve.getApproxCurveLength();
		
		
		double LD = Math.max((curveLength - counts.nBP*RNA.BASE_PAIR_DISTANCE) / (double) counts.nLD, 0);
		
		double[] t = new double[n+1];

		{
			double len = 0;
			int k = 0;
			int b = firstBase;
			while (b <= lastBase) {
				t[k] = len;
				k++;
				
				int l = _listeBases.get(b).getElementStructure();
				if (b < l && l < lastBase) {
					len += RNA.BASE_PAIR_DISTANCE;
					b = l;
				} else {
					len += LD;
					b++;
				}
			}
		}
		
		// Ellipse with axis = X,Y
		Point2D.Double[] sequenceBasesCoords = curve.uniformParam(t);
		
		// Compute the symmetric half-ellipse
		if (reverse) {
			AffineTransform tranform1 = new AffineTransform();
			tranform1.scale(1, -1);
			tranform1.transform(sequenceBasesCoords, 0, sequenceBasesCoords, 0, sequenceBasesCoords.length);
		}

		// Translate + rotate such that ellipse is at the right place
		AffineTransform tranform = HalfEllipse.matchAxisA(P0, P3);
		tranform.transform(sequenceBasesCoords, 0, sequenceBasesCoords, 0, sequenceBasesCoords.length);
		
		{
			int k = 0;
			int b = firstBase;
			while (b <= lastBase) {
				coords[b] = sequenceBasesCoords[k];
				//coords[b] = curve.standardParam((double) k/n);
				//System.out.println(b + " " + coords[b]);
				int l = _listeBases.get(b).getElementStructure();
				if (b < l && l < lastBase) {
					b = l;
				} else {
					b++;
				}
				k++;
			}
		}

	}
	
	/**
	 * This functions draws the RNA sequence between (including)
	 * firstBase and lastBase along a curve.
	 * The sequence may contain helices.
	 * 
	 * Bezier curve:
	 * A Bezier curve can be defined by four points,
	 * see http://en.wikipedia.org/wiki/Bezier_curve#Cubic_B.C3.A9zier_curves
	 * 
	 * Straight line:
	 * If P1 and P2 are null, the bases are drawn on a straight line.
	 * 
	 * OUT: The bases positions are not changed in fact,
	 * instead the coords and centers arrays are modified.
	 * 
	 * Argument reverse says if we should reverse the direction of inserted helices.
	 */
	private void drawAlongCurve(
			int firstBase,
			int lastBase,
			Point2D.Double P0,
			Point2D.Double P1,
			Point2D.Double P2,
			Point2D.Double P3,
			Point2D.Double[] coords,
			Point2D.Double[] centers,
			double[] angles,
			DrawRNATemplateCurveMethod curveMethod,
			boolean reverse,
			boolean straightBulges) {
		
		// First we find the bases which are directly on the Bezier curve
		ArrayList<Integer> alongBezierCurve = new ArrayList<Integer>();
		for (int depth=0, i=firstBase; i<=lastBase; i++) {
			int k = _listeBases.get(i).getElementStructure();
			if (k < 0 || k > lastBase || k < firstBase) {
				if (depth == 0) {
					alongBezierCurve.add(i);
				}
			} else {
				if (i < k) {
					if (depth == 0) {
						alongBezierCurve.add(i);
						alongBezierCurve.add(k);
					}
					depth++;
				} else {
					depth--;
				}
			}
		}
		// number of bases along the Bezier curve
		int n = alongBezierCurve.size();
		int[] alongBezierCurveArray = RNATemplateAlign.intArrayFromList(alongBezierCurve);
		if (n > 0) {
			if (curveMethod == DrawRNATemplateCurveMethod.ALWAYS_REPLACE_BY_ELLIPSES) {
				drawOnEllipse(firstBase, lastBase, P0, P3, coords, centers, reverse);
			} else if (curveMethod == DrawRNATemplateCurveMethod.SMART) {
				boolean passed;
				if (P1 != null && P2 != null) {
					passed = drawOnBezierCurve(firstBase, lastBase, P0, P1, P2, P3, coords, centers, true);
				} else {
					passed = drawOnStraightLine(firstBase, lastBase, P0, P3, coords, centers, true);
				}
				if (!passed) {
					drawOnEllipse(firstBase, lastBase, P0, P3, coords, centers, reverse);
				}
			} else {
				if (P1 != null && P2 != null) {
					drawOnBezierCurve(firstBase, lastBase, P0, P1, P2, P3, coords, centers, false);
				} else {
					drawOnStraightLine(firstBase, lastBase, P0, P3, coords, centers, false);
				}
			}
		}
		// Now use the radiate algorithm to draw the helixes
		for (int k=0; k<n-1; k++) {
			int b1 = alongBezierCurveArray[k];
			int b2 = alongBezierCurveArray[k+1];
			if (_listeBases.get(b1).getElementStructure() == b2) {
				Point2D.Double b1pos = coords[b1];
				Point2D.Double b2pos = coords[b2];
				double alpha = MiscGeom.angleFromVector(b2pos.x - b1pos.x, b2pos.y - b1pos.y);
				rna.drawLoop(b1,
						 b2,
						 (b1pos.x + b2pos.x)/2,
						 (b1pos.y + b2pos.y)/2,
						 alpha - Math.PI / 2,
						 coords,
						 centers,
						 angles,
						 straightBulges);
				if (reverse) {
					Point2D.Double symAxisVect = new Point2D.Double(coords[b2].x - coords[b1].x, coords[b2].y - coords[b1].y); 
					symmetric(coords[b1], symAxisVect, coords, b1, b2);
					symmetric(coords[b1], symAxisVect, centers, b1, b2);
				}
			}
		}	
	}
	
	
	/**
	 * Compute the symmetric of all the points from first to last in the points array
	 * relative to the line that goes through p and has director vector v.
	 * The array is modified in-place.
	 */
	private static void symmetric(
			Point2D.Double p,
			Point2D.Double v,
			Point2D.Double[] points,
			int first,
			int last) {
		// ||v||^2
		double lv = v.x*v.x + v.y*v.y;
		for (int i=first; i<=last; i++) {
			// A is the coordinates of points[i] after moving the origin at p
			Point2D.Double A = new Point2D.Double(points[i].x - p.x, points[i].y - p.y);
			// Symmetric of A
			Point2D.Double B = new Point2D.Double(
					-(A.x*v.y*v.y - 2*A.y*v.x*v.y - A.x*v.x*v.x) / lv,
					 (A.y*v.y*v.y + 2*A.x*v.x*v.y - A.y*v.x*v.x) / lv);
			// Change the origin back
			points[i].x = B.x + p.x;
			points[i].y = B.y + p.y;
		}
	}
	
	private void computeHelixTranslations(
			Tree<RNANodeValueTemplate> tree, // IN
			Map<RNATemplateHelix, Point2D.Double> translateVectors, // OUT (must be initialized)
			RNATemplateMapping mapping,      // IN
			Point2D.Double parentDeltaVector, // IN
			boolean straightBulges
	) {
		RNANodeValueTemplate nvt = tree.getValue();
		Point2D.Double newDeltaVector = parentDeltaVector;
		if (nvt instanceof RNANodeValueTemplateBasePair) {
			RNATemplateHelix helix = ((RNANodeValueTemplateBasePair) nvt).getHelix();
			if (! translateVectors.containsKey(helix)) {
				translateVectors.put(helix, parentDeltaVector);
				int[] basesInHelixArray;
				if (mapping.getAncestor(helix) != null) {
					basesInHelixArray = RNATemplateAlign.intArrayFromList(mapping.getAncestor(helix));
					Arrays.sort(basesInHelixArray);
				} else {
					basesInHelixArray = new int[0];
				}
				Point2D.Double helixDeltaVector = computeLengthIncreaseDelta(basesInHelixArray, helix, straightBulges);
				newDeltaVector = new Point2D.Double(parentDeltaVector.x+helixDeltaVector.x, parentDeltaVector.y+helixDeltaVector.y);
			} 
		}
		for (Tree<RNANodeValueTemplate> subtree: tree.getChildren()) {
			computeHelixTranslations(subtree, translateVectors, mapping, newDeltaVector, straightBulges);
		}
	}
	
	private Map<RNATemplateHelix, Point2D.Double> computeHelixTranslations(
			Tree<RNANodeValueTemplate> tree, // IN
			RNATemplateMapping mapping,       // IN
			boolean straightBulges
	) {
		Map<RNATemplateHelix, Point2D.Double> translateVectors = new HashMap<RNATemplateHelix, Point2D.Double>();
		computeHelixTranslations(tree, translateVectors, mapping, new Point2D.Double(0,0), straightBulges);
		return translateVectors;
	}
}
