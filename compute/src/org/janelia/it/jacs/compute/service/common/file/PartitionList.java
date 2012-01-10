
package org.janelia.it.jacs.compute.service.common.file;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Sep 25, 2007
 * Time: 11:04:38 AM
 */
public class PartitionList implements Serializable {

    private List<File> fileList;
    private long databaseLength; /* NOTE: the meaning of this attribute depends on the context of the node type */

    public PartitionList() {
        fileList = new ArrayList<File>();
    }

    public List<File> getFileList() {
        return fileList;
    }

    public void setFileList(List<File> fileList) {
        this.fileList = fileList;
    }

    public long getDatabaseLength() {
        return databaseLength;
    }

    public void setDatabaseLength(long databaseLength) {
        this.databaseLength = databaseLength;
    }

    public void add(File partitionFile) {
        fileList.add(partitionFile);
    }

    public int size() {
        return fileList.size();
    }

    public Iterator<File> iterator() {
        return fileList.iterator();
    }
}
