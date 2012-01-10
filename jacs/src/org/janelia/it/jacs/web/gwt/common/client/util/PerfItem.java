
package org.janelia.it.jacs.web.gwt.common.client.util;

/**
 * This class is used with PerfStats class to monitor amount of time that code sections take to complete
 *
 * @author Tareq Nabeel
 */
public class PerfItem {

    long startTime;
    long total;
    String name;

    public PerfItem(String name) {
        this.name = name;
    }
}
