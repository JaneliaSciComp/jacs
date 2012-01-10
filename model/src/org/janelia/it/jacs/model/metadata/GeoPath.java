
package org.janelia.it.jacs.model.metadata;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 5, 2006
 * Time: 3:43:13 PM
 */
public class GeoPath extends CollectionSite implements IsSerializable {

    private List<GeoPoint> points = new ArrayList();

    public GeoPath() {
    }

    public GeoPath(List<GeoPoint> points) {
        this.points = points;
    }

    public List<GeoPoint> getPoints() {
        return points;
    }

    public void setPoints(List<GeoPoint> points) {
        this.points = points;
    }

    public void addPoint(GeoPoint newPoint) {
        points.add(newPoint);
    }
}
