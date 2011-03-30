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

create index sequence_entity_ix_taxon on sequence_entity using btree (taxon_id)
create index sequence_entity_ix_assembly on sequence_entity using btree (assembly_acc)
create index sequence_entity_ix_sample on sequence_entity using btree (sample_acc)
create index sequence_entity_ix_dna_acc on sequence_entity using btree (dna_acc)
create index sequence_entity_ix_non_system_owner on sequence_entity using btree (owner_id) where (owner_id <> 60)
create index sequence_entity_ix_external_source on sequence_entity using btree (external_source) where (source_id = 2 or source_id = 3)
create index sequence_entity_ix_read_library_template on sequence_entity using btree (library_acc,template_acc) where (entity_type_code=3)
create index sequence_entity_ix_orf_protein on sequence_entity using btree (protein_acc) where (entity_type_code = 5)
create index sequence_entity_ix_protein_orf on sequence_entity using btree (orf_acc) where (entity_type_code = 6)
create index sequence_entity_ix_other_type on sequence_entity using btree (entity_type_code) where (((entity_type_code <> 3) and (entity_type_code <> 5)) and (entity_type_code <> 6))
