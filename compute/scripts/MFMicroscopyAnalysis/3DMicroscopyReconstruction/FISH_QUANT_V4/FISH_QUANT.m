function varargout = FISH_QUANT(varargin)
% FISH_QUANT M-file for FISH_QUANT.fig

% Last Modified by GUIDE v2.5 10-Sep-2011 13:54:46

% Begin initialization code - DO NOT EDIT
gui_Singleton = 1;
gui_State = struct('gui_Name',       mfilename, ...
                   'gui_Singleton',  gui_Singleton, ...
                   'gui_OpeningFcn', @FISH_QUANT_OpeningFcn, ...
                   'gui_OutputFcn',  @FISH_QUANT_OutputFcn, ...
                   'gui_LayoutFcn',  [] , ...
                   'gui_Callback',   []);
if nargin && ischar(varargin{1})
    gui_State.gui_Callback = str2func(varargin{1});
end

if nargout
    [varargout{1:nargout}] = gui_mainfcn(gui_State, varargin{:});
else
    gui_mainfcn(gui_State, varargin{:});
end
% End initialization code - DO NOT EDIT


% --- Executes just before FISH_QUANT is made visible.
function handles = FISH_QUANT_OpeningFcn(hObject, eventdata, handles, varargin)

%- Set font-size to 10
%  For whatever reason are all the fonts on windows set back to 8 when the
%  .fig is openend

h_font_8 = findobj(handles.h_fishquant,'FontSize',8);
set(h_font_8,'FontSize',10)

%- Some parameters
handles.file_name_settings = [];

%- Get installation directory of FISH-QUANT and initiate 
p               = mfilename('fullpath');        
handles.FQ_path = fileparts(p); 
handles         = FISH_QUANT_start_up_v2(handles);

%- Change name of GUI
%set(handles.h_fishquant,'Name', ['FISH-QUANT ', handles.version, ': main interface']);

%- Initialize GUI
%  (a) settings up the parameters
%  (b) preparing GUI with these parameters
handles = FISH_QUANT_init_v3(handles);
handles = FISH_QUANT_populate_v2(handles);

%- Initialize MIJ
call_options = varargin;

if handles.flag_JAVA_init  == 0
    handles                = Java_tools_init_v1(handles,call_options);
    handles.flag_JAVA_init = 1;
end

%- Enable controls
FISH_QUANT_enable_controls_v8(handles);

%= Invisible plots
%- Clear the plot axes
cla(handles.axes_image,'reset');
cla(handles.axes_histogram_th,'reset');
cla(handles.axes_histogram_all,'reset');
cla(handles.axes_proj_xy,'reset');
cla(handles.axes_proj_xz,'reset');
cla(handles.axes_resid_xy,'reset');

set(handles.axes_image,'Visible','off');
set(handles.axes_histogram_th,'Visible','off');
set(handles.axes_histogram_all,'Visible','off');
set(handles.axes_proj_xy,'Visible','off');
set(handles.axes_proj_xz,'Visible','off');
set(handles.axes_resid_xy,'Visible','off');

%- Calculate PSF with default settings
pop_up_exp_default_Callback(hObject, eventdata, handles) 

%- 
set(handles.text_psf_fit_sigmaX,'String', ' ');
set(handles.text_psf_fit_sigmaY,'String', ' ');
set(handles.text_psf_fit_sigmaZ,'String', ' ');
set(handles.text_psf_fit_amp,'String',    ' ');
set(handles.text_psf_fit_bgd,'String',    ' ');

%- Save values
status_update(hObject, eventdata, handles,{'FISH-QUANT successfully initiated.'})
handles.output = hObject;
guidata(hObject, handles);

FISH_QUANT_enable_controls_v8(handles)



% --- Outputs from this function are returned to the command line.
function varargout = FISH_QUANT_OutputFcn(hObject, eventdata, handles) 
varargout{1} = handles.output;



%==========================================================================
%==== Experimental settings
%==========================================================================

%== Modify the experimental settings
function button_define_exp_Callback(hObject, eventdata, handles)
par_microscope = handles.par_microscope;

dlgTitle = 'Experimental parameters';

prompt(1) = {'Pixel-size xy [nm]'};
prompt(2) = {'Pixel-size z [nm]'};
prompt(3) = {'Refractive index'};
prompt(4) = {'Numeric aperture NA'};
prompt(5) = {'Emission wavelength'};
prompt(6) = {'Excitation wavelength'};
prompt(7) = {'Microscope'};

defaultValue{1} = num2str(par_microscope.pixel_size.xy);
defaultValue{2} = num2str(par_microscope.pixel_size.z);
defaultValue{3} = num2str(par_microscope.RI);
defaultValue{4} = num2str(par_microscope.NA);
defaultValue{5} = num2str(par_microscope.Em);
defaultValue{6} = num2str(par_microscope.Ex);
defaultValue{7} = num2str(par_microscope.type);

userValue = inputdlg(prompt,dlgTitle,1,defaultValue);

if( ~ isempty(userValue))
    par_microscope.pixel_size.xy = str2double(userValue{1});
    par_microscope.pixel_size.z  = str2double(userValue{2});   
    par_microscope.RI            = str2double(userValue{3});   
    par_microscope.NA            = str2double(userValue{4});
    par_microscope.Em            = str2double(userValue{5});   
    par_microscope.Ex            = str2double(userValue{6});
    par_microscope.type    = userValue{7};   
end

handles.par_microscope = par_microscope;
set(handles.pop_up_exp_default,'Value',4);
guidata(hObject, handles);
pop_up_exp_default_Callback(hObject, eventdata, handles)  


%== Pop-up menu to select default settings for experimental setup
function pop_up_exp_default_Callback(hObject, eventdata, handles)  
par_microscope = handles.par_microscope;

% Determine the selected data set.
str = get(handles.pop_up_exp_default, 'String');
val = get(handles.pop_up_exp_default,'Value');

%- Set experimental settings based on selection
switch str{val};
    
    case 'Betrand lab' 
    par_microscope.pixel_size.xy = 160;
    par_microscope.pixel_size.z  = 300;   
    par_microscope.RI            = 1.458;   
    par_microscope.NA            = 1.25;
    par_microscope.Em            = 568;   
    par_microscope.Ex            = 568;
    par_microscope.type          = 'widefield';    
    
    case 'Darzacq lab' 
    par_microscope.pixel_size.xy = 64;
    par_microscope.pixel_size.z  = 300;   
    par_microscope.RI            = 1.458;   
    par_microscope.NA            = 1.4;
    par_microscope.Em            = 568;   
    par_microscope.Ex            = 568;
    par_microscope.type          = 'widefield';   
    
    case 'Zimmer lab' 
    par_microscope.pixel_size.xy = 64;
    par_microscope.pixel_size.z  = 400;   
    par_microscope.RI            = 1.33;   
    par_microscope.NA            = 1.4;
    par_microscope.Em            = 509;   
    par_microscope.Ex            = 509;
    par_microscope.type          = 'widefield'; 
    
    case 'User settings' 
 
end

%- Calculate theoretical PSF and show it 
[PSF_theo.xy_nm, PSF_theo.z_nm] = sigma_PSF_BoZhang_v1(par_microscope);
PSF_theo.xy_pix = PSF_theo.xy_nm / par_microscope.pixel_size.xy ;
PSF_theo.z_pix  = PSF_theo.z_nm  / par_microscope.pixel_size.z ;

set(handles.text_psf_theo_xy,'String',num2str(round(PSF_theo.xy_nm)));
set(handles.text_psf_theo_z, 'String',num2str(round(PSF_theo.z_nm)));

%- Calculate size of detection region and show it
handles.detect.region.xy = round(2*PSF_theo.xy_pix)+1;       % Size of detection zone in xy 
handles.detect.region.z  = round(2*PSF_theo.z_pix)+1;        % Size of detection zone in z 

%- Update handles structure
handles.PSF_theo       = PSF_theo;
handles.par_microscope = par_microscope;
guidata(hObject, handles);



%==========================================================================
%==== IMAGE: load, bgd, outline
%==========================================================================

%== Load image and show maximum projection
function handles = button_load_image_Callback(hObject, eventdata, handles)

button = questdlg('Loading new image will delete results of previous analysis. Continue?','Load new image','Yes','No','No');

if strcmp(button,'Yes')    
         
    [file_name_image,path_name_image] = uigetfile({'*.tif';'*.stk';'*.dv';'*.TIF'},'Select file');

    if file_name_image ~= 0

        %- Load image and plot
        handles.image_struct               = load_stack_data_v4(fullfile(path_name_image,file_name_image));
        handles.image_struct.data_filtered = handles.image_struct.data;
        
        %- Delete results of detection
        
        handles.cell_prop = struct('label', {}, 'x', {}, 'y', {}, 'pos_TS', {}, 'spots_fit', {},'thresh',{},'spots_proj',{});

        cla(handles.axes_image,'reset');
        cla(handles.axes_histogram_th,'reset');
        cla(handles.axes_histogram_all,'reset');
        cla(handles.axes_proj_xy,'reset');
        cla(handles.axes_proj_xz,'reset');
        cla(handles.axes_resid_xy,'reset');

        set(handles.axes_histogram_th,'Visible','off');
        set(handles.axes_histogram_all,'Visible','off');
        set(handles.axes_proj_xy,'Visible','off');
        set(handles.axes_proj_xz,'Visible','off');
        set(handles.axes_resid_xy,'Visible','off');        
        
        handles.status_image      = 1;
        handles.status_filtered   = 0;

        %- Save results
        handles.file_name_image = file_name_image;
        handles.path_name_image = path_name_image;
        
        handles.file_name_image_filtered = [];
        
        guidata(hObject, handles);

        %- Plot results and enable controls
        status_update(hObject, eventdata, handles,{'Image loaded.'})
        set(handles.pop_up_image_select,'Value',1);     % Save selection to raw image
        
        plot_image(handles,handles.axes_image);
        FISH_QUANT_enable_controls_v8(handles)
    end
end


%== BGD correction
function button_correct_bgd_Callback(hObject, eventdata, handles)
[file_name_bgd,path_name_bgd] = uigetfile({'*.txt'},'Select file with background');

if file_name_bgd ~= 0
    
    %- Read in fil
    name_full = fullfile(path_name_bgd,file_name_bgd);
    fid = fopen(name_full);
    
    BGD = textscan(fid,' %f %f','delimiter', '/t','CollectOutput',1);
    fclose(fid);
    
    BGD_val    = BGD{1};
    
    %- Generate matrix with background values
    [NY NX N_Z] = size(handles.image_struct.data);
    dum(1,1,:) = BGD_val(:,2);
    BGD_matrix = repmat(dum,[NY,NX,1]);
    
    
    %- Subtract background
    handles.image_struct.data = handles.image_struct.data - BGD_matrix;
    handles.image_struct.data_filtered = handles.image_struct.data_filtered - BGD_matrix;

    guidata(hObject, handles);
end


%==========================================================================
%==== LOAD AND SAVE
%==========================================================================

%== Menu: save filtered image
function menu_save_filtered_img_Callback(hObject, eventdata, handles)

current_dir = pwd;
cd(handles.path_name_image)

[dum, name_file] = fileparts(handles.file_name_image); 
file_name_save   = [name_file,'_filtered.tif'];

[file_name_save,path_name_save] = uiputfile(file_name_save,'Specify file name to save filtered image'); 
name_save = fullfile(path_name_save,file_name_save);

if file_name_save ~= 0
    image_save_v1(handles.image_struct.data_filtered,name_save);
    handles.file_name_image_filtered = file_name_save;
    guidata(hObject, handles);
end

cd(current_dir)


%== Menu: save outline of cell and TS
function menu_save_outline_Callback(hObject, eventdata, handles)
FISH_QUANT_save_outline_v5(handles,[])


%== Menu: save results of spot detection   
function file_name_results = menu_save_spots_Callback(hObject, eventdata, handles)
if isempty(handles.file_name_settings)
    [handles.file_name_settings handles.path_name_settings] = FISH_QUANT_save_settings_v7([],handles);
end

parameters.cell_prop                = handles.cell_prop;
parameters.par_microscope           = handles.par_microscope;
parameters.path_name_image          = handles.path_name_image;
parameters.file_name_image          = handles.file_name_image;
parameters.file_name_image_filtered = handles.file_name_image_filtered;
parameters.file_name_settings       = handles.file_name_settings;
parameters.version                  = handles.version;

file_name_results = FISH_QUANT_save_results_v7([],parameters);
handles.file_name_results  = file_name_results;
guidata(hObject, handles); 
    

%== Menu: save settings  
function menu_save_settings_Callback(hObject, eventdata, handles)
[handles.file_name_settings handles.path_name_settings] = FISH_QUANT_save_settings_v7([],handles);
guidata(hObject, handles);


%== Menu: load settings  
function menu_load_settings_Callback(hObject, eventdata, handles)
[file_name_settings,path_name_settings] = uigetfile({'*.txt'},'Select file with settings');

if file_name_settings ~= 0
    handles = load_settings(fullfile(path_name_settings,file_name_settings),handles);    
    handles = FISH_QUANT_populate_v2(handles);  
    guidata(hObject, handles);
