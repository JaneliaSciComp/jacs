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

package org.janelia.it.jacs.shared.dma.formatdb;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.shared.fasta.FastaFile;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 15, 2006
 * Time: 10:39:39 AM
 *
 * @version $Id: CreateBlastDatabaseFromFastaTool.java 1 2011-02-16 21:07:19Z tprindle $
 */

public class CreateBlastDatabaseFromFastaTool {
    public static final String NUCLEOTIDE = "nucleotide";
    public static final String PEPTIDE = "peptide";

    private String fastaFilePath;
    private String residueType;
    private String partitionPrefix;
    private String outputPath;
    private Long partitionSize;
    private Long partitionEntries;
    private Long numResidues;
    private Long numPartitions;
    private Properties properties;
    private FastaFile fastaFile;
    private Logger logger;

    public CreateBlastDatabaseFromFastaTool(Logger logger) {
        this.logger = logger;
    }

    public Long getNumPartitions() {
        return numPartitions;
    }

    public Long getNumResidues() {
        return numResidues;
    }

    public void setFastaFile(FastaFile fastaFile) {
        this.fastaFile = fastaFile;
    }

    public void setPartitionPrefix(String partitionPrefix) {
        this.partitionPrefix = partitionPrefix;
    }

    public void setPartitionEntries(Long partitionEntries) {
        this.partitionEntries = partitionEntries;
    }

    public void setPartitionSize(Long partitionSize) {
        this.partitionSize = partitionSize;
    }

    public void setFastaFilePath(String fastaFilePath) {
        this.fastaFilePath = fastaFilePath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public void setResidueType(String residueType) {
        residueType = residueType.toLowerCase();
        if (NUCLEOTIDE.equals(residueType) || PEPTIDE.equals(residueType)) {
            this.residueType = residueType;
        }
        else {
            throw new RuntimeException("Unknown residue type: " + residueType);
        }
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void partition() {
        // Create directory;

        System.out.println("Partitioning...");

        // Creating directory for output
        File outdir = new File(this.outputPath);
        if (outdir.isDirectory()) {
            System.out.println(outdir.getName() + " directory exists.  Using...");
        }
        else {
            boolean success = outdir.mkdirs();
            if (!success) {
                System.out.println(outdir.getName() + " directory doesn't exist and can't be created.");
                throw new RuntimeException("Could not create directory: " + outdir.getName());
            }
            else {
                System.out.println(outdir.getName() + " directory created!");
            }
        }

        // Getting number of residues in source fasta file
        if (fastaFile == null) {
            fastaFile = new FastaFile(this.fastaFilePath);
        }
        // Splitting Fasta file apart
        try {
            File opf = new File(this.outputPath);
            if (this.partitionSize == null)
                throw new RuntimeException("Partition Size was not defined.");
            if (this.partitionPrefix == null)
                throw new RuntimeException("Partition Prefix was not defined.");
            this.numPartitions = fastaFile.split(opf, this.partitionPrefix, this.partitionSize, this.partitionEntries);
            System.out.println("numPartitions:" + this.getNumPartitions());
            // set the number of residues from the fasta file size
            this.numResidues = fastaFile.getSize().getBases();
            System.out.println("numResidues:" + this.numResidues);
        }
        catch (Exception e) {
            System.err.println("Exception while trying to split " + this.fastaFilePath);
            e.printStackTrace();
        }

        // Formatdb files
        FormatDBTool fdbt = new FormatDBTool(this.properties, logger, partitionPrefix);
        if (NUCLEOTIDE.equals(this.residueType)) {
            fdbt.formatNucleotideDir(outdir);
        }
        else if (PEPTIDE.equals(this.residueType)) {
            fdbt.formatProteinDir(outdir);
        }
    }

    public static void main(String[] args) {
        try {
            CreateBlastDatabaseFromFastaTool pf = new CreateBlastDatabaseFromFastaTool(
                    Logger.getLogger(CreateBlastDatabaseFromFastaTool.class));
            pf.setFastaFilePath("c:\\temp\\sample.fasta");
            pf.setResidueType(NUCLEOTIDE);
            pf.setOutputPath("c:\\temp\\junk");
            pf.setPartitionPrefix("yo");
            pf.setPartitionSize(10000L);
            pf.setPartitionEntries(1000L);
            Properties prop = new Properties();
            prop.setProperty(FormatDBTool.FORMATDB_PATH_PROP,
                    "c:\\BLAST\\bin\\formatdb");
            prop.setProperty(SystemCall.SCRATCH_DIR_PROP,
                    "/scratch/jboss/tmp_exec");
            prop.setProperty(SystemCall.SHELL_PATH_PROP,
                    "c:\\BLAST\\bin");
            prop.setProperty(SystemCall.STREAM_DIRECTOR_PROP,
                    ">&");

            pf.setProperties(prop);
            pf.partition();

            System.out.println(
                    "Number of Partitions:" + pf.getNumPartitions());
            System.out.println(
                    "Number of Residues:" + pf.getNumResidues());
        }
        catch (Exception e) {
            System.err.println("Exception while trying to PrepareFasta.");
            e.printStackTrace();
        }
    }

}
