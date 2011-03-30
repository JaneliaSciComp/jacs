#! /usr/bin/perl -w

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

# code to generate a blastn run in the compute system
# Example usage: perl wsBlastNClient.pl tsafford 12345678 9876543210
use strict;
use SOAP::Lite;
print SOAP::Lite
   -> service('http://saffordt-ws1:8180/compute-compute/ComputeWS?wsdl')
   -> runBlastN($ARGV[0],              # owner
		'',                    # security token
		'08020',               # project id, used for grid submissions
		'',                    # work session id
		'New DB Blast Test',   # jobName
		$ARGV[1],              # subjectDatabaseNodeId
		$ARGV[2],              # queryFastaNodeId
                1,          # databaseAlignmentsPerQuery
		'F',        # lowComplexityFiltering
		1,          # eValueExponent
		0,    # lowercaseFiltering
		0,    # believeDefline - should always be false
		0,          # databaseSize
		-1,         # gapExtendCost
		1,     # gappedAlignment
		0,          # hitExtensionThreshold
		'BLOSUM62', # matrix
		0,          # multiHitWindow
		'both',     # searchStrand
		20,         # ungappedExtensionDropoff
		0,          # bestHitsToKeep
		50,         # finalGappedDropoff
		-1,         # gapOpenCost
		300,        # gappedAlignmentDropoff
		4,          # matchReward
		-5,         # mismatchPenalty
		0,          # searchSize
		0,    # showGIs
		11,         # wordcount
                '');     # blast output format types.  values: xml,btab,txt,tab,tabh xml,btab are used by default 
