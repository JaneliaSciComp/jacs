package org.janelia.it.jacs.compute.validation;

import org.junit.Assert;
import org.janelia.it.jacs.compute.process_result_validation.ValidationLogScanner;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;

/**
 * Created by fosterl on 8/5/14.
 */
public class ValidationLogScannerTest {
    @Test
    public void scanLog() throws Exception {
        // Looks for the test validation subdirectory.
        File validation = new File("validation");
        Assert.assertTrue( "No validation directory", validation.exists() );
        ValidationLogScanner scanner = new ValidationLogScanner(validation);
        scanner.writeStatisticSummary( new PrintWriter( System.out ) );
    }
}
