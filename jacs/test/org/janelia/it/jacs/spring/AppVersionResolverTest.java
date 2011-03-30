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

package org.janelia.it.jacs.spring;

import junit.framework.TestCase;

public class AppVersionResolverTest extends TestCase {
    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testVersionComparison() throws Exception {
        AppVersionResolver versionResolver=new AppVersionResolver();
        AppVersionResolver.AppVersionComparator vComp=
                new AppVersionResolver.AppVersionComparator();
        AppVersionResolver.AppVersion v1=versionResolver.parseVersion("1.9.0.rc9");
        AppVersionResolver.AppVersion v2=versionResolver.parseVersion("1.10");
        AppVersionResolver.AppVersion v3=versionResolver.parseVersion("1.9.0.rc10");
        AppVersionResolver.AppVersion v4=versionResolver.parseVersion("1.10");
        AppVersionResolver.AppVersion v5=versionResolver.parseVersion("1.99.0.rc10");
        AppVersionResolver.AppVersion v6=versionResolver.parseVersion("1.99.0.rc11");
        AppVersionResolver.AppVersion v7=versionResolver.parseVersion("1.10a.0.rc10");
        assertTrue(vComp.compare(v1,v2) < 0);
        assertTrue(vComp.compare(v2,v4) == 0);
        assertTrue(vComp.compare(v5,v6) < 0);
        assertTrue(vComp.compare(v6,v7) > 0);
    }

}
