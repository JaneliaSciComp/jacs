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

-- **************************************************************************
-- *!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*
-- *!!!                                                                  !!!*
-- *!!! do NOT run this script until ALL panda files have been processed !!!*
-- *!!!                                                                  !!!*
-- *!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*
-- **************************************************************************
--
-- *********************************************
-- * move PANDA updates into production tables *
-- *********************************************
set search_path=panda,camera,public;
set enable_hashjoin=false;
set enable_mergejoin=false;
set enable_seqscan=false;
--
-- remove sequences that belong to the metagenomic set
begin transaction;
-- est 20 minutes
  delete from panda_entity
    where ncbi_gi_number in
      (select ncbi_gi_number from sequence_entity
       where source_id=1 and ncbi_gi_number>0 and entity_type_code+0 in (6,7));
-- est 15 minutes
  delete from panda_entity
    where external_source in ('RF','NCBI') and split_part(external_acc,'.',1) in
      (select split_part(external_acc,'.',1) from sequence_entity
       where source_id=1 and entity_type_code+0 in (6,7)
       and external_source in ('RF','NCBI') and split_part(external_acc,'.',1)>'');
end transaction;
--
-- assign sample/assembly to MF taxons
update panda_entity f
  set sample_id=s.sample_id, sample_acc=s.sample_acc, assembly_id=a.assembly_id, assembly_acc=a.assembly_acc
  from assembly a inner join bio_sample s on s.sample_acc=a.sample_acc
  where a.assembly_acc like 'MF%' and f.taxon_id=a.taxon_id;
select s.sample_name, a.organism, a.taxon_id,
      (select count(*) from panda_entity e
       where e.taxon_id=a.taxon_id) as new_seqcnt,
      (select count(*) from sequence_entity e
       where e.sample_id=s.sample_id and not e.obs_flag and e.entity_type_code+0 in (6,7) and e.source_id=2) as old_seqcnt
  from assembly a
  inner join bio_sample s on s.sample_acc=a.sample_acc
  where a.assembly_acc like 'MF%';
--
-- vaccum/analyze PANDA tables
vacuum verbose analyze panda_entity;
vacuum verbose analyze panda_sequence;
--
-- add new taxonomy data
insert into taxonomy
  select * from panda_taxonomy;
insert into taxonomy_children
  select * from panda_taxonomy_children;
insert into taxonomy_synonym
  select taxon_id,preferred_name,'scientific name'
    from panda_taxonomy;
begin transaction;
  truncate taxonomy_names;
  insert into taxonomy_names
    select * from taxonomy_names_view;
  analyze taxonomy_names;
end transaction;
--
-- update sequence_entity
begin transaction;
  delete from sequence_entity where entity_id in (select entity_id from panda_entity);
  update sequence_entity set obs_flag=true where source_id=2 and not obs_flag;
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
    from panda_entity;
end transaction;
--
-- update bio_sequence
begin transaction;
  delete
    from bio_sequence
    where sequence_id in (select sequence_id from panda_sequence);
  insert into bio_sequence(sequence_id,sequence_type_code,sequence,sequence_md5,source_id)
    select sequence_id, case when entity_type_code=0 then 2 else 1 end, sequence, sequence_md5, 2
    from panda_sequence;
end transaction;
--
-- make sure no sequences are missing
select count(*) as num_missing_sequences
  from (select sequence_id from sequence_entity where source_id=2 and not obs_flag
        except
        select sequence_id fropm bio_sequence where soucre_id=2)  missing_sequences;
--
-- vacuum/analyze sequence tables
vacuum verbose analyze sequence_entity;
vacuum verbose analyze bio_sequence;
--
-- purge staging tables
/*
truncate
  panda.panda_entity,
  panda.panda_sequence,
  panda.panda_taxonomy,
  panda.panda_taxonomy_children,
  panda.stage_fasta_nuc,
  panda.stage_fasta_pep
 */
