package fr.orsay.lri.varna.models.templates;

import java.awt.geom.Line2D;
import java.util.Arrays;

import fr.orsay.lri.varna.models.geom.LinesIntersect;
import fr.orsay.lri.varna.models.rna.RNA;

/**
 * @author Raphael Champeimont
 *
 */
public class Benchmark {
	private RNA rna;
	
	// number of backbone crossings
	public int backboneCrossings;
	// average distance between unpaired consecutive bases
	public double averageUnpairedDistance;
	// median distance between consecutive bases
	public double medianConsecutiveBaseDistance;
	// Number of consecutive bases that are too near from each other
	public int tooNearConsecutiveBases;
	// Number of consecutive bases that are too far from each other
	public int tooFarConsecutiveBases;
	
	
	
	// Parameters
	public double targetConsecutiveBaseDistance = RNA.LOOP_DISTANCE;
	// If consecutive bases are nearer that tooNearFactor * target distance, we call them too near.
	public double tooNearFactor = 0.5;
	// If consecutive bases are further that tooFarFactor * target distance, we call them too far.
	public double tooFarFactor = 2;
	
	
	
	public Benchmark(RNA rna) {
		this.rna = rna;
		computeAll();
	}
	
	private void computeAll() {
		// Compute number of backbone crossings
		{
			int n = rna.getSize();
			Line2D.Double[] lines = new Line2D.Double[n-1];
			for (int i=0; i<n-1; i++) {
				lines[i] = new Line2D.Double(rna.getCoords(i), rna.getCoords(i+1));
			}
			int intersectLines = 0;
			for (int i=0; i<n-1; i++) {
				for (int j=i+2; j<n-1; j++) {
					if (LinesIntersect.linesIntersect(lines[i], lines[j])) {
						intersectLines++;
					}
				}
			}
			backboneCrossings = intersectLines;
		}
		
		// Stats about distances between consecutive bases not both part of an helix
		{
			int n = rna.getSize();
			double sum = 0;
			int count = 0;
			for (int i=0; i<n-1; i++) {
				int indexOfAssociatedBase1 = rna.getBaseAt(i).getElementStructure();
				int indexOfAssociatedBase2 = rna.getBaseAt(i+1).getElementStructure();
				if (indexOfAssociatedBase1 != -1 || indexOfAssociatedBase2 != -1) {
					// If they are not both associated (ie. not part of an helix)
					sum += rna.getBaseAt(i).getCoords().distance(rna.getBaseAt(i+1).getCoords());
					count++;
				}
			}
			averageUnpairedDistance = sum / count;
		}
		
		// Stats about base distances
		{
			int n = rna.getSize();
			double distances[] = new double[n-1];
			for (int i=0; i<n-1; i++) {
				// If they are not both associated (ie. not part of an helix)
				distances[i] = rna.getBaseAt(i).getCoords().distance(rna.getBaseAt(i+1).getCoords());
			}
			Arrays.sort(distances);
			double median = distances[distances.length/2];
			medianConsecutiveBaseDistance = median;
			
			// We take the median as target distance, and count
			// the number of consecutive bases with a distance too different
			tooNearConsecutiveBases = 0;
			tooFarConsecutiveBases = 0;
			for (int i=0; i<n-1; i++) {
				if (distances[i] < tooNearFactor*targetConsecutiveBaseDistance) {
					tooNearConsecutiveBases++;
				}
				if (distances[i] > tooFarFactor*targetConsecutiveBaseDistance) {
					tooFarConsecutiveBases++;
				}
			}
		}
	}
	
	@SuppressWarnings("unused")
	private int percent(int a, int b) {
		return (int) Math.round((double) a / (double) b * 100.0);
	}

	public void printAll() {
		System.out.println("Benchmark:");
		System.out.println("\tBackbone crossings = " + backboneCrossings);
		System.out.println("\tAverage unpaired distance = " + averageUnpairedDistance);
		System.out.println("\tMedian of consecutive base distance = " + medianConsecutiveBaseDistance);
		System.out.println("\tNumber of too near consecutive bases = " + tooNearConsecutiveBases); // + " ie. " + percent(tooNearConsecutiveBases, rna.getSize()-1) + " %");
		System.out.println("\tNumber of too far consecutive bases = " + tooFarConsecutiveBases); // + " ie. " + percent(tooFarConsecutiveBases, rna.getSize()-1) + " %");
	}
}

