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

truncate stage_protein_annotation;
copy stage_protein_annotation(protein_acc,category,id,evidence) from '/db/pgsql/work/anno.txt';

----------- REMOVE WHITESPACE and STANDARDIZE IDS and CATEGORIES --------------- 
update stage_protein_annotation set
  protein_acc=trim(' ' from protein_acc),
  category=trim(' ' from category),
  id=trim('.' from trim(' ' from id)),
  evidence=trim(' ' from evidence);
 
delete from stage_protein_annotation where coalesce(id,'')='';
 
update stage_protein_annotation set id='TR:'||id where category='TIGR_role';
 
update stage_protein_annotation set id='EC:'||id where category='EC';
 
update stage_protein_annotation set category=
  case category
    when 'common_name' then 'protein function'
    when 'TIGR_role' then 'TIGR role'
    else lower(replace(category,'_',' '))
  end;
update stage_protein_annotation set name=id, id=null where category in ('gene symbol','protein function');
 
----------- CLEAN-UP EC IDENTIFIERS --------------- 
update stage_protein_annotation set id=fix_ecid(id) where category='ec' and id not in (select ec_id from ec);
update stage_protein_annotation set id=split_part(id,'.',1)||'.'||split_part(id,'.',2)||'.-.-'
  where category='ec' and id not in (select ec_id from ec);

----------- FIND and ADD any NEW GO/EC/TR DEFINITIONS --------------- 
select id from stage_protein_annotation where id like 'EC:%' except select ec_id from ec;
select id from stage_protein_annotation where id like 'GO:%' except select go_id from go;
select id from stage_protein_annotation where id like 'TR:%' except select tr_id from tigr_role;

----------- CLEAN-UP and ASSIGN ACCESSIONS to PROTEIN FUNCTIONS --------------- 
insert into gene_name
  select 'CAM_CNAM_'||nextval('seq_cn'),'protein function',name,currval('seq_cn')
  from
    (select min(stg.name) as name
     from stage_protein_annotation stg
     where stg.category='protein function'
     and not exists (select 1 from gene_name gn where gn.nametype='protein function' and lower(gn.name)=lower(stg.name))
     group by lower(stg.name)) x;
 
update stage_protein_annotation set
  id=(select min(name_acc) from gene_name gn where gn.nametype='protein function' and lower(gn.name)=lower(stage_protein_annotation.name))
  where category='protein function';
 
update stage_protein_annotation set name=(select name from gene_name where name_acc=id) where category='protein function' and id>'';
 
----------- ASSIGN ACCESSIONS to GENE SYMBOLS --------------- 
insert into gene_name
  select 'CAM_GSYM_'||nextval('seq_cn'),'gene symbol',name,currval('seq_cn')
  from
    (select name from stage_protein_annotation where category='gene symbol'
     except
     select name from gene_name where nametype='gene symbol') x;
 
update stage_protein_annotation set
  id=(select name_acc from gene_name gn where gn.nametype='gene symbol' and gn.name=stage_protein_annotation.name and lower(gn.name)=lower(stage_protein_annotation.name))
  where category='gene symbol';

----------- FILL IN NAMES for EC/GO/TR --------------- 
update stage_protein_annotation set
  name=(select name from ec where ec_id=id)
  where category='ec';

update stage_protein_annotation set
  name=(select name from go where go_id=id)
  where category='go';
 
update stage_protein_annotation set
  name=(select main_role||coalesce(': '||sub_roles,'') from tigr_role where tr_id=id)
  where category='TIGR role';
 
----------- SAVE DATA in HOLDING TABLE ----------------
vacuum verbose analyze camera.stage_protein_annotation;
drop table camera.add_protein_annotation;
create table camera.add_protein_annotation as select * from protein_annotation limit 0;

set enable_hashjoin=false;
set enable_mergejoin=false;
insert into camera.add_protein_annotation
  select se.entity_id,a.protein_acc,a.id,a.name,a.category,min(a.evidence),null,null
  from stage_protein_annotation a inner join sequence_entity se on se.camera_acc=a.protein_acc
  group by se.entity_id,a.protein_acc,a.id,a.name,a.category;
update camera.add_protein_annotation
  set evidence=camera.appendrows('select distinct evidence from camera.stage_protein_annotation '||
                                 'where protein_acc='''||protein_acc||''' and id='''||id||''' order by 1',', ');
set enable_hashjoin=true;
set enable_mergejoin=true;
