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

package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Sep 22, 2010
 * Time: 12:16:04 PM
 */
public class Fasta2BsmlTask extends Task {

    /*

        OPTIONS

        B<--fasta_input>
            Input files or folders.  Can be a comma-separated list of mixed input types.

        B<--fasta_list>
            Text file that is a list of input files and/or folders.

        B<--format>
            Format.  'multi' (default) writes all sequences to a multi-entry bsml file, and 'single' writes each sequence in a separate file named like $id.bsml

        B<--class>
            Sets the class attribute of each Sequence element created.  Default = assembly.
        DESCRIPTION

        B<--organism> Flag that if set will set the Genus and Species name to the name of the
        fasta file being used

        This script is used to convert fasta to BSML.  The input is meant to be as flexible
        as possible and is described below.  The output can be either a single file with
        multiple <Sequence> entries, or separate files for each entry.     

     */

    transient public static final String PARAM_fasta_input = "fasta input";
    transient public static final String PARAM_fasta_list = "fasta list";
    transient public static final String PARAM_format = "format";
    transient public static final String PARAM_class = "class";
    transient public static final String PARAM_organism = "organsim";
    transient public static final String PARAM_genus = "genus";
    transient public static final String PARAM_species = "species";
    
    transient public static final String PARAM_fasta_input_node_id = "input node id";
    transient public static final String PARAM_format_defult = "multi";
    
    public Fasta2BsmlTask() {
        super();
        setTaskName("Fasta2BsmlTask");
        setParameter(PARAM_fasta_input_node_id, "");
        setParameter(PARAM_fasta_input, "");
        setParameter(PARAM_fasta_list, "");
        setParameter(PARAM_format, PARAM_format_defult);
    }

    public String getDisplayName() {
        return "Fasta2BsmlTask";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        // no match
        return null;
    }

    public String generateCommandOptions(String outputDirectory) throws ParameterException {
        StringBuffer sb = new StringBuffer();
        addCommandParameter(sb, "--fast_input", PARAM_fasta_input);
        addCommandParameter(sb, "--fasta_list", PARAM_fasta_list);
        addCommandParameter(sb, "--format", PARAM_format);
        addCommandParameter(sb, "--class", PARAM_class);

        return sb.toString();
    }

}