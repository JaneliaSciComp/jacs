function [file_save path_save] = FQ_TS_settings_save_v1(file_name_full,handles)

current_dir = pwd;


%% Ask for file-name if it's not specified
if isempty(file_name_full)    
    
    if not(isempty(handles.path_name_image))
        cd(handles.path_name_image);    
        [dum, name_file] = fileparts(handles.file_name_image); 
        file_name_default_spot = [name_file,'__settings_TxSite.txt'];
    else
        file_name_default_spot = 'FISH_QUANT_detection_settings_TxSite.txt';
    end

    [file_save,path_save] = uiputfile(file_name_default_spot,'File-name for detection settings');
    file_name_full = fullfile(path_save,file_save);
    
else   
    [dum, file_save,ext] = fileparts(file_name_full); 
    file_save = [file_save,ext];
end


%% Only write if FileName specified
if file_save ~= 0
    
           
    fid = fopen(fullfile(file_name_full),'w');
    %- Header 
    fprintf(fid,'FISH-QUANT\t%s\n', handles.version);
    fprintf(fid,'SETTINGS FOR TRANSCRIPTION SITE QUANTIFICATION %s \n', date);       
    
    %- Experimental parameters    
    fprintf(fid,'\n# EXPERIMENTAL PARAMETERS\n');
    fprintf(fid,'lambda_EM=%g\n',  handles.par_microscope.Em);
    fprintf(fid,'lambda_Ex=%g\n',  handles.par_microscope.Ex);
    fprintf(fid,'NA=%g\n',         handles.par_microscope.NA);
    fprintf(fid,'RI=%g\n',         handles.par_microscope.RI);
    fprintf(fid,'Microscope=%s\n', handles.par_microscope.type);
    fprintf(fid,'Pixel_XY=%g\n',   handles.par_microscope.pixel_size.xy);
    fprintf(fid,'Pixel_Z=%g\n',    handles.par_microscope.pixel_size.z);    
 
             
    %- Name of PSF and BGD
    fprintf(fid,'\n# DESCRIPTION OF PSF AND BGD \n');
    
    fprintf(fid,'PSF_path_name=%s\n', handles.PSF_path_name);
    fprintf(fid,'PSF_file_name=%s\n', handles.PSF_file_name);
    
    fprintf(fid,'BGD_path_name=%s\n', handles.BGD_path_name);
    fprintf(fid,'BGD_file_name=%s\n', handles.BGD_file_name);  
    
    if isempty(handles.BGD_file_name) 
        fprintf(fid,'PSF_BGD_value=%g\n', handles.bgd_value); 
    end 
    
    fprintf(fid,'AMP_path_name=%s\n', handles.AMP_path_name);
    fprintf(fid,'AMP_file_name=%s\n', handles.AMP_file_name);
    
    fprintf(fid,'fact_os_xy=%g\n', handles.fact_os.xy);
    fprintf(fid,'fact_os_z=%g\n',  handles.fact_os.z);
      

    %- Settings for detection: FLAGS
    fprintf(fid,'\n# SETTINGS FOR QUANTIFICATION \n');   
    
    fprintf(fid,'FLAG_placement=%g\n', handles.parameters_quant.flags.placement);
    fprintf(fid,'FLAG_quality=%g\n', handles.parameters_quant.flags.quality);
    fprintf(fid,'FLAG_posWeight=%g\n', handles.parameters_quant.flags.posWeight);
    fprintf(fid,'FLAG_bgd_local=%g\n', handles.parameters_quant.flags.bgd_local);    
    fprintf(fid,'FLAG_crop=%g\n', handles.parameters_quant.flags.crop);
    fprintf(fid,'FLAG_psf=%g\n', handles.parameters_quant.flags.psf);    
    fprintf(fid,'FLAG_shift=%g\n', handles.parameters_quant.flags.shift);      
    fprintf(fid,'FLAG_parallel=%g\n',get(handles.checkbox_parallel_computing,'Value'));
    
    %- Settings for detection: PARAMETERs
    
    fprintf(fid,'N_reconstruct=%g\n', handles.parameters_quant.N_reconstruct);
    fprintf(fid,'N_run_prelim=%g\n', handles.parameters_quant.N_run_prelim);
    fprintf(fid,'nBins=%g\n', handles.parameters_quant.nBins);
    fprintf(fid,'per_avg_bgd=%g\n', handles.parameters_quant.per_avg_bgd);
    fprintf(fid,'crop_image_xy_nm=%g\n', handles.parameters_quant.crop_image.xy_nm);
    fprintf(fid,'crop_image_z_nm=%g\n', handles.parameters_quant.crop_image.z_nm);
    fprintf(fid,'factor_Q_ok=%g\n', handles.parameters_quant.factor_Q_ok);
    
    if handles.parameters_quant.flags.bgd_local == 0 
        fprintf(fid,'TS_BGD_value=%g\n', handles.parameters_quant.BGD.amp); 
    end
        
    %- Autodetection of TxSite
    fprintf(fid,'\n# SETTINGS FOR AUTO-Detection \n');     
    if handles.status_auto_detect == 1         
        fprintf(fid,'FLAG_auto_detect=%g\n', 1);
        fprintf(fid,'auto_detect_th_int=%g\n', handles.parameters_auto_detect.int_th);
        fprintf(fid,'auto_detect_conn=%g\n', handles.parameters_auto_detect.conn);
    else
        fprintf(fid,'FLAG_auto_detect=%g\n', 0);
    end 
    
    
end
