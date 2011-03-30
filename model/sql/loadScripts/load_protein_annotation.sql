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

psql cameradb_dev -U postgres -c "truncate stage_protein_annotation"
psql cameradb_dev -U postgres -c "copy stage_protein_annotation(protein_acc,category,id,evidence) from stdin" < /usr/local/projects/GOSII/ANNOTATION/dbupload/GOSIO.dbupload.out

-------------------------------------------------------------------------------
/*
 * clean up leading/trailing blanks and malformed rows
 * change category to value used in production database
 */
update stage_protein_annotation set
  protein_acc=trim(' ' from protein_acc),
  category=trim(' ' from category),
  id=trim('.' from trim(' ' from id)),
  evidence=trim(' ' from evidence);
 
delete from stage_protein_annotation where coalesce(id,'')='';
 
update stage_protein_annotation set category=
  case category
    when 'common_name' then 'protein function'
    when 'TIGR_role' then 'TIGR role'
    else lower(replace(category,'_',' '))
  end;
/*
 * clean up EC data
 * manually add new EC ids to EC table
 */
update stage_protein_annotation set id='EC:'||id
  where category='EC';
 
update stage_protein_annotation set id=fix_ecid(id)
  where category='ec' and id not in (select ec_id from ec);

select distinct id
  from stage_protein_annotation
  where category='ec' and id not in (select ec_id from ec);

update stage_protein_annotation
  set id=split_part(id,'.',1)||'.'||split_part(id,'.',2)||'.-.-'
  where category='ec'  and id not in (select ec_id from ec);

update stage_protein_annotation set name=id, id=null
  where category in ('protein function','gene symbol');

delete from stage_protein_annotation
  where category='ec'  and id not in (select ec_id from ec);
 
update stage_protein_annotation set
  name=(select name from ec where ec_id=id)
  where category='ec';
/*
 * clean up GO data
 * manually add new GO ids to GO table (columns go_id, name, and category)
 */
select distinct id
  from stage_protein_annotation
  where category='go' and id not in (select go_id from go);

delete from stage_protein_annotation
  where category='go' and id not in (select go_id from go);
 
update stage_protein_annotation set
  name=(select name from go where go_id=id)
  where category='go';
/*
 * clean up TIGR role data
 * manually add new TIOGR roles to TIGR_role table
 */
update stage_protein_annotation set id='TR:'||id where category='TIGR_role';

select distinct id
  from stage_protein_annotation
  where category='TIGR role' and id not in (select tr_id from tigr_role);

update stage_protein_annotation set
  name=(select main_role||coalesce(': '||sub_roles,'') from tigr_role where tr_id=id)
  where category='TIGR role';
------------------------------------------------------------------
/*
 * standardize capitalization of protein functions and add new values to gene_name table
 */
insert into gene_name
  select 'CAM_CNAM_'||nextval('seq_cn'),'protein function',name,currval('seq_cn')
  from
    (select min(stg.name) as name
     from stage_protein_annotation stg
     where stg.category='protein function'
     and not exists (select 1 from gene_name gn where gn.nametype='protein function' and
     lower(gn.name)=lower(stg.name))
  group by lower(stg.name)) x;
 
update stage_protein_annotation
  set id=(select min(name_acc) from gene_name gn
          where gn.nametype='protein function' and lower(gn.name)=lower(stage_protein_annotation.name))
  where category='protein function';

update stage_protein_annotation
  set name=(select name from gene_name where name_acc=id)
  where category='protein function' and id>'';
/*
 * add new gene symbols to gene_name table
 */
insert into gene_name
  select 'CAM_GSYM_'||nextval('seq_cn'),'gene symbol',name,currval('seq_cn')
  from
    (select name from stage_protein_annotation where category='gene symbol'
     except
     select name from gene_name where nametype='gene symbol') x;
 
update stage_protein_annotation
  set id=(select name_acc from gene_name gn
          where gn.nametype='gene symbol'
          and gn.name=stage_protein_annotation.name
          and lower(gn.name)=lower(stage_protein_annotation.name))  -- index is on lower(name)
  where category='gene symbol';
/*
 * make sure all annotations have an assigned id and descriptive name
 */
select * from stage_protein_annotation
  where coalesce(id,'')='' or coalesce(name,'')='';

/*
 * append multi-evidence
 */
drop table pa;
create table pa as
  select protein_acc, id, name, category, min(evidence) as evidence
  from stage_protein_annotation
  group by protein_acc, id, name, category;
update pa
  set evidence=
    appendrows('select distinct evidence from stage_protein_annotation '||
               'where protein_acc='''||protein_acc||''' and id=\''||id||''' '||
               'order by 1',', ');
---------------------------------------------------------------------------------- 
insert into protein_annotation
  select se.entity_id,pa.protein_acc,pa.id,pa.name,pa.category,pa.evidence,null,null
  from pa
  inner join sequence_entity se on se.camera_acc=pa.protein_acc;
 
 
