package org.janelia.workstation.jfs.fileshare;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.janelia.workstation.jfs.exception.FileNotFoundException;
import org.janelia.workstation.jfs.exception.FileUploadException;
import org.janelia.workstation.jfs.propfind.Multistatus;
import org.janelia.workstation.jfs.propfind.Prop;
import org.janelia.workstation.jfs.propfind.PropfindResponse;
import org.janelia.workstation.jfs.propfind.Propstat;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;

/**
 * Created by schauderd on 6/26/15.
 */
public class BlockFileShare extends FileShare {
    @Override
    public StreamingOutput getFile(HttpServletResponse response, String qualifiedFilename) throws FileNotFoundException {
        final String filename = "/" + qualifiedFilename;
        try {
            response.setHeader("Content-Length", Long.toString(Files.size(Paths.get(filename))));
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new FileNotFoundException("Problem retrieving content length data on file");
        }
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                Files.copy(Paths.get(filename), output);
            }
        };
    }

    @Override
    public void putFile(HttpServletRequest request, InputStream binaryStream, String filepath, boolean checksum, boolean local) throws FileUploadException {
        try {
            Files.copy(binaryStream,
                    Paths.get("/" + filepath),
                    new CopyOption[]{StandardCopyOption.REPLACE_EXISTING});
        } catch (IOException e) {
            e.printStackTrace();
            throw new FileUploadException("Problem creating new file on file share");
        }
    }

    @Override
    public void deleteFile(String qualifiedFilename, boolean local) throws IOException {
        Files.delete(Paths.get("/" + qualifiedFilename));
    }


    private PropfindResponse generatePropMetadata(HttpServletRequest request, java.nio.file.Path file) throws IOException {
        PropfindResponse fileMeta = new PropfindResponse();
        Propstat propstat = new Propstat();
        Prop prop = new Prop();
        prop.setCreationDate(Files.getAttribute(file, "creationTime").toString());

        // workaround for buggey probeContentType
        prop.setGetContentType(new MimetypesFileTypeMap().getContentType(file.toFile()));

        prop.setGetContentLength(Long.toString(Files.size(file)));
        prop.setGetLastModified(Files.getLastModifiedTime(file).toString());
        fileMeta.setHref("/JFS/api/file" + file.toString());
        if (Files.isDirectory(file)) {
            prop.setResourceType("collection");
            fileMeta.setHref(fileMeta.getHref() + "/");
        }
        propstat.setProp(prop);
        propstat.setStatus("HTTP/1.1 200 OK");
        fileMeta.setPropstat(propstat);

        return fileMeta;
    }

    private void discoverFiles(HttpServletRequest request, Multistatus container, java.nio.file.Path file,
                               int depth, int discoveryLevel) throws IOException {
        if (depth<discoveryLevel) {
            if (Files.isDirectory(file)) {
                depth++;
                try (DirectoryStream<java.nio.file.Path> directoryStream = Files.newDirectoryStream(file)) {
                    for (java.nio.file.Path subpath : directoryStream) {
                        discoverFiles(request, container, subpath, depth, discoveryLevel);
                    }
                }
            }
        }
        container.getResponse().add(generatePropMetadata(request, file));
    }

    @Override
    public String propFind(HttpServletRequest request, HttpHeaders headers, String path) throws FileNotFoundException, IOException {
        String filepath = "/" + path;

        // create Multistatus top level
        Multistatus propfindContainer = new Multistatus();

        java.nio.file.Path fileHandle = Paths.get(filepath);
        if (Files.exists(fileHandle)) {
            // check DEPTH header to check whether to get subdirectory information
            int discoveryLevel = 0;
            List<String> depth = headers.getRequestHeader("Depth");
            if (depth != null && depth.size() > 0) {
                String depthValue = depth.get(0).trim();
                if (depthValue.toLowerCase().equals("infinity")) {
                    discoveryLevel = 20; // prevents insanity
                } else {
                    discoveryLevel = Integer.parseInt(depthValue);
                }
            }
            discoverFiles(request, propfindContainer, fileHandle, 0, discoveryLevel);
        } else {
            throw new FileNotFoundException("File does not exist");
        }

        ObjectMapper xmlMapper = new XmlMapper();
        String xml = null;
        try {
            xml = xmlMapper.writeValueAsString(propfindContainer);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            // TO DO throw specific error
        }
        return xml;

    }
}
