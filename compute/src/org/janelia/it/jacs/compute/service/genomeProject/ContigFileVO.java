
package org.janelia.it.jacs.compute.service.genomeProject;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 30, 2010
 * Time: 3:13:28 PM
 */
public class ContigFileVO implements Comparable {
    private Long length;
    private String filePath;

    public ContigFileVO(Long length, String filePath) {
        this.length = length;
        this.filePath = filePath;
    }

    public Long getLength() {
        return length;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public int compareTo(Object o) {
        if (null != o && null != ((ContigFileVO) o).getLength() && null != length) {
            return ((ContigFileVO) o).getLength().compareTo(length);
        }
        return -1;
    }
}
