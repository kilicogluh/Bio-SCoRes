package gov.nih.nlm.ling.sem;

/**
 * Represents a modality scale/value pair.<p>
 * The allowable scales are defined by <code>ScaleType</code> enum. The valid values are: <ul>
 * <li> ScaleType#EPISTEMIC
 * <li> ScaleType#DEONTIC
 * <li> ScaleType#POTENTIAL
 * <li> ScaleType#VOLITIVE
 * <li> ScaleType#INTERROGATIVE
 * <li> ScaleType#SUCCESS
 * <li> ScaleType#INTENTIONAL
 * <li> ScaleType#EVALUATIVE
 * </ul>
 * More details are available in Kilicoglu (2012). The value is an interval between 0.0 and 1.0. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class ScalarModalityValue {

	/**
	 * Types of modality scales.
	 *
	 */
	public enum ScaleType {
		EPISTEMIC, DEONTIC, POTENTIAL, VOLITIVE, INTERROGATIVE, SUCCESS, INTENTIONAL, EVALUATIVE
	}
	
	private ScaleType scaleType;
	private Interval value;
	
	/**
	 * Instantiates a modality scale/value pair. 
	 * 
	 * @param scaleType the type of the scale
	 * @param value		the value on the scale
	 */
	public ScalarModalityValue(ScaleType scaleType, Interval value) {
		this.scaleType = scaleType;
		this.value = value;
	}
	
	/**
	 * Instantiates a modality scale/value pair with a single double value.
	 * 
	 * @param scaleType	the type of the scale
	 * @param value		the value on the scale
	 */
	public ScalarModalityValue(ScaleType scaleType, double value) {
		this.scaleType = scaleType;
		this.value = new Interval(value,value);
	}

	public ScaleType getScaleType() {
		return scaleType;
	}

	public void setScaleType(ScaleType scaleType) {
		this.scaleType = scaleType;
	}

	public Interval getValue() {
		return value;
	}

	public void setValue(Interval value) {
		this.value = value;
	}
	
	public String toString() {
		return scaleType.toString() + "_" + value.toString();
	}

}
