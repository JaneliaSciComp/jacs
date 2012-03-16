
package org.janelia.it.jacs.web.control;

import com.sun.security.auth.UserPrincipal;
import org.janelia.it.jacs.model.user_data.User;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Nov 6, 2007
 * Time: 2:45:29 PM
 */
public class OpenAccessUserVerificationInterceptor extends UserVerificationInterceptor {
    private static final Long ANONYMOUS_USER_DB_ID = new Long(100L);

    protected User obtainUser(String userLogin) throws Exception {
        if ("__ANONYMOUS__".equals(userLogin)) {
            return super.obtainUser(userLogin);
//            User u = new User();
//            u.setUserId(ANONYMOUS_USER_DB_ID);
//            return u;
        }
        else
            return super.obtainUser(userLogin);    //use real one
    }

    protected Principal getPrincipal(HttpServletRequest request) {
        Principal p = request.getUserPrincipal();
        if (request.getRequestURL().indexOf("healthMonitor.chk") >= 0) {
            // Specifies the time, in seconds, between client requests before the servlet container will invalidate
            // this session. A negative time indicates the session should never timeout.
            // We don't want healthCheck sessions around forever in the server.  It's bad.
            request.getSession().setMaxInactiveInterval(30);
        }
        if (p == null) {
            // generate one out of thin air to allow anonymous access
            p = new UserPrincipal("__ANONYMOUS__");
        }

        return p;
    }
}
