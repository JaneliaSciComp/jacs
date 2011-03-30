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

package org.janelia.it.jacs.compute.service.export.writers;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.shared.export.ExportWriterConstants;

import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 6, 2008
 * Time: 3:38:40 PM
 */
public class ExportHtmlWriter extends ExportWriter {

    public ExportHtmlWriter() {
    }

    public ExportHtmlWriter(String fullPathFilename, List<SortArgument> headerItems) {
        super(fullPathFilename, headerItems);
    }

    public String getFormatType() {
        return ExportWriterConstants.EXPORT_TYPE_HTML;
    }

    /**
     * This method is intended to start off the file type with any characters necessary. ie <xml ... for XmlWriters.
     */
    public void start() throws IOException {
        writer.write("");
    }

    public void writeItem(List<String> itemStrings) throws IOException {
        writer.write("");
    }

    /**
     * This method is intended to close the file type with any characters necessary. ie </xml> for XmlWriters.
     */
    protected void endFormatting() throws IOException {
        writer.write("");
    }
}
