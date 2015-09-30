#!/usr/local/perl

#
# Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
# 
# This file is part of JCVI VICS.
# 
# JCVI VICS is free software; you can redistribute it and/or modify it 
# under the terms and conditions of the Artistic License 2.0.  For 
# details, see the full text of the license in the file LICENSE.txt.  
# No other rights are granted.  Any and all third party software rights 
# to remain with the original developer.
# 
# JCVI VICS is distributed in the hope that it will be useful in 
# bioinformatics applications, but it is provided "AS IS" and WITHOUT 
# ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to implied 
# warranties of merchantability or fitness for any particular purpose.  
# For details, see the full text of the license in the file LICENSE.txt.
# 
# You should have received a copy of the Artistic License 2.0 along with 
# JCVI VICS.  If not, the license can be obtained from 
# "http://www.perlfoundation.org/artistic_license_2_0."
# 

use strict;
use SOAP::Lite;
print SOAP::Lite
   -> service('http://saffordt-ws1:8180/compute-compute/ComputeWS?wsdl')
   -> runBlastP(getlogin,                     # owner
                '0',                          # not used
                '08020',                      # project code
                'mgAnno BlastP for Megan',    # jobName
                '1351610549003094709',        # subjectDatabaseNodeId, PANDA Allgroup
                '1352790563782396346',        # queryFastaNodeId, fill in your query node
                10,                           # databaseAlignmentsPerQuery, -b=10 (-b/-v treated as same parameter by VICS)
                'T',                          # filter, -F
                -5,                           # eValueExponent, -e=1e-5
                0,                            # lowercaseFiltering
                0,                            # believeDefline - should always be false
                0,                            # databaseSize
                -1,                           # gapExtendCost
                1,                            # gappedAlignment
                0,                            # hitExtensionThreshold
                0,                            # multiHitWindow
                0,                            # showGIs
                0,                            # wordcount
                0,                            # bestHitsToKeep
                0,                            # finalGappedDropoff
                -1,                           # gapOpenCost
                0,                            # gappedAlignmentDropoff
                'BLOSUM62',                   # matrix
                0,                            # searchSize
                0);                           # ungappedExtensionDropoff
