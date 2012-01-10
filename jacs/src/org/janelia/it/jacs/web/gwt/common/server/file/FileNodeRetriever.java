
package org.janelia.it.jacs.web.gwt.common.server.file;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.server.access.FileNodeDAO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Retrieves a FileNode from the database and filesystem. Based on org.janelia.it.jacs.web.control.file.FileNodeServerController
 *
 * @author Michael Press
 */
public class FileNodeRetriever {
    private static Logger log = Logger.getLogger(FileNodeRetriever.class);
    private FileNodeDAO _fileNodeDAO;

    public FileNodeRetriever(FileNodeDAO fileNodeDAO) {
        _fileNodeDAO = fileNodeDAO;
    }

    public String retrieveFileNode(Long taskId, String contentType, User validUser)
            throws Exception {
        log.info("FileNodeRetriever.retrieveFileNode()");

        // Get the filenode from the db via the job
        FileNode[] fileNodes = _fileNodeDAO.getResultFileNodesByTaskId(taskId);
        if (fileNodes == null || fileNodes.length == 0)
            fail("No such filenode found in database");
        FileNode fileNode = fileNodes[0];

        // Verify the user has access to this node
        String fileOwner = fileNode.getOwner();
        if (fileOwner == null) {
            fail("Proper naming credentials for node owner could not be found");
            return null;
        }

        if (!fileOwner.equals(validUser.getUserLogin())) {
            log.error("Requested node id " + taskId + " user login name is " + fileOwner +
                    " but session user principal name is " + validUser.getUserLogin());
            throw new Exception("User not authorized to view this information");
        }

        String path = fileNode.getFilePathByTag("html"); //TODO: support other types?
        return readFile(new File(path));
    }

    private String readFile(File file) throws IOException {
        FileInputStream instream = null;
        byte bytes[] = null;
        try {
            instream = new FileInputStream(file);
            int len = instream.available();
            bytes = new byte[len];
            instream.read(bytes);
        }
        finally {
            if (instream != null)
                instream.close();
        }

        //TODO: will handle null bytes?
        return new String(bytes);
    }

    private void fail(String msg) throws Exception {
        log.error(msg);
        throw new Exception(msg);
    }
}