end


%== Function to load settings and assign thresholds
function handles = load_settings(file_name_full, handles)

%- Set all threshold locks to zero - the ones which are locked will be
% changed to one 
handles.thresh_all.sigmaxy.lock = 0;
handles.thresh_all.sigmaz.lock  = 0;
handles.thresh_all.amp.lock     = 0;
handles.thresh_all.bgd.lock     = 0;
handles.thresh_all.pos_z.lock   = 0;

handles = FISH_QUANT_load_settings_v3(file_name_full,handles);

%- Update some of the parameters
handles = FISH_QUANT_populate_v2(handles);

%- Update when fit with fixed parameters is selected

if handles.flag_fit == 0;
    set(handles.checkbox_fit_fixed_width,'Value',0);    
else
    set(handles.checkbox_fit_fixed_width,'Value',1);  
    
    set(handles.text_psf_fit_sigmaX,'String', num2str(handles.par_fit.sigma_XY_fixed));
    set(handles.text_psf_fit_sigmaY,'String', num2str(handles.par_fit.sigma_XY_fixed));
    set(handles.text_psf_fit_sigmaZ,'String', num2str(handles.par_fit.sigma_Z_fixed));
    
    set(handles.pop_up_select_psf,'Value',4);
end


%- Update the ones that had a locked threshold
names_all = fieldnames(handles.thresh_all);
    
N_names   = size(names_all,1);

for i_name = 1:N_names
    par_name   = char(names_all{i_name});
    par_fields = getfield(handles.thresh_all,par_name);
    locked     = par_fields.lock;

    if locked
        par_fields.min_hist;
        par_fields.max_hist;        
                    
            switch par_name
                
                case 'sigmaxy'
                    handles.thresh_all.sigmaxy.min_hist = par_fields.min_hist;                 
                    handles.thresh_all.sigmaxy.max_hist = par_fields.max_hist;   
                
                case 'sigmaz'
                     handles.thresh_all.sigmaz.min_hist = par_fields.min_hist;                 
                    handles.thresh_all.sigmaz.max_hist = par_fields.max_hist;   
                
                case 'amp'
                    handles.thresh_all.amp.min_hist = par_fields.min_hist;                 
                    handles.thresh_all.amp.max_hist = par_fields.max_hist;                  
                    
                case 'bgd'
                    handles.thresh_all.bgd.min_hist = par_fields.min_hist;                 
                    handles.thresh_all.bgd.max_hist = par_fields.max_hist; 
                    
                case 'pos_z'
                    handles.thresh_all.pos_z.min_hist = par_fields.min_hist;                 
                    handles.thresh_all.pos_z.max_hist = par_fields.max_hist;    
                 
                otherwise
                    warndlg('Thresholding parameter not defined.','load_settings');
            end
    end
end
       
 
%== Menu item to load image
function menu_load_image_Callback(hObject, eventdata, handles)
handles = button_load_image_Callback(hObject, eventdata, handles);
guidata(hObject, handles);


%== Menu: load outline of cell and position of transcription site
function menu_load_outline_Callback(hObject, eventdata, handles)
handles = load_result_files(hObject, eventdata, handles);
guidata(hObject, handles);
status_update(hObject, eventdata, handles,{'Outline loaded.'})


%== Button: load outline of cell and position of transcription site
function button_outline_load_Callback(hObject, eventdata, handles)
handles = load_result_files(hObject, eventdata, handles);
guidata(hObject, handles);
status_update(hObject, eventdata, handles,{'Outline and results (if defined) loaded.'})


%== Button: determine outline of cell and position of transcription site
function button_outline_define_Callback(hObject, eventdata, handles)
handles.cell_prop  = FISH_QUANT_outline('HandlesMainGui',handles);  
set(handles.checkbox_plot_outline,'Value',1);
handles = analyze_cellprop(hObject, eventdata, handles); 
guidata(hObject, handles);


%= Menu to load detected spots
function menu_load_detected_spots_Callback(hObject, eventdata, handles)
handles = load_result_files(hObject, eventdata, handles);
guidata(hObject, handles);
status_update(hObject, eventdata, handles,{'Outline and results (if defined) loaded.'})


%= Menu to load detected spots
function handles = load_result_files(hObject, eventdata, handles)
[file_name_results,path_name_results] = uigetfile({'*.txt'},'Select file');

if file_name_results ~= 0
    
    %- Reset GUI
    FISH_QUANT_OpeningFcn(hObject, eventdata, handles); 
    
    %- Load results
    [cell_prop file_name par_microscope file_name_settings file_name_image_filtered] = FISH_QUANT_load_results_v7(fullfile(path_name_results,file_name_results));
        
    set(handles.checkbox_plot_outline,'Value',1);
    
    %- Assign results
    handles.cell_prop           = cell_prop;
    handles.path_name_image     = path_name_results;
    handles.file_name_image     = file_name;
    handles.par_microscope      = par_microscope;
    handles.file_name_settings  = file_name_settings;
    handles.file_name_image_filtered  = file_name_image_filtered;
    handles.file_name_results  = file_name_results;    
    
    %- Load image 
    handles.image_struct    = load_stack_data_v4(fullfile(handles.path_name_image,handles.file_name_image));
    handles.status_image    = 1;
    
     %- Load filtered image if specified 
     if not(isempty(handles.file_name_image_filtered))
         image_filt  = load_stack_data_v4(fullfile(handles.path_name_image,handles.file_name_image_filtered));
         handles.image_struct.data_filtered = image_filt.data;
         handles.status_filtered   = 1;        
     else     
        handles.image_struct.data_filtered = handles.image_struct.data;
        handles.status_filtered   = 0;
    end
    
    
    %- Load settings if specified in file
    if not(isempty(file_name_settings))
        handles = load_settings(fullfile(handles.path_name_image,file_name_settings),handles);    
        handles = FISH_QUANT_populate_v2(handles); 
    end    
          
    %- Update experimental settings
    set(handles.pop_up_exp_default,'Value',4);

    [PSF_theo.xy_nm, PSF_theo.z_nm] = sigma_PSF_BoZhang_v1(handles.par_microscope);
    PSF_theo.xy_pix = PSF_theo.xy_nm / handles.par_microscope.pixel_size.xy ;
    PSF_theo.z_pix  = PSF_theo.z_nm  / handles.par_microscope.pixel_size.z ;
    handles.PSF_theo = PSF_theo;
    
    set(handles.text_psf_theo_xy,'String',num2str(round(PSF_theo.xy_nm)));
    set(handles.text_psf_theo_z, 'String',num2str(round(PSF_theo.z_nm)));
        
    %- Analyze detected regions
    handles = analyze_cellprop(hObject, eventdata, handles);    
    status_update(hObject, eventdata, handles,{'Results of spot detection loaded.'}) 
    
    %- Save everything
    guidata(hObject, handles); 
end
    

%= Function to analyze detected regions
function handles = analyze_cellprop(hObject, eventdata, handles)

cell_prop = handles.cell_prop;
detect    = handles.detect;
dim_sub_z = 2*detect.region.z+1;

%- Parallel computing - open MATLAB session for parallel computation 
flag_struct.parallel = get(handles.checkbox_parallel_computing, 'Value');

%- Populate pop-up menu with labels of cells
N_cell = size(cell_prop,2);

if N_cell > 0

    %- Call pop-up function to show results and bring values into GUI
    for i = 1:N_cell
        str_menu{i,1} = cell_prop(i).label;

        cell_prop(i).status_filtered = 0;    % Image filterd
        cell_prop(i).status_image    = 1;    % Image loaded
        cell_prop(i).status_detect   = 0;    % Spots detected
        cell_prop(i).status_fit      = 0;    % Spots fit with 3D Gaussian
        cell_prop(i).status_avg      = 0;    % Spots averaged
        cell_prop(i).status_avg_rad  = 0;    % Averaged spots were fit
        cell_prop(i).status_avg_con  = 0;    % Averaged spots were used for reconstruction
        cell_prop(i).spots_proj      = 0;    % Averaged spots were fit        
        
        %- Assign thresholding parameters
        if not(isempty(cell_prop(i).spots_fit))
            cell_prop(i).status_fit        = 1;
            cell_prop(i).FIT_Result        = [];            
            
            %- Extract sub-spots for fitting
            image_struct      = handles.image_struct;
            img_mask          = [];                    
            PSF_theo          = handles.PSF_theo;
            
            flag_struct.output    = 0;
            flag_struct.score     = detect.score;
            flag_struct.threshold = 0;
            
            %= Options            
            [sub_spots sub_spots_filt] = spots_predetect_mosaic_v1(image_struct,img_mask,cell_prop(i).spots_detected,flag_struct);

            %- Calculate projections for plot with montage function
            spots_proj = [];
            for k=1:size(cell_prop(i).spots_fit,1)
                
                %- MIP in xy
                MIP_xy = max(sub_spots{k},[],3);
                spots_proj.xy(:,:,1,k) = MIP_xy;
                
                %- MIP in XZ
                MIP_xz = squeeze(max(sub_spots{k},[],1))';
                dim_MIP_z = size(MIP_xz,1); 
            
                %- Add zeros if not enough planes (for incomplete spots)
                if dim_MIP_z < dim_sub_z
                   MIP_xz(dim_MIP_z+1:dim_sub_z,:) = 0;
                end
                spots_proj.xz(:,:,1,k) = MIP_xz;
            end
            
            %- Save results
            cell_prop(i).spots_proj     = spots_proj;
            cell_prop(i).status_detect  = 1;
            cell_prop(i).spots_detected = cell_prop(i).spots_detected;
            cell_prop(i).sub_spots      = sub_spots;
            cell_prop(i).sub_spots_filt = sub_spots_filt;            
        end        
  
    end  
else
    str_menu = {' '};
end

%- Save and analyze results
set(handles.pop_up_outline_sel_cell,'String',str_menu);
set(handles.pop_up_outline_sel_cell,'Value',1);

handles.cell_prop = cell_prop;
handles.detect    = detect;

handles = pop_up_outline_sel_cell_Callback(hObject, eventdata, handles);        

%- Enable outline selection
FISH_QUANT_enable_controls_v8(handles)

%- Save everything
guidata(hObject, handles); 
status_update(hObject, eventdata, handles,{'Spot data analyzed.'})        


%== Used defined outline of cell and populate everything
function handles = pop_up_outline_sel_cell_Callback(hObject, eventdata, handles)

val       = get(handles.pop_up_outline_sel_cell,'Value');
cell_prop = handles.cell_prop;

%- If results of spot detection were saved as well
if not(isempty(cell_prop))
    if not(isempty(cell_prop(val).spots_fit))     
        handles.cell_prop(val).status_fit = 1;
        handles                           = fit_analyze(hObject,eventdata, handles);    
        FISH_QUANT_enable_controls_v8(handles)    
    else
        cla(handles.axes_histogram_th,'reset');
        cla(handles.axes_histogram_all,'reset');
        cla(handles.axes_proj_xy,'reset');
        cla(handles.axes_proj_xz,'reset');
        cla(handles.axes_resid_xy,'reset');

        set(handles.axes_histogram_th,'Visible','off');
        set(handles.axes_histogram_all,'Visible','off');
        set(handles.axes_proj_xy,'Visible','off');
        set(handles.axes_proj_xz,'Visible','off');
        set(handles.axes_resid_xy,'Visible','off');   
    end
    
else
    cla(handles.axes_histogram_th,'reset');
    cla(handles.axes_histogram_all,'reset');
    cla(handles.axes_proj_xy,'reset');
    cla(handles.axes_proj_xz,'reset');
    cla(handles.axes_resid_xy,'reset');

    set(handles.axes_histogram_th,'Visible','off');
    set(handles.axes_histogram_all,'Visible','off');
    set(handles.axes_proj_xy,'Visible','off');
    set(handles.axes_proj_xz,'Visible','off');
    set(handles.axes_resid_xy,'Visible','off');   
end

%- Save results 
guidata(hObject, handles);

%- Update controls and plot
plot_image(handles,handles.axes_image);
FISH_QUANT_enable_controls_v8(handles)



%==========================================================================
%==== Detection + Fit
%==========================================================================

%=== Filter image for pre-detection
function button_filter_Callback(hObject, eventdata, handles)
set(handles.h_fishquant,'Pointer','watch');
PSF_theo = handles.PSF_theo;

filter.factor_bgd = str2double(get(handles.text_kernel_factor_bgd,'String'));
filter.factor_psf = str2double(get(handles.text_kernel_factor_filter,'String'));
filter.pad        = 2*filter.factor_bgd;

%- 1. Pad array with Matlab function padarray
img_pad = double(padarray(handles.image_struct.data,[filter.pad filter.pad filter.pad],'symmetric','both'));

%- 2. Background: apply Gaussian smoothing to images.
status_update(hObject, eventdata, handles,{'Filtering [1/2]: determine background .... in progress ....'})
if filter.factor_bgd 
    img_bgd  = gaussSmooth(img_pad, filter.factor_bgd*[PSF_theo.xy_pix PSF_theo.xy_pix PSF_theo.z_pix], 'same');    
else
    img_bgd = zeros(size(img_pad));
