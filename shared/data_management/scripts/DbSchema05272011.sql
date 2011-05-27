CREATE TABLE accession_ts_result ( node_id bigint NOT NULL, accession varchar(255) NOT NULL, docid bigint NOT NULL, doctype varchar(255), docname varchar(255), headline varchar(255), PRIMARY KEY (node_id, accession, docid), CONSTRAINT FK49A8F8259881D82 FOREIGN KEY (node_id) REFERENCES node (node_id), INDEX FK49A8F8259881D82 (node_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE accounting ( task_id bigint NOT NULL, job_id varchar(255) NOT NULL, submit_time datetime, start_time datetime, end_time datetime, wallclock int, user_time int, system_time int, cpu_time int, memory float, vmemory int, maxvmem int, exit_status smallint, status varchar(255), queue varchar(255), PRIMARY KEY (task_id, job_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE annotations ( id bigint NOT NULL, namespace varchar(255), owner varchar(255) NOT NULL, term varchar(255) NOT NULL, comment varchar(2000), conditional tinytext, value varchar(400), source varchar(255), createdDate date, parentIdentifier bigint, deprecated bit NOT NULL, PRIMARY KEY (id), CONSTRAINT ix1 UNIQUE (id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE assembly ( assembly_id bigint NOT NULL, assembly_acc varchar(255), description varchar(255), taxon_id int, sample_acc varchar(255), organism varchar(255), project varchar(255), PRIMARY KEY (assembly_id), CONSTRAINT FKE9BE3DE6F841CE3D FOREIGN KEY (sample_acc) REFERENCES bio_sample (sample_acc), CONSTRAINT assembly_acc UNIQUE (assembly_acc), INDEX FKE9BE3DE6F841CE3D (sample_acc) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE assembly_library ( assembly_id bigint NOT NULL, library_id bigint NOT NULL, PRIMARY KEY (assembly_id, library_id), CONSTRAINT FK6624C3627F6AD893 FOREIGN KEY (library_id) REFERENCES library (library_id) , CONSTRAINT FK6624C3627FAE6265 FOREIGN KEY (assembly_id) REFERENCES assembly (assembly_id), INDEX FK6624C3627F6AD893 (library_id), INDEX FK6624C3627FAE6265 (assembly_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE author ( name varchar(255) NOT NULL, PRIMARY KEY (name) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE bio_material ( material_id bigint NOT NULL, project_symbol varchar(255) NOT NULL, material_acc varchar(255) NOT NULL, collection_site_id bigint, collection_host_id bigint, collection_start_time datetime NOT NULL, collection_stop_time datetime NOT NULL, PRIMARY KEY (material_id), CONSTRAINT FK6DF427E86AE34FE FOREIGN KEY (collection_host_id) REFERENCES collection_host (host_id) , CONSTRAINT FK6DF427EC261DC9E FOREIGN KEY (collection_site_id) REFERENCES collection_site (site_id), CONSTRAINT material_acc UNIQUE (material_acc), INDEX FK6DF427EC261DC9E (collection_site_id), INDEX FK6DF427E86AE34FE (collection_host_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE bio_material_sample ( material_id bigint NOT NULL, sample_id bigint NOT NULL, PRIMARY KEY (material_id, sample_id), CONSTRAINT FKE9DE068B320BB9A1 FOREIGN KEY (sample_id) REFERENCES bio_sample (sample_id) , CONSTRAINT FKE9DE068B5C2345BB FOREIGN KEY (material_id) REFERENCES bio_material (material_id), INDEX FKE9DE068B320BB9A1 (sample_id), INDEX FKE9DE068B5C2345BB (material_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE bio_sample ( sample_id bigint NOT NULL, sample_acc varchar(255) NOT NULL, sample_name varchar(255) NOT NULL, filter_min double NOT NULL, filter_max double NOT NULL, intellectual_property_notice varchar(255), sample_title varchar(255), PRIMARY KEY (sample_id), CONSTRAINT sample_acc UNIQUE (sample_acc), CONSTRAINT sample_name UNIQUE (sample_name) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE bio_sample_comment ( sample_id bigint NOT NULL, comment_text varchar(255), comment_no int NOT NULL, PRIMARY KEY (sample_id, comment_no), CONSTRAINT bio_sample_comment_fk_sample FOREIGN KEY (sample_id) REFERENCES bio_sample (sample_id), INDEX bio_sample_comment_fk_sample (sample_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE bio_sequence ( sequence_id bigint NOT NULL, sequence_type_code int NOT NULL, sequence text NOT NULL, source_id int NOT NULL, PRIMARY KEY (sequence_id), CONSTRAINT FK457C55188FE1C85A FOREIGN KEY (sequence_type_code) REFERENCES sequence_type (code), INDEX FK457C55188FE1C85A (sequence_type_code) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE blast_hit ( blast_hit_id bigint NOT NULL, subject_acc varchar(255), subject_begin int NOT NULL, subject_end int NOT NULL, subject_orientation int NOT NULL, query_acc varchar(255), query_node_id bigint, query_begin int NOT NULL, query_end int NOT NULL, query_orientation int NOT NULL, result_node_id bigint, result_rank int, program_used varchar(255) NOT NULL, blast_version varchar(255), bit_score float, hsp_score float, expect_score double, comment varchar(255), length_alignment int, entropy float, number_identical int, number_similar int, subject_length int, subject_gaps int, subject_gap_runs int, subject_stops int, subject_number_unalignable int, subject_frame int, query_length int, query_gaps int, query_gap_runs int, query_stops int, query_number_unalignable int, query_frame int, subject_align_string varchar(255), midline_align_string varchar(255), query_align_string varchar(255), PRIMARY KEY (blast_hit_id), CONSTRAINT FK6B3A98CC53DE1F60 FOREIGN KEY (result_node_id) REFERENCES node (node_id) , CONSTRAINT FK6B3A98CC6CFFB02F FOREIGN KEY (result_node_id) REFERENCES node (node_id), INDEX FK6B3A98CC6CFFB02F (result_node_id), INDEX FK6B3A98CC53DE1F60 (result_node_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE blast_result_node_defline_map ( node_id bigint NOT NULL, defline varchar(255), camera_acc varchar(255) NOT NULL, PRIMARY KEY (node_id, camera_acc), CONSTRAINT FKBDA1997472A9AE51 FOREIGN KEY (node_id) REFERENCES node (node_id), INDEX FKBDA1997472A9AE51 (node_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE blastdataset_node_members ( dataset_node_id bigint NOT NULL, blastdb_filenode_id bigint NOT NULL, PRIMARY KEY (dataset_node_id, blastdb_filenode_id), CONSTRAINT FK68149A3BA514BF71 FOREIGN KEY (dataset_node_id) REFERENCES node (node_id) , CONSTRAINT FK68149A3BE7F3E806 FOREIGN KEY (blastdb_filenode_id) REFERENCES node (node_id), INDEX FK68149A3BA514BF71 (dataset_node_id), INDEX FK68149A3BE7F3E806 (blastdb_filenode_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE collection_host ( host_id bigint NOT NULL, organism varchar(255) NOT NULL, taxon_id int, host_details varchar(255), PRIMARY KEY (host_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE collection_observation ( material_id bigint NOT NULL, value varchar(255), units varchar(255), instrument varchar(255), comment varchar(255), observation_type varchar(255) NOT NULL, PRIMARY KEY (material_id, observation_type), CONSTRAINT FK1635E80B5C2345BB FOREIGN KEY (material_id) REFERENCES bio_material (material_id), INDEX FK1635E80B5C2345BB (material_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE collection_site ( site_id bigint NOT NULL, site_type_code int NOT NULL, region varchar(255) NOT NULL, location varchar(255), comment varchar(255), site_description varchar(255), PRIMARY KEY (site_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE data_file ( oid bigint NOT NULL, path varchar(255), info_location varchar(255), description text, size bigint, multifile_archive bit, PRIMARY KEY (oid) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE data_file_sample_link ( data_file_id bigint NOT NULL, sample_id bigint NOT NULL, PRIMARY KEY (data_file_id, sample_id), CONSTRAINT FK4E016321320BB9A1 FOREIGN KEY (sample_id) REFERENCES bio_sample (sample_id) , CONSTRAINT FK4E0163219DDED49D FOREIGN KEY (data_file_id) REFERENCES data_file (oid), INDEX FK4E016321320BB9A1 (sample_id), INDEX FK4E0163219DDED49D (data_file_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE data_source ( source_id bigint NOT NULL, source_name varchar(255) NOT NULL, data_version varchar(255), PRIMARY KEY (source_id), CONSTRAINT source_name UNIQUE (source_name), INDEX data_source_key_source_name (source_name) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE entity ( ID bigint NOT NULL, entity_type_id bigint NOT NULL, user_login varchar(255) NOT NULL, creation_date datetime, updated_date datetime, status varchar(60), PRIMARY KEY (ID), CONSTRAINT fk_entity_type_id2 FOREIGN KEY (entity_type_id) REFERENCES entityType (ID) , CONSTRAINT fk_user_login FOREIGN KEY (user_login) REFERENCES user_accounts (user_login), INDEX fk_user_login (user_login), INDEX fk_entity_type_id2 (entity_type_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE entityAttribute ( ID bigint NOT NULL, name varchar(60) NOT NULL, vals varchar(4000), style varchar(60), DESCRIPTION text, PRIMARY KEY (ID), CONSTRAINT name_constraint UNIQUE (name) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE entityData ( ID bigint NOT NULL, entity_id bigint NOT NULL, entity_att_id bigint NOT NULL, val longtext, user_login varchar(255) NOT NULL, creation_date datetime, updated_date datetime, PRIMARY KEY (ID), CONSTRAINT fk_att_id2 FOREIGN KEY (entity_att_id) REFERENCES entityAttribute (ID) , CONSTRAINT fk_entity_id FOREIGN KEY (entity_id) REFERENCES entity (ID) , CONSTRAINT fk_user_login2 FOREIGN KEY (user_login) REFERENCES user_accounts (user_login), INDEX fk_entity_id (entity_id), INDEX fk_user_login2 (user_login), INDEX fk_att_id2 (entity_att_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE entityStatus ( ID bigint NOT NULL, name varchar(60) NOT NULL, PRIMARY KEY (ID), CONSTRAINT name_constraint UNIQUE (name) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE entityType ( ID bigint NOT NULL, SEQUENCE decimal, name varchar(60) NOT NULL, style varchar(60), DESCRIPTION text, ICONURL varchar(255), PRIMARY KEY (ID), CONSTRAINT name_constraint UNIQUE (name) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE entityTypeAttribute ( ID bigint NOT NULL, entity_type_id bigint NOT NULL, entity_att_id bigint NOT NULL, PRIMARY KEY (ID), CONSTRAINT fk_entity_att_id FOREIGN KEY (entity_att_id) REFERENCES entityAttribute (ID) , CONSTRAINT fk_entity_type_id FOREIGN KEY (entity_type_id) REFERENCES entityType (ID), INDEX fk_entity_type_id (entity_type_id), INDEX fk_entity_att_id (entity_att_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE entity_type ( code int NOT NULL, name varchar(255) NOT NULL, abbrev varchar(255) NOT NULL, description varchar(255), sequence_type int NOT NULL, PRIMARY KEY (code), CONSTRAINT FK4C655A168ACB887E FOREIGN KEY (sequence_type) REFERENCES sequence_type (code), CONSTRAINT name UNIQUE (name), CONSTRAINT abbrev UNIQUE (abbrev), INDEX FK4C655A168ACB887E (sequence_type) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE geo_path_point ( path_id bigint NOT NULL, point_id bigint NOT NULL, path_order int NOT NULL, PRIMARY KEY (path_id, path_order), CONSTRAINT FKD6ED66A463C79B44 FOREIGN KEY (path_id) REFERENCES collection_site (site_id) , CONSTRAINT FKD6ED66A4E761E550 FOREIGN KEY (point_id) REFERENCES collection_site (site_id), INDEX FKD6ED66A463C79B44 (path_id), INDEX FKD6ED66A4E761E550 (point_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE geo_point ( location_id bigint NOT NULL, country varchar(255) NOT NULL, longitude varchar(255) NOT NULL, latitude varchar(255) NOT NULL, altitude varchar(255), depth varchar(255) NOT NULL, PRIMARY KEY (location_id), CONSTRAINT FK3BADC722EC852D0B FOREIGN KEY (location_id) REFERENCES collection_site (site_id), INDEX FK3BADC722EC852D0B (location_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE hierarchy_node ( oid bigint NOT NULL, name varchar(255), description varchar(255), PRIMARY KEY (oid) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE hierarchy_node_data_file_link ( hierarchy_node_id bigint NOT NULL, data_file_id bigint NOT NULL, position int NOT NULL, PRIMARY KEY (hierarchy_node_id, position), CONSTRAINT FKDBD10D5B9DDED49D FOREIGN KEY (data_file_id) REFERENCES data_file (oid) , CONSTRAINT FKDBD10D5BC973E5D7 FOREIGN KEY (hierarchy_node_id) REFERENCES hierarchy_node (oid), INDEX FKDBD10D5BC973E5D7 (hierarchy_node_id), INDEX FKDBD10D5B9DDED49D (data_file_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE hierarchy_node_to_children_link ( parent_id bigint NOT NULL, child_id bigint NOT NULL, position int NOT NULL, PRIMARY KEY (parent_id, position), CONSTRAINT FK4A0BA5891C3ADC87 FOREIGN KEY (child_id) REFERENCES hierarchy_node (oid) , CONSTRAINT FK4A0BA58934B77639 FOREIGN KEY (parent_id) REFERENCES hierarchy_node (oid), INDEX FK4A0BA5891C3ADC87 (child_id), INDEX FK4A0BA58934B77639 (parent_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE library ( library_id bigint NOT NULL, library_acc varchar(255), max_insert_size int, min_insert_size int, number_of_reads int NOT NULL, sequencing_technology varchar(255), sample_acc varchar(255), PRIMARY KEY (library_id), CONSTRAINT FK9E824BBF841CE3D FOREIGN KEY (sample_acc) REFERENCES bio_sample (sample_acc), CONSTRAINT library_acc UNIQUE (library_acc), INDEX FK9E824BBF841CE3D (sample_acc) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE multi_select_choices ( choice_id bigint NOT NULL, choice_element varchar(255) NOT NULL, choice_position int NOT NULL, PRIMARY KEY (choice_id, choice_position), CONSTRAINT FKE4D1D6759E11278A FOREIGN KEY (choice_id) REFERENCES parameter_vo (oid), INDEX FKE4D1D6759E11278A (choice_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE multi_select_values ( value_id bigint NOT NULL, value_element varchar(255) NOT NULL, value_position int NOT NULL, PRIMARY KEY (value_id, value_position), CONSTRAINT FK8A82A13F176FA1BA FOREIGN KEY (value_id) REFERENCES parameter_vo (oid), INDEX FK8A82A13F176FA1BA (value_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE node ( node_id bigint DEFAULT '0' NOT NULL, subclass varchar(255) DEFAULT 'FileNode' NOT NULL, name varchar(255), node_owner varchar(255), task_id bigint, description varchar(255), visibility varchar(255), data_type varchar(255), length bigint, ord int, relative_session_path varchar(255), is_replicated bit, path_override varchar(255), blast_hit_count bigint, sequence_type varchar(20), sequence_count int, data_source_id bigint, decypher_db_id varchar(255), partition_count int DEFAULT '0' NOT NULL, is_Assembled_Data bit, num_hmms int DEFAULT '0' NOT NULL, user_id bigint, PRIMARY KEY (node_id), CONSTRAINT decypher_db_id UNIQUE (decypher_db_id), INDEX node_fk_data_source (data_source_id), INDEX node_fk_user (user_id), INDEX node_fk_task (task_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE parameter_vo ( oid bigint NOT NULL, discriminator varchar(255) NOT NULL, boolean_value bit, double_min_value double, double_max_value double, double_value double, long_min_value bigint, long_max_value bigint, long_value bigint, description varchar(255), text_max_length int, text_value varchar(255), PRIMARY KEY (oid) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE project ( symbol varchar(255) NOT NULL, description text, principal_investigators varchar(255), email varchar(255), organization varchar(255), website_url varchar(255), funded_by varchar(255), institutional_affiliation varchar(255), name varchar(255), released bit, PRIMARY KEY (symbol) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE project_publication_link ( project_id varchar(255) NOT NULL, publication_id bigint NOT NULL, position int NOT NULL, PRIMARY KEY (project_id, position), CONSTRAINT FK61B3CE9363F6B82C FOREIGN KEY (project_id) REFERENCES project (symbol) , CONSTRAINT FK61B3CE93989D224C FOREIGN KEY (publication_id) REFERENCES publication (oid), INDEX FK61B3CE93989D224C (publication_id), INDEX FK61B3CE9363F6B82C (project_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE publication ( oid bigint NOT NULL, publication_acc varchar(50), abstractOfPublication text, summary text, title text, subjectDocument text, supplemental_text text, pub_date date, journal_entry varchar(255), description_html text, PRIMARY KEY (oid), CONSTRAINT publication_acc UNIQUE (publication_acc) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE publication_author_link ( publication_id bigint NOT NULL, author_id varchar(255) NOT NULL, position int NOT NULL, PRIMARY KEY (publication_id, position), CONSTRAINT FKED48A49B3D48CE88 FOREIGN KEY (author_id) REFERENCES author (name) , CONSTRAINT FKED48A49B989D224C FOREIGN KEY (publication_id) REFERENCES publication (oid), INDEX FKED48A49B989D224C (publication_id), INDEX FKED48A49B3D48CE88 (author_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE publication_combined_archives_link ( publication_id bigint NOT NULL, data_file_id bigint NOT NULL, position int NOT NULL, PRIMARY KEY (publication_id, position), CONSTRAINT FK95ED5B41989D224C FOREIGN KEY (publication_id) REFERENCES publication (oid) , CONSTRAINT FK95ED5B419DDED49D FOREIGN KEY (data_file_id) REFERENCES data_file (oid), INDEX FK95ED5B41989D224C (publication_id), INDEX FK95ED5B419DDED49D (data_file_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE publication_hierarchy_node_link ( publication_id bigint NOT NULL, hierarchy_node_id bigint NOT NULL, position int NOT NULL, PRIMARY KEY (publication_id, position), CONSTRAINT FK12CD9BFA989D224C FOREIGN KEY (publication_id) REFERENCES publication (oid) , CONSTRAINT FK12CD9BFAC973E5D7 FOREIGN KEY (hierarchy_node_id) REFERENCES hierarchy_node (oid), INDEX FK12CD9BFA989D224C (publication_id), INDEX FK12CD9BFAC973E5D7 (hierarchy_node_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE read_assembly ( scaffold_acc varchar(255) NOT NULL, read_acc varchar(255) NOT NULL, scaf_begin int, scaf_end int, scaf_orientation int, scaffold_length int, assembly_description varchar(255), PRIMARY KEY (scaffold_acc, read_acc) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE read_mate ( read_id bigint NOT NULL, mate_id bigint NOT NULL, PRIMARY KEY (read_id, mate_id), CONSTRAINT FKBD9EF40EA9FE1A16 FOREIGN KEY (mate_id) REFERENCES sequence_entity (entity_id) , CONSTRAINT FKBD9EF40EB84471E5 FOREIGN KEY (read_id) REFERENCES sequence_entity (entity_id), INDEX FKBD9EF40EB84471E5 (read_id), INDEX FKBD9EF40EA9FE1A16 (mate_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE sequence_entity ( entity_id bigint NOT NULL, entity_type_code int NOT NULL, owner_id bigint, source_id int NOT NULL, obs_flag bit NOT NULL, replaced_by varchar(255), camera_acc varchar(255) NOT NULL, defline varchar(255), sequence_id bigint, sequence_length int, external_source varchar(255), external_acc varchar(255), ncbi_gi_number int, organism varchar(255), taxon_id int, assembly_acc varchar(255), assembly_id bigint, sample_acc varchar(255), sample_id bigint, library_acc varchar(255), library_id bigint, type varchar(255), trace_acc varchar(255), template_acc varchar(255), sequencing_direction varchar(255), clear_range_begin int, clear_range_end int, protein_acc varchar(255), protein_id bigint, dna_acc varchar(255), dna_id bigint, dna_begin int, dna_end int, dna_orientation int, translation_table varchar(255), stop_5_prime varchar(255), stop_3_prime varchar(255), orf_acc varchar(255), orf_id bigint, PRIMARY KEY (entity_id), CONSTRAINT FKA23C5E21320BB9A1 FOREIGN KEY (sample_id) REFERENCES bio_sample (sample_id) , CONSTRAINT FKA23C5E213EA50CFA FOREIGN KEY (owner_id) REFERENCES user_accounts (user_id) , CONSTRAINT FKA23C5E2153897EBE FOREIGN KEY (dna_id) REFERENCES sequence_entity (entity_id) , CONSTRAINT FKA23C5E2164A5458F FOREIGN KEY (orf_id) REFERENCES sequence_entity (entity_id) , CONSTRAINT FKA23C5E217F6AD893 FOREIGN KEY (library_id) REFERENCES library (library_id) , CONSTRAINT FKA23C5E217FAE6265 FOREIGN KEY (assembly_id) REFERENCES assembly (assembly_id) , CONSTRAINT FKA23C5E21B810A30F FOREIGN KEY (protein_id) REFERENCES sequence_entity (entity_id) , CONSTRAINT FKA23C5E21C964BBDE FOREIGN KEY (entity_type_code) REFERENCES entity_type (code) , CONSTRAINT FKA23C5E21E60408F7 FOREIGN KEY (sequence_id) REFERENCES bio_sequence (sequence_id), CONSTRAINT camera_acc UNIQUE (camera_acc), INDEX FKA23C5E21C964BBDE (entity_type_code), INDEX FKA23C5E2164A5458F (orf_id), INDEX FKA23C5E217F6AD893 (library_id), INDEX FKA23C5E2153897EBE (dna_id), INDEX FKA23C5E21320BB9A1 (sample_id), INDEX FKA23C5E21B810A30F (protein_id), INDEX FKA23C5E21E60408F7 (sequence_id), INDEX FKA23C5E217FAE6265 (assembly_id), INDEX FKA23C5E213EA50CFA (owner_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE sequence_type ( code int NOT NULL, name varchar(255) NOT NULL, description varchar(255) NOT NULL, elements varchar(255) NOT NULL, complements varchar(255) NOT NULL, residue_type varchar(255) NOT NULL, PRIMARY KEY (code) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE single_select_values ( value_id bigint NOT NULL, value_element varchar(255) NOT NULL, position int NOT NULL, PRIMARY KEY (value_id, position), CONSTRAINT FKFA414A0EE27CCE23 FOREIGN KEY (value_id) REFERENCES parameter_vo (oid), INDEX FKFA414A0EE27CCE23 (value_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE task ( task_id bigint NOT NULL, subclass varchar(255) NOT NULL, parent_task_id bigint, task_name varchar(255) NOT NULL, task_owner varchar(255) NOT NULL, job_name varchar(255), task_deleted_flag bit, expiration_date date, task_note varchar(255), user_id bigint, PRIMARY KEY (task_id), INDEX task_fk_user (user_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE task_event ( task_id bigint NOT NULL, event_no int NOT NULL, description varchar(255), event_timestamp datetime NOT NULL, event_type varchar(255) NOT NULL, PRIMARY KEY (task_id, event_no), CONSTRAINT task_event_fk_task FOREIGN KEY (task_id) REFERENCES task (task_id), INDEX task_event_fk_task (task_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE task_input_node ( task_id bigint NOT NULL, node_id bigint NOT NULL, PRIMARY KEY (task_id, node_id), CONSTRAINT task_input_node_fk_task FOREIGN KEY (task_id) REFERENCES task (task_id), INDEX task_input_node_fk_node (node_id), INDEX task_input_node_fk_task (task_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE task_message ( message_id bigint NOT NULL, message text NOT NULL, task_id bigint, PRIMARY KEY (message_id), CONSTRAINT FK6239874D1233B4B2 FOREIGN KEY (task_id) REFERENCES task (task_id), INDEX FK6239874D1233B4B2 (task_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE task_parameter ( task_id bigint NOT NULL, parameter_name varchar(255) NOT NULL, parameter_value text, PRIMARY KEY (task_id, parameter_name), CONSTRAINT task_parameters_fk_task FOREIGN KEY (task_id) REFERENCES task (task_id), INDEX task_parameters_fk_task (task_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE user_accounts ( user_id bigint NOT NULL, user_login varchar(255) NOT NULL, fullName varchar(255), email varchar(255), PRIMARY KEY (user_id), CONSTRAINT user_login UNIQUE (user_login) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE TABLE user_preference_map ( user_id bigint NOT NULL, name varchar(255), category varchar(255), value varchar(255), category_and_name varchar(255) NOT NULL, PRIMARY KEY (user_id, category_and_name), CONSTRAINT FK1B46BEECD2BE5CE2 FOREIGN KEY (user_id) REFERENCES user_accounts (user_id), INDEX FK1B46BEECD2BE5CE2 (user_id) ) ENGINE=InnoDB DEFAULT CHARSET=latin1;
