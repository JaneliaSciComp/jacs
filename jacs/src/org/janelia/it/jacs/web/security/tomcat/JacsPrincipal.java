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