end
img_diff = img_pad-img_bgd;
img_diff = img_diff.*(img_diff>0);      % Set negative values to zero

%- 3. Convolution with the Theoretical gaussian Kernel
status_update(hObject, eventdata, handles,{'Filtering [2/2]: spot enhancement .... in progress ....'})
if filter.factor_psf
    img_filt = gaussSmooth( img_diff, filter.factor_psf *[PSF_theo.xy_pix PSF_theo.xy_pix PSF_theo.z_pix], 'same');    
    img_filt = img_filt.*(img_filt>0);
else
    img_filt = img_diff;
end
       
img_filt = img_filt(filter.pad+1:end-filter.pad,filter.pad+1:end-filter.pad,filter.pad+1:end-filter.pad);
handles.img_plot = max(img_filt,[],3);

handles.image_struct.data_filtered = img_filt;
handles.filter                     = filter;
handles.status_filtered            = 1;

guidata(hObject, handles);

%- Enable corresponding options in GUI
FISH_QUANT_enable_controls_v8(handles)

%- Show filtered image (maximum projection)
axes(handles.axes_image);
h_plot = imshow(handles.img_plot,[]);
set(h_plot, 'ButtonDownFcn', @axes_image_ButtonDownFcn)
title('Maximum projection of filtered image (Gaussian)','FontSize',8); 
colormap(hot)
status_update(hObject, eventdata, handles,{'Filtering: FINISHED!'})
set(handles.h_fishquant,'Pointer','arrow');

%=== Pre-detection
function button_predetect_Callback(hObject, eventdata, handles)

status_update(hObject, eventdata, handles,{'Pre-detection .... in progress ..... '})
[handles.cell_prop handles.detect ] = FISH_QUANT_predetect('HandlesMainGui',handles);
guidata(hObject, handles);

%- Update GUI and enable controls
FISH_QUANT_enable_controls_v8(handles)
status_update(hObject, eventdata, handles,{'Pre-detection: FINISHED'})


%=== Fit with 3D Gaussian
function button_fit_3d_Callback(hObject, eventdata, handles)
set(handles.h_fishquant,'Pointer','watch');

status_update(hObject, eventdata, handles,{'Fitting: STARTED ... '})

%- Some parameters
PSF_theo              = handles.PSF_theo;
pixel_size            = handles.par_microscope.pixel_size;
flag_struct.parallel  = get(handles.checkbox_parallel_computing, 'Value');
detect.region         = handles.detect.region;

%- All cells or which cell?
status_proc_all = 1;
if status_proc_all
    N_cells = length(handles.cell_prop);
    ind_cell_v  = (1:N_cells);
else
    N_cells = 1;
    ind_cell_v  = get(handles.pop_up_outline_sel_cell,'Value');
end

%== Determine mode of fitting
flag_fit  = handles.flag_fit; %get(handles.checkbox_fit_fixed_width, 'Value');

if flag_fit == 0
    mode_fit        = 'sigma_free_xz';
    par_start       = [];
    handles.par_fit = [];
else
    mode_fit       = 'sigma_fixed';    
    sigma.xy = str2double(get(handles.text_psf_fit_sigmaX,'String'));
    sigma.z  = str2double(get(handles.text_psf_fit_sigmaZ,'String'));

    %- Assign starting value
    par_start.sigmax = sigma.xy;
    par_start.sigmay = sigma.xy;
    par_start.sigmaz = sigma.z;

    %- Save values
    handles.par_fit.sigma_XY_fixed = sigma.xy;
    handles.par_fit.sigma_Z_fixed  = sigma.z; 
end

%== Loop over cells
for ind_cell_rel = 1:N_cells

    ind_cell       = ind_cell_v(ind_cell_rel);
    spots_detected = handles.cell_prop(ind_cell).spots_detected;
    sub_spots      = handles.cell_prop(ind_cell).sub_spots;

    %- Call fitting routine 
    parameters.pixel_size  = pixel_size;
    parameters.PSF_theo    = PSF_theo;
    parameters.par_start   = par_start;
    parameters.flag_struct = flag_struct;
    parameters.mode_fit    = mode_fit;
    
    [spots_fit FIT_Result] = spots_fit_batch_3D_Gauss_v6(spots_detected, sub_spots,parameters);

    %- Assigning thresholding parameters
    thresh.all  = ones(size(spots_fit,1),1);
    thresh.in   = ones(size(spots_fit,1),1);

    %- Save fitted spots for this cell
    handles.cell_prop(ind_cell).spots_fit  = spots_fit;
    handles.cell_prop(ind_cell).FIT_Result = FIT_Result;
    handles.cell_prop(ind_cell).thresh     = thresh;
    handles.cell_prop(ind_cell).status_fit = 1;      
end

%- Some thresholding parameters
handles.thresh_all.sigmaxy.lock   = 0;
handles.thresh_all.sigmaz.lock    = 0;
handles.thresh_all.amp.lock       = 0;
handles.thresh_all.bgd.lock       = 0;
handles.thresh_all.pos_z.lock     = 0;

%- Analyze results
handles = fit_analyze(hObject,eventdata, handles);

%- Save results
guidata(hObject, handles);
status_update(hObject, eventdata, handles,{'Fitting: ... FINSIHED! '})
set(handles.h_fishquant,'Pointer','arrow');

%=== Function to analyze the results of the fit
function handles = fit_analyze(hObject,eventdata, handles)

col_par = handles.col_par;

%- Extracted fitted spots for this cell
ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
spots_fit  = handles.cell_prop(ind_cell).spots_fit;
FIT_Result = handles.cell_prop(ind_cell).FIT_Result;
spots_proj = handles.cell_prop(ind_cell).spots_proj; 
thresh     = handles.cell_prop(ind_cell).thresh; 
thresh_all = handles.thresh_all;

%- Clear the plot axes
cla(handles.axes_image,'reset');
cla(handles.axes_histogram_th,'reset');
cla(handles.axes_histogram_all,'reset');
cla(handles.axes_proj_xy,'reset');
cla(handles.axes_proj_xz,'reset');
cla(handles.axes_resid_xy,'reset');    

%- Experimental PSF settings
if not(isempty(spots_fit))
    PSF_exp.sigmax_all = mean(spots_fit(:,col_par.sigmax));
    PSF_exp.sigmax_th  = mean(spots_fit(:,col_par.sigmax));
    PSF_exp.sigmax_avg = mean(spots_fit(:,col_par.sigmax));
    
    PSF_exp.sigmax_all_std = std(spots_fit(:,col_par.sigmax));
    PSF_exp.sigmax_th_std  = std(spots_fit(:,col_par.sigmax));
    PSF_exp.sigmax_avg_std = std(spots_fit(:,col_par.sigmax));

    PSF_exp.sigmay_all = mean(spots_fit(:,col_par.sigmay));
    PSF_exp.sigmay_th  = mean(spots_fit(:,col_par.sigmay));
    PSF_exp.sigmay_avg = mean(spots_fit(:,col_par.sigmay));
    
    PSF_exp.sigmay_all_std = std(spots_fit(:,col_par.sigmay));
    PSF_exp.sigmay_th_std  = std(spots_fit(:,col_par.sigmay));
    PSF_exp.sigmay_avg_std = std(spots_fit(:,col_par.sigmay));
    
    PSF_exp.sigmaz_all = mean(spots_fit(:,col_par.sigmaz));
    PSF_exp.sigmaz_th  = mean(spots_fit(:,col_par.sigmaz));
    PSF_exp.sigmaz_avg = mean(spots_fit(:,col_par.sigmaz));
    
    PSF_exp.sigmaz_all_std = std(spots_fit(:,col_par.sigmaz));
    PSF_exp.sigmaz_th_std  = std(spots_fit(:,col_par.sigmaz));
    PSF_exp.sigmaz_avg_std = std(spots_fit(:,col_par.sigmaz));

    PSF_exp.amp_all    = mean(spots_fit(:,col_par.amp));
    PSF_exp.amp_th     = mean(spots_fit(:,col_par.amp));
    PSF_exp.amp_avg    = mean(spots_fit(:,col_par.amp));
    
    PSF_exp.amp_all_std  = std(spots_fit(:,col_par.amp));
    PSF_exp.amp_th_std   = std(spots_fit(:,col_par.amp));
    PSF_exp.amp_avg_std  = std(spots_fit(:,col_par.amp));    

    PSF_exp.bgd_all    = mean(spots_fit(:,col_par.bgd));
    PSF_exp.bgd_th     = mean(spots_fit(:,col_par.bgd));
    PSF_exp.bgd_avg    = mean(spots_fit(:,col_par.bgd));

    PSF_exp.bgd_all_std    = std(spots_fit(:,col_par.bgd));
    PSF_exp.bgd_th_std     = std(spots_fit(:,col_par.bgd));
    PSF_exp.bgd_avg_std    = std(spots_fit(:,col_par.bgd));
    
    set(handles.text_psf_fit_sigmaX,'String', num2str(PSF_exp.sigmax_all,'%.0f'));
    set(handles.text_psf_fit_sigmaY,'String', num2str(PSF_exp.sigmay_all,'%.0f'));
    set(handles.text_psf_fit_sigmaZ,'String', num2str(PSF_exp.sigmaz_all,'%.0f'));
    set(handles.text_psf_fit_amp,'String',    num2str(PSF_exp.amp_all,'%.0f'));
    set(handles.text_psf_fit_bgd,'String',    num2str(PSF_exp.bgd_all,'%.0f'));
    set(handles.pop_up_select_psf,'Value',1);

    %- Allocate according space for residuals 
    if not(isempty(FIT_Result))
        spots_proj.res_xy = zeros(size(spots_proj.xy));
    end

    %- Assign values
    for k=1:size(spots_fit,1)
        if not(isempty(FIT_Result))
            spots_proj.res_xy(:,:,1,k) =  max(FIT_Result{k}.im_residual,[],3);   

        %- Loaded results have sub-spots but not fits and residuals
        elseif not(isempty(spots_proj))
            spots_proj.res_xy(:,:,1,k) =  0;        
        else
            spots_proj = [];
            spots_proj.res_xy(:,:,1,k) =  0;
            spots_proj.xy(:,:,1,k)     =  0;
            spots_proj.xz(:,:,1,k)     =  0;
        end
    end
    
    %- Set-up structure for thresholding
    thresh.sigmaxy.values   = spots_fit(:,col_par.sigmax);
    thresh.sigmaxy.min      = min(spots_fit(:,col_par.sigmax));
    thresh.sigmaxy.max      = max(spots_fit(:,col_par.sigmax));
    thresh.sigmaxy.diff     = max(spots_fit(:,col_par.sigmax)) - min(spots_fit(:,col_par.sigmax));             
    thresh.sigmaxy.in       = thresh.in;
    if thresh_all.sigmaxy.lock == 0
        thresh.sigmaxy.min_hist = min(spots_fit(:,col_par.sigmax));               
        thresh.sigmaxy.max_hist = max(spots_fit(:,col_par.sigmax)); 
    else
        thresh.sigmaxy.min_hist = thresh_all.sigmaxy.min_hist;               
        thresh.sigmaxy.max_hist = thresh_all.sigmaxy.max_hist; 
    end

    thresh.sigmaz.values = spots_fit(:,col_par.sigmaz);
    thresh.sigmaz.min      = min(spots_fit(:,col_par.sigmaz));
    thresh.sigmaz.max      = max(spots_fit(:,col_par.sigmaz));
    thresh.sigmaz.diff     = max(spots_fit(:,col_par.sigmaz)) - min(spots_fit(:,col_par.sigmaz));             
    thresh.sigmaz.in       = thresh.in;  
    if thresh_all.sigmaz.lock == 0
        thresh.sigmaz.min_hist = min(spots_fit(:,col_par.sigmaz));               
        thresh.sigmaz.max_hist = max(spots_fit(:,col_par.sigmaz)); 
    else
        thresh.sigmaz.min_hist = thresh_all.sigmaz.min_hist;               
        thresh.sigmaz.max_hist = thresh_all.sigmaz.max_hist; 
    end

    thresh.amp.values = spots_fit(:,col_par.amp);
    thresh.amp.min      = min(spots_fit(:,col_par.amp));
    thresh.amp.max      = max(spots_fit(:,col_par.amp));
    thresh.amp.diff     = max(spots_fit(:,col_par.amp)) - min(spots_fit(:,col_par.amp));             
    thresh.amp.in       = thresh.in;             
    if thresh_all.amp.lock == 0
        thresh.amp.min_hist = min(spots_fit(:,col_par.amp));               
        thresh.amp.max_hist = max(spots_fit(:,col_par.amp)); 
    else
        thresh.amp.min_hist = thresh_all.amp.min_hist;               
        thresh.amp.max_hist = thresh_all.amp.max_hist; 
    end

    thresh.bgd.values = spots_fit(:,col_par.bgd);
    thresh.bgd.min      = min(spots_fit(:,col_par.bgd));
    thresh.bgd.max      = max(spots_fit(:,col_par.bgd));
    thresh.bgd.diff     = max(spots_fit(:,col_par.bgd)) - min(spots_fit(:,col_par.bgd));             
    thresh.bgd.in       = thresh.in;           
    if thresh_all.bgd.lock == 0
        thresh.bgd.min_hist = min(spots_fit(:,col_par.bgd));               
        thresh.bgd.max_hist = max(spots_fit(:,col_par.bgd)); 
    else
        thresh.bgd.min_hist = thresh_all.bgd.min_hist;               
        thresh.bgd.max_hist = thresh_all.bgd.max_hist; 
    end
    
    thresh.pos_z.values = spots_fit(:,col_par.pos_z);
    thresh.pos_z.min      = min(spots_fit(:,col_par.pos_z));
    thresh.pos_z.max      = max(spots_fit(:,col_par.pos_z));
    thresh.pos_z.diff     = max(spots_fit(:,col_par.pos_z)) - min(spots_fit(:,col_par.pos_z));             
    thresh.pos_z.in       = thresh.in;           
    if thresh_all.pos_z.lock == 0
        thresh.pos_z.min_hist = min(spots_fit(:,col_par.pos_z));               
        thresh.pos_z.max_hist = max(spots_fit(:,col_par.pos_z)); 
    else
        thresh.pos_z.min_hist = thresh_all.pos_z.min_hist;               
        thresh.pos_z.max_hist = thresh_all.pos_z.max_hist; 
    end
    
    %- Save results
    handles.cell_prop(ind_cell).FIT_Result = FIT_Result;
    handles.cell_prop(ind_cell).thresh     = thresh;
    handles.cell_prop(ind_cell).spots_proj = spots_proj; 
    handles.PSF_exp    = PSF_exp;

    %- Call functions to illustrate the fits
    handles = pop_up_threshold_Callback(hObject, eventdata, handles);
    handles = button_threshold_Callback(hObject, eventdata, handles);

    %- Save results
    guidata(hObject, handles);

    %- Set selections for plot accordingly
    set(handles.pop_up_image_select,'Value',2);
    set(handles.pop_up_image_spots,'Value',2);

