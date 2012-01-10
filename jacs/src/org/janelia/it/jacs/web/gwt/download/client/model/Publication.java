
package org.janelia.it.jacs.web.gwt.download.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.download.Author;
import org.janelia.it.jacs.model.download.DataFile;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNodeImpl;

import java.io.Serializable;
import java.util.List;

/**
 * User: Lfoster
 * Date: Aug 21, 2006
 * Time: 11:45:56 AM
 * <p/>
 * Publication interface.  A Java Bean, by getter convention.  Represents a paper or
 * model, with its title, author(s), summary anbstract, rolled-up data distributions, and document.
 */
public interface Publication extends IsSerializable, Serializable {

    String getAccessionNumber();

    String getTitle();

    String getSummary();

    String getAbstract();

    String getDescription();

    DownloadableDataNode getSubjectDocument();

    List<Author> getAuthors();

    List<DownloadableDataNodeImpl> getDataFiles();

    List<DataFile> getRolledUpDataArchives();
}
