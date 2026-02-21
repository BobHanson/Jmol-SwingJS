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
package fr.orsay.lri.varna.models.naView;


import java.util.ArrayList;

import fr.orsay.lri.varna.exceptions.ExceptionNAViewAlgorithm;
import fr.orsay.lri.varna.interfaces.InterfaceVARNAListener;
import fr.orsay.lri.varna.interfaces.InterfaceVARNAObservable;

public class NAView {
	private final double ANUM = 9999.0;
	private final int MAXITER = 500;

	private ArrayList<Base> bases;
	private int nbase, nregion, loop_count;

	private Loop root = new Loop();
	private ArrayList<Loop> loops;

	private ArrayList<Region> regions;

	private Radloop rlphead = new Radloop();

	private double lencut=0.8;
	private final double RADIUS_REDUCTION_FACTOR = 1.4;

	// show algorithm step by step
	private boolean debug = false;

	private double angleinc;

	private double _h;

	private ArrayList<InterfaceVARNAListener> _listeVARNAListener = new ArrayList<InterfaceVARNAListener>();

	private boolean noIterationFailureYet = true;

	double HELIX_FACTOR = 0.6;
	double BACKBONE_DISTANCE = 27;

	public int naview_xy_coordinates(ArrayList<Short> pair_table2,
			ArrayList<Double> x, ArrayList<Double> y)
			throws ExceptionNAViewAlgorithm {
		if (debug)
			System.out.println("naview_xy_coordinates");
		if (pair_table2.size() == 0)
			return 0;
		int i;
		ArrayList<Integer> pair_table = new ArrayList<Integer>(pair_table2
				.size() + 1);
		pair_table.add(pair_table2.size());

		for (int j = 0; j < pair_table2.size(); j++) {
			pair_table.add(pair_table2.get(j) + 1);
		}

		if (debug) {
			infoStructure(pair_table);
		}
		// length
		nbase = pair_table.get(0);
		bases = new ArrayList<Base>(nbase + 1);

		for (int index = 0; index < bases.size(); index++) {
			bases.add(new Base());
		}

		regions = new ArrayList<Region>();
		for (int index = 0; index < nbase + 1; index++) {
			regions.add(new Region());
		}

		read_in_bases(pair_table);

		if (debug)
			infoBasesMate();

		rlphead = null;

		find_regions();

		if (debug)
			infoRegions();

		loop_count = 0;
		loops = new ArrayList<Loop>(nbase + 1);
		for (int index = 0; index < nbase + 1; index++) {
			loops.add(new Loop());
		}

		construct_loop(0);

		if (debug)
			infoBasesExtracted();

		find_central_loop();

		if (debug)
			infoRoot();

		if (debug)
			dump_loops();

		traverse_loop(root, null);

		for (i = 0; i < nbase; i++) {
			x.add(100 + BACKBONE_DISTANCE * bases.get(i + 1).getX());
			y.add(100 + BACKBONE_DISTANCE * bases.get(i + 1).getY());
		}

		return nbase;
	}

	private void infoStructure(ArrayList<Integer> pair_table) {
		System.out.println("structure:");
		for (int j = 0; j < pair_table.size(); j++) {
			System.out.print("#" + j + ":" + pair_table.get(j) + "\t");
			if (j % 10 == 0)
				System.out.println();
		}
		System.out.println();
	}

	private void infoBasesMate() {
		System.out.println("Bases mate:");
		for (int index = 0; index < bases.size(); index++) {
			System.out.print("#" + index + ":" + bases.get(index).getMate()
					+ "\t");
			if (index % 10 == 0)
				System.out.println();
		}
		System.out.println();
	}

	private void infoRegions() {
		System.out.println("regions:");
		for (int index = 0; index < regions.size(); index++) {
			System.out.print("(" + regions.get(index).getStart1() + ","
					+ regions.get(index).getStart2() + ";"
					+ regions.get(index).getEnd1() + ","
					+ regions.get(index).getEnd2() + ")\t\t");
			if (index % 5 == 0)
				System.out.println();
		}
		System.out.println();
	}

	private void infoBasesExtracted() {
		System.out.println("Bases extracted:");
		for (int index = 0; index < bases.size(); index++) {
			System.out.print("i=" + index + ":"
					+ bases.get(index).isExtracted() + "\t");
			if (index % 5 == 0)
				System.out.println();
		}
		System.out.println();
	}

	private void infoRoot() {
		System.out.println("root" + root.getNconnection() + ";"
				+ root.getNumber());
		System.out.println("\troot : ");
		System.out.println("\tdepth=" + root.getDepth());
		System.out.println("\tmark=" + root.isMark());
		System.out.println("\tnumber=" + root.getNumber());
		System.out.println("\tradius=" + root.getRadius());
		System.out.println("\tx=" + root.getX());
		System.out.println("\ty=" + root.getY());
		System.out.println("\tnconnection=" + root.getNconnection());
	}

	private void read_in_bases(ArrayList<Integer> pair_table) {
		if (debug)
			System.out.println("read_in_bases");

		int i, npairs;

		// Set up an origin.
		bases.add(new Base());
		bases.get(0).setMate(0);
		bases.get(0).setExtracted(false);
		bases.get(0).setX(ANUM);
		bases.get(0).setY(ANUM);

		for (npairs = 0, i = 1; i <= nbase; i++) {
			bases.add(new Base());
			bases.get(i).setExtracted(false);
			bases.get(i).setX(ANUM);
			bases.get(i).setY(ANUM);
			bases.get(i).setMate(pair_table.get(i));
			if ((int) pair_table.get(i) > i)
				npairs++;
		}
		// must have at least 1 pair to avoid segfault
		if (npairs == 0) {
			bases.get(1).setMate(nbase);
			bases.get(nbase).setMate(1);
		}
	}

