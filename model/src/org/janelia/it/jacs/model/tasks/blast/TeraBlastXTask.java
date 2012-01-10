
package org.janelia.it.jacs.model.tasks.blast;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Aug 5, 2010
 * Time: 11:51:40 AM
 */
public class TeraBlastXTask extends BlastXTask implements TeraBlastTask {

    public TeraBlastXTask() {
        super();
        this.taskName = this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".")+1);
    }

    public String getParameterFile() {
        return "tera-blastx";
    }
}
