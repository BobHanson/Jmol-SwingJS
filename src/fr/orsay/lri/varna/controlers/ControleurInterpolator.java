/*
 VARNA is a tool for the automated drawing, visualization and annotation of the secondary structure of RNA, designed as a companion software for web servers and databases.
 Copyright (C) 2008  Kevin Darty, Alain Denise and Yann Ponty.
 electronic mail : Yann.Ponty@lri.fr
 paper mail : LRI, bat 490 Université Paris-Sud 91405 Orsay Cedex France

 This file is part of VARNA version 3.1.
 VARNA version 3.1 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 VARNA version 3.1 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with VARNA version 3.1.
 If not, see http://www.gnu.org/licenses.
 */
package fr.orsay.lri.varna.controlers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.Timer;

import fr.orsay.lri.varna.VARNAPanel;
import fr.orsay.lri.varna.exceptions.MappingException;
import fr.orsay.lri.varna.models.VARNAConfig;
import fr.orsay.lri.varna.models.rna.Mapping;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.RNA;

public class ControleurInterpolator extends Thread implements ActionListener {

	private static final int START = 0;
	private static final int LOOP = 1;
	private static final int END = 2;

	VARNAPanel _vpn;
	protected int _numSteps = 25;  // BH SwingJS
	private long _timeDelay = /** @j2sNative 2 || */15;
	private boolean _running = false;
	Targets _d = new Targets();
	protected int _step;   // BH SwingJS
	protected boolean _firstHalf;  // BH SwingJS

	public ControleurInterpolator(VARNAPanel vpn) {
		_vpn = vpn;
	}

	public synchronized void addTarget(RNA target, Mapping mapping) {
		addTarget(target, null, mapping);
	}

	
	public synchronized void addTarget(RNA target, VARNAConfig conf, Mapping mapping) {
		_d.add(new TargetsHolder(target,conf,mapping));
	}

	public synchronized void addTarget(RNA target) {
		addTarget(target, null, Mapping.DefaultOutermostMapping(_vpn.getRNA()
				.get_listeBases().size(), target.get_listeBases().size()));
	}
	
	public boolean isInterpolationInProgress()
	{
		return _running;
	}

	private Point2D.Double computeDestination(Point2D pli, Point2D pri,
			Point2D pi, int i, int n, Point2D plf, Point2D prf) {
		Point2D.Double plm = new Point2D.Double(
				(pli.getX() + plf.getX()) / 2.0,
				(pli.getY() + plf.getY()) / 2.0);
		Point2D.Double prm = new Point2D.Double(
				(pri.getX() + prf.getX()) / 2.0,
				(pri.getY() + prf.getY()) / 2.0);
		Point2D.Double pm = new Point2D.Double(((n - i) * plm.getX() + i
				* prm.getX())
				/ n, ((n - i) * plm.getY() + i * prm.getY()) / n);
		Point2D.Double v = new Point2D.Double(pm.getX() - pi.getX(), pm.getY()
				- pi.getY());
		Point2D.Double pf = new Point2D.Double(pi.getX() + 2.0 * v.getX(), pi
				.getY()
				+ 2.0 * v.getY());
		return pf;
	}

	private Vector<Vector<Integer>> clusterIndices(int numIndices,
			int[] mappedIndices) throws MappingException {
		int[] indices = new int[numIndices];
		for (int i = 0; i < numIndices; i++) {
			indices[i] = i;
		}
		return clusterIndices(indices, mappedIndices);
	}

