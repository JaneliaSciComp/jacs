function handles = FQ_PSF_analyze_v1(handles) 


%== Get parameters
par_microscope    = handles.par_microscope;
fact_os           = handles.fact_os;

pixel_size_os.xy  = par_microscope.pixel_size.xy / fact_os.xy;
pixel_size_os.z   = par_microscope.pixel_size.z  / fact_os.z;


% ==== Read in image and analyze

%- Get background
if not(isempty(handles.BGD_file_name))
    file_bgd = fullfile(handles.BGD_path_name, handles.BGD_file_name);
    img_BGD_struct = load_stack_data_v4(file_bgd);    
    PSF_BGD = img_BGD_struct.data;
else
    PSF_BGD = handles.bgd_value;
end

%- Assign parameters
parameters_PSF.PSF_BGD        = PSF_BGD;
parameters_PSF.par_microscope = par_microscope;
parameters_PSF.pixel_size     = pixel_size_os;
parameters_PSF.par_crop       = [];

%- Flags for analysis
parameters_PSF.flags.crop   = 1;
parameters_PSF.flags.output = 1;

%- Read in PSF and analyze
file_name_PSF                                  = fullfile(handles.PSF_path_name, handles.PSF_file_name);
[handles.img_PSF_OS_struct handles.PSF_OS_fit file_name_PSF] = PSF_3d_analyse_v3(file_name_PSF, parameters_PSF);
[handles.PSF_path_name name ext]  = fileparts(file_name_PSF);
handles.PSF_file_name = [name,ext];

% ==== Generate shifted PSF for reconstruction
range_shift_xy = (0:1:fact_os.xy-1) - floor(fact_os.xy/2);
range_shift_z  = (0:1:fact_os.z-1)  - floor(fact_os.z/2);

parameters_shift.fact_os        = fact_os;
parameters_shift.pixel_size_os  = pixel_size_os;
parameters_shift.pixel_size     = par_microscope.pixel_size;
parameters_shift.range_shift_xy = range_shift_xy;
parameters_shift.range_shift_z  = range_shift_z;

parameters_shift.par_crop       = [];
parameters_shift.par_microscope = par_microscope;
parameters_shift.flags.crop     = 1;
parameters_shift.flags.output   = 0;
parameters_shift.flags.norm     = 0;  % Normalize to same total intensity

handles.PSF_shift               = PSF_3D_generate_shifted_v1(handles.img_PSF_OS_struct ,parameters_shift);

