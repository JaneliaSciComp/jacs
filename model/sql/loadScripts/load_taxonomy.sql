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

create or replace function ncbi_taxon_ancestors() returns void as $$
declare
  i integer := 1;
  eod boolean := false;
  ancestor ncbi_taxonomy_children ;
begin
  while not eod
  loop
    eod := true;
    for ancestor in select p.parent_id, c.child_id, i+1 from ncbi_taxonomy_children c, ncbi_taxonomy_children p where c.depth=i and p.child_id=c.parent_id and p.depth=1
    loop
      eod := false;
      insert into ncbi_taxonomy_children(parent_id,child_id,depth) values(ancestor.parent_id, ancestor.child_id, ancestor.depth );
    end loop;
    i := i + 1;
  end loop;
  return;
end $$ language plpgsql;
 
cat nodes.dmp | awk ' BEGIN { FS="\t"; OFS="\t" }{ print $1,$3,$5}' > nodes.tsv
psql cameradb_dev -U postgres -c "drop table ncbi_tax_node"
psql cameradb_dev -U postgres -c "create table ncbi_tax_node (taxon_id integer, parent_id integer, rank text)"
psql cameradb_dev -U postgres -c "copy ncbi_tax_node from stdin with delimiter E'\t'" <  nodes.tsv
cat names.dmp | awk ' BEGIN { FS="\t"; OFS="\t" }{ print $1,$3,$5,$7}' > names.tsv
psql cameradb_dev -U postgres -c "drop table ncbi_tax_name"
psql cameradb_dev -U postgres -c "create table ncbi_tax_name (taxon_id integer, name text, unique_name text, name_class text)"
psql cameradb_dev -U postgres -c "copy ncbi_tax_name from stdin with delimiter E'\t'" <  names.tsv
psql cameradb_dev -U postgres -c "create index ncbi_tax_name_ix_lower_name on ncbi_tax_name(lower(name))"
psql cameradb_dev -U postgres -c "create index ncbi_tax_name_ix_taxon on ncbi_tax_name(taxon_id)"

psql cameradb_dev -U postgres -c "drop table ncbi_taxonomy"
psql cameradb_dev -U postgres -c "create table ncbi_taxonomy as select taxon_id, (select min(name) from ncbi_tax_name where ncbi_tax_name.taxon_id=ncbi_tax_node.taxon_id and ncbi_tax_name.name_class='scientific name') as preferred_name, false as is_obsolete from ncbi_tax_node"
psql cameradb_dev -U postgres -c "insert into ncbi_taxonomy select taxon_id,preferred_name,true from taxonomy where taxon_id in (select taxon_id from taxonomy except select taxon_id from ncbi_taxonomy) and exists(select 1 from sequence_entity e where e.taxon_id=taxonomy.taxon_id)"
psql cameradb_dev -U postgres -c "create index ncbi_taxonomy_ix_taxon on ncbi_taxonomy(taxon_id)"
psql cameradb_dev -U postgres -c "create table ncbi_taxonomy_synonym as select taxon_id, name, name_class as name_type from ncbi_tax_name"
psql cameradb_dev -U postgres -c "insert into ncbi_taxonomy_synonym select taxon_id, name, name_type from taxonomy_synonym where taxon_id in (select taxon_id from ncbi_taxonomy where is_obsolete)"

psql cameradb_dev -U postgres -c "drop table ncbi_taxonomy_children"
psql cameradb_dev -U postgres -c "create table ncbi_taxonomy_children as select taxon_id as parent_id, taxon_id as child_id, 0 as depth from ncbi_taxonomy"
psql cameradb_dev -U postgres -c "insert into ncbi_taxonomy_children select parent_id, taxon_id, 1 from ncbi_tax_node"
psql cameradb_dev -U postgres -c "insert into ncbi_taxonomy_children select parent_id, child_id, depth from taxonomy_children where depth=1 and child_id in -- manually splint obsolete orphans back into hierarchy
psql cameradb_dev -U postgres -c "insert into ncbi_taxonomy_children select c.parent_id, c.child_id, 1 from ncbi_taxonomy n, taxonomy_children c, ncbi_taxonomy p where n.is_obsolete and c.child_id=n.taxon_id and p.taxon_id=c.parent_id and not exists(select 1 from ncbi_taxonomy_children ntc where ntc.child_id=n.taxon_id) and c.depth=1"
psql cameradb_dev -U postgres -c "insert into ncbi_taxonomy_children select c.parent_id, c.child_id, 1 from ncbi_taxonomy n, taxonomy_children c, ncbi_taxonomy p where n.is_obsolete and c.child_id=n.taxon_id and p.taxon_id=c.parent_id and not exists(select 1 from ncbi_taxonomy_children ntc where ntc.child_id=n.taxon_id) and c.depth=2"
psql cameradb_dev -U postgres -c "insert into ncbi_taxonomy_children select c.parent_id, c.child_id, 1 from ncbi_taxonomy n, taxonomy_children c, ncbi_taxonomy p where n.is_obsolete and c.child_id=n.taxon_id and p.taxon_id=c.parent_id and not exists(select 1 from ncbi_taxonomy_children ntc where ntc.child_id=n.taxon_id) and c.depth=3"
psql cameradb_dev -U postgres -c "insert into ncbi_taxonomy_children select c.parent_id, c.child_id, 1 from ncbi_taxonomy n, taxonomy_children c, ncbi_taxonomy p where n.is_obsolete and c.child_id=n.taxon_id and p.taxon_id=c.parent_id and not exists(select 1 from ncbi_taxonomy_children ntc where ntc.child_id=n.taxon_id) and c.depth=4"
psql cameradb_dev -U postgres -c "delete from ncbi_taxonomy_children where parent_id=1"
psql cameradb_dev -U postgres -c "create index ncbi_taxonomy_children_ix_depthchild on ncbi_taxonomy_children(depth,child_id)"
create or replace function ncbi_taxon_ancestors() returns void as $$
declare
  i integer := 1;
  eod boolean := false;
  ancestor ncbi_taxonomy_children ;
