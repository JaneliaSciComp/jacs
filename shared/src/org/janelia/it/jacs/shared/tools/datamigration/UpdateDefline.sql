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

drop table tmp_internal_sequence_entity_with_sample;
drop table tmp_pointer_for_internal_sequence_entity_with_sample;

create table tmp_internal_sequence_entity_with_sample (
entity_id            bigint not null,
camera_acc           character varying(255),
defline              text,
owner_id             bigint,
sequence_id          bigint,
sequence_length      integer,
entity_type_code     integer,
external_source      character varying(20),
external_acc         character varying(40),
ncbi_gi_number       integer,
comment              character varying(1000),
assembly_acc         character varying(255),
library_acc          character varying(255),
sample_acc           character varying(255),
organism             character varying(255),
taxon_id             integer,
locus                character varying(255),
protein_acc          character varying(255),
orf_acc              character varying(255),
dna_acc              character varying(255),
dna_begin            integer,
dna_end              integer,
dna_orientation      integer,
translation_table    character varying(8),
stop_5_prime         character varying(3),
stop_3_prime         character varying(3),
trace_acc            character varying(255),
template_acc         character varying(255),
clear_range_begin    integer,
clear_range_end      integer,
sequencing_direction character varying(255),
type                 character varying(40),
strain               character varying(255),
source_id            integer default 0,
obs_flag             boolean default false
);

create table tmp_pointer_for_internal_sequence_entity_with_sample (
curr_offset integer,
last_entity_id bigint,
last_sequence_acc character varying(255)
);

CREATE OR REPLACE FUNCTION populateTmpSeqEntityWithSeqTableInSteps(maxrows integer,steps integer)
RETURNS void AS $$
DECLARE
current_step integer;
last_entity_id record;
BEGIN
for current_step in 1..$2 loop
select * into last_entity_id from populateTmpSeqEntityWithSeqTable($1);
raise notice 'LAST ENTITY ID: %',
last_entity_id.populateTmpSeqEntityWithSeqTable;
end loop;
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION populateTmpSeqEntityWithSeqTable(integer,integer);
CREATE OR REPLACE FUNCTION populateTmpSeqEntityWithSeqTable(maxrows integer,
pagesize integer)
RETURNS RECORD AS $$
DECLARE
status_row record;
seq_ent record;
secursor CURSOR (key bigint) IS
SELECT *
FROM camera.sequence_entity se
WHERE se.entity_id > key
ORDER BY se.entity_id;
nrows integer;
pagesize integer;
BEGIN
nrows := 0;
if $2 = 0 then pagesize := 1;
else pagesize := $2;
end if;
select * into status_row
from camera.tmp_pointer_for_internal_sequence_entity_with_sample
for update;
if status_row.curr_offset is null then
status_row.curr_offset := 0;
status_row.last_entity_id := 0;
status_row.last_sequence_acc := '';
insert into camera.tmp_pointer_for_internal_sequence_entity_with_sample
values (
status_row.curr_offset,
status_row.last_entity_id,
status_row.last_sequence_acc
);
end if;
open secursor(status_row.last_entity_id);
<<l1>>
loop
fetch secursor into seq_ent;
exit l1 when not found;
begin
if nrows % pagesize = 0 then
raise notice '% EntityId, Acc: %,% ',
nrows,
seq_ent.entity_id,
seq_ent.camera_acc;
end if;
nrows := nrows + 1;
exit l1 when $1 > 0 and nrows > $1;
if not seq_ent.sample_acc is null then
select fullDefLine into seq_ent.defline
from fullDefLine(seq_ent.camera_acc);
insert into camera.tmp_internal_sequence_entity_with_sample (
entity_id,
camera_acc,
defline,
owner_id,
sequence_id,
sequence_length,
entity_type_code,
external_source,
external_acc,
ncbi_gi_number,
comment,
assembly_acc,
library_acc,
sample_acc,
organism,
taxon_id,
locus,
protein_acc,
orf_acc,
dna_acc,
dna_begin,
dna_end,
dna_orientation,
translation_table,
stop_5_prime,
stop_3_prime,
trace_acc,
template_acc,
clear_range_begin,
clear_range_end,
sequencing_direction,
type,
strain,
source_id,
obs_flag
) values (
seq_ent.entity_id,
seq_ent.camera_acc,
seq_ent.defline,
seq_ent.owner_id,
seq_ent.sequence_id,
seq_ent.sequence_length,
seq_ent.entity_type_code,
seq_ent.external_source,
seq_ent.external_acc,
seq_ent.ncbi_gi_number,
seq_ent.comment,
seq_ent.assembly_acc,
seq_ent.library_acc,
seq_ent.sample_acc,
seq_ent.organism,
seq_ent.taxon_id,
seq_ent.locus,
seq_ent.protein_acc,
seq_ent.orf_acc,
seq_ent.dna_acc,
seq_ent.dna_begin,
seq_ent.dna_end,
seq_ent.dna_orientation,
seq_ent.translation_table,
seq_ent.stop_5_prime,
seq_ent.stop_3_prime,
seq_ent.trace_acc,
seq_ent.template_acc,
seq_ent.clear_range_begin,
seq_ent.clear_range_end,
seq_ent.sequencing_direction,
seq_ent.type,
seq_ent.strain,
seq_ent.source_id,
seq_ent.obs_flag
);
end if;
status_row.last_entity_id := seq_ent.entity_id;
status_row.last_sequence_acc := seq_ent.camera_acc;
status_row.curr_offset := status_row.curr_offset + 1;
end;
end loop;
close secursor;
-- since postgres doesn't support nested transaction
-- is pointless to update the status table
-- after each insert
update camera.tmp_pointer_for_internal_sequence_entity_with_sample set
curr_offset = status_row.curr_offset,
last_entity_id = status_row.last_entity_id,
last_sequence_acc = status_row.last_sequence_acc;
return status_row;
END;
$$ LANGUAGE plpgsql;

