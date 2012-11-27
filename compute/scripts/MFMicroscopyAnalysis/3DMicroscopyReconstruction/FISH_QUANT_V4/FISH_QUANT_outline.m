function varargout = FISH_QUANT_outline(varargin)
% FISH_QUANT_OUTLINE M-file for FISH_QUANT_outline.fig

% Begin initialization code - DO NOT EDIT
gui_Singleton = 1;
gui_State = struct('gui_Name',       mfilename, ...
                   'gui_Singleton',  gui_Singleton, ...
                   'gui_OpeningFcn', @FISH_QUANT_outline_OpeningFcn, ...
                   'gui_OutputFcn',  @FISH_QUANT_outline_OutputFcn, ...
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


% --- Executes just before FISH_QUANT_outline is made visible.
function FISH_QUANT_outline_OpeningFcn(hObject, eventdata, handles, varargin)
handles.output = hObject;

%- Set font-size to 10
%  For whatever reason are all the fonts on windows are set back to 8 when the .fig is openend
h_font_8 = findobj(handles.h_fishquant_outline,'FontSize',8);
set(h_font_8,'FontSize',10)

%- Get installation directory of FISH-QUANT and initiate 
p = mfilename('fullpath');        
handles.FQ_path = fileparts(p); 
handles         = FISH_QUANT_start_up_v2(handles);

%- Change name of GUI
set(handles.h_fishquant_outline,'Name', ['FISH-QUANT ', handles.version, ': outline designer']);

%- Export figure handle to workspace - will be used in Close All button of main Interface
assignin('base','h_outline',handles.h_fishquant_outline)

%=== Options for TxSite quantification
handles.parameters_quant = FQ_TS_settings_init_v1;

%= Some parameters
handles.image_struct = [];

%= Default parameter for loading of GUI - might be overwritten by later functions
handles.child = 0;    
handles.cell_counter  = 1;  % Avoid having cells with same name after deleting one. 
handles.axis_fig = [];

handles.file_name_settings = [];
handles.par_microscope.pixel_size.xy = 160;
handles.par_microscope.pixel_size.z  = 300;   
handles.par_microscope.RI            = 1.458;   
handles.par_microscope.NA            = 1.25;
handles.par_microscope.Em            = 568;   
handles.par_microscope.Ex            = 568;
handles.par_microscope.type          = 'widefield';  

%- Other parameters
handles.status_draw   = 0;        % Used to avoid multiple calls of draw functions
handles.v_axis        = [];
handles.status_second = 0;
handles.img2_min_show = 0;
handles.img2_max_show = 0;
handles.img2_transparency = 0.3;

handles.cMap2 = bone(256); 
handles.cMap1 = hot(256); 

handles.status_zoom = 0;
handles.h_zoom = rand(1);
handles.status_pan = 0;
handles.h_pan = rand(1);


global status_plot_first
status_plot_first = 1;

%- Update status of various controls
set(handles.button_finished,'Enable', 'off');    
set(handles.h_fishquant_outline,'WindowStyle','normal')

set(handles.button_open_image,'Enable', 'on');   
set(handles.pop_up_parameters,'Enable', 'on');    
set(handles.button_parameters,'Enable', 'on'); 

%= Load data if called from other GUI
if not(isempty(varargin))

    if strcmp( varargin{1},'HandlesMainGui')
        
        handles.child = 1;        
        
        handles_MAIN = varargin{2};

       % handles.h_fishquant    = handles_MAIN.h_fishquant;
        handles.cell_prop      = handles_MAIN.cell_prop;
        handles.par_microscope = handles_MAIN.par_microscope;

        handles.file_name_image    = handles_MAIN.file_name_image;
        handles.path_name_image    = handles_MAIN.path_name_image;
        handles.file_name_settings = handles_MAIN.file_name_settings;
        handles.file_name_image_filtered = handles_MAIN.file_name_image_filtered;
        handles.cell_counter       = size(handles.cell_prop,2) +1;     % Avoid having cells with same name after deleting one. 

        %- Change name of GUI
        set(handles.h_fishquant_outline,'Name', ['FISH-QUANT ', handles.version, ': outline designer - ', handles.file_name_image ]);
        
        %- Image
        handles.image_struct  = handles_MAIN.image_struct;
        handles.img_plot      = max(handles.image_struct.data,[],3); 
        handles.img_min       =  min(handles.image_struct.data(:)); 
        handles.img_max       =  max(handles.image_struct.data(:)); 
        handles.img_diff      =  handles.img_max-handles.img_min; 
        
        %- Analyze outline
        if not(isempty(handles.file_name_image))
            handles = analyze_outline(hObject, eventdata, handles);
        end
      
        %- Update status of various controls
        set(handles.button_finished,'Enable', 'on');
        set(handles.button_open_image,'Enable', 'off');
        set(handles.h_fishquant_outline,'WindowStyle','normal')
        set(handles.pop_up_parameters,'Enable', 'off');    
        set(handles.button_parameters,'Enable', 'off');         
        
    elseif strcmp( varargin{1},'par_microscope')         
        handles.par_microscope = varargin{2};
        
    end 
end


%- Update handles structure
guidata(hObject, handles);  

%- Check which elements should be enabled
GUI_enable(handles)

%- UIWAIT makes FISH_QUANT_outline wait for user response (see UIRESUME)
if not(isempty(varargin))
    if strcmp( varargin{1},'HandlesMainGui')
        uiwait(handles.h_fishquant_outline);
    end
end


%== Check with controls should be enabled
function GUI_enable(handles)

if isempty(handles.image_struct)
     set(handles.button_cell_new,'Enable', 'off');    
     set(handles.button_cell_modify,'Enable', 'off');        
     set(handles.button_cell_delete,'Enable', 'off');    
     set(handles.listbox_cell,'Enable', 'off');  
     set(handles.listbox_TS,'Enable', 'off');    
     set(handles.button_TS_modify,'Enable', 'off');        
     set(handles.button_TS_delete,'Enable', 'off');    
     set(handles.button_TS_new,'Enable', 'off');      
else
     set(handles.button_cell_new,'Enable', 'on');    
     set(handles.button_cell_modify,'Enable', 'on');        
     set(handles.button_cell_delete,'Enable', 'on');    
     set(handles.listbox_cell,'Enable', 'on');  
     set(handles.listbox_TS,'Enable', 'on');    
     set(handles.button_TS_modify,'Enable', 'on');        
     set(handles.button_TS_delete,'Enable', 'on');    
     set(handles.button_TS_new,'Enable', 'on')
end

%- Delete/modify only possible if listbox populated
if isempty(get(handles.listbox_cell,'String'))    
     set(handles.button_cell_modify,'Enable', 'off');
     set(handles.button_cell_delete,'Enable', 'off'); 
else
     set(handles.button_cell_modify,'Enable', 'on');
     set(handles.button_cell_delete,'Enable', 'on');
end

%- Delete/modify only possible if listbox populated
if isempty(get(handles.listbox_TS,'String'))    
     set(handles.button_TS_modify,'Enable', 'off');
     set(handles.button_TS_delete,'Enable', 'off'); 
else
     set(handles.button_TS_modify,'Enable', 'on');
     set(handles.button_TS_delete,'Enable', 'on');
end


%- Enable only if image is present
if isfield(handles,'image_struct')
    if not(isempty(handles.image_struct))
        set(handles.menu_save_outline,'Enable', 'on');
    else
        set(handles.menu_save_outline,'Enable', 'off');
    end
else
    set(handles.menu_save_outline,'Enable', 'off');
end

%- Enable showing the second stack
if handles.status_second
    set(handles.checkbox_second_stack,'Enable','on')
else
    set(handles.checkbox_second_stack,'Enable','off')    
end

%== Parameter that are returned
function varargout = FISH_QUANT_outline_OutputFcn(hObject, eventdata, handles) 

%- Only if called from another GUI
if handles.child
    varargout{1} = handles.cell_prop;
    delete(handles.h_fishquant_outline);
end

%== Resume GUI when called from other GUI
function button_finished_Callback(hObject, eventdata, handles)
uiresume(handles.h_fishquant_outline)


% --- Executes when user attempts to close h_fishquant_outline.
function h_fishquant_outline_CloseRequestFcn(hObject, eventdata, handles)
if handles.child 
    uiresume(handles.h_fishquant_outline)
else
    delete(handles.h_fishquant_outline)
    if isfield(handles,'axes_sep')
        try
            delete(handles.axes_sep)
        catch; end
    end
end


%== OPEN IMAGE
function button_open_image_Callback(hObject, eventdata, handles)
[file_name_image,path_name_image] = uigetfile({'*.tif';'*.dv';'*.stk';'*.TIF'},'Select file');

if file_name_image ~= 0
    
    %- Load image and plot
    handles.image_struct  = load_stack_data_v4(fullfile(path_name_image,file_name_image));
    
    %- Convert to 8bit
    data_dum = handles.image_struct.data;
    data_min = min(data_dum(:));
    data_max = max(data_dum(:));
    
    handles.data_norm = uint8((data_dum-data_min) * 255/(data_max-data_min)); 
    
    handles.img_plot      =  max(handles.data_norm,[],3); 
    handles.img_min       =  min(handles.data_norm(:)); 
    handles.img_max       =  max(handles.data_norm(:)); 
    handles.img_diff      =  handles.img_max-handles.img_min;                 
    
   % handles.img_plot      =  max(handles.image_struct.data,[],3); 
   % handles.img_min       =  min(handles.image_struct.data(:)); 
   % handles.img_max       =  max(handles.image_struct.data(:)); 
   % handles.img_diff      =  handles.img_max-handles.img_min; 
    
    handles.cell_prop      = [];
    handles.cell_counter   = 1;
    handles.status_second  = 0;
    handles.cell_counter   = 1; % Reset cell counter
    handles.v_axis         = [];
    
    
    %- Save results
    handles.file_name_image = file_name_image;
    handles.path_name_image = path_name_image;
    handles.file_name_image_filtered = [];
    guidata(hObject, handles);    
    
    %- Change name of GUI
    set(handles.h_fishquant_outline,'Name', ['FISH-QUANT ', handles.version, ': outline designer - ', handles.file_name_image ]);
    
    %= Check which elements should be enabled
    GUI_enable(handles)
    set(handles.listbox_cell,'String', []);
    set(handles.listbox_TS,'String', []);
    plot_image(handles,handles.axes_image);
    
end


% =========================================================================
%  OPEN SECOND IMAGE
% =========================================================================

%== Second image
function menu_2nd_load_Callback(hObject, eventdata, handles)
[file_name_image,path_name_image] = uigetfile({'*.tif';'*.dv';'*.stk';'*.TIF'},'Select file');

if file_name_image ~= 0
    
    %- Load image and plot
    handles.image2_struct  = load_stack_data_v4(fullfile(path_name_image,file_name_image));
    
    %-
    data_dum = handles.image2_struct.data;
    data_min = min(data_dum(:));
    data_max = max(data_dum(:));
    
    handles.data2_norm = uint8((data_dum-data_min) * 255/(data_max-data_min)); 
        
    handles.img2_plot      =  max(handles.data2_norm,[],3); 
    handles.img2_min       =  min(handles.data2_norm(:)); 
    handles.img2_max       =  max(handles.data2_norm(:)); 
    handles.img2_diff      =  handles.img2_max-handles.img2_min;                 
   
    
    %handles.img2_plot      =  max(handles.image2_struct.data,[],3); 
    %handles.img2_min       =  min(handles.image2_struct.data(:)); 
    %handles.img2_max       =  max(handles.image2_struct.data(:)); 
    %handles.img2_diff      =  handles.img2_max-handles.img2_min; 

    handles.img2_min_show = handles.img2_min;
    handles.img2_max_show = handles.img2_max;
    
    %- Save results
    handles.file_name_image2 = file_name_image;
    handles.path_name_image2 = path_name_image;
    
    handles.status_second = 1;
    set(handles.checkbox_second_stack,'Enable','on')
    set(handles.checkbox_second_stack,'Value',1)
    guidata(hObject, handles);     
    plot_image(handles,handles.axes_image);
end


%== Contrast to show second stack
function menu_2nd_contrast_Callback(hObject, eventdata, handles)
% Function to modify the settings of TS quantification

%- User-dialog
dlgTitle = 'Contrast for second stack';
prompt_avg(1) = {'CONTRAST: MIN'};
prompt_avg(2) = {'CONSTRAST:MAX'};
prompt_avg(3) = {'Transparency [0,1]'};
defaultValue_avg{1} = num2str(handles.img2_min_show);
defaultValue_avg{2} = num2str(handles.img2_max_show);
defaultValue_avg{3} = num2str(handles.img2_transparency);

options.Resize='on';
%options.WindowStyle='normal';
userValue = inputdlg(prompt_avg,dlgTitle,1,defaultValue_avg,options);

%- Return results if specified
if( ~ isempty(userValue))
    handles.img2_min_show     = str2double(userValue{1});
    handles.img2_max_show     = str2double(userValue{2});
    handles.img2_transparency     = str2double(userValue{3});
    guidata(hObject, handles);     
    plot_image(handles,handles.axes_image);
end


% =========================================================================
% Load and save
% =========================================================================

%== Save outline
%function button_save_outline_Callback(hObject, eventdata, handles)
function menu_save_outline_Callback(hObject, eventdata, handles)
FISH_QUANT_save_outline_v5(handles,[])


%== Load outline
%function button_load_outline_Callback(hObject, eventdata, handles)
function menu_load_outline_Callback(hObject, eventdata, handles)
[file_name_results,path_name_results] = uigetfile({'*.txt'},'Select file with outline');

if file_name_results ~= 0
    [cell_prop file_name par_microscope file_name_settings file_name_image_filtered] = FISH_QUANT_load_results_v7(fullfile(path_name_results,file_name_results));
    
    %- Save results
    handles.cell_prop       = cell_prop;
    handles.par_microscope  = par_microscope;
    handles.file_name_image = file_name;
    handles.path_name_image = path_name_results;
    handles.file_name_settings = file_name_settings;
    handles.file_name_image_filtered = file_name_image_filtered;
        
    %- Load image
    handles.image_struct  = load_stack_data_v4(fullfile(handles.path_name_image,handles.file_name_image));
    
    %- Convert to 8bit
    data_dum = handles.image_struct.data;
    data_min = min(data_dum(:));
    data_max = max(data_dum(:));
    
    handles.data_norm = uint8((data_dum-data_min) * 255/(data_max-data_min)); 
    
    handles.img_plot      =  max(handles.data_norm,[],3); 
    handles.img_min       =  min(handles.data_norm(:)); 
    handles.img_max       =  max(handles.data_norm(:)); 
    handles.img_diff      =  handles.img_max-handles.img_min;     
    
    
    %handles.img_plot      = max(handles.image_struct.data,[],3); 
    %handles.img_min       =  min(handles.image_struct.data(:)); 
    %handles.img_max       =  max(handles.image_struct.data(:)); 
    %handles.img_diff      =  handles.img_max-handles.img_min;  
    
    handles.status_second = 0;
    handles.v_axis        = [];
        
    %- Change name of GUI
    set(handles.h_fishquant_outline,'Name', ['FISH-QUANT ', handles.version, ': outline designer - ', handles.file_name_image ]);
    
    %- Analyze outline
    handles = analyze_outline(hObject, eventdata, handles); 
    guidata(hObject, handles);
end


%== Function to analyze loaded outline 
function handles = analyze_outline(hObject, eventdata, handles)


%- Populate list with names of cells
cell_prop = handles.cell_prop;

if not(isempty(cell_prop))

    N_cell = size(cell_prop,2);
    for i_cell = 1:N_cell
        
        %- Add fields needed for modification
        cell_prop(i_cell).reg_type = 'Polygon';
        
        reg_pos = [];
        reg_pos(:,1)  = cell_prop(i_cell).x;
        reg_pos(:,2)  = cell_prop(i_cell).y;
        
        cell_prop(i_cell).reg_pos = reg_pos;

        
        %- Get string with name of cell
        str_list_cell{i_cell} =  cell_prop(i_cell).label;

        str_list_TS = {};
        
        N_TS = size(cell_prop(i_cell).pos_TS,2);
        for i_TS = 1:N_TS
            
            %- Add fields needed for modification
            cell_prop(i_cell).pos_TS(i_TS).reg_type = 'Polygon';

            reg_pos = [];
            reg_pos(:,1)  = cell_prop(i_cell).pos_TS(i_TS).x;
            reg_pos(:,2)  = cell_prop(i_cell).pos_TS(i_TS).y;

            cell_prop(i_cell).pos_TS(i_TS).reg_pos = reg_pos;

            %- Get name of transcription site
            str_list_TS{i_TS} =  cell_prop(i_cell).pos_TS(i_TS).label;
        end
        cell_prop(i_cell).str_list_TS = str_list_TS;
        cell_prop(i_cell).TS_counter  = N_TS +1;
    end

    set(handles.listbox_cell,'String', str_list_cell);
end

%- Save parameters
handles.cell_prop    = cell_prop;
handles.cell_counter = size(cell_prop,2)+1;
guidata(hObject, handles);

%- Show plot
set(handles.listbox_cell,'Value',1);
set(handles.listbox_TS,'Value',1);
listbox_cell_Callback(hObject, eventdata, handles)



% =========================================================================
% Functions to manipulate cells
% =========================================================================


%== New cell is entire image
function button_cell_image_Callback(hObject, eventdata, handles)

%- Get current list
str_list = get(handles.listbox_cell,'String');
N_Cell   = size(str_list,1);
ind_cell = N_Cell+1;  


handles.cell_prop(ind_cell).label = 'EntireImage';
w = handles.image_struct.w;
h = handles.image_struct.h;
handles.cell_prop(ind_cell).x     = [1 1 w w];
handles.cell_prop(ind_cell).y     = [1 h h 1];
handles.cell_prop(ind_cell).status_filtered = 1;    % Image filterd
handles.cell_prop(ind_cell).pos_TS = [];
handles.cell_prop(ind_cell).str_list_TS = [];
handles.cell_prop(ind_cell).TS_counter   = 1;  

handles.cell_prop(ind_cell).reg_type = 'Rectangle';
handles.cell_prop(ind_cell).reg_pos  = [1 1 w h];

%- Add entry at the end and update list
str_list{ind_cell} = 'EntireImage';

set(handles.listbox_cell,'String',str_list)
set(handles.listbox_cell,'Value',ind_cell)
listbox_cell_Callback(hObject, eventdata, handles)


%- Save and show results
guidata(hObject, handles);
plot_image(handles,handles.axes_image);


%- UIWAIT makes FISH_QUANT_outline wait for user response (see UIRESUME)
%- New call is necessary since impoly breaks first call
if handles.child;
    uiwait(handles.h_fishquant_outline);
end


%== New cell
function button_cell_new_Callback(hObject, eventdata, handles)

if not(handles.status_draw)
    
    %- Set status that one object is currently constructed
    handles.status_draw = 1;
    guidata(hObject, handles);

    %- Determine if plot should be done in separate figure
    fig_sep = get(handles.checkbox_sep_window,'Value');
    handles = plot_decide_window(hObject, eventdata, handles);

    %- Get current list
    str_list = get(handles.listbox_cell,'String');
    N_Cell   = size(str_list,1);
    ind_cell = N_Cell+1;

    %- Draw region
    str = get(handles.pop_up_region, 'String');
    val = get(handles.pop_up_region,'Value');

    param.reg_type = str{val};
    param.h_axes   = gca;
    param.pos      = [];

    reg_result = FQ_draw_region_v1(param);

    position = reg_result.position;

    handles.axis_fig  = axis;

    %- Save position
    handles.cell_prop(ind_cell).reg_type = reg_result.reg_type;
    handles.cell_prop(ind_cell).reg_pos  = reg_result.reg_pos;

    handles.cell_prop(ind_cell).x = round(position(:,1))';  % v3: Has to be a row vector to agree with read-in from files
    handles.cell_prop(ind_cell).y = round(position(:,2))';  % v3: Has to be a row vector to agree with read-in from files

    handles.cell_prop(ind_cell).pos_TS = [];
    handles.cell_prop(ind_cell).str_list_TS = [];
    handles.cell_prop(ind_cell).TS_counter   = 1;

    %- Add entry at the end and update list
    str_cell = ['Cell_', num2str(handles.cell_counter)];
    str_list{ind_cell} = str_cell;

    set(handles.listbox_cell,'String',str_list)
    set(handles.listbox_cell,'Value',ind_cell)
    
    listbox_cell_Callback(hObject, eventdata, handles)

    handles.cell_prop(ind_cell).label = str_cell;
    handles.cell_counter = handles.cell_counter+1;
    
    if fig_sep
        handles.v_axis = axis(handles.axes_sep);
    end
    
    %- Save and show results
    handles.status_draw = 0;
    guidata(hObject, handles);
    plot_image(handles,handles.axes_image);


    %- UIWAIT makes FISH_QUANT_outline wait for user response (see UIRESUME)
    %- New call is necessary since impoly breaks first call
    if handles.child;
        uiwait(handles.h_fishquant_outline);
    end
end


%== Modify cell
function button_cell_modify_Callback(hObject, eventdata, handles)

if not(handles.status_draw)
    
    %- Set status that one object is currently constructed
    handles.status_draw = 1;
    guidata(hObject, handles);

    %- Determine if plot should be done in separate figure
    fig_sep = get(handles.checkbox_sep_window,'Value');
    handles = plot_decide_window(hObject, eventdata, handles);

    %- Extract index and properties of highlighted cell
    ind_sel  = get(handles.listbox_cell,'Value');

    cell_prop = handles.cell_prop(ind_sel);
    
    %- Check if reg-type is defined
    is_reg_type = isfield(cell_prop,'reg_type');
       
    if is_reg_type    
        
        reg_type  = cell_prop.reg_type;
        reg_pos   = cell_prop.reg_pos;

        %- Modify region region
        param.reg_type = reg_type;
        param.h_axes   = gca;
        param.pos      = reg_pos;

        reg_result = FQ_draw_region_v1(param);

        position            = reg_result.position;
        cell_prop.reg_type  = reg_result.reg_type;
        cell_prop.reg_pos   = reg_result.reg_pos;

        cell_prop.x = round(position(:,1))';  % v3: Has to be a row vector to agree with read-in from files
        cell_prop.y = round(position(:,2))';  % v3: Has to be a row vector to agree with read-in from files

        handles.cell_prop(ind_sel) = cell_prop;
        handles.axis_fig     = axis;
        
        if fig_sep
            handles.v_axis = axis(handles.axes_sep);
        end
        
        %- Save results
        handles.status_draw = 0;
        guidata(hObject, handles);
        plot_image(handles,handles.axes_image);


        %- UIWAIT makes FISH_QUANT_outline wait for user response (see UIRESUME)
        %- New call is necessary since impoly breaks first call
        if handles.child;
            uiwait(handles.h_fishquant_outline);    
        end
    else
        msgbox('Geometry can be modified - only deleted','Outline definition','warn'); 
    end
end


%== Delete cell
function button_cell_delete_Callback(hObject, eventdata, handles)

%- Show plot
plot_image(handles,handles.axes_image);

%- Ask user to confirm choice
choice = questdlg('Do you really want to delete this cell?', 'FISH-QUANT', 'Yes','No','No');

if strcmp(choice,'Yes')
    
    %- Extract index of highlighted cell
    str_list = get(handles.listbox_cell,'String');
    ind_sel  = get(handles.listbox_cell,'Value');
    
    %- Delete highlighted cell
    str_list(ind_sel) = [];
    handles.cell_prop(ind_sel) = [];   
    set(handles.listbox_cell,'String',str_list)
    set(handles.listbox_cell,'Value',1)
    
    %- Save results
    guidata(hObject, handles);
    
    %- Show plot
    set(handles.listbox_cell,'Value',1);
    listbox_cell_Callback(hObject, eventdata, handles)   
    plot_image(handles,handles.axes_image);
end


%== Listbox cell
function listbox_cell_Callback(hObject, eventdata, handles)

%-Update list of transcription sites
ind_sel  = get(handles.listbox_cell,'Value');

if not(isempty(handles.cell_prop))
    str_list = handles.cell_prop(ind_sel).str_list_TS;
    set(handles.listbox_TS,'String',str_list)
    set(handles.listbox_TS,'Value',1)
end

%- Update plot
%pop_up_image_Callback(hObject, eventdata, handles)
plot_image(handles,handles.axes_image);



% =========================================================================
% Functions to automatically detect transcription sites
% =========================================================================

%== Autodetect transcription sites
function button_auto_detect_Callback(hObject, eventdata, handles)


disp('Auto-detection of transcription sites ... please wait .... ')

int_th = str2double(get(handles.text_th_auto_detect,'String'));

%- Parameters
parameters.int_th       = int_th;
parameters.conn         = handles.parameters_quant.conn;
parameters.flags.output = 0;
parameters.crop_image   = handles.parameters_quant.crop_image;
parameters.pixel_size   = handles.par_microscope.pixel_size;
parameters.cell_prop    = handles.cell_prop;
parameters.min_dist = handles.parameters_quant.min_dist;

%- Detect and analyse
handles.cell_prop        = TxSite_detect_v1(handles.image_struct,parameters);
handles                  = analyze_outline(hObject, eventdata, handles);
plot_image(handles,handles.axes_image);

%- Save results
handles.status_auto_detect = 1;
handles.parameters_auto_detect.int_th = parameters.int_th  ;
handles.parameters_auto_detect.conn = parameters.conn  ;


%- Show plot
plot_image(handles,handles.axes_image);

fig_sep = get(handles.checkbox_sep_window,'Value');

if fig_sep
    if not(isfield(handles,'axes_sep'))
        figure
        handles.axes_sep = gca;
    end    
    plot_image(handles,handles.axes_sep);
    axis(handles.axis_fig);        
end

guidata(hObject, handles); 
disp('Auto-detection of transcription sites ... FINISHED .... ')


%=== Options for quantification
function menu_options_Callback(hObject, eventdata, handles)
handles.parameters_quant = FQ_TS_settings_detect_modify_v1(handles.parameters_quant);
guidata(hObject, handles);


% =========================================================================
% Functions to manipulate TxSites
% =========================================================================

%== New TxSite
function button_TS_new_Callback(hObject, eventdata, handles)

if not(handles.status_draw)
    
    %- Set status that one object is currently constructed
    handles.status_draw = 1;
    guidata(hObject, handles);

    %- Determine if plot should be done in separate figure
    fig_sep = get(handles.checkbox_sep_window,'Value');
    handles = plot_decide_window(hObject, eventdata, handles);

    %- Get current cell and list of TS of this cell
    ind_cell = get(handles.listbox_cell,'Value');
    cell_X   = handles.cell_prop(ind_cell).x;
    cell_Y   = handles.cell_prop(ind_cell).y;

    pos_TS   = handles.cell_prop(ind_cell).pos_TS; 

    %- Get current list
    str_list = get(handles.listbox_TS,'String');
    N_TS     = size(pos_TS,2);
    ind_TS   = N_TS+1;


    %---- Draw region
    str = get(handles.pop_up_region, 'String');
    val = get(handles.pop_up_region,'Value');

    param.reg_type = str{val};
    param.h_axes   = gca;
    param.pos      = [];

    reg_result = FQ_draw_region_v1(param);
    position   = reg_result.position;

    %---- Analyse region
    TS_X = round(position(:,1))';   % Has to be a row vector to agree with read-in from files
    TS_Y = round(position(:,2))';

    in_cell = inpolygon(TS_X,TS_Y,cell_X,cell_Y);

    if sum(in_cell) < length(TS_X)    
        handles.status_draw = 0;
        guidata(hObject, handles);
        
        errordlg('Transcription site has to be within the cell.', 'FISH-QUANT')
        plot_image(handles,handles.axes_image);  
    else
        
        %- Add entry at the end and update list
        str_TS = ['TS_', num2str(handles.cell_prop(ind_cell).TS_counter)];
        str_list{ind_TS} = str_TS;
        set(handles.listbox_TS,'String',str_list)

        %- Save position
        pos_TS(ind_TS).x        = TS_X;  
        pos_TS(ind_TS).y        = TS_Y;  
        pos_TS(ind_TS).label    = str_TS; 
        pos_TS(ind_TS).reg_type = reg_result.reg_type;  
        pos_TS(ind_TS).reg_pos  = reg_result.reg_pos;     


        %- Update information of this cell
        handles.cell_prop(ind_cell).pos_TS = pos_TS;
        handles.cell_prop(ind_cell).str_list_TS = str_list;        
        handles.cell_prop(ind_cell).TS_counter = handles.cell_prop(ind_cell).TS_counter+1;   

        if fig_sep
            handles.v_axis = axis(handles.axes_sep);
        end
                
        %- Save results
        handles.status_draw = 0;
        guidata(hObject, handles);

        %- Show plot
        plot_image(handles,handles.axes_image);

    end

    %- UIWAIT makes FISH_QUANT_outline wait for user response (see UIRESUME)
    %- New call is necessary since impoly breaks first call
    if handles.child;  
        uiwait(handles.h_fishquant_outline);
    end
end


%== Modify existing TS
function button_TS_modify_Callback(hObject, eventdata, handles)

if not(handles.status_draw)
    
    %- Set status that one object is currently constructed
    handles.status_draw = 1;
    guidata(hObject, handles);
    
    %- Determine if plot should be done in separate figure
    fig_sep = get(handles.checkbox_sep_window,'Value');
    handles = plot_decide_window(hObject, eventdata, handles);

    %- Get current cell and list of TS of this cell
    ind_cell = get(handles.listbox_cell,'Value');
    cell_X   = handles.cell_prop(ind_cell).x;
    cell_Y   = handles.cell_prop(ind_cell).y;

    %- Extract index of highlighted transcription site
    ind_TS  = get(handles.listbox_TS,'Value');
    pos_TS  = handles.cell_prop(ind_cell).pos_TS(ind_TS); 


    %- Extract index and properties of highlighted cell
    reg_type  = handles.cell_prop(ind_cell).pos_TS(ind_TS).reg_type;
    reg_pos   = handles.cell_prop(ind_cell).pos_TS(ind_TS).reg_pos;

    %- Modify region region
    param.reg_type = reg_type;
    param.h_axes   = gca;
    param.pos      = reg_pos;

    reg_result = FQ_draw_region_v1(param);

    position   = reg_result.position;

    %- Analyse distribution
    TS_X = round(position(:,1))';  % v3: Has to be a row vector to agree with read-in from files
    TS_Y = round(position(:,2))';  % v3: Has to be a row vector to agree with read-in from files

    in_cell = inpolygon(TS_X,TS_Y,cell_X,cell_Y);

    if sum(in_cell) < length(TS_X)    
        handles.status_draw = 0;
        guidata(hObject, handles);
        
        errordlg('Transcription site has to be within the cell.', 'FISH-QUANT')
        plot_image(handles,handles.axes_image);  
    else

        %- Save position
        pos_TS = handles.cell_prop(ind_cell).pos_TS(ind_TS);    
        pos_TS.x     = TS_X;  
        pos_TS.y     = TS_Y; 
        pos_TS.reg_type = reg_result.reg_type;  
        pos_TS.reg_pos  = reg_result.reg_pos; 
        handles.cell_prop(ind_cell).pos_TS(ind_TS) = pos_TS;
        
        if fig_sep
            handles.v_axis = axis(handles.axes_sep);
        end
        
        %- Save results
        handles.status_draw = 0;
        guidata(hObject, handles);

        %- Show results
        plot_image(handles,handles.axes_image);

        set(handles.listbox_TS,'Value',ind_TS);
    end

    %- UIWAIT makes FISH_QUANT_outline wait for user response (see UIRESUME)
    %- New call is necessary since impoly breaks first call
    if handles.child;  
        uiwait(handles.h_fishquant_outline);
    end
end


%== Delete Transcription site
function button_TS_delete_Callback(hObject, eventdata, handles)

%- Show plot
plot_image(handles,handles.axes_image);

%- Ask user to confirm choice
choice = questdlg('Do you really want to delete this TxSite?', 'FISH-QUANT', 'Yes','No','No');

if strcmp(choice,'Yes')
    
    %- Get current cell and list of TS of this cell
    ind_cell = get(handles.listbox_cell,'Value');
    pos_TS   = handles.cell_prop(ind_cell).pos_TS;    
    str_list_TS   = handles.cell_prop(ind_cell).str_list_TS;
    
    %- Extract index of highlighted cell
    ind_sel  = get(handles.listbox_TS,'Value');
    
    %- Delete highlighted TS 
    str_list_TS(ind_sel) = [];    
    pos_TS(ind_sel) = [];      
   
    %- Save results
    handles.cell_prop(ind_cell).pos_TS      = pos_TS; 
    handles.cell_prop(ind_cell).str_list_TS = str_list_TS;
    set(handles.listbox_TS,'String',str_list_TS)
    guidata(hObject, handles);
    
    %- Update GUI and show plot
    set(handles.listbox_TS,'Value',1);
    listbox_TS_Callback(hObject, eventdata, handles) 
    plot_image(handles,handles.axes_image); 
    
end


%== Listbox TS
function listbox_TS_Callback(hObject, eventdata, handles)
plot_image(handles,handles.axes_image);


% =========================================================================
% Plot
% =========================================================================


%== Decide which plot window to use
function handles = plot_decide_window(hObject, eventdata, handles)

%- Determine if plot should be done in separate figure
fig_sep = get(handles.checkbox_sep_window,'Value');

if fig_sep
    
    %- Has there already been a figure handle?
    if isfield(handles,'axes_sep')
        
        %- Is this figure handle still present?
        
        %  Handles is deletec
        if not(ishandle(handles.axes_sep))
            figure;
            handles.axes_sep = gca;
            handles.axis_fig = [];
            guidata(hObject, handles);  
            
        %  Handles is still there
        else
            axes(handles.axes_sep);            
        end
    
    % New figure handles    
    else
         figure;
         handles.axes_sep = gca;
         handles.axis_fig = [];
         guidata(hObject, handles);
         
    end
    
    plot_image(handles,handles.axes_sep);
    if not(isempty(handles.v_axis))
        axis(handles.v_axis);
    end
end


%== Plot function
function handles = plot_image(handles,axes_select)

global status_plot_first

%- Show second stack
if handles.status_second
    status_second = get(handles.checkbox_second_stack,'Value');

   if  status_second
       img_2nd_min = handles.img2_min_show;
       img_2nd_max = handles.img2_max_show;
   end
else
    status_second = 0;
end

%- Select output axis
if isempty(axes_select)
    figure
else
    axes(axes_select)
    v = axis;
end

    
%- Determine the contrast of the image
slider_min = get(handles.slider_contrast_min,'Value');
slider_max = get(handles.slider_contrast_max,'Value');

img_min  = handles.img_min;
img_diff = handles.img_diff;

Im_min = slider_min*img_diff+img_min;
Im_max = slider_max*img_diff+img_min;

if Im_max < Im_min
    Im_max = Im_min+1;
end


%- Determine which image should be shown
str = get(handles.pop_up_image, 'String');
val = get(handles.pop_up_image,'Value');

% Set experimental settings based on selection
switch str{val};
    
    case 'Maximum projection' 
        
    %    img_RGB = [];
    %    img_RGB(:,:,1) = (handles.img_plot-Im_min)/(Im_max-Im_min);
    %    img_RGB(:,:,2) = zeros(size(handles.img_plot));
    %    img_RGB(:,:,3) = zeros(size(handles.img_plot));
        %subimage(img_RGB);  
        
        dum1  = uint8((handles.img_plot-Im_min) * (255/(Im_max-Im_min)));
        dum2 = ind2rgb(dum1,handles.cMap1);        
        subimage(dum2)
   
        
        %=== THIS WOULD BE AN ALTERNATIVE IMPLEMENTATION WITH COLORMAPS
        %cMap = hot(256);      
        %img_min = min(handles.img_plot(:));
        %img_max = 0.2*max(handles.img_plot(:));
        %img_norm = uint8((handles.img_plot-img_min)* (256/(img_max-img_min)));
        %test = ind2rgb(img_norm,cMap);
        %figure, imshow(test)
        
        title('Maximum projection of loaded image','FontSize',9);        
        if status_second            
            hold on
            
            %img2_RGB = [];
            %img2_RGB(:,:,1) = zeros(size(handles.img2_plot));
            %img2_RGB(:,:,2) = zeros(size(handles.img2_plot));
            %img2_RGB(:,:,3) = (handles.img2_plot-img_2nd_min)/(img_2nd_max-img_2nd_min);
            dum1 = uint8((handles.img2_plot-img_2nd_min) * (255/(img_2nd_max-img_2nd_min)));            
            dum2 = ind2rgb(dum1,handles.cMap2); 
            h2 = subimage(dum2);
            
            hold off
            set(h2, 'AlphaData', handles.img2_transparency)   
        end
     
    
    case 'Z-stack'
        
        ind_plot = str2double(get(handles.text_z_slice,'String'));
        
        %img_plot =  handles.image_struct.data(:,:,ind_plot);
        %img_RGB = [];
        %img_RGB(:,:,1) = (img_plot-Im_min)/(Im_max-Im_min);
        %img_RGB(:,:,2) = zeros(size(img_plot));
        %img_RGB(:,:,3) = zeros(size(img_plot));
        %subimage(img_RGB);  
        
        img_plot =  handles.data_norm(:,:,ind_plot);
        dum1  = uint8((img_plot-Im_min) * (255/(Im_max-Im_min)));
        dum2 = ind2rgb(dum1,handles.cMap1);        
        subimage(dum2)        
        
        N_slice  = handles.image_struct.size;
        title(['slice #' , num2str(ind_plot) , ' of ', num2str(N_slice)],'FontSize',9);
        
        if status_second
            %img2_plot =  handles.image2_struct.data(:,:,ind_plot);            
            %img2_RGB = [];
            %img2_RGB(:,:,1) = zeros(size(img2_plot));
            %img2_RGB(:,:,2) = zeros(size(img2_plot));
            %img2_RGB(:,:,3) = (img2_plot-img_2nd_min)/(img_2nd_max-img_2nd_min);       
            
            img_plot =  handles.data2_norm(:,:,ind_plot);
            dum1  = uint8((img_plot-img_2nd_min) * (255/(img_2nd_max-img_2nd_min)));
            dum2 = ind2rgb(dum1,handles.cMap1);   
            
            hold on
               %h2 = subimage(img2_RGB);
                h2 = subimage(dum2);
            hold off
            set(h2, 'AlphaData', handles.img2_transparency)   
        end
        
        colormap(hot)
end

%- Plot outline of cell and TS
hold on
if isfield(handles,'cell_prop')    
    cell_prop = handles.cell_prop;    
    if not(isempty(cell_prop))  
        for i = 1:size(cell_prop,2)
            x = cell_prop(i).x;
            y = cell_prop(i).y;
            plot([x,x(1)],[y,y(1)],'b','Linewidth', 2)     
        end
        
        %- Plot selected cell in different color
        ind_cell = get(handles.listbox_cell,'Value');
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


                %- Plot selected cell in different color
                ind_sel = get(handles.listbox_TS,'Value');
                x = pos_TS(ind_sel).x;
                y = pos_TS(ind_sel).y;
                plot([x,x(1)],[y,y(1)],'g','Linewidth', 2)             
            end                
        end            
    end        
end    
hold off


%- Same zoom as before
if not(status_plot_first)
    if axes_select == handles.axes_image
        axis(v);
    end
end

%- Save everything
status_plot_first = 0;

%= Check which elements should be enabled
GUI_enable(handles)


%== Slider minimum contrast
function slider_contrast_min_Callback(hObject, eventdata, handles)

slider_min = get(handles.slider_contrast_min,'Value');

img_min  = handles.img_min;
img_diff = handles.img_diff;

contr_min = slider_min*img_diff+img_min;
set(handles.text_contr_min,'String',num2str(round(contr_min)));

plot_image(handles,handles.axes_image);


%== Slider maximum contrast
function slider_contrast_max_Callback(hObject, eventdata, handles)
slider_min = get(handles.slider_contrast_min,'Value');
slider_max = get(handles.slider_contrast_max,'Value');

img_min  = handles.img_min;
img_diff = handles.img_diff;

contr_min = slider_min*img_diff+img_min;
contr_max = slider_max*img_diff+img_min;

if contr_max < contr_min
    contr_max = contr_min+1;
end
set(handles.text_contr_max,'String',num2str(round(contr_max)));

plot_image(handles,handles.axes_image); 


%== Slider for slice
function slider_slice_Callback(hObject, eventdata, handles)
N_slice      = handles.image_struct.size;
slider_value = get(handles.slider_slice,'Value');

ind_slice = round(slider_value*(N_slice-1)+1);
set(handles.text_z_slice,'String',num2str(ind_slice));

plot_image(handles,handles.axes_image); 
guidata(hObject, handles); 


%== Up one slice
function button_slice_incr_Callback(hObject, eventdata, handles)
N_slice = handles.image_struct.size;

%- Get next value for slice
ind_slice = str2double(get(handles.text_z_slice,'String'))+1;
if ind_slice > N_slice;ind_slice = N_slice;end
set(handles.text_z_slice,'String',ind_slice);

%-Update slider
slider_value = (ind_slice-1)/(N_slice-1);
set(handles.slider_slice,'Value',slider_value);

%- Save and plot image
plot_image(handles,handles.axes_image);
guidata(hObject, handles);


%== Down one slice
function button_slice_decr_Callback(hObject, eventdata, handles)
N_slice = handles.image_struct.size;

%- Get next value for slice
ind_slice = str2double(get(handles.text_z_slice,'String'))-1;
if ind_slice <1;ind_slice = 1;end
set(handles.text_z_slice,'String',ind_slice);

%-Update slider
slider_value = (ind_slice-1)/(N_slice-1);
set(handles.slider_slice,'Value',slider_value);

%- Save and plot image
plot_image(handles,handles.axes_image);
guidata(hObject, handles);


%== Selection which image
function pop_up_image_Callback(hObject, eventdata, handles)

str = get(handles.pop_up_image, 'String');
val = get(handles.pop_up_image,'Value');

% Set experimental settings based on selection
switch str{val};
    
    case 'Maximum projection' 
        set(handles.text_z_slice,'String',NaN);
        set(handles.slider_slice,'Value',0);
        
        set(handles.button_slice_decr,'Enable','off');
        set(handles.button_slice_incr,'Enable','off');        
        set(handles.slider_slice,'Enable','off'); 
    
    case 'Z-stack'
        set(handles.text_z_slice,'String',1);
        set(handles.slider_slice,'Value',0);
        
        set(handles.button_slice_decr,'Enable','on');
        set(handles.button_slice_incr,'Enable','on');        
        set(handles.slider_slice,'Enable','on'); 
end


plot_image(handles,handles.axes_image);


%== Selection which image
function checkbox_sep_window_Callback(hObject, eventdata, handles)

status_sep = get(handles.checkbox_sep_window,'Value');
if status_sep == 0
    handles.v_axis = [];
end


%== Enable/disable display of second stack
function checkbox_second_stack_Callback(hObject, eventdata, handles)
plot_image(handles,handles.axes_image); 


%== Zoom
function button_zoom_Callback(hObject, eventdata, handles)
if handles.status_zoom == 0
    h_zoom = zoom;
    set(h_zoom,'Enable','on');
    handles.status_zoom = 1;
    handles.status_pan  = 0;
    handles.h_zoom      = h_zoom;
else
    set(handles.h_zoom,'Enable','off');    
    handles.status_zoom = 0;
end
guidata(hObject, handles);


%== Pan
function button_pan_Callback(hObject, eventdata, handles)
if handles.status_pan == 0
    h_pan = pan;
    set(h_pan,'Enable','on');
    handles.status_pan  = 1;
    handles.status_zoom = 0;
    handles.h_pan      = h_pan;    
else
    set(handles.h_pan,'Enable','off');    
    handles.status_pan = 0;
end
guidata(hObject, handles);


%== Cursor
function button_cursor_Callback(hObject, eventdata, handles)

%- Deactivate zoom
if ishandle(handles.h_zoom)
    set(handles.h_zoom,'Enable','off');  
end

%- Deactivate pan
if ishandle(handles.h_pan)
    set(handles.h_pan,'Enable','off');  
end

%-Datacursormode
dcm_obj = datacursormode;

set(dcm_obj,'SnapToDataVertex','off');
set(dcm_obj,'UpdateFcn',@(x,y)myupdatefcn(x,y,handles))



%=== Function for Data cursor
function txt = myupdatefcn(empt,event_obj,handles)

pos    = get(event_obj,'Position');
target = get(event_obj,'Target');

%- Update cursor accordingly
img_plot = handles.img_plot;
x_pos = round(pos(1));
y_pos = round(pos(2));

txt = {['X: ',num2str(x_pos)],...
       ['Y: ',num2str(y_pos)],...
       ['Int: ',num2str(img_plot(y_pos,x_pos))]};
     

% =========================================================================
% Experimental parameters
% =========================================================================

%== Modify parameters
function button_parameters_Callback(hObject, eventdata, handles)

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
set(handles.pop_up_parameters,'Value',3);
guidata(hObject, handles);
pop_up_parameters_Callback(hObject, eventdata, handles) 


%== Popup control
function pop_up_parameters_Callback(hObject, eventdata, handles)
par_microscope = handles.par_microscope;

% Determine the selected data set.
str = get(handles.pop_up_parameters, 'String');
val = get(handles.pop_up_parameters,'Value');

% Set experimental settings based on selection
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
    par_microscope.pixel_size.z  = 200;   
    par_microscope.RI            = 1.458;   
    par_microscope.NA            = 1.4;
    par_microscope.Em            = 568;   
    par_microscope.Ex            = 568;
    par_microscope.type          = 'widefield';   
    
    case 'User settings' 
 
end

%- Update handles structure
handles.par_microscope = par_microscope;
guidata(hObject, handles);



% =========================================================================
% NOT USED
% =========================================================================

function listbox_TS_CreateFcn(hObject, eventdata, handles)

function listbox_cell_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function pop_up_parameters_CreateFcn(hObject, eventdata, handles)

if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_CreateFcn(hObject, eventdata, handles)
if isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor',[.9 .9 .9]);
end

function slider_contrast_min_CreateFcn(hObject, eventdata, handles)
if isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor',[.9 .9 .9]);
end

function slider_contrast_max_CreateFcn(hObject, eventdata, handles)
if isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor',[.9 .9 .9]);
end

function slider_slice_CreateFcn(hObject, eventdata, handles)

if isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor',[.9 .9 .9]);
end

function pop_up_image_CreateFcn(hObject, eventdata, handles)

if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function pop_up_region_Callback(hObject, eventdata, handles)

function pop_up_region_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_th_auto_detect_Callback(hObject, eventdata, handles)

function text_th_auto_detect_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function Untitled_1_Callback(hObject, eventdata, handles)

function Untitled_2_Callback(hObject, eventdata, handles)
