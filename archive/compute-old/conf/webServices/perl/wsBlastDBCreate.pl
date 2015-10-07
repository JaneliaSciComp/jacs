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

# code to generate a blast database in the compute system
# Example usage: perl wsBlastDBCreate.pl tsafford '' 'CL Test' 'CL Description' /local/camera/fasta/testDB.fasta
use strict;
use SOAP::Lite;
print SOAP::Lite
   -> service('http://saffordt-ws1:8180/compute-compute/ComputeWS?wsdl')
   -> uploadAndFormatBlastDataset($ARGV[0], # owner
				  '',       # security token, unused
				  $ARGV[1], # work session id       
				  $ARGV[2],   # name of the blast database
				  $ARGV[3],   # description of the blast database
				  $ARGV[4]);  # absolute path to the fasta file
