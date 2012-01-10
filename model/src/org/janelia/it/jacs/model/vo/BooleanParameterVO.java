
package org.janelia.it.jacs.model.vo;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 25, 2006
 * Time: 4:15:52 PM
 */
public class BooleanParameterVO extends ParameterVO {
    public static final String PARAM_BOOLEAN = "Boolean";

    private boolean booleanValue;

    /**
     * For GWT support only
     */
    public BooleanParameterVO() {
        super();
        this.booleanValue = true;
    }

    public BooleanParameterVO(boolean value) {
        this.booleanValue = value;
    }

    public void setBooleanValue(boolean value) {
        this.booleanValue = value;
    }

    public boolean getBooleanValue() {
        return booleanValue;
    }

    public String getStringValue() {
        return String.valueOf(booleanValue);
    }

    public boolean isValid() {
        return true;
    }

    public String getType() {
        return PARAM_BOOLEAN;
    }

    public String toString() {
        return "BooleanParameterVO{" +
                "booleanValue=" + booleanValue +
                '}';
    }

}
