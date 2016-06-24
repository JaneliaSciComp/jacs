package org.janelia.it.jacs.compute.wsrest.security;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.DefaultPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by schauderd on 6/26/15.
 */
public class LDAPProvider {
    private String url;
    private String groupAttribute;
    private HashMap<String,Set<String>> cachedMembership = new HashMap<>();
    private Dn groupDN;
    private Dn baseDN;

    private static final Logger logger = LoggerFactory.getLogger(LDAPProvider.class);

    DefaultPoolableLdapConnectionFactory factory;
    LdapConnectionPool pool;

    public LDAPProvider() {
        init();
    }

    public void init() {
        LdapConnectionConfig config = new LdapConnectionConfig();
        setBaseDN(SystemConfigurationProperties.getString("LDAP.BaseDN"));
        setGroupDN(SystemConfigurationProperties.getString("LDAP.GroupDN"));
        setGroupAttribute(SystemConfigurationProperties.getString("LDAP.GroupAttribute"));
        String url  = SystemConfigurationProperties.getString("LDAP.URL");
        url = url.replaceAll(":389","");
        config.setLdapHost( url );
        config.setLdapPort(389);
        config.setTimeout(1000);
        factory = new DefaultPoolableLdapConnectionFactory( config );
        pool = new LdapConnectionPool( factory );
    }

    // perform a bind and then check group membership in LDAP group jacsdata
    public boolean login(Token credentials) throws RuntimeException {
        // logic for basic auth
        String username;
        if (credentials instanceof BasicAuthToken) {
            username = ((BasicAuthToken) credentials).getUsername();
        } else {
            return false;
        }

        LdapConnection connection = null;
        try {
            connection = pool.getConnection();
            String userDN = "cn=" + username + "," + baseDN;
            connection.bind(userDN, ((BasicAuthToken) credentials).getPassword());
            SearchRequest req = new SearchRequestImpl();
            req.setScope(SearchScope.SUBTREE);
            req.setTimeLimit(0);
            req.setBase(groupDN);
            req.setFilter("(" + groupAttribute + "=" + username + ")");
            SearchCursor searchCursor = connection.search(req);
            if (searchCursor.next()) {
                return true;
            } else return false;
        } catch (LdapException le) {
            throw new RuntimeException ("Problems connecting to LDAP Resource");
        } catch (CursorException e) {
            throw new RuntimeException ("Problems checking LDAP authorization to resource " + groupDN + " for user " + username);
        } finally {
            try {
                connection.unBind();
                pool.releaseConnection(connection);
            } catch (LdapException le) {
                throw new RuntimeException ("Problems closing LDAP connection");
            }
        }
    }

    // perform a bind and then check group membership in LDAP group jacsdata
    public Subject generateSubjectInfo (String username) throws RuntimeException {
        LdapConnection connection = null;
        Subject newUser = new Subject();
        try {
            connection = pool.getConnection();
            EntryCursor cursor = connection.search( baseDN, "(cn="+username+")", SearchScope.ONELEVEL );

            while ( cursor.next() )
            {
                Entry entry = cursor.get();
                newUser.setEmail(entry.get("mail").getString());
                newUser.setFullName(entry.get("givenName").getString() + " " + entry.get("sn").getString());
                HashSet<String> baseGroups = new HashSet<String>();
                baseGroups.add("group:workstation_users");
                newUser.setGroups(baseGroups);
                newUser.setKey("user:" + username);
                newUser.setName(username);
            }

            cursor.close();
            return newUser;
        } catch (LdapException le) {
            throw new RuntimeException ("Problems connecting to LDAP Resource");
        } catch (CursorException e) {
            throw new RuntimeException ("Problems retrieving LDAP information for user " + username);
        } finally {
            try {
                connection.unBind();
                pool.releaseConnection(connection);
            } catch (LdapException le) {
                throw new RuntimeException ("Problems closing LDAP connection");
            }
        }
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Dn getGroupDN() {
        return groupDN;
    }

    public void setGroupDN(String group)  {
        try {
            this.groupDN = new Dn(group);
        } catch (LdapInvalidDnException e) {
            logger.error("There was a problem with your dn format" + e.getMessage());
        }
    }

    public Dn getBaseDN() {
        return baseDN;
    }

    public void setBaseDN(String base) {
        try {
            this.baseDN = new Dn(base);
        } catch (LdapInvalidDnException e) {
            logger.error("There was a problem with your dn format" + e.getMessage());
        }
    }

    public String getGroupAttribute() {
        return groupAttribute;
    }

    public void setGroupAttribute(String groupAttribute) {
        this.groupAttribute = groupAttribute;
    }
}