	/**
	 * Identifies the regions in the structure.
	 */
	private void find_regions()

	{
		if (debug)
			System.out.println("find_regions");
		int i, mate, nb1;
		nb1 = nbase + 1;
		ArrayList<Boolean> mark = new ArrayList<Boolean>(nb1);
		for (i = 0; i < nb1; i++)
			mark.add(false);
		nregion = 0;
		for (i = 0; i <= nbase; i++) {
			if ((mate = bases.get(i).getMate()) != 0 && !mark.get(i)) {
				regions.get(nregion).setStart1(i);
				regions.get(nregion).setEnd2(mate);
				mark.set(i, true);
				mark.set(mate, true);
				bases.get(i).setRegion(regions.get(nregion));
				bases.get(mate).setRegion(regions.get(nregion));
				for (i++, mate--; i < mate && bases.get(i).getMate() == mate; i++, mate--) {
					mark.set(mate, true);
					mark.set(i, true);
					bases.get(i).setRegion(regions.get(nregion));
					bases.get(mate).setRegion(regions.get(nregion));
				}
				regions.get(nregion).setEnd1(--i);
				regions.get(nregion).setStart2(mate + 1);
				if (debug) {
					if (nregion == 0)
						System.out.printf("\nRegions are:\n");
					System.out.printf(
							"Region %d is %d-%d and %d-%d with gap of %d.\n",
							nregion + 1, regions.get(nregion).getStart1(),
							regions.get(nregion).getEnd1(), regions
									.get(nregion).getStart2(), regions.get(
									nregion).getEnd2(), regions.get(nregion)
									.getStart2()
									- regions.get(nregion).getEnd1() + 1);
				}
				nregion++;
			}
		}
	}

	/**
	 * Starting at residue ibase, recursively constructs the loop containing
	 * said base and all deeper bases.
	 * 
	 * @throws ExceptionNAViewAlgorithm
	 */
	private Loop construct_loop(int ibase) throws ExceptionNAViewAlgorithm {
		if (debug)
			System.out.println("construct_loop");
		int i, mate;
		Loop retloop = new Loop(), lp = new Loop();
		Connection cp = new Connection();
		Region rp = new Region();
		Radloop rlp = new Radloop();
		retloop = loops.get(loop_count++);
		retloop.setNconnection(0);
		//System.out.println(""+ibase+" "+nbase);
		//ArrayList<Connection> a = new ArrayList<Connection>(nbase + 1); 
		//retloop.setConnections(a);
//		for (int index = 0; index < nbase + 1; index++)
//			retloop.getConnections().add(new Connection());
		//for (int index = 0; index < nbase + 1; index++)
		//	retloop.addConnection(index,new Connection());
		retloop.setDepth(0);
		retloop.setNumber(loop_count);
		retloop.setRadius(0.0);
		for (rlp = rlphead; rlp != null; rlp = rlp.getNext())
			if (rlp.getLoopnumber() == loop_count)
				retloop.setRadius(rlp.getRadius());
		i = ibase;
		do {
			if ((mate = bases.get(i).getMate()) != 0) {
				rp = bases.get(i).getRegion();
				if (!bases.get(rp.getStart1()).isExtracted()) {
					if (i == rp.getStart1()) {
						bases.get(rp.getStart1()).setExtracted(true);
						bases.get(rp.getEnd1()).setExtracted(true);
						bases.get(rp.getStart2()).setExtracted(true);
						bases.get(rp.getEnd2()).setExtracted(true);
						lp = construct_loop(rp.getEnd1() < nbase ? rp.getEnd1() + 1
								: 0);
					} else if (i == rp.getStart2()) {
						bases.get(rp.getStart2()).setExtracted(true);
						bases.get(rp.getEnd2()).setExtracted(true);
						bases.get(rp.getStart1()).setExtracted(true);
						bases.get(rp.getEnd1()).setExtracted(true);
						lp = construct_loop(rp.getEnd2() < nbase ? rp.getEnd2() + 1
								: 0);
					} else {
						throw new ExceptionNAViewAlgorithm(
								"naview:Error detected in construct_loop. i = "
										+ i + " not found in region table.\n");
					}
					retloop.setNconnection(retloop.getNconnection() + 1);
					cp = new Connection();
					retloop.setConnection(retloop.getNconnection() - 1,	cp);
					retloop.setConnection(retloop.getNconnection(), null);
					cp.setLoop(lp);
					cp.setRegion(rp);
					if (i == rp.getStart1()) {
						cp.setStart(rp.getStart1());
						cp.setEnd(rp.getEnd2());
					} else {
						cp.setStart(rp.getStart2());
						cp.setEnd(rp.getEnd1());
					}
					cp.setExtruded(false);
					cp.setBroken(false);
					lp.setNconnection(lp.getNconnection() + 1);
					cp = new Connection();
					lp.setConnection(lp.getNconnection() - 1, cp);
					lp.setConnection(lp.getNconnection(), null);
					cp.setLoop(retloop);
					cp.setRegion(rp);
					if (i == rp.getStart1()) {
						cp.setStart(rp.getStart2());
						cp.setEnd(rp.getEnd1());
					} else {
						cp.setStart(rp.getStart1());
						cp.setEnd(rp.getEnd2());
					}
					cp.setExtruded(false);
					cp.setBroken(false);
				}
				i = mate;
			}
			if (++i > nbase)
				i = 0;
		} while (i != ibase);
		return retloop;
	}

