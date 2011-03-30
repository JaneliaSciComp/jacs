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
