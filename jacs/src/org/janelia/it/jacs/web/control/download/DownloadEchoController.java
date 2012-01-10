
package org.janelia.it.jacs.web.control.download;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.web.gwt.common.server.DownloadEchoServiceImpl;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Spring controller to return a tab-delimited text version of a GWT SortableTable.  The table sends its data to
 * this servlet, which streams it back to the browser so the user can save as a CSV or tab-delimited file.
 *
 * @author Michael Press
 */
public class DownloadEchoController implements Controller {

    private static Logger logger = Logger.getLogger(DownloadEchoController.class.getName());

    public static final String SUGGESTED_FILENAME_PARAM = "suggestedFilename";
    public static final String SUGGESTED_CONTENT_TYPE_PARAM = "contentType";
    private static final String DEFAULT_CONTENT_TYPE = "text/csv"; // TODO: default to text?

    /**
     * Implementation for Controller. Accepts data to export from the client, and resends on the HttpResponse
     */
    //TODO: is this multi-user thread-safe?
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("DownloadTableController.handleRequest()");

        // Get the data stored in the session
        String suggestedFileName = request.getParameter(SUGGESTED_FILENAME_PARAM);
        String contentType = request.getParameter(SUGGESTED_CONTENT_TYPE_PARAM);
        if (contentType == null)
            contentType = DEFAULT_CONTENT_TYPE;
        String postedData = (String) request.getSession().getAttribute(DownloadEchoServiceImpl.POSTED_DATA_ATTRIBUTE_NAME);

        // Remove the data from the session 
        request.removeAttribute(DownloadEchoServiceImpl.POSTED_DATA_ATTRIBUTE_NAME);

        // Return the 
        doDownload(response, loadData(postedData), suggestedFileName, contentType);

        return null; // Signal: am handling this operation here.
    }

    /**
     * We're returning the posted data, so no conversion is necessary
     */
    protected String loadData(String postedData) {
        return postedData;
    }

    /**
     * Sends its input to the ServletResponse output stream.
     */
    private void doDownload(HttpServletResponse response, String data, String suggestedFileName, String contentType)
            throws IOException, ServletException {
        response.setContentType(contentType);
        response.setStatus(200);

        // These are needed to avoid a download bug in IE.
        //TODO: push to superclass
        response.setDateHeader("Expires", 0);
        response.setHeader("Pragma", "public");
        response.setHeader("Cache-Control", "max-age=0");
        response.setHeader("Content-Disposition", "attachment;filename=\"" + suggestedFileName + "\"");

        PrintWriter responseWriter = response.getWriter();

        try {
            // Output the input
            responseWriter.print(data);
        }
        finally {
            responseWriter.flush();
            responseWriter.close();
        }

    }
}
