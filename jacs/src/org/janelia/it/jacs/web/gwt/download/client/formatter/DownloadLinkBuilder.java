
package org.janelia.it.jacs.web.gwt.download.client.formatter;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Oct 6, 2006
 * Time: 1:00:27 PM
 * <p/>
 * Create links usable for download.
 */
public class DownloadLinkBuilder {

    /**
     * Static class, only.
     */
    private DownloadLinkBuilder() {
    }

    /**
     * return a link to handoff to a file delivery service.
     */
    public static String getDownloadLink(String dataFileLocation) {

        // NOTE: may not use the system setting for file sep, because that has not
        // been implemented in the GWT emulation.
        int lastPathLeg = dataFileLocation.lastIndexOf("/");
        if (lastPathLeg == -1)
            lastPathLeg = dataFileLocation.lastIndexOf("\\");
        String dataFileNameOnly = null;
        if (lastPathLeg > -1)
            dataFileNameOnly = dataFileLocation.substring(lastPathLeg + 1);
        else
            dataFileNameOnly = dataFileLocation;

        int lastDotPos = dataFileNameOnly.lastIndexOf(".");
        dataFileNameOnly = dataFileNameOnly.replace('.', '_');
        dataFileNameOnly = dataFileNameOnly.substring(0, lastDotPos) + "." +
                dataFileNameOnly.substring(lastDotPos + 1);

        return "/jacs/fileDelivery.htm?inputfilename=" +
                dataFileLocation +
                "&suggestedfilename=" +
                dataFileNameOnly;
    }
}
