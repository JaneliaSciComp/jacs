function handles = FISH_QUANT_init_v3(handles)
%
% Initiate the GUI with all relevant parameter. Call populate function
% afterwards to use these values to prepare GUI. Split in two function
% occurs to allow loading of (partial settings).
%


%== Columns where parameters are stored
handles.col_par.amp = 4;
handles.col_par.bgd = 5;
handles.col_par.sigmax = 7;
handles.col_par.sigmay = 8;
handles.col_par.sigmaz = 9;

handles.col_par.pos_x = 2;
handles.col_par.pos_y = 1;
handles.col_par.pos_z = 3;

handles.col_par.pos_x_sub = 14;
handles.col_par.pos_y_sub = 13;
handles.col_par.pos_z_sub = 15;


%== Flags and sliders
set(handles.checkbox_th_lock, 'Value',0);
set(handles.checkbox_parallel_computing, 'Value',0);

set(handles.slider_th_min,'Value', 0)
set(handles.text_th_min,'String', '');     
     
set(handles.slider_th_max,'Value',1)
set(handles.text_th_max,'String', '');

%== Pop-up menus
set(handles.pop_up_outline_sel_cell,'Value',1)
set(handles.pop_up_outline_sel_cell,'String',{''})


%- MIJ and Bio-Formats to interact with ImageJ and read-in files already started
if not(isfield(handles,'flag_JAVA_init'))
    handles.flag_JAVA_init = 0; 
end

%- MIJ started
if not(isfield(handles,'flag_MIJ'))
    handles.flag_MIJ = 0; 
end


%- Status of current processing steps
handles.status_filtered = 0;    % Image filterd
handles.status_image    = 0;    % Image loaded
handles.flag_fit        = 0;    % Fit mode (0 for free parameters, 1 for fixed size parameters)

%- File-name and path-name
handles.path_name_image          = [];
handles.file_name_image          = [];
handles.file_name_image_filtered = [];
handles.file_name_settings = [];
handles.file_name_results  = [];
handles.path_name_settings = [];
handles.cell_prop          = struct('label', {}, 'x', {}, 'y', {}, 'pos_TS', {}, 'spots_fit', {},'thresh',{},'spots_proj',{});
handles.PSF_exp            = [];
           
handles.image_struct.data_filtered = [];
handles.image_struct.data = [];

%- Default parameters for the experiment --> only if not already defined
%  Useful when processing multiple images and the same settings should be
%  used for all of them.
if not(isfield(handles,'par_microscope'))
    handles.par_microscope.pixel_size.xy = 160;
    handles.par_microscope.pixel_size.z  = 300;   
    handles.par_microscope.RI            = 1.458;   
    handles.par_microscope.NA            = 1.25;
    handles.par_microscope.Em            = 568;   
    handles.par_microscope.Ex            = 568;
    handles.par_microscope.type          = 'widefield';  
end

%- Filtering
handles.filter.factor_bgd = 5; 
handles.filter.factor_psf = 0.75;

%- Theoretical PSF - will be used to calculated the size of the detection region
[PSF_theo.xy_nm,PSF_theo.z_nm] = sigma_PSF_BoZhang_v1(handles.par_microscope);
PSF_theo.xy_pix                = PSF_theo.xy_nm / handles.par_microscope.pixel_size.xy ;
PSF_theo.z_pix                 = PSF_theo.z_nm  / handles.par_microscope.pixel_size.z ;
handles.PSF_theo               = PSF_theo;

%- Detection 
handles.detect.region.xy = round(2*PSF_theo.xy_pix)+1;       % Size of detection zone in xy 
handles.detect.region.z  = round(2*PSF_theo.z_pix)+1;        % Size of detection zone in z 

handles.detect.thresh_int   = 0;        % Minimum score of quality parameter for predetection
handles.detect.thresh_score = 0;        % Minimum score of quality parameter for predetection
handles.detect.score = 'Standard deviation';
handles.detect.flag_region_smaller = 0;  % Flag to indicate if smaller region in Z can be detected

%- Fitting
handles.par_fit.sigma_XY_fixed = [];
handles.par_fit.sigma_Z_fixed = [];

%=== Averaging

%- Area to consider around the spots +/- in xy and z
handles.average.crop.xy = round(5*PSF_theo.xy_pix)+1;
handles.average.crop.z  = round(5*PSF_theo.xy_pix)+1;

%- Factor for oversampling
handles.average.fact_os.xy = 3;
handles.average.fact_os.z  = 3;

%== For thresholding
handles.thresh_all.sigmaxy.lock  = 0; 
handles.thresh_all.sigmaz.lock   = 0;  
handles.thresh_all.amp.lock      = 0;  
handles.thresh_all.bgd.lock      = 0; 
handles.thresh_all.score.lock    = 0; 
handles.thresh_all.iter.lock     = 0;  
handles.thresh_all.resNorm.lock  = 0; 


%=== Options for TxSite quantification
handles.TS_quant.options.flag_struct.placement = 2;
handles.TS_quant.options.flag_struct.quality   = 2;
handles.TS_quant.options.N_Run_analysis = 500;
handles.TS_quant.options.N_run_prelim   = 5;
handles.TS_quant.options.nBins          = 50;
handles.TS_quant.options.per_avg_bgd    = 0.95;
handles.TS_quant.options.crop_image.xy_nm = 1000;
handles.TS_quant.options.crop_image.z_nm  = 3000;