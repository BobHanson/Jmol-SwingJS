package fr.orsay.lri.varna.models.treealign;

/**
 * We use the following convention: If the node is marked by a couple (n,m)
 * these integers are stored in leftBasePosition and rightBasePosition
 * (ie. we have a pair of bases), if only
 * one value is present, it is stored in leftBasePosition and rightBasePosition
 * contains -1 (ie. we have a non-paired base).
 * The right and left nucleotides follow the same rule, but are optional,
 * and '_' is used as undefined instead of -1.
 * Note that it is part of the contract of this class that default
 * values are -1 and _.
 * 
 * @author Raphael Champeimont
 */
public class RNANodeValue implements GraphvizDrawableNodeValue {
	private int leftBasePosition = -1;
	private int rightBasePosition = -1;
	
	private String leftNucleotide = "_";
	private String rightNucleotide = "_";
	
	public enum Origin {
		BASE_PAIR_FROM_HELIX,
		BASE_FROM_HELIX_STRAND5,
		BASE_FROM_HELIX_STRAND3,
		BASE_FROM_UNPAIRED_REGION;
	}
	
	/**
	 * Used to store the origin of this base / base pair.
	 */
	private Origin origin = null;
	
	public Origin getOrigin() {
		return origin;
	}
	public void setOrigin(Origin comesFromAnHelix) {
		this.origin = comesFromAnHelix;
	}
	
	
	
	public String getLeftNucleotide() {
		return leftNucleotide;
	}
	public void setLeftNucleotide(String leftNucleotide) {
		this.leftNucleotide = leftNucleotide;
	}
	public String getRightNucleotide() {
		return rightNucleotide;
	}
	public void setRightNucleotide(String rightNucleotide) {
		this.rightNucleotide = rightNucleotide;
	}
	public int getLeftBasePosition() {
		return leftBasePosition;
	}
	public void setLeftBasePosition(int leftBasePosition) {
		this.leftBasePosition = leftBasePosition;
	}
	public int getRightBasePosition() {
		return rightBasePosition;
	}
	public void setRightBasePosition(int rightBasePosition) {
		this.rightBasePosition = rightBasePosition;
	}
	
	public String toGraphvizNodeName() {
		if (rightNucleotide.equals("_")) {
			if (leftNucleotide.equals("_")) {
				if (rightBasePosition == -1) {
					if (leftBasePosition == -1) {
						return super.toString();
					} else {
						return Integer.toString(leftBasePosition);
					}
				} else {
					return "(" + leftBasePosition + "," + rightBasePosition + ")";
				}
			} else {
				return leftNucleotide;
			}
		} else {
			return "(" + leftNucleotide + "," + rightNucleotide + ")";
		}
	}
	
	public String toString() {
		if (rightBasePosition == -1) {
			if (leftBasePosition == -1) {
				return super.toString();
			} else {
				return Integer.toString(leftBasePosition);
			}
		} else {
			return "(" + leftBasePosition + "," + rightBasePosition + ")";
		}
	}
	

}
