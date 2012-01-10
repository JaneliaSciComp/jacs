
package org.janelia.it.jacs.web.gwt.common.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.web.gwt.common.client.service.DownloadEchoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class DownloadEchoServiceImpl extends JcviGWTSpringController implements DownloadEchoService {

    static Logger logger = Logger.getLogger(DownloadEchoServiceImpl.class.getName());

    public static final String POSTED_DATA_ATTRIBUTE_NAME = "postedData";

    /**
     * Accepts data that can be streamed back later;  used to allow user to download data that
     * is available on the client side (in the browser).
     *
     * @return unique identifier for retrieval of the data
     * @data data to cache
     */
    public String postData(String data) {
        logger.debug("DownloadEchoService.postData()");

        // Stuff the data in the session
        HttpServletRequest request = getThreadLocalRequest();
        HttpSession session = request.getSession(false);
        session.setAttribute(POSTED_DATA_ATTRIBUTE_NAME, data);

        return "ok";// TODO: change return type to void
    }
}
