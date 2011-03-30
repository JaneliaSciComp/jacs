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
-- massage peptide data
-- parse defline
update stage_fasta_pep
  set
    entity_id=nextval('uid_generated_sequence'),
    sequence_id=nextval('uid_generated_sequence'),
    entity_type_code=6,
    obs_flag=false,
    sequence_length=length(sequence),
    defline=trim(' ' from substring(defline from 1 for position(' /read_defline=' in defline))),
    orf_acc=get_def_tag(defline,'/orf_id=',' '),
    translation_table=get_def_tag(defline,'/ttable=',' '),
    stop_5_prime=get_def_tag(defline,'/5_prime_stop=',' '),
    stop_3_prime=get_def_tag(defline,'/3_prime_stop=',' '),
    dna_acc=get_def_tag(defline,'/read_id=',' '),
    dna_begin=get_def_tag(defline,'/begin=',' ')::integer,
    dna_end=get_def_tag(defline,'/end=',' ')::integer,
    dna_orientation=get_def_tag(defline,'/orientation=',' ')::integer;
--
-- inherit contextual data (assembly, sample, taxon, etc) from read
update stage_fasta_pep pep
  set
    dna_id=se.entity_id,
    defline=pep.defline||coalesce(' '||substring(se.defline from position('/sample_id=' in se.defline)),''),
    sample_acc=se.sample_acc,
    sample_id=se.sample_id,
    assembly_acc=se.assembly_acc,
    assembly_id=se.assembly_id,
    taxon_id=se.taxon_id,
    organism=se.organism,
    source_id=se.source_id,
    owner_id=se.owner_id
  from sequence_entity se
  where se.camera_acc=pep.dna_acc;
--
-- remove any undesirable peptides
delete from stage_fasta_pep
  where dna_id is null;     -- read not yet loaded
delete from stage_fasta_pep
  where sample_acc not in   -- not IO sample
    (select sample_acc from gos_io); 
--
-- remove dead space
--
-- remove mate if it slipped into defline
update stage_fasta_pep
  set defline=replace(defline,' /mate='||get_def_tag(defline,'/mate=',' '),'')
  where defline like '%/mate=%';
--
-- remove dead space
drop table sfp;
create table sfp as select * from stage_fasta_pep;
truncate stage_fasta_pep;
insert into stage_fasta_pep select * from sfp;
drop table sfp;
analyze stage_fasta_pep;
---------------------------------------------------------------------------------------------------------------
-- massage orf data
-- parse defline
update stage_fasta_nuc
  set
    entity_id=nextval('uid_generated_sequence'),
    sequence_id=nextval('uid_generated_sequence'),
    entity_type_code=5,
    obs_flag=false,
    sequence_length=length(sequence),
    defline=trim(' ' from substring(defline from 1 for position(' /read_defline=' in defline))),
    protein_acc=get_def_tag(defline,'/pep_id=',' '),
    translation_table=get_def_tag(defline,'/ttable=',' '),
    stop_5_prime=get_def_tag(defline,'/5_prime_stop=',' '),
    stop_3_prime=get_def_tag(defline,'/3_prime_stop=',' '),
    dna_acc=get_def_tag(defline,'/read_id=',' '),
    dna_begin=get_def_tag(defline,'/begin=',' ')::integer,
    dna_end=get_def_tag(defline,'/end=',' ')::integer,
    dna_orientation=get_def_tag(defline,'/orientation=',' ')::integer;
--
-- remove any undesirable orfs
delete from stage_fasta_nuc nuc
  where not exists
   (select camera_acc from stage_fasta_pep pep where pep.camera_acc=nuc.protein_acc);
--
-- inherit contextual data (assembly, sample, taxon, etc) from read
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
--
-- remove mate if it slipped into defline
update stage_fasta_nuc
  set defline=replace(defline,' /mate='||get_def_tag(defline,'/mate=',' '),'')
  where defline like '%/mate=%';
--
-- remove dead space
drop table sfn;
create table sfn as select * from stage_fasta_nuc;
truncate stage_fasta_nuc;
insert into stage_fasta_nuc select * from sfn;
drop table sfn;
analyze stage_fasta_nuc;

