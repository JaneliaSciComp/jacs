package org.janelia.it.jacs.shared.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 * Tests for the StringUtils class.
 *
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
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

    public void testIsEmpty() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty("  "));
        assertFalse(StringUtils.isEmpty(" a "));
    }

    public void testIsBlank() {
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank("  "));
        assertFalse( StringUtils.isBlank(" a "));
    }

    public void testDefaultIfEmpty() {
        assertEquals("Default", StringUtils.defaultIfNullOrEmpty(null, "Default"));
        assertEquals("Default", StringUtils.defaultIfNullOrEmpty("", "Default"));
        assertEquals("NonDefault", StringUtils.defaultIfNullOrEmpty("NonDefault", "Default"));
    }

    public void testEmptyIfNull() {
        assertEquals("", StringUtils.emptyIfNull(null));
        assertEquals("Test", StringUtils.emptyIfNull("Test"));
    }

    public void testAbbreviate() {
        assertEquals("Abbr...", StringUtils.abbreviate("Abbreviate", 7));
    }

    public void testAreEqual() {
        Object o1 = new Integer(1);
        Object o2 = new Integer(1);
        Object o3 = new Integer(2);
        assertTrue(StringUtils.areEqual(null, null));
        assertTrue(StringUtils.areEqual(o1, o1));
        assertTrue(StringUtils.areEqual(o1, o2));
        assertFalse(StringUtils.areEqual(o1, o3));
    }

    public void testAreAllEmpty() {
        List<String> strs = new ArrayList<>();
        strs.add("");
        strs.add("");
        strs.add("");
        assertTrue(StringUtils.areAllEmpty(strs));
        strs.add(" ");
        assertFalse(StringUtils.areAllEmpty(strs));
    }

    public void testGetIndent() {
        assertEquals("    ", StringUtils.getIndent(4, " "));
        assertEquals("-*-*", StringUtils.getIndent(2, "-*"));
    }

    public void testUnderscoreToTitleCase() {
        assertEquals("Test This Case", StringUtils.underscoreToTitleCase("test_this_case"));
        assertEquals("Test", StringUtils.underscoreToTitleCase("test"));
        assertEquals("Test Two", StringUtils.underscoreToTitleCase("test__two"));
    }

    public void testGetCommaDelimited() {
        assertEquals("a", StringUtils.getCommaDelimited("a"));
        assertEquals("a, b, c", StringUtils.getCommaDelimited("a", "b", "c"));
        assertEquals("a...", StringUtils.getCommaDelimited(Arrays.asList("a", "b", "c"), 4));
    }

    public void testEncodeToHex() {
        String result = StringUtils.encodeToHex( STEP_UP_COLOR_EXAMPLE );
        assertEquals( "Failed step-up test ", result, STEP_UP_RESULT );
        result = StringUtils.encodeToHex( ALL_HEX_COLOR_EXAMPLE );
        assertEquals( "Failed all-hex test ", ALL_HEX_RESULT, result );
        result = StringUtils.encodeToHex( ZEROS_EXAMPLE );
        assertEquals( "Failed zeros test ", ZEROS_RESULT, result );
    }

    public void testIteratedName() {
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