else
    status_update(hObject, eventdata, handles,{'Fitting: ... no spots for fitting! '})    
end

%- Update GUI and enable controls
FISH_QUANT_enable_controls_v8(handles)
    
    
%==========================================================================
%==== THRESHOLD SPOTS
%==========================================================================


%=== Select thresholding parameter
function handles = pop_up_threshold_Callback(hObject, eventdata, handles)

%- Extracted fitted spots for this cell
ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
spots_fit  = handles.cell_prop(ind_cell).spots_fit;
thresh_all = handles.thresh_all;

%- Executes only if there are results
if not(isempty(spots_fit))
    
    thresh = handles.cell_prop(ind_cell).thresh;
    
    str = get(handles.pop_up_threshold,'String');
    val = get(handles.pop_up_threshold,'Value');
    popup_parameter = str{val};

    
    switch (popup_parameter)
        
        case 'Sigma - XY'
            thresh_sel     = thresh.sigmaxy;
            thresh_sel_all = thresh_all.sigmaxy;
            
            %- Check if selection was locked
            if thresh_all.sigmaxy.lock == 1; set(handles.checkbox_th_lock, 'Value',1); end;
            if thresh_all.sigmaxy.lock == 0; set(handles.checkbox_th_lock, 'Value',0); end;
            
        case 'Sigma - Z'
            thresh_sel = thresh.sigmaz;
            thresh_sel_all = thresh_all.sigmaz;
            
            %- Check if selection was locked
            if thresh_all.sigmaz.lock == 1; set(handles.checkbox_th_lock, 'Value',1); end;
            if thresh_all.sigmaz.lock == 0; set(handles.checkbox_th_lock, 'Value',0); end;
            
        case 'Amplitude'
            thresh_sel = thresh.amp;
            thresh_sel_all = thresh_all.amp;
            
            %- Check if selection was locked
            if thresh_all.amp.lock == 1; set(handles.checkbox_th_lock, 'Value',1); end;
            if thresh_all.amp.lock == 0; set(handles.checkbox_th_lock, 'Value',0); end;
            
        case 'Background'
            thresh_sel = thresh.bgd;
            thresh_sel_all = thresh_all.bgd;
            
            %- Check if selection was locked
            if thresh_all.bgd.lock == 1; set(handles.checkbox_th_lock, 'Value',1); end;
            if thresh_all.bgd.lock == 0; set(handles.checkbox_th_lock, 'Value',0); end;        
            
        case 'Pos (Z)'
            thresh_sel = thresh.pos_z;
            thresh_sel_all = thresh_all.pos_z;
            
            %- Check if selection was locked
            if thresh_all.pos_z.lock == 1; set(handles.checkbox_th_lock, 'Value',1); end;
            if thresh_all.pos_z.lock == 0; set(handles.checkbox_th_lock, 'Value',0); end;             
            
  
    end

    %== For slider functions calls and call of threshold function
    thresh.min  = thresh_sel.min;   
    thresh.max  = thresh_sel.max;
    thresh.diff = thresh_sel.diff; 
    thresh.values = thresh_sel.values; 
    
    
    %== Set sliders and text box according to selection   
    
    %-  Locked - based on saved values
    if thresh_sel_all.lock == 1; 
        
        %- Assign global values to this particular cell
        thresh_sel.min_hist = thresh_sel_all.min_hist;
        thresh_sel.max_hist = thresh_sel_all.max_hist;
        
        %- Update slider values
        value_min = (thresh_sel.min_hist-thresh_sel.min)/thresh_sel.diff;
        if value_min < 0; value_min = 0; end    % Might be necessary if slider was at the left end
        
        value_max = (thresh_sel.max_hist-thresh_sel.min)/thresh_sel.diff;
        if value_max > 1; value_max = 1; end   % Might be necessary if slider was at the right end        
        
        %- Slider for lower limit and corresponding text box
        set(handles.slider_th_min,'Value',value_min)
        set(handles.text_th_min,'String', num2str(thresh_sel.min_hist));     
     
        %- Slider for upper limit and corresponding text box
        set(handles.slider_th_max,'Value',value_max)
        set(handles.text_th_max,'String', num2str(thresh_sel.max_hist));
    
    %- Not locked - not thresholding
    else                    
        
        %- Slider for lower limit and corresponding text box
        set(handles.slider_th_min,'Value',0)
        set(handles.text_th_min,'String', num2str(thresh_sel.min));     
     
        %- Slider for upper limit and corresponding text box
        set(handles.slider_th_max,'Value',1)
        set(handles.text_th_max,'String', num2str(thresh_sel.max));
    end
            
    %=== Update histogram of all values
    cla(handles.axes_histogram_all,'reset');    
   
    %- Plot thresholded histogram   
    axes(handles.axes_histogram_all);
    colormap(jet);
    hist(thresh_sel.values,25);
        
    % Plot lines at locked thresholds if specified
    v = axis;
    hold on
    if thresh_sel_all.lock == 1    
        handles.h_hist_min = plot([thresh_sel.min_hist thresh_sel.min_hist] , [0 1e5],'r');
        handles.h_hist_max = plot([thresh_sel.max_hist thresh_sel.max_hist] , [0 1e5],'g');
    else        
        handles.h_hist_min = plot([thresh_sel.min thresh_sel.min] , [0 1e5],'r');
        handles.h_hist_max = plot([thresh_sel.max thresh_sel.max] , [0 1e5],'g');
    end
    hold off
    axis(v);
    
    title(['Total # of spots: ',sprintf('%d' ,length(thresh.in) )],'FontSize',8);     
    freezeColors(gca) 
    
    %=== Update thresholded histogram
    cla(handles.axes_histogram_th,'reset');
    axes(handles.axes_histogram_th);
    colormap(jet);
    hist(thresh_sel.values(thresh.in==1),25);           
    
    % Plot lines at locked thresholds if specified
    v = axis;
    hold on
    if thresh_sel_all.lock == 1    
        handles.h_hist_th_min = plot([thresh_sel.min_hist thresh_sel.min_hist] , [0 1e5],'r');
        handles.h_hist_th_max = plot([thresh_sel.max_hist thresh_sel.max_hist] , [0 1e5],'g');
    else        
        handles.h_hist_th_min = plot([thresh_sel.min thresh_sel.min] , [0 1e5],'r');
        handles.h_hist_th_max = plot([thresh_sel.max thresh_sel.max] , [0 1e5],'g');
    end
    hold off
    axis(v);
    title(strcat('Selected # of spots ',sprintf('%d' ,sum(thresh.in) )),'FontSize',8);    
    freezeColors(gca)    
        
    %- Save handles-structure
    handles.cell_prop(ind_cell).thresh = thresh;
    handles.output = hObject;
    guidata(hObject, handles);  

end


%=== Threshold data based on selection
function handles = button_threshold_Callback(hObject, eventdata, handles)

%- Extracted fitted spots for this cell
ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
spots_fit  = handles.cell_prop(ind_cell).spots_fit;
thresh_all = handles.thresh_all;

%- Execute only if there are results
if not(isempty(spots_fit))
    
    thresh     = handles.cell_prop(ind_cell).thresh;
    
    %- Locked threshold?
    th_lock  = get(handles.checkbox_th_lock, 'Value');
    
    %- Selected thresholds
    min_hist = floor(str2double(get(handles.text_th_min,'String')));        % floor and ceil necessary for extreme slider position to select all points.
    max_hist = ceil(str2double(get(handles.text_th_max,'String')));      
    
    thresh.min_hist =  min_hist;    
    thresh.max_hist =  max_hist;
    
    %- Thresholding parameter
    str = get(handles.pop_up_threshold,'String');
    val = get(handles.pop_up_threshold,'Value');
    popup_parameter = str{val};     
    
    switch (popup_parameter)
        case 'Sigma - XY'                       
            thresh_all.sigmaxy.lock = th_lock;
            thresh_all.sigmaxy.min_hist = min_hist;
            thresh_all.sigmaxy.max_hist = max_hist;            
            thresh.sigmaxy.in  = (thresh.sigmaxy.values >= min_hist) & (thresh.sigmaxy.values<=max_hist);
            thresh.in_sel      = thresh.sigmaxy.in; 
            
        case 'Sigma - Z'            
            thresh_all.sigmaz.lock = th_lock;
            thresh_all.sigmaz.min_hist = min_hist;
            thresh_all.sigmaz.max_hist = max_hist;            
            thresh.sigmaz.in  = (thresh.sigmaz.values >= min_hist) & (thresh.sigmaz.values<=max_hist);
            thresh.in_sel     = thresh.sigmaz.in; 
            
        case 'Amplitude'            
            thresh_all.amp.lock = th_lock;
            thresh_all.amp.min_hist = min_hist;
            thresh_all.amp.max_hist = max_hist;            
            thresh.amp.in  =   (thresh.amp.values >= min_hist) & (thresh.amp.values<=max_hist);
            thresh.in_sel  = thresh.amp.in;    
            
        case 'Background'            
            thresh_all.bgd.lock = th_lock;
            thresh_all.bgd.min_hist = min_hist;
            thresh_all.bgd.max_hist = max_hist;            
            thresh.bgd.in  = (thresh.bgd.values >= min_hist) & (thresh.bgd.values<=max_hist);
            thresh.in_sel  = thresh.bgd.in;
            
            
       case 'Pos (Z)'            
            thresh_all.pos_z.lock = th_lock;
            thresh_all.pos_z.min_hist = min_hist;
            thresh_all.pos_z.max_hist = max_hist;            
            thresh.pos_z.in  = (thresh.pos_z.values >= min_hist) & (thresh.pos_z.values<=max_hist);
            thresh.in_sel    = thresh.pos_z.in;
            
    end
    
    handles.cell_prop(ind_cell).thresh = thresh;
    handles.thresh_all = thresh_all;
    
    %=== Apply threshold
    handles = threshold_apply(hObject, handles);
    
    %=== Save data
    guidata(hObject, handles);
    
end


%=== Function to apply selected thresholds
function handles = threshold_apply(hObject, handles)

col_par = handles.col_par;

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
thresh_all = handles.thresh_all;
thresh     = handles.cell_prop(ind_cell).thresh;
spots_fit  = handles.cell_prop(ind_cell).spots_fit;

%- Other parameters
PSF_exp    = handles.PSF_exp;

%- Thresholding under consideration of locked ones
%  Can be written in boolean algebra: Implication Z = x?y = ?x?y = not(x) or y
%  http://en.wikipedia.org/wiki/Boolean_algebra_%28logic%29#Basic_operations
%  x = [0,1] ... unlocked [0] and locked [1]
%  y = [0,1] ... index is thresholded [0] or not [1]
%  Number is always considered (z=1) unless paramter is locked (x=1) and number is thresholed (y=0)
%          y
%       |0   1
%     ----------
%   x 0 |1   1
%     1 |0   1

%- Save old thresholding
thresh.in_old         = thresh.in;
thresh.logic_out_man  = (thresh.in == -1);

%- New thresholding only with locked values
thresh.logic_in  = (not(thresh_all.sigmaxy.lock) | thresh.sigmaxy.in == 1) & ...
                   (not(thresh_all.sigmaz.lock)  | thresh.sigmaz.in == 1) & ...
                   (not(thresh_all.amp.lock)     | thresh.amp.in == 1) & ...
                   (not(thresh_all.bgd.lock)     | thresh.bgd.in == 1) & ...
                   (not(thresh_all.pos_z.lock)   | thresh.pos_z.in == 1); 