	/**
	 * Displays all the loops.
	 */
	private void dump_loops() {
		System.out.println("dump_loops");
		int il, ilp, irp;
		Loop lp;
		Connection cp;

		System.out.printf("\nRoot loop is #%d\n", loops.indexOf(root) + 1);
		for (il = 0; il < loop_count; il++) {
			lp = loops.get(il);
			System.out.printf("Loop %d has %d connections:\n", il + 1, lp
					.getNconnection());
			for (int i = 0; (cp = lp.getConnection(i)) != null; i++) {
				ilp = (loops.indexOf(cp.getLoop())) + 1;
				irp = (regions.indexOf(cp.getRegion())) + 1;
				System.out.printf("  Loop %d Region %d (%d-%d)\n", ilp, irp, cp
						.getStart(), cp.getEnd());
			}
		}
	}

	/**
	 * Find node of greatest branching that is deepest.
	 */
	private void find_central_loop() {
		if (debug)
			System.out.println("find_central_loop");
		Loop lp = new Loop();
		int maxconn, maxdepth, i;

		determine_depths();
		maxconn = 0;
		maxdepth = -1;
		for (i = 0; i < loop_count; i++) {
			lp = loops.get(i);
			if (lp.getNconnection() > maxconn) {
				maxdepth = lp.getDepth();
				maxconn = lp.getNconnection();
				root = lp;
			} else if (lp.getDepth() > maxdepth
					&& lp.getNconnection() == maxconn) {
				maxdepth = lp.getDepth();
				root = lp;
			}
		}
	}

	/**
	 * Determine the depth of all loops.
	 */
	private void determine_depths() {
		if (debug)
			System.out.println("determine_depths");
		Loop lp = new Loop();
		int i, j;

		for (i = 0; i < loop_count; i++) {
			lp = loops.get(i);
			for (j = 0; j < loop_count; j++)
				loops.get(j).setMark(false);
			lp.setDepth(depth(lp));
		}
	}

	/**
	 * Determines the depth of loop, lp. Depth is defined as the minimum
	 * distance to a leaf loop where a leaf loop is one that has only one or no
	 * connections.
	 */
	private int depth(Loop lp) {
		if (debug)
			System.out.println("depth");
		int count, ret, d;

		if (lp.getNconnection() <= 1)
			return 0;
		if (lp.isMark())
			return -1;
		lp.setMark(true);
		count = 0;
		ret = 0;
		for (int i = 0; lp.getConnection(i) != null; i++) {
			d = depth(lp.getConnection(i).getLoop());
			if (d >= 0) {
				if (++count == 1)
					ret = d;
				else if (ret > d)
					ret = d;
			}
		}
		lp.setMark(false);
		return ret + 1;
	}

