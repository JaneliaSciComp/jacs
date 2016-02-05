package org.janelia.it.jacs.shared.utils;

import junit.framework.TestCase;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 5/8/13
 * Time: 12:45 PM
 *
 * Testing some string utility functions.
 */
public class StringUtilsTest extends TestCase {

    private static final String STEP_UP_COLOR_EXAMPLE = "   32 64 128";
    private static final String STEP_UP_RESULT = "204080";

    private static final String ALL_HEX_COLOR_EXAMPLE = "10 255 12  ";
    private static final String ALL_HEX_RESULT = "0AFF0C";

    private static final String ZEROS_EXAMPLE = "0 0 0";
    private static final String ZEROS_RESULT = "000000";

    public static final String COLOR_TABLE_STRING = "annotation-neuron-styles={\"2232632564906983592\":{\"color\":[204,0,0],\"visibility\":true}}";

    public void setUp() {}

    public void tearDown() {}

    public void testEncodeToHex() {
        String result = StringUtils.encodeToHex( STEP_UP_COLOR_EXAMPLE, null );
        assertEquals( "Failed step-up test ", result, STEP_UP_RESULT );
        result = StringUtils.encodeToHex( ALL_HEX_COLOR_EXAMPLE, null );
        assertEquals( "Failed all-hex test ", ALL_HEX_RESULT, result );
        result = StringUtils.encodeToHex( ZEROS_EXAMPLE, null );
        assertEquals( "Failed zeros test ", ZEROS_RESULT, result );
    }

    public void testEmptyIfNull() {
        String result = StringUtils.emptyIfNull( null );
        assertNotNull( result );
        assertEquals( "", result );
    }

    public void testIsEmpty() {
        boolean result = StringUtils.isEmpty( null );
        assertTrue( "Null not empty", result );

        result = StringUtils.isEmpty( "" );
        assertTrue( "Empty string not empty", result );
    }
    
    public void testJustName() {
        String justName = StringUtils.getIteratedName("myfile.swc", 1);
        assertEquals( "New name invalid.", "myfile_1.swc", justName );
    }
    
    public void testDigitSafeReplace() {
        String rtnVal = StringUtils.digitSafeReplace("[11111,1111,111]", "1111", "111");
        System.out.println(rtnVal);

        String modified = StringUtils.digitSafeReplace(COLOR_TABLE_STRING, "2232632564906983592", "2953896094652362322");
        System.out.println(modified);

        assertNotNull( "Failed to digit-safe-replace " + COLOR_TABLE_STRING );
        assertNotNull( "Failed to digit-safe-replace.", rtnVal);
        assertEquals( "Not expected value", "[11111,111,111]", rtnVal );
    }
}
