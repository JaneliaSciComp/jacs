/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.compute.access;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Apr 20, 2010
 * Time: 10:42:27 AM
 */
public class TextFileIO {

    static public String readTextFile(String path) throws Exception {
        File file = new File(cleanPath(path));
        return readTextFile(file);
    }

    static public String readTextFile(File aFile) throws Exception {
        StringBuilder contents = new StringBuilder();

        //use buffering, reading one line at a time
        //FileReader always assumes default encoding is OK!
        BufferedReader input = new BufferedReader(new FileReader(aFile));
        String line; //not declared within while loop
/*
 * readLine is a bit quirky :
 * it returns the content of a line MINUS the newline.
 * it returns null only for the END of the stream.
 * it returns an empty String if two newlines appear in a row.
 */
        while ((line = input.readLine()) != null) {
            contents.append(line);
            contents.append(System.getProperty("line.separator"));
        }
        input.close();
        return contents.toString();

    }

    static String cleanPath(String path) {
        String newpath = path;
        while (newpath.length() > 0
                && (newpath.substring(newpath.length() - 1).equals("\n") ||
                newpath.substring(newpath.length() - 1).equals("\r"))) {
            newpath = newpath.substring(0, newpath.length() - 1);
        }
        return newpath;
    }

    static public void writeTextFile(String path, String contents) throws Exception {
        File file = new File(cleanPath(path));
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new Exception("writeTextFile: could not create file ".concat(path));
            }
        }
        file.setReadable(true);
        file.setWritable(true, true);
        writeTextFile(file, contents);
    }

    static public void writeTextFile(File file, String contents) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("File should not be null.");
        }
        if (!file.exists()) {
            throw new FileNotFoundException("File does not exist: " + file);
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Should not be a directory: " + file);
        }
        if (!file.canWrite()) {
            throw new IllegalArgumentException("File cannot be written: " + file);
        }

        //use buffering
        Writer output = new BufferedWriter(new FileWriter(file));
        //FileWriter always assumes default encoding is OK!
        output.write(contents);
        output.close();
    }
}