	/**
	 * This is the workhorse of the display program. The algorithm is recursive
	 * based on processing individual loops. Each base pairing region is
	 * displayed using the direction given by the circle diagram, and the
	 * connections between the regions is drawn by equally spaced points. The
	 * radius of the loop is set to minimize the square error for lengths
	 * between sequential bases in the loops. The "correct" length for base
	 * links is 1. If the least squares fitting of the radius results in loops
	 * being less than 1/2 unit apart, then that segment is extruded.
	 * 
	 * The variable, anchor_connection, gives the connection to the loop
	 * processed in an previous level of recursion.
	 * 
	 * @throws ExceptionNAViewAlgorithm
	 */
	private void traverse_loop(Loop lp, Connection anchor_connection)
			throws ExceptionNAViewAlgorithm {
		if (debug)
			System.out.println("  traverse_loop");
		double xs, ys, xe, ye, xn, yn, angleinc, r;
		double radius, xc, yc, xo, yo, astart, aend, a;
		Connection cp, cpnext, acp, cpprev;
		int i, j, n, ic;
		double da, maxang;
		int count, icstart, icend, icmiddle, icroot;
		boolean done, done_all_connections, rooted;
		int sign;
		double midx, midy, nrx, nry, mx, my, vx, vy, dotmv, nmidx, nmidy;
		int icstart1, icup, icdown, icnext, direction;
		double dan, dx, dy, rr;
		double cpx, cpy, cpnextx, cpnexty, cnx, cny, rcn, rc, lnx, lny, rl, ac, acn, sx, sy, dcp;
		int imaxloop = 0;

		angleinc = 2 * Math.PI / (nbase + 1);
		acp = null;
		icroot = -1;
		int indice = 0;
		
		for (ic = 0; (cp = lp.getConnection(indice)) != null; indice++, ic++) {
			// xs = cos(angleinc*cp.setStart(); ys = sin(angleinc*cp.setStart();
			// xe =
			// cos(angleinc*cp.setEnd()); ye = sin(angleinc*cp.setEnd());
			xs = -Math.sin(angleinc * cp.getStart());
			ys = Math.cos(angleinc * cp.getStart());
			xe = -Math.sin(angleinc * cp.getEnd());
			ye = Math.cos(angleinc * cp.getEnd());
			xn = ye - ys;
			yn = xs - xe;
			r = Math.sqrt(xn * xn + yn * yn);
			cp.setXrad(xn / r);
			cp.setYrad(yn / r);
			cp.setAngle(Math.atan2(yn, xn));
			if (cp.getAngle() < 0.0)
				cp.setAngle(cp.getAngle() + 2 * Math.PI);
			if (anchor_connection != null
					&& anchor_connection.getRegion() == cp.getRegion()) {
				acp = cp;
				icroot = ic;
			}
		}
		// remplacement d'une etiquette de goto
		set_radius: while (true) {
			determine_radius(lp, lencut);
			radius = lp.getRadius()/RADIUS_REDUCTION_FACTOR;
			if (anchor_connection == null)
				xc = yc = 0.0;
			else {
				xo = (bases.get(acp.getStart()).getX() + bases
						.get(acp.getEnd()).getX()) / 2.0;
				yo = (bases.get(acp.getStart()).getY() + bases
						.get(acp.getEnd()).getY()) / 2.0;
				xc = xo - radius * acp.getXrad();
				yc = yo - radius * acp.getYrad();
			}

			// The construction of the connectors will proceed in blocks of
			// connected connectors, where a connected connector pairs means two
			// connectors that are forced out of the drawn circle because they
			// are too close together in angle.

			// First, find the start of a block of connected connectors

			if (icroot == -1)
				icstart = 0;
			else
				icstart = icroot;
			cp = lp.getConnection(icstart);
			count = 0;
			if (debug)
			{
				System.out.printf("Now processing loop %d\n", lp.getNumber());
				System.out.println("  "+lp);
			}
			done = false;
			do {
				j = icstart - 1;
				if (j < 0)
					j = lp.getNconnection() - 1;
				cpprev = lp.getConnection(j);
				if (!connected_connection(cpprev, cp)) {
					done = true;
				} else {
					icstart = j;
					cp = cpprev;
				}
				if (++count > lp.getNconnection()) {
					// Here everything is connected. Break on maximum angular
					// separation between connections.

					maxang = -1.0;
					for (ic = 0; ic < lp.getNconnection(); ic++) {
						j = ic + 1;
						if (j >= lp.getNconnection())
							j = 0;
						cp = lp.getConnection(ic);
						cpnext = lp.getConnection(j);
						ac = cpnext.getAngle() - cp.getAngle();
						if (ac < 0.0)
							ac += 2 * Math.PI;
						if (ac > maxang) {
							maxang = ac;
							imaxloop = ic;
						}
					}
					icend = imaxloop;
					icstart = imaxloop + 1;
					if (icstart >= lp.getNconnection())
						icstart = 0;
					cp = lp.getConnection(icend);
					cp.setBroken(true);
					done = true;
				}
			} while (!done);
			done_all_connections = false;
			icstart1 = icstart;
			if (debug)
				System.out.printf("  Icstart1 = %d\n", icstart1);
			while (!done_all_connections) {
				count = 0;
				done = false;
				icend = icstart;
				rooted = false;
				while (!done) {
					cp = lp.getConnection(icend);
					if (icend == icroot)
						rooted = true;
					j = icend + 1;
					if (j >= lp.getNconnection()) {
						j = 0;
					}
					cpnext = lp.getConnection(j);
					if (connected_connection(cp, cpnext)) {
						if (++count >= lp.getNconnection())
							break;
						icend = j;
					} else {
						done = true;
					}
				}
				icmiddle = find_ic_middle(icstart, icend, anchor_connection,
						acp, lp);
				ic = icup = icdown = icmiddle;
				if (debug)
					System.out.printf("  IC start = %d  middle = %d  end = %d\n",
							icstart, icmiddle, icend);
				done = false;
				direction = 0;
				while (!done) {
					if (direction < 0) {
						ic = icup;
					} else if (direction == 0) {
						ic = icmiddle;
					} else {
						ic = icdown;
					}
					if (ic >= 0) {
						cp = lp.getConnection(ic);
						if (anchor_connection == null || acp != cp) {
							if (direction == 0) {
								astart = cp.getAngle()
										- Math.asin(1.0 / 2.0 / radius);
								aend = cp.getAngle()
										+ Math.asin(1.0 / 2.0 / radius);
								bases.get(cp.getStart()).setX(
										xc + radius * Math.cos(astart));
								bases.get(cp.getStart()).setY(
										yc + radius * Math.sin(astart));
								bases.get(cp.getEnd()).setX(
										xc + radius * Math.cos(aend));
								bases.get(cp.getEnd()).setY(
										yc + radius * Math.sin(aend));
							} else if (direction < 0) {
								j = ic + 1;
								if (j >= lp.getNconnection())
									j = 0;
								cp = lp.getConnection(ic);
								cpnext = lp.getConnection(j);
								cpx = cp.getXrad();
								cpy = cp.getYrad();
								ac = (cp.getAngle() + cpnext.getAngle()) / 2.0;
								if (cp.getAngle() > cpnext.getAngle())
									ac -= Math.PI;
								cnx = Math.cos(ac);
								cny = Math.sin(ac);
								lnx = cny;
								lny = -cnx;
								da = cpnext.getAngle() - cp.getAngle();
								if (da < 0.0)
									da += 2 * Math.PI;
								if (cp.isExtruded()) {
									if (da <= Math.PI / 2)
										rl = 2.0;
									else
										rl = 1.5;
								} else {
									rl = 1.0;
								}
								bases.get(cp.getEnd()).setX(
										bases.get(cpnext.getStart()).getX()
												+ rl * lnx);
								bases.get(cp.getEnd()).setY(
										bases.get(cpnext.getStart()).getY()
												+ rl * lny);
								bases.get(cp.getStart()).setX(
										bases.get(cp.getEnd()).getX() + cpy);
								bases.get(cp.getStart()).setY(
										bases.get(cp.getEnd()).getY() - cpx);
							} else {
								j = ic - 1;
								if (j < 0)
									j = lp.getNconnection() - 1;
								cp = lp.getConnection(j);
								cpnext = lp.getConnection(ic);
								cpnextx = cpnext.getXrad();
								cpnexty = cpnext.getYrad();
								ac = (cp.getAngle() + cpnext.getAngle()) / 2.0;
								if (cp.getAngle() > cpnext.getAngle())
									ac -= Math.PI;
								cnx = Math.cos(ac);
								cny = Math.sin(ac);
								lnx = -cny;
								lny = cnx;
								da = cpnext.getAngle() - cp.getAngle();
								if (da < 0.0)
									da += 2 * Math.PI;
								if (cp.isExtruded()) {
									if (da <= Math.PI / 2)
										rl = 2.0;
									else
										rl = 1.5;
								} else {
									rl = 1.0;
								}
								bases.get(cpnext.getStart()).setX(
										bases.get(cp.getEnd()).getX() + rl
												* lnx);
								bases.get(cpnext.getStart()).setY(
										bases.get(cp.getEnd()).getY() + rl
												* lny);
								bases.get(cpnext.getEnd()).setX(
										bases.get(cpnext.getStart()).getX()
												- cpnexty);
								bases.get(cpnext.getEnd()).setY(
										bases.get(cpnext.getStart()).getY()
												+ cpnextx);
							}
						}
					}
					if (direction < 0) {
						if (icdown == icend) {
							icdown = -1;
						} else if (icdown >= 0) {
							if (++icdown >= lp.getNconnection()) {
								icdown = 0;
							}
						}
						direction = 1;
					} else {
						if (icup == icstart)
							icup = -1;
						else if (icup >= 0) {
							if (--icup < 0) {
								icup = lp.getNconnection() - 1;
							}
						}
						direction = -1;
					}
					done = icup == -1 && icdown == -1;
				}
				icnext = icend + 1;
				if (icnext >= lp.getNconnection())
					icnext = 0;
				if (icend != icstart
						&& (!(icstart == icstart1 && icnext == icstart1))) {

					// Move the bases just constructed (or the radius) so that
					// the bisector of the end points is radius distance away
					// from the loop center.

					cp = lp.getConnection(icstart);
					cpnext = lp.getConnection(icend);
					dx = bases.get(cpnext.getEnd()).getX()
							- bases.get(cp.getStart()).getX();
					dy = bases.get(cpnext.getEnd()).getY()
							- bases.get(cp.getStart()).getY();
					midx = bases.get(cp.getStart()).getX() + dx / 2.0;
					midy = bases.get(cp.getStart()).getY() + dy / 2.0;
					rr = Math.sqrt(dx * dx + dy * dy);
					mx = dx / rr;
					my = dy / rr;
					vx = xc - midx;
					vy = yc - midy;
					rr = Math.sqrt(dx * dx + dy * dy);
					vx /= rr;
					vy /= rr;
					dotmv = vx * mx + vy * my;
					nrx = dotmv * mx - vx;
					nry = dotmv * my - vy;
					rr = Math.sqrt(nrx * nrx + nry * nry);
					nrx /= rr;
					nry /= rr;

					// Determine which side of the bisector the center should
					// be.

					dx = bases.get(cp.getStart()).getX() - xc;
					dy = bases.get(cp.getStart()).getY() - yc;
					ac = Math.atan2(dy, dx);
					if (ac < 0.0)
						ac += 2 * Math.PI;
					dx = bases.get(cpnext.getEnd()).getX() - xc;
					dy = bases.get(cpnext.getEnd()).getY() - yc;
					acn = Math.atan2(dy, dx);
					if (acn < 0.0)
						acn += 2 * Math.PI;
					if (acn < ac)
						acn += 2 * Math.PI;
					if (acn - ac > Math.PI)
						sign = -1;
					else
						sign = 1;
					nmidx = xc + sign * radius * nrx;
					nmidy = yc + sign * radius * nry;
					if (rooted) {
						xc -= nmidx - midx;
						yc -= nmidy - midy;
					} else {
						for (ic = icstart;;) {
							cp = lp.getConnection(ic);
							i = cp.getStart();
							bases.get(i).setX(
									bases.get(i).getX() + nmidx - midx);
							bases.get(i).setY(
									bases.get(i).getY() + nmidy - midy);
							i = cp.getEnd();
							bases.get(i).setX(
									bases.get(i).getX() + nmidx - midx);
							bases.get(i).setY(
									bases.get(i).getY() + nmidy - midy);
							if (ic == icend)
								break;
							if (++ic >= lp.getNconnection())
								ic = 0;
						}
					}
				}
				icstart = icnext;
				done_all_connections = icstart == icstart1;
			}
			for (ic = 0; ic < lp.getNconnection(); ic++) {
				cp = lp.getConnection(ic);
				j = ic + 1;
				if (j >= lp.getNconnection())
					j = 0;
				cpnext = lp.getConnection(j);
				dx = bases.get(cp.getEnd()).getX() - xc;
				dy = bases.get(cp.getEnd()).getY() - yc;
				rc = Math.sqrt(dx * dx + dy * dy);
				ac = Math.atan2(dy, dx);
				if (ac < 0.0)
					ac += 2 * Math.PI;
				dx = bases.get(cpnext.getStart()).getX() - xc;
				dy = bases.get(cpnext.getStart()).getY() - yc;
				rcn = Math.sqrt(dx * dx + dy * dy);
				acn = Math.atan2(dy, dx);
				if (acn < 0.0)
					acn += 2 * Math.PI;
				if (acn < ac)
					acn += 2 * Math.PI;
				dan = acn - ac;
				dcp = cpnext.getAngle() - cp.getAngle();
				if (dcp <= 0.0)
					dcp += 2 * Math.PI;
				if (Math.abs(dan - dcp) > Math.PI) {
					if (cp.isExtruded()) {
						warningEmition("Warning from traverse_loop. Loop "
								+ lp.getNumber() + " has crossed regions\n");
					} else if ((cpnext.getStart() - cp.getEnd()) != 1) {
						cp.setExtruded(true);
						continue set_radius; // remplacement du goto
					}
				}
				if (cp.isExtruded()) {
					construct_extruded_segment(cp, cpnext);
				} else {
					n = cpnext.getStart() - cp.getEnd();
					if (n < 0)
						n += nbase + 1;
					angleinc = dan / n;
					for (j = 1; j < n; j++) {
						i = cp.getEnd() + j;
						if (i > nbase)
							i -= nbase + 1;
						a = ac + j * angleinc;
						rr = rc + (rcn - rc) * (a - ac) / dan;
						bases.get(i).setX(xc + rr * Math.cos(a));
						bases.get(i).setY(yc + rr * Math.sin(a));
					}
				}
			}
			break;
		}
		for (ic = 0; ic < lp.getNconnection(); ic++) {
			if (icroot != ic) {
				cp = lp.getConnection(ic);
				generate_region(cp);
				traverse_loop(cp.getLoop(), cp);
			}
		}
		n = 0;
		sx = 0.0;
		sy = 0.0;
		for (ic = 0; ic < lp.getNconnection(); ic++) {
			j = ic + 1;
			if (j >= lp.getNconnection())
				j = 0;
			cp = lp.getConnection(ic);
			cpnext = lp.getConnection(j);
			n += 2;
			sx += bases.get(cp.getStart()).getX()
					+ bases.get(cp.getEnd()).getX();
			sy += bases.get(cp.getStart()).getY()
					+ bases.get(cp.getEnd()).getY();
			if (!cp.isExtruded()) {
				for (j = cp.getEnd() + 1; j != cpnext.getStart(); j++) {
					if (j > nbase)
						j -= nbase + 1;
					n++;
					sx += bases.get(j).getX();
					sy += bases.get(j).getY();
				}
			}
		}
		lp.setX(sx / n);
		lp.setY(sy / n);
	}

