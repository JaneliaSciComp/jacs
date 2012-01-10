
package org.janelia.it.jacs.web.gwt.common.client.model.tasks;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author Michael Press
 */
public class LegendItem implements Comparable, IsSerializable {
    private String _itemAcc;
    private String _displayValue;
    private String _red;
    private String _green;
    private String _blue;
    private Float _hue;

    /**
     * required for GWT
     */
    public LegendItem() {
    }

    public LegendItem(String itemAcc, String displayValue, String red, String green, String blue, Float hue) {
        _itemAcc = itemAcc;
        _displayValue = displayValue;
        _red = red;
        _green = green;
        _blue = blue;
        _hue = hue;
    }

    public String getBlue() {
        return _blue;
    }

    public void setBlue(String blue) {
        _blue = blue;
    }

    public String getDisplayValue() {
        return _displayValue;
    }

    public void setDisplayValue(String displayValue) {
        _displayValue = displayValue;
    }

    public String getGreen() {
        return _green;
    }

    public void setGreen(String green) {
        _green = green;
    }

    public String getId() {
        return _itemAcc;
    }

    public void setId(String itemAcc) {
        _itemAcc = itemAcc;
    }

    public String getRed() {
        return _red;
    }

    public void setRed(String red) {
        _red = red;
    }


    public Float getHue() {
        return _hue;
    }

    public int compareTo(Object o) {
        LegendItem li1 = (LegendItem) o;
        return this.getHue().compareTo(li1.getHue());
    }
}
