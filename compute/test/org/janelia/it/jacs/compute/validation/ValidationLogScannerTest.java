package org.janelia.it.jacs.compute.validation;

import org.junit.Assert;
import org.janelia.it.jacs.compute.process_result_validation.ValidationLogScanner;
import org.junit.Test;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Created by fosterl on 8/5/14.
 */
public class ValidationLogScannerTest {
    @Test
    public void scanLog() throws Exception {
        // Looks for the test validation subdirectory.
        File validation = new File("compute/test/conf/validation");
        Assert.assertTrue( "No validation directory " + validation.getAbsolutePath(), validation.exists() );
        ValidationLogScanner scanner = new ValidationLogScanner(validation);
        CharArrayWriter outWriter = new CharArrayWriter();
        PrintWriter pw = new PrintWriter(outWriter);
        scanner.writeStatisticSummary( new PrintWriter( pw ) );
        Assert.assertTrue(
                "No output produced from validation scan.",
                outWriter.toString() != null  &&  outWriter.toString().trim().length() > 0
        );

        System.out.println( outWriter.toString() );
    }
}