	/**
	 * For the loop pointed to by lp, determine the radius of the loop that will
	 * ensure that each base around the loop will have a separation of at least
	 * lencut around the circle. If a segment joining two connectors will not
	 * support this separation, then the flag, extruded, will be set in the
	 * first of these two indicators. The radius is set in lp.
	 * 
	 * The radius is selected by a least squares procedure where the sum of the
	 * squares of the deviations of length from the ideal value of 1 is used as
	 * the error function.
	 */
	private void determine_radius(Loop lp, double lencut) {
		if (debug)
			System.out.println("  Determine_radius");
		double mindit, ci, dt, sumn, sumd, radius, dit;
		int i, j, end, start, imindit = 0;
		Connection cp = new Connection(), cpnext = new Connection();
		double rt2_2 = 0.7071068;

		do {
			mindit = 1.0e10;
			for (sumd = 0.0, sumn = 0.0, i = 0; i < lp.getNconnection(); i++) {
				cp = lp.getConnection(i);
				j = i + 1;
				if (j >= lp.getNconnection())
					j = 0;
				cpnext = lp.getConnection(j);
				end = cp.getEnd();
				start = cpnext.getStart();
				if (start < end)
					start += nbase + 1;
				dt = cpnext.getAngle() - cp.getAngle();
				if (dt <= 0.0)
					dt += 2 * Math.PI;
				if (!cp.isExtruded())
					ci = start - end;
				else {
					if (dt <= Math.PI / 2)
						ci = 2.0;
					else
						ci = 1.5;
				}
				sumn += dt * (1.0 / ci + 1.0);
				sumd += dt * dt / ci;
				dit = dt / ci;
				if (dit < mindit && !cp.isExtruded() && ci > 1.0) {
					mindit = dit;
					imindit = i;
				}
			}
			radius = sumn / sumd;
			if (radius < rt2_2)
				radius = rt2_2;
			if (mindit * radius < lencut) {
				lp.getConnection(imindit).setExtruded(true);
			}
		} while (mindit * radius < lencut);
		if (lp.getRadius() > 0.0)
			radius = lp.getRadius();
		else
			lp.setRadius(radius);
	}