begin
  while not eod
  loop
    eod := true;
    for ancestor in select p.parent_id, c.child_id, i+1 from ncbi_taxonomy_children c, ncbi_taxonomy_children p where c.depth=i and p.child_id=c.parent_id and p.depth=1
    loop
      eod := false;
      insert into ncbi_taxonomy_children(parent_id,child_id,depth) values(ancestor.parent_id, ancestor.child_id, ancestor.depth );
    end loop;
    i := i + 1;
  end loop;
  return;
end $$ language plpgsql;
psql cameradb_dev -U postgres -c "select ncbi_taxon_ancestors()"

psql cameradb_dev -U postgres -c "truncate table taxonomy"
psql cameradb_dev -U postgres -c "insert into taxonomy(taxon_id,preferred_name,is_obsolete) select taxon_id,preferred_name,is_obsolete from ncbi_taxonomy"
psql cameradb_dev -U postgres -c "analyze taxonomy"

psql cameradb_dev -U postgres -c "truncate table taxonomy_synonym"
psql cameradb_dev -U postgres -c "insert into taxonomy_synonym select taxon_id,name,min(name_type) from ncbi_taxonomy_synonym group by taxon_id,name"
psql cameradb_dev -U postgres -c "analyze taxonomy_synonym"

psql cameradb_dev -U postgres -c "truncate table taxonomy_children"
psql cameradb_dev -U postgres -c "insert into taxonomy_children select * from ncbi_taxonomy_children"
psql cameradb_dev -U postgres -c "analyze taxonomy_children"

psql cameradb_dev -U postgres -c "truncate table taxonomy_tag"
psql cameradb_dev -U postgres -c "insert into taxonomy_tag select 'viroids',child_id from (select child_id from taxonomy_children where parent_id=12884) x"
psql cameradb_dev -U postgres -c "insert into taxonomy_tag select distinct 'archaea',child_id from (select child_id from camera.taxonomy_children where parent_id=2157) x"
psql cameradb_dev -U postgres -c "insert into taxonomy_tag select distinct 'bacteria',child_id from (select child_id from camera.taxonomy_children where parent_id=2) x"
psql cameradb_dev -U postgres -c "insert into taxonomy_tag select distinct 'viruses',child_id from (select child_id from taxonomy_children where parent_id=10239) x"
psql cameradb_dev -U postgres -c "insert into taxonomy_tag select distinct 'eukaryota',child_id from (select child_id from taxonomy_children where parent_id=2759) x"
psql cameradb_dev -U postgres -c "insert into taxonomy_tag select distinct 'other_taxons',child_id from (select child_id from taxonomy_children where parent_id in (3193,33208)) x"
psql cameradb_dev -U postgres -c "insert into taxonomy_tag select distinct 'fungi',child_id from (select child_id from camera.taxonomy_children where parent_id=4751) x"
psql cameradb_dev -U postgres -c "insert into taxonomy_tag select distinct 'microbes',child_id from (select child_id from taxonomy_children where parent_id=131567 except select child_id from taxonomy_children where parent_id in (3193,33208)) x"
psql cameradb_dev -U postgres -c "insert into taxonomy_tag select distinct 'prokaryota',child_id from (select child_id from taxonomy_children where parent_id in (2,2157)) x"
psql cameradb_dev -U postgres -c "insert into taxonomy_tag select distinct 'viral',child_id from (select child_id from taxonomy_children where parent_id in (10239,12884)) x"
psql cameradb_dev -U postgres -c "analyze taxonomy_tag"

create or replace view taxonomy_names_view as
  select taxon_id, case when names='' then preferred_name else names end as names from (
   select taxon_id,
     appendrows('select name from taxonomy_synonym where
          CASE name_type
            WHEN ''scientific name'' THEN 1
            WHEN ''equivalent name'' THEN 2
            WHEN ''synonym'' THEN 3
            WHEN ''common name'' THEN 4
            WHEN ''acronym'' THEN 5
            WHEN ''teleomorph'' THEN 5
            WHEN ''holomorph'' THEN 5
            ELSE 99
          END<99 and taxon_id='||taxon_id||
          ' group by name order by min(CASE name_type
            WHEN ''scientific name'' THEN 1
            WHEN ''equivalent name'' THEN 2
            WHEN ''synonym'' THEN 3
            WHEN ''common name'' THEN 4
            WHEN ''anamorph'' THEN 5
            WHEN ''teleomorph'' THEN 5
            WHEN ''holomorph'' THEN 5
            ELSE 99
        END)','; ') as names, preferred_name from taxonomy) x;
psql cameradb_dev -U postgres -c "truncate taxonomy_names"
psql cameradb_dev -U postgres -c "insert into taxonomy_names select * from taxonomy_names_view"
psql cameradb_dev -U postgres -c "analyze taxonomy_names"

psql cameradb_dev -U postgres -c "drop ncbi_tax_node, ncbi_tax_name, ncbi_taxonomy_children, ncbi_taxonomy_synonym, ncbi_taxonomy"
 
 