thresh.in(thresh.logic_in == 1) = 1;
thresh.in(thresh.logic_in == 0) = 0;
thresh.in(thresh.logic_out_man) = -1;

thresh.out = (thresh.in == 0) | (thresh.in == -1);

% Spots which are in considering the current selection even if it is not locked
thresh.in_display = thresh.in_sel  & thresh.logic_in; 

%- Update experimental PSF settings    
PSF_exp.sigmax_th  = mean(spots_fit(thresh.in_display,col_par.sigmax));
PSF_exp.sigmay_th  = mean(spots_fit(thresh.in_display,col_par.sigmay));
PSF_exp.sigmaz_th  = mean(spots_fit(thresh.in_display,col_par.sigmaz));
PSF_exp.amp_th     = mean(spots_fit(thresh.in_display,col_par.amp));
PSF_exp.bgd_th     = mean(spots_fit(thresh.in_display,col_par.bgd));

PSF_exp.sigmax_th_std  = std(spots_fit(thresh.in_display,col_par.sigmay));
PSF_exp.sigmay_th_std  = std(spots_fit(thresh.in_display,col_par.sigmay));
PSF_exp.sigmaz_th_std  = std(spots_fit(thresh.in_display,col_par.sigmaz));
PSF_exp.amp_th_std     = std(spots_fit(thresh.in_display,col_par.amp));
PSF_exp.bgd_th_std     = std(spots_fit(thresh.in_display,col_par.bgd));

set(handles.text_psf_fit_sigmaX,'String', num2str(PSF_exp.sigmax_th,'%.0f'));
set(handles.text_psf_fit_sigmaY,'String', num2str(PSF_exp.sigmay_th,'%.0f'));
set(handles.text_psf_fit_sigmaZ,'String', num2str(PSF_exp.sigmaz_th,'%.0f'));
set(handles.text_psf_fit_amp,'String',    num2str(PSF_exp.amp_th,'%.0f'));
set(handles.text_psf_fit_bgd,'String',    num2str(PSF_exp.bgd_th,'%.0f'));

set(handles.pop_up_select_psf,'Value',2);

%=== Save data
handles.PSF_exp = PSF_exp;
handles.handles.cell_prop(ind_cell).thresh  = thresh;

%=== VARIOUS PLOTS

%- Clear the plot axes
cla(handles.axes_image,'reset');
cla(handles.axes_histogram_th,'reset');
cla(handles.axes_histogram_all,'reset');
cla(handles.axes_proj_xy,'reset');
cla(handles.axes_proj_xz,'reset');
cla(handles.axes_resid_xy,'reset');

%- Plot histogram
handles.cell_prop(ind_cell).thresh = thresh; 

handles = plot_hist_all(handles,handles.axes_histogram_all);

%- Plot thresholded histogram
handles = plot_hist_th(handles,handles.axes_histogram_th);

%- Plot spot-projection in xy
plot_proj_xy(handles,handles.axes_proj_xy)

%- Plot spot-projection in xy
plot_proj_xz(handles,handles.axes_proj_xz)

%- Plot residual-projection in xy
plot_resid_xy(handles,handles.axes_resid_xy)

%- Plot image and position of selected and rejected spots
plot_image(handles,handles.axes_image);

%- Set selections for plot accordingly
%set(handles.pop_up_image_select,'Value',2);
set(handles.pop_up_image_spots,'Value',3);

%=== Save data
guidata(hObject, handles); 


%==== Button to unlock all thresholds
function button_th_unlock_all_Callback(hObject, eventdata, handles)

handles.thresh_all.sigmaxy.lock = 0; 
handles.thresh_all.sigmaz.lock  = 0;
handles.thresh_all.amp.lock     = 0; 
handles.thresh_all.bgd.lock     = 0;               
handles.thresh_all.pos_z.lock   = 0;

handles = pop_up_threshold_Callback(hObject, eventdata, handles);
handles = button_threshold_Callback(hObject, eventdata, handles);

guidata(hObject, handles);


%=== Check-box for locking parameters
function checkbox_th_lock_Callback(hObject, eventdata, handles) 
handles = button_threshold_Callback(hObject, eventdata, handles);
guidata(hObject, handles);


%=== Slider for minimum values of threshold
function slider_th_min_Callback(hObject, eventdata, handles)
sliderValue = get(handles.slider_th_min,'Value');

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
thresh     = handles.cell_prop(ind_cell).thresh;

%- Determine value at current slider position
value_thresh = sliderValue*thresh.diff+thresh.min;

%- Change text box and line in histogram
set(handles.text_th_min,'String', value_thresh);

axes(handles.axes_histogram_all);
delete(handles.h_hist_min);
v = axis;
hold on, 
handles.h_hist_min = plot([value_thresh value_thresh] , [0 1e5],'r');
hold off
axis(v);

axes(handles.axes_histogram_th);
delete(handles.h_hist_th_min);
v = axis;
hold on, 
handles.h_hist_th_min = plot([value_thresh value_thresh] , [0 1e5],'r');
hold off
axis(v);

guidata(hObject, handles);      % Update handles structure


%== Slider for maximum values of threshold
function slider_th_max_Callback(hObject, eventdata, handles)

sliderValue = get(handles.slider_th_max,'Value');

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
thresh     = handles.cell_prop(ind_cell).thresh;

%- Determine value at current slider position
value_thresh = sliderValue*thresh.diff+thresh.min;

%- Change text box and line in histogram
set(handles.text_th_max,'String', value_thresh);

axes(handles.axes_histogram_all);
delete(handles.h_hist_max);
v = axis;
hold on
handles.h_hist_max = plot([value_thresh value_thresh] , [0 1e5],'r');
hold off
axis(v);

axes(handles.axes_histogram_th);
delete(handles.h_hist_th_max);
v = axis;
hold on
handles.h_hist_th_max = plot([value_thresh value_thresh] , [0 1e5],'r');
hold off
axis(v);

guidata(hObject, handles);      % Update handles structure


%=== Edit values of slider selection: minimum 
function text_th_min_Callback(hObject, eventdata, handles)
value_edit = str2double(get(handles.text_th_min,'String'));

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
thresh     = handles.cell_prop(ind_cell).thresh;

%- Set new slider value only if value is within range
if value_edit > thresh.min  && value_edit < thresh.max
    slider_new = (value_edit-thresh.min)/thresh.diff;
    set(handles.slider_th_min,'Value',slider_new);   

else
    %sliderValue  = get(handles.slider_th_min ,'Value');
    %value_edit = sliderValue*thresh.diff+thresh.min;
    set(handles.text_th_min,'String',num2str(value_edit))    
end


%=== Edit values of slider selection: maximum 
function text_th_max_Callback(hObject, eventdata, handles)
value_edit = str2double(get(handles.text_th_max,'String'));

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
thresh     = handles.cell_prop(ind_cell).thresh;

%- Set new slider value only if value is within range
if value_edit > thresh.min  && value_edit < thresh.max
    slider_new = (value_edit-thresh.min)/thresh.diff;
    set(handles.slider_th_max,'Value',slider_new);
    slider_th_max_Callback(hObject, eventdata, handles)
else
    %sliderValue = get(handles.slider_th_max ,'Value');
    %value_edit  = sliderValue*thresh.diff+thresh.min;
    set(handles.text_th_max,'String',num2str(value_edit))     
end


%==========================================================================
%==== AVERAGE SPOTS
%==========================================================================


%=== Average spots 
function menu_avg_calc_Callback(hObject, eventdata, handles)

%set(handles.h_fishquant,'Pointer','watch');   % Change the mouse cursor to an hourglass

col_par  = handles.col_par;

%- Various flags
flag_output = 1;
flag_os     = 1;

%- Parameters needed for function call
image      = handles.image_struct.data;
pixel_size = handles.par_microscope.pixel_size;

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
spots_fit  = handles.cell_prop(ind_cell).spots_fit;
thresh     = handles.cell_prop(ind_cell).thresh;
ind_spots  = thresh.in == 1;
spots_fit  = spots_fit(ind_spots,:);


%=== Area to consider around the spots +/- in xy and z
%- Crop region for fit

dlgTitle = 'Parameters for averaging';

prompt_avg(1) = {'Size of region around pixels [XY]'};
prompt_avg(2) = {'Size of region around pixels [Z]'};
prompt_avg(3) = {'Factor for oversampling [XY]'};
prompt_avg(4) = {'Factor for oversampling [Z]'};
prompt_avg(5) = {'Background subtracted form each spot (Y=1, N=0)'};
prompt_avg(6) = {'[BGD] offset for averaging (to calc bgd image)'};

defaultValue_avg{1} = num2str(handles.average.crop.xy);
defaultValue_avg{2} = num2str(handles.average.crop.z);
defaultValue_avg{3} = num2str(handles.average.fact_os.xy);
defaultValue_avg{4} = num2str(handles.average.fact_os.z);
defaultValue_avg{5} = num2str(0);
defaultValue_avg{6} = num2str(0);

userValue = inputdlg(prompt_avg,dlgTitle,1,defaultValue_avg);

if( ~ isempty(userValue))
    handles.average.crop.xy    = str2double(userValue{1});
    handles.average.crop.z     = str2double(userValue{2}); 
    handles.average.fact_os.xy = str2double(userValue{3});
    handles.average.fact_os.z  = str2double(userValue{4});     
    flag_bgd                   = str2double(userValue{5});   
    offset                     = str2double(userValue{6});

    pixel_size_os.xy = pixel_size.xy / handles.average.fact_os.xy;
    pixel_size_os.z  = pixel_size.z  / handles.average.fact_os.z;

    par_spots = spots_fit(:,[col_par.pos_y col_par.pos_x col_par.pos_z col_par.bgd]);
    
    parameters.pixel_size  = pixel_size;
    parameters.par_spots   = par_spots;
    parameters.par_crop    = handles.average.crop;
    parameters.fact_os     = handles.average.fact_os;
    parameters.offset      = offset;
    parameters.flag_os     = flag_os;
    parameters.flag_output = flag_output;
    parameters.flag_bgd    = flag_bgd;
        
    
    [spot_avg spot_avg_os] = PSF_3D_average_spots_v8(image,[],parameters);

    %- Save values
    handles.cell_prop(ind_cell).par_microscope.pixel_size_os = pixel_size_os;
    handles.cell_prop(ind_cell).status_avg   = 1;
    handles.cell_prop(ind_cell).spot_avg     = spot_avg;
    handles.cell_prop(ind_cell).spot_avg_os  = spot_avg_os;
    handles.flag_avg_bgd = flag_bgd;
    guidata(hObject, handles);      % Update handles structure

    %- Enable fit of averaged spot, export, and construction
    FISH_QUANT_enable_controls_v8(handles)
end 


%=== Construct from averaged spots 
function menu_spot_avg_construct_Callback(hObject, eventdata, handles)

pixel_size   = handles.par_microscope.pixel_size;
flag_output  = 1;
ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
spot_avg_os  = handles.cell_prop(ind_cell).spot_avg_os;
fact_os      = handles.average.fact_os; 

%- Center of PSF can be placed at different sub-pixel locations
dlgTitle = 'Parameters for construction';

prompt(1) = {'Shift of center in X [+/- subpixels]'};
prompt(2) = {'Shift of center in Y [+/- subpixels]'};
prompt(3) = {'Shift of center in Z [+/- subpixels]'};
prompt(4) = {'Additional scaling [XY, >1]'};
prompt(5) = {'Additional scaling [Z, >1]'};

defaultValue{1} = num2str(0);
defaultValue{2} = num2str(0);
defaultValue{3} = num2str(0);
defaultValue{4} = num2str(1);
defaultValue{5} = num2str(1);

userValue = inputdlg(prompt,dlgTitle,1,defaultValue);

if( ~ isempty(userValue))
        
    shift.x = str2double(userValue{1});
    shift.y = str2double(userValue{2});
    shift.z = str2double(userValue{3}); 
        
    %- Correct if spots are larger than one sub-pixel-resolution
    shift.x = rem(shift.x,fact_os.xy);
    shift.y = rem(shift.y,fact_os.xy);
    shift.z = rem(shift.y,fact_os.z);
    
    spot_avg_os_shift = spot_avg_os(fact_os.xy-shift.y:end,fact_os.xy-shift.x:end,fact_os.z-shift.z:end); 
    
    %- Additional scaling
    scale_diff.xy = str2double(userValue{4});
    scale_diff.z  = str2double(userValue{5}); 
    fact_os_mod.xy = fact_os.xy * scale_diff.xy;
    fact_os_mod.z  = fact_os.z  * scale_diff.z;
    
    pixel_size_mod.xy = pixel_size.xy * fact_os_mod.xy;
    pixel_size_mod.z = pixel_size.z   * fact_os_mod.z;
    
    
    %- Dimension and subregion of averaged image
    [dim_os.Y dim_os.X dim_os.Z] = size(spot_avg_os_shift);
    dim_rec.Y = floor(dim_os.X/fact_os_mod.xy); 
    dim_rec.X = floor(dim_os.X/fact_os_mod.xy); 
    dim_rec.Z = floor(dim_os.Z/fact_os_mod.z); 
    
    spot_avg_os_sub = spot_avg_os_shift( 1:dim_rec.Y*fact_os_mod.xy, ...
                                         1:dim_rec.X*fact_os_mod.xy, ...
                                         1:dim_rec.Z*fact_os_mod.z);       
       
    %- Range of reconstructed image
    range_rec.X_nm = (1:dim_rec.X)*pixel_size_mod.xy;
    range_rec.Y_nm = (1:dim_rec.Y)*pixel_size_mod.xy;
    range_rec.Z_nm = (1:dim_rec.Z)*pixel_size_mod.z;
    
    
    handles.cell_prop(ind_cell).psf_construct = PSF_3D_reconstruct_from_os_v1(spot_avg_os_sub,range_rec,fact_os_mod,flag_output);
    handles.cell_prop(ind_cell).range_rec      = range_rec;
    
    %- Update value
    handles.cell_prop(ind_cell).status_avg_con = 1;
    FISH_QUANT_enable_controls_v8(handles)
    guidata(hObject, handles);
