#!/bin/bash
HOST=prd-db
DB=flyportal
USER=flyportalAdmin
PASS=flyp0rt@lAdm1n

mysql -h ${HOST} -u ${USER} -p${PASS} ${DB}<<EOFMYSQL
analyze table accession_ts_result;                
analyze table accounting;                         
analyze table annotations;                        
analyze table assembly;                           
analyze table assembly_library;                   
analyze table author;                             
analyze table bio_material;                       
analyze table bio_material_sample;                
analyze table bio_sample;                         
analyze table bio_sample_comment;                 
analyze table bio_sequence;                       
analyze table blast_hit;                          
analyze table blast_result_node_defline_map;      
analyze table blastdataset_node_members;          
analyze table collection_host;                    
analyze table collection_observation;             
analyze table collection_site;                    
analyze table data_file;                          
analyze table data_file_sample_link;              
analyze table data_source;                        
analyze table entity;                             
analyze table entityAttribute;                    
analyze table entityData;                         
analyze table entityStatus;                       
analyze table entityType;                         
analyze table entityTypeAttribute;                
analyze table entity_type;                        
analyze table geo_path_point;                     
analyze table geo_point;                          
analyze table hierarchy_node;                     
analyze table hierarchy_node_data_file_link;      
analyze table hierarchy_node_to_children_link;    
analyze table library;                            
analyze table multi_select_choices;               
analyze table multi_select_values;                
analyze table node;                               
analyze table parameter_vo;                       
analyze table project;                            
analyze table project_publication_link;           
analyze table publication;                        
analyze table publication_author_link;            
analyze table publication_combined_archives_link; 
analyze table publication_hierarchy_node_link;    
analyze table read_assembly;                      
analyze table read_mate;                          
analyze table search_results;                     
analyze table sequence_entity;                    
analyze table sequence_type;                      
analyze table single_select_values;               
analyze table task;                               
analyze table task_event;                         
analyze table task_input_node;                    
analyze table task_message;                       
analyze table task_parameter;                     
analyze table user_accounts;                      
analyze table user_preference_map;                
EOFMYSQL
