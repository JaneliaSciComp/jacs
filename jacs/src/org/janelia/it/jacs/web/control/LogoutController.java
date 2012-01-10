
package org.janelia.it.jacs.web.control;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.BaseCommandController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Jan 10, 2007
 * Time: 10:30:36 AM
 */
public class LogoutController extends BaseCommandController {

    protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        httpServletRequest.getSession().invalidate();
        return new ModelAndView(new RedirectView("/forward.html", true));
    }
}