end


%==== Fit averaged spots
function handles = menu_avg_fit_Callback(hObject, eventdata, handles)

ind_cell = get(handles.pop_up_outline_sel_cell,'Value');

%set(handles.h_fishquant,'Pointer','watch');   % Change the mouse cursor to an hourglass

%- Parameters needed for function call
flag_crop      = 1;
flag_output    = 1;
img_PSF.data   = handles.cell_prop(ind_cell).spot_avg_os;
pixel_size_os  = handles.cell_prop(ind_cell).par_microscope.pixel_size_os;
par_microscope = handles.par_microscope;
PSF_exp        = handles.PSF_exp;

%- Crop region for fit
size_detect = handles.detect.region ;
        %= handles.detect.region;

%size_detect.xy = str2double(get(handles.text_detect_region_xy,'String'));
%size_detect.z  = str2double(get(handles.text_detect_region_z, 'String'));

fact_os.xy = handles.average.fact_os.xy; 
fact_os.z  = handles.average.fact_os.z;

par_crop_fit.xy = size_detect.xy * fact_os.xy;
par_crop_fit.z  = size_detect.z  * fact_os.z;

%- Fit with 3D Gaussian
parameters.pixel_size     = pixel_size_os;
parameters.par_microscope = par_microscope;
parameters.flags.crop     = flag_crop;
parameters.flags.output   = flag_output;
parameters.par_crop       = par_crop_fit;

[PSF_fit_os] = PSF_3D_Gauss_fit_v4(img_PSF,parameters);

%- Update experimental PSF settings
PSF_exp.sigmax_avg = PSF_fit_os.sigma_xy;
PSF_exp.sigmay_avg = PSF_fit_os.sigma_xy;
PSF_exp.sigmaz_avg = PSF_fit_os.sigma_z;
PSF_exp.amp_avg    = PSF_fit_os.amp;
PSF_exp.bgd_avg    = PSF_fit_os.bgd;

set(handles.text_psf_fit_sigmaX,'String', num2str(PSF_exp.sigmax_avg,'%.0f'));
set(handles.text_psf_fit_sigmaY,'String', num2str(PSF_exp.sigmay_avg,'%.0f'));
set(handles.text_psf_fit_sigmaZ,'String', num2str(PSF_exp.sigmaz_avg,'%.0f'));
set(handles.text_psf_fit_amp,'String',    num2str(PSF_exp.amp_avg,'%.0f'));
set(handles.text_psf_fit_bgd,'String',    num2str(PSF_exp.bgd_avg,'%.0f'));

set(handles.pop_up_select_psf,'Value',3);

%- Save values
handles.cell_prop(ind_cell).PSF_fit_os = PSF_fit_os;
handles.cell_prop(ind_cell).status_avg_fit = 1;
handles.PSF_exp = PSF_exp;

%set(handles.h_fishquant,'Pointer','arrow');   % Change the mouse cursor to an arrow
guidata(hObject, handles);      % Update handles structure


%=== Radial averaging of averaged spot 
function menu_spot_avg_radial_avg_Callback(hObject, eventdata, handles)

flag_output   = 1;

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');

spot_os_avg   = handles.cell_prop(ind_cell).spot_avg_os; 
pixel_size_os = handles.cell_prop(ind_cell).par_microscope.pixel_size_os;

%- If function has not been fit yet, perform fit 
if not(isfield(handles.cell_prop(ind_cell),'PSF_fit_os'))
    handles = menu_avg_fit_Callback(hObject, eventdata, handles);
end

PSF_fit_os = handles.cell_prop(ind_cell).PSF_fit_os;

%- Size of over-sampled image
[dim_os.Y dim_os.X dim_os.Z] = size(spot_os_avg);
range_os.X = 1:dim_os.X;
range_os.Y = 1:dim_os.Y;
range_os.Z = 1:dim_os.Z;

%- Center
center_os.x_pix = PSF_fit_os.mu_x_pix + PSF_fit_os.crop_off_x_pix;
center_os.y_pix = PSF_fit_os.mu_y_pix + PSF_fit_os.crop_off_y_pix;

%- Radial average
[psf_radial_bin_os range_os] = PSF_3D_calc_dist_rad_axial_v3(spot_os_avg,pixel_size_os,[],range_os,center_os,flag_output);

%- Save values
handles.cell_prop(ind_cell).status_avg_rad = 1;
handles.cell_prop(ind_cell).psf_radial_bin_os = psf_radial_bin_os;
handles.cell_prop(ind_cell).range_os          = range_os;
guidata(hObject, handles);      % Update handles structure

%- Update menu's
FISH_QUANT_enable_controls_v8(handles)


%=== Construct PSF from radial average
function menu_spot_rad_avg_construct_Callback(hObject, eventdata, handles)

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');

pixel_size        = handles.par_microscope.pixel_size;
flag_output       = 1;
psf_radial_bin_os = handles.cell_prop(ind_cell).psf_radial_bin_os;
range_os          = handles.cell_prop(ind_cell).range_os; 

%- Center of PSF can be placed at different sub-pixel locations

dlgTitle = 'Shift of center of PSF in pixel';

prompt(1) = {'X'};
prompt(2) = {'Y'};

defaultValue{1} = num2str(0);
defaultValue{2} = num2str(0);

userValue = inputdlg(prompt,dlgTitle,1,defaultValue);

if( ~ isempty(userValue))
    x_shift = str2double(userValue{1});
    y_shift = str2double(userValue{2});      

    %- Range of reconstructed image
    [dim_rec.Y dim_rec.X dim_rec.Z] = size(handles.cell_prop(ind_cell).spot_avg);
    dim_rec.Z = dim_rec.Z-2;           % Removal of buffer zone 

    range_rec.X_nm = (1:dim_rec.X)*pixel_size.xy;
    range_rec.Y_nm = (1:dim_rec.Y)*pixel_size.xy;
    range_rec.Z_nm = (1:dim_rec.Z)*pixel_size.z;

    %- Center of image 
    center_rec.x_nm = range_rec.X_nm(floor(dim_rec.X/2)+1) + x_shift*pixel_size.xy;
    center_rec.y_nm = range_rec.Y_nm(floor(dim_rec.Y/2)+1) + y_shift*pixel_size.xy;

    PSF_r_nm = range_os.r_bin_nm;

    %- Reconstruct
    psf_construct = PSF_3D_reconstruct_from_radial_v2(psf_radial_bin_os,PSF_r_nm,range_rec,center_rec,flag_output);

    %- Save values
    handles.cell_prop(ind_cell).psf_construct  = psf_construct;
    handles.cell_prop(ind_cell).range_rec      = range_rec;
    guidata(hObject, handles);      % Update handles structure
    
    %- Update value
    handles.cell_prop(ind_cell).status_avg_con = 1;
    FISH_QUANT_enable_controls_v8(handles)


end


%=== Fit reconstructed spot
function menu_spot_contruct_fit_Callback(hObject, eventdata, handles)

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');

img_PSF.data = handles.cell_prop(ind_cell).psf_construct;
flag_output  = 1;
flag_crop    = 1;
par_microscope = handles.par_microscope;
pixel_size     = par_microscope.pixel_size;

%- Regular sampling
parameters.pixel_size     = pixel_size;
parameters.par_microscope = par_microscope;
parameters.flags.crop     = flag_crop;
parameters.flags.output   = flag_output;
parameters.par_crop       = handles.detect.region;

PSF_3D_Gauss_fit_v4(img_PSF,parameters); 



%==========================================================================
%==== VISUALIZATION
%==========================================================================

%==========================================================================
%==== Functions for the different plots

%=== Image with position of detected spots
function plot_image(handles,axes_select)


col_par = handles.col_par;

flag_outline  = get(handles.checkbox_plot_outline, 'Value');
pixel_size    = handles.par_microscope.pixel_size;
ind_cell      = get(handles.pop_up_outline_sel_cell,'Value');

%- Might be called with no cell properties defined
if isempty(handles.cell_prop)
    spots_fit = {}; thresh = []; cell_prop = {};
else
    spots_fit  = handles.cell_prop(ind_cell).spots_fit;
    thresh     = handles.cell_prop(ind_cell).thresh;
    cell_prop  = handles.cell_prop; 
end

%- Select image-data to plot
str       = get(handles.pop_up_image_select,'String');
val       = get(handles.pop_up_image_select,'Value');
sel_image = str{val};

switch (sel_image)    
    case 'Raw image'  
        image = handles.image_struct.data;
    case 'Filtered image'  
        image = handles.image_struct.data_filtered;
end

%- Select spots to plot
str       = get(handles.pop_up_image_spots,'String');
val       = get(handles.pop_up_image_spots,'Value');
sel_spots = str{val};

flag_spots = 0;

switch sel_spots      
    case 'Detected spots',       flag_spots = 1;
    case 'Thresholded spots',    flag_spots = 2;
end
            
%- Calculate maximum projection of loaded image
img_plot = max(image,[],3); 

%- 1. Plot image
if isempty(axes_select)
    %figure
    %imshow(img_plot,[]);
    imtool(img_plot,[]);
else
    axes(axes_select);
    h = imshow(img_plot,[]);
    set(h, 'ButtonDownFcn', @axes_image_ButtonDownFcn)
end

title('Maximum projection of loaded image','FontSize',9);
colormap(hot)

%- 2. Plot-spots
if flag_spots
          
    if not(isempty(spots_fit))
        
        %- Select spots which will be shown
        if     flag_spots == 1
            ind_plot_in  = logical(thresh.all);
            ind_plot_out = not(thresh.all);
            ind_plot_out_man = not(thresh.all);
            
        elseif flag_spots == 2
            ind_plot_in      = logical(thresh.in_display);
            ind_plot_out     = not(thresh.in_display);
            ind_plot_out_man = thresh.in == -1 ; 
        end
        
        %- Plot spots        
        %  Add one pixel since image starts at one and detected spots at pixel
        hold on
            plot((spots_fit(ind_plot_in,col_par.pos_x)/pixel_size.xy  + 1) ,    (spots_fit(ind_plot_in,col_par.pos_y)/pixel_size.xy +1)     ,'og','MarkerSize',10);
            plot((spots_fit(ind_plot_out,col_par.pos_x)/pixel_size.xy + 1),     (spots_fit(ind_plot_out,col_par.pos_y)/pixel_size.xy +1)    ,'ob','MarkerSize',10);
            plot((spots_fit(ind_plot_out_man,col_par.pos_x)/pixel_size.xy + 1), (spots_fit(ind_plot_out_man,col_par.pos_y)/pixel_size.xy +1),'om','MarkerSize',10);
 
        hold off
        title(['Spots Detected ', num2str(length(ind_plot_in ))],'FontSize',9); 
        colormap(hot)
        freezeColors(gca)
        
        
        if sum(ind_plot_out) && sum(ind_plot_out_man) 
            legend('Selected Spots','Rejected Spots [auto]','Rejected Spots [man]');  
        elseif sum(ind_plot_out)  
            legend('Selected Spots','Rejected Spots [auto]');      
        else
            legend('Selected Spots');
        end 
    end   
end

%- 3. Plot outline if specified
if flag_outline
    
    %- Plot outline of cell and TS
    hold on
    if isfield(handles,'cell_prop')    
           
        if not(isempty(cell_prop))  
            for i = 1:size(cell_prop,2)
                x = cell_prop(i).x;
                y = cell_prop(i).y;
                plot([x,x(1)],[y,y(1)],'b','Linewidth', 2)     
            end

            %- Plot selected cell in different color
            ind_cell = get(handles.pop_up_outline_sel_cell,'Value');
            x = cell_prop(ind_cell).x;
            y = cell_prop(ind_cell).y;
            plot([x,x(1)],[y,y(1)],'y','Linewidth', 2)  

            %- TS
            pos_TS   = handles.cell_prop(ind_cell).pos_TS;   
            if not(isempty(pos_TS))  
                for i = 1:size(pos_TS,2)
                    x = pos_TS(i).x;
                    y = pos_TS(i).y;
                    plot([x,x(1)],[y,y(1)],'g','Linewidth', 2)  
            
                end                
            end            
        end        
    end
    hold off
    
