
package org.janelia.it.jacs.web.gwt.common.client.wizard;

/**
 * Defines a transition from one page to another (and implicitly back)
 *
 * @author Michael Press
 */
public class PageTransition {
    private String _name;
    private String _startPage;
    private String _destPage;

    public PageTransition(String transitionName, String startPageToken, String destPageToken) {
        _name = transitionName;
        _startPage = startPageToken;
        _destPage = destPageToken;
    }

    public String getName() {
        return _name;
    }

    public String getStartPageToken() {
        return _startPage;
    }

    public String getDestPageToken() {
        return _destPage;
    }
}