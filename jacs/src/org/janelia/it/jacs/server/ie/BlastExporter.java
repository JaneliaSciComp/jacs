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

package org.janelia.it.jacs.server.ie;

import org.hibernate.Session;
import org.janelia.it.jacs.model.user_data.blast.BlastResultNode;
import org.janelia.it.jacs.server.utils.HibernateSessionSource;
import org.janelia.it.jacs.shared.blast.blastxmlparser.BlastXMLWriter;

import java.io.FileWriter;
import java.io.Writer;

/**
 * Created by IntelliJ IDEA.
 * User: kli
 * Date: Apr 26, 2007
 * Time: 2:21:30 PM
 */
public class BlastExporter {

    public static void main(String[] args) {

        HibernateSessionSource sessionSource = new HibernateSessionSource();
        Session session = sessionSource.getOrCreateSession();
        session.beginTransaction();

        try {
            BlastExporter be = new BlastExporter();

            BlastResultNode brn = (BlastResultNode)
                    session.getNamedQuery("findBlastResultNodeByTaskId").
                            setLong("taskId", 1073351283978535263L).uniqueResult();

            Writer writer = new FileWriter("junk.xml");
            be.exportBLASTasXML(writer, brn);
            writer.close();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        session.close();

    }

    public void exportBLASTasXML(Writer writer, BlastResultNode brn) throws Exception {

        BlastXMLWriter bxmlw = new BlastXMLWriter();

        bxmlw.setBlastSource(brn);
        bxmlw.serialize(writer);

    }
}
