function varargout = FISH_QUANT_spots(varargin)
% FISH_QUANT_SPOTS MATLAB code for FISH_QUANT_spots.fig
%      FISH_QUANT_SPOTS, by itself, creates a new FISH_QUANT_SPOTS or raises the existing
%      singleton*.
%
%      H = FISH_QUANT_SPOTS returns the handle to a new FISH_QUANT_SPOTS or the handle to
%      the existing singleton*.
%
%      FISH_QUANT_SPOTS('CALLBACK',hObject,eventData,handles,...) calls the local
%      function named CALLBACK in FISH_QUANT_SPOTS.M with the given input arguments.
%
%      FISH_QUANT_SPOTS('Property','Value',...) creates a new FISH_QUANT_SPOTS or raises the
%      existing singleton*.  Starting from the left, property value pairs are
%      applied to the GUI before FISH_QUANT_spots_OpeningFcn gets called.  An
%      unrecognized property name or invalid value makes property application
%      stop.  All inputs are passed to FISH_QUANT_spots_OpeningFcn via varargin.
%
%      *See GUI Options on GUIDE's Tools menu.  Choose "GUI allows only one
%      instance to run (singleton)".
%
% See also: GUIDE, GUIDATA, GUIHANDLES

% Edit the above text to modify the response to help FISH_QUANT_spots

% Last Modified by GUIDE v2.5 25-Jul-2011 16:58:53

