
package org.janelia.it.jacs.model.vo;

/**
 * Based on LongParameterVO written by smurphy
 * User: cgoina
 * Date: Aug 29, 2006
 * Time: 11:25:35 AM
 */
public class IntegerParameterVO extends ParameterVO {

    /**
     * NOTE: As this class is used by GWT, and it's values converted into javascript, we have to be careful exactly
     * what we set the values of Max and Min to.  Calling Integer.MIN Integer.MAX was preventing the bridge from javascript
     * back to this class.  This must always be kept in mind when setting min and max values.
     * For this reason I am forcing people to set min and max when using this object;
     */
    public static final String PARAM_INTEGER    = "Integer";
    public static final transient Integer INTEGER_MIN = (int) -1000000;
    public static final transient Integer INTEGER_MAX = (int) 1000000;
    private Integer minValue;
    private Integer maxValue;
    private Integer actualValue;

    /**
     * For GWT use
     */
    public IntegerParameterVO() {
        super();
    }

    public IntegerParameterVO(Integer defaultValue) {
        this.minValue = INTEGER_MIN;
        this.maxValue = INTEGER_MAX;
        this.actualValue = defaultValue;
    }

    public IntegerParameterVO(Integer minValue, Integer maxValue, Integer defaultValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.actualValue = defaultValue;
    }

    public Integer getMinValue() {
        return minValue;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    // These private accessors needed for Hibernate
    private void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }

    private void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }

    public void setActualValue(Integer newValue) throws ParameterException {
        this.actualValue = newValue;
    }

    public Integer getActualValue() {
        return actualValue;
    }

    public String getStringValue() {
        return actualValue.toString();
    }

    public boolean isValid() {
        return null != this.actualValue && !((null == minValue) || (null == maxValue) ||
                (actualValue < minValue) ||
                (actualValue > maxValue));
    }

    public String getType() {
        return PARAM_INTEGER;
    }

    public String toString() {
        return "IntegerParameterVO{" +
                "minValue=" + minValue +
                "maxValue=" + maxValue +
                "actualValue=" + actualValue +
                '}';
    }

}
