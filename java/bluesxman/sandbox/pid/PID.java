package bluesxman.sandbox.pid;

/**
 * Implements the algorithm for a Proportional-Integral-Derivative Controller (PID) as discussed in
 * http://en.wikipedia.org/wiki/PID_controller.  The PID is used to calculate an output value for a given history
 * of measured error values.
 * 
 * A PID is first constructed at an absolute time with no error.  The PID has no expectations about the
 * units used for error, time, or gain other than the units are self-consistent across all calls to the pid.
 * 
 * As error measurements are made, the next() function is called to create an updated instance of the PID.  The PID's
 * calculated output value can then be obtained from calling the getOutput() function on the new PID return by next().  
 * 
 * All instances of PID are immutable.  Therefore, each call to next() should be made on the last return PID returned
 * by next().
 */
public class PID {
	private static final int P = 0;
	private static final int I = 1;
	private static final int D = 2;
	private double[] gains;
	
	private double totalError;  // error integrated over time
	private double lastError;
	private double output;
	private double timeMeasured;
	
	/**
	 * Creates a new PID with no at startTime.
	 * 
	 * @param pGain Gain for the proportional error component
	 * @param iGain Gain for the integral error component
	 * @param dGain Gain for the derivative error component
	 * @param startTime Absolute clock time when error measurements began.
	 */
	public PID(double pGain, double iGain, double dGain, double startTime){
		gains = new double[]{pGain, iGain, dGain};
		totalError = 0;
		lastError = 0;
		output = 0;
		timeMeasured = startTime;
	}
	
	private PID(double[] gains, double prevTotError, double prevError, double inputError, double timeNow, double timeLast){
		double deltaT = timeNow - timeLast;
		double errorRate = (inputError - prevError) / deltaT;
		
		this.gains = gains;
		timeMeasured = timeNow;
		totalError = prevTotError + inputError * deltaT;
		output = gains[P] * inputError + gains[I] * totalError + gains[D] * errorRate;
		lastError = inputError;
	}
	
	/**
	 * Note, units of time need to be consistent across all usage of the PID.
	 * 
	 * @param inputError The signed error in the measurement where 0 means no error.
	 * @param timeMeasured Absolute clock time at which the new error measurement was made.
	 * @return The new state of the PID
	 */
	public PID next(double inputError, double timeMeasured){
		return new PID(gains, totalError, lastError, inputError, timeMeasured, this.timeMeasured);
	}
	
	/**
	 * 
	 * @return Output Output value calculated by the PID controller at the time of the last error measurement.
	 */
	public double getOutput(){
		return output;
	}
	
	/**
	 * Creates a PID with the history of the original but with the new gains.
	 * 
	 * @param pGain
	 * @param iGain
	 * @param dGain
	 * 
	 * @return The PID with the new gains.
	 */
	public PID setGains(double pGain, double iGain, double dGain){
		PID rval = new PID(pGain, iGain, dGain, timeMeasured);
		
		rval.totalError = this.totalError;
		rval.lastError = this.lastError;
		rval.output = this.output;
		
		return rval;
	}
}