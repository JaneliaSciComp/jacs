
package org.janelia.it.jacs.web.gwt.common.client.wizard;

import com.google.gwt.http.client.URL;
import org.gwtwidgets.client.util.WindowUtils;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * Handles URLs for the WizardController so WizardPages can add data to the URL such that the page can be
 * reconstituted via "deep entry", i.e. external linking without progressing through the Wizard.
 * <p/>
 * Manages parameters stored AFTER the anchor, which is probably not a valid form of URI.
 *
 * @author Michael Press
 */
public class WizardURLManager {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.wizard.WizardURLManager");

    WizardController _controller;

    protected WizardURLManager(WizardController controller) {
        _controller = controller;
    }

    /**
     * Determines the start page as specified by a pageToken anchor on the URL, or 0 if not specified.
     *
     * @param URL starting page url
     * @return int of page starting from
     */
    public int getStartPageFromURL(String URL) {
        int startPage = 0;

        String pageToken = getURLAnchor(URL);
        if (pageToken != null) {
            _logger.debug("--------------------Found initial page token " + pageToken);
            startPage = _controller.findPageByName(pageToken);
            // Make sure the page is valid
            if (_controller.isValidPage(startPage)) {
                _logger.info("Starting at page " + startPage);
            }
            else {
                _logger.error(startPage + " is not a valid page. Starting at page 0");
                startPage = 0;
            }
        }

        return startPage;
    }

    /**
     * Returns the anchor in the in the URL, not including the anchor symbol(#), and not including any
     * page-specific URL params after the anchor tag
     * <p/>
     * Example - returns "SecondPage" for URL "http://localhost/jacs/org....Status/Status.htm?param1=value1#SecondPage?pageParam1=pageValue1"
     *
     * @param url anchor
     * @return anchor string
     */
    public String getURLAnchor(String url) {
        String anchor = null;

        String decodedUrl = URL.decodeComponent(url);
        int anchorIndex = decodedUrl.indexOf("#");
        if (anchorIndex > 0 && decodedUrl.length() >= (anchorIndex + 1)) {
            anchor = decodedUrl.substring(anchorIndex + 1);

            // Strip off any URL params after the anchor tag
            int questionMark = anchor.indexOf("?");
            if (questionMark > 0 && anchor.length() >= (questionMark + 1))
                anchor = anchor.substring(0, questionMark);
        }

        return anchor;
    }

    /**
     * This extracts page-specific URL-params added by the page as part of the history token, NOT regular URL
     * params in the URL before the anchor.
     * <p/>
     * Example:  http://localhost/jacs/org....Status/Status.htm?param1=value1#pageToken?pageParam1=pageValue1
     * will extract the page param "pageParam1" with value "pageValue1", but NOT "param1=value1"
     */
    public void processURLParams() {
        String url = URL.decodeComponent(WindowUtils.getLocation().getHref());
        int anchorIndex = url.indexOf("#"); // make sure there's an anchor before the question mark
        int questionMark = url.lastIndexOf("?");
        String urlQuery = null;
        if (anchorIndex > 0 && questionMark > 0) {
            if (anchorIndex < questionMark) {
                urlQuery = url.substring(questionMark + 1);
            }
            else {
                urlQuery = url.substring(questionMark + 1, anchorIndex);
            }
        }
        else if (questionMark > 0) {
            urlQuery = url.substring(questionMark + 1);
        }
        if (urlQuery != null) {
            String[] kvPairs = urlQuery.split("&");
            for (String kvPair : kvPairs) {
                String[] kv = kvPair.split("=");
                if (kv.length > 1)
                    _controller.processURLParam(kv[0], kv[1]); // need to re-decode value?
                else
                    _controller.processURLParam(kv[0], "");
            }
        }
    }

}
