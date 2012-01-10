
package org.janelia.it.jacs.server.datavalidation;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Test program for validating blastable subject nodes
 */
public class ValidateBlastableSubjectSets {

    public ValidateBlastableSubjectSets() {
    }

    static public void main(String[] args) {
        try {
            AbstractApplicationContext ctx
                    = new ClassPathXmlApplicationContext(new String[]{
                    "WEB-INF/applicationContext-server.xml",
                    "WEB-INF/applicationContext-common.xml",
                    "WEB-INF/applicationContext-standalone.xml"
            });
            BlastSubjectNodeValidator blastSubjectsValidator =
                    (BlastSubjectNodeValidator)
                            ctx.getBean("blastSubjectsValidator");
            blastSubjectsValidator.validatePublicSubjectNodes(true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
