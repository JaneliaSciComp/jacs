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

package org.janelia.it.jacs.web.gwt.common.server;

import org.gwtwidgets.server.spring.GWTSpringController;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.collection.GWTEntityCleaner;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.web.security.JacsSecurityUtils;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Oct 9, 2006
 * Time: 3:28:46 PM
 */
public class JcviGWTSpringController extends GWTSpringController {

    private User user = null;
    private SessionFactory sessionFactory;
    private TaskDAO taskDao;

    public User getSessionUser() {
        return user;
    }

    public ModelAndView handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        user = JacsSecurityUtils.getSessionUser(httpServletRequest);
        return super.handleRequest(httpServletRequest, httpServletResponse);
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void setTaskDao(TaskDAO taskDao) {
        this.taskDao = taskDao;
    }

    protected void cleanForGWT(Object obj) {
        Session session = SessionFactoryUtils.getSession(sessionFactory, false);
        GWTEntityCleaner.evictAndClean(obj, session);
    }

    protected void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        validateUserByLogin(user.getUserLogin());
    }

    protected void validateUserByTaskId(Long taskId) {
        try {
            Task task = taskDao.getTaskById(taskId);
            validateUserByLogin(task.getOwner());
        }
        catch (DaoException e) {
            throw new RuntimeException("Could not find task " + taskId);
        }
    }

    protected void validateUserByTaskId(String taskId) {
        validateUserByTaskId(new Long(taskId));
    }

    /**
     * Throws exception if userlogin doesn't match session user login
     *
     * @param userLogin
     */
    protected void validateUserByLogin(String userLogin) {
        String loggedInUserLogin = getSessionUser().getUserLogin();
        if (!loggedInUserLogin.equals(userLogin)) {
            throw new SecurityException("requesting info for user " + userLogin + " while actually logged in as " + loggedInUserLogin);
        }
    }
}
