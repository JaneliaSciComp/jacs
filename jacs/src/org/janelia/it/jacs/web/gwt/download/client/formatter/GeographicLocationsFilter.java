
package org.janelia.it.jacs.web.gwt.download.client.formatter;

import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;

import java.util.List;

/**
 * Filter: leave stuff out if the node does not come from a location on our locations list.
 * <p/>
 * User: Lfoster
 * Date: Nov 6, 2006
 * Time: 4:44:41 PM
 */
public class GeographicLocationsFilter implements DownloadableDataNodeFilter {
    private List _locationsList;

    /**
     * Filter constructor takes list of locations.  Or null.
     *
     * @param locations
     */
    public GeographicLocationsFilter(List locations) {
        setLocations(locations);
    }

    /**
     * Allow construction with no list.  Must set list to do any filtering.
     */
    public GeographicLocationsFilter() {

    }

    /**
     * The list of locations should be a list of strings.  They should represent
     * geographic locations--the full set that are included in this filter.
     *
     * @param locations all acceptable locations.
     */
    public void setLocations(List locations) {
        _locationsList = locations;
    }

    /**
     * Return true if the location associated with the node, is one of those in the current
     * locations list for this filter object.
     *
     * @param node what to test.
     * @return true if in filter/false if leave-it-out.
     */
    public boolean isAcceptable(DownloadableDataNode node) {
        if (_locationsList == null || _locationsList.size() == 0)
            return true;

        if (node.getSite() == null)
            return false;
        if (node.getSite().getGeographicLocation() == null)
            return false;

        String locationOfInterest = node.getSite().getGeographicLocation();
        if (locationOfInterest == null)
            return false;

        for (int i = 0; i < _locationsList.size(); i++) {
            if (locationOfInterest.equals(_locationsList.get(i))) {
                return true;
            }
        }

        return false;
    }
}
