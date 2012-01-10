
package org.janelia.it.jacs.model.vo;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 25, 2006
 * Time: 4:15:13 PM
 */
public class TextParameterVO extends ParameterVO {
    public static final String PARAM_TEXT    = "Text";
    private String textValue;
    private int maxLength;

    public String getType() {
        return PARAM_TEXT;
    }

    public TextParameterVO() {
        super();
    }

    public TextParameterVO(String textValue) {
        this.textValue = textValue;
    }

    public TextParameterVO(String textValue, int maxLength) {
        this.maxLength = maxLength;
        this.textValue = textValue;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String newValue) throws ParameterException {
        this.textValue = newValue;
    }

    public String getStringValue() {
        return getTextValue();
    }

    public int getMaxLength() {
        return maxLength;
    }

    // needed for Hibernate
    private void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public boolean isValid() {
        if (null == this.textValue) {
            return false;
        }
        else if (maxLength > 0 && maxLength < this.textValue.length()) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "TextParameterVO{" +
                "textValue='" + textValue + '\'' + "," +
                "maxLength=" + maxLength +
                '}';
    }
}
