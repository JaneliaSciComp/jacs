
package org.janelia.it.jacs.web.control;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.server.access.UserDAO;
import org.janelia.it.jacs.web.security.JacsSecurityUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Dec 5, 2006
 * Time: 6:37:11 PM
 */
public class UserVerificationInterceptor extends HandlerInterceptorAdapter {
    private Logger logger = Logger.getLogger(this.getClass());

    private UserDAO userDAO;
    private ComputeBeanRemote computeServerBean;

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void setComputeServerBean(ComputeBeanRemote computeServerBean) {
        this.computeServerBean = computeServerBean;
    }

    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object object) throws Exception {
        JacsPrincipal principal = new JacsPrincipal(httpServletRequest);

//        if (principal == null) {
//            logger.error("Attempted to execute controller without valid principal");
//            return false;
//        }
//
        String userLogin = principal.getName();

        if (!StringUtils.hasText(userLogin)) {
            logger.error("Attempted to execute controller without valid user login");
            return false;
        }

        User sessionUser = (User) httpServletRequest.getSession().getAttribute(JacsSecurityUtils.USER_OBJECT_ATTR_NAME);
        if (sessionUser != null) {
            if (!userLogin.equals(sessionUser.getUserLogin())) {
                /** there are 3 cases possible:
                 *  1. anonymous -> authenticated
                 *  2. authenticated -> anonymous
                 *  3. authenticated -> authenticated
                 *  only in the first case we will keep user session intact.
                 */

                if (!sessionUser.getUserLogin().equals("__ANONYMOUS__")) {
                    // authentication takes precedence - must reload user
                    logger.error("Caught mismatch between principal (" + userLogin + ") and session user (" + sessionUser.getUserLogin() + ")");
                    // invalidate session, and send user to home page
                    httpServletRequest.getSession().invalidate();
                }
            }
            else // all is well and loadded, may return
                return true;

        }

        try {
            sessionUser = obtainUser(userLogin);
            if (httpServletRequest.isUserInRole("jacs-admin")) {
                sessionUser.setAdministrator(true);
            }
        }
        catch (Exception e) {
            return false;
        }
        // store in the session
        httpServletRequest.getSession().setAttribute(JacsSecurityUtils.USER_OBJECT_ATTR_NAME, sessionUser);
        return true;
    }

    protected User obtainUser(String userLogin) throws Exception {
        User user = userDAO.getUserByName(userLogin);
        if (null == user) {
            // will not be able to execute any computes, so throw an exception
            throw new IOException("Unable to login user " + userLogin);
        }
        return user;
    }

}
