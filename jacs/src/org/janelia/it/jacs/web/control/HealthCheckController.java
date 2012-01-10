
package org.janelia.it.jacs.web.control;

import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.server.access.UserDAO;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.BaseCommandController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Writer;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Mar 2, 2007
 * Time: 12:37:01 PM
 */
public class HealthCheckController extends BaseCommandController {
    private ComputeBeanRemote ejbProxy;

    public void setEjbProxy(ComputeBeanRemote remote) {
        this.ejbProxy = remote;
    }

    private UserDAO userDAO;

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    private String jacswebVersion;

    public void setAppVersion(String appVersion) {
        jacswebVersion = appVersion;
    }

    protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {

        // the fact that it's called shows that this server is fine :)

        // now let's check DB
        long userCount = userDAO.countAllUsers();

        // now let's check EJB
        String computeVersion = ejbProxy.getAppVersion();

        httpServletResponse.setContentType("text/html");
        Writer writer = httpServletResponse.getWriter();
        writer.write("<html><body>");
        writer.write("Vicsweb application version: " + jacswebVersion + "<br/>");
        writer.write("Compute application version: " + computeVersion + "<br/>");
        writer.write("Total users in DB: " + userCount + "<br/>");
        writer.write("</body></html>");
        writer.close();
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
