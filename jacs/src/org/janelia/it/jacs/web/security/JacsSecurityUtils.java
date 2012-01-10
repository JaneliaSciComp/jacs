
package org.janelia.it.jacs.web.security;

import org.janelia.it.jacs.model.user_data.User;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Nov 6, 2007
 * Time: 4:39:56 PM
 */
public class JacsSecurityUtils {
    public final static String USER_OBJECT_ATTR_NAME = "USER_OBJECT";

    public static boolean isAuthenticated(HttpServletRequest request) {
        User u = getSessionUser(request);
        return (u != null && u.getUserLogin() != null && !u.getUserLogin().equals("__CAMERA__ANONYMOUS__"));
    }

    public static boolean isAdmin(HttpServletRequest request) {
        User user = getSessionUser(request);
        return user != null && user.isAdministrator();
    }

    public static User getSessionUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(USER_OBJECT_ATTR_NAME);
    }
}
