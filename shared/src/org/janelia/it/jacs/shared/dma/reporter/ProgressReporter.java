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

package org.janelia.it.jacs.shared.dma.reporter;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to output collection of Progress instances in a readable format
 *
 * @author Tareq Nabeel
 */
public class ProgressReporter {

    public static synchronized String getReport(Progress progress, boolean addHeader, String fileName) {
        StringBuilder paddedVal = new StringBuilder();
        List<ItemNameValueLength> items = new ArrayList<ItemNameValueLength>();
        addHeaders(items, addHeader, paddedVal, fileName);
        addProgress(items, progress, paddedVal);
        return paddedVal.toString();
    }

    /**
     */
    public static synchronized String getReport(List<Progress> progressList, boolean addHeader, String fileName) {
        if (progressList.size() == 0) {
            return "";
        }
        StringBuilder paddedVal = new StringBuilder();
        List<ItemNameValueLength> items = new ArrayList<ItemNameValueLength>();
        addHeaders(items, addHeader, paddedVal, fileName);
        for (Progress progress : progressList) {
            addProgress(items, progress, paddedVal);
        }
        return paddedVal.toString();
    }

    private static void addHeaders(List<ItemNameValueLength> items, boolean addHeader, StringBuilder paddedVal, String fileName) {
        int maxFileName = 40;
        if (fileName != null && fileName.length() > maxFileName) {
            fileName = fileName.substring(0, maxFileName - 2);
        }
        items.add(new ItemNameValueLength(fileName, AppendOrient.left, maxFileName));
        items.add(new ItemNameValueLength("Processed MB", AppendOrient.right, 12));
        items.add(new ItemNameValueLength("Targ MB", AppendOrient.right, 11));
        items.add(new ItemNameValueLength("Completed", AppendOrient.right, 10));
        items.add(new ItemNameValueLength("Processed Seqs", AppendOrient.right, 15));
        items.add(new ItemNameValueLength("Target Seqs", AppendOrient.right, 12));
        items.add(new ItemNameValueLength("Completed", AppendOrient.right, 10));
        items.add(new ItemNameValueLength("Errors", AppendOrient.left, 8, "  "));
        items.add(new ItemNameValueLength("AvgRate", AppendOrient.right, 7));
        items.add(new ItemNameValueLength("ActRate", AppendOrient.right, 8));
        items.add(new ItemNameValueLength("Process", AppendOrient.left, 20, "  "));

        if (addHeader) {
            appendName(paddedVal, items);
            appendDash(paddedVal, items);
        }
    }

    private static void addProgress(List<ItemNameValueLength> items, Progress progress, StringBuilder paddedVal) {
        ItemNameValueLength nameItem = items.get(0);
        String progressItemName = progress.getItemName();
        if (progressItemName != null && progressItemName.length() > nameItem.getMaxLength()) {
            progressItemName = progressItemName.substring(0, nameItem.getMaxLength() - 1);
        }
        items.get(0).setValue(progressItemName);
        items.get(1).setValue(progress.getStrTotalBytesProcessed());
        items.get(2).setValue(progress.getStrTotalTargetBytes());
        items.get(3).setValue(progress.getStrMBPercentComplete());
        items.get(4).setValue(progress.getStrTotalSequencesProcessed());
        items.get(5).setValue(progress.getStrTargetSequences());
        items.get(6).setValue(progress.getStrPercentSeqComplete());
        items.get(7).setValue(progress.getStrSequencesInError());
        items.get(8).setValue(progress.getStrAvgSequencePerSec());
        items.get(9).setValue(progress.getStrActualSequencePerSec());
        items.get(10).setValue(progress.getStrTotalElapsedTime());

        appendValue(paddedVal, items);
    }

    private static void appendName(StringBuilder paddedVal, List<ItemNameValueLength> items) {
        for (ItemNameValueLength item : items) {
            append(paddedVal, item, item.getName(), item.getOrient());
        }
        paddedVal.append("\n");
    }

    private static void appendDash(StringBuilder paddedVal, List<ItemNameValueLength> items) {
        for (ItemNameValueLength item : items) {
            append(paddedVal, item, getDashes(item), item.getOrient());
        }
        paddedVal.append("\n");
    }

    private static void appendValue(StringBuilder paddedVal, List<ItemNameValueLength> items) {
        for (ItemNameValueLength item : items) {
            append(paddedVal, item, item.getValue(), item.getOrient());
        }
        paddedVal.append("\n");
    }

    private static void append(StringBuilder buff, ItemNameValueLength item, String value, AppendOrient appendOrient) {
        if (value == null) {
            value = "";
        }
        int actualLength = value.length();
        if (appendOrient == AppendOrient.left) {
            buff.append(item.getPrepend());
            buff.append(value);
            pad(buff, item, actualLength);
        }
        else {
            pad(buff, item, actualLength);
            buff.append(value);
        }
    }

    private static String getDashes(ItemNameValueLength item) {
        int numDashes = item.getName().length();
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < numDashes; i++) {
            buff.append("-");
        }
        return buff.toString();
    }

    private static void pad(StringBuilder buff, ItemNameValueLength item, int actualLength) {
        int numCharsToPad = 0;
        if (item.getMaxLength() > actualLength) {
            numCharsToPad = item.getMaxLength() - actualLength;
        }
        for (int i = 0; i < numCharsToPad; ++i) {
            buff.append(" ");
        }
    }

    private static class ItemNameValueLength {
        private String name;
        private String value = "";
        private int maxLength;
        private AppendOrient orient;
        private String prepend;

        public ItemNameValueLength(String name, AppendOrient orient, int maxLength, String prepend) {
            this.name = name;
            this.maxLength = maxLength;
            this.orient = orient;
            this.prepend = prepend;
        }

        public ItemNameValueLength(String name, AppendOrient orient, int maxLength) {
            this(name, orient, maxLength, "");
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public int getMaxLength() {
            return maxLength;
        }


        public void setValue(String value) {
            this.value = value;
        }

        public AppendOrient getOrient() {
            return orient;
        }

        public String getPrepend() {
            return prepend;
        }
    }

    private enum AppendOrient {
        left, right
    }
}