end
    

%=== Plot-histogram of all values
function handles = plot_hist_all(handles,axes_select)

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
thresh     = handles.cell_prop(ind_cell).thresh;

if isempty(axes_select)
    figure
    hist(thresh.values(:),25); 
    v = axis;
    hold on
         plot([thresh.min_hist thresh.min_hist] , [0 1e5],'r');
         plot([thresh.max_hist thresh.max_hist] , [0 1e5],'r');
    hold off
    axis(v);
    colormap jet; 
    
% Handles for min and max line are returned for slider callback function    
else
    axes(axes_select); 
    hist(thresh.values(:),25); 
    h = findobj(axes_select);
    v = axis;
    hold on
         handles.h_hist_min = plot([thresh.min_hist thresh.min_hist] , [0 1e5],'r');
         handles.h_hist_max = plot([thresh.max_hist thresh.max_hist] , [0 1e5],'r');
    hold off
    axis(v);
    colormap jet;
    freezeColors;
    set(h,'ButtonDownFcn',@axes_histogram_all_ButtonDownFcn);   % Button-down function has to be set again
end
 
title(strcat('Total # of spots: ',sprintf('%d' ,length(thresh.in_old) )),'FontSize',9);    
    

%=== Plot-histogram of thresholded parameters
function handles = plot_hist_th(handles,axes_select)

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
thresh     = handles.cell_prop(ind_cell).thresh;

if isempty(axes_select)
    figure
    hist(thresh.values(handles.thresh.in_display),25); 
    v = axis;
    hold on
         plot([thresh.min_hist thresh.min_hist] , [0 1e5],'r');
         plot([thresh.max_hist thresh.max_hist] , [0 1e5],'r');
    hold off
    axis(v);
    colormap jet; 
    
% Handles for min and max line are returned for slider callback function    
else
    axes(axes_select); 
    hist(thresh.values(thresh.in_display),25); 
    h = findobj(axes_select);
    v = axis;
    hold on
         handles.h_hist_th_min = plot([thresh.min_hist thresh.min_hist] , [0 1e5],'r');
         handles.h_hist_th_max = plot([thresh.max_hist thresh.max_hist] , [0 1e5],'r');
    hold off
    axis(v);
    colormap jet;
    freezeColors;
    set(h,'ButtonDownFcn',@axes_histogram_th_ButtonDownFcn);   % Button-down function has to be set again
end
    
title(strcat('Selected # of spots ',sprintf('%d' ,sum(thresh.in_display) )),'FontSize',9);     


%=== Projection in xy
function plot_proj_xy(handles,axes_select)

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
thresh     = handles.cell_prop(ind_cell).thresh;
spots_proj = handles.cell_prop(ind_cell).spots_proj;

%- Plot only if data is present
if sum(spots_proj.xy(:))
    if isempty(axes_select)
        figure
        montage(spots_proj.xy(:,:,:,thresh.in == 1),'DisplayRange', []);    
        set(gcf,'Position', [300   300   500   400])       
        set(gca,'Units','normalized')
        set(gca,'Position', [0.1   0.1   0.8   0.8])
        colormap(jet);
    else
        axes(axes_select);
        h = montage(spots_proj.xy(:,:,:,thresh.in_display),'DisplayRange', []);
        set(h,'ButtonDownFcn',@axes_proj_xy_ButtonDownFcn);   % Button-down function has to be set again
        colormap(jet);
        freezeColors;
    end
    title('Max-projection XY','FontSize',9)  
else
   set(handles.axes_proj_xy,'Visible','off');     
end
  

%=== Projection in xz
function plot_proj_xz(handles,axes_select)

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
thresh     = handles.cell_prop(ind_cell).thresh;
spots_proj = handles.cell_prop(ind_cell).spots_proj;

%- Plot only if data is present
if sum(spots_proj.xz(:))  
    if isempty(axes_select)
        figure
        montage(spots_proj.xz(:,:,:,thresh.in == 1),'DisplayRange', []);    
        set(gcf,'Position', [300   300   500   400])       
        set(gca,'Units','normalized')
        set(gca,'Position', [0.1   0.1   0.8   0.8])
        colormap(jet);
    else
        axes(axes_select);
        h = montage(spots_proj.xz(:,:,:,thresh.in_display),'DisplayRange', []);
        set(h,'ButtonDownFcn',@axes_proj_xz_ButtonDownFcn);   % Button-down function has to be set again
        colormap(jet);
        freezeColors;
    end
    title('Max-projection XZ','FontSize',9) 
else
   set(handles.axes_proj_xz,'Visible','off');     
end
    

%=== Residuals in xy
function plot_resid_xy(handles,axes_select)

ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
thresh     = handles.cell_prop(ind_cell).thresh;
spots_proj = handles.cell_prop(ind_cell).spots_proj;

%- Plot only if data is present
if isfield(spots_proj,'res_xy')
    if sum(spots_proj.res_xy(:))     
        if isempty(axes_select)
            figure
            montage(spots_proj.res_xy(:,:,:,thresh.in == 1),'DisplayRange', []);    
            set(gcf,'Position', [300   300   500   400])       
            set(gca,'Units','normalized')
            set(gca,'Position', [0.1   0.1   0.8   0.8])
            colormap(jet);    
        else
            axes(axes_select);
            h_xy = montage(spots_proj.res_xy(:,:,:,thresh.in_display),'DisplayRange', []);
            set(h_xy,'ButtonDownFcn',@axes_resid_xy_ButtonDownFcn);   % Button-down function has to be set again
            colormap(jet);
            freezeColors;
        end
        title('RESID: Max-proj XY','FontSize',9)
    else
        set(handles.axes_resid_xy,'Visible','off');       
    end    
else
   set(handles.axes_resid_xy,'Visible','off');     
end
   



%==========================================================================
%===  Functions for double clicks on plots 

%=== 2D plot
function axes_image_ButtonDownFcn(hObject, eventdata, handles)
sel_type = get(gcf,'selectiontype');    % Normal for single click, Open for double click
   
if strcmp(sel_type,'open')
    handles = guidata(hObject);        % Appears that handles are not always input parameter for function call
    plot_image(handles,[]);
end


%=== Projections in xy
function axes_proj_xy_ButtonDownFcn(hObject, eventdata, handles)
sel_type = get(gcf,'selectiontype');    % Normal for single click, Open for double click
    
if strcmp(sel_type,'open')
    handles = guidata(hObject);        % Appears that handles are not always input parameter for function call
    plot_proj_xy(handles,[]);
end


%=== Projections in xy
function axes_proj_xz_ButtonDownFcn(hObject, eventdata, handles)
sel_type = get(gcf,'selectiontype');    % Normal for single click, Open for double click
    
if strcmp(sel_type,'open')
    handles = guidata(hObject);        % Appears that handles are not always input parameter for function call
    plot_proj_xz(handles,[]);
end


%=== Residuals
function axes_resid_xy_ButtonDownFcn(hObject, eventdata, handles)
sel_type = get(gcf,'selectiontype');    % Normal for single click, Open for double click
    
if strcmp(sel_type,'open')
    handles = guidata(hObject);        % Appears that handles are not always input parameter for function call
    plot_resid_xy(handles,[]);
end


%=== Histogram
function axes_histogram_all_ButtonDownFcn(hObject, eventdata, handles)
sel_type = get(gcf,'selectiontype');    % Normal for single click, Open for double click
    
if strcmp(sel_type,'open')
    handles = guidata(hObject);        % Appears that handles are not always input parameter for function call
    plot_hist_all(handles,[]);
end


%=== Thresholded histogram
function axes_histogram_th_ButtonDownFcn(hObject, eventdata, handles)
sel_type = get(gcf,'selectiontype');    % Normal for single click, Open for double click
    
if strcmp(sel_type,'open')
    handles = guidata(hObject);        % Appears that handles are not always input parameter for function call
    plot_hist_th(handles,[]);
end


%==========================================================================
%===  Various calls of the plot functions


%==== Plot function within GUI
function button_plot_image_Callback(hObject, eventdata, handles)
plot_image(handles,handles.axes_image);


%=== Show figures in Matlab
function button_visualize_matlab_Callback(hObject, eventdata, handles)
FISH_QUANT_spots('HandlesMainGui',handles);  

%=== Menu: averaged spot
function Menu_averaged_spot_Callback(hObject, eventdata, handles)
if handles.flag_JAVA_init == 0
    set(handles.menu_spot_avg_imagej,'Enable', 'off')
end


%=== Menu: show averaged spot with normal sampling in ImageJ
function menu_spot_avg_imagej_ns_Callback(hObject, eventdata, handles)
MIJ_start(hObject, eventdata, handles)
ind_cell  = get(handles.pop_up_outline_sel_cell,'Value');
MIJ.createImage('Matlab: PSF with normal sampling', uint16(handles.cell_prop(ind_cell).spot_avg),1);    


%=== Menu: show averaged spot with over-sampling in ImageJ
function menu_spot_avg_imagej_os_Callback(hObject, eventdata, handles)
MIJ_start(hObject, eventdata, handles)
ind_cell  = get(handles.pop_up_outline_sel_cell,'Value');
MIJ.createImage('Matlab: over-sampled PSF', uint16(handles.cell_prop(ind_cell).spot_avg_os),1);    


%=== Menu: show radial averaged curve in ImageJ
function menu_spot_avg_imagej_radial_Callback(hObject, eventdata, handles)
MIJ_start(hObject, eventdata, handles)
ind_cell  = get(handles.pop_up_outline_sel_cell,'Value');
MIJ.createImage('Matlab: radial PSF', uint16(handles.cell_prop(ind_cell).psf_radial_bin_os),1);    


%=== Menu: show constructed spot in ImageJ
function menu_spot_avg_imagej_const_ns_Callback(hObject, eventdata, handles)
MIJ_start(hObject, eventdata, handles)
ind_cell  = get(handles.pop_up_outline_sel_cell,'Value');
MIJ.createImage('Matlab: constructed PSF', uint16(handles.cell_prop(ind_cell).psf_construct),1);    


%= Function to start MIJ
function MIJ_start(hObject, eventdata, handles)
if isfield(handles,'flag_MIJ')
    if handles.flag_MIJ == 0
       MIJ.start(fullfile(handles.path_imagej,'ImageJ'))                          % Start MIJ/ImageJ by running the Matlab command: MIJ.start("imagej-path")
       handles.flag_MIJ = 1;       
    end
else
   MIJ.start(fullfile(handles.path_imagej,'ImageJ'))                          % Start MIJ/ImageJ by running the Matlab command: MIJ.start("imagej-path")
   handles.flag_MIJ = 1; 
end
guidata(hObject, handles);


%==========================================================================
%==== Tools
%==========================================================================


%== Outline editor
function menu_outline_Callback(hObject, eventdata, handles)
FISH_QUANT_outline('par_microscope',handles.par_microscope);


%== Batch mode
function menu_batch_Callback(hObject, eventdata, handles)
FISH_QUANT_batch %('HandleMainGui',handles);


%== Spot inspector
function menu_spot_inspector_Callback(hObject, eventdata, handles)
FISH_QUANT_spots


%== Spot inspector
function menu_TxSite_quant_Callback(hObject, eventdata, handles)
FISH_QUANT_TxSite('HandlesMainGui',handles);

 
%==========================================================================
%==== Various Features
%==========================================================================

%== Parallel computing
function checkbox_parallel_computing_Callback(hObject, eventdata, handles)

flag_parallel = get(handles.checkbox_parallel_computing,'Value');

if exist('matlabpool')

    %- Parallel computing - open MATLAB session for parallel computation 
    if flag_parallel == 1    
        isOpen = matlabpool('size') > 0;
        if (isOpen==0)
            
            %- Update status
            set(handles.h_fishquant,'Pointer','watch');
            status_text = {' ';'== STARTING matlabpool for parallel computing ... please wait ... '};
            status_update(hObject, eventdata, handles,status_text);

            matlabpool open;

            %- Update status
            status_text = {' ';'    ... STARTED'};
            status_update(hObject, eventdata, handles,status_text);        
            set(handles.h_fishquant,'Pointer','arrow');
        end

    %- Parallel computing - close MATLAB session for parallel computation     
    else
        isOpen = matlabpool('size') > 0;
        if (isOpen==1)
            
            
            %- Update status
            set(handles.h_fishquant,'Pointer','watch');
            status_text = {' ';'== STOPPING matlabpool for parallel computing ... please wait ... '};
            status_update(hObject, eventdata, handles,status_text);

            matlabpool close;

            %- Update status
            status_text = {' ';'    ... STOPPED'};
            status_update(hObject, eventdata, handles,status_text);
            set(handles.h_fishquant,'Pointer','arrow');
        end
    end
    
else
    warndlg('Parallel toolbox not available','FISH_QUANT')
    set(handles.checkbox_parallel_computing,'Value',0);
end


%== Change fit mode
function checkbox_fit_fixed_width_Callback(hObject, eventdata, handles)
val_check = get(handles.checkbox_fit_fixed_width,'Value');
handles.flag_fit = val_check;
guidata(hObject, handles);


