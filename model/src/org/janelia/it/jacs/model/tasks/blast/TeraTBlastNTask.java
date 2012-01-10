
package org.janelia.it.jacs.model.tasks.blast;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Aug 5, 2010
 * Time: 11:52:52 AM
 */
public class TeraTBlastNTask extends TBlastNTask implements TeraBlastTask {

    public TeraTBlastNTask() {
        super();
        this.taskName = this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".")+1);
    }

    public String getParameterFile() {
        return "tera-tblastn";
    }
}
