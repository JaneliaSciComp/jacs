
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
