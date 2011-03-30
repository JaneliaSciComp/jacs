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

import org.apache.catalina.Container;
import org.apache.catalina.Server;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.catalina.realm.JNDIRealm;
import org.apache.log4j.Logger;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.servlet.http.HttpServletRequest;
import java.util.Hashtable;

public class JacsPrincipal {

    HttpServletRequest request = null;

    public JacsPrincipal(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * This method connects to LDAP and retrieves the email address of a user based on the user id.
     * Since, it connects to LDAP, this method should be used cautiously.
     *
     * @return Email address
     */
    public String getEmailAddress() {

        String mail = null;
        try {
            Server server = ServerFactory.getServer();
            //Note, this assumes the Container is "Catalina"
            Service service = server.findService("Catalina");
            Container engine = service.getContainer();
            JNDIRealm realm = (JNDIRealm) engine.getRealm();

            // Connect to LDAP server.
            Hashtable<String, String> env = new Hashtable<String, String>(2);
            env.put(Context.INITIAL_CONTEXT_FACTORY, realm.getContextFactory());
            env.put(Context.PROVIDER_URL, realm.getConnectionURL());

            InitialDirContext ctx = null;
            ctx = new InitialDirContext(env);

            // beacause our LDAP is structured wierdly we cannot just look up
            // an entry, we have to serach for it through the whole directory
            SearchControls ss = new SearchControls();
            ss.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration answer = ctx.search(realm.getUserBase(), "uid=" + getName(), ss);
            while (answer.hasMore()) {
                SearchResult sr = (SearchResult) answer.next();
                try {
                    Attributes attrs = sr.getAttributes();
                    mail = attrs.get("mail").get().toString();
                    if (mail != null && mail.length() > 0)
                        break;
                }
                catch (Exception e) {
                    // mo such attribute - look at the next one
                }
            }
        }
        catch (Throwable e) {
            Logger logger = Logger.getLogger(this.getClass());
            logger.error("Error getting the email address.  Returning nothing.", e);
            mail = "";
        }

        return mail;
    }

    /**
     * This method returns the user name from the Principal object.
     *
     * @return username
     */
    public String getName() {
        return request.getUserPrincipal().getName();
    }
}
