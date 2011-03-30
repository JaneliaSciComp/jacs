/**************************************************************************
  Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.

  This file is part of JCVI VICS.

  JCVI VICS is free software; you can redistribute it and/or modify it 
  under the terms and conditions of the Artistic License 2.0.  For 
  details, see the full text of the license in the file LICENSE.txt.  
  No other rights are granted.  Any and all third party software rights 
  to remain with the original developer.

  JCVI VICS is distributed in the hope that it will be useful in 
  bioinformatics applications, but it is provided "AS IS" and WITHOUT 
  ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to implied 
  warranties of merchantability or fitness for any particular purpose.  
  For details, see the full text of the license in the file LICENSE.txt.

  You should have received a copy of the Artistic License 2.0 along with 
  JCVI VICS.  If not, the license can be obtained from 
  "http://www.perlfoundation.org/artistic_license_2_0."
***************************************************************************/

--
-- RNA
--
-- parse defline
update stage_fasta_nuc
  set
    entity_id=nextval('uid_generated_sequence'),
    sequence_id=nextval('uid_generated_sequence'),
    entity_type_code=8,
    obs_flag=false,
    sequence_length=length(sequence),
    type=get_def_tag(defline,'/type=',' '),
    defline=trim(' ' from substring(defline from 1 for position(' /read_defline=' in defline))),
    dna_acc=get_def_tag(defline,'/read_id=',' '),
    dna_begin=get_def_tag(defline,'/begin=',' ')::integer,
    dna_end=get_def_tag(defline,'/end=',' ')::integer,
    dna_orientation=get_def_tag(defline,'/orientation=',' ')::integer;
--
-- inherited attributes from read
update stage_fasta_nuc nuc
  set
    dna_id=se.entity_id,
    defline=nuc.defline||coalesce(' '||substring(se.defline from position('/sample_id=' in se.defline)),''),
    sample_acc=se.sample_acc,
    sample_id=se.sample_id,
    assembly_acc=se.assembly_acc,
    assembly_id=se.assembly_id,
    taxon_id=se.taxon_id,
    organism=se.organism,
    source_id=se.source_id,
    owner_id=se.owner_id
  from sequence_entity se
  where se.camera_acc=nuc.dna_acc;
--
-- remove features from unloaded reads
delete from stage_fasta_nuc
  where dna_id is null;
--
-- remove mate if it slipped into defline
update stage_fasta_nuc
  set defline=replace(defline,' /mate='||get_def_tag(defline,'/mate=',' '),'')
  where defline like '%/mate=%';
/*
--
-- identify any old rna's matching this one
set enable_mergejoin=false;
update stage_fasta_nuc nuc
  set
    replacement_for=se.camera_acc
  from sequence_entity se
  where se.dna_id=nuc.dna_id
  and se.dna_begin=nuc.dna_begin
  and se.dna_end=nuc.dna_end
  and se.dna_orientation=nuc.dna_orientation;
 */
--
-- move data into production tables
insert into bio_sequence
  select sequence_id,2,sequence,md5(sequence),1 from stage_fasta_nuc;
insert into sequence_entity(
    assembly_acc,assembly_id,camera_acc,clear_range_begin,clear_range_end,comment,defline,dna_acc,dna_begin,dna_end,dna_id,dna_orientation,entity_id,
    entity_type_code,external_acc,external_source,library_acc,library_id,locus,ncbi_gi_number,obs_flag,orf_acc,orf_id,organism,owner_id,protein_acc,
    protein_id,replaced_by,sample_acc,sample_id,sequence_id,sequence_length,sequencing_direction,source_id,stop_3_prime,stop_5_prime,taxon_id,template_acc,
    trace_acc,translation_table,type)
  select
    assembly_acc,assembly_id,camera_acc,clear_range_begin,clear_range_end,comment,defline,dna_acc,dna_begin,dna_end,dna_id,dna_orientation,entity_id,
    entity_type_code,external_acc,external_source,library_acc,library_id,locus,ncbi_gi_number,obs_flag,orf_acc,orf_id,organism,owner_id,protein_acc,
    protein_id,replaced_by,sample_acc,sample_id,sequence_id,sequence_length,sequencing_direction,source_id,stop_3_prime,stop_5_prime,taxon_id,template_acc,
    trace_acc,translation_table,type
  from stage_fasta_nuc;