	/**
	 * Builds and returns an interval array, alternating unmatched regions and
	 * matched indices. Namely, returns a vector of vector such that the vectors
	 * found at odd positions contain those indices that are NOT associated with
	 * any other base in the current mapping. On the other hand, vectors found
	 * at even positions contain only one mapped index. <br/>
	 * Ex: If indices=<code>[1,2,3,4,5]</code> and mappedIndices=
	 * <code>[2,5]</code> then the function will return
	 * <code>[[1],[2],[3,4],[5],[]]</code>.
	 * 
	 * @param indices
	 *            The total list of indices
	 * @param mappedIndices
	 *            Matched indices, should be a subset of <code>indices</code>
	 * @return A clustered array
	 * @throws MappingException
	 *             If one of the parameters is an empty array
	 */
	private Vector<Vector<Integer>> clusterIndices(int[] indices,
			int[] mappedIndices) throws MappingException {
		if ((mappedIndices.length == 0) || (indices.length == 0)) {
			throw new MappingException(
					"Mapping Error: Cannot cluster indices in an empty mapping");
		}
		Vector<Vector<Integer>> res = new Vector<Vector<Integer>>();

		Arrays.sort(indices);
		Arrays.sort(mappedIndices);
		int i, j = 0, k;
		Vector<Integer> tmp = new Vector<Integer>();
		for (i = 0; (i < indices.length) && (j < mappedIndices.length); i++) {
			if (indices[i] == mappedIndices[j]) {
				res.add(tmp);
				tmp = new Vector<Integer>();
				tmp.add(indices[i]);
				res.add(tmp);
				tmp = new Vector<Integer>();
				j++;
			} else {
				tmp.add(indices[i]);
			}
		}
		k = i;
		for (i = k; (i < indices.length); i++) {
			tmp.add(indices[i]);
		}
		res.add(tmp);
		return res;
	}
	
	

	public void run() {
		while (true)
		{
			TargetsHolder d = _d.get();
			_running = true;
			try{
			nextTarget(d.target,d.conf,d.mapping);
			}
			catch(Exception e)
			{
				System.err.println(e);
				e.printStackTrace();
			}
			_running = false;
			/**
			 * @j2sNative
			 * 
			 * break;
			 */
		}
		
	}
	
	/**
	 * Compute the centroid of the RNA bases that have their indexes
	 * in the given array.
	 */
	private static Point2D.Double computeCentroid(ArrayList<ModeleBase> rnaBases, int[] indexes) {		
		double centroidX = 0, centroidY = 0;
		for (int i=0; i<indexes.length; i++) {
			int index = indexes[i];
			Point2D coords = rnaBases.get(index).getCoords();
			centroidX += coords.getX();
			centroidY += coords.getY();
		}
		centroidX /= indexes.length;
		centroidY /= indexes.length;
		
		return new Point2D.Double(centroidX, centroidY);
	}
	
	/**
	 * The purpose of this class is to compute the rotation that minimizes the
	 * RMSD between the bases of the first RNA and the bases of the second RNA.
	 * 
	 * @author Raphael Champeimont
	 */
	private static class MinimizeRMSD {
		private double[] X1, X2, Y1, Y2;
		
		public MinimizeRMSD(double[] X1, double[] Y1, double[] X2, double[] Y2) {
			this.X1 = X1;
			this.X2 = X2;
			this.Y1 = Y1;
			this.Y2 = Y2;
		}
		
		/**
		 * A function such that minimizing it is equivalent to
		 * minimize the RMSD between between the two arrays of vectors,
		 * supposing we rotate the points in [X2,Y2] by theta.
		 */
		private double f(double theta) {
		    double cos_theta = Math.cos(theta);
		    double sin_theta = Math.sin(theta);
		    int n = X1.length;
		    double sum = 0;
		    for (int i=0; i<n; i++) {
		        double dsx = X2[i]*cos_theta - Y2[i]*sin_theta - X1[i];
		        double dsy = X2[i]*sin_theta + Y2[i]*cos_theta - Y1[i];
		        sum = sum + dsx*dsx + dsy*dsy;
		    }
		    return sum;
		}
		
		/**
		 * d/dtheta f(theta)
		 */
		private double fprime(double theta) {
		    double cos_theta = Math.cos(theta);
		    double sin_theta = Math.sin(theta);
		    int n = X1.length;
		    double sum = 0;
		    for (int i=0; i<n; i++) {
		        sum = sum
		            + (X1[i]*X2[i] + Y1[i]*Y2[i]) * sin_theta
		            + (X1[i]*Y2[i] - X2[i]*Y1[i]) * cos_theta;
		    }
		    return sum;
		}
		
		/**
		 * d^2/dtheta^2 f(theta)
		 */
		private double fsecond(double theta) {
		    double cos_theta = Math.cos(theta);
		    double sin_theta = Math.sin(theta);
		    int n = X1.length;
		    double sum = 0;
		    for (int i=0; i<n; i++) {
		        sum = sum
		            + (X1[i]*X2[i] + Y1[i]*Y2[i]) * cos_theta
		            + (X2[i]*Y1[i] - X1[i]*Y2[i]) * sin_theta;
		    }
		    return sum;
		}


