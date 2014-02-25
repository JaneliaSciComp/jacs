package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_builder.VolumeDataBean;
import junit.framework.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/25/13
 * Time: 11:26 PM
 *
 * Test for the renderables-builder base class facility methods.
 *
 * Maintenance Note: if the down sampler is ever intentionally changed, such that the output below is supposed
 * to change from what it is at time of writing, simply verify that the "Report Not as Expected:" contents
 * are correct for the new design.  Then, for each test, log-scrape that output and insert it into the appropriate
 * constant definition.  Afterwards, when the test is re-run, the reports should again match.
 */
public class DownSamplerTest {

    private static final String REPORT_2_B =
            "TEST: 2-bytes per voxel/tube\n\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,1,0,1,0,1,0,0,\n" +
            "0,1,0,1,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,1,0,1,0,1,0,0,\n" +
            "0,1,0,1,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,1,0,1,0,0,0,0,\n" +
            "0,1,0,1,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "Length total=128\n" +
            "Dimensions are 4 x 4 x 4\n";

    private static final String REPORT_1_TUBE =
            "TEST: 1-byte-per-voxel/tube\n" +
            "\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,1,1,1,1,1,0,0,\n" +
            "0,1,1,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,1,1,1,1,1,0,0,\n" +
            "0,1,1,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,1,1,0,0,0,0,0,\n" +
            "0,1,1,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "Length total=128\n" +
            "Dimensions are 8 x 4 x 4\n";

    private static final String REPORT_1_CIRCLE =
            "TEST: 1-byte-per-voxel/circle\n" +
            "\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,1,1,1,0,0,0,\n" +
            "0,1,1,0,1,1,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,1,1,1,0,0,0,\n" +
            "0,1,1,0,1,1,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,1,1,1,0,0,0,\n" +
            "0,1,1,0,1,1,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "0,0,0,0,0,0,0,0,\n" +
            "Length total=128\n" +
            "Dimensions are 8 x 4 x 4\n";

    private static final HashMap<String,String> testNameVsOutput = new HashMap<String,String>();
    static {
        testNameVsOutput.put( "2-bytes per voxel/tube",  REPORT_2_B );
        testNameVsOutput.put( "1-byte-per-voxel/tube",   REPORT_1_TUBE );
        testNameVsOutput.put( "1-byte-per-voxel/circle", REPORT_1_CIRCLE );
    }

    private static final byte[] RAW_VOLUME = new byte[] {
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,1,0,1,0,1,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,1,0,1,0,1,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,1,0,1,0,1,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,1,0,1,0,1,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,1,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,1,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,1,0,1,0,1,0,1,0,0,
            0,0,0,1,0,1,0,0,0,1,0,1,0,1,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    };

    private static final byte[] ONE_BYTE_VOLUME = new byte[] {
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,1,0,1,0,1,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,1,0,1,0,1,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,1,0,1,0,1,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,1,0,1,0,1,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,
            0,0,0,1,0,1,0,1,0,1,0,1,0,0,0,0,
            0,0,0,1,0,1,0,0,0,1,0,1,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    };

    private static final byte[] CIRCLE_VOLUME = new byte[] {
        //  0       3       7      11    15

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,0,
            0,0,0,0,1,1,0,0,1,1,0,0,0,0,0,0,
            0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,

    };

    @Test
    public void doDownSample2PerVoxel() throws Exception {
        doDownSample( "2-bytes per voxel/tube", RAW_VOLUME, 8, 8, 8, 2.0, 2 );
    }

    @Test
    public void doDownSampleTube1PerVoxel() throws Exception {
        doDownSample( "1-byte-per-voxel/tube", ONE_BYTE_VOLUME, 16, 8, 8, 2.0, 1 );
    }

    @Test
    public void doDownSampleCircle1PerVoxel() throws Exception {
        doDownSample( "1-byte-per-voxel/circle", CIRCLE_VOLUME, 16, 8, 8, 2.0, 1 );
    }

    public void doDownSample(
            String testName, byte[] volume, int sx, int sy, int sz, double scaleAll, int voxelBytes )
            throws Exception {

        StringBuilder builder = new StringBuilder();
        builder.append("TEST: " + testName ).append('\n');
        double xScale = scaleAll;
        double yScale = scaleAll;
        double zScale = scaleAll;

        DownSampler downSampler = new DownSampler( sx, sy, sz );
        DownSampler.DownsampledTextureData data =
                downSampler.getDownSampledVolume( new VolumeDataBean( volume, sx, sy, sz ), voxelBytes, xScale, yScale, zScale );
        Assert.assertNotSame( "Zero-length volume.", data.getVolume().length(), 0 );
        for (int i = 0; i < data.getVolume().length(); i++ ) {
            if ( i % (data.getSx() * voxelBytes) == 0 ) {
                builder.append("\n");
            }
            builder.append(data.getVolume().getValueAt(i) + ",");
        }
        builder.append('\n');
        builder.append( "Length total=" + data.getVolume().length() ).append('\n');
        builder.append( "Dimensions are " + data.getSx() + " x " + data.getSy() + " x " + data.getSz() ).append('\n');

        Assert.assertEquals(
                "Report Not as Expected: \n" + builder.toString(), testNameVsOutput.get( testName ), builder.toString()
        );
    }

}
