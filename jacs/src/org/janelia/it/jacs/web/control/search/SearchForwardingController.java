
package org.janelia.it.jacs.web.control.search;

import org.janelia.it.jacs.server.access.NodeDAO;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.BaseCommandController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 28, 2007
 * Time: 4:40:37 PM
 */
public class SearchForwardingController extends BaseCommandController {
    private NodeDAO _nodeDAO;

    protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        String keyword = RequestUtils.getStringParameter(httpServletRequest, "keyword");
        String relativeRedirectURL =
                "/gwt/Search/Search.oa" +
                        (keyword != null && keyword.length() > 0 ? "?keyword=" + keyword : "");
        RedirectView rv = new RedirectView(relativeRedirectURL, true);
        return new ModelAndView(rv);
    }

    public void setNodeDAO(NodeDAO nodeDAO) {
        _nodeDAO = nodeDAO;
    }

}
