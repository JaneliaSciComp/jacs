function varargout = FISH_QUANT_TxSite(varargin)
% FISH_QUANT_TXSITE MATLAB code for FISH_QUANT_TxSite.fig
% Last Modified by GUIDE v2.5 13-Sep-2011 14:27:25

% Begin initialization code - DO NOT EDIT
gui_Singleton = 1;
gui_State = struct('gui_Name',       mfilename, ...
                   'gui_Singleton',  gui_Singleton, ...
                   'gui_OpeningFcn', @FISH_QUANT_TxSite_OpeningFcn, ...
                   'gui_OutputFcn',  @FISH_QUANT_TxSite_OutputFcn, ...
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


% --- Executes just before FISH_QUANT_TxSite is made visible.
function FISH_QUANT_TxSite_OpeningFcn(hObject, eventdata, handles, varargin)

%- Set font-size to 10
%  For whatever reason are all the fonts on windows are set back to 8 when the .fig is openend
h_font_8 = findobj(handles.h_FQ_TxSite,'FontSize',8);
set(h_font_8,'FontSize',10)

%- Get installation directory of FISH-QUANT and initiate 
p = mfilename('fullpath');        
handles.FQ_path = fileparts(p); 
handles         = FISH_QUANT_start_up_v2(handles);

%- Change name of GUI
set(handles.h_FQ_TxSite,'Name', ['FISH-QUANT ', handles.version, ': TxSite quantification']);

%- Export figure handle to workspace - will be used in Close All button of main Interface
assignin('base','h_TxSite',handles.h_FQ_TxSite)


%=== Options for TxSite quantification
handles.parameters_quant = FQ_TS_settings_init_v1;

%- Some parameters
handles.status_PAR   = 0;
handles.status_PSF   = 0;
handles.status_BGD   = 0;
handles.status_AMP   = 0;
handles.status_PROC  = 0;
handles.status_QUANT = 0;
handles.status_QUANT_ALL = 0;
handles.status_auto_detect = 0;
handles.cell_prop    = {};

% File-names for PSF and BGD

handles.PSF_path_name = [];
handles.PSF_file_name = [];
handles.BGD_path_name = [];
handles.BGD_file_name = [];
handles.AMP_path_name = [];
handles.AMP_file_name = [];


%- Default for oversampling
handles.fact_os.xy = 3;
handles.fact_os.z  = 3;

%- Check if called from another GUI

if not(isempty(varargin))
    
    if      strcmp( varargin{1},'HandlesMainGui') 
        %- Read data from Main GUI
        handles_MAIN = varargin{2};
        handles.cell_prop          = handles_MAIN.cell_prop;   
        
        if not(isempty(handles.cell_prop))
            ind_cell           = get(handles_MAIN.pop_up_outline_sel_cell,'Value');
            str_cells          = get(handles_MAIN.pop_up_outline_sel_cell,'String');

            handles.par_microscope     = handles_MAIN.par_microscope;

            handles.file_name_image    = handles_MAIN.file_name_image;
            handles.path_name_image    = handles_MAIN.path_name_image;
            handles.file_name_settings = handles_MAIN.file_name_settings;
            handles.file_name_image_filtered = handles_MAIN.file_name_image_filtered;

            %- Get image data
            handles.image_struct = handles_MAIN.image_struct;  

            %- Save everything
            guidata(hObject, handles); 

            set(handles.text_data,'String','IMG defined')
            set(handles.text_data,'ForegroundColor','g')
            
            set(handles.pop_up_outline_sel_cell,'String',str_cells);
            set(handles.pop_up_outline_sel_cell,'Value',ind_cell);
            
            %- Analyze selected cell and plot
            handles = analyze_cellprop(hObject, eventdata, handles);
            pop_up_outline_sel_cell_Callback(hObject, eventdata, handles);
            plot_image(handles,handles.axes_main);
            
        end
    end
end


% Update handles structure
handles.output = hObject;
guidata(hObject, handles);
enable_controls(hObject, eventdata, handles)


% --- Outputs from this function are returned to the command line.
function varargout = FISH_QUANT_TxSite_OutputFcn(hObject, eventdata, handles) 
varargout{1} = handles.output;



%==========================================================================
%==== TxSite quantification: enable
%==========================================================================

function enable_controls(hObject, eventdata, handles)

%- Enable processing of PSF
if   not(isempty(handles.cell_prop))
    set(handles.pop_up_outline_sel_cell,'Enable','on');
    set(handles.pop_up_outline_sel_TS,'Enable','on');
else
    set(handles.pop_up_outline_sel_cell,'Enable','off');   
    set(handles.pop_up_outline_sel_TS,'Enable','off');   
end

%- Enable processing of PSF
if   handles.status_PSF && handles.status_BGD && handles.status_PAR
    set(handles.button_analyze_PSF,'Enable','on');
else
    set(handles.button_analyze_PSF,'Enable','off');   
end


%- Enable processing
if   handles.status_PROC && handles.status_AMP
    set(handles.button_process,'Enable','on');
    set(handles.button_process_all,'Enable','on');
else
    set(handles.button_process,'Enable','off');   
    set(handles.button_process_all,'Enable','off');   
end


%- Enable processing
if   handles.status_QUANT
    set(handles.button_visualize_results,'Enable','on');    
else
    set(handles.button_visualize_results,'Enable','off');    
end

%- Enable processing
if   handles.status_QUANT_ALL
    set(handles.menu_save_quantification,'Enable','on');    
else
    set(handles.menu_save_quantification,'Enable','off');    
end


%==========================================================================
%==== Load image data
%==========================================================================

function button_load_data_Callback(hObject, eventdata, handles)

[file_name_results,path_name_results] = uigetfile({'*.txt'},'Select file');

if file_name_results ~= 0
       
    %- Load results
    [cell_prop file_name par_microscope file_name_settings file_name_image_filtered] = FISH_QUANT_load_results_v7(fullfile(path_name_results,file_name_results));
    
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
            
    %- Analyze detected regions
    status_update(hObject, eventdata, handles,{'Outlines loaded.'})           
    handles = analyze_cellprop(hObject, eventdata, handles);   
    
    %- Save everything
    guidata(hObject, handles); 
    
    %- Update status
    set(handles.text_data,'String','IMG defined')
    set(handles.text_data,'ForegroundColor','g')    
        
    %- Analyze selected cell and plot
    pop_up_outline_sel_cell_Callback(hObject, eventdata, handles)
    plot_image(handles,handles.axes_main)
end


%= Function to analyze detected regions
function handles = analyze_cellprop(hObject, eventdata, handles)

cell_prop = handles.cell_prop;

%- Populate pop-up menu with labels of cells
N_cell = size(cell_prop,2);

if N_cell > 0

    %- Call pop-up function to show results and bring values into GUI
    for i = 1:N_cell
        str_menu{i,1} = cell_prop(i).label;
        
        %- Analyze transcription sites
        pos_TS = cell_prop(i).pos_TS;       
        N_TS   =  size(pos_TS,2);
        
        if N_TS == 0
            str_menu_TS = {' '};
        else
            str_menu_TS = {};

            for i_TS = 1:N_TS
                str_menu_TS{i_TS,1} = pos_TS(i_TS).label; 
                cell_prop(i).pos_TS(i_TS).status_QUANT = 0;
            end    
        end
        
        cell_prop(i).str_menu_TS = str_menu_TS;
    end  
else
    str_menu = {' '};
end

%- Save everything
handles.cell_prop = cell_prop;
guidata(hObject, handles); 

%- Save and analyze results
set(handles.pop_up_outline_sel_cell,'String',str_menu);
set(handles.pop_up_outline_sel_cell,'Value',1);

set(handles.pop_up_outline_sel_TS,'String',cell_prop(1).str_menu_TS);
set(handles.pop_up_outline_sel_TS,'Value',1);

    
%- Enable outline selection
enable_controls(hObject, eventdata, handles)
status_update(hObject, eventdata, handles,{'Outlines analyzed.'})        


%==========================================================================
%==== Settings
%==========================================================================

%=== Load settings
function menu_load_settings_Callback(hObject, eventdata, handles)

[file_name_settings_TS,path_name_settings_TS] = uigetfile({'*.txt'},'Select file with settings for TS quantification');

if file_name_settings_TS ~= 0

    %- Load settings
    handles = FQ_TS_settings_load_v1(fullfile(path_name_settings_TS,file_name_settings_TS),handles);   
    handles.file_name_settings_TS = file_name_settings_TS;
    handles.path_name_settings_TS = path_name_settings_TS;   
    
    %- Update status   
    if isfield(handles, 'par_microscope') && isfield(handles, 'fact_os') 
        set(handles.text_PSF_par,'String','PAR defined')
        set(handles.text_PSF_par,'ForegroundColor','g')
        handles.status_PAR = 1;       
    end   
    
    if isfield(handles, 'PSF_file_name')
        set(handles.text_PSF_img,'String','PSF defined')
        set(handles.text_PSF_img,'ForegroundColor','g')
        handles.status_PSF = 1;       
    end

   if isfield(handles, 'bgd_value') || isfield(handles, 'BGD_file_name') 
        set(handles.text_PSF_bgd,'String','BGD defined')
        set(handles.text_PSF_bgd,'ForegroundColor','g')
        handles.status_BGD = 1;        
   end
     
   if handles.parameters_quant.flags.bgd_local
       set(handles.button_TS_bgd,'Value',0);
   else
       set(handles.button_TS_bgd,'Value',1);
       set(handles.txt_TS_bgd,'String',num2str(handles.parameters_quant.BGD.amp));
   end
      
   if handles.FLAG_auto_detect
       set(handles.text_th_auto_detect,'String',num2str(handles.parameters_auto_detect.int_th));
   end
           
    %- Update status
    guidata(hObject, handles); 
    enable_controls(hObject, eventdata, handles)    
end


%=== Save settings
function menu_save_settings_Callback(hObject, eventdata, handles)
[handles.file_name_settings handles.path_name_settings] = FQ_TS_settings_save_v1([],handles);
guidata(hObject, handles);



%==========================================================================
%==== TxSite quantification: parameters, options, and quantificatiion
%==========================================================================

%=== Define parameters
function button_par_PSF_Callback(hObject, eventdata, handles)

par_microscope = handles.par_microscope;
fact_os        = handles.fact_os;

dlgTitle = 'Experimental parameters';

prompt(1) = {'Oversampling XY'};
prompt(2) = {'Oversampling Z'};
prompt(3) = {'Pixel-size xy [nm]'};
prompt(4) = {'Pixel-size z [nm]'};
prompt(5) = {'Refractive index'};
prompt(6) = {'Numeric aperture NA'};
prompt(7) = {'Emission wavelength'};
prompt(8) = {'Excitation wavelength'};
prompt(9) = {'Microscope'};


defaultValue{1} = num2str(fact_os.xy);
defaultValue{2} = num2str(fact_os.z);
defaultValue{3} = num2str(par_microscope.pixel_size.xy);
defaultValue{4} = num2str(par_microscope.pixel_size.z);
defaultValue{5} = num2str(par_microscope.RI);
defaultValue{6} = num2str(par_microscope.NA);
defaultValue{7} = num2str(par_microscope.Em);
defaultValue{8} = num2str(par_microscope.Ex);
defaultValue{9} = num2str(par_microscope.type);

userValue = inputdlg(prompt,dlgTitle,1,defaultValue);

if( ~ isempty(userValue))
    fact_os.xy                   = str2double(userValue{1});
    fact_os.z                    = str2double(userValue{2});
    par_microscope.pixel_size.xy = str2double(userValue{3});
    par_microscope.pixel_size.z  = str2double(userValue{4});   
    par_microscope.RI            = str2double(userValue{5});   
    par_microscope.NA            = str2double(userValue{6});
    par_microscope.Em            = str2double(userValue{7});   
    par_microscope.Ex            = str2double(userValue{8});
    par_microscope.type          = userValue{9}; 
        
    handles.par_microscope = par_microscope;
    handles.fact_os         = fact_os;
    handles.status_PAR = 1;
    
    
    %- Update status
    set(handles.text_PSF_par,'String','PAR defined')
    set(handles.text_PSF_par,'ForegroundColor','g')
    handles.status_PAR = 1;        

    %- Update status
    text_update = {'  '; '## Parameters defined'};        
    status_update(hObject, eventdata, handles,text_update);         
    guidata(hObject, handles); 
end


%=== Load image of PSF
function button_PSF_img_Callback(hObject, eventdata, handles)

[PSF_file_name,PSF_path_name] = uigetfile('.tif','Select 3D-image of PSF','MultiSelect','off');
if PSF_file_name ~= 0
    handles.PSF_file_name = PSF_file_name;
    handles.PSF_path_name = PSF_path_name; 
        
    %- Update status
    set(handles.text_PSF_img,'String','PSF defined')
    set(handles.text_PSF_img,'ForegroundColor','g')
    handles.status_PSF = 1;        

    %- Update status
    text_update = {'  '; '## PSF defined';handles.PSF_file_name};        
    status_update(hObject, eventdata, handles,text_update);         
    guidata(hObject, handles);     
end


%=== Define background
function button_PSF_bgd_Callback(hObject, eventdata, handles)


choice = questdlg('Background subtraction with file or scalar value?', 'Background subtraction', 'Scalar','File','Scalar');

if not(strcmp(choice,''))

    switch (choice)
        case 'Scalar'
            

            dlgTitle = 'Background';
            prompt(1) = {'Value'};
            defaultValue{1} = num2str(0);
            userValue = inputdlg(prompt,dlgTitle,1,defaultValue);

            if( ~ isempty(userValue))
                bgd_value = str2double(userValue{1});
            
                handles.BGD_file_name = [];
                handles.BGD_path_name = []; 
                
                handles.bgd_value = bgd_value;
                %- Update status
                set(handles.text_PSF_bgd,'String','BGD defined')
                set(handles.text_PSF_bgd,'ForegroundColor','g')
                handles.status_BGD = 1;        

                %- Update status
                text_update = {'  '; ['## BGD defined. Scalar: ', num2str(bgd_value)]};        
                status_update(hObject, eventdata, handles,text_update);         
                guidata(hObject, handles);
                
                
            end
            
      
        case 'File'
            [BGD_file_name,BGD_path_name] = uigetfile('.tif','Select 3D-image of BGD - cancel for no bgd','MultiSelect','off');

            if BGD_file_name ~= 0
                handles.BGD_file_name = BGD_file_name;
                handles.BGD_path_name = BGD_path_name; 

                %- Update status
                set(handles.text_PSF_bgd,'String','BGD defined')
                set(handles.text_PSF_bgd,'ForegroundColor','g')
                handles.status_BGD = 1;        

                %- Update status
                text_update = {'  '; '## BGD defined';handles.BGD_file_name};        
                status_update(hObject, eventdata, handles,text_update);         
                guidata(hObject, handles);
            end
            
    end

else
    handles.BGD_file_name = [];
    handles.BGD_path_name = []; 
    handles.bgd_value     = 0;      
    %- Update status
    set(handles.text_PSF_bgd,'String','NO BGD correction')
    set(handles.text_PSF_bgd,'ForegroundColor','g')
    handles.status_BGD = 1;        

    %- Update status
    text_update = {'  '; '## BGD will NOT be corrected'};        
    status_update(hObject, eventdata, handles,text_update);         
    guidata(hObject, handles); 
    
end


%=== Analyze PSF
function button_analyze_PSF_Callback(hObject, eventdata, handles)

handles = FQ_PSF_analyze_v1(handles); 

% ==== Update and save

%- Update status
set(handles.text_PROC,'String','PSF analyzed')
set(handles.text_PROC,'ForegroundColor','g')
handles.status_PROC = 1;        

%- Update status
text_update = {'  '; ...
               '## PSF analysis finished'};        
status_update(hObject, eventdata, handles,text_update);         
guidata(hObject, handles);         

                
%=== Define amplitude
function button_PSF_amp_Callback(hObject, eventdata, handles)

choice = questdlg('Use current value OR load from file', 'Amplitudes of mRNA', 'Current analysis','File','Current analysis');

if not(strcmp(choice,''))

    switch (choice)
        case 'Current analysis'
            
            if isempty(handles.cell_prop(1).spots_fit)                              
                warndlg('No results of spot detection in current analysis','PSF - define amplitude')
                return
            else
                
                ind_cell   = get(handles.pop_up_outline_sel_cell,'Value');
                spots_fit  = handles.cell_prop(ind_cell).spots_fit;
                thresh.in  = handles.cell_prop(ind_cell).thresh.in;
                handles.AMP_file_name = 'current analysis';
                handles.AMP_path_name = 'current analysis';
            end

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
        handles.mRNA_prop = FQ_AMP_analyze_v1(spots_fit,thresh,handles.axes_main); 
        
        %- Assigng fitted size
        handles.mRNA_prop.sigma_xy       = handles.PSF_OS_fit.sigma_xy;
        handles.mRNA_prop.sigma_z        = handles.PSF_OS_fit.sigma_z;

        %- Update status
        handles.status_AMP = 1; 
        guidata(hObject, handles);
        
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


%=== Auto-detect transcription site
function button_auto_detect_Callback(hObject, eventdata, handles)


int_th = str2double(get(handles.text_th_auto_detect,'String'));

%- Parameters
parameters.int_th       = int_th;
parameters.conn         = handles.parameters_quant.conn;
parameters.flags.output = 1;
parameters.crop_image   = handles.parameters_quant.crop_image;
parameters.pixel_size   = handles.par_microscope.pixel_size;
parameters.cell_prop    = handles.cell_prop;
parameters.min_dist     = handles.parameters_quant.min_dist;

%- Detect and analyse
handles.cell_prop        = TxSite_detect_v1(handles.image_struct,parameters);
handles                  = analyze_cellprop(hObject, eventdata, handles);
plot_image(handles,handles.axes_main);

%- Save results
handles.status_auto_detect = 1;
handles.parameters_auto_detect.int_th = parameters.int_th  ;
handles.parameters_auto_detect.conn = parameters.conn  ;

guidata(hObject, handles); 

%- Update status
status_text = {' ';'== Transcription site automatically detected'};
status_update(hObject, eventdata, handles,status_text);


%== BGD of transcription site
function button_TS_bgd_Callback(hObject, eventdata, handles)
status_button = get(handles.button_TS_bgd,'Value');

if status_button == 1
    set(handles.txt_TS_bgd,'Enable','on');
else
    set(handles.txt_TS_bgd,'Enable','off');    
end


%=== Quantify transcription site
function button_process_all_Callback(hObject, eventdata, handles)

list_cell  = get(handles.pop_up_outline_sel_cell,'String');
list_TS    = get(handles.pop_up_outline_sel_TS,'String');

N_cell = length(list_cell);
N_TS   = length(list_TS);

TS_counter = 1;

for i_cell = 1:N_cell
    for i_TS = 1:N_TS
        set(handles.pop_up_outline_sel_cell,'Value',i_cell);
        set(handles.pop_up_outline_sel_TS,'Value',i_TS);
        handles = button_process_Callback(hObject, eventdata, handles);
        
        
        %== Save results         
        TS_summary(TS_counter).file_name_image = handles.file_name_image;
        TS_summary(TS_counter).TxSite_quant    = handles.cell_prop(i_cell).pos_TS(i_TS).TxSite_quant;
        TS_summary(TS_counter).TxSite_SIZE     = handles.cell_prop(i_cell).pos_TS(i_TS).TxSite_SIZE;
        TS_summary(TS_counter).cell_label      = handles.cell_prop(i_cell).label;
        TS_summary(TS_counter).TS_label        = handles.cell_prop(i_cell).pos_TS(i_TS).label;
        TS_counter = TS_counter +1;
    end
end

%- Save results
handles.TS_summary = TS_summary;
handles.status_QUANT_ALL = 1;
guidata(hObject, handles); 
enable_controls(hObject, eventdata, handles)


%=== Quantify transcription site
function handles = button_process_Callback(hObject, eventdata, handles)

%== Get parameters
cell_prop        = handles.cell_prop;
image_struct     = handles.image_struct;
PSF_shift        = handles.PSF_shift;

par_microscope   = handles.par_microscope;
fact_os          = handles.fact_os;
parameters_quant = handles.parameters_quant;

pixel_size_os.xy  = par_microscope.pixel_size.xy / fact_os.xy;
pixel_size_os.z   = par_microscope.pixel_size.z  / fact_os.z;

%== Determine how results are shown
flag_plot = get(handles.checkbox_output,'Value');    % Can be a list - corresponding constructions will be plotted
%- Display only results
if flag_plot == 0
    parameters_quant.flags.output = 1;
%- Display results and show plots
else 
    parameters_quant.flags.output = 2;
end


%=== BGD
status_bgd = get(handles.button_TS_bgd,'Value');

if status_bgd == 1
    parameters_quant.flags.bgd_local = 0;
    parameters_quant.BGD.amp = str2num(get(handles.txt_TS_bgd,'String'));  
    
else
    parameters_quant.flags.bgd_local = 1;
end


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
status_text = {' ';'== Transcription site quantification: STARTED'};
status_update(hObject, eventdata, handles,status_text); 

ind_cell  = get(handles.pop_up_outline_sel_cell,'Value');
ind_TS    = get(handles.pop_up_outline_sel_TS,'Value');

pos_TS = cell_prop(ind_cell).pos_TS(ind_TS);    
 
[TxSite_quant TxSite_SIZE REC_prop TS_analysis_results] = TS_quant_v5(image_struct,pos_TS,PSF_shift,parameters_quant);

handles.cell_prop(ind_cell).pos_TS(ind_TS).TxSite_quant         = TxSite_quant;
handles.cell_prop(ind_cell).pos_TS(ind_TS).TxSite_SIZE          = TxSite_SIZE;
handles.cell_prop(ind_cell).pos_TS(ind_TS).REC_prop             = REC_prop;
handles.cell_prop(ind_cell).pos_TS(ind_TS).TS_analysis_results  = TS_analysis_results;  
handles.cell_prop(ind_cell).pos_TS(ind_TS).status_QUANT         = 1;
handles.status_QUANT = 1;
handles.parameters_quant = parameters_quant;

%- Save results
guidata(hObject, handles); 

%- Plot summary of quantification
axes(handles.axes_main)
cla(handles.axes_main,'reset')
plot(REC_prop.summary_Q_run_N_MRNA,REC_prop.Q_norm)
v = axis;
hold on
plot([0 max(REC_prop.summary_Q_run_N_MRNA)], [parameters_quant.factor_Q_ok parameters_quant.factor_Q_ok],'k')
hold off
box on
axis(v)    
xlabel('Number of placed mRNA')
ylabel('Quality score')
title(['Estimated # of nascent transcripts: ', num2str(round(TxSite_quant.N_mRNA_Q_limit.mean))])
 

%- Update status
status_text = {' ';'== Transcription site quantification: FINSIHED'};
status_update(hObject, eventdata, handles,status_text);


%=== Options for quantification
function menu_options_Callback(hObject, eventdata, handles)
handles.parameters_quant = FQ_TS_settings_modify_v1(handles.parameters_quant);
status_update(hObject, eventdata, handles,{'  ';'## Options modified'});         
guidata(hObject, handles);


%==========================================================================
%==== Save and plot
%==========================================================================


%=== Save results of quantification
function menu_save_quantification_Callback(hObject, eventdata, handles)

if isfield(handles,'TS_summary')
        
    %== Parameters
    parameters.path_name_image    = handles.path_name_image;
    parameters.file_name_settings = handles.file_name_settings_TS;
    parameters.version            = handles.version;
    parameters.file_name_default  = [];

    FISH_QUANT_batch_save_summary_TS_v3([],handles.TS_summary,parameters)
end


%=== Visualize results of TS quantification
function button_visualize_results_Callback(hObject, eventdata, handles)
MIJ_start(hObject, eventdata, handles)

ind_cell  = get(handles.pop_up_outline_sel_cell,'Value');
pos_TS    = handles.cell_prop(ind_cell).pos_TS;

i_TS    = get(handles.pop_up_outline_sel_TS,'Value');


label_TS = pos_TS(i_TS).label;
img_res  = pos_TS(i_TS).REC_prop.img_res;
img_TS   =  pos_TS(i_TS).TS_analysis_results.img_TS_crop_xyz;
img_fit  = pos_TS(i_TS).REC_prop.img_fit;

%- Plot TS and Fit
MIJ.createImage('TS_img', uint16(img_TS),1);
MIJ.createImage('TS_fit', uint16(img_fit),1);

%-Combine stacks next to each other, rename and autoscale
MIJ.run('Combine...', 'stack1=TS_img stack2=TS_fit');

label_img = ['FQ: ',label_TS , ' : LEFT: image | RIGHT: fit' ];
string_rename = ['rename("',label_img,'")'];
ij.IJ.runMacro(string_rename);    

ij.IJ.setSlice(round(size(img_res,3)/2))
MIJ.run('Enhance Contrast', 'saturated=0.35');
MIJ.run('In');MIJ.run('In');MIJ.run('In');MIJ.run('In');MIJ.run('In');MIJ.run('In');MIJ.run('In');MIJ.run('In');

%- Residuals

img_res_pos = img_res.*(img_res > 0); 
img_res_neg = img_res.*(img_res < 0)*(-1);

MIJ.createImage('res_pos', uint16(img_res_pos),1);   
MIJ.createImage('res_neg', uint16(img_res_neg),1);


title_resid = ['FQ: ',label_TS, ' : resid of fit - red:pos - green:neg'];
MIJ.run('Concatenate...', ['stack1=[res_pos] stack2=[res_neg] title=[', title_resid , ']']);
MIJ.run('Stack to Hyperstack...', ['order=xyzct channels=2 slices=',num2str(size(img_res,3)) ,' frames=1 display=Composite']);
MIJ.run('In');MIJ.run('In');MIJ.run('In');MIJ.run('In');MIJ.run('In');MIJ.run('In');MIJ.run('In');MIJ.run('In');

ij.IJ.runMacro('Stack.setChannel(1)');
ij.IJ.runMacro('run("Red")');

ij.IJ.runMacro('Stack.setChannel(2)');
ij.IJ.runMacro('run("Green")');   
    



%==========================================================================
%==== VISUALIZATION
%==========================================================================

%==========================================================================
%==== Functions for the different plots

%=== Image with position of detected spots
function plot_image(handles,axes_select)

ind_cell  = get(handles.pop_up_outline_sel_cell,'Value');
image     = handles.image_struct.data;
cell_prop = handles.cell_prop;
            
%- Calculate maximum projection of loaded image
img_plot = max(image,[],3); 

%- 1. Plot image
if isempty(axes_select)
    figure
    imshow(img_plot,[]);
else
    axes(axes_select);
    cla(axes_select,'reset')
    h = imshow(img_plot,[]);
end

title('Maximum projection of loaded image','FontSize',9);
colormap(hot)

   
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
        x = cell_prop(ind_cell).x;
        y = cell_prop(ind_cell).y;
        plot([x,x(1)],[y,y(1)],'g','Linewidth', 2)  

        %- TS
        pos_TS   = handles.cell_prop(ind_cell).pos_TS;  
        if not(isempty(pos_TS))
            
            for i = 1:size(pos_TS,2)    
                
                x = pos_TS(i).x;
                y = pos_TS(i).y;
                plot([x,x(1)],[y,y(1)],'b','Linewidth', 2)  

            end  
        
            %- Plot selected cell in different color
            ind_TS  = get(handles.pop_up_outline_sel_TS,'Value');
            x = pos_TS(ind_TS).x;
            y = pos_TS(ind_TS).y;
            plot([x,x(1)],[y,y(1)],'g','Linewidth', 2)  
        
        end 
    end        
end
hold off
    

%==========================================================================
%==== Various Functions
%==========================================================================


%=== Parallel computing
function checkbox_parallel_computing_Callback(hObject, eventdata, handles)

flag_parallel = get(handles.checkbox_parallel_computing,'Value');

if exist('matlabpool')

    %- Parallel computing - open MATLAB session for parallel computation 
    if flag_parallel == 1    
        isOpen = matlabpool('size') > 0;
        if (isOpen==0)
            
            set(handles.h_FQ_TxSite,'Pointer','watch');
            %- Update status
            status_text = {' ';'== STARTING matlabpool for parallel computing ... please wait ... '};
            status_update(hObject, eventdata, handles,status_text);

            matlabpool open;

            %- Update status
            status_text = {' ';'    ... STARTED'};
            status_update(hObject, eventdata, handles,status_text);        
            set(handles.h_FQ_TxSite,'Pointer','arrow');
        end

    %- Parallel computing - close MATLAB session for parallel computation     
    else
        isOpen = matlabpool('size') > 0;
        if (isOpen==1)
            
            set(handles.h_FQ_TxSite,'Pointer','watch');
            %- Update status
            status_text = {' ';'== STOPPING matlabpool for parallel computing ... please wait ... '};
            status_update(hObject, eventdata, handles,status_text);

            matlabpool close;

            %- Update status
            status_text = {' ';'    ... STOPPED'};
            status_update(hObject, eventdata, handles,status_text);
            set(handles.h_FQ_TxSite,'Pointer','arrow');
        end
    end
    
else
    warndlg('Parallel toolbox not available','FISH_QUANT')
    set(handles.checkbox_parallel_computing,'Value',0);
end


%== Update status
function status_update(hObject, eventdata, handles,status_text)
status_old = get(handles.list_box_status,'String');
status_new = [status_old;status_text];
set(handles.list_box_status,'String',status_new)
set(handles.list_box_status,'ListboxTop',round(size(status_new,1)))
drawnow
enable_controls(hObject, eventdata, handles)
guidata(hObject, handles); 


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


%= Pop-up to select cells
function pop_up_outline_sel_cell_Callback(hObject, eventdata, handles)

ind_cell    = get(handles.pop_up_outline_sel_cell,'Value');
str_menu_TS = handles.cell_prop(ind_cell).str_menu_TS;

set(handles.pop_up_outline_sel_TS,'String',str_menu_TS);
set(handles.pop_up_outline_sel_TS,'Value',1);

pop_up_outline_sel_TS_Callback(hObject, eventdata, handles) 
       

%= Pop-up to select TxSite
function pop_up_outline_sel_TS_Callback(hObject, eventdata, handles)           
ind_cell  = get(handles.pop_up_outline_sel_cell,'Value');
ind_TS    = get(handles.pop_up_outline_sel_TS,'Value');

if not(isempty(handles.cell_prop(ind_cell).pos_TS))

    handles.status_QUANT = handles.cell_prop(ind_cell).pos_TS(ind_TS).status_QUANT;

    %- Save results
    guidata(hObject, handles); 
    enable_controls(hObject, eventdata, handles)
    plot_image(handles,handles.axes_main)   


    if handles.status_QUANT
        TxSite_quant        = handles.cell_prop(ind_cell).pos_TS(ind_TS).TxSite_quant;
        TxSite_SIZE         = handles.cell_prop(ind_cell).pos_TS(ind_TS).TxSite_SIZE;
        REC_prop            = handles.cell_prop(ind_cell).pos_TS(ind_TS).REC_prop;
        %- Update status
        status_text = { ' ';
                        '== Results of quantification ... ' ; ...

                       ['# of nascent mRNA [RANGE of Q-score]: ' , num2str( TxSite_quant.N_mRNA_Q_limit.mean)    , ...
                              '+/-'                              , num2str( TxSite_quant.N_mRNA_Q_limit.stdev)]  ; ...

                       ['# of nascent mRNA [Best 10%]        : ' , num2str(TxSite_quant.N_mRNA_TS_mean_10per), ...
                              '+/-'                              , num2str(TxSite_quant.N_mRNA_TS_std_10per)]; ...
                              
                        ['# of nascent mRNA [Integrated int] : ' , num2str(TxSite_quant.N_mRNA_integrated_int)]; ...

                        ' '; ...
                       ['Dist AVG [CENTERED DATA]            : ', num2str(TxSite_SIZE.dist_avg_shift,'%10.0f'),' nm']};
         status_update(hObject, eventdata, handles,status_text);          

         %- Plot results if specified
         flag_plot = get(handles.checkbox_output,'Value');    % Can be a list - corresponding constructions will be plotted

         if flag_plot  
             parameters.flags.output = 2;
             parameters.factor_Q_ok  = handles.parameters_quant.factor_Q_ok;         
             TxSite_reconstruct_Output_v1(TxSite_quant, REC_prop, parameters)
         end

    else
        status_text = { ' '; 'Site is not quantified .... ' };
        status_update(hObject, eventdata, handles,status_text);   
    end
    
else
    status_text = { ' '; 'Not TxSite defined .... ' };
    status_update(hObject, eventdata, handles,status_text);      
    
end


%==========================================================================
%==== Not used functions
%==========================================================================

function list_box_status_Callback(hObject, eventdata, handles)

function list_box_status_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_OS_z_Callback(hObject, eventdata, handles)

function text_OS_z_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_OS_xy_Callback(hObject, eventdata, handles)

function text_OS_xy_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function pop_up_outline_sel_cell_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function pop_up_outline_sel_TS_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function aux_Callback(hObject, eventdata, handles)

function checkbox_output_Callback(hObject, eventdata, handles)

function txt_TS_bgd_Callback(hObject, eventdata, handles)

function txt_TS_bgd_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_th_auto_detect_Callback(hObject, eventdata, handles)

function text_th_auto_detect_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end