%== Close all windows except GUI
function button_close_all_windows_Callback(hObject, eventdata, handles)

%- Find all figure handles and delete the one of the GUI
fh    =  findall(0,'Type','Figure');
fh(fh == handles.h_fishquant) = [];

if evalin('base', 'exist(''h_batch'',''var'')');
    h_batch = evalin('base', 'h_batch');
    fh(fh == h_batch) = [];
end

if evalin('base', 'exist(''h_outline'',''var'')');
    h_outline = evalin('base', 'h_outline');
    fh(fh == h_outline) = [];
end

if evalin('base', 'exist(''h_spots'',''var'')');
    h_spots = evalin('base', 'h_spots');
    fh(fh == h_spots) = [];
end

if evalin('base', 'exist(''h_predetect'',''var'')');
    h_spots = evalin('base', 'h_predetect');
    fh(fh == h_spots) = [];
end

if evalin('base', 'exist(''h_TxSite'',''var'')');
    h_spots = evalin('base', 'h_TxSite');
    fh(fh == h_spots) = [];
end

close(fh)


%== Reset GUI to starting values
function menu_GUI_reset_Callback(hObject, eventdata, handles)
button = questdlg('Are you sure that you want to reset the GUI?','RESET GUI','Yes','No','No');

if strcmp(button,'Yes')    
    FISH_QUANT_OpeningFcn(hObject, eventdata, handles);    
end


%=== Pop-up to select which estimates should be shown
function pop_up_select_psf_Callback(hObject, eventdata, handles)

PSF_exp = handles.PSF_exp;      
    
%- Thresholding parameter
str = get(handles.pop_up_select_psf,'String');
val = get(handles.pop_up_select_psf,'Value');
popup_parameter = str{val};


switch (popup_parameter)
    
    case 'All spots'           

    set(handles.text_psf_fit_sigmaX,'String', num2str(PSF_exp.sigmax_all,'%.0f'));
    set(handles.text_psf_fit_sigmaY,'String', num2str(PSF_exp.sigmay_all,'%.0f'));
    set(handles.text_psf_fit_sigmaZ,'String', num2str(PSF_exp.sigmaz_all,'%.0f'));
    set(handles.text_psf_fit_amp,'String',    num2str(PSF_exp.amp_all,'%.0f'));
    set(handles.text_psf_fit_bgd,'String',    num2str(PSF_exp.bgd_all,'%.0f'));

    disp(' ')
    disp('FIT TO 3D GAUSSIAN: avg of ALL spots ')
    disp(['Sigma (xy): ', num2str(round(PSF_exp.sigmax_all)), ' +/- ', num2str(round(PSF_exp.sigmax_all_std))])
    disp(['Sigma (z) : ', num2str(round(PSF_exp.sigmaz_all)), ' +/- ', num2str(round(PSF_exp.sigmaz_all_std))])
    disp(['Amplitude : ', num2str(round(PSF_exp.amp_all)), ' +/- ', num2str(round(PSF_exp.amp_all_std))])
    disp(['BGD       : ', num2str(round(PSF_exp.bgd_all)), ' +/- ', num2str(round(PSF_exp.bgd_all_std))])
    disp(' ')
    
        
    case 'Thresholded spots'
                
    set(handles.text_psf_fit_sigmaX,'String', num2str(PSF_exp.sigmax_th,'%.0f'));
    set(handles.text_psf_fit_sigmaY,'String', num2str(PSF_exp.sigmay_th,'%.0f'));
    set(handles.text_psf_fit_sigmaZ,'String', num2str(PSF_exp.sigmaz_th,'%.0f'));
    set(handles.text_psf_fit_amp,'String',    num2str(PSF_exp.amp_th,'%.0f'));
    set(handles.text_psf_fit_bgd,'String',    num2str(PSF_exp.bgd_th,'%.0f'));  
    
    disp(' ')
    disp('FIT TO 3D GAUSSIAN: avg of ALL spots ')
    disp(['Sigma (xy): ', num2str(round(PSF_exp.sigmax_th)), ' +/- ', num2str(round(PSF_exp.sigmax_th_std))])
    disp(['Sigma (z) : ', num2str(round(PSF_exp.sigmaz_th)), ' +/- ', num2str(round(PSF_exp.sigmaz_th_std))])
    disp(['Amplitude : ', num2str(round(PSF_exp.amp_th)), ' +/- ', num2str(round(PSF_exp.amp_th_std))])
    disp(['BGD       : ', num2str(round(PSF_exp.bgd_th)), ' +/- ', num2str(round(PSF_exp.bgd_th_std))])
    disp(' ')
      
                
    case 'Averaged spots'
        
    set(handles.text_psf_fit_sigmaX,'String', num2str(PSF_exp.sigmax_avg,'%.0f'));
    set(handles.text_psf_fit_sigmaY,'String', num2str(PSF_exp.sigmay_avg,'%.0f'));
    set(handles.text_psf_fit_sigmaZ,'String', num2str(PSF_exp.sigmaz_avg,'%.0f'));
    set(handles.text_psf_fit_amp,'String',    num2str(PSF_exp.amp_avg,'%.0f'));
    set(handles.text_psf_fit_bgd,'String',    num2str(PSF_exp.bgd_avg,'%.0f'));   
    
    case 'User defined'
        
    set(handles.text_psf_fit_sigmaX,'String', num2str(handles.par_fit.sigma_XY_fixed));
    set(handles.text_psf_fit_sigmaY,'String', num2str(handles.par_fit.sigma_XY_fixed));
    set(handles.text_psf_fit_sigmaZ,'String', num2str(handles.par_fit.sigma_Z_fixed));
    set(handles.text_psf_fit_amp,'String',    '');
    set(handles.text_psf_fit_bgd,'String',    ''); 
    
    
    
        
end


%=== Manually change PSF-X
function text_psf_fit_sigmaX_Callback(hObject, eventdata, handles)
handles.par_fit.sigma_XY_fixed = str2double(get(handles.text_psf_fit_sigmaX,'String'));
set(handles.text_psf_fit_sigmaY,'String',handles.par_fit.sigma_XY_fixed);
guidata(hObject, handles);

set(handles.pop_up_select_psf,'Value',4);
FISH_QUANT_enable_controls_v8(handles)


%=== Manually change PSF-Y
function text_psf_fit_sigmaY_Callback(hObject, eventdata, handles)
handles.par_fit.sigma_XY_fixed = str2double(get(handles.text_psf_fit_sigmaY,'String'));
set(handles.text_psf_fit_sigmaX,'String',handles.par_fit.sigma_XY_fixed);
guidata(hObject, handles);

set(handles.pop_up_select_psf,'Value',4);
FISH_QUANT_enable_controls_v8(handles)


%=== Manually change PSF-Z
function text_psf_fit_sigmaZ_Callback(hObject, eventdata, handles)
handles.par_fit.sigma_Z_fixed = str2double(get(handles.text_psf_fit_sigmaZ,'String'));
guidata(hObject, handles);

set(handles.pop_up_select_psf,'Value',4);
FISH_QUANT_enable_controls_v8(handles)


%== Update status
function status_update(hObject, eventdata, handles,status_text)
status_old = get(handles.list_box_status,'String');
status_new = [status_old;status_text];
set(handles.list_box_status,'String',status_new)
set(handles.list_box_status,'ListboxTop',round(size(status_new,1)))
drawnow
guidata(hObject, handles); 


%== Close GUI
function h_fishquant_CloseRequestFcn(hObject, eventdata, handles)
button = questdlg('Are you sure that you want to close the GUI?','RESET GUI','Yes','No','No');

if strcmp(button,'Yes')    
   delete(hObject);   
end


%== CellC - cell counter
function menu_cellC_Callback(hObject, eventdata, handles)
cellc

%==========================================================================
%==== MENU
%==========================================================================

%== MENU
function menu_loadSave_Callback(hObject, eventdata, handles)
if isfield(handles,'pos_cell')
    set(handles.menu_save_outline,'Enable','on')
end

%- Export spots
if not(isempty(handles.cell_prop))
    ind_cell  = get(handles.pop_up_outline_sel_cell,'Value');
    spots_fit = handles.cell_prop(ind_cell).spots_fit;
    if not(isempty(spots_fit))
        set(handles.menu_save_spots,'Enable','on')
    else
        set(handles.menu_save_spots,'Enable','off')   
    end
else
    set(handles.menu_save_spots,'Enable','off')
end


%== Display help file
function menu_help_show_help_file_Callback(hObject, eventdata, handles)
file_name_pdf = ['FISH_QUANT_', handles.version,'.pdf'];
open(file_name_pdf)


% ===== CREATE FUNCTIONS and CALL BACKS with no additional code
function menu_spot_avg_imagej_Callback(hObject, eventdata, handles)
    
function text_psf_theo_xy_Callback(hObject, eventdata, handles)

function text_psf_theo_z_Callback(hObject, eventdata, handles)

function pop_up_exp_default_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_psf_theo_xy_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
%    set(hObject,'BackgroundColor','white');
end

function text_psf_theo_z_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
 %   set(hObject,'BackgroundColor','white');
end

function text_TS_size_xy_Callback(hObject, eventdata, handles)

function text_TS_size_xy_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_TS_size_z_Callback(hObject, eventdata, handles)

function text_TS_size_z_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function pop_up_detect_quality_Callback(hObject, eventdata, handles)

function pop_up_detect_quality_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_detect_region_xy_Callback(hObject, eventdata, handles)

function text_detect_region_xy_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_detect_region_z_Callback(hObject, eventdata, handles)

function text_detect_region_z_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_kernel_bgd_xy_Callback(hObject, eventdata, handles)

function text_kernel_bgd_xy_CreateFcn(hObject, eventdata, handles)
 if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_kernel_factor_bgd_Callback(hObject, eventdata, handles)

function text_kernel_factor_bgd_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_kernel_filter_xy_Callback(hObject, eventdata, handles)

function text_kernel_filter_xy_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_kernel_factor_filter_Callback(hObject, eventdata, handles)

function text_kernel_factor_filter_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_th_min_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_th_max_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function slider_th_min_CreateFcn(hObject, eventdata, handles)
if isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor',[.9 .9 .9]);
end

function slider_th_max_CreateFcn(hObject, eventdata, handles)
if isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor',[.9 .9 .9]);
end

function checkbox_th_lock_max_Callback(hObject, eventdata, handles)

function pop_up_imagej_style_Callback(hObject, eventdata, handles)

function pop_up_imagej_style_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function pop_up_imagej_1st_Callback(hObject, eventdata, handles)

function pop_up_imagej_1st_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function pop_up_imagej_2nd_Callback(hObject, eventdata, handles)

function pop_up_imagej_2nd_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function button_save_Callback(hObject, eventdata, handles)

function pop_up_save_Callback(hObject, eventdata, handles)

function pop_up_save_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function pop_up_threshold_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function menu_help_Callback(hObject, eventdata, handles)

function menu_load_Callback(hObject, eventdata, handles)

function menu_save_Callback(hObject, eventdata, handles)

function edit15_Callback(hObject, eventdata, handles)

function text_psf_fit_sigmaX_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_psf_fit_sigmaY_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_psf_fit_sigmaZ_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_psf_fit_bgd_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function menu_test_settings_Callback(hObject, eventdata, handles)

function test_settings_load_image_Callback(hObject, eventdata, handles)

function test_settings_load_outline_Callback(hObject, eventdata, handles)

function test_settings_run_Callback(hObject, eventdata, handles)

function edit20_Callback(hObject, eventdata, handles)

function edit20_CreateFcn(hObject, eventdata, handles)

if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_detection_threshold_Callback(hObject, eventdata, handles)

function text_detection_threshold_CreateFcn(hObject, eventdata, handles)

if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function pop_up_image_select_Callback(hObject, eventdata, handles)

function pop_up_image_select_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_th_min_2_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function pop_up_image_spots_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_th_low_Callback(hObject, eventdata, handles)

function text_psf_fit_amp_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_avg_size_xy_Callback(hObject, eventdata, handles)

function text_avg_size_xy_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_avg_size_z_Callback(hObject, eventdata, handles)

function text_avg_size_z_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_fact_os_xy_Callback(hObject, eventdata, handles)

function text_fact_os_xy_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_fact_os_z_Callback(hObject, eventdata, handles)

function text_fact_os_z_CreateFcn(hObject, eventdata, handles)

if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function pop_up_select_psf_CreateFcn(hObject, eventdata, handles)

if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function Menu_export_handles_Callback(hObject, eventdata, handles)

function pop_up_outline_sel_cell_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function checkbox_plot_outline_Callback(hObject, eventdata, handles)

function pop_up_image_spots_Callback(hObject, eventdata, handles)

function button_outline_clear_Callback(hObject, eventdata, handles)

function menu_tools_Callback(hObject, eventdata, handles)

function list_box_status_Callback(hObject, eventdata, handles)

function list_box_status_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function checkbox_proc_all_cells_Callback(hObject, eventdata, handles)

function menu_TS_Callback(hObject, eventdata, handles)


% --- Executes on button press in pushbutton30.
function pushbutton30_Callback(hObject, eventdata, handles)


% hObject    handle to menu_cellC (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)
