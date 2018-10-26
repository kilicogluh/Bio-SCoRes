/**
 * 
 */
package gov.nih.nlm.ling.util;

/**
 * Utility class for simple string operations. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class StringUtils {
	
	/**
	 * Indicates whether a string consists only of letters.
	 * 
	 * @param str the string to check
	 * @return true if <var>str</var> consists only of letters
	 */
	public static boolean isAllLetters(String str) {
		if (str.length() == 0) return false;
		for (int i = 0; i < str.length(); i++) {
			if (Character.isLetter(str.charAt(i)) == false) {
				return false;
		    }
		}
		return true;
	}
	
	/**
	 * Indicates whether a string consists only of digits.
	 * 
	 * @param str the string to check
	 * @return true if <var>str</var> consists only of digits
	 */
	public static boolean isAllDigits(String str) {
		// doesn't consider /, , etc.
		if (str.length() == 0) return false;
		for (int i = 0; i < str.length(); i++) {
			if (Character.isDigit(str.charAt(i)) == false) {
				return false;
		    }
		}
		return true;
	}
	
	/**
	 * Indicates whether a string consists only of uppercase characters.
	 * 
	 * @param str the string to check
	 * @return true if <var>str</var> consists only of uppercase characters
	 */
	public static boolean isAllUpperCase(String str) {
		if (str.length() == 0) return false;
		for (int i = 0; i < str.length(); i++) {
	    	if (Character.isUpperCase(str.charAt(i)) == false) 
	    		return false;
		}
		return true;
	}
	
	/**
	 * Indicates whether a string contains a digit.
	 * 
	 * @param str the string to check
	 * @return true if <var>str</var> contains a digit
	 */
	public static boolean containsDigit(String str) {
		if (str.length() == 0) return false;
		for (int i = 0; i < str.length(); i++) {
			if (Character.isDigit(str.charAt(i))) {
				return true;
		    }
		}
		return false;
	}
	
	/**
	 * Indicates whether a string contains uppercase characters.
	 * 
	 * @param str the string to check
	 * @return true if <var>str</var> contains uppercase characters
	 */
	public static boolean containsUpperCase(String str) {
		int len = str.length();
	    for (int i = 0; i < len; i++) {
	    	if (Character.isUpperCase(str.charAt(i))) return true;
	    }
	    return false;
	}
}