	/**
	 * Determines if the connections cp and cpnext are connected
	 */
	private boolean connected_connection(Connection cp, Connection cpnext) {
		if (debug)
			System.out.println("  Connected_connection");
		if (cp.isExtruded()) {
			return true;
		} else if (cp.getEnd() + 1 == cpnext.getStart()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Finds the middle of a set of connected connectors. This is normally the
	 * middle connection in the sequence except if one of the connections is the
	 * anchor, in which case that connection will be used.
	 * 
	 * @throws ExceptionNAViewAlgorithm
	 */
	private int find_ic_middle(int icstart, int icend,
			Connection anchor_connection, Connection acp, Loop lp)
			throws ExceptionNAViewAlgorithm {
		if (debug)
			System.out.println("  Find_ic_middle");
		int count, ret, ic, i;
		boolean done;

		count = 0;
		ret = -1;
		ic = icstart;
		done = false;
		while (!done) {
			if (count++ > lp.getNconnection() * 2) {
				throw new ExceptionNAViewAlgorithm(
						"Infinite loop detected in find_ic_middle");
			}
			if (anchor_connection != null && lp.getConnection(ic) == acp) {
				ret = ic;
			}
			done = ic == icend;
			if (++ic >= lp.getNconnection()) {
				ic = 0;
			}
		}
		if (ret == -1) {
			for (i = 1, ic = icstart; i < (count + 1) / 2; i++) {
				if (++ic >= lp.getNconnection())
					ic = 0;
			}
			ret = ic;
		}
		return ret;
	}

	/**
	 * Generates the coordinates for the base pairing region of a connection
	 * given the position of the starting base pair.
	 * 
	 * @throws ExceptionNAViewAlgorithm
	 */
	private void generate_region(Connection cp) throws ExceptionNAViewAlgorithm {
		if (debug)
			System.out.println("  Generate_region");
		int l, start, end, i, mate;
		Region rp;

		rp = cp.getRegion();
		l = 0;
		if (cp.getStart() == rp.getStart1()) {
			start = rp.getStart1();
			end = rp.getEnd1();
		} else {
			start = rp.getStart2();
			end = rp.getEnd2();
		}
		if (bases.get(cp.getStart()).getX() > ANUM - 100.0
				|| bases.get(cp.getEnd()).getX() > ANUM - 100.0) {
			throw new ExceptionNAViewAlgorithm(
					"Bad region passed to generate_region. Coordinates not defined.");
		}
		for (i = start + 1; i <= end; i++) {
			l++;
			bases.get(i).setX(
					bases.get(cp.getStart()).getX() + HELIX_FACTOR * l
							* cp.getXrad());
			bases.get(i).setY(
					bases.get(cp.getStart()).getY() + HELIX_FACTOR * l
							* cp.getYrad());
			mate = bases.get(i).getMate();
			bases.get(mate).setX(
					bases.get(cp.getEnd()).getX() + HELIX_FACTOR * l
							* cp.getXrad());
			bases.get(mate).setY(
					bases.get(cp.getEnd()).getY() + HELIX_FACTOR * l
							* cp.getYrad());
			
		}
	}

	/**
	 * Draws the segment of residue between the bases numbered start through
	 * end, where start and end are presumed to be part of a base pairing
	 * region. They are drawn as a circle which has a chord given by the ends of
	 * two base pairing regions defined by the connections.
	 * 
	 * @throws ExceptionNAViewAlgorithm
	 */
	private void construct_circle_segment(int start, int end)
			throws ExceptionNAViewAlgorithm {
		if (debug)
			System.out.println("  Construct_circle_segment");
		double dx, dy, rr, midx, midy, xn, yn, nrx, nry, mx, my, a;
		int l, j, i;

		dx = bases.get(end).getX() - bases.get(start).getX();
		dy = bases.get(end).getY() - bases.get(start).getY();
		rr = Math.sqrt(dx * dx + dy * dy);
		l = end - start;
		if (l < 0)
			l += nbase + 1;
		if (rr >= l) {
			dx /= rr;
			dy /= rr;
			for (j = 1; j < l; j++) {
				i = start + j;
				if (i > nbase)
					i -= nbase + 1;
				bases.get(i).setX(
						bases.get(start).getX() + dx * (double) j / (double) l);
				bases.get(i).setY(
						bases.get(start).getY() + dy * (double) j / (double) l);
			}
		} else {
			find_center_for_arc((l - 1), rr);
			dx /= rr;
			dy /= rr;
			midx = bases.get(start).getX() + dx * rr / 2.0;
			midy = bases.get(start).getY() + dy * rr / 2.0;
			xn = dy;
			yn = -dx;
			nrx = midx + _h * xn;
			nry = midy + _h * yn;
			mx = bases.get(start).getX() - nrx;
			my = bases.get(start).getY() - nry;
			rr = Math.sqrt(mx * mx + my * my);
			a = Math.atan2(my, mx);
			for (j = 1; j < l; j++) {
				i = start + j;
				if (i > nbase)
					i -= nbase + 1;
				bases.get(i).setX(nrx + rr * Math.cos(a + j * angleinc));
				bases.get(i).setY(nry + rr * Math.sin(a + j * angleinc));
			}
		}
	}

	/**
	 * Constructs the segment between cp and cpnext as a circle if possible.
	 * However, if the segment is too large, the lines are drawn between the two
	 * connecting regions, and bases are placed there until the connecting
	 * circle will fit.
	 * 
	 * @throws ExceptionNAViewAlgorithm
	 */
	private void construct_extruded_segment(Connection cp, Connection cpnext)
			throws ExceptionNAViewAlgorithm {
		if (debug)
			System.out.println("  Construct_extruded_segment");
		double astart, aend1, aend2, aave, dx, dy, a1, a2, ac, rr, da, dac;
		int start, end, n, nstart, nend;
		boolean collision;

		astart = cp.getAngle();
		aend2 = aend1 = cpnext.getAngle();
		if (aend2 < astart)
			aend2 += 2 * Math.PI;
		aave = (astart + aend2) / 2.0;
		start = cp.getEnd();
		end = cpnext.getStart();
		n = end - start;
		if (n < 0)
			n += nbase + 1;
		da = cpnext.getAngle() - cp.getAngle();
		if (da < 0.0) {
			da += 2 * Math.PI;
		}
		if (n == 2)
			construct_circle_segment(start, end);
		else {
			dx = bases.get(end).getX() - bases.get(start).getX();
			dy = bases.get(end).getY() - bases.get(start).getY();
			rr = Math.sqrt(dx * dx + dy * dy);
			dx /= rr;
			dy /= rr;
			if (rr >= 1.5 && da <= Math.PI / 2) {
				nstart = start + 1;
				if (nstart > nbase)
					nstart -= nbase + 1;
				nend = end - 1;
				if (nend < 0)
					nend += nbase + 1;
				bases.get(nstart).setX(bases.get(start).getX() + 0.5 * dx);
				bases.get(nstart).setY(bases.get(start).getY() + 0.5 * dy);
				bases.get(nend).setX(bases.get(end).getX() - 0.5 * dx);
				bases.get(nend).setY(bases.get(end).getY() - 0.5 * dy);
				start = nstart;
				end = nend;
			}
			do {
				collision = false;
				construct_circle_segment(start, end);
				nstart = start + 1;
				if (nstart > nbase)
					nstart -= nbase + 1;
				dx = bases.get(nstart).getX() - bases.get(start).getX();
				dy = bases.get(nstart).getY() - bases.get(start).getY();
				a1 = Math.atan2(dy, dx);
				if (a1 < 0.0)
					a1 += 2 * Math.PI;
				dac = a1 - astart;
				if (dac < 0.0)
					dac += 2 * Math.PI;
				if (dac > Math.PI)
					collision = true;
				nend = end - 1;
				if (nend < 0)
					nend += nbase + 1;
				dx = bases.get(nend).getX() - bases.get(end).getX();
				dy = bases.get(nend).getY() - bases.get(end).getY();
				a2 = Math.atan2(dy, dx);
				if (a2 < 0.0)
					a2 += 2 * Math.PI;
				dac = aend1 - a2;
				if (dac < 0.0)
					dac += 2 * Math.PI;
				if (dac > Math.PI)
					collision = true;
				if (collision) {
					ac = minf2(aave, astart + 0.5);
					bases.get(nstart).setX(
							bases.get(start).getX() + Math.cos(ac));
					bases.get(nstart).setY(
							bases.get(start).getY() + Math.sin(ac));
					start = nstart;
					ac = maxf2(aave, aend2 - 0.5);
					bases.get(nend).setX(bases.get(end).getX() + Math.cos(ac));
					bases.get(nend).setY(bases.get(end).getY() + Math.sin(ac));
					end = nend;
					n -= 2;
				}
			} while (collision && n > 1);
		}
	}

	/**
	 * Given n points to be placed equidistantly and equiangularly on a polygon
	 * which has a chord of length, b, find the distance, h, from the midpoint
	 * of the chord for the center of polygon. Positive values mean the center
	 * is within the polygon and the chord, whereas negative values mean the
	 * center is outside the chord. Also, the radial angle for each polygon side
	 * is returned in theta.
	 * 
	 * The procedure uses a bisection algorithm to find the correct value for
	 * the center. Two equations are solved, the angles around the center must
	 * add to 2*Math.PI, and the sides of the polygon excluding the chord must
	 * have a length of 1.
	 * 
	 * @throws ExceptionNAViewAlgorithm
	 */
	private void find_center_for_arc(double n, double b)
			throws ExceptionNAViewAlgorithm {
		if (debug)
			System.out.println("  Find_center_for_arc");
		double h, hhi, hlow, r, disc, theta, e, phi;
		int iter;

		hhi = (n + 1.0) / Math.PI;
		// changed to prevent div by zero if (ih)
		hlow = -hhi - b / (n + 1.000001 - b);
		if (b < 1)
			// otherwise we might fail below (ih)
			hlow = 0;
		iter = 0;
		do {
			h = (hhi + hlow) / 2.0;
			r = Math.sqrt(h * h + b * b / 4.0);
			// if (r<0.5) {r = 0.5; h = 0.5*Math.sqrt(1-b*b);}
			disc = 1.0 - 0.5 / (r * r);
			if (Math.abs(disc) > 1.0) {
				throw new ExceptionNAViewAlgorithm(
						"Unexpected large magnitude discriminant = " + disc
								+ " " + r);
			}
			theta = Math.acos(disc);
			// theta = 2*Math.acos(Math.sqrt(1-1/(4*r*r)));
			phi = Math.acos(h / r);
			e = theta * (n + 1) + 2 * phi - 2 * Math.PI;
			if (e > 0.0) {
				hlow = h;
			} else {
				hhi = h;
			}
		} while (Math.abs(e) > 0.0001 && ++iter < MAXITER);
		if (iter >= MAXITER) {
			if (noIterationFailureYet) {
				warningEmition("Iteration failed in find_center_for_arc");
				noIterationFailureYet = false;
			}
			h = 0.0;
			theta = 0.0;
		}
		_h = h;
		angleinc = theta;
	}

	private double minf2(double x1, double x2) {
		return ((x1) < (x2)) ? (x1) : (x2);
	}

	private double maxf2(double x1, double x2) {
		return ((x1) > (x2)) ? (x1) : (x2);
	}

	public void warningEmition(String warningMessage) throws ExceptionNAViewAlgorithm {
		throw (new ExceptionNAViewAlgorithm(warningMessage));
	}
}
