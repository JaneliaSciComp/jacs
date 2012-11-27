function [file_save path_save] = FISH_QUANT_save_settings_batch(file_name_full,handles)

% v3
%- Supports default file name
%- Can be called from within the main interface or the batch processing
%  interface

current_dir = pwd;


%- Ask for file-name if it's not specified
if isempty(file_name_full)    
    
    if not(isempty(handles.path_name_image))
        cd(handles.path_name_image);    
        [dum, name_file] = fileparts(handles.file_name_image); 
        file_name_default_spot = [name_file,'__settings.txt'];
    else
        file_name_default_spot = 'FISH_QUANT_detection_settings.txt';
    end

    [file_save,path_save] = uiputfile(file_name_default_spot,'File-name for detection settings');
    file_name_full = fullfile(path_save,file_save);
    
else   
    [dum, file_save,ext] = fileparts(file_name_full); 
    file_save = [file_save,ext];
end


% Only write if FileName specified
if file_save ~= 0
    
           
    fid = fopen(fullfile(file_name_full),'w');
    %- Header 
    fprintf(fid,'FISH-QUANT\t%s\n', handles.version);
    fprintf(fid,'ANALYSIS SETTINGS %s \n', date);       
    
    %- Experimental parameters    
    fprintf(fid,'# EXPERIMENTAL PARAMETERS\n');
    fprintf(fid,'lambda_EM=%g\n',  handles.par_microscope.Em);
    fprintf(fid,'lambda_Ex=%g\n',  handles.par_microscope.Ex);
    fprintf(fid,'NA=%g\n',         handles.par_microscope.NA);
    fprintf(fid,'RI=%g\n',         handles.par_microscope.RI);
    fprintf(fid,'Microscope=%s\n', handles.par_microscope.type);
    fprintf(fid,'Pixel_XY=%g\n',   handles.par_microscope.pixel_size.xy);
    fprintf(fid,'Pixel_Z=%g\n',    handles.par_microscope.pixel_size.z);
    
    if isfield(handles,'PSF_theo')
        fprintf(fid,'PSF_THEO_XY=%g\n', handles.PSF_theo.xy_nm);
        fprintf(fid,'PSF_THEO_Z=%g\n',  handles.PSF_theo.z_nm);
    end
    
    %- Some flags
    fprintf(fid,'# GENERAL PROPERTIES\n');
    fprintf(fid,'flag_parallel=%g\n',0.0);   
    
    %- Settings for filtering
    fprintf(fid,'# FILTERING\n');
    fprintf(fid,'Kernel_bgd=%g\n', handles.filter.factor_bgd);
    fprintf(fid,'Kernel_psf=%g\n', handles.filter.factor_psf);
    
    %- Settings for pre-detection
    fprintf(fid,'# PRE-DETECTION\n');
    fprintf(fid,'Detect_Region_XY=%g\n',    handles.detect.region.xy);
    fprintf(fid,'Detect_Region_Z=%g\n',     handles.detect.region.z);
    fprintf(fid,'Detect_Thresh_int=%g\n',   handles.detect.thresh_int);
    fprintf(fid,'Detect_Thresh_score=%g\n', handles.detect.thresh_score);
    
    if isfield(handles.detect,'flag_region_smaller')
        fprintf(fid,'Detect_FLAG_reg_smaller=%g\n', handles.detect.flag_region_smaller);
    else
        fprintf(fid,'Detect_FLAG_reg_smaller=%g\n', 0);
    end    
    
    if isfield(handles,'pop_up_detect_quality')
        str = get(handles.pop_up_detect_quality, 'String');
        val = get(handles.pop_up_detect_quality, 'Value');   
        fprintf(fid,'Detect_Score=%s\n',str{val}); 
    else
        fprintf(fid,'Detect_Score=%s\n',handles.detect.score); 
    end
    
    %- Settings for Averaging
    fprintf(fid,'# AVERAGING\n');
    fprintf(fid,'AVG_Region_XY=%g\n',   handles.average.crop.xy);
    fprintf(fid,'AVG_Region_Z=%g\n',    handles.average.crop.z);
    fprintf(fid,'AVG_OS_XY=%g\n',       handles.average.fact_os.xy );
    fprintf(fid,'AVG_OS_Z=%g\n',        handles.average.fact_os.z);    
        
   
    %==== Settings for thresholding
    fprintf(fid,'# FITTING OF CURVES\n');
    flag_fit  = handles.flag_fit;
    
    if flag_fit == 0
        mode_fit       = 'sigma_free_xz';
        fprintf(fid,'Fit_mode=%s\n',mode_fit); 
 
    else
        mode_fit  = 'sigma_fixed'; 
        fprintf(fid,'Fit_mode=%s\n',mode_fit); 
        fprintf(fid,'Sigma_XY_fixed=%g\n',  handles.par_fit.sigma_XY_fixed);
        fprintf(fid,'Sigma_Z_fixed=%g\n',   handles.par_fit.sigma_Z_fixed);
    end    
        
    
    %==== Settings for thresholding
    fprintf(fid,'# THRESHOLDING OF DETECTED SPOTS\n');
    
    if isfield(handles,'thresh_all')
        if not(isempty(handles.thresh_all))
            thresh   = handles.thresh_all;
        else
            thresh = [];
        end
     else
        thresh   = handles.thresh;
    end
    
    
    if not(isempty(thresh))
    
        if thresh.sigmaxy.lock 
            fprintf(fid,'SPOTS_TH_sigmaXY_min=%g\n',   thresh.sigmaxy.min_hist);
            fprintf(fid,'SPOTS_TH_sigmaXY_max=%g\n',   thresh.sigmaxy.max_hist); 
        end

        if thresh.sigmaz.lock    
            fprintf(fid,'SPOTS_TH_sigmaZ_min=%g\n',   thresh.sigmaz.min_hist);
            fprintf(fid,'SPOTS_TH_sigmaZ_max=%g\n',   thresh.sigmaz.max_hist); 
        end

        if thresh.amp.lock       
            fprintf(fid,'SPOTS_TH_amp_min=%g\n',   thresh.amp.min_hist);
            fprintf(fid,'SPOTS_TH_amp_max=%g\n',   thresh.amp.max_hist); 
        end

        if thresh.bgd.lock       
            fprintf(fid,'SPOTS_TH_bgd_min=%g\n',   thresh.bgd.min_hist);
            fprintf(fid,'SPOTS_TH_bgd_max=%g\n',   thresh.bgd.max_hist); 
        end
        
        if isfield(thresh,'pos_z')
            if thresh.pos_z.lock       
                fprintf(fid,'SPOTS_TH_pos_z_min=%g\n',   thresh.pos_z.min_hist);
                fprintf(fid,'SPOTS_TH_pos_z_max=%g\n',   thresh.pos_z.max_hist); 
            end
        end       
        

    end
    
    fclose(fid);
end

cd(current_dir)


