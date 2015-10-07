<%@ page import="org.janelia.it.jacs.compute.web.MetaGenoPipeController" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"  %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"  %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
  ~ Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
  ~
  ~ This file is part of JCVI VICS.
  ~
  ~ JCVI VICS is free software; you can redistribute it and/or modify it
  ~ under the terms and conditions of the Artistic License 2.0.  For
  ~ details, see the full text of the license in the file LICENSE.txt.  No
  ~ other rights are granted.  Any and all third party software rights to
  ~ remain with the original developer.
  ~
  ~ JCVI VICS is distributed in the hope that it will be useful in
  ~ bioinformatics applications, but it is provided "AS IS" and WITHOUT
  ~ ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
  ~ implied warranties of merchantability or fitness for any particular
  ~ purpose.  For details, see the full text of the license in the file
  ~ LICENSE.txt.
  ~
  ~ You should have received a copy of the Artistic License 2.0 along with
  ~ JCVI VICS.  If not, the license can be obtained from
  ~ "http://www.perlfoundation.org/artistic_license_2_0."
  --%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link href="MetaGenoPipeStyle.css" rel="stylesheet" type="text/css" />

<%

%>
<head>
<title>J. Craig Venter Institute</title>
</head>

<body>

<div id="title_section">
    Metagenomics Pipeline Home - example project code: 08020
</div>

<div class="top_submit">
    Check pipeline <a href="/compute/MetaGenoPipeStatus?status=all">status</a>
</div>

<div class="top_submit">
    Enter file path for fasta upload
</div>

<form action="/compute/MetaGenoPipeController?type=<%=MetaGenoPipeController.SUBMIT_NODE%>" method="post">
    <input type="text" size="60" name="Upload Fasta File Node"/>
    <input type="submit" value="Submit"/>
</form>

<table class="submit_example_table" cellspacing="5" cellpadding="5" border="0">
    <col width="800"/>
    <tbody>
        <tr>
            <td>Example upload file: /usr/local/annotation/METAGENOMIC/BENCHMARK/input-sets/bearpaw_reads.fasta</td>
        </tr>
    </tbody>
</table>

<div class="top_submit">
    Enter FastaFileNode Id for Metagenomics ORF Calling Pipeline
</div>

<form action="/compute/MetaGenoPipeController?type=<%=MetaGenoPipeController.SUBMIT_ORF%>" method="post">
    <table cellspacing="5" cellpadding="5" border="0">
        <col width="800"/>
        <tbody>
            <tr>
                <td><input type="text" name="Start Metagenomic ORF Calling Pipeline"/></td>
            </tr>
            <tr>
                <td><input type="checkbox" name="clear_range" value="true">Filter Clear Range (requires specific defline format)</td>
            </tr>
            <tr>
                <td>Project Code<input type="text" name="Project Code"/></td>
            </tr>
            <tr>
                <td><input type="submit" value="Submit"/></td>
            </tr>
        </tbody>
    </table>
</form>

<table class="submit_example_table" cellspacing="5" cellpadding="5" border="0">
    <col width="800"/>
    <tbody>
        <tr>
            <td>Example file node w/ 100 sequences and NO clear range: 1333565030372737399</td>
        </tr>
        <tr>
            <td>Example file node w/ 9000 sequences and clear range: 1328874328904696183</td>
        </tr>
        <tr>
            <td>Example file node w/ 560,000 sequences and no clear range: 1329211269172953463</td>
        </tr>
    </tbody>
</table>

<div class="top_submit">
    Enter FastaFileNode Id for Metagenomics Annotation Pipeline
</div>

<form action="/compute/MetaGenoPipeController?type=<%=MetaGenoPipeController.SUBMIT_ANNO%>" method="post">
    <table cellspacing="5" cellpadding="5" border="0">
        <col width="800"/>
        <tbody>
            <tr>
                <td><input type="text" name="Start Metagenomic Annotation Pipeline"/></td>
            </tr>
            <tr>
                <td>Project Code<input type="text" name="Project Code"/></td>
            </tr>
            <tr>
                <td><input type="submit" value="Submit"/></td>
            </tr>
        </tbody>
    </table>
</form>

<table class="submit_example_table" cellspacing="5" cellpadding="5" border="0">
    <col width="800"/>
    <tbody>
        <tr>
            <td>Example file node w/ 500 orf sequences: 1329166265368969591</td>
        </tr>
        <tr>
            <td>Example file node w/ 9000 orf sequences: 1329166495283937655</td>
        </tr>
    </tbody>
</table>

<div class="top_submit">
    Enter FastaFileNode Id for Metagenomics Combined ORF Calling and Annotation Pipeline
</div>

<form action="/compute/MetaGenoPipeController?type=<%=MetaGenoPipeController.SUBMIT_COMBINED%>" method="post">
    <table cellspacing="5" cellpadding="5" border="0">
        <col width="800"/>
        <tbody>
            <tr>
                <td><input type="text" name="Start Metagenomic Combined Pipeline"/></td>
            </tr>
            <tr>
                <td><input type="checkbox" name="combined_clear_range" value="true">Filter Clear Range (requires specific defline format)</td>
            </tr>
            <tr>
                <td>Project Code<input type="text" name="Project Code"/></td>
            </tr>
            <tr>
                <td><input type="submit" value="Submit"/></td>
            </tr>
        </tbody>
    </table>
</form>


</body>
</html>

