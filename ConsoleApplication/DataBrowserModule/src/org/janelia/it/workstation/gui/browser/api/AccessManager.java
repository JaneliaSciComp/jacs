package org.janelia.it.workstation.gui.browser.api;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.enums.SubjectRole;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.framework.console.Browser;
import org.janelia.it.workstation.api.stub.data.FatalCommError;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.workstation.shared.util.PropertyConfigurator;
import org.janelia.it.workstation.api.stub.data.SystemError;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.LoginProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AccessManager {
    private static final Logger log = LoggerFactory.getLogger(AccessManager.class);

    public static String RUN_AS_USER = "RunAs";
    public static String USER_NAME = LoginProperties.SERVER_LOGIN_NAME;
    public static String USER_PASSWORD = LoginProperties.SERVER_LOGIN_PASSWORD;

    private static final AccessManager accessManager = new AccessManager();
    private boolean isLoggedIn;
    private Subject loggedInSubject;
    private Subject authenticatedSubject;

    private Long currentSessionId;

    private AccessManager() {
        log.info("Initializing Access Manager");

        String tempLogin = (String) SessionMgr.getSessionMgr().getModelProperty(USER_NAME);
        String tempPassword = (String) SessionMgr.getSessionMgr().getModelProperty(USER_PASSWORD);
        if (tempLogin != null && tempPassword != null) {
            PropertyConfigurator.getProperties().setProperty(USER_NAME, tempLogin);
            PropertyConfigurator.getProperties().setProperty(USER_PASSWORD, tempPassword);
        }

    }

    static public AccessManager getAccessManager() {
        return accessManager;
    }

    public boolean loginSubject(String username, String password) {
        try {
            boolean relogin = false;

            if (isLoggedIn()) {
                logoutUser();
                log.info("RELOGIN");
                relogin = true;
            }

            //findAndRemoveWindowsSplashFile();
            // Login and start the session
            authenticatedSubject = authenticateSubject(username, password);
            System.out.println ("TRACE 2");

            System.out.println (authenticatedSubject);
            if (null != authenticatedSubject) {
                System.out.println ("TRACE 1");
                isLoggedIn = true;                
                loggedInSubject = authenticatedSubject;
                log.info("Authenticated as {}", authenticatedSubject.getKey());

                beginSession();
                if (relogin) {
                    resetSession();
                }
            }

            return isLoggedIn;
        }
        catch (Exception e) {
            isLoggedIn = false;
            log.error("Error logging in", e);
            throw new FatalCommError(ConsoleProperties.getInstance().getProperty("interactive.server.url"),
                    "Cannot authenticate login. The server may be down. Please try again later.");
        }
    }

    private Subject authenticateSubject (String username, String password) {
        // make RESTful call to authenticate user
        final String user = username;
        final String pw = password;

        try {
            Subject authenticatedSubject = DomainMgr.getDomainMgr().getModel().loginSubject(username, password);

            if (authenticatedSubject!=null) {
                log.info("Setting default authenticator");
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user,
                                pw.toCharArray());
                    }
                });
                SessionMgr.getSessionMgr().getWebDavClient().setCredentialsUsingAuthenticator();
            }
            return authenticatedSubject;
        } catch (Exception e) {
            log.error("Problem getting the subject using key " + username);
        }
        return null;
    }

    private void beginSession () {
        // TO DO: add to eventBus server logging
    }

    private void endSession () {
        // TO DO: add to eventBus server logging
    }

    public boolean setRunAsUser(String runAsUser) {
        
        if (!AccessManager.authenticatedSubjectIsInGroup(SubjectRole.Admin) && !StringUtils.isEmpty(runAsUser)) {
            throw new IllegalStateException("Non-admin user cannot run as another user");
        }
        
        try {
            if (!StringUtils.isEmpty(runAsUser)) {
                // set subject from RESTful service
                Subject runAsSubject = DomainMgr.getDomainMgr().getModel().getSubjectByKey(runAsUser);
                if (runAsSubject==null) {
                    return false;
                }
                loggedInSubject = runAsSubject;
            }
            else {
                loggedInSubject = authenticatedSubject;
            }

            if (!authenticatedSubject.getId().equals(loggedInSubject.getId())) {
                log.info("Authenticated as {} (Running as {})", authenticatedSubject.getKey(), loggedInSubject.getId());
            }
                
            resetSession();
            return true;
        }
        catch (Exception e) {
            loggedInSubject = authenticatedSubject;
            SessionMgr.getSessionMgr().handleException(e);
            return false;
        }
    }
    
    private void resetSession() {
        final Browser browser = SessionMgr.getBrowser();
        if (browser != null) {
            log.info("Refreshing all views");
            browser.resetView();
        }
        log.info("Resetting model");
        ModelMgr.getModelMgr().reset();
        SessionMgr.getSessionMgr().getSessionModel().removeAllBrowserModels();
    }
    
    public void logoutUser() {
        try {
            if (loggedInSubject != null) {
                endSession();
                log.info("Logged out with: {}", loggedInSubject.getKey());
            }
            isLoggedIn = false;
            loggedInSubject = null;
            authenticatedSubject = null;
        }
        catch (Exception e) {
            log.error("Error logging out", e);
        }
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setSubject(Subject subject) {
        this.loggedInSubject = subject;
    }

    public Subject getSubject() {
        return loggedInSubject;
    }

    public Subject getAuthenticatedSubject() {
        return authenticatedSubject;
    }

    public static Subject getSubjectByKey (String key) {
        try {
            return DomainMgr.getDomainMgr().getModel().getSubjectByKey(key);
        } catch (Exception e) {
            log.error("Error getting Subject Key: " + key + ", ", e);
        }
        return null;
    }

    public static boolean authenticatedSubjectIsInGroup(SubjectRole role) {
        Subject subject = AccessManager.getAccessManager().getAuthenticatedSubject();
        return subject.getGroups().contains(role.getRole());
    }

    public static boolean currentUserIsInGroup(String groupName) {
        Subject subject = AccessManager.getAccessManager().getSubject();
        return subject.getGroups().contains(groupName);
    }

    public static List<String> getSubjectKeys() {
        List<String> subjectKeys = new ArrayList<>();
        Subject subject = AccessManager.getAccessManager().getSubject();
        if (subject != null) {
            subjectKeys.add(subject.getKey());
            subjectKeys.addAll(subject.getGroups());
        }
        return subjectKeys;
    }

    public static String getSubjectKey() {
        Subject subject = getAccessManager().getSubject();
        if (subject == null) {
            throw new SystemError("Not logged in");
        }
        return subject.getKey();
    }

    public static String getUsername() {
        Subject subject = getAccessManager().getSubject();
        if (subject == null) {
            throw new SystemError("Not logged in");
        }
        return subject.getName();
    }

    public static String getUserEmail() {
        Subject subject = getAccessManager().getSubject();
        if (subject == null) {
            throw new SystemError("Not logged in");
        }
        return subject.getEmail();
    }

    public Long getCurrentSessionId() {
        return currentSessionId;
    }

    public void setCurrentSessionId(Long currentSessionId) {
        this.currentSessionId = currentSessionId;
    }
}
