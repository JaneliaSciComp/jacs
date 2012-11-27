function varargout = FISH_QUANT_batch(varargin)
% FISH_QUANT_BATCH M-file for FISH_QUANT_batch.fig

% Last Modified by GUIDE v2.5 09-Nov-2011 12:29:21

% Begin initialization code - DO NOT EDIT
gui_Singleton = 1;
gui_State = struct('gui_Name',       mfilename, ...
                   'gui_Singleton',  gui_Singleton, ...
                   'gui_OpeningFcn', @FISH_QUANT_batch_OpeningFcn, ...
                   'gui_OutputFcn',  @FISH_QUANT_batch_OutputFcn, ...
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


% --- Executes just before FISH_QUANT_batch is made visible.
function FISH_QUANT_batch_OpeningFcn(hObject, eventdata, handles, varargin)

%- Set font-size to 10
%  On windows are set back to 8 when the .fig is openend
h_font_8 = findobj(handles.h_gui_batch,'FontSize',8);
set(h_font_8,'FontSize',10)


%- Get installation directory of FISH-QUANT and initiate 
p = mfilename('fullpath');        
handles.FQ_path = fileparts(p); 
handles = FISH_QUANT_start_up_v2(handles);

%- Some other parameters
handles.status_setting = 0;
handles.status_files   = 0;
handles.cell_summary   = [];
handles.PSF            = [];
handles.status_fit     = 0;
handles.flag_fit       = 0;
handles.status_outline_unique_loaded = 0;
handles.status_outline_unique_enable = 0;

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


%- TxSite quantification
handles.FLAG_auto_detect = 0;

%- Some parameters
handles.status_AMP   = 0;
handles.status_settings_TS  = 0;
handles.status_QUANT = 0;
handles.status_settings_TS_proc = 0;

%=== Options for TxSite quantification
handles.parameters_quant = FQ_TS_settings_init_v1;

%- Default file-names
handles.file_name_suffix_spots  = ['_spots_', datestr(date,'yymmdd'), '.txt'];
handles.file_name_summary       = ['FISH-QUANT__batch_summary_mature_', datestr(date,'yymmdd'), '.txt'];
handles.file_name_summary_TS    = ['FISH-QUANT__batch_summary_nascent_', datestr(date,'yymmdd'), '.txt'];
handles.file_name_settings_save = ['FISH-QUANT__batch_settings_', datestr(date,'yymmdd'), '.txt'];

handles.settings_save.N_ident = 4;

%- Export figure handle to workspace - will be used in Close All button of
% main Interface
assignin('base','h_batch',handles.h_gui_batch)

%- Update everything and save
controls_enable(hObject, eventdata, handles)
handles.output = hObject;
guidata(hObject, handles);

% =========================================================================
% Enable function
% =========================================================================

function controls_enable(hObject, eventdata, handles)

str_list = get(handles.listbox_files,'String');

if not(isempty(str_list))
    handles.status_files = 1;
    set(handles.text_status_files,'String','Files listed')
    set(handles.text_status_files,'ForegroundColor','g')
else
    handles.status_files = 0;
    set(handles.text_status_files,'String','No files')
    set(handles.text_status_files,'ForegroundColor','r')
end

if handles.status_files 
   set(handles.button_files_delete,'Enable','on'); 
   set(handles.button_files_delete_all,'Enable','on'); 
else
   set(handles.button_files_delete,'Enable','off');
   set(handles.button_files_delete_all,'Enable','off'); 
end


if handles.status_setting && handles.status_files 
   set(handles.button_process,'Enable','on');     
else
   set(handles.button_process,'Enable','off'); 
end

if handles.status_setting && handles.status_files 
   set(handles.button_process,'Enable','on');     
else
   set(handles.button_process,'Enable','off'); 
end

if not(isempty(handles.cell_summary))
   set(handles.menu_avg_calc,'Enable','on');     
else
   set(handles.menu_avg_calc,'Enable','off'); 
end

if isfield(handles,'spot_avg')
    set(handles.menu_avg_fit ,'Enable','on')
    set(handles.menu_imagej_ns ,'Enable','on')   
    set(handles.menu_imagej_os ,'Enable','on')
    set(handles.menu_construct_os ,'Enable','on') 

else
    set(handles.menu_avg_fit ,'Enable','off')
    set(handles.menu_imagej_ns ,'Enable','off')
    set(handles.menu_imagej_os ,'Enable','off')
    set(handles.menu_construct_os ,'Enable','off')
end


if isfield(handles,'psf_rec')
    set(handles.menu_avg_ij_constructed ,'Enable','on') 
else

    set(handles.menu_avg_ij_constructed ,'Enable','off')
end

% ==== After fit is done - thresholding and saving
if handles.status_fit

    %- Enable thresholding
    set(handles.button_threshold,'Enable','on')
    set(handles.pop_up_threshold,'Enable','on')
    set(handles.slider_th_min,'Enable','on')
    set(handles.slider_th_max,'Enable','on')
    set(handles.text_th_min,'Enable','on')
    set(handles.text_th_max,'Enable','on')
    set(handles.checkbox_th_lock,'Enable','on')
    set(handles.button_th_unlock_all,'Enable','on')

else

    %- Enable thresholding
    set(handles.button_threshold,'Enable','off')
    set(handles.pop_up_threshold,'Enable','off')
    set(handles.slider_th_min,'Enable','off')
    set(handles.slider_th_max,'Enable','off')
    set(handles.text_th_min,'Enable','off')
    set(handles.text_th_max,'Enable','off')
    set(handles.checkbox_th_lock,'Enable','off')
    set(handles.button_th_unlock_all,'Enable','off')

end


% === Enable unique outline processing
if handles.status_outline_unique_loaded
    set(handles.menu_load_outline_enable ,'Enable','on') 
else

    set(handles.menu_load_outline_enable ,'Enable','off')
end


% === TxSite quantification: analyze settings
if handles.status_setting && handles.status_settings_TS 
   set(handles.button_analyze_settings_TxSite,'Enable','on');     
else
   set(handles.button_analyze_settings_TxSite,'Enable','off'); 
end

% === TxSite settings are processed: amplitude distribution 
if handles.status_settings_TS_proc
    set(handles.button_PSF_amp,'Enable','on');  
else
    set(handles.button_PSF_amp,'Enable','off');  
end


%== Auto-detection of transcription site
if handles.FLAG_auto_detect
    set(handles.status_autodetect,'Value',1);
    set(handles.text_th_auto_detect,'String',num2str(handles.parameters_auto_detect.int_th));

else
    set(handles.status_autodetect,'Value',0);
    set(handles.text_th_auto_detect,'String',' ');
end

%== Transcription site background
if handles.parameters_quant.flags.bgd_local == 0 
    set(handles.button_TS_bgd,'Value',1);
    set(handles.txt_TS_bgd,'String',num2str(handles.parameters_quant.BGD.amp));

else
    set(handles.button_TS_bgd,'Value',0);
    set(handles.txt_TS_bgd,'String',' ');
end

%== Analyze TxSite
if handles.status_AMP && handles.status_settings_TS_proc && not(isempty(str_list))
    set(handles.button_process_TxSite,'Enable','on');  
else
    set(handles.button_process_TxSite,'Enable','off');  
end



% =========================================================================
% FILES
% =========================================================================


%== Load settings
function load_settings_Callback(hObject, eventdata, handles)
[file_name_settings,path_name_settings] = uigetfile({'*.txt'},'Select file with settings');

if file_name_settings ~= 0
    
    
    %- Set all threshold locks to zero - the ones which are locked will be changed to one 
    handles.thresh_all.sigmaxy.lock = 0;
    handles.thresh_all.sigmaz.lock  = 0;
    handles.thresh_all.amp.lock     = 0;
    handles.thresh_all.bgd.lock     = 0;
    handles.thresh_all.pos_z.lock     = 0;
    
    %- Load settings
    handles = FISH_QUANT_load_settings_v3(fullfile(path_name_settings,file_name_settings),handles);   
    handles.file_name_settings = file_name_settings;
    handles.path_name_settings = path_name_settings;    
    
    %- Update when fit with fixed parameters is selected
    if handles.flag_fit == 0;
        set(handles.checkbox_fixed_width,'Value',0);    
    else
        set(handles.checkbox_fixed_width,'Value',1);  

        set(handles.text_psf_fit_sigmaX,'String', num2str(handles.par_fit.sigma_XY_fixed));
        set(handles.text_psf_fit_sigmaZ,'String', num2str(handles.par_fit.sigma_Z_fixed));
    end       
    
    %- Save region for fit in separate variable --> otherwise complications
    % occur when fitting averaged spot!    
    handles.fit.region = handles.detect.region;
    
    %- Update the ones that had a locked threshold
    names_all = fieldnames(handles.thresh_all);

    N_names   = size(names_all,1);

    for i_name = 1:N_names
        par_name   = char(names_all{i_name});
        par_fields = getfield(handles.thresh_all,par_name);
        
        if isfield(par_name, 'lock')
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
                            handles.thresh_all.bgd.min_hist = par_fields.min_hist;                 
                            handles.thresh_all.bgd.max_hist = par_fields.max_hist;  

                        otherwise
                            warndlg('Thresholding parameter not defined.','load_settings');
                    end
            end
        end
    end
    
    
    %- Update status
    set(handles.text_status_settings,'String','Settings defined')
    set(handles.text_status_settings,'ForegroundColor','g')
    handles.status_setting = 1;
            
    status_update(hObject, eventdata, handles,{'  ';'## Settings loaded'});     
    
    %- Save results
    guidata(hObject, handles);
    controls_enable(hObject, eventdata, handles)
end


%== Add files
function button_files_add_Callback(hObject, eventdata, handles)

[file_name_outline,path_name_image] = uigetfile({'*.txt';'*.tif';'*.stk'},'Select file with outline definition or image files','MultiSelect', 'on');

if ~iscell(file_name_outline)
    dum =file_name_outline; 
    file_name_outline = {dum};
end
    
if file_name_outline{1} ~= 0 
    
    str_list_old = get(handles.listbox_files,'String');
    
    if isempty(str_list_old)
        str_list_new = file_name_outline';
    else
        str_list_new = [str_list_old;file_name_outline'];
    end
    
    set(handles.listbox_files,'String',str_list_new);
    handles.path_name_image = path_name_image;
    
    %- Update status
    controls_enable(hObject, eventdata, handles)    
    status_text = { ' ';'## Outline definition files specified'; [num2str(size(str_list_new,1)) ' files will be processed']};
    status_update(hObject, eventdata, handles,status_text);  
    
    %- Save results
    guidata(hObject, handles); 

end


%== Delete selected files
function button_files_delete_Callback(hObject, eventdata, handles)

str_list = get(handles.listbox_files,'String');

if not(isempty(str_list))

    %- Ask user to confirm choice
    choice = questdlg('Do you really want to remove this file?', 'FISH-QUANT', 'Yes','No','No');

    if strcmp(choice,'Yes')

        %- Extract index of highlighted cell
        ind_sel  = get(handles.listbox_files,'Value');

        %- Delete highlighted cell
        str_list(ind_sel) = [];
        set(handles.listbox_files,'String',str_list)
        set(handles.listbox_files,'Value',1)

        %- Update status
        controls_enable(hObject, eventdata, handles)        
        status_text = {' ';'## File removed'; [num2str(size(str_list,1)) ' files will be processed']};
        status_update(hObject, eventdata, handles,status_text);  
    end
end


%== Delete all files
function button_files_delete_all_Callback(hObject, eventdata, handles)
%- Ask user to confirm choice
choice = questdlg('Do you really want to remove all files?', 'FISH-QUANT', 'Yes','No','No');

if strcmp(choice,'Yes')
    
    set(handles.listbox_files,'String',{})
    set(handles.listbox_files,'Value',1)
    
    %- Update status
    controls_enable(hObject, eventdata, handles)
    status_update(hObject, eventdata, handles,{' ';'## All files removed'});    
end


%== Load files with results
function menu_load_results_Callback(hObject, eventdata, handles)

[file_name_results,path_name_results] = uigetfile({'*.txt'},'Select file with results of spot detection','MultiSelect', 'on');

if ~iscell(file_name_results)
    dum =file_name_results; 
    file_name_results = {dum};
end
    
if file_name_results{1} ~= 0 


    ind_cell = 1;
    for i_F = 1:length(file_name_results)
    
       [cell_prop file_name_image par_microscope file_name_settings file_name_image_filtered] = FISH_QUANT_load_results_v7(fullfile(path_name_results,file_name_results{i_F}));
       
       file_summary(i_F).file_name_image          = file_name_image;
       file_summary(i_F).file_name_image_filtered = file_name_image_filtered;
       file_summary(i_F).par_microscope           = par_microscope;
       file_summary(i_F).cells.start              = ind_cell;
       
        for i_C = 1:length(cell_prop)
            cell_summary(ind_cell,1).name_image               = file_name_image;
            cell_summary(ind_cell,1).file_name_image_filtered = file_name_image_filtered;
            cell_summary(ind_cell,1).cell                     = cell_prop(i_C).label;
            cell_summary(ind_cell,1).x                        = cell_prop(i_C).x; 
            cell_summary(ind_cell,1).y                        = cell_prop(i_C).y;  
            cell_summary(ind_cell,1).pos_TS                   = cell_prop(i_C).pos_TS;             
            cell_summary(ind_cell,1).spots_fit                = cell_prop(i_C).spots_fit;
            cell_summary(ind_cell,1).spots_detected           = cell_prop(i_C).spots_detected;
            
            if not(isempty(cell_summary(ind_cell,1).spots_fit))
                cell_summary(ind_cell,1).thresh.in = cell_prop(i_C).thresh.in;
                N_total = size(cell_prop(i_C).spots_fit,1);  
                cell_summary(ind_cell,1).N_total    = N_total;
            else
                cell_summary(ind_cell,1).thresh.in = [];                
            end
            
            spot_count(ind_cell,:) = [0 N_total];
            ind_cell = ind_cell+1;
        end 
        file_summary(i_F).cells.end      = ind_cell-1;          
    end
    
    handles.par_microscope  = par_microscope;    
       
    %--- Load settings
    
    %- Set all threshold locks to zero - the ones which are locked will be changed to one 
    handles.thresh_all.sigmaxy.lock = 0;
    handles.thresh_all.sigmaz.lock  = 0;
    handles.thresh_all.amp.lock     = 0;
    handles.thresh_all.bgd.lock     = 0;
    handles.thresh_all.pos_z.lock     = 0;
    
    handles = FISH_QUANT_load_settings_v3(fullfile(path_name_results,file_name_settings),handles);   
    handles.file_name_settings = file_name_settings;
    handles.par_microscope = par_microscope;
    
    %- Assign results
    handles.cell_summary    = cell_summary;
    handles.path_name_image = path_name_results;
    handles.path_name_image = path_name_results;
    handles.spot_count   = spot_count;
    handles.file_summary = file_summary;
    
    %- Update status
    status_text = {' ';'== Result files read in.';[num2str(ind_cell-1) ' can be averaged']};
    status_update(hObject, eventdata, handles,status_text);
    controls_enable(hObject, eventdata, handles)
    
    %- Save results
    handles.status_fit = 1;
    handles = results_summarize(hObject, eventdata, handles);
    handles = results_analyse(hObject, eventdata, handles);
    handles = pop_up_threshold_Callback(hObject, eventdata, handles);
    handles = button_threshold_Callback(hObject, eventdata, handles);
    
end 
  
%- Save results
guidata(hObject, handles); 

 
% =========================================================================
% Batch process
% =========================================================================

%== Load file with detection threshold settings
function menu_settings_detect_thresh_Callback(hObject, eventdata, handles)
[file_name_settings,path_name_settings] = uigetfile({'*.txt'},'Specify files with pre-detection settings for files','MultiSelect', 'on');

if file_name_settings ~= 0
    fid = fopen(fullfile(path_name_settings,file_name_settings));

    %- Read-in header line and determine number of columns
    header_line = fgetl(fid);
    num_cols    = 1 + sum(header_line == sprintf('\t'));

    %- Read in data
    str_read_in = ['%s',repmat('%f', 1, num_cols-1)];
    thresh_struct = textscan(fid, str_read_in,'HeaderLines',0,'delimiter','\t','CollectOutput',0);
    fclose(fid);

    %- Assign values
    detect_file.name = thresh_struct{1};
    
    if num_cols == 2 
        detect_file.th_detect = thresh_struct{2};
    end
    
    if num_cols > 2 
        detect_file.th_detect = thresh_struct{2};
        detect_file.th_score  = thresh_struct{3};
    end
   
    handles.detect_file = detect_file;
    guidata(hObject, handles);
end


%== Fit with fixed width
function checkbox_fixed_width_Callback(hObject, eventdata, handles)
val_check = get(handles.checkbox_fixed_width,'Value');
handles.flag_fit = val_check;
guidata(hObject, handles);


%== Process all files
function button_process_Callback(hObject, eventdata, handles)

flag_struct.parallel  = get(handles.checkbox_parallel_computing,'Value');
flag_struct.threshold = 1;

%- Determine mode of fitting
flag_fit  = handles.flag_fit; %get(handles.checkbox_fixed_width,'Value');

if flag_fit == 0
    mode_fit  = 'sigma_free_xz';
    par_start = [];
    handles.par_fit = [];
else
    mode_fit = 'sigma_fixed';    
    sigma.xy = str2double(get(handles.text_psf_fit_sigmaX,'String'));
    sigma.z  = str2double(get(handles.text_psf_fit_sigmaZ,'String'));
    
    %- Starting point
    par_start.sigmax = sigma.xy;
    par_start.sigmay = sigma.xy;
    par_start.sigmaz = sigma.z;
        
    %- Save values
    handles.par_fit.sigma_XY_fixed = sigma.xy;
    handles.par_fit.sigma_Z_fixed  = sigma.z; 
end

%- Update status
status_text = {' ';'== Processing files - check command window for details'};
status_update(hObject, eventdata, handles,status_text);

file_list = get(handles.listbox_files,'String');
path_name = handles.path_name_image;
N_file = size(file_list,1);


%- (Some) Parameters for detection and fitting
parameters.path_name = path_name;
parameters.mode_fit = mode_fit;
parameters.par_start = par_start;
parameters.par_microscope = handles.par_microscope;
parameters.file_name_settings = handles.file_name_settings;
parameters.flag_struct = flag_struct; 

cell_counter = 1;
cell_summary = [];

for i_file = 1:size(file_list,1)
    
    file_name_outline = file_list{i_file};
       
    disp(' ');
    disp(['- Processing file ', num2str(i_file), ' of ', num2str(N_file)]);
        
    %- Update status
    status_text = {['- Processing file ', num2str(i_file), ' of ', num2str(N_file)]};
    status_update(hObject, eventdata, handles,status_text);
        
    
    %- Check if separate detection settings are specified
    parameters.detect  = handles.detect;
    
    if isfield(handles,'detect_file')
        if isfield(handles.detect_file,'name')
            ind_file = find(strcmpi(file_name_outline,handles.detect_file.name));

            if not(isempty(ind_file))
                if isfield(handles.detect_file,'th_detect')
                    parameters.detect.thresh_int = handles.detect_file.th_detect(ind_file);            
                end

                if isfield(handles.detect_file,'th_score')
                    parameters.detect.thresh_score = handles.detect_file.th_score(ind_file);
                end
            end
        end
    end
        
    %- Process files  
    parameters.file_name_load = file_name_outline; 
    [cell_prop par_microscope file_name_image file_name_image_filtered] = spot_detect_fit_v14(handles,parameters);  
    
    file_summary(i_file).file_name_image          = file_name_image;
    file_summary(i_file).file_name_image_filtered = file_name_image_filtered;
    file_summary(i_file).par_microscope           = par_microscope;
    
    
    %- Summarize results and loop over all processed cells for this file
    file_summary(i_file).cells.start = cell_counter;
    N_cell                           =  size(cell_prop,2);
    
    for i_cell = 1:N_cell
          
        N_total = size(cell_prop(i_cell).spots_fit,1);        
        cell_summary(cell_counter,1).name_image               = file_name_image;
        cell_summary(cell_counter,1).file_name_image_filtered = file_name_image_filtered;
        cell_summary(cell_counter,1).cell                     = cell_prop(i_cell).label;
        cell_summary(cell_counter,1).N_total                  = N_total;
        cell_summary(cell_counter,1).spots_fit                = cell_prop(i_cell).spots_fit;
        cell_summary(cell_counter,1).spots_detected           = cell_prop(i_cell).spots_detected;
        cell_summary(cell_counter,1).thresh.in                = ones(size(cell_prop(i_cell).spots_fit,1),1);
        
        cell_summary(cell_counter,1).label                    = cell_prop(i_cell).label; 
        cell_summary(cell_counter,1).x                        = cell_prop(i_cell).x; 
        cell_summary(cell_counter,1).y                        = cell_prop(i_cell).y;  
        cell_summary(cell_counter,1).pos_TS                   = cell_prop(i_cell).pos_TS; 

        spot_count(cell_counter,:) = [0 N_total];
        cell_counter = cell_counter +1;
        
        %- Update status
        status_text = ['Spots: [total] ', num2str(N_total)];
        status_update(hObject, eventdata, handles,status_text);   
    end
    file_summary(i_file).cells.end      = cell_counter-1;        
end

handles.cell_summary = cell_summary; 
handles.spot_count   = spot_count;
handles.file_summary = file_summary;

%- Analyze and save results
handles.status_fit = 1;
handles = results_summarize(hObject, eventdata, handles);
if not(isempty(handles.spots_fit_all))
    handles = results_analyse(hObject, eventdata, handles);
    handles = pop_up_threshold_Callback(hObject, eventdata, handles);
    handles = button_threshold_Callback(hObject, eventdata, handles);
    guidata(hObject, handles); 
else
    status_text = {' ';'NO SPOTS DETECTED'};
    status_update(hObject, eventdata, handles,status_text); 
end


%== Summarize results for quick illustration
function handles = results_summarize(hObject, eventdata, handles)

%-- Extract relevant parameters of all cells
cell_summary = handles.cell_summary;
thresh_all   = handles.thresh_all;

spots_fit_all      = [];
spots_detected_all = [];
thresh_all.in      = [];

for i_cell = 1:size(cell_summary,1)
    
    %- Save start index of cell
    spots_range(i_cell).start = size(spots_fit_all,1)+1;
    
    %- Extract parameters for each cells
    spots_fit_loop      = cell_summary(i_cell,1).spots_fit;
    spots_detected_loop = cell_summary(i_cell,1).spots_detected;    
    thresh_in_loop      = cell_summary(i_cell,1).thresh.in;
    
    %- Save in long list
    spots_fit_all      = vertcat(spots_fit_all,spots_fit_loop);
    spots_detected_all = vertcat(spots_detected_all,spots_detected_loop);
    thresh_all.in      = vertcat(thresh_all.in,thresh_in_loop);
    
    %- Save end index of cell
    spots_range(i_cell).end = size(spots_fit_all,1);    
     
end

%- Calculate averaged value for PSF
col_par = handles.col_par;
if not(isempty(spots_fit_all))
    PSF.avg_OF.avg_sigmaxy = round(mean(spots_fit_all(:,col_par.sigmax)));
    PSF.avg_OF.avg_sigmaz  = round(mean(spots_fit_all(:,col_par.sigmaz)));
    PSF.avg_OF.avg_bgd     = round(mean(spots_fit_all(:,col_par.bgd)));
    PSF.avg_OF.avg_amp     = round(mean(spots_fit_all(:,col_par.amp)));

    PSF.avgED.avg_sigmaxy = PSF.avg_OF.avg_sigmaxy;
    PSF.avgED.avg_sigmaz  = PSF.avg_OF.avg_sigmaz;
    PSF.avgED.avg_bgd     = PSF.avg_OF.avg_bgd;
    PSF.avgED.avg_amp     = PSF.avg_OF.avg_amp;
else
    PSF.avg_OF.avg_sigmaxy = [];
    PSF.avg_OF.avg_sigmaz  = [];
    PSF.avg_OF.avg_bgd     = [];
    PSF.avg_OF.avg_amp     = [];

    PSF.avgED.avg_sigmaxy = [];
    PSF.avgED.avg_sigmaz  = [];
    PSF.avgED.avg_bgd     = [];
    PSF.avgED.avg_amp     = [];
end
    

set(handles.text_psf_fit_sigmaX,'String',num2str(PSF.avg_OF.avg_sigmaxy))
set(handles.text_psf_fit_sigmaZ,'String',num2str(PSF.avg_OF.avg_sigmaz))
set(handles.text_psf_bgd,'String',num2str(PSF.avg_OF.avg_bgd))
set(handles.text_psf_amp,'String',num2str(PSF.avg_OF.avg_amp))

%- Plot histogram of detected spots
axes(handles.axes_image)
hist(handles.spot_count(:,2),30)
title('Histogram of number of mRNA per cell')

%- Analyse distribution of detected spots
N_total_mean = mean(handles.spot_count(:,2));
N_total_std  = std(handles.spot_count(:,2));

N_th_mean = mean(handles.spot_count(:,1));
N_th_std  = std(handles.spot_count(:,1));

%- Update status
status_text = {' '; ...
               ['AVG # spots per cell [total] ', num2str(N_total_mean), ' +/- ', num2str(N_total_std)]; ...
               ['AVG # spots per cell [after threshold] ', num2str(N_th_mean), ' +/- ', num2str(N_th_std)]};

status_update(hObject, eventdata, handles,status_text);  
controls_enable(hObject, eventdata, handles)


%- Save all results
handles.PSF = PSF;
handles.spots_fit_all = spots_fit_all;
handles.spots_range   = spots_range;
handles.thresh_all = thresh_all;
guidata(hObject, handles); 


%== Analyze results of batch processing for further thresholding
function handles = results_analyse(hObject, eventdata, handles)

spots_fit_all = handles.spots_fit_all;
thresh_all    = handles.thresh_all;
col_par       = handles.col_par;

%- Set-up structure for thresholding
thresh_all.sigmaxy.values   = spots_fit_all(:,col_par.sigmax);
thresh_all.sigmaxy.min      = min(spots_fit_all(:,col_par.sigmax));
thresh_all.sigmaxy.max      = max(spots_fit_all(:,col_par.sigmax));
thresh_all.sigmaxy.diff     = max(spots_fit_all(:,col_par.sigmax)) - min(spots_fit_all(:,col_par.sigmax));             
thresh_all.sigmaxy.in       = thresh_all.in;
if thresh_all.sigmaxy.lock == 0
    thresh_all.sigmaxy.min_hist = min(spots_fit_all(:,col_par.sigmax));               
    thresh_all.sigmaxy.max_hist = max(spots_fit_all(:,col_par.sigmax)); 
end

thresh_all.sigmaz.values   = spots_fit_all(:,col_par.sigmaz );
thresh_all.sigmaz.min      = min(spots_fit_all(:,col_par.sigmaz ));
thresh_all.sigmaz.max      = max(spots_fit_all(:,col_par.sigmaz ));
thresh_all.sigmaz.diff     = max(spots_fit_all(:,col_par.sigmaz )) - min(spots_fit_all(:,col_par.sigmaz ));             
thresh_all.sigmaz.in       = thresh_all.in; 
if thresh_all.sigmaz.lock == 0
    thresh_all.sigmaz.min_hist = min(spots_fit_all(:,col_par.sigmaz ));               
    thresh_all.sigmaz.max_hist = max(spots_fit_all(:,col_par.sigmaz )); 
end

thresh_all.amp.values   = spots_fit_all(:,col_par.amp);
thresh_all.amp.min      = min(spots_fit_all(:,col_par.amp));
thresh_all.amp.max      = max(spots_fit_all(:,col_par.amp));
thresh_all.amp.diff     = max(spots_fit_all(:,col_par.amp)) - min(spots_fit_all(:,col_par.amp));             
thresh_all.amp.in       = thresh_all.in;           
if thresh_all.amp.lock == 0
    thresh_all.amp.min_hist = min(spots_fit_all(:,col_par.amp));               
    thresh_all.amp.max_hist = max(spots_fit_all(:,col_par.amp)); 
end

thresh_all.bgd.values = spots_fit_all(:,col_par.bgd );
thresh_all.bgd.min      = min(spots_fit_all(:,col_par.bgd ));
thresh_all.bgd.max      = max(spots_fit_all(:,col_par.bgd ));
thresh_all.bgd.diff     = max(spots_fit_all(:,col_par.bgd )) - min(spots_fit_all(:,col_par.bgd ));             
thresh_all.bgd.in       = thresh_all.in;            
if thresh_all.bgd.lock == 0
    thresh_all.bgd.min_hist = min(spots_fit_all(:,col_par.bgd ));               
    thresh_all.bgd.max_hist = max(spots_fit_all(:,col_par.bgd )); 
end

thresh_all.pos_z.values = spots_fit_all(:,col_par.pos_z );
thresh_all.pos_z.min      = min(spots_fit_all(:,col_par.pos_z ));
thresh_all.pos_z.max      = max(spots_fit_all(:,col_par.pos_z ));
thresh_all.pos_z.diff     = max(spots_fit_all(:,col_par.pos_z )) - min(spots_fit_all(:,col_par.pos_z ));             
thresh_all.pos_z.in       = thresh_all.in;            
if thresh_all.pos_z.lock == 0
    thresh_all.pos_z.min_hist = min(spots_fit_all(:,col_par.pos_z ));               
    thresh_all.pos_z.max_hist = max(spots_fit_all(:,col_par.pos_z )); 
end

%- Save all results
handles.thresh_all = thresh_all;


%== Update status
function status_update(hObject, eventdata, handles,status_text)
status_old = get(handles.listbox_status,'String');
status_new = [status_old;status_text];
set(handles.listbox_status,'String',status_new)
set(handles.listbox_status,'ListboxTop',round(size(status_new,1)))
drawnow
guidata(hObject, handles); 


% =========================================================================
% Thresholding
% =========================================================================

%== When selecting a new thresholding parameter
function handles = pop_up_threshold_Callback(hObject, eventdata, handles)

%- Extracted fitted spots for this cell
spots_fit  = handles.spots_fit_all;

%- Executes only if there are results
if not(isempty(spots_fit))
    
    thresh_all     = handles.thresh_all;
    
    str = get(handles.pop_up_threshold,'String');
    val = get(handles.pop_up_threshold,'Value');
    popup_parameter = str{val};

    
    switch (popup_parameter)
        
        case 'Sigma - XY'
            thresh_sel = thresh_all.sigmaxy;
            
            %- Check if selection was locked
            if thresh_all.sigmaxy.lock == 1; set(handles.checkbox_th_lock, 'Value',1); end;
            if thresh_all.sigmaxy.lock == 0; set(handles.checkbox_th_lock, 'Value',0); end;
            
        case 'Sigma - Z'
            thresh_sel = thresh_all.sigmaz;
            
            %- Check if selection was locked
            if thresh_all.sigmaz.lock == 1; set(handles.checkbox_th_lock, 'Value',1); end;
            if thresh_all.sigmaz.lock == 0; set(handles.checkbox_th_lock, 'Value',0); end;
            
        case 'Amplitude'
            thresh_sel = thresh_all.amp;
            
            %- Check if selection was locked
            if thresh_all.amp.lock == 1; set(handles.checkbox_th_lock, 'Value',1); end;
            if thresh_all.amp.lock == 0; set(handles.checkbox_th_lock, 'Value',0); end;
            
        case 'Background'
            thresh_sel = thresh_all.bgd;
            
            %- Check if selection was locked
            if thresh_all.bgd.lock == 1; set(handles.checkbox_th_lock, 'Value',1); end;
            if thresh_all.bgd.lock == 0; set(handles.checkbox_th_lock, 'Value',0); end;
            
        case 'Pos (Z)'
            thresh_sel = thresh_all.pos_z;
            
            %- Check if selection was locked
            if thresh_all.pos_z.lock == 1; set(handles.checkbox_th_lock, 'Value',1); end;
            if thresh_all.pos_z.lock == 0; set(handles.checkbox_th_lock, 'Value',0); end;
            

    end

    %== For slider functions calls and call of threshold function
    thresh_all.min  = thresh_sel.min;   
    thresh_all.max  = thresh_sel.max;
    thresh_all.diff = thresh_sel.diff; 
    thresh_all.values = thresh_sel.values; 
    
    %== Set sliders and text box according to selection    
    %-  Locked - based on saved values
    if thresh_sel.lock == 1; 
        set(handles.checkbox_th_lock, 'Value',1);  
        
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
                
        set(handles.checkbox_th_lock, 'Value',0);
        
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
    if thresh_sel.lock == 1    
        handles.h_hist_min = plot([thresh_sel.min_hist thresh_sel.min_hist] , [0 1e5],'r');
        handles.h_hist_max = plot([thresh_sel.max_hist thresh_sel.max_hist] , [0 1e5],'g');
    else        
        handles.h_hist_min = plot([thresh_sel.min thresh_sel.min] , [0 1e5],'r');
        handles.h_hist_max = plot([thresh_sel.max thresh_sel.max] , [0 1e5],'g');
    end
    hold off
    axis(v);
    
    title(['Total # of spots: ',sprintf('%d' ,length(thresh_all.in) )],'FontSize',8);     
    freezeColors(gca) 
    
    %=== Update thresholded histogram
    cla(handles.axes_histogram_th,'reset');
    axes(handles.axes_histogram_th);
    colormap(jet);
    hist(thresh_sel.values(thresh_all.in == 1),25);           
    
    % Plot lines at locked thresholds if specified
    v = axis;
    hold on
    if thresh_sel.lock == 1    
        handles.h_hist_th_min = plot([thresh_sel.min_hist thresh_sel.min_hist] , [0 1e5],'r');
        handles.h_hist_th_max = plot([thresh_sel.max_hist thresh_sel.max_hist] , [0 1e5],'g');
    else        
        handles.h_hist_th_min = plot([thresh_sel.min thresh_sel.min] , [0 1e5],'r');
        handles.h_hist_th_max = plot([thresh_sel.max thresh_sel.max] , [0 1e5],'g');
    end
    hold off
    axis(v);
    title(strcat('Selected # of spots ',sprintf('%d' ,sum(thresh_all.in) )),'FontSize',8);    
    freezeColors(gca)    
        
    %- Save handles-structure
    handles.thresh_all = thresh_all;
    handles.output = hObject;
    guidata(hObject, handles);  

end


%== Threshold parameters
function handles = button_threshold_Callback(hObject, eventdata, handles)

%- Extracted fitted spots for this cell
spots_fit  = handles.spots_fit_all;

%- Execute only if there are results
if not(isempty(spots_fit))
    
    thresh_all     = handles.thresh_all;
    
    %- Locked threshold?
    th_lock  = get(handles.checkbox_th_lock, 'Value');
    
    %- Selected thresholds
    min_hist = floor(str2double(get(handles.text_th_min,'String')));        % floor and ceil necessary for extreme slider position to select all points.
    max_hist = ceil(str2double(get(handles.text_th_max,'String')));      
    
    thresh_all.min_hist =  min_hist;    
    thresh_all.max_hist =  max_hist;
    
    %- Thresholding parameter
    str = get(handles.pop_up_threshold,'String');
    val = get(handles.pop_up_threshold,'Value');
    popup_parameter = str{val};     
    
    switch (popup_parameter)
        case 'Sigma - XY'
            thresh_all.sigmaxy.lock     = th_lock;
            thresh_all.sigmaxy.min_hist = min_hist;
            thresh_all.sigmaxy.max_hist = max_hist;            
            thresh_all.sigmaxy.in       = (thresh_all.sigmaxy.values >=min_hist) & (thresh_all.sigmaxy.values<=max_hist);
            thresh_all.in_sel           = thresh_all.sigmaxy.in; 
            
        case 'Sigma - Z'           
            thresh_all.sigmaz.lock     = th_lock;
            thresh_all.sigmaz.min_hist = min_hist;
            thresh_all.sigmaz.max_hist = max_hist;            
            thresh_all.sigmaz.in       = (thresh_all.sigmaz.values >= min_hist) & (thresh_all.sigmaz.values<=max_hist);
            thresh_all.in_sel          = thresh_all.sigmaz.in; 
            
        case 'Amplitude'            
            thresh_all.amp.lock     = th_lock;
            thresh_all.amp.min_hist = min_hist;
            thresh_all.amp.max_hist = max_hist;            
            thresh_all.amp.in       =   (thresh_all.amp.values >= min_hist) & (thresh_all.amp.values<=max_hist);
            thresh_all.in_sel       = thresh_all.amp.in;    
            
        case 'Background'            
            thresh_all.bgd.lock     = th_lock;
            thresh_all.bgd.min_hist = min_hist;
            thresh_all.bgd.max_hist = max_hist;            
            thresh_all.bgd.in       = (thresh_all.bgd.values >= min_hist) & (thresh_all.bgd.values<=max_hist);
            thresh_all.in_sel       = thresh_all.bgd.in;
 
        case 'Pos (Z)'            
            thresh_all.pos_z.lock     = th_lock;
            thresh_all.pos_z.min_hist = min_hist;
            thresh_all.pos_z.max_hist = max_hist;            
            thresh_all.pos_z.in       = (thresh_all.pos_z.values >= min_hist) & (thresh_all.pos_z.values<=max_hist);
            thresh_all.in_sel         = thresh_all.pos_z.in;          
                 
    end
    
    
    %-- Apply all thresholds --> will be checked later if locked or not
    thresh_all.sigmaxy.in  = (thresh_all.sigmaxy.values >= thresh_all.sigmaxy.min_hist) & (thresh_all.sigmaxy.values<=thresh_all.sigmaxy.max_hist);
    thresh_all.sigmaz.in   = (thresh_all.sigmaz.values >= thresh_all.sigmaz.min_hist) & (thresh_all.sigmaz.values<=thresh_all.sigmaz.max_hist);
    thresh_all.amp.in      = (thresh_all.amp.values >= thresh_all.amp.min_hist) & (thresh_all.amp.values<=thresh_all.amp.max_hist);
    thresh_all.bgd.in      = (thresh_all.bgd.values >= thresh_all.bgd.min_hist) & (thresh_all.bgd.values<=thresh_all.bgd.max_hist);
    thresh_all.pos_z.in    = (thresh_all.pos_z.values >= thresh_all.pos_z.min_hist) & (thresh_all.pos_z.values<=thresh_all.pos_z.max_hist);
    
    handles.thresh_all = thresh_all;
    
    %=== Apply threshold
    handles = threshold_apply(hObject, eventdata, handles);
    
    %=== Save data
    guidata(hObject, handles); 
end


%=== Function to apply selected thresholds
function handles = threshold_apply(hObject, eventdata, handles)

thresh_all   = handles.thresh_all;
spots_fit    = handles.spots_fit_all;
spots_range  =  handles.spots_range;
cell_summary = handles.cell_summary;

%- Other parameters
PSF    = handles.PSF;


%- Thresholding under consideration of locked ones
%  Can be written in boolean algebra: Implication Z = x?y = ?x?y = not(x) or y
%  http://en.wikipedia.org/wiki/Boolean_algebra_%28logic%29#Basic_operations
%  x = [0,1] ... unlocked [0] and locked [1]
%  y = [0,1] ... index is thresholded [0] or not [1]
%  Number is always considered (z=1) unless paramter is locked (x=1) and number is thresholded (y=0)
%          y
%       |0   1
%     ----------
%   x 0 |1   1
%     1 |0   1

%- Save old thresholding
thresh_all.in_old         = thresh_all.in;
thresh_all.logic_out_man  = (thresh_all.in == -1);

%- New thresholding only with locked values
thresh_all.logic_in  = (not(thresh_all.sigmaxy.lock) | thresh_all.sigmaxy.in) & ...
                       (not(thresh_all.sigmaz.lock)  | thresh_all.sigmaz.in) & ...
                       (not(thresh_all.amp.lock)     | thresh_all.amp.in) & ...
                       (not(thresh_all.bgd.lock)     | thresh_all.bgd.in) & ...
                       (not(thresh_all.pos_z.lock)   | thresh_all.pos_z.in) ;          

thresh_all.in(thresh_all.logic_in == 1) = 1;
thresh_all.in(thresh_all.logic_in == 0) = 0;
thresh_all.in(thresh_all.logic_out_man) = -1;  

thresh_all.out = (thresh_all.in == 0) | (thresh_all.in == -1);

status_text = ' ';
status_update(hObject, eventdata, handles,status_text);  
  
for ind_cell = 1: length(spots_range)
    cell_summary(ind_cell,1).thresh.in = thresh_all.in(spots_range(ind_cell).start:spots_range(ind_cell).end);
    
    N_total = cell_summary(ind_cell,1).N_total;
    N_count = sum(cell_summary(ind_cell,1).thresh.in);
               
    spot_count(ind_cell,:) = [N_count N_total];
        
    %- Update status
    status_text = ['Cell ', num2str(ind_cell), '; spots: [total] ', num2str(N_total), ', [after thresholding] :', num2str(N_count)];
    status_update(hObject, eventdata, handles,status_text);  
    
    cell_summary(ind_cell,1).spot_count = spot_count;
    cell_summary(ind_cell,1).N_count = N_count;    
end


% Spots which are in considering the current selection even if it is not locked
thresh_all.in_display = thresh_all.in_sel  & thresh_all.logic_in; 
col_par = handles.col_par;


%- Update experimental PSF settings    
PSF.avg_OF.avg_sigmaxy  = round(mean(spots_fit(thresh_all.in_display,col_par.sigmax)));
PSF.avg_OF.avg_sigmaz   = round(mean(spots_fit(thresh_all.in_display,col_par.sigmaz)));
PSF.avg_OF.avg_amp      = round(mean(spots_fit(thresh_all.in_display,col_par.amp )));
PSF.avg_OF.avg_bgd      = round(mean(spots_fit(thresh_all.in_display,col_par.bgd)));

PSF.avg_OF.avg_sigmaxy_std = round(std(spots_fit(thresh_all.in_display,col_par.sigmax)));
PSF.avg_OF.avg_sigmaz_std  = round(std(spots_fit(thresh_all.in_display,col_par.sigmaz)));
PSF.avg_OF.avg_amp_std     = round(std(spots_fit(thresh_all.in_display,col_par.amp )));
PSF.avg_OF.avg_bgd_std     = round(std(spots_fit(thresh_all.in_display,col_par.bgd)));

disp(' ')
disp('FIT TO 3D GAUSSIAN: avg of ALL spots ')
disp(['Sigma (xy): ', num2str(PSF.avg_OF.avg_sigmaxy), ' +/- ', num2str(PSF.avg_OF.avg_sigmaxy_std)])
disp(['Sigma (z) : ', num2str(PSF.avg_OF.avg_sigmaz), ' +/- ', num2str(PSF.avg_OF.avg_sigmaz_std )])
disp(['Amplitude : ', num2str(PSF.avg_OF.avg_amp), ' +/- ', num2str(PSF.avg_OF.avg_amp_std)])
disp(['BGD       : ', num2str(PSF.avg_OF.avg_bgd), ' +/- ', num2str(PSF.avg_OF.avg_bgd_std )])
disp(' ')

set(handles.text_psf_fit_sigmaX,'String',num2str(PSF.avg_OF.avg_sigmaxy))
set(handles.text_psf_fit_sigmaZ,'String',num2str(PSF.avg_OF.avg_sigmaz))
set(handles.text_psf_bgd,'String',num2str(PSF.avg_OF.avg_bgd))
set(handles.text_psf_amp,'String',num2str(PSF.avg_OF.avg_amp))

%=== Save data
handles.PSF = PSF;
handles.thresh_all   = thresh_all;
handles.cell_summary = cell_summary;

%=== VARIOUS PLOTS

%- Plot histogram
handles = plot_hist_all(handles,handles.axes_histogram_all);

%- Plot thresholded histogram
handles = plot_hist_th(handles,handles.axes_histogram_th);

%- Plot histogram of detected spots
axes(handles.axes_image)
hist(spot_count(:,1),30)
title('Histogram of number of mRNA per cell')


%=== Save data
guidata(hObject, handles); 


%==== Button to unlock all thresholds
function button_th_unlock_all_Callback(hObject, eventdata, handles)

thresh_all     = handles.thresh_all;

thresh_all.sigmaxy.lock = 0; 
thresh_all.sigmaz.lock  = 0;
thresh_all.amp.lock     = 0; 
thresh_all.bgd.lock     = 0;               
thresh_all.pos_z.lock   = 0;  

handles.thresh_all = thresh_all;

handles = pop_up_threshold_Callback(hObject, eventdata, handles);
handles = button_threshold_Callback(hObject, eventdata, handles);

guidata(hObject, handles);


%=== Check-box for locking parameters
function checkbox_th_lock_Callback(hObject, eventdata, handles) 
handles = button_threshold_Callback(hObject, eventdata, handles);


%=== Slider for minimum values of threshold
function slider_th_min_Callback(hObject, eventdata, handles)
sliderValue = get(handles.slider_th_min,'Value');

thresh_all     = handles.thresh_all;

%- Determine value at current slider position
value_thresh = sliderValue*thresh_all.diff+thresh_all.min;

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
thresh_all     = handles.thresh_all;

%- Determine value at current slider position
value_thresh = sliderValue*thresh_all.diff+thresh_all.min;

%- Change text box and line in histogram
set(handles.text_th_max,'String', value_thresh);

axes(handles.axes_histogram_all);
delete(handles.h_hist_max);
v = axis;
hold on
handles.h_hist_max = plot([value_thresh value_thresh] , [0 1e5],'g');
hold off
axis(v);

axes(handles.axes_histogram_th);
delete(handles.h_hist_th_max);
v = axis;
hold on
handles.h_hist_th_max = plot([value_thresh value_thresh] , [0 1e5],'g');
hold off
axis(v);

guidata(hObject, handles);      % Update handles structure


%=== Edit values of slider selection: minimum 
function text_th_min_Callback(hObject, eventdata, handles)
value_edit = str2double(get(handles.text_th_min,'String'));

thresh_all     = handles.thresh_all;

%- Set new slider value only if value is within range
if value_edit > thresh_all.min  && value_edit < thresh_all.max
    slider_new = (value_edit-thresh_all.min)/thresh_all.diff;
    set(handles.slider_th_min,'Value',slider_new);   

else
    %sliderValue  = get(handles.slider_th_min ,'Value');
    %value_edit = sliderValue*thresh_all.diff+thresh_all.min;
    set(handles.text_th_min,'String',num2str(value_edit))    
end


%=== Edit values of slider selection: maximum 
function text_th_max_Callback(hObject, eventdata, handles)
value_edit = str2double(get(handles.text_th_max,'String'));
thresh_all = handles.thresh_all;

%- Set new slider value only if value is within range    
if value_edit > thresh_all.min && value_edit < thresh_all.max
    slider_new = (value_edit-thresh_all.min)/thresh_all.diff;
    set(handles.slider_th_max,'Value',slider_new);
    slider_th_max_Callback(hObject, eventdata, handles)
else
    %sliderValue = get(handles.slider_th_max ,'Value');
    %value_edit  = sliderValue*thresh_all.diff+thresh_all.min;
    set(handles.text_th_max,'String',num2str(value_edit)) 
end



% =========================================================================
% MISC functions
% =========================================================================


%== Advanced settings
function menu_adv_settings_Callback(hObject, eventdata, handles)


%== Same outline for all images: load outline
function menu_load_outline_enable_Callback(hObject, eventdata, handles)
outline_unique = handles.status_outline_unique_enable;

if outline_unique
    default_answer = 'NO';
else
    default_answer = 'YES';
end

choice = questdlg('Use same outline for each image?', 'Outline definition', 'YES','NO',default_answer);


switch choice
    case 'YES'
        handles.status_outline_unique_enable = 1;
        set(handles.menu_load_outline_enable ,'Check','on') 
    case 'NO'
        handles.status_outline_unique_enable = 0;
        set(handles.menu_load_outline_enable ,'Check','off') 
end
guidata(hObject, handles);


%== Same outline for all images: load outline
function menu_load_outline_same_Callback(hObject, eventdata, handles)
[file_name_outline,path_name] = uigetfile({'*.txt'},'Select file with outline definition','MultiSelect', 'off');

if file_name_outline ~= 0 
      
    %- Load results
    handles.cell_prop_loaded  = FISH_QUANT_load_results_v7(fullfile(path_name,file_name_outline));
    
    %- Save and update status
    handles.status_outline_unique_loaded = 1;
    handles.status_outline_unique_enable = 1;
    set(handles.menu_load_outline_enable ,'Check','on') 
    guidata(hObject, handles);
    controls_enable(hObject, eventdata, handles)
end


% =========================================================================
% Save results
% =========================================================================


%== Settings for save
function menu_settings_save_Callback(hObject, eventdata, handles)
handles.settings_save = FQ_change_setting_save_v1(handles.settings_save);
status_update(hObject, eventdata, handles,{'  ';'## Settings for SAVING are modified'});         
guidata(hObject, handles);


%== Save settings from menu
function handles = menu_save_settings_Callback(hObject, eventdata, handles)

current_dir = pwd;
cd(handles.path_name_image)

name_settings = handles.file_name_settings_save;

%- User-dialog
dlgTitle = 'File with detection settings';
prompt_avg(1) = {'File-name'};
defaultValue{1} = name_settings;
options.Resize='on';

userValue = inputdlg(prompt_avg,dlgTitle,1,defaultValue,options);

%- Save results
if( ~ isempty(userValue))
    name_settings  = userValue{1};
    
    handles.file_name_settings_new  = FISH_QUANT_save_settings_v7(fullfile(handles.path_name_image,name_settings),handles);
    handles.file_name_settings_save = name_settings;
    guidata(hObject, handles);   
end
cd(current_dir)
  
   
%== Check if settings file already saved
function handles = save_settings(hObject, eventdata, handles)
if not(isfield(handles,'file_name_settings_new'))
     handles = menu_save_settings_Callback(hObject, eventdata, handles);
     guidata(hObject, handles); 
end  
     
   
%== Save summary of parameters of all detected spots
function menu_save_Callback(hObject, eventdata, handles)


%== Function to save results of all spots
function save_summary_spots(hObject, eventdata, handles,flag_threshold)

%- Save settings
handles = save_settings(hObject, eventdata, handles);

%- User-dialog for file-name
dlgTitle = 'File with all spots';
prompt_avg(1) = {'File-name'};
name_default =  ['FISH-QUANT__all_spots_', datestr(date,'yymmdd'), '.txt'];
defaultValue_avg{1} = name_default;

options.Resize='on';
userValue = inputdlg(prompt_avg,dlgTitle,1,defaultValue_avg,options);

%- Save results
if( ~ isempty(userValue))
    
    %- Get name to save data
    name_default   = userValue{1};
    file_name_full = fullfile(handles.path_name_image,name_default);    

       
    %- Dialog to determine how rows should be labeled
    choice = questdlg('How should rows describing each spot be labeled?', 'Save summary file', ...
                       'Name of file & cell','File identifier','None','Name of file & cell');
    if not(strcmp(choice,''))
                   
        switch choice
            case 'Name of file & cell'
                options.flag_label = 1;
            case 'File identifier'
                options.flag_label = 2;
            case 'None'
                options.flag_label = 3;   
        end
    
        %- Save results
        options.N_ident               = handles.settings_save.N_ident;
        options.flag_only_thresholded = flag_threshold; 
        FISH_QUANT_save_results_all_v5(file_name_full,handles.file_summary,handles.cell_summary,handles.par_microscope,handles.path_name_image,handles.file_name_settings_new,handles.version,options);
    end
end


%== Save summary of all spots in one file [all spots]
function menu_save_summary_spots_Callback(hObject, eventdata, handles)
save_summary_spots(hObject, eventdata, handles,0);


%== Save summary of all spots in one file [thresholded spots]
function menu_save_summary_thresolded_spots_Callback(hObject, eventdata, handles)
save_summary_spots(hObject, eventdata, handles,1);


%== Save results for each image [all spots]
function menu_save_results_image_Callback(hObject, eventdata, handles)

%- Save settings
handles = save_settings(hObject, eventdata, handles);

%== Get file-names for saving

%- Default names
flag_fit  = handles.flag_fit;
if flag_fit == 1    
   [dum, name, ext] = fileparts(handles.file_name_suffix_spots);
   suffix_results = [name,'_fixed_sigma',ext]; 
else
    suffix_results = handles.file_name_suffix_spots; 
end


%- User-dialog
dlgTitle  = 'Names for result files';
prompt(1) = {'Suffix for result files'};
defaultValue{1} = suffix_results;

options.Resize='on';
%options.WindowStyle='normal';

userValue = inputdlg(prompt,dlgTitle,1,defaultValue,options);

%- Save results of individual images
if( ~ isempty(userValue))
    suffix_results  = userValue{1};
    cell_summary = handles.cell_summary;
    file_summary = handles.file_summary;

    for i_file = 1:length(file_summary)

        par_microscope  = handles.par_microscope;
        file_name_image = file_summary(i_file).file_name_image;
        file_name_image_filtered = file_summary(i_file).file_name_image_filtered;

        i_start = file_summary(i_file).cells.start;
        i_end = file_summary(i_file).cells.end;

        cell_prop = {};
        for i_abs = i_start:i_end

            i_rel = i_abs-i_start +1;  

            cell_prop(i_rel).spots_fit = cell_summary(i_abs,1).spots_fit; 
            cell_prop(i_rel).spots_detected = cell_summary(i_abs,1).spots_detected; 
            cell_prop(i_rel).thresh.in = cell_summary(i_abs,1).thresh.in;
            cell_prop(i_rel).x         = cell_summary(i_abs,1).x; 
            cell_prop(i_rel).y         = cell_summary(i_abs,1).y;
            cell_prop(i_rel).pos_TS    = cell_summary(i_abs,1).pos_TS;
            cell_prop(i_rel).label     = cell_summary(i_abs,1).label; 

        end

        %- Save results
        [dum, name_file] = fileparts(file_name_image); 

        file_name_save   = [name_file,suffix_results];
        file_name_full   = fullfile(handles.path_name_image,file_name_save);    
       
        parameters.cell_prop                = cell_prop;
        parameters.par_microscope           = par_microscope;
        parameters.path_name_image          = handles.path_name_image;
        parameters.file_name_image          = file_name_image;
        parameters.file_name_image_filtered = file_name_image_filtered;
        parameters.file_name_settings       = handles.file_name_settings_new;
        parameters.version                  = handles.version;
              
       FISH_QUANT_save_results_v7(file_name_full,parameters);
    end
end
 

%== Save results for each image [thresholded spots]
function menu_save_results_image_only_th_Callback(hObject, eventdata, handles)

handles = save_settings(hObject, eventdata, handles);

%-- Get file-names for saving

%- Default names
flag_fit  = handles.flag_fit;
if flag_fit == 1    
   [dum, name, ext] = fileparts(handles.file_name_suffix_spots);
   suffix_results = [name,'_fixed_sigma',ext]; 
else
    suffix_results = handles.file_name_suffix_spots; 
end


%- User-dialog
dlgTitle  = 'Names for result files';
prompt(1) = {'Suffix for result files'};
defaultValue{1} = suffix_results;

options.Resize='on';
%options.WindowStyle='normal';

userValue = inputdlg(prompt,dlgTitle,1,defaultValue,options);

%- Save results of individual images
if( ~ isempty(userValue))
    suffix_results  = userValue{1};
    cell_summary    = handles.cell_summary;
    file_summary    = handles.file_summary;

    for i_file = 1:length(file_summary)

        par_microscope  = handles.par_microscope;
        file_name_image = file_summary(i_file).file_name_image;
        file_name_image_filtered = file_summary(i_file).file_name_image_filtered;

        i_start = file_summary(i_file).cells.start;
        i_end = file_summary(i_file).cells.end;

        cell_prop = {};
        for i_abs = i_start:i_end

            i_rel = i_abs-i_start +1;  

            %- Save only thresholded spots
            spots_fit      = cell_summary(i_abs,1).spots_fit;
            spots_detected = cell_summary(i_abs,1).spots_detected;
            thresh.in      = cell_summary(i_abs,1).thresh.in;
            ind_save       = (thresh.in == 1);            
            
            cell_prop(i_rel).spots_fit      = spots_fit(ind_save,:); 
            cell_prop(i_rel).spots_detected = spots_detected(ind_save,:); 
            cell_prop(i_rel).thresh.in      = thresh.in(ind_save);
            
            %- Other properties of the cell
            cell_prop(i_rel).x         = cell_summary(i_abs,1).x; 
            cell_prop(i_rel).y         = cell_summary(i_abs,1).y;
            cell_prop(i_rel).pos_TS    = cell_summary(i_abs,1).pos_TS;
            cell_prop(i_rel).label     = cell_summary(i_abs,1).label; 

        end

        %- Save results
        [dum, name_file] = fileparts(file_name_image); 

        file_name_save   = [name_file,suffix_results];
        file_name_full   = fullfile(handles.path_name_image,file_name_save);    
        
        parameters.cell_prop                = cell_prop;
        parameters.par_microscope           = par_microscope;
        parameters.path_name_image          = handles.path_name_image;
        parameters.file_name_image          = file_name_image;
        parameters.file_name_image_filtered = file_name_image_filtered;
        parameters.file_name_settings       = handles.file_name_settings_new;
        parameters.version                  = handles.version;
              
        FISH_QUANT_save_results_v7(file_name_full,parameters);
        
     end
end


%== Save results of TS quantification
function menu_save_nascent_Callback(hObject, eventdata, handles)
if isfield(handles,'TS_summary')
        
    %== Parameters
    parameters.path_name_image    = handles.path_name_image;
    parameters.file_name_settings = handles.file_name_settings_TS;
    parameters.version            = handles.version;
    parameters.file_name_default  = handles.file_name_summary_TS;

    FISH_QUANT_batch_save_summary_TS_v3([],handles.TS_summary,parameters)

end


%== Save results of counting mature mRNA
function menu_save_mature_Callback(hObject, eventdata, handles)
current_dir = pwd;
cd(handles.path_name_image)

handles = save_settings(hObject, eventdata, handles);

flag_fit  = get(handles.checkbox_fixed_width,'Value');
if flag_fit == 1    
   [dum, name, ext] = fileparts(handles.file_name_summary);
   name_summary = [name,'fixed_sigma',ext]; 
       
else
    name_summary = handles.file_name_summary; 
end

%- User-dialog
dlgTitle = 'File with counts of mature mRNA';
prompt_avg(1)   = {'File-name'};
defaultValue{1} = name_summary;
options.Resize='on';

userValue = inputdlg(prompt_avg,dlgTitle,1,defaultValue,options);

%- Save results
if( ~ isempty(userValue))
    name_summary  = userValue{1};
    FISH_QUANT_batch_save_summary_v2(name_summary,handles.cell_summary,handles.path_name_image,handles.file_name_settings_new,handles.version)
end
cd(current_dir)

        

% =========================================================================
% Average spots
% =========================================================================


%== Average all spots
function menu_avg_calc_Callback(hObject, eventdata, handles)
%- Update status
status_text = {' ';'=== Averaging spots: see command window for details'};
status_update(hObject, eventdata, handles,status_text);
 
[handles.spot_avg handles.spot_avg_os handles.average handles.par_microscope] = FISH_QUANT_batch_avg_v3(handles);
guidata(hObject, handles);
controls_enable(hObject, eventdata, handles)


%== Fit averaged spot
function menu_avg_fit_Callback(hObject, eventdata, handles)

%- Parameters needed for function call
flag_crop      = 1;
flag_output    = 1;
img_PSF.data   = handles.spot_avg_os;
par_microscope = handles.par_microscope;
pixel_size_os  = par_microscope.pixel_size_os;
detect.region  = handles.detect.region;

%- Dialog for cropping area
dlgTitle = 'Parameters for cropping';

prompt_crop(1) = {'Size of cropping region [XY]'};
prompt_crop(2) = {'Size of cropping region [Z]'};

defaultValue_crop{1} = num2str(detect.region.xy);
defaultValue_crop{2} = num2str(detect.region.z);

userValue = inputdlg(prompt_crop,dlgTitle,1,defaultValue_crop);

if( ~ isempty(userValue))
    fit.region.xy   = str2double(userValue{1});
    fit.region.z    = str2double(userValue{2}); 
        
    handles.fit.region = handles.fit.region;
    guidata(hObject, handles);
end 

fact_os.xy = handles.average.fact_os.xy; 
fact_os.z  = handles.average.fact_os.z;

par_crop_fit.xy = fit.region.xy * fact_os.xy;
par_crop_fit.z  = fit.region.z  * fact_os.z;

%- Fit with 3D Gaussian
parameters.pixel_size     = pixel_size_os;
parameters.par_microscope = par_microscope;
parameters.flags.crop     = flag_crop;
parameters.flags.output   = flag_output;
parameters.par_crop       = par_crop_fit;

[PSF_fit_os] = PSF_3D_Gauss_fit_v4(img_PSF,parameters);


%== Construct PSF from over-sampled data
function menu_construct_os_Callback(hObject, eventdata, handles)

pixel_size   = handles.par_microscope.pixel_size;
flag_output  = 1;
spot_avg_os  = handles.spot_avg_os;
fact_os      = handles.average.fact_os; 

%- Center of PSF can be placed at different sub-pixel locations
dlgTitle = 'Parameters for contstruction';

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
    
    
    handles.psf_rec = PSF_3D_reconstruct_from_os_v1(spot_avg_os_sub,range_rec,fact_os_mod,flag_output);
    
    controls_enable(hObject, eventdata, handles)
    guidata(hObject, handles);
end
    

%== Show averaged function with normal sampling in ImageJ
function menu_imagej_ns_Callback(hObject, eventdata, handles)
MIJ_start(hObject, eventdata, handles)
MIJ.createImage('Matlab: PSF with normal sampling', uint16(handles.spot_avg),1); 


%== Show averaged function with over-sampling in ImageJ
function menu_imagej_os_Callback(hObject, eventdata, handles)
MIJ_start(hObject, eventdata, handles)
MIJ.createImage('Matlab: PSF with normal sampling', uint16(handles.spot_avg_os),1); 


%== Show averaged function with over-sampling in ImageJ
function menu_avg_ij_constructed_Callback(hObject, eventdata, handles)
MIJ_start(hObject, eventdata, handles)
MIJ.createImage('Matlab: PSF with normal sampling', uint16(handles.psf_rec),1); 


%= Function to start MIJ
function MIJ_start(hObject, eventdata, handles)
if isfield(handles,'flag_MIJ')
    if handles.flag_MIJ == 0
       MIJ.start(fullfile(handles.path_imagej,'ImageJ'))                           % Start MIJ/ImageJ by running the Matlab command: MIJ.start("imagej-path")
       handles.flag_MIJ = 1;
    end
else
    MIJ.start(fullfile(handles.path_imagej,'ImageJ'))                           % Start MIJ/ImageJ by running the Matlab command: MIJ.start("imagej-path")
    handles.flag_MIJ = 1;
end
guidata(hObject, handles);


% =========================================================================
% TxSite quantification
% =========================================================================


%== Settings of quantification
function menu_settings_TS_Callback(hObject, eventdata, handles)
handles.TS_quant = FQ_TS_settings_modify_v1(handles.TS_quant);
status_update(hObject, eventdata, handles,{'  ';'## Options for transcription site quantification modified'});         
guidata(hObject, handles);


%== Load image of PSF
function button_load_settings_TxSite_Callback(hObject, eventdata, handles)
[file_name_settings_TS,path_name_settings_TS] = uigetfile({'*.txt'},'Select file with settings for TS quantification');

if file_name_settings_TS ~= 0

    %- Load settings
    handles = FQ_TS_settings_load_v1(fullfile(path_name_settings_TS,file_name_settings_TS),handles);   
    handles.file_name_settings_TS = file_name_settings_TS;
    handles.path_name_settings_TS = path_name_settings_TS;   
    handles.status_settings_TS = 1; 
    guidata(hObject, handles);    
    
    %- Update status
    set(handles.text_status_PSF,'String','Settings loaded')
    set(handles.text_status_PSF,'ForegroundColor','g')
    controls_enable(hObject, eventdata, handles)
    
end


%== Analyze PSF
function button_analyze_settings_TxSite_Callback(hObject, eventdata, handles)

%== Analyze PSF
handles = FQ_PSF_analyze_v1(handles); 

%- Update status
set(handles.text_PSF_analyze,'String','PSF analyzed')
set(handles.text_PSF_analyze,'ForegroundColor','g')
handles.status_settings_TS_proc = 1; 
guidata(hObject, handles);
controls_enable(hObject, eventdata, handles)

%- Update status
text_update = {'  '; ...
               '## PSF analysis finished'};
status_update(hObject, eventdata, handles,text_update);         
  


%== Define amplitudes of individual mRNA
function button_PSF_amp_Callback(hObject, eventdata, handles)

choice = questdlg('Use current value OR load from file', 'Amplitudes of mRNA', 'Current analysis','File','Current analysis');

if not(strcmp(choice,''))

    switch (choice)
        case 'Current analysis'
            spots_fit = handles.spots_fit_all;
            thresh.in = handles.thresh_all.in;            
  
        case 'File'

            [file_name_results,path_name_results] = uigetfile({'*.txt'},'Select file with results of spot detection','MultiSelect', 'off');
            
            handles.AMP_file_name = file_name_results;
            handles.AMP_path_name = path_name_results;            
            
            if file_name_results ~= 0 
                [cell_prop file_name_image par_microscope file_name_settings file_name_image_filtered] = FISH_QUANT_load_results_v7(fullfile(path_name_results,file_name_results));
                spots_fit = cell_prop(1).spots_fit;
                thresh.in = logical(cell_prop(1).thresh.in);
            else
                spots_fit = [];
            end
    end
        
    if not(isempty(spots_fit))
        
        handles.mRNA_prop = FQ_AMP_analyze_v1(spots_fit,thresh,handles.axes_image); 

        %- Assigng fitted size
        handles.mRNA_prop.sigma_xy       = handles.PSF_OS_fit.sigma_xy;
        handles.mRNA_prop.sigma_z        = handles.PSF_OS_fit.sigma_z;
        
        %- Save data
        handles.status_AMP = 1;
        guidata(hObject, handles);
        controls_enable(hObject, eventdata, handles)
        
        %- Update status
        set(handles.text_AMP,'String','AMPs defined')
        set(handles.text_AMP,'ForegroundColor','g')

        %- Update status
        text_update = {'  '; ...
                       '## Amplitudes defined'; ...
                       'Fit with skewed normal distribution'; ...
                       ['Mean:     ', num2str(handles.mRNA_prop.amp_mean )]; ...
                       ['Sigma:    ', num2str(handles.mRNA_prop.amp_sigma)]; ...
                       ['Skewness: ', num2str(handles.mRNA_prop.amp_skew)]; ...
                       ['Kurtosis: ', num2str(handles.mRNA_prop.amp_kurt)]};        
        status_update(hObject, eventdata, handles,text_update);

        
    end
                
end
    

%== Quantify transcription site
function button_process_TxSite_Callback(hObject, eventdata, handles)    

status_save = get(handles.status_save_results_TxSite_quant,'Value');

%== Parameters for quantification
status_auto_detect = get(handles.status_autodetect,'Value');

if status_auto_detect
    int_th = str2double(get(handles.text_th_auto_detect,'String'));

    %- Parameters
    parameters_detect.int_th       = int_th;
    parameters_detect.conn         = 18;
    parameters_detect.flags.output = 1;
    parameters_detect.crop_image   = handles.parameters_quant.crop_image;
    parameters_detect.pixel_size   = handles.par_microscope.pixel_size;
end

%== Background
status_bgd = get(handles.button_TS_bgd,'Value');

if status_bgd == 1
    parameters_quant.flags.bgd_local = 0;
    parameters_quant.BGD.amp = str2double(get(handles.txt_TS_bgd,'String'));   
end

%== Get parameters
PSF_shift        = handles.PSF_shift;

par_microscope   = handles.par_microscope;
fact_os          = handles.fact_os;
parameters_quant = handles.parameters_quant;

pixel_size_os.xy  = par_microscope.pixel_size.xy / fact_os.xy;
pixel_size_os.z   = par_microscope.pixel_size.z  / fact_os.z;

parameters_quant.flags.output = 0;

%=== Parameters for quantificaiton
parameters_quant.pixel_size          = par_microscope.pixel_size;
parameters_quant.pixel_size_os       = pixel_size_os;
parameters_quant.N_mRNA_analysis_MAX = [];
parameters_quant.fact_os             = fact_os;
parameters_quant.par_microscope      = par_microscope;
parameters_quant.pad_image           = [];

%= mRNA properties
parameters_quant.mRNA_prop                = handles.mRNA_prop;
parameters_quant.mRNA_prop.pix_brightest  = 200;  % Needs to be implemented

%= FLAGS for QUANTIFICATION
parameters_quant.flags.parallel    = get(handles.checkbox_parallel_computing,'Value');

%- Update status
status_text = {' ';'== Transcription site quantification: STARTED.' ; '   See Workspace for details.'};
status_update(hObject, eventdata, handles,status_text); 

file_list = get(handles.listbox_files,'String');
path_name = handles.path_name_image;
N_file = size(file_list,1);
TS_counter = 1;

%-- Loop over all files
for i_file = 1:size(file_list,1)
    
    file_name_load      = file_list{i_file};
    file_name_load_full = fullfile(handles.path_name_image,file_name_load);
    
    disp(' ')
    disp('++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++');
    disp(['+++ Processing file ', num2str(i_file), ' of ', num2str(N_file)]);
    
    
    %== Determine what type of file we have
    [pathstr, name_file, ext] = fileparts(file_name_load);


    %-- Load data from outline definition file
    if strcmpi(ext,'.txt')
        [cell_prop file_name_image dum file_name_settings file_name_image_filtered] = FISH_QUANT_load_results_v7(file_name_load_full);    
        image_struct = load_stack_data_v4(fullfile(path_name,file_name_image));

    %- Load image files 
    elseif strcmpi(ext,'.tif') || strcmpi(ext,'.stk')
        image_struct = load_stack_data_v4(file_name_load_full);
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
    
    N_cell = length(cell_prop);

   if status_auto_detect
       parameters_detect.cell_prop = cell_prop;
       cell_prop = TxSite_detect_v1(image_struct,parameters_detect);
   end
    
    %== [2] Loop over all cells and all TxSites
    for ind_cell = 1:N_cell
         disp(' ') 
         disp(' ')
         disp(['+++ Cell ', num2str(ind_cell), ' of ', num2str(N_cell)]);
         
         if status_save
            folder_new = [handles.path_name_image,name_file,'_',datestr(date,'yymmdd')];
            
            is_dir = exist(folder_new,'dir'); 
            
            if is_dir == 0
                mkdir(folder_new)
            end
            
            cd(folder_new)
         end
         
         
         pos_TS_all = cell_prop(ind_cell).pos_TS;
         
         N_TS = length(pos_TS_all);
         
         for ind_TS = 1: N_TS
            disp(' ')
            disp(['+++ TxSite ', num2str(ind_TS), ' of ', num2str(N_TS)]);
              
            %- Get transcription site and analyze
            pos_TS = pos_TS_all(ind_TS); 
            [TxSite_quant TxSite_SIZE REC_prop TS_analysis_results] = TS_quant_v5(image_struct,pos_TS,PSF_shift,parameters_quant);
    
            %== [3] Save results in summary file
            TS_summary(TS_counter).file_name_image = file_name_image;
            TS_summary(TS_counter).TxSite_quant    = TxSite_quant;
            TS_summary(TS_counter).TxSite_SIZE     = TxSite_SIZE;
            TS_summary(TS_counter).cell_label      = cell_prop(ind_cell).label;
            TS_summary(TS_counter).TS_label        = pos_TS.label;
            TS_counter = TS_counter +1;
            
            
            if status_save
                parameters.file_name_save_REC = [pos_TS.label , '__REC.tif'];
                parameters.file_name_save_RES = [pos_TS.label , '__RESIDUAL.tif'];
                TxSite_quant_save_results_v1( REC_prop, parameters);
            end
            
            
            
         end
    end
    
    if status_save
        cd(handles.path_name_image)
        if status_auto_detect
            
            %=== Save new outline definition
            file_name_OUTLINE   = [name_file,'_outline_TxS_auto.txt'];
            file_name_OUTLINE_full = fullfile(handles.path_name_image,file_name_OUTLINE);

            %- Assign parameters which should be saved
            struct_save.par_microscope           = par_microscope;
            struct_save.cell_prop                = cell_prop;
            struct_save.version                  = handles.version;
            struct_save.file_name_image          = file_name_image;
            struct_save.file_name_image_filtered = [];
            struct_save.file_name_settings       = [];
            struct_save.path_name_image          = handles.path_name_image;

            %- Save settings    
            FISH_QUANT_save_outline_v5(struct_save,file_name_OUTLINE_full);
            
            
            %=== Folder with all results

        
        end
    end
    
end

%- Update status
status_text = {' ';'== Transcription site quantification: FINISHED'};
status_update(hObject, eventdata, handles,status_text); 

handles.TS_summary = TS_summary;
guidata(hObject, handles); 



% =========================================================================
% PLOTS
% =========================================================================

%=== Plot-histogram of all values
function handles = plot_hist_all(handles,axes_select)

thresh_all = handles.thresh_all;

if isempty(axes_select)
    figure
    hist(thresh_all.values(:),25); 
    v = axis;
    hold on
         plot([thresh_all.min_hist thresh_all.min_hist] , [0 1e5],'r');
         plot([thresh_all.max_hist thresh_all.max_hist] , [0 1e5],'g');
    hold off
    axis(v);
    colormap jet; 
    
% Handles for min and max line are returned for slider callback function    
else
    axes(axes_select); 
    hist(thresh_all.values(:),25); 
    h = findobj(axes_select);
    v = axis;
    hold on
         handles.h_hist_min = plot([thresh_all.min_hist thresh_all.min_hist] , [0 1e5],'r');
         handles.h_hist_max = plot([thresh_all.max_hist thresh_all.max_hist] , [0 1e5],'g');
    hold off
    axis(v);
    colormap jet;
    freezeColors;
    set(h,'ButtonDownFcn',@axes_histogram_all_ButtonDownFcn);   % Button-down function has to be set again
end
 
title(strcat('Total # of spots: ',sprintf('%d' ,length(thresh_all.in_old) )),'FontSize',9);    
    

%=== Plot-histogram of thresholded parameters
function handles = plot_hist_th(handles,axes_select)

thresh_all     = handles.thresh_all;

if isempty(axes_select)
    figure
    hist(thresh_all.values(handles.thresh_all.in_display),25); 
    v = axis;
    hold on
         plot([thresh_all.min_hist thresh_all.min_hist] , [0 1e5],'r');
         plot([thresh_all.max_hist thresh_all.max_hist] , [0 1e5],'g');
    hold off
    axis(v);
    colormap jet; 
    
% Handles for min and max line are returned for slider callback function    
else
    axes(axes_select); 
    hist(thresh_all.values(thresh_all.in_display),25); 
    h = findobj(axes_select);
    v = axis;
    hold on
         handles.h_hist_th_min = plot([thresh_all.min_hist thresh_all.min_hist] , [0 1e5],'r');
         handles.h_hist_th_max = plot([thresh_all.max_hist thresh_all.max_hist] , [0 1e5],'g');
    hold off
    axis(v);
    colormap jet;
    freezeColors;
    set(h,'ButtonDownFcn',@axes_histogram_th_ButtonDownFcn);   % Button-down function has to be set again
end
    
title(strcat('Selected # of spots ',sprintf('%d' ,sum(thresh_all.in_display) )),'FontSize',9);     



% =========================================================================
% VARIOUS FUNCTIONS
% =========================================================================

%== Activate parallel computing
function checkbox_parallel_computing_Callback(hObject, eventdata, handles)

flag_parallel = get(handles.checkbox_parallel_computing,'Value');

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


% =========================================================================
% Functions without function
% =========================================================================

function varargout = FISH_QUANT_batch_OutputFcn(hObject, eventdata, handles) 
varargout{1} = handles.output;

function listbox_files_Callback(hObject, eventdata, handles)

function listbox_files_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function checkbox2_Callback(hObject, eventdata, handles)

function text_results_file_suffix_Callback(hObject, eventdata, handles)

function text_results_file_suffix_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function listbox_status_Callback(hObject, eventdata, handles)

function listbox_status_CreateFcn(hObject, eventdata, handles)

if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_status_2_Callback(hObject, eventdata, handles)

function text_status_2_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_psf_fit_sigmaX_Callback(hObject, eventdata, handles)

function text11_Callback(hObject, eventdata, handles)

function text_psf_fit_sigmaZ_Callback(hObject, eventdata, handles)

function text_psf_bgd_Callback(hObject, eventdata, handles)

function popup_psf_spots_select_Callback(hObject, eventdata, handles)

function popup_psf_spots_select_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_results_file_suffix_fixed_Callback(hObject, eventdata, handles)

function text_results_file_suffix_fixed_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_results_file_summary_Callback(hObject, eventdata, handles)

function text_results_file_summary_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function menu_avg_Callback(hObject, eventdata, handles)

function menu_load_save_Callback(hObject, eventdata, handles)

function menu_imagej_Callback(hObject, eventdata, handles)

function menu_avg_construct_Callback(hObject, eventdata, handles)

function checkbox_use_filtered_Callback(hObject, eventdata, handles)

function checkbox_save_filtered_Callback(hObject, eventdata, handles)

function pop_up_threshold_CreateFcn(hObject, eventdata, handles)

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

function text_th_min_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_th_max_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_file_name_settings_Callback(hObject, eventdata, handles)

function text_file_name_settings_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function checkbox7_Callback(hObject, eventdata, handles)

function button_PSF_define_model_Callback(hObject, eventdata, handles)

function popup_placement_Callback(hObject, eventdata, handles)

function popup_placement_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function popup_residuals_Callback(hObject, eventdata, handles)

function popup_residuals_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function checkbox8_Callback(hObject, eventdata, handles)

function Untitled_1_Callback(hObject, eventdata, handles)

function Untitled_2_Callback(hObject, eventdata, handles)

function checkbox_parallel_computing1_Callback(hObject, eventdata, handles)

function text_th_auto_detect_Callback(hObject, eventdata, handles)

function text_th_auto_detect_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function status_autodetect_Callback(hObject, eventdata, handles)

function button_TS_bgd_Callback(hObject, eventdata, handles)

function txt_TS_bgd_Callback(hObject, eventdata, handles)

function txt_TS_bgd_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function status_save_results_TxSite_quant_Callback(hObject, eventdata, handles)

function menu_settings_detect_Callback(hObject, eventdata, handles)
