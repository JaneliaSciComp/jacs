function [file_save path_save] = FISH_QUANT_save_results_v7(file_name_full,parameters)

%== Extract parameters
cell_prop                = parameters.cell_prop;
par_microscope           = parameters.par_microscope;
path_name_image          = parameters.path_name_image;
file_name_image          = parameters.file_name_image;
file_name_image_filtered = parameters.file_name_image_filtered;
file_name_settings       = parameters.file_name_settings;
version                  = parameters.version;


current_dir = pwd;

%- Ask for file-name if it's not specified
if isempty(file_name_full)
    cd(path_name_image);

    %- Ask user for file-name for spot results
    [dum, name_file] = fileparts(file_name_image); 
    file_name_default_spot = [name_file,'__spots.txt'];

    [file_save,path_save] = uiputfile(file_name_default_spot,'Save results of spot detection');
    file_name_full = fullfile(path_save,file_save);
    
    %- Ask user to specify comment
    prompt = {'Comment (cancel for no comment):'};
    dlg_title = 'User comment for file';
    num_lines = 1;
    def = {''};
    answer = inputdlg(prompt,dlg_title,num_lines,def);
else   
    file_save = 1;
    path_save = fileparts(file_name_full); 
    answer = 'Batch detection';
end


% Only write if FileName specified
if file_save ~= 0
    
    fid = fopen(file_name_full,'w');
    
    %- Header    
    fprintf(fid,'FISH-QUANT\t%s\n', version );
    fprintf(fid,'RESULTS OF SPOT DETECTION PERFORMED ON %s \n', date);
    fprintf(fid,'%s\t%s\n','COMMENT',char(answer));     
    
    %- File Name
    fprintf(fid,'%s\t%s\n','FILE',file_name_image);    
    fprintf(fid,'%s\t%s\n','FILTERED',file_name_image_filtered); 
    
    %- Experimental parameters and analysis settings
    fprintf(fid,'PARAMETERS\n');
    fprintf(fid,'Pix-XY\tPix-Z\tRI\tEx\tEm\tNA\tType\n');
    fprintf(fid,'%g\t%g\t%g\t%g\t%g\t%g\t%s\n', par_microscope.pixel_size.xy, par_microscope.pixel_size.z, par_microscope.RI, par_microscope.Ex, par_microscope.Em,par_microscope.NA, par_microscope.type );
    
    fprintf(fid,'ANALYSIS-SETTINGS \t%s\n', file_name_settings);   
        
    %- Outline of cell and detected spots
    for i_cell = 1:size(cell_prop,2)
                
        %- Outline of cell
        fprintf(fid,'%s\t%s\n', 'CELL', cell_prop(i_cell).label);
        fprintf(fid,'X_POS\t');
        fprintf(fid,'%g\t',cell_prop(i_cell).x);
        fprintf(fid,'END\n');
        fprintf(fid,'Y_POS\t');
        fprintf(fid,'%g\t',cell_prop(i_cell).y);
        fprintf(fid,'END\n');
    
        %- TS
        for i_TS = 1:size(cell_prop(i_cell).pos_TS,2)
            fprintf(fid,'%s\t%s\n', 'TxSite', cell_prop(i_cell).pos_TS(i_TS).label);
            fprintf(fid,'X_POS\t');
            fprintf(fid,'%g\t',cell_prop(i_cell).pos_TS(i_TS).x);
            fprintf(fid,'END\n');
            fprintf(fid,'Y_POS\t');
            fprintf(fid,'%g\t',cell_prop(i_cell).pos_TS(i_TS).y);
            fprintf(fid,'END\n');        
        end
    
        %- Spots
        % NOTE: fprintf works on columns - transformation is therefore needed
        if not(isempty(cell_prop(i_cell).spots_fit));
            spots_output   = [cell_prop(i_cell).spots_fit,cell_prop(i_cell).spots_detected,cell_prop(i_cell).thresh.in];
            
            N_par = size(spots_output,2);            
            string_output = [repmat('%g\t',1,N_par-1),'%g\n'];
            
            fprintf(fid,'%s\n', 'SPOTS');          
            fprintf(fid,'Pos_Y\tPos_X\tPos_Z\tAMP\tBGD\tRES\tSigmaX\tSigmaY\tSigmaZ\tCent_Y\tCent_X\tCent_Z\tMuY\tMuX\tMuZ\tITERY_det\tY_det\tX_det\tZ_det\tY_min\tY_max\tX_min\tX_max\tZ_min\tZ_max\tSC_det\tSC_det_norm\tTH_det\tTH_fit\n');
            fprintf(fid, string_output,spots_output');       
        end
    end
end
fclose(fid);
cd(current_dir)
