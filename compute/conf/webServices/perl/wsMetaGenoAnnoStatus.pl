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

# code to return the state of blast processing
# terminal states are 'error' and 'completed'
# Example usage: perl wsBlastStatus.pl 11713267437827
use strict;
use SOAP::Lite;
print SOAP::Lite
   -> service('http://localhost:8080/compute-compute/Blast?wsdl')
   -> getMetaGenoAnnotationStatus('smurphy',   # owner
                                  $ARGV[0]);   # passed in - job id given from the execution
