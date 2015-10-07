
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
