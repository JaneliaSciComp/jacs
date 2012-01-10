
package org.janelia.it.jacs.model.user_data.blast;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Dec 6, 2006
 * Time: 2:48:06 PM
 */
public interface Blastable {

    public void setSequenceType(String type);

    public String getSequenceType();

    public void setSequenceCount(Integer count);

    public Integer getSequenceCount();

}