---------------------------------------------------------------------------------------------------------------
-- synchronize orf/peptide references
update stage_fasta_nuc nuc set protein_id=
  (select pep.entity_id from stage_fasta_pep pep where pep.camera_acc=nuc.protein_acc);
update stage_fasta_pep pep set orf_id=
  (select nuc.entity_id from stage_fasta_nuc nuc where nuc.camera_acc=pep.orf_acc);
/*
 * no old IO features
--
-- forward track old features to new
-- (code below needs to be revised when requirements
--  for matching old features to new are agreed upon)
update stage_fasta_nuc nuc
  set
    replacement_for=se.camera_acc
  from sequence_entity se
  where se.dna_id=nuc.dna_id
  and se.dna_begin=nuc.dna_begin
  and se.dna_end=nuc.dna_end
  and se.dna_orientation=nuc.dna_orientation;
set enable_mergejoin=false;
update stage_fasta_pep pep
  set
    replacement_for=se.camera_acc
  from sequence_entity se
  where se.dna_id=pep.dna_id
  and se.dna_begin=pep.dna_begin
  and se.dna_end=pep.dna_end
  and se.dna_orientation=pep.dna_orientation;
 *
 */
---------------------------------------------------------------------------------------------------------------
-- move data into production tables
insert into bio_sequence
  select sequence_id,2,sequence,md5(sequence),source_id from stage_fasta_nuc;
insert into sequence_entity(
    assembly_acc,assembly_id,camera_acc,clear_range_begin,clear_range_end,comment,defline,
    dna_acc,dna_begin,dna_end,dna_id,dna_orientation,
    entity_id,entity_type_code,external_acc,external_source,
    library_acc,library_id,locus,ncbi_gi_number,obs_flag,
    orf_acc,orf_id,organism,owner_id,protein_acc,protein_id,
    replaced_by,sample_acc,sample_id,sequence_id,sequence_length,
    sequencing_direction,source_id,stop_3_prime,stop_5_prime,
    taxon_id,template_acc,trace_acc,translation_table,type)
  select
    assembly_acc,assembly_id,camera_acc,clear_range_begin,clear_range_end,comment,defline,
    dna_acc,dna_begin,dna_end,dna_id,dna_orientation,
    entity_id,entity_type_code,external_acc,external_source,
    library_acc,library_id,locus,ncbi_gi_number,obs_flag,
    orf_acc,orf_id,organism,owner_id,protein_acc,protein_id,
    replaced_by,sample_acc,sample_id,sequence_id,sequence_length,
    sequencing_direction,source_id,stop_3_prime,stop_5_prime,
    taxon_id,template_acc,trace_acc,translation_table,type
  from stage_fasta_nuc;

insert into bio_sequence
  select sequence_id,1,sequence,md5(sequence),source_id from stage_fasta_pep;
insert into sequence_entity(
    assembly_acc,assembly_id,camera_acc,clear_range_begin,clear_range_end,comment,defline,
    dna_acc,dna_begin,dna_end,dna_id,dna_orientation,
    entity_id,entity_type_code,external_acc,external_source,
    library_acc,library_id,locus,ncbi_gi_number,obs_flag,
    orf_acc,orf_id,organism,owner_id,protein_acc,protein_id,
    replaced_by,sample_acc,sample_id,sequence_id,sequence_length,
    sequencing_direction,source_id,stop_3_prime,stop_5_prime,
    taxon_id,template_acc,trace_acc,translation_table,type)
  select
    assembly_acc,assembly_id,camera_acc,clear_range_begin,clear_range_end,comment,defline,
    dna_acc,dna_begin,dna_end,dna_id,dna_orientation,
    entity_id,entity_type_code,external_acc,external_source,
    library_acc,library_id,locus,ncbi_gi_number,obs_flag,
    orf_acc,orf_id,organism,owner_id,protein_acc,protein_id,
    replaced_by,sample_acc,sample_id,sequence_id,sequence_length,
    sequencing_direction,source_id,stop_3_prime,stop_5_prime,
    taxon_id,template_acc,trace_acc,translation_table,type
  from stage_fasta_pep;
