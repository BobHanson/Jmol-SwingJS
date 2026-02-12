package fr.orsay.lri.varna.models.templates;

import fr.orsay.lri.varna.models.treealign.RNANodeValue;
import fr.orsay.lri.varna.models.treealign.RNANodeValue2;
import fr.orsay.lri.varna.models.treealign.TreeAlignLabelDistanceAsymmetric;

/**
 * Distance between an RNANodeValue2 and an RNANodeValueTemplate.
 * 
 * @author Raphael Champeimont
 *
 */
public class RNANodeValue2TemplateDistance implements TreeAlignLabelDistanceAsymmetric<RNANodeValue2,RNANodeValueTemplate> {
	public double delete(RNANodeValue2 v) {
		if (v == null) {
			// deleting nothing costs nothing
			return 0;
		} else {
			if (v.isSingleNode()) {
				if (v.getNode().getRightBasePosition() < 0) {
					// delete one base (that comes from a broken base pair)
					return 1;
				} else {
					// delete a base pair
					return 2;
				}
			} else {
				// delete a sequence
				return v.getNodes().size();
			}
		}
	}
	
	public double insert(RNANodeValueTemplate v) {
		if (v == null) {
			// inserting nothing costs nothing
			return 0;
		} else {
			if (v instanceof RNANodeValueTemplateSequence) {
				return ((RNANodeValueTemplateSequence) v).getSequence().getLength();
			} else if (v instanceof RNANodeValueTemplateBrokenBasePair) {
				// insert one base
				return 1;
			} else { // this is a base pair
				// delete the base pair
				return 2;
			}
		}
	}
	
	public double f(RNANodeValue2 v1, RNANodeValueTemplate v2) {
		if (v1 == null) {
			return insert(v2);
		} else if (v2 == null) {
			return delete(v1);
		} else if (!v1.isSingleNode()) { // v1 is a sequence
			if (v2 instanceof RNANodeValueTemplateSequence) {
				// the cost is the difference between the sequence lengths
				return Math.abs(v1.getNodes().size() - ((RNANodeValueTemplateSequence) v2).getSequence().getLength());
			} else {
				// a sequence cannot be changed in something else
				return Double.POSITIVE_INFINITY;
			}
		} else if (v1.getNode().getRightBasePosition() >= 0) { // v1 is a base pair
			if (v2 instanceof RNANodeValueTemplateBasePair) {
				// ok, a base pair can be mapped to a base pair
				return 0;
			} else {
				// a base pair cannot be changed in something else
				return Double.POSITIVE_INFINITY;
			}
		} else { // v1 is a broken base pair
			if (v2 instanceof RNANodeValueTemplateBrokenBasePair) {
				RNANodeValueTemplateBrokenBasePair brokenBasePair = ((RNANodeValueTemplateBrokenBasePair) v2);
				boolean strand5onTemplateSide = brokenBasePair.getPositionInHelix() < brokenBasePair.getHelix().getLength();
				boolean strand3onTemplateSide = ! strand5onTemplateSide;
				boolean strand5onRNASide = (v1.getNode().getOrigin() == RNANodeValue.Origin.BASE_FROM_HELIX_STRAND5);
				boolean strand3onRNASide = (v1.getNode().getOrigin() == RNANodeValue.Origin.BASE_FROM_HELIX_STRAND3);
				if ((strand5onTemplateSide && strand5onRNASide)
						|| (strand3onTemplateSide && strand3onRNASide)) {
					// Ok they can be mapped together
					return 0.0;
				} else {
					// A base on a 5' strand of an helix
					// cannot be mapped to base on a 3' strand of an helix
					return Double.POSITIVE_INFINITY;
				}
			} else {
				return Double.POSITIVE_INFINITY;
			}
		}
	}
}