		/**
		 * Find the theta that minimizes f(theta).
		 * We use Newton's method.
		 */
		public  double computeOptimalTheta() {
			final double epsilon = 1E-5;
			double x_n = 0;
			int numsteps = 0;
			// In practice the number of steps to reach precision 1E-5 is smaller that
			// 10 most of the time.
			final int maxnumsteps = 100;
			double x_n_plus_1;
			double result;
			while (true) {
			    numsteps = numsteps + 1;
			    double d = fsecond(x_n);
			    if (d == 0) {
			        // if f''(x_n) is 0 we cannot divide by it,
			        // so we move a little.
			        x_n_plus_1 = x_n + epsilon * (Math.random() - 0.5);
			    } else {
			        x_n_plus_1 = x_n - fprime(x_n)/fsecond(x_n);
			        if (Math.abs(x_n_plus_1 - x_n) < epsilon) {
			             result = x_n_plus_1;
			             break;
			        }
			    }
			    if (numsteps >= maxnumsteps) {
			        // If things go bad (for example f''(x) keeps being 0)
			        // we need to give up after what we consider to be too many steps.
			        // In practice this can happen only in pathological cases
			        // like f being constant, which is very unlikely.
			        result = x_n_plus_1;
			        break;
				}
			    x_n = x_n_plus_1;
			}

			// We now have either found the min or the max at x = result.
			// If we have the max at x we know the min is at x+pi.
			if (f(result + Math.PI) < f(result)) {
			    result = result + Math.PI;
			}
			
			return result;
		}


	}
	
	
	/**
	 * We suppose we have two lists of points. The coordinates of the first
	 * list of points are X1 and Y1, and X2 and Y2 for the second list.
	 * We suppose that the centroids of [X1,Y1] and [X2,Y2] are both (0,0).
	 * This function computes the rotation to apply to the second set of
	 * points such that the RMSD (Root mean square deviation) between them
	 * is minimum. The returned angle is in radians.
	 */
	private static double minimizeRotateRMSD(double[] X1, double[] Y1, double[] X2, double[] Y2) {
		MinimizeRMSD minimizer = new MinimizeRMSD(X1, Y1, X2, Y2);
		return minimizer.computeOptimalTheta();
	}
	
	
	/**
	 * Move rna2 using a rotation so that it would move as little as possible
	 * (in the sense that we minimize the RMSD) when transforming
	 * it to rna1 using the given mapping.
	 */
	public static void moveNearOtherRNA(RNA rna1, RNA rna2, Mapping mapping) {
		int[] rna1MappedElems = mapping.getSourceElems();
		int[] rna2MappedElems = mapping.getTargetElems();
		ArrayList<ModeleBase> rna1Bases = rna1.get_listeBases();
		ArrayList<ModeleBase> rna2Bases = rna2.get_listeBases();
		int n = rna1MappedElems.length;
		
		// If there is less than 2 points, it is useless to rotate the RNA.
		if (n < 2) return;
		
		// We can now assume that n >= 2.
		
		// Compute the centroids of both RNAs
		Point2D.Double rna1MappedElemsCentroid = computeCentroid(rna1Bases, rna1MappedElems);
		Point2D.Double rna2MappedElemsCentroid = computeCentroid(rna2Bases, rna2MappedElems);

		// Compute the optimal rotation
		// We first compute coordinates for both RNAs, changing the origins
		// to be the centroids.
		double[] X1 = new double[rna1MappedElems.length];
		double[] Y1 = new double[rna1MappedElems.length];
		double[] X2 = new double[rna2MappedElems.length];
		double[] Y2 = new double[rna2MappedElems.length];
		for (int i=0; i<rna1MappedElems.length; i++) {
			int base1Index = rna1MappedElems[i];
			Point2D.Double coords1 = rna1Bases.get(base1Index).getCoords();
			X1[i] = coords1.x - rna1MappedElemsCentroid.x;
			Y1[i] = coords1.y - rna1MappedElemsCentroid.y;
			Point2D.Double coords2 = rna2Bases.get(mapping.getPartner(base1Index)).getCoords();
			X2[i] = coords2.x - rna2MappedElemsCentroid.x;
			Y2[i] = coords2.y - rna2MappedElemsCentroid.y;
		}
		
		// Compute the optimal rotation angle theta
		double theta = minimizeRotateRMSD(X1, Y1, X2, Y2);
		
		// Rotate RNA2
		rna2.globalRotation(theta * 180.0 / Math.PI);
	}

