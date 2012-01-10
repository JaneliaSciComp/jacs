
package org.janelia.it.jacs.model.tasks.blast;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Aug 5, 2010
 * Time: 11:54:09 AM
 */
public class TeraTBlastXTask extends TBlastXTask implements TeraBlastTask {

    public TeraTBlastXTask() {
        super();
        this.taskName = this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".")+1);
    }

    public String getParameterFile() {
        return "tera-tblastx";
    }
}
