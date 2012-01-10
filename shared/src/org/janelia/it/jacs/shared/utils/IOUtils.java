
package org.janelia.it.jacs.shared.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Sep 1, 2009
 * Time: 2:53:18 PM
 */
public class IOUtils {
    static public String readInputStream(InputStream input) {
        InputStreamReader in = new InputStreamReader(input);
        StringWriter writer = new StringWriter();
        char[] buffer = new char[10000];
        int n;
        try {
            while (-1 != (n = in.read(buffer))) {
                writer.write(buffer, 0, n);
            }
            return writer.toString();
        }
        catch (IOException e) {
            return e.getMessage();
        }
    }
}
