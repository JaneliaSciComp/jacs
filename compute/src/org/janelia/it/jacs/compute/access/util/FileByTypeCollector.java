/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.access.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this to deep-dive a directory structure, looking for files of a given
 * extension.
 *
 * @author fosterl
 */
public class FileByTypeCollector extends SimpleFileVisitor<Path> {
    private static Logger LOG = LoggerFactory.getLogger(ResultSetIterator.class);

    private final int maxDescent;
    private final Path basePath;
    private final String fileType;
    
    private boolean visitationComplete = false;
    
    private final Set<File> files;

    public FileByTypeCollector(String basePathStr, String fileType, int maxDescent) {
        this.maxDescent = maxDescent;
        this.fileType = fileType;
        this.files = new HashSet<>();
        basePath = Paths.get(basePathStr);
        LOG.info("Examining " + basePath.toString());
    }
    
    /**
     * Launches the visit process.
     */
    public void exec() throws IOException {
        Files.walkFileTree(basePath, EnumSet.noneOf(FileVisitOption.class), maxDescent, this);
    }

    /**
     * After completion, can get the full set of stuff that was found.
     */
    public Set<File> getFileSet() {
        if (!visitationComplete) {
            throw new IllegalStateException("Process incomplete: please await termination of visitation.");
        }
        return files;
    }

    //-------------------------------------------OVERRIDES SimpleFileVisitor
    /**
     * Avoid file exceptions' killing the process.
     */
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException io) {
        LOG.warn("Error in " + file + " due to " + io);
        return FileVisitResult.SKIP_SUBTREE;
    }
    
    /**
     * This is presented with all files encountered.
     * 
     * @param file to check
     * @param attrs to avail checking.
     * @return always continue.
     * @throws IOException 
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
        throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(attrs);

        File classicFile = file.toFile();
        if (attrs.isRegularFile()  &&  classicFile.getName().endsWith(fileType)) {
            files.add(classicFile);
        }
        else {
            LOG.info("Rejecting " + file.getFileName().toString());
            LOG.info("Reason: endswith? " + file.getFileName().endsWith(fileType));
            LOG.info("Reason: isRegularFile? " + attrs.isRegularFile());
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            throws IOException {
        if (dir.equals(basePath)) {
            visitationComplete = true;
        }
        return FileVisitResult.CONTINUE;
    }

}