	public void nextTarget(RNA _target, VARNAConfig _conf, Mapping _mapping) {
		nextTarget(_target, _conf, _mapping, false);
	}
	
	Runnable _loop, _end;
	
	/**
	 * The argument moveTarget specifies whether the RNA _target should
	 * be rotated so that bases move as little as possible when switching
	 * from the current RNA to _target using the animation.
	 * Note that this will modify the _target object directly.
	 */
	public void nextTarget(final RNA _target, final VARNAConfig _conf, Mapping _mapping, boolean moveTarget)
	{
		_end = new Runnable() {

			@Override
			public void run() {
				_vpn.showRNA(_target);
				_vpn.repaint();
			}
			
		};
		
		try {
			final RNA source = _vpn.getRNA();
			
			if (moveTarget) moveNearOtherRNA(source, _target, _mapping);

			if (source.getSize()!=0&&_target.getSize()!=0)
			{
			ArrayList<ModeleBase> currBases = source.get_listeBases();
			ArrayList<ModeleBase> destBases = _target.get_listeBases();
			Vector<Vector<Integer>> intArrSource = new Vector<Vector<Integer>>();
			Vector<Vector<Integer>> intArrTarget = new Vector<Vector<Integer>>();
				// Building interval arrays
				intArrSource = clusterIndices(currBases.size(), _mapping
						.getSourceElems());
				intArrTarget = clusterIndices(destBases.size(), _mapping
						.getTargetElems());

			// Duplicating source and target coordinates
			final Point2D.Double[] initPosSource = new Point2D.Double[currBases
					.size()];
			final Point2D.Double[] finalPosTarget = new Point2D.Double[destBases
					.size()];

			for (int i = 0; i < currBases.size(); i++) {
				Point2D tmp = currBases.get(i).getCoords();
				initPosSource[i] = new Point2D.Double(tmp.getX(), tmp.getY());
			}
			for (int i = 0; i < destBases.size(); i++) {
				Point2D tmp = destBases.get(i).getCoords();
				finalPosTarget[i] = new Point2D.Double(tmp.getX(), tmp.getY());
			}

			/**
			 * Assigning final (Source) and initial (Target) coordinates
			 */
			final Point2D.Double[] finalPosSource = new Point2D.Double[initPosSource.length];
			final Point2D.Double[] initPosTarget = new Point2D.Double[finalPosTarget.length];
			// Final position of source model
			for (int i = 0; i < finalPosSource.length; i++) {
				if (_mapping.getPartner(i) != Mapping.UNKNOWN) {
					Point2D dest;
					dest = finalPosTarget[_mapping.getPartner(i)];
					finalPosSource[i] = new Point2D.Double(dest.getX(), dest
							.getY());
				}
			}

			for (int i = 0; i < intArrSource.size(); i += 2) {
				int matchedNeighborLeft, matchedNeighborRight;
				if (i == 0) {
					matchedNeighborLeft = intArrSource.get(1).get(0);
					matchedNeighborRight = intArrSource.get(1).get(0);
				} else if (i == intArrSource.size() - 1) {
					matchedNeighborLeft = intArrSource.get(
							intArrSource.size() - 2).get(0);
					matchedNeighborRight = intArrSource.get(
							intArrSource.size() - 2).get(0);
				} else {
					matchedNeighborLeft = intArrSource.get(i - 1).get(0);
					matchedNeighborRight = intArrSource.get(i + 1).get(0);
				}
				Vector<Integer> v = intArrSource.get(i);
				for (int j = 0; j < v.size(); j++) {
					int index = v.get(j);
					finalPosSource[index] = computeDestination(
							initPosSource[matchedNeighborLeft],
							initPosSource[matchedNeighborRight],
							initPosSource[index], j + 1, v.size() + 1,
							finalPosSource[matchedNeighborLeft],
							finalPosSource[matchedNeighborRight]);
				}
			}
			for (int i = 0; i < initPosTarget.length; i++) {
				if (_mapping.getAncestor(i) != Mapping.UNKNOWN) {
					Point2D dest;
					dest = initPosSource[_mapping.getAncestor(i)];
					initPosTarget[i] = new Point2D.Double(dest.getX(), dest.getY());
				}
			}
			for (int i = 0; i < intArrTarget.size(); i += 2) {
				int matchedNeighborLeft, matchedNeighborRight;
				if (i == 0) {
					matchedNeighborLeft = intArrTarget.get(1).get(0);
					matchedNeighborRight = intArrTarget.get(1).get(0);
				} else if (i == intArrTarget.size() - 1) {
					matchedNeighborLeft = intArrTarget.get(
							intArrTarget.size() - 2).get(0);
					matchedNeighborRight = intArrTarget.get(
							intArrTarget.size() - 2).get(0);
				} else {
					matchedNeighborLeft = intArrTarget.get(i - 1).get(0);
					matchedNeighborRight = intArrTarget.get(i + 1).get(0);
				}
				Vector<Integer> v = intArrTarget.get(i);
				for (int j = 0; j < v.size(); j++) {
					int index = v.get(j);
					initPosTarget[index] = computeDestination(
							finalPosTarget[matchedNeighborLeft],
							finalPosTarget[matchedNeighborRight],
							finalPosTarget[index], j + 1, v.size() + 1,
							initPosTarget[matchedNeighborLeft],
							initPosTarget[matchedNeighborRight]);
				}
			}

			mode = START;
			_loop = new Runnable(){

				@Override
				public void run() {
					int i = _step;
					RNA current = (_firstHalf ? source : _target);
					if (i == _numSteps / 2) {
						_vpn.showRNA(_target);
						current = _target;
						_firstHalf = false;
						if (_conf!=null)
						{_vpn.setConfig(_conf);}
						
						for (int j = 0; j < initPosSource.length; j++) {
							source.setCoord(j, initPosSource[j]);
						}
					}
					ArrayList<ModeleBase> currBases = current.get_listeBases();
					for (int j = 0; j < currBases.size(); j++) {
						ModeleBase m = currBases.get(j);
						Point2D mpc, mnc;
						if (_firstHalf) {
							mpc = initPosSource[j];
							mnc = finalPosSource[j];
						} else {
							mpc = initPosTarget[j];
							mnc = finalPosTarget[j];
						}
						m.setCoords(new Point2D.Double(((_numSteps - 1 - i)
								* mpc.getX() + (i) * mnc.getX())
								/ (_numSteps - 1), ((_numSteps - 1 - i)
								* mpc.getY() + i * mnc.getY())
								/ (_numSteps - 1)));
					}
					_vpn.repaint();
				}
				
			};
			actionPerformed(null);
			} else {
				_end.run();
			}
		} catch (MappingException e) {
			e.printStackTrace();
			_end.run();
		}catch (Exception e) {
			e.printStackTrace();
			_end.run();
		}
		

	}