create or replace function quote_defline_value(v varchar)
returns varchar as $$
declare
lv varchar;
begin
lv := trim(both ' ' from v);
if strpos(lv,' ') > 0 then
return quote_ident(lv);
else
return lv;
end if;
end;
$$ language plpgsql;

CREATE OR REPLACE FUNCTION observationDefline(material_id bigint,suffix varchar)
RETURNS varchar AS $$
DECLARE
defline varchar;
obsEntry record;
BEGIN
defline := NULL;
FOR obsEntry IN
select '/'||replace(obs.observation_type,' ','_')||suffix||
'='||
quote_defline_value(obs.value||coalesce(' '||obs.units,'')||'') as obs_defline
from collection_observation obs
where obs.material_id = $1
order by observation_type
LOOP
if defline IS NULL THEN
defline := obsEntry.obs_defline;
ELSE
if obsEntry.obs_defline is not null then
defline := defline || ' ' || obsEntry.obs_defline;
end if;
end if;
END LOOP;
return defline;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fullDefLine(entityAcc varchar)
RETURNS varchar AS $$
DECLARE
siteEntry record;
sampleEntry record;
originalDefline record;
fdefline varchar;
nSiteRecords integer;
siteRecordIndex integer;
siteSuffix varchar;
observation_values varchar;
BEGIN
fdefline := NULL;
FOR originalDefline IN
select
se.entity_type_code as entityType,
assembly_acc,
se.defline as defline
from sequence_entity se
where se.camera_acc = $1
LOOP
IF fdefline is NULL THEN
fdefline := originalDefline.defline;
ELSE
fdefline := fdefline || ' ' || originalDefline.defline;
END IF;
if originalDefline.assembly_acc is not null then
fdefline := fdefline || ' ' ||
'/assembly_id=' ||
quote_defline_value(originalDefline.assembly_acc);
fdefline :=  regexp_replace(fdefline,'/assembly=.* ',' ');
end if;
END LOOP;
-- remove the existing IP notice
fdefline := regexp_replace(fdefline,' /ip_notice=".*"','');
IF originalDefline.entityType != 3  THEN
-- if not a read remove the read defline
fdefline := regexp_replace(fdefline,' /read_defline=".*"','');
END IF;
fdefline :=  regexp_replace(fdefline,'/sample_id=.* ',' ');
select * into sampleEntry
from bio_sample sample
where sample.sample_acc = (select entity.sample_acc
from sequence_entity entity
where entity.camera_acc = $1);
if sampleEntry.sample_acc is not null then
fdefline := fdefline || ' ' ||
'/sample_id=' ||
quote_defline_value(sampleEntry.sample_acc) ||' '||
'/sample_name=' ||
quote_defline_value(sampleEntry.sample_name);
end if;
select count(1) into nSiteRecords
from bio_material_sample bms
inner join bio_material bm on bm.material_id = bms.material_id
inner join collection_site site on site.site_id = bm.collection_site_id
inner join geo_point gp on gp.location_id = site.site_id
where bms.sample_id = sampleEntry.sample_id;
siteRecordIndex := 1;
if nSiteRecords > 0 then
fdefline := fdefline || ' ' ||
'/number_of_sites=' || nSiteRecords;
end if;
FOR siteEntry IN
select bm.material_acc as site_id_value,
site.location as location_value,
site.region as region_value,
gp.country as country_value,
gp.altitude  as altitude_value,
gp.depth as depth_value,
bm.material_id as material_id
from bio_material_sample bms
inner join bio_material bm on bm.material_id = bms.material_id
inner join collection_site site on site.site_id = bm.collection_site_id
inner join geo_point gp on gp.location_id = site.site_id
where bms.sample_id = sampleEntry.sample_id
order by location
LOOP
siteSuffix := '_' || siteRecordIndex;
-- append the defline properties
if siteEntry.site_id_value is not null then
fdefline := fdefline || ' ' ||
'/site_id' || siteSuffix || '=' ||
quote_defline_value(siteEntry.site_id_value);
end if;
if siteEntry.location_value is not null then
fdefline := fdefline || ' ' ||
'/location' || siteSuffix || '=' ||
quote_defline_value(siteEntry.location_value);
end if;
if siteEntry.region_value is not null then
fdefline := fdefline || ' ' ||
'/region' || siteSuffix || '=' ||
quote_defline_value(siteEntry.region_value);
end if;
if siteEntry.country_value is not null then
fdefline := fdefline || ' ' ||
'/country' || siteSuffix || '=' ||
quote_defline_value(siteEntry.country_value);
end if;
if siteEntry.altitude_value is not null then
fdefline := fdefline || ' ' ||
'/site_altitude' || siteSuffix || '=' ||
quote_defline_value(siteEntry.altitude_value);
end if;
if siteEntry.depth_value is not null then
fdefline := fdefline || ' ' ||
'/site_depth' || siteSuffix || '=' ||
quote_defline_value(siteEntry.depth_value);
end if;
observation_values :=
observationDefline(siteEntry.material_id,siteSuffix);
if observation_values is not null then
fdefline := fdefline || ' ' || observation_values;
end if;
siteRecordIndex := siteRecordIndex + 1;
END LOOP;
IF sampleEntry.intellectual_property_notice IS NOT NULL THEN
-- if the sample has an ip notice append it at the end
fdefline := fdefline || ' ' ||
'/ip_notice=' ||
quote_defline_value(sampleEntry.intellectual_property_notice);
end if;
RETURN fdefline;
END;
$$ LANGUAGE plpgsql;
