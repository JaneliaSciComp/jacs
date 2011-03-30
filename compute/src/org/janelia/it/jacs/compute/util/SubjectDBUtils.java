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

package org.janelia.it.jacs.compute.util;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;

import java.io.File;

/**
 * User: aresnick
 * Date: Jul 10, 2009
 * Time: 12:24:59 PM
 * <p/>
 * <p/>
 * Description:
 */
public class SubjectDBUtils {
    public static File getSubjectDBFile(File dbDir, String fileSuffix) throws MissingDataException {
        for (File file : dbDir.listFiles(new FileSuffixFilter(fileSuffix))) {
            return file;
        }

        // if no db file found, throw exception
        throw new MissingDataException(
                "Can't find subject database file with suffix " + fileSuffix
                        + " in subject db directory " + dbDir);
    }
}
