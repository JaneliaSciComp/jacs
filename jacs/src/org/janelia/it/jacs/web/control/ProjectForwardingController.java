
package org.janelia.it.jacs.web.control;

import org.springframework.web.bind.RequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.BaseCommandController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: May 7, 2008
 * Time: 1:01:08 PM
 */
public class ProjectForwardingController extends BaseCommandController {

    protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        String projectSymbol = RequestUtils.getStringParameter(httpServletRequest, "projectSymbol");
        String relativeRedirectURL =
                "/gwt/BrowseProjectsPage/BrowseProjectsPage.oa" +
                        (projectSymbol != null && projectSymbol.length() > 0 ? "?projectSymbol=" + projectSymbol : "");
        RedirectView rv = new RedirectView(relativeRedirectURL, true);
        return new ModelAndView(rv);
    }

}
