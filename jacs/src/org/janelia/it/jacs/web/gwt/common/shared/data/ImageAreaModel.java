
package org.janelia.it.jacs.web.gwt.common.shared.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Sep 26, 2007
 * Time: 9:13:01 AM
 */
public class ImageAreaModel implements Serializable, IsSerializable {

    private String shape;
    private String coordinates;
    private String tips;
    private String areaTitle;

    public ImageAreaModel() {
    }

    public String getAreaTitle() {
        return areaTitle;
    }

    public void setAreaTitle(String areaTitle) {
        this.areaTitle = areaTitle;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String coordinates) {
        this.coordinates = coordinates;
    }

    public String getTips() {
        return tips;
    }

    public void setTips(String tips) {
        this.tips = tips;
    }

}
