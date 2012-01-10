
package org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables;

/**
 * A Comparable that holds a display string and a numeric value used for comparing with other NumericStrings.
 * Makes an attempt to deduce the numeric value by parsing the display string using the following rules:<ul>
 * <li>Ignores leading non-numeric values (letters, periods, spaces)</li>
 * <li>Accepts valid digits (digits, negative sign, periods, less than, greater than) up to the first non-valid digit</li>
 * <li>Ignores the rest</li>
 * </ul>
 * <p/>
 * Example: the input string "1.2 *C"   yields a float value of "1.2". <br>
 * Example: the input string "-20-80m"  yields a float value of "-20". <br>
 * Example: the input string "ca. 9000" yields a float value of "9000".<br>
 *
 * @author Michael Press
 */
public class NumericString implements Comparable {
    private String _stringVal;
    private float _floatVal;

    /**
     * @param displayValue the display value;  the value for sorting will be computed by parsing the display value
     */
    public NumericString(String displayValue) {
        setStringValue(cleanDisplayValue(displayValue));
    }

    /**
     * Allows caller to explicitly set the value used for sorting.
     */
    public NumericString(String displayValue, float sortValue) {
        setStringValue(cleanDisplayValue(displayValue), false);
        _floatVal = sortValue;
    }

    private String cleanDisplayValue(String displayValue) {
        if (displayValue == null || displayValue.equals("")) // handle nulls - display a blank
            displayValue = "&nbsp;";
        return displayValue;
    }

    public void setStringValue(String value) {
        setStringValue(value, true);
    }

    /**
     * Sets the display value.  If updateFloatRepresentation is true, the floating point representation of the
     * string value is calculated.
     */
    public void setStringValue(String value, boolean updateFloatRepresentaton) {
        _stringVal = cleanDisplayValue(value);
        if (updateFloatRepresentaton)
            parseFloatVal();
    }

    protected void setFloatValue(float value) {
        _floatVal = value;
    }

    /**
     * Sets the NumericString's actual numeric value by parsing the string;  see class notes for parsing rules.
     */
    protected void parseFloatVal() {
        if (_stringVal.equals("&nbsp;")) {
            _floatVal = 0;
            return;
        }

        char[] chars = _stringVal.toCharArray();

        // Walk past leading chars (like "ca.")
        int start = 0;
        while (start < chars.length && isLeadingChar(chars[start]))
            start++;

        // Walk until we find an invalid digit
        int end = start;
        if (chars[start] == '-') // allow leading dash, but not range dash found after first digit
            end++;
        while (end < chars.length && isDigit(chars[end]))
            end++;

        setFloatValue(Float.parseFloat(_stringVal.substring(start, end)));
    }

    /**
     * Valid leading chars are letters, '.', or ' '.
     */
    private boolean isLeadingChar(char c) {
        return Character.isLetter(c) || c == '.' || c == ' ' || c == '<' || c == '>';
    }

    /**
     * Valid digits are 0-9, '.', '<' or '>'
     */
    protected boolean isDigit(char c) {
        return Character.isDigit(c) || c == '.';
    }

    public float getFloatValue() {
        return _floatVal;
    }

    public String getStringValue() {
        return _stringVal;
    }

    public int compareTo(Object o) {
        if (o == null)
            return 1;
        else
            return Float.compare(_floatVal, ((NumericString) o).getFloatValue());
    }

    public String toString() {
        return _stringVal;
    }
}
