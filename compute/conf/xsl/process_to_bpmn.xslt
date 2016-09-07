<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <!-- XSLT: to construct anchor cylinder xml. -->

    <xsl:output indent="no" method="xml" omit-xml-declaration="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="/process">
        <xsl:variable name="process_name" select="@name" />
        <xsl:variable name="processor" select="@processor" />
        <xsl:variable name="start_event" select="@startEvent" />

        <!--  order=descending implies that the 'order by' property is for descending order, once the entities have been read.
              it does not imply that the entites have already been placed into descending order. -->
        <cylinder version="1.0" anchor_length="{$anchor_length}" anchor_name="{$anchor_name}" order="ascending"><xsl:text>&#xa;</xsl:text>
            <!-- Add an entity for the anchor itself. -->
            <entity><xsl:text>&#xa;</xsl:text>
                <subentity qstart="1" qend="{$anchor_length}" sstart="1" send="{$anchor_length}" strand="0"><xsl:text>&#xa;</xsl:text>
                    <p value="Anchor" name="subEntityType"/><xsl:text>&#xa;</xsl:text>
                    <p name="name" value="{$anchor_name}"/><xsl:text>&#xa;</xsl:text>
                </subentity><xsl:text>&#xa;</xsl:text>
            </entity><xsl:text>&#xa;</xsl:text>
            <xsl:apply-templates select="BlastOutput_iterations" />
        </cylinder><xsl:text>&#xa;</xsl:text>
    </xsl:template>

    <xsl:template match="BlastOutput/BlastOutput_iterations">
        <xsl:apply-templates select="Iteration" />
    </xsl:template>

    <xsl:template match="BlastOutput_iterations/Iteration">
        <xsl:apply-templates select="Iteration_hits" />
    </xsl:template>

    <xsl:template match="Iteration/Iteration_hits">
        <xsl:apply-templates select="Hit" />
    </xsl:template>

    <xsl:template match="Iteration_hits/Hit">
        <xsl:apply-templates select="Hit_hsps" />
    </xsl:template>

    <xsl:template match="Hit/Hit_hsps">
        <xsl:apply-templates select="Hsp" />
    </xsl:template>

    <xsl:template match="Hit_hsps/Hsp">
        <!-- Making an entity and sub entity out of every HSP, to avoid having to deal with overlapping HSPs, and
             because HSPs can be thought of as competing alignments to each other, between this subject and the query -->
        <entity><xsl:text>&#xa;</xsl:text>
            <xsl:variable name="accession" select="../../Hit_accession"/>
            <xsl:variable name="defline_info" select="../../Hit_id"/>
            <xsl:variable name="prog" select="../../../../../../BlastOutput_program" />
            <xsl:variable name="qstart" select="Hsp_query-from" />
            <xsl:variable name="qend" select="Hsp_query-to" />
            <xsl:variable name="sstart" select="Hsp_hit-from" />
            <xsl:variable name="send" select="Hsp_hit-to" />
            <xsl:variable name="score" select="Hsp_score" />
            <xsl:variable name="evalue" select="Hsp_evalue" />
            <xsl:variable name="bitscore" select="Hsp_bit-score" />
            <xsl:variable name="query_align" select="Hsp_qseq" />
            <xsl:variable name="subject_align" select="Hsp_hseq" />
            <xsl:variable name="midline" select="Hsp_midline" />
            <xsl:if test="$prog='blastp'" >
                <xsl:variable name="strand" select="0" />
                <!-- TODO call function -->
                <subentity qstart="{$qstart}" qend="{$qend}" sstart="{$sstart}" send="{$send}" strand="{$strand}"><xsl:text>&#xa;</xsl:text>
                    <p name="score" value="{$score}" /><xsl:text>&#xa;</xsl:text>
                    <p name="bitscore" value="{$bitscore}" /><xsl:text>&#xa;</xsl:text>
                    <p name="evalue" value="{$evalue}" /><xsl:text>&#xa;</xsl:text>
                    <p name="accession" value="{$accession}" /><xsl:text>&#xa;</xsl:text>
                    <p name="subEntityType" value="{$prog}" /><xsl:text>&#xa;</xsl:text>
                    <p name="query_align" value="{$query_align}" /><xsl:text>&#xa;</xsl:text>
                    <p name="subject_align" value="{$subject_align}" /><xsl:text>&#xa;</xsl:text>
                    <p name="midline" value="{$midline}" /><xsl:text>&#xa;</xsl:text>
                    <p name="order by" value="{$bitscore}" /><xsl:text>&#xa;</xsl:text>
                    <p name="order by tiebreaker" value="{$accession}" /><xsl:text>&#xa;</xsl:text>
                </subentity><xsl:text>&#xa;</xsl:text>
            </xsl:if>
            <xsl:if test="$prog='blastn'" >
                <!-- Strand is positive if sstart greater than send -->
                <xsl:if test="$qend &gt; $qstart" >
                    <xsl:variable name="strand" select="1" />
                    <subentity qstart="{$qstart}" qend="{$qend}" sstart="{$sstart}" send="{$send}" strand="{$strand}"><xsl:text>&#xa;</xsl:text>
                        <p name="score" value="{$score}" /><xsl:text>&#xa;</xsl:text>
                        <p name="bitscore" value="{$bitscore}" /><xsl:text>&#xa;</xsl:text>
                        <p name="evalue" value="{$evalue}" /><xsl:text>&#xa;</xsl:text>
                        <p name="accession" value="{$accession}" /><xsl:text>&#xa;</xsl:text>
                        <p name="subEntityType" value="{$prog}" /><xsl:text>&#xa;</xsl:text>
                        <p name="strand" value="Forward" /><xsl:text>&#xa;</xsl:text>
                        <p name="query_align" value="{$query_align}" /><xsl:text>&#xa;</xsl:text>
                        <p name="subject_align" value="{$subject_align}" /><xsl:text>&#xa;</xsl:text>
                        <p name="midline" value="{$midline}" /><xsl:text>&#xa;</xsl:text>
                        <p name="order by" value="{$bitscore}" /><xsl:text>&#xa;</xsl:text>
                        <p name="order by tiebreaker" value="{$accession}" /><xsl:text>&#xa;</xsl:text>
                    </subentity><xsl:text>&#xa;</xsl:text>
                </xsl:if>
                <xsl:if test="$qend &lt; $qstart" >
                    <xsl:variable name="strand" select="-1" />
                    <subentity qstart="{$qend}" qend="{$qstart}" sstart="{$sstart}" send="{$send}" strand="{$strand}"><xsl:text>&#xa;</xsl:text>
                        <p name="score" value="{$score}" /><xsl:text>&#xa;</xsl:text>
                        <p name="bitscore" value="{$bitscore}" /><xsl:text>&#xa;</xsl:text>
                        <p name="evalue" value="{$evalue}" /><xsl:text>&#xa;</xsl:text>
                        <p name="accession" value="{$accession}" /><xsl:text>&#xa;</xsl:text>
                        <p name="subEntityType" value="{$prog}" /><xsl:text>&#xa;</xsl:text>
                        <p name="strand" value="Reverse" /><xsl:text>&#xa;</xsl:text>
                        <p name="query_align" value="{$query_align}" /><xsl:text>&#xa;</xsl:text>
                        <p name="subject_align" value="{$subject_align}" /><xsl:text>&#xa;</xsl:text>
                        <p name="midline" value="{$midline}" /><xsl:text>&#xa;</xsl:text>
                        <p name="order by" value="{$bitscore}" /><xsl:text>&#xa;</xsl:text>
                        <p name="order by tiebreaker" value="{$accession}" /><xsl:text>&#xa;</xsl:text>
                    </subentity><xsl:text>&#xa;</xsl:text>
                </xsl:if>
            </xsl:if>
        </entity><xsl:text>&#xa;</xsl:text>
    </xsl:template>

</xsl:stylesheet>