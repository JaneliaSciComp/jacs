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

package org.janelia.it.jacs.shared.perf;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.DateUtil;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Mar 30, 2007
 * Time: 11:05:05 AM
 */
public class PerfStats {
    private static Logger logger = Logger.getLogger(PerfStats.class);
    private static boolean perfOn = SystemConfigurationProperties.getBoolean("perf.testing", false);

    public static final String KEY_INIT = "FastaInputParser.init";
    public static final String KEY_EXTRACT_SEQUENCE_INFOS = "SequenceExtractor.extractSequenceInfos";
    public static final String KEY_READLINE = "SequenceExtractor.Reader.readline";
    public static final String KEY_FETCH_SOURCE_DB_SEQUENCE_INFOS = "SequenceExtractor.fetchSourceDBSequenceInfos";
    public static final String KEY_SETUP_EXISTING_SEQUENCE_INFOS = "SequenceExtractor.setupExistingSequenceInfos";
    public static final String KEY_SETUP_ASSEMBLIES = "SequenceExtractor.setupAssemblies";
    public static final String KEY_ADD_SEQUENCE_INFO = "SequenceExtractor.addSequenceInfo";
    public static final String KEY_IMPORT_NEW_TAGS = "TagImporter.importTags";
    public static final String KEY_CREATE_TAG_LINKS_FOR_EXISTING_SEQUENCES = "TagEntityImporter.createTagLinksForExistingSequences";
    public static final String KEY_INSERT_SEQUENCES = "SequenceImporter.insertSequences";
    public static final String KEY_INSERT_SEQ_ENTITIES = "SequenceImporter.insertSeqEntities";
    public static final String KEY_INSERT_ASSEMBLIES = "SequenceImporter.insertAssemblies";
    public static final String KEY_UPDATE_ASSEMBLIES = "SequenceImporter.updateAssemblies";
    public static final String KEY_CREATE_TAG_LINKS_FOR_NEW_SEQUENCES = "TagEntityImporter.createTagLinksForNewSequences";
    public static final String KEY_LOAD_ALL_TAGS_IN_CACHE = "TagLoader.loadAllTagsInCache";

    public static final String KEY_FASTA_CREATOR_CREATE = "BlastDbCreator.create";


    private static TreeMap<String, PerfItem> perfItems = new TreeMap<String, PerfItem>();

    public static void clear() {
        perfItems.clear();
    }

    public static void start(String id, Class clazz, String item) {
        start(id + ". " + clazz.getSimpleName() + " " + item);
    }

    public static void start(String perfItemName) {
        if (!perfOn) {
            return;
        }
        PerfItem perfItem = perfItems.get(perfItemName);
        if (perfItem == null) {
            perfItem = new PerfItem(perfItemName);
        }
        perfItem.startTime = System.currentTimeMillis();
        perfItems.put(perfItemName, perfItem);
    }

    public static void end(String id, Class clazz, String item) {
        end(id + ". " + clazz.getSimpleName() + " " + item);
    }

    public static void end(String perfItem) {
        if (!perfOn) {
            return;
        }
        PerfItem item = perfItems.get(perfItem);
        item.total = item.total + (System.currentTimeMillis() - item.startTime);
    }

    public static String getStats() {
        if (!perfOn) {
            return "";
        }
        return getStatValues(perfItems.values());
    }

    public static String getStatsByValues() {
        if (!perfOn) {
            return "";
        }
        List<PerfItem> perfStats = new ArrayList<PerfItem>();
        perfStats.addAll(perfItems.values());

        Collections.sort(perfStats, new InverseTotalComparator());
        return getStatValues(perfStats);
    }

    private static String getStatValues(Collection<PerfItem> stats) {
        StringBuffer fullMsg = new StringBuffer("\n-------- Performance -------\n");
        for (PerfItem perfItem : stats) {
            fullMsg.append(perfItem.name);
            fullMsg.append(DateUtil.getElapsedTime(" took ", perfItem.total));
            fullMsg.append("\n");
        }
        perfItems.clear();
        return fullMsg.toString();
    }

    public static void printStats() {
        logger.info(getStatsByValues());
    }

    public static void printStatsToStdOut() {
        System.out.println(getStatsByValues());
    }

    public static void printStats(Writer writer) throws IOException {
        writer.write(getStatsByValues());
    }

    public static void printStats(PrintStream out) throws IOException {
        out.write(getStatsByValues().getBytes());
    }

    private static class InverseTotalComparator implements Comparator<PerfItem> {

        public int compare(PerfItem o1, PerfItem o2) {
            long thisVal = o2.total;
            long anotherVal = o1.total;
            return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
        }
    }
}
