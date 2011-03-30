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

package org.janelia.it.jacs.web.security.tomcat;

import org.apache.log4j.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Oct 4, 2006
 * Time: 6:46:08 PM
 */
public class FakeLoginModule implements LoginModule {
    private static Logger log = Logger.getLogger(FakeLoginModule.class);

    protected CallbackHandler callbackHandler = null;
    protected boolean committed = false;
    protected boolean debug = false;
    protected Map options = null;
    protected Principal principal = null;
    protected Map sharedState = null;
    protected Subject subject = null;

    public boolean abort() throws LoginException {
        log.info("abort");
        return (true);
    }

    public boolean commit() throws LoginException {
        log.info("commit phase");
        // If authentication was not successful, just return false
        if (principal == null) {
            log.error("no principal commit fails");
            return (false);
        }
        if (!subject.getPrincipals().contains(principal))
            subject.getPrincipals().add(principal);
        // add role principals
        // NOTE!!!!!!!!: This role better be in the web.xml file that is used!  (web.jaas.xml in this case)
        subject.getPrincipals().add(new JacsRolePrincipal("user"));
        committed = true;
        log.info("commit succesful");
        return (true);
    }

    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map sharedState, Map options) {

        // Save configuration values
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

    }

    public boolean login() throws LoginException {
        log.info("login phase");
        // Set up our CallbackHandler requests
        if (callbackHandler == null)
            throw new LoginException("No CallbackHandler specified");
        Callback callbacks[] = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);

        // Interact with the user to retrieve the username and password
        String username = null;
        String password = null;
        try {
            callbackHandler.handle(callbacks);
            username = ((NameCallback) callbacks[0]).getName();
            password =
                    new String(((PasswordCallback) callbacks[1]).getPassword());
        }
        catch (IOException e) {
            throw new LoginException(e.toString());
        }
        catch (UnsupportedCallbackException e) {
            throw new LoginException(e.toString());
        }
        if (!authenticate(username, password))
            return false;
        principal = new JacsPrincipal(username);
        return true;
    }

    public boolean logout() throws LoginException {

        subject.getPrincipals().remove(principal);
        committed = false;
        principal = null;
        return (true);

    }

    boolean authenticate(String s, String p) {
        // NO AUTHENTICATION!!!
        return true;
    }

    static public void main(String args[]) throws Exception {
        LoginContext ctx = new LoginContext("TomCatAdminApplication");
        ctx.login();
    }
}

