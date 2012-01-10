
package org.janelia.it.jacs.model.tasks.blast;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Aug 5, 2010
 * Time: 11:49:42 AM
 */
public class TeraBlastPTask extends BlastPTask implements TeraBlastTask {

    public TeraBlastPTask() {
        super();
        this.taskName = this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".")+1);
    }
    
    public String getParameterFile() {
        return "tera-blastp";
    }
}