% Begin initialization code - DO NOT EDIT
gui_Singleton = 1;
gui_State = struct('gui_Name',       mfilename, ...
                   'gui_Singleton',  gui_Singleton, ...
                   'gui_OpeningFcn', @FISH_QUANT_spots_OpeningFcn, ...
                   'gui_OutputFcn',  @FISH_QUANT_spots_OutputFcn, ...
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


% --- Executes just before FISH_QUANT_spots is made visible.
function FISH_QUANT_spots_OpeningFcn(hObject, eventdata, handles, varargin)


%- Set font-size to 10
%  For whatever reason are all the fonts on windows are set back to 8 when the
%  .fig is openend
h_font_8 = findobj(handles.h_fishquant_spots,'FontSize',8);
set(h_font_8,'FontSize',10)

%- Get installation directory of FISH-QUANT and initiate 
p = mfilename('fullpath');        
handles.FQ_path = fileparts(p); 
handles         = FISH_QUANT_start_up_v2(handles);

%- Change name of GUI
set(handles.h_fishquant_spots,'Name', ['FISH-QUANT ', handles.version, ': spot inspector']);

%= Some parameters
handles.image_struct = [];
handles.status_data_cursor = 0;
handles.status_zoom = 0;
handles.h_zoom = rand(1);
handles.status_pan = 0;
handles.status_plot_first = 1;  % Indicate if plot command was never used
handles.h_pan = rand(1);

handles.marker_size_spot = 10;
handles.marker_extend_z  = 3;

%= Load data if called from other GUI
%- Some controls are different when spot inspector is called from main
%interface or as stand-alone GUI
handles.status_child = 0;
set(handles.checkbox_remove_man,'Enable','on')

%- Matlab stuff
handles.output = hObject;
guidata(hObject, handles);

%- Check if called from another GUI
if not(isempty(varargin))
    
    %- Specific controls 
    handles.status_child = 1;
    set(handles.checkbox_remove_man,'Enable','off')

    %- Read data from Main GUI
    handles_MAIN = varargin{2};
    
    handles.col_par = handles_MAIN.col_par;

    handles.cell_prop          = handles_MAIN.cell_prop;
    handles.par_microscope     = handles_MAIN.par_microscope;

    handles.file_name_image    = handles_MAIN.file_name_image;
    handles.path_name_image    = handles_MAIN.path_name_image;
    handles.file_name_settings = handles_MAIN.file_name_settings;
    handles.file_name_image_filtered = handles_MAIN.file_name_image_filtered;

    %- Other parameters
    handles.detect.region   = handles_MAIN.detect.region;        
    handles.marker_extend_z = handles.detect.region.z;

    %- Get image data
    handles.image_struct = handles_MAIN.image_struct;

    %- Analyze results
    handles = analyze_results(hObject, eventdata, handles);

    %- Analyze detected regions
    handles = analyze_cellprop(hObject, eventdata, handles);    

    %- Set selector of plot-type accordingly 
    handles.ident_caller = varargin{1};   
     
     if      strcmp( varargin{1},'HandlesMainGui') 
        set(handles.pop_up_image,'Value',1);
        
     elseif  strcmp( varargin{1},'MS2Q_2D')    
        set(handles.pop_up_image,'Value',2);  
     end    
     
     
    %- Save everything
    guidata(hObject, handles); 
    pop_up_image_Callback(hObject, eventdata, handles)
    handles = plot_image(hObject, eventdata, handles);

else
    handles.ident_caller = [];

    handles.col_par.pos_y  = 1;
    handles.col_par.pos_x  = 2;
    handles.col_par.pos_z  = 3;
    handles.col_par.amp    = 4;
    handles.col_par.bgd    = 5;
    handles.col_par.sigmax = 7;
    handles.col_par.sigmay = 8;
    handles.col_par.sigmaz = 9;
    handles.col_par.pos_x_sub = 14;
    handles.col_par.pos_y_sub = 13;
    handles.col_par.pos_z_sub = 15;    
    
    %- Save everything
    guidata(hObject, handles); 
end

%- Export figure handle to workspace - will be used in Close All button of
% main Interface
assignin('base','h_spots',handles.h_fishquant_spots)


% --- Outputs from this function are returned to the command line.
function varargout = FISH_QUANT_spots_OutputFcn(hObject, eventdata, handles) 
varargout{1} = handles.output;


% --- Executes when user attempts to close h_fishquant_spots.
function h_fishquant_spots_CloseRequestFcn(hObject, eventdata, handles)
if handles.status_child 
    %uiresume(handles.h_fishquant_outline)
else    
    delete(handles.h_fishquant_spots)
end

% =========================================================================
% Load and save
% =========================================================================


%== Save results of spot detection
function menu_save_spots_Callback(hObject, eventdata, handles)

parameters.cell_prop                = handles.cell_prop;
parameters.par_microscope           = handles.par_microscope;
parameters.path_name_image          = handles.file_name_image;
parameters.file_name_image          = handles.file_name_image;
parameters.file_name_image_filtered = handles.file_name_image_filtered;
parameters.file_name_settings       = handles.file_name_settings;
parameters.version                  = handles.version;

file_name_results = FISH_QUANT_save_results_v7([],parameters);
handles.file_name_results  = file_name_results;
guidata(hObject, handles);


%=== Load results of spot detection
function menu_load_spots_Callback(hObject, eventdata, handles)

[file_name_results,path_name_results] = uigetfile({'*.txt'},'Select file with results of spot detection');

if file_name_results ~= 0
    
    %- Reset GUI
    FISH_QUANT_spots_OpeningFcn(hObject, eventdata, handles); 
    
    %- Load results
    [cell_prop file_name par_microscope file_name_settings file_name_image_filtered] = FISH_QUANT_load_results_v7(fullfile(path_name_results,file_name_results));
    
    %- Load settings
    handles = FISH_QUANT_load_settings_v3(fullfile(path_name_results,file_name_settings),handles);
    
    %- Assign results
    handles.cell_prop                 = cell_prop;
    handles.path_name_image           = path_name_results;
    handles.file_name_image           = file_name;
    handles.par_microscope            = par_microscope;
    handles.file_name_settings        = file_name_settings;
    handles.file_name_image_filtered  = file_name_image_filtered;  
   
    %- Load image 
    handles.image_struct    = load_stack_data_v4(fullfile(handles.path_name_image,handles.file_name_image));    
    
    %- Load filtered image if specified 
    if not(isempty(handles.file_name_image_filtered))
        image_filt  = load_stack_data_v4(fullfile(handles.path_name_image,handles.file_name_image_filtered));
        handles.image_struct.data_filtered = image_filt.data;
    else     
        handles.image_struct.data_filtered = handles.image_struct.data;
    end    
        
    %- Analyze results
    handles = analyze_results(hObject, eventdata, handles);
    
    %- Analyze detected regions
    handles = analyze_cellprop(hObject, eventdata, handles);    
    
    %- Save everything
    guidata(hObject, handles); 
    handles = plot_image(hObject, eventdata, handles);

end


%= Function to analyze detected regions
function handles = analyze_results(hObject, eventdata, handles)

%- Analyze image
handles.img_plot      =  max(handles.image_struct.data,[],3); 
handles.img_min       =  min(handles.image_struct.data(:)); 
handles.img_max       =  max(handles.image_struct.data(:)); 
handles.img_diff      =  handles.img_max-handles.img_min; 

set(handles.text_contr_min,'String',num2str(round(handles.img_min)));
set(handles.text_contr_max,'String',num2str(round(handles.img_max)));

%- Analyze filtered image
handles.img_filt_plot      =  max(handles.image_struct.data_filtered,[],3); 
handles.img_filt_min       =  min(handles.image_struct.data_filtered(:)); 
handles.img_filt_max       =  max(handles.image_struct.data_filtered(:)); 
handles.img_filt_diff      =  handles.img_filt_max-handles.img_filt_min; 


%- Select min, max of image
type_select = get(handles.pop_up_filtered,'Value');

if type_select == 1
    handles.img_min_plot  = handles.img_min;
    handles.img_diff_plot = handles.img_diff;

elseif type_select == 2
    handles.img_min_plot  = handles.img_filt_min;
    handles.img_diff_plot = handles.img_filt_diff;
end
    
%- Save everything
guidata(hObject, handles); 


%= Function to analyze detected regions
function handles = analyze_cellprop(hObject, eventdata, handles)

cell_prop = handles.cell_prop;

%- Populate pop-up menu with labels of cells
N_cell = size(cell_prop,2);

[dim.Y dim.X dim.Z] = size(handles.image_struct.data);

if N_cell > 0

    %- Call pop-up function to show results and bring values into GUI
    for i = 1:N_cell
        str_menu{i,1} = cell_prop(i).label;
     
        %- Calculate projections for plot with montage function
        spots_proj_GUI = [];
        
        if isempty(cell_prop(i).FIT_Result)
        
            for k=1:size(cell_prop(i).spots_fit,1)

                spots_detected = cell_prop(i).spots_detected;

                y_min = spots_detected(k,4);
                y_max = spots_detected(k,5);
                x_min = spots_detected(k,6);
                x_max = spots_detected(k,7);         
                z_min = spots_detected(k,8);
                z_max = spots_detected(k,9);

                img_sub = handles.image_struct.data(y_min:y_max,x_min:x_max,z_min:z_max);            

                spots_proj_GUI(k).xy = max(img_sub,[],3);
                spots_proj_GUI(k).xz = squeeze(max(img_sub,[],1))';
                spots_proj_GUI(k).yz = squeeze(max(img_sub,[],2))'; 
                
                spots_proj_GUI(k).xy_fit = [];
                spots_proj_GUI(k).xz_fit = [];
                spots_proj_GUI(k).yz_fit = []'; 
                
            end
        else
            for k=1:length(cell_prop(i).FIT_Result)
                spot_image = cell_prop(i).sub_spots{k};
                spot_fit   = cell_prop(i).FIT_Result{k}.img_fit;
                
                spots_proj_GUI(k).xy = max(spot_image,[],3);
                spots_proj_GUI(k).xz = squeeze(max(spot_image,[],1))';
                spots_proj_GUI(k).yz = squeeze(max(spot_image,[],2))'; 
                
                spots_proj_GUI(k).xy_fit = max(spot_fit,[],3);
                spots_proj_GUI(k).xz_fit = squeeze(max(spot_fit,[],1))';
                spots_proj_GUI(k).yz_fit = squeeze(max(spot_fit,[],2))';                 
                
                
            end
            
        end
            
            
        %- Save results
        cell_prop(i).spots_proj_GUI  = spots_proj_GUI;
    end  
else
    str_menu = {' '};
end

%- Save and analyze results
set(handles.pop_up_cell_select,'String',str_menu);
set(handles.pop_up_cell_select,'Value',1);

handles.cell_prop  = cell_prop;

%- Save everything
guidata(hObject, handles); 


% =========================================================================
% Different functions
% =========================================================================

%=== Manually remove one data-point
function checkbox_remove_man_Callback(hObject, eventdata, handles)
datacursormode off

ind_spot = str2double(get(handles.text_spot_id,'String'));

if not(isnan(ind_spot))

    %- Which cell
    ind_cell   = get(handles.pop_up_cell_select,'Value');
    
    check_box = get(handles.checkbox_remove_man,'Value');
        
    if check_box == 1
        handles.cell_prop(ind_cell).thresh.in(ind_spot) = -1;
     else
        handles.cell_prop(ind_cell).thresh.in(ind_spot) = 1;
    end
        
    %- Save everything and plot
    handles = plot_image(hObject, eventdata, handles);
    guidata(hObject, handles);
end

%- Uncheck check-box and start datacursormode again
set(handles.checkbox_remove_man,'Value',0);
button_imgage_data_cursor_Callback(hObject, eventdata, handles)


%=== Options
function menu_options_Callback(hObject, eventdata, handles)

dlgTitle = 'Different parameters for visualization';

prompt(1) = {'Size for circles to show spots'};
prompt(2) = {'[Z-stack] show spots for +/- frames'};

defaultValue{1} = num2str(handles.marker_size_spot);
defaultValue{2} = num2str(handles.marker_extend_z);

userValue = inputdlg(prompt,dlgTitle,1,defaultValue);

if( ~ isempty(userValue))
    handles.marker_size_spot = str2double(userValue{1}); 
    handles.marker_extend_z = str2double(userValue{2});     
end

guidata(hObject, handles);
 

%=== Close window
function button_close_window_Callback(hObject, eventdata, handles)
button = questdlg('Are you sure that you want to close the GUI?','Close GUI','Yes','No','No');

if strcmp(button,'Yes')    
    delete(handles.h_fishquant_spots)   
end


% =========================================================================
% Plot
% =========================================================================

%=== Plot cell
function handles = plot_image(hObject, eventdata, handles)
datacursormode off

col_par = handles.col_par;

pixel_size = handles.par_microscope.pixel_size;
N_slice    = handles.image_struct.size;

%- Select output axis
axes(handles.axes_main)
v = axis;

%- Contrast of image
Im_min = str2double(get(handles.text_contr_min,'String'));
Im_max = str2double(get(handles.text_contr_max,'String'));

%- Determine which image should be shown
str = get(handles.pop_up_image, 'String');
val = get(handles.pop_up_image,'Value');

% Set experimental settings based on selection
switch str{val};
    
    case 'Maximum projection' 
       
        %- Raw or filtered
        type_select = get(handles.pop_up_filtered,'Value');
        if type_select == 1
            handles.h_img = imshow(handles.img_plot,[Im_min Im_max]);
        elseif type_select == 2
            handles.h_img = imshow(handles.img_filt_plot,[Im_min Im_max]);    
        end
    
        title('Maximum projection of loaded image','FontSize',9);
        colormap(hot)
        handles.img_plot_GUI = handles.img_plot;
        status_z_stack = 0;
        set(handles.text_z_slice,'String','');
        
    case '3D-Stack'
    
        ind_plot = str2double(get(handles.text_z_slice,'String'));
        
        %- Raw or filtered
        type_select = get(handles.pop_up_filtered,'Value');
        if type_select == 1
            img_plot = handles.image_struct.data(:,:,ind_plot);
        elseif type_select == 2
            img_plot = handles.image_struct.data_filtered(:,:,ind_plot);   
        end       
        
        handles.h_img = imshow(img_plot,[Im_min Im_max]); 
        title(['slice #' , num2str(ind_plot) , ' of ', num2str(N_slice)],'FontSize',9);
        colormap(hot)
        handles.img_plot_GUI = img_plot;

        status_z_stack = 1;
        z_min = ind_plot - handles.marker_extend_z;
        z_max = ind_plot + handles.marker_extend_z;

        if z_min < 1; z_min = 1;end;
        if z_max > handles.image_struct.size; z_max = handles.image_struct.size; end;    

        set(handles.text_z_slice,'String',num2str(ind_plot));
end

%- Which cell
ind_cell      = get(handles.pop_up_cell_select,'Value');

%- SPOTS
spots_fit       = handles.cell_prop(ind_cell).spots_fit;
spots_detected  = handles.cell_prop(ind_cell).spots_detected;
thresh          = handles.cell_prop(ind_cell).thresh;

%- Select spots which will be shown
ind_plot_in      = thresh.in == 1;
ind_plot_out     = thresh.in == 0;
ind_plot_out_man = thresh.in == -1 ;

ind_all      = 1:length(thresh.in);  

%- Plot spots
if status_z_stack == 1 
    
    ind_spots_range = spots_detected(:,3) >= z_min & spots_detected(:,3) <= z_max; 
else
    ind_spots_range = true(size(ind_plot_in));
end

%- Get the relative indices - separate plots for the spots but in
%  the selection we refer back to the complete lists of spots
handles.ind_rel_in      = ind_all(ind_spots_range & ind_plot_in);
handles.ind_rel_out     = ind_all(ind_spots_range & ind_plot_out);
handles.ind_rel_out_man = ind_all(ind_spots_range & ind_plot_out_man); 


%- Plot spots if any are in range
hold on    

if sum(ind_spots_range & ind_plot_in)
    handles.h_spots_in  = plot((spots_fit(ind_spots_range & ind_plot_in,col_par.pos_x)/pixel_size.xy + 1), (spots_fit(ind_spots_range & ind_plot_in,col_par.pos_y)/pixel_size.xy + 1),'og','MarkerSize',handles.marker_size_spot);
else
    handles.h_spots_in = 0;
end

if sum(ind_spots_range & ind_plot_out)
    handles.h_spots_out = plot((spots_fit(ind_spots_range & ind_plot_out,col_par.pos_x)/pixel_size.xy + 1), (spots_fit(ind_spots_range & ind_plot_out,col_par.pos_y)/pixel_size.xy + 1),'ob','MarkerSize',handles.marker_size_spot);
else
    handles.h_spots_out = 0;
end

%if not(isempty(handles.ind_rel_out_man))
if sum(ind_spots_range & ind_plot_out_man)
    handles.h_spots_out_man = plot((spots_fit(ind_spots_range & ind_plot_out_man,col_par.pos_x)/pixel_size.xy + 1), (spots_fit(ind_spots_range & ind_plot_out_man,col_par.pos_y)/pixel_size.xy + 1),'om','MarkerSize',handles.marker_size_spot);
else
    handles.h_spots_out_man = 0;
end

hold off

colormap(hot)
freezeColors(gca)

if     sum(ind_spots_range & ind_plot_in) && sum(ind_spots_range & ind_plot_out) && sum(ind_spots_range & ind_plot_out_man)
    legend('Selected Spots','Rejected Spots [auto]','Rejected Spots [man]');  

elseif sum(ind_spots_range & ind_plot_in) && sum(ind_spots_range & ind_plot_out) 
    legend('Selected Spots','Rejected Spots [auto]');      

elseif sum(ind_spots_range & ind_plot_in) && sum(ind_spots_range & ind_plot_out_man)
    legend('Selected Spots','Rejected Spots [man]');   

elseif sum(ind_spots_range & ind_plot_out) && sum(ind_spots_range & ind_plot_out_man)
    legend('Rejected Spots [auto]','Rejected Spots [man]');     
    
elseif sum(ind_spots_range & ind_plot_in) 
    legend('Selected Spots');      

elseif sum(ind_spots_range & ind_plot_out)
    legend('Rejected Spots [auto]');   

elseif sum(ind_spots_range & ind_plot_out_man)
    legend('Rejected Spots [man]');        
end 

%- Plot outline of cell and TS
hold on
if isfield(handles,'cell_prop')    
    cell_prop = handles.cell_prop;    
    if not(isempty(cell_prop))  
        for i = ind_cell:ind_cell %1:size(cell_prop,2)
            x = cell_prop(i).x;
            y = cell_prop(i).y;
            plot([x,x(1)],[y,y(1)],'b','Linewidth', 2)     
          
    
            %- TS
            pos_TS   = cell_prop(i).pos_TS;   
            if not(isempty(pos_TS))  
                for i = 1:size(pos_TS,2)
                    x = pos_TS(i).x;
                    y = pos_TS(i).y;
                    plot([x,x(1)],[y,y(1)],'g','Linewidth', 2)  

                end                
            end  
        end
    end        
end   
hold off

%- Same zoom as before
if not(handles.status_plot_first)
    axis(v);
end

%- Save everything
handles.status_plot_first = 0;
handles.h_fig_plot = gcf;
handles.h_gca_plot = gca;
guidata(hObject, handles); 


%== Slider: contrast minimum
function slider_contrast_min_Callback(hObject, eventdata, handles)
slider_min = get(handles.slider_contrast_min,'Value');

img_min  = handles.img_min_plot;
img_diff = handles.img_diff_plot;

contr_min = slider_min*img_diff+img_min;
set(handles.text_contr_min,'String',num2str(round(contr_min)));

handles = plot_image(hObject, eventdata, handles);
guidata(hObject, handles);


%== Slider: contrast maximum
function slider_contrast_max_Callback(hObject, eventdata, handles)

slider_min = get(handles.slider_contrast_min,'Value');
slider_max = get(handles.slider_contrast_max,'Value');

img_min  = handles.img_min_plot;
img_diff = handles.img_diff_plot;

contr_min = slider_min*img_diff+img_min;
contr_max = slider_max*img_diff+img_min;

if contr_max < contr_min
    contr_max = contr_min+1;
end
set(handles.text_contr_max,'String',num2str(round(contr_max)));

handles = plot_image(hObject, eventdata, handles);
guidata(hObject, handles); 


%== Slider to select z-plane
function slider_slice_Callback(hObject, eventdata, handles)
N_slice      = handles.image_struct.size;
slider_value = get(handles.slider_slice,'Value');

ind_slice = round(slider_value*(N_slice-1)+1);
set(handles.text_z_slice,'String',num2str(ind_slice));

handles = plot_image(hObject, eventdata, handles);
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
handles = plot_image(hObject, eventdata, handles);
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
handles = plot_image(hObject, eventdata, handles);
guidata(hObject, handles);


%== Select different way to plot image
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
    
    case '3D-Stack'
        set(handles.text_z_slice,'String',1);
        set(handles.slider_slice,'Value',0);
        
        set(handles.button_slice_decr,'Enable','on');
        set(handles.button_slice_incr,'Enable','on');        
        set(handles.slider_slice,'Enable','on'); 
end

handles = plot_image(hObject, eventdata, handles);
guidata(hObject, handles); 


%== Select raw vs. filtered image
function pop_up_filtered_Callback(hObject, eventdata, handles)

type_select = get(handles.pop_up_filtered,'Value');

set(handles.slider_contrast_min,'Value',0);
set(handles.slider_contrast_max,'Value',1);
    
if type_select == 1
    handles.img_min_plot  = handles.img_min;
    handles.img_diff_plot = handles.img_diff;   
    
    set(handles.text_contr_min,'String',num2str(round(handles.img_min)));
    set(handles.text_contr_max,'String',num2str(round(handles.img_max)));
    
elseif type_select == 2
    handles.img_min_plot  = handles.img_filt_min;
    handles.img_diff_plot = handles.img_filt_diff;
    
    set(handles.text_contr_min,'String',num2str(round(handles.img_filt_min)));
    set(handles.text_contr_max,'String',num2str(round(handles.img_filt_max)));
    
end

handles = plot_image(hObject, eventdata, handles);
guidata(hObject, handles); 


%=== Image region
function button_image_region_Callback(hObject, eventdata, handles)%impixelregion(handles.axes_main)
if not(isfield(handles,'h_impixregion'))
    handles.h_impixregion = impixelregion(handles.axes_main);
else 
    if not(ishandle(handles.h_impixregion))
        handles.h_impixregion = impixelregion(handles.axes_main);
    end
end
guidata(hObject, handles);


%=== Data cursor
function button_imgage_data_cursor_Callback(hObject, eventdata, handles)

%- Delete region inspector if present
if isfield(handles,'h_impixregion')   
    if ishandle(handles.h_impixregion)
        delete(handles.h_impixregion)
    end
end

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
function txt = myupdatefcn(empty,event_obj,handles)


   
%  First check is too make sure that the call is really for the current
%  figure. There are some problems with this function call ...
if gcf == handles.h_fishquant_spots     

    col_par = handles.col_par;

    pos    = get(event_obj,'Position');
    target = get(event_obj,'Target');
    index  = get(event_obj,'DataIndex');

    %- Plot subregions of image

    if  target == handles.h_spots_in || target == handles.h_spots_out  || target == handles.h_spots_out_man
       ind_cell   = get(handles.pop_up_cell_select,'Value');
       spots_fit  = handles.cell_prop(ind_cell).spots_fit;
       thresh     = handles.cell_prop(ind_cell).thresh;

       pixel_size    = handles.par_microscope.pixel_size;

       %- Get index of target and link back to original spot number   
       if         target == handles.h_spots_in
            spot_ind = handles.ind_rel_in(index);   
       elseif     target == handles.h_spots_out
            spot_ind = handles.ind_rel_out(index); 
       elseif     target == handles.h_spots_out_man
            spot_ind = handles.ind_rel_out_man(index);           
       end

       img_xy = handles.cell_prop(ind_cell).spots_proj_GUI(spot_ind).xy;
       img_xz = handles.cell_prop(ind_cell).spots_proj_GUI(spot_ind).xz;
       img_yz = handles.cell_prop(ind_cell).spots_proj_GUI(spot_ind).yz;
       
       fit_xy = handles.cell_prop(ind_cell).spots_proj_GUI(spot_ind).xy_fit;
       fit_xz = handles.cell_prop(ind_cell).spots_proj_GUI(spot_ind).xz_fit;
       fit_yz = handles.cell_prop(ind_cell).spots_proj_GUI(spot_ind).yz_fit;

       spot_pos_x = spots_fit(spot_ind,col_par.pos_x_sub);
       spot_pos_y = spots_fit(spot_ind,col_par.pos_y_sub);
       spot_pos_z = spots_fit(spot_ind,col_par.pos_z_sub);

       %- Plot the sub-images
       Im_min = str2double(get(handles.text_contr_min,'String'));
       Im_max = str2double(get(handles.text_contr_max,'String'));   

       axes(handles.axes_zoom_xy)   
       %imshow(img_xy,[Im_min Im_max],'Parent',handles.axes_zoom_xy ,'XData',[0 (size(img_xy,2)-1)*pixel_size.xy],'YData',[0 (size(img_xy,1)-1)*pixel_size.xy])
       imshow(img_xy,[Im_min Im_max],'XData',[0 (size(img_xy,2)-1)*pixel_size.xy],'YData',[0 (size(img_xy,1)-1)*pixel_size.xy])
      
       colormap(hot)       
       colorbar('peer',handles.axes_zoom_xy) 
       hold on
            plot(handles.axes_zoom_xy,spot_pos_x,spot_pos_y,'+g')
       hold off
  
       axes(handles.axes_zoom_xz)
       imshow(img_xz,[Im_min Im_max],'Parent',handles.axes_zoom_xz ,'XData',[0 (size(img_xz,2)-1)*pixel_size.xy],'YData',[0 (size(img_xz,1)-1)*pixel_size.z])
       colormap(hot)
       colorbar('peer',handles.axes_zoom_xz)
       hold on
            plot(handles.axes_zoom_xz,spot_pos_x,spot_pos_z,'+g')
       hold off

       axes(handles.axes_zoom_yz)   
       imshow(img_yz,[Im_min Im_max],'Parent',handles.axes_zoom_yz ,'XData',[0 (size(img_yz,2)-1)*pixel_size.xy],'YData',[0 (size(img_yz,1)-1)*pixel_size.z])
       colormap(hot)
       colorbar('peer',handles.axes_zoom_yz)
       hold on
            plot(handles.axes_zoom_yz,spot_pos_y,spot_pos_z,'+g')
       hold off

       %- Plot fit
       if not(isempty(fit_xy))
           axes(handles.axes_fit_xy)   
           imshow(fit_xy,[Im_min Im_max],'Parent',handles.axes_fit_xy ,'XData',[0 (size(img_xy,2)-1)*pixel_size.xy],'YData',[0 (size(img_xy,1)-1)*pixel_size.xy])
           colormap(hot)       
           colorbar('peer',handles.axes_fit_xy) 
           hold on
                plot(handles.axes_fit_xy,spot_pos_x,spot_pos_y,'+g')
           hold off

           axes(handles.axes_fit_xz)
           imshow(fit_xz,[Im_min Im_max],'Parent',handles.axes_fit_xz ,'XData',[0 (size(img_xz,2)-1)*pixel_size.xy],'YData',[0 (size(img_xz,1)-1)*pixel_size.z])
           colormap(hot)
           colorbar('peer',handles.axes_fit_xz)
           hold on
                plot(handles.axes_fit_xz,spot_pos_x,spot_pos_z,'+g')
           hold off

           axes(handles.axes_fit_yz)   
           imshow(fit_yz,[Im_min Im_max],'Parent',handles.axes_fit_yz ,'XData',[0 (size(img_yz,2)-1)*pixel_size.xy],'YData',[0 (size(img_yz,1)-1)*pixel_size.z])
           colormap(hot)
           colorbar('peer',handles.axes_fit_yz)
           hold on
                plot(handles.axes_fit_yz,spot_pos_y,spot_pos_z,'+g')
           hold off

       else
           
        	cla(handles.axes_fit_xy)
            cla(handles.axes_fit_xz)
            cla(handles.axes_fit_yz)
       end
       
       
       %- Update information about spot
       set(handles.text_spot_id,'String',round(spot_ind));
       set(handles.text_sigmaxy,'String',round(spots_fit(spot_ind,col_par.sigmax)));
       set(handles.text_sigmaz,'String',round(spots_fit(spot_ind,col_par.sigmaz)));
       set(handles.text_amp,'String',round(spots_fit(spot_ind,col_par.amp)));
       set(handles.text_bgd,'String',round(spots_fit(spot_ind,col_par.bgd)));

       %- Set selector for manual removal
       if thresh.in(spot_ind) == -1
           set(handles.checkbox_remove_man,'Value',1);
       else
           set(handles.checkbox_remove_man,'Value',0);
       end
    else

        cla(handles.axes_zoom_xy)
        cla(handles.axes_zoom_xz)
        cla(handles.axes_zoom_yz)
        
        cla(handles.axes_fit_xy,'reset')
        cla(handles.axes_fit_xz)
        cla(handles.axes_fit_yz)

        set(handles.text_spot_id,'String','');
        set(handles.text_sigmaxy,'String','');
        set(handles.text_sigmaz,'String','');
        set(handles.text_amp,'String','');
        set(handles.text_bgd,'String','');

        set(handles.checkbox_remove_man,'Value',0);
    end


    %- Update cursor accordingly
    img_plot = handles.img_plot_GUI;
    x_pos = round(pos(1));
    y_pos = round(pos(2));


    if     target == handles.h_spots_in
        txt = {['Good spot: ',num2str(spot_ind)], ...
               ['X: ',num2str(x_pos)],...
               ['Y: ',num2str(y_pos)],...
               ['Int: ',num2str(img_plot(y_pos,x_pos))]};

    elseif target == handles.h_spots_out
        txt = {['Bad spot [auto]: ',num2str(spot_ind)], ...
               ['X: ',num2str(x_pos)],...
               ['Y: ',num2str(y_pos)],...
               ['Int: ',num2str(img_plot(y_pos,x_pos))]}; 
    elseif target == handles.h_spots_out_man
        txt = {['Bad spot [man]: ',num2str(spot_ind)], ...
               ['X: ',num2str(x_pos)],...
               ['Y: ',num2str(y_pos)],...
               ['Int: ',num2str(img_plot(y_pos,x_pos))]}; 
    else
        txt = {['X: ',num2str(x_pos)],...
               ['Y: ',num2str(y_pos)],...
               ['Int: ',num2str(img_plot(y_pos,x_pos))]};
    end
    else
        txt = [''];
end

%=== Function for Cell selector
function pop_up_cell_select_Callback(hObject, eventdata, handles)
handles = plot_image(hObject, eventdata, handles);
guidata(hObject, handles);


%== ZOOM button
function button_zoom_in_Callback(hObject, eventdata, handles)
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


%== PAN button
function menu_pan_Callback(hObject, eventdata, handles)
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


% =========================================================================
% Not used
% =========================================================================

function slider_contrast_max_CreateFcn(hObject, eventdata, handles)
if isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor',[.9 .9 .9]);
end

function slider_contrast_min_CreateFcn(hObject, eventdata, handles)
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

function Untitled_1_Callback(hObject, eventdata, handles)

function pushbutton1_Callback(hObject, eventdata, handles)

function pop_up_filtered_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function pop_up_cell_select_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function axes_main_ButtonDownFcn(hObject, eventdata, handles)

function zoom_in_ClickedCallback(hObject, eventdata, handles)
h = zoom;

function mycallback_zoom_in(obj,event_obj)
h = zoom;
