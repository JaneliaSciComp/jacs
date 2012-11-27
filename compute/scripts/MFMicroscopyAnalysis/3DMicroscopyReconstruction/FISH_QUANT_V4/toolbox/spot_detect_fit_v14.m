function [cell_prop par_microscope file_name_image file_name_image_filtered] = spot_detect_fit_v14(handles,parameters)

path_name            = parameters.path_name;
file_name_load       = parameters.file_name_load;
mode_fit             = parameters.mode_fit;
par_start            = parameters.par_start;
par_microscope       = parameters.par_microscope;
file_name_sett_batch = parameters.file_name_settings;
flag_struct          = parameters.flag_struct;             
detect               = handles.detect;

flag_struct.output   = 0;


%========================================================================== 
% General parameters
%==========================================================================

%=== Theoretical PSF
pixel_size = handles.par_microscope.pixel_size;

[PSF_theo.xy_nm, PSF_theo.z_nm] = sigma_PSF_BoZhang_v1(par_microscope);
PSF_theo.xy_pix = PSF_theo.xy_nm / par_microscope.pixel_size.xy ;
PSF_theo.z_pix  = PSF_theo.z_nm  / par_microscope.pixel_size.z ;


%========================================================================== 
% Load data
%==========================================================================

%== Determine what type of file we have
[pathstr, name, ext] = fileparts(file_name_load);


%-- Load data from outline definition file
if strcmpi(ext,'.txt')
    [cell_prop file_name_image dum file_name_settings file_name_image_filtered] = FISH_QUANT_load_results_v7(fullfile(path_name,file_name_load));    
    image_struct = load_stack_data_v4(fullfile(path_name,file_name_image));

%- Load image files 
elseif strcmpi(ext,'.tif') || strcmpi(ext,'.stk')
    image_struct = load_stack_data_v4(fullfile(path_name,file_name_load));
    file_name_image_filtered = [];
    file_name_image          = file_name_load;    
       
    %- Dimension of entire image
    w = image_struct.w;
    h = image_struct.h;
    cell_prop(1).x      = [1 1 w w];
    cell_prop(1).y      = [1 h h 1];
    
    %- Other parameters
    cell_prop(1).pos_TS = [];
    cell_prop(1).label = 'EntireImage';
    cell_prop(1).pos_TS          = [];

end

%- Check if same outline should be used for all images
if handles.status_outline_unique_enable
     cell_prop = deal(handles.cell_prop_loaded );
end
    
    

%========================================================================== 
% Filter image
%==========================================================================

%- Check if filtered images should be used and are available
use_filtered = get(handles.checkbox_use_filtered,'Value');

if use_filtered && (not(isempty(file_name_image_filtered)))
    image_filt_struct = load_stack_data_v4(fullfile(path_name,file_name_image_filtered));
    image_struct.data_filtered = image_filt_struct.data;

else
    filter.factor_bgd = handles.filter.factor_bgd;
    filter.factor_psf = handles.filter.factor_psf;
    filter.pad        = 2*filter.factor_bgd;

    %- 1. Pad array with Matlab function padarray
    img_pad = double(padarray(image_struct.data,[filter.pad filter.pad filter.pad],'symmetric','both'));

    %- 2. Background: apply Gaussian smoothing to images.
    img_bgd  = gaussSmooth(img_pad, filter.factor_bgd*[PSF_theo.xy_pix PSF_theo.xy_pix PSF_theo.z_pix], 'same');    
    img_diff = img_pad-img_bgd;    
    img_diff = img_diff.*(img_diff>0);      % Set negative values to zero

    %- 3. Convolution with the Theoretical gaussian Kernel
    img_filt = gaussSmooth( img_diff, filter.factor_psf *[PSF_theo.xy_pix PSF_theo.xy_pix PSF_theo.z_pix], 'same');    
    img_filt = img_filt.*(img_filt>0);
    img_filt = img_filt(filter.pad+1:end-filter.pad,filter.pad+1:end-filter.pad,filter.pad+1:end-filter.pad);
    handles.img_plot = max(img_filt,[],3);

    image_struct.data_filtered = img_filt;
end


%- Check if filtered images should be saved
save_filtered = get(handles.checkbox_save_filtered,'Value');

if save_filtered && isempty(file_name_image_filtered)
    
    current_dir = pwd;
    cd(handles.path_name_image)
    
    %- Save filtered image
    [dum, name_file]    = fileparts(file_name_image); 
    file_name_FILT      = [name_file,'_filtered_batch.tif'];
    file_name_FILT_full = fullfile(path_name,file_name_FILT);
    
    %- Make sure file doesn't exit - otherwise planes will be simply added
    if not(exist(file_name_FILT_full,'file'))
        image_save_v1(image_struct.data_filtered,file_name_FILT);
    end
    
    %- Save new outline definition
    file_name_OUTLINE   = [name_file,'_outline_batch.txt'];
    file_name_OUTLINE_full = fullfile(handles.path_name_image,file_name_OUTLINE);
    
    %- Assign parameters which should be saved
    struct_save.par_microscope           = par_microscope;
    struct_save.cell_prop                = cell_prop;
    struct_save.version                  = handles.version;
    struct_save.file_name_image          = file_name_image;
    struct_save.file_name_image_filtered = file_name_FILT;
    struct_save.file_name_settings       = file_name_sett_batch;
    struct_save.path_name_image          = path_name;
    
    %- Save settings    
    FISH_QUANT_save_outline_v5(struct_save,file_name_OUTLINE_full);
    
    file_name_image_filtered = file_name_FILT;
    
    cd(current_dir)
end
    


%========================================================================== 
% Process each cell in the image
%==========================================================================

%- Number of cells per image
N_cell = size(cell_prop,2);


for i_cell =1:N_cell

    %- Set-up options for pre-detection
    flag_struct.score   = handles.detect.score;
        
    options_detect.size_detect = detect.region;
    options_detect.detect_th   = detect.thresh_int;
    options_detect.cell_prop   = cell_prop(i_cell);
    options_detect.pixel_size  = pixel_size;
    options_detect.detect_th_score = detect.thresh_score;
    options_detect.PSF             = PSF_theo;            
    
    if isfield(handles.detect,'flag_region_smaller')
        flag_struct.region_smaller = detect.flag_region_smaller;
    else
        flag_struct.region_smaller = 0;
    end    
    
    %- Pre-detection of spots
    [spots_detected_pos]                               = spots_predetect_v9(image_struct,options_detect,flag_struct);
    [spots_detected dum detect.thresh_score sub_spots] = spots_predetect_analysis_v7(image_struct,[],spots_detected_pos,options_detect,flag_struct);
    
    %- Fitting of all pre-detected spots
    parameters.pixel_size  = pixel_size;
    parameters.PSF_theo    = PSF_theo;
    parameters.par_start   = par_start;
    parameters.flag_struct = flag_struct;
    parameters.mode_fit    = mode_fit;
    flag_struct.output = 0;
    
    spots_fit = spots_fit_batch_3D_Gauss_v6(spots_detected, sub_spots,parameters);
                           
    %- Save fitted spots for this cell
    cell_prop(i_cell).spots_fit      = spots_fit;
    cell_prop(i_cell).spots_detected = spots_detected;
end
     

