package objects;

import utilities.Constants;

public class TimeStep implements Comparable<TimeStep> {

	private volatile int timeStep;
	private volatile Float realTime;
	private volatile int realTimeInt;

	public TimeStep(int timeStep, float realTime) {
		this.timeStep = timeStep;
		this.realTime = realTime;
	}

	public synchronized int getTimeStep() {
		return timeStep;
	}

	public synchronized float getRealTime() {
		return realTime == null ? new Float(realTimeInt) : realTime;
	}

	@Override
	public String toString() {
		return "[" + timeStep + ": " + Constants.decimalFormat.format(realTime)
				+ " yr]";
	}

	@Override
	public int compareTo(TimeStep o) {
		return new Integer(timeStep).compareTo(o.getTimeStep());
	}
}
