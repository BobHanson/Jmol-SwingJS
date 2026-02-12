package fr.orsay.lri.varna.models.treealign;


/**
 * This distance is such that a substitution costs nothing.
 * 
 * @author Raphael Champeimont
 *
 */
public class ExampleDistance2 implements TreeAlignLabelDistanceSymmetric<RNANodeValue2> {
	public double f(RNANodeValue2 v1, RNANodeValue2 v2) {
		if (v1 == null) {
			if (v2 == null) {
				return 0;
			} else if (!v2.isSingleNode()) { // v2 is a list of bases
				return v2.getNodes().size();
			} else { // v2 is a single node
				return 2;
			}
		} else if (!v1.isSingleNode()) { // v1 is a list of bases
			if (v2 == null) {
				return v1.getNodes().size();
			} else if (!v2.isSingleNode()) { // v2 is a list of bases
				return Math.abs(v2.getNodes().size() - v1.getNodes().size());
			} else { // v2 is a single node
				return 2 + v1.getNodes().size();
			}
		} else { // v1 is a single node
			// all the same as when v1 == null
			if (v2 == null) {
				return 2;
			} else if (!v2.isSingleNode()) { // v2 is a list of bases
				return 2 + v2.getNodes().size();
			} else { // v2 is a single node
				return 0;
			}
		}
	}
}
