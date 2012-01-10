
package org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables;

/**
 * Comparable that sorts on a provided int value but outputs a formatted String
 */
public class IntegerString implements Comparable {
    private long _value;
    private String _formattedValue;

    public IntegerString(Integer value, String displayValue) {
        _value = value.longValue();
        _formattedValue = displayValue;
    }

    public IntegerString(Long value, String displayValue) {
        _value = value.longValue();
        _formattedValue = displayValue;
    }

    public IntegerString(int value, String displayValue) {
        _value = value;
        _formattedValue = displayValue;
    }

    public IntegerString(long value, String displayValue) {
        _value = value;
        _formattedValue = displayValue;
    }

    public String toString() {
        return _formattedValue;
    }

    public long getValue() {
        return _value;
    }

    public int compareTo(Object o) {
        if (o == null)
            return 1;

        IntegerString other = (IntegerString) o;
        return (_value > other.getValue() ? 1 : (_value == other.getValue() ? 0 : -1));
    }
}
