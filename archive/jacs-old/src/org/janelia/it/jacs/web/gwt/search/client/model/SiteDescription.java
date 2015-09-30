
package org.janelia.it.jacs.web.gwt.search.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 2, 2007
 * Time: 3:56:48 PM
 */
public class SiteDescription implements Serializable, IsSerializable {
    private String location;
    private String longitude;
    private String latitude;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getFormattedLatitude() {
        return latitude.replace('d', '\u00B0');
    }

    public String getFormattedLongitude() {
        return longitude.replace('d', '\u00B0');
    }
}
