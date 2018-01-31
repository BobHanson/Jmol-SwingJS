package jspecview.common;

import java.util.EventObject;

import jspecview.common.Coordinate;

@SuppressWarnings("serial")
public class PeakPickEvent extends EventObject {
	
	private Coordinate coord;
	private PeakInfo peakInfo;
	
	public PeakPickEvent(Object source, Coordinate coord, PeakInfo peakInfo) {
		super(source);
		this.coord = coord;
		this.peakInfo = (peakInfo == null ? null : peakInfo);
	}

	public Coordinate getCoord() {
		return coord;
	}

	public PeakInfo getPeakInfo() {
		return peakInfo;
	}

	@Override
	public String toString() {
	  return (peakInfo == null ? null : peakInfo.toString());
	}
}
