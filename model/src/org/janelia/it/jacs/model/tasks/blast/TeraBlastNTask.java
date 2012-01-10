
package org.janelia.it.jacs.model.tasks.blast;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Aug 5, 2010
 * Time: 10:51:00 AM
 */
public class TeraBlastNTask extends BlastNTask implements TeraBlastTask {

    public TeraBlastNTask() {
        super();
        this.taskName = this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".")+1);
    }

    public String getParameterFile() {
        return "tera-blastn";
    }
}
