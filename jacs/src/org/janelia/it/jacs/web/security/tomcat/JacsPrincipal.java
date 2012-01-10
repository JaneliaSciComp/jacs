
package org.janelia.it.jacs.web.security.tomcat;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Oct 4, 2006
 * Time: 6:53:03 PM
 */
public class JacsPrincipal implements java.security.Principal {
    String m_Name = new String("");

    public JacsPrincipal(String name) {
        m_Name = name;
    }

    public boolean equals(Object another) {
        try {
            JacsPrincipal pm = (JacsPrincipal) another;
            return pm.m_Name.equalsIgnoreCase(m_Name);
        }
        catch (Exception e) {
            return false;
        }
    }

    public String getName() {
        return m_Name;
    }

    public int hashCode() {
        return m_Name.hashCode();
    }

    public String toString() {
        return m_Name;
    }
}
