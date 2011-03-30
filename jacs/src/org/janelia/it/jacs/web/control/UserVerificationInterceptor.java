/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.control;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.server.access.UserDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.web.security.JacsOpenAccessGamaSsoFilter;
import org.janelia.it.jacs.web.security.JacsSecurityUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;

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

        if (principal == null) {
            logger.error("Attempted to execute controller without valid principal");
            return false;
        }

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

                if (!sessionUser.getUserLogin().equals(JacsOpenAccessGamaSsoFilter.CAMERA_ANONYMOUS_USER_ID)) {
                    // authentication takes precedence - must reload user
                    logger.error("Caught mismatch between GAMA principal (" + userLogin + ") and session user (" + sessionUser.getUserLogin() + ")");
                    // invalidate session, and send user to home page
                    httpServletRequest.getSession().invalidate();
                    sessionUser = null;
                }
            }
            else // all is well and loadded, may return
                return true;

        }

        try {
            sessionUser = obtainUser(userLogin, userLogin, principal);
            if (httpServletRequest.isUserInRole("jacs-admin"))
                sessionUser.setAdministrator(true);
        }
        catch (Exception e) {
            return false;
        }
        // store in the session
        httpServletRequest.getSession().setAttribute(JacsSecurityUtils.USER_OBJECT_ATTR_NAME, sessionUser);
        return true;
    }

    protected User obtainUser(String userLoginId, String userName) throws Exception {
        return obtainUser(userLoginId, userName, (Long) null);
    }

    protected User obtainUser(String userLoginId, String userName, Long id) throws Exception {
        try {
            User user = userDAO.getUserByName(userLoginId);
            if (null == user) {
                logger.info("Creating user " + userLoginId);
                if (id == null)
                    user = userDAO.createUser(userLoginId, userName);
                else
                    user = userDAO.createUserWithID(id, userLoginId, userName);

                boolean successful = computeServerBean.buildUserFilestoreDirectory(userLoginId);
                if (!successful) {
                    // will not be able to execute any computes, so throw an exception
                    throw new IOException("Unable to create directory for user " + userLoginId);
                }
            }
            return user;
        }
        catch (DaoException e) {
            logger.error("DAOException: " + e.getMessage(), e);
            throw e;
        }
        catch (RemoteException e) {
            logger.error("RemoteException: " + e.getMessage(), e);
            throw e;
        }
        catch (Exception e) {
            logger.error("Exception: " + e.getMessage(), e);
            throw e;
        }
    }

    protected User obtainUser(String userLoginId, String userName, JacsPrincipal vp) throws Exception {
        try {
            User user = userDAO.getUserByName(userLoginId);
            if (null == user) {
                String email = vp.getEmailAddress();
                logger.info("Creating user " + userLoginId);
                user = userDAO.createUser(userLoginId, userName, email);
                boolean successful = computeServerBean.buildUserFilestoreDirectory(userLoginId);
                if (!successful) {
                    // will not be able to execute any computes, so throw an exception
                    throw new IOException("Unable to create directory for user " + userLoginId);
                }
            }
            else {
                if (user.getEmail() == null) {
                    String email = vp.getEmailAddress();
                    user.setEmail(email);
                    userDAO.saveOrUpdateUser(user);
                }
            }

            return user;
        }
        catch (DaoException e) {
            logger.error("DAOException: " + e.getMessage(), e);
            throw e;
        }
        catch (RemoteException e) {
            logger.error("RemoteException: " + e.getMessage(), e);
            throw e;
        }
        catch (Exception e) {
            logger.error("Exception: " + e.getMessage(), e);
            throw e;
        }
    }
}
