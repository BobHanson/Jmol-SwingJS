package jspecview.common;



class PeakPick extends Measurement {

	PeakPick setValue(double x, double y, Spectrum spec, String text,
			double value) {
		if (text == null) {
			set(x, y);
			setPt2(spec, false);
		} else {
			setA(x, y, spec, text, false, false, 0, 6);
			this.value = value;
			setPt2(getXVal(), getYVal());
		}
		return this;
	}

}