	private class TargetsHolder
	{
		public RNA target;
		public VARNAConfig conf;
		public Mapping mapping;
		public TargetsHolder(RNA t, VARNAConfig c, Mapping m)
		{
			target = t;
			conf = c;
			mapping = m;
		}
	}
	
	private class Targets
	{
		LinkedList<TargetsHolder> _d = new LinkedList<TargetsHolder>();
		public Targets() {
			// BH j2s SwingJS added only to remove Eclipse warning
		}
		public synchronized void add(TargetsHolder d)
		{
			_d.addLast(d);
			
			@SuppressWarnings("unused")
			Runnable interpolator = ControleurInterpolator.this;
			/**
			 * BH SwingJS no notify()
			 * @j2sNative 
			 * 
			 * interpolator.run();
			 * 
			 */
			{
			notify();
			}
		}

		public synchronized TargetsHolder get() {

			/**
			 * BH SwingJS no wait()
			 * 
			 * @j2sNative
			 * 
			 * 
			 */
			{
				while (_d.size() == 0) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			TargetsHolder x = _d.getLast();
			_d.clear();
			return x;
		}
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		runAnimation();
	}

	private int mode;

	private void runAnimation() {
		switch (mode) {
		case START:
			_firstHalf = true;
			_step = 0;
			mode = LOOP;
			// Fall through
		case LOOP:
			if (_step < _numSteps) {
				_loop.run();
				++_step;
				break;
			}
			mode = END;
			// Fall through
		case END:
			_end.run();
			return;
		}
		Timer t = new Timer((int) _timeDelay, this);
		t.setRepeats(false);
		t.start();
		// try {
		// for (int i = 0; i < _numSteps; i++) {
		// _step = i;
		// loop.run();
		//
		// sleep(_timeDelay);
		// }
		// end.run();
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// end.run();
		// }
	}

}
