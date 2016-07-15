package org.janelia.it.jacs.shared.utils;

import junit.framework.TestCase;

/**
 * Test for the FileUtil class.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileUtilTest extends TestCase {

    public void testGetExtension() {
        assertEquals("", FileUtil.getExtension("/test/my.thing/thing"));
        assertEquals("tar", FileUtil.getExtension("/test/my.thing/thing.tar"));
        assertEquals("tar.gz", FileUtil.getExtension("/test/my.thing/thing.tar.gz"));
        assertEquals("lsm.bz2", FileUtil.getExtension("/test/my.thing/thing.lsm.bz2"));
    }

    public void testGetBasename() {
        assertEquals("/test/my.thing/thing", FileUtil.getBasename("/test/my.thing/thing"));
        assertEquals("/test/my.thing/thing", FileUtil.getBasename("/test/my.thing/thing.tar"));
        assertEquals("/test/my.thing/thing", FileUtil.getBasename("/test/my.thing/thing.tar.gz"));
        assertEquals("/test/my.thing/thing", FileUtil.getBasename("/test/my.thing/thing.lsm.bz2"));
    }

}
