function varargout = FISH_QUANT_predetect(varargin)
% FISH_QUANT_PREDETECT MATLAB code for FISH_QUANT_predetect.fig
%      FISH_QUANT_PREDETECT, by itself, creates a new FISH_QUANT_PREDETECT or raises the existing
%      singleton*.
%
%      H = FISH_QUANT_PREDETECT returns the handle to a new FISH_QUANT_PREDETECT or the handle to
%      the existing singleton*.
%
%      FISH_QUANT_PREDETECT('CALLBACK',hObject,eventData,handles,...) calls the local
%      function named CALLBACK in FISH_QUANT_PREDETECT.M with the given input arguments.
%
%      FISH_QUANT_PREDETECT('Property','Value',...) creates a new FISH_QUANT_PREDETECT or raises the
%      existing singleton*.  Starting from the left, property value pairs are
%      applied to the GUI before FISH_QUANT_predetect_OpeningFcn gets called.  An
%      unrecognized property name or invalid value makes property application
%      stop.  All inputs are passed to FISH_QUANT_predetect_OpeningFcn via varargin.
%
%      *See GUI Options on GUIDE's Tools menu.  Choose "GUI allows only one
%      instance to run (singleton)".
%
% See also: GUIDE, GUIDATA, GUIHANDLES

% Edit the above text to modify the response to help FISH_QUANT_predetect

% Last Modified by GUIDE v2.5 31-Oct-2011 13:09:37

% Begin initialization code - DO NOT EDIT
gui_Singleton = 1;
gui_State = struct('gui_Name',       mfilename, ...
                   'gui_Singleton',  gui_Singleton, ...
                   'gui_OpeningFcn', @FISH_QUANT_predetect_OpeningFcn, ...
                   'gui_OutputFcn',  @FISH_QUANT_predetect_OutputFcn, ...
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


% --- Executes just before FISH_QUANT_predetect is made visible.
function FISH_QUANT_predetect_OpeningFcn(hObject, eventdata, handles, varargin)
handles.output = hObject;

%- Set font-size to 10
%  For whatever reason are all the fonts on windows are set back to 8 when the
%  .fig is openend
h_font_8 = findobj(handles.h_fishquant_predetect,'FontSize',8);
set(h_font_8,'FontSize',10)

%- Get installation directory of FISH-QUANT and initiate 
p = mfilename('fullpath');        
handles.FQ_path = fileparts(p); 
handles = FISH_QUANT_start_up_v2(handles);

%- Change name of GUI
set(handles.h_fishquant_predetect,'Name', ['FISH-QUANT ', handles.version, ': pre-detection']);

assignin('base','h_predetect',handles.h_fishquant_predetect)

%- Other parameters
handles.status_pre_detect    = 0;
handles.status_quality_score = 0;

%= Get data from main GUI
if not(isempty(varargin))

    if strcmp( varargin{1},'HandlesMainGui')
        
        handles.child = 1;        
        
        handles_MAIN = varargin{2};
        
        handles.par_microscope = handles_MAIN.par_microscope;

        handles.file_name_settings       = handles_MAIN.file_name_settings;
        handles.file_name_image_filtered = handles_MAIN.file_name_image_filtered;
     
        %- Parameters from the main GUI
        handles.PSF_theo       = handles_MAIN.PSF_theo;
                        
        %- Thresholding parameters
        handles.detect.thresh_int     = handles_MAIN.detect.thresh_int;
        handles.detect.thresh_score   = handles_MAIN.detect.thresh_score;
        handles.detect.score          = handles_MAIN.detect.score;
        handles.detect.region         = handles_MAIN.detect.region;
        handles.detect.flag_region_smaller  = handles_MAIN.detect.flag_region_smaller;
        
        set(handles.text_detect_region_xy,'String',num2str(handles.detect.region.xy))
        set(handles.text_detect_region_z,'String',num2str(handles.detect.region.z))                
                
        set(handles.text_detection_threshold,'String',handles.detect.thresh_int);
        set(handles.text_detect_th_qual,'String',handles.detect.thresh_score);        
        
        set(handles.checkbox_smaller_detection,'Value',handles.detect.flag_region_smaller);
        
        str_scores = get(handles.pop_up_detect_quality, 'String');
        str_match  = find(strcmpi(handles.detect.score ,str_scores));        
        set(handles.pop_up_detect_quality,'Value',str_match);
        
        %- Selected cell in main window               
        handles.cell_ind_main = get(handles_MAIN.pop_up_outline_sel_cell,'Value');        
        
        %- Image data
        handles.image_struct   = handles_MAIN.image_struct;
        
        handles.img_lin = handles.image_struct.data_filtered(:);
        img_lin_no_0    = handles.img_lin;
        img_lin_no_0(handles.img_lin == 0) = [];
        handles.img_lin_no_0 = img_lin_no_0;
        
        handles.img_min = min(handles.img_lin);
        handles.img_max = max(handles.img_lin);
        handles.img_diff = handles.img_max - handles.img_min;
        handles.img_plot = max(handles.image_struct.data_filtered,[],3);
        
        %- If not cell_prop are defined 
        handles.cell_prop      = handles_MAIN.cell_prop;
        if isempty(handles.cell_prop) 
            handles.cell_prop = define_cell_prop(handles);
        end       
        
        %- Calc the histogram
        [n,xout] = hist(handles.img_lin_no_0,5000);

        xout(n==0) =[];
        n(n==0) =[];
        handles.xout = xout;
        handles.n = n;                
       
        %- Plot results
        plot_hist_int(hObject, eventdata, handles)
        
        
elseif strcmp( varargin{1},'MS2Q_2D')       
        
        %=== Handles structure from calling GUI
        handles_MAIN = varargin{2};
        handles.cell_prop      = handles_MAIN.cell_prop;
        handles.par_microscope = handles_MAIN.par_microscope;         
        handles.file_name_settings       = handles_MAIN.file_name_settings;
        handles.file_name_image_filtered = handles_MAIN.file_name_image_filtered;
        handles.PSF_theo       = handles_MAIN.PSF_theo;       
                 
        %- No detection in Z [stack is not a z-stack but a time-series
        set(handles.text_detect_region_z,'Enable','off');  
        
        %=== Thresholding parameters
        handles.detect.thresh_int     = handles_MAIN.detect.thresh_int;
        handles.detect.thresh_score   = handles_MAIN.detect.thresh_score;
        handles.detect.score          = handles_MAIN.detect.score;
        handles.detect.region         = handles_MAIN.detect.region;
                
        set(handles.text_detect_region_xy,'String',num2str(handles.detect.region.xy))
        set(handles.text_detect_region_z,'String',num2str(handles.detect.region.z))
        
        set(handles.text_detection_threshold,'String',handles.detect.thresh_int);
        set(handles.text_detect_th_qual,'String',handles.detect.thresh_score);        
              
        str_scores = get(handles.pop_up_detect_quality, 'String');
        str_match  = find(strcmpi(handles.detect.score ,str_scores));        
        set(handles.pop_up_detect_quality,'Value',str_match);
       
        
        %=== Selected cell in main window               
        handles.cell_ind_main = 1;
        
        %=== Image data
        handles.image_struct   = handles_MAIN.image_struct;
        
        handles.img_lin = handles.image_struct.data_filtered(:);
        img_lin_no_0    = handles.img_lin;
        img_lin_no_0(handles.img_lin == 0) = [];
        handles.img_lin_no_0 = img_lin_no_0;
        
        handles.img_min = min(handles.img_lin);
        handles.img_max = max(handles.img_lin);
        handles.img_diff = handles.img_max - handles.img_min;
        
        handles.img_plot = max(handles.image_struct.data_filtered,[],3);
        
        %=== Calc the histogram
        [n,xout] = hist(handles.img_lin_no_0,5000);

        xout(n==0) =[];
        n(n==0) =[];
        handles.xout = xout;
        handles.n = n;                
       
        %=== Plot results
        plot_hist_int(hObject, eventdata, handles)        
        
    end 
end


% Update handles structure
guidata(hObject, handles);

% UIWAIT makes FISH_QUANT_predetect wait for user response (see UIRESUME)
uiwait(handles.h_fishquant_predetect);


%== Outputs from this function are returned to the command line.
function varargout = FISH_QUANT_predetect_OutputFcn(hObject, eventdata, handles) 
varargout{1} = handles.cell_prop;
varargout{2} = handles.detect;
delete(handles.h_fishquant_predetect);


%== Executes when user attempts to close h_fishquant_predetect.
function h_fishquant_predetect_CloseRequestFcn(hObject, eventdata, handles)
%delete(hObject);


function cell_prop = define_cell_prop(handles);

%- Dimension of entire image
w = handles.image_struct.w;
h = handles.image_struct.h;
cell_prop(1).x      = [1 1 w w];
cell_prop(1).y      = [1 h h 1];
cell_prop(1).reg_type = 'Rectangle';
cell_prop(1).reg_pos  = [1 1 w h];

%- Other parameters
cell_prop(1).label = 'EntireImage';
cell_prop(1).pos_TS          = [];
cell_prop(1).spots_fit       = [];
cell_prop(1).thresh          = [];
cell_prop(1).spots_proj      = 0;
cell_prop(1).status_filtered = 0;
cell_prop(1).str_list_TS     = [];
cell_prop(1).TS_counter      = 1;
cell_prop(1).status_image    = 1;
cell_prop(1).status_detect   = 0;
cell_prop(1).status_fit      = 0;
cell_prop(1).status_avg      = 0;
cell_prop(1).status_avg_rad  = 0;
cell_prop(1).status_avg_con  = 0;    
    



%==========================================================================
%== Closing GUI
%==========================================================================

%== With pre-detection on all cells
function button_finished_Callback(hObject, eventdata, handles)

%- Parameters of cells
cell_prop    = handles.cell_prop;
N_cells      = length(cell_prop);
image_struct = handles.image_struct;

%- Parameters for pre-detection    
options             = {};
options.size_detect = handles.detect.region; % handles.size_detect;
options.detect_th   = str2double(get(handles.text_detection_threshold,'String'));
options.pixel_size  = handles.par_microscope.pixel_size;
options.PSF         = handles.PSF_theo; 

%- Quality-score
str = get(handles.pop_up_detect_quality, 'String');
val = get(handles.pop_up_detect_quality,'Value');
flag_struct.score = str{val};
detect_threshold_score = str2double(get(handles.text_detect_th_qual,'String'));

%- Perform pre-detection
flag_struct.output   = 0;
flag_struct.parallel = 0;
flag_struct.region_smaller = get(handles.checkbox_smaller_detection,'Value');
  
%- Save detection parameters
handles.detect.region       = options.size_detect;
handles.detect.thresh_int   = options.detect_th;
handles.detect.thresh_score = detect_threshold_score;
handles.detect.score        = flag_struct.score;
handles.detect.flag_region_smaller = flag_struct.region_smaller;
dim_sub_z = 2*options.size_detect.z+1;

%- Loop over all cells
for ind_cell = 1:N_cells
    
    %- Add cell-prop to options
    options.cell_prop   = cell_prop(ind_cell);    
    
    %- Perform pre-detection   
    [spots_detected_all handles.img_mask] = spots_predetect_v9(image_struct,options,flag_struct);    
       
    %- Calculate quality-score   
    [spots_detected_all sub_spots_all sub_spots_filt_all] = spots_predetect__qual_calc_v2(image_struct,spots_detected_all,options,flag_struct);
    
    if not(isempty(spots_detected_all))
    
        %- Threshold based on quality score
        quality_score = spots_detected_all(:,10);
        ind_th_out    = find ( quality_score < detect_threshold_score);
        N_Spots       = size(spots_detected_all,1);    % Number of candidates 

        ind_all     = (1:N_Spots);
        ind_th_in   = setdiff(ind_all,ind_th_out);

        spots_detected_all(ind_th_in,12)  = 1;    
        spots_detected_all(ind_th_out,12) = 0; 
        spots_detected = spots_detected_all(ind_th_in,:);

        %- Calculate projections for plot with montage function
        spots_proj = [];    
        for k=1:length(ind_th_in)

            %- Get index in original list (before thresholding)
            ind_spot_rel = ind_th_in(k);

            sub_spots{k}      = sub_spots_all{ind_spot_rel};
            sub_spots_filt{k} = sub_spots_filt_all{ind_spot_rel};

            %- MIP in XY
            MIP_xy =  max(sub_spots{k},[],3);
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
         handles.cell_prop(ind_cell).status_detect  = 1;
         handles.cell_prop(ind_cell).spots_detected = spots_detected;
         handles.cell_prop(ind_cell).sub_spots      = sub_spots;
         handles.cell_prop(ind_cell).sub_spots_filt = sub_spots_filt;
         handles.cell_prop(ind_cell).spots_proj     = spots_proj; 
    end
end
    
guidata(hObject, handles); 
uiresume(handles.h_fishquant_predetect)


%== Just closing without pre-detectin
function button_close_GUI_Callback(hObject, eventdata, handles)
uiresume(handles.h_fishquant_predetect)


%==========================================================================
%== Local maximum
%==========================================================================

%== Intensity threshold: change slider value
function slider_hist_int_Callback(hObject, eventdata, handles)

%- Set text value
slider_value = get(handles.slider_hist_int,'Value');
int_value = round(handles.img_min + handles.img_diff*slider_value);
set(handles.text_detection_threshold,'String',num2str(int_value));

%- Plot
plot_hist_int(hObject, eventdata, handles)
guidata(hObject, handles);


%== Intensity threshold: change text box
function text_detection_threshold_Callback(hObject, eventdata, handles)
%- Set slider
int_value = str2double(get(handles.text_detection_threshold,'String'));
slider_value = (int_value-handles.img_min)/handles.img_diff;
set(handles.slider_hist_int,'Value',slider_value);

%- Plot
plot_hist_int(hObject, eventdata, handles)
guidata(hObject, handles);


%== Plot histogram of intensity
function plot_hist_int(hObject, eventdata, handles)

detect_th   = str2double(get(handles.text_detection_threshold,'String'));

%- Plot histogram
axes(handles.axes_hist_int)
plot(handles.xout,handles.n) 
v = axis;
hold on
plot([detect_th, detect_th], [0.1, +20*max(handles.n)],'-r')
hold off
axis(v)
set(gca,'Xscale','log');
set(gca,'Yscale','log'); 
xlabel('Intensity [log]')
ylabel('Counts [log]')
title('Histogram of all pixel intensities [filtered image]')


%== Pre-detection
function button_analyze_maxsupr_Callback(hObject, eventdata, handles)

flag_struct.parallel = 0;
flag_struct.output   = 1;

%- Determine options
cell_prop       = handles.cell_prop(handles.cell_ind_main);

options             = {};
options.cell_prop   = cell_prop;
options.size_detect = handles.detect.region; %handles.size_detect;
options.detect_th   = str2double(get(handles.text_detection_threshold,'String'));
options.pixel_size  = handles.par_microscope.pixel_size;

%- Perform pre-detection
flag_struct.output         = 1;
flag_struct.region_smaller = get(handles.checkbox_smaller_detection,'Value');

[spots_detected handles.img_mask] = spots_predetect_v9(handles.image_struct,options,flag_struct);

%- Update status
status_text = {' ';['== Intensity: ', num2str(options.detect_th),', # of spot-candidates: ',num2str(size(spots_detected,1))]};
status_update(hObject, eventdata, handles,status_text);

%- Plot results
handles.status_pre_detect    = 1;
handles.status_quality_score = 0;
handles.cell_prop(handles.cell_ind_main).spots_detected_all = spots_detected;

handles.flag_spots = 1;
guidata(hObject, handles);
plot_hist_int(hObject, eventdata, handles)

plot_image(handles,handles.axes_img,[])

%- Calculate the corresponding quality scores
pop_up_detect_quality_Callback(hObject, eventdata, handles)


%==========================================================================
%== Quality score
%==========================================================================


%== Calculate the quality score
function pop_up_detect_quality_Callback(hObject, eventdata, handles)
str = get(handles.pop_up_detect_quality, 'String');
val = get(handles.pop_up_detect_quality,'Value');
flag_struct.score = str{val};
flag_struct.parallel = 0;

%- Analyze pre-detection
spots_detected_all = handles.cell_prop(handles.cell_ind_main).spots_detected_all;

options              = {};
options.pixel_size   = handles.par_microscope.pixel_size;
options.size_detect  = handles.detect.region; %handles.size_detect;
options.PSF          = handles.PSF_theo; 
[spots_detected_all] = spots_predetect__qual_calc_v2(handles.image_struct,spots_detected_all,options,flag_struct);

%- Analyze scores of pre-detected spots
if not(isempty(spots_detected_all))
    [handles.qual_count,handles.qual_bin] = hist(spots_detected_all(:,10),30);
    handles.thresh.qual_max               = max(spots_detected_all(:,10));

    handles.status_quality_score = 1;
    handles.cell_prop(handles.cell_ind_main).spots_detected_all = spots_detected_all;

    guidata(hObject, handles);
    plot_qualityscore(hObject, eventdata, handles)

    %- Update status
    status_text = {' '; 'Quality scores calculated'};
    status_update(hObject, eventdata, handles,status_text);
    
else   
    %- Update status
    status_text = {' '; 'Not spots detected'};
    status_update(hObject, eventdata, handles,status_text);
    handles.thresh.qual_max = []; 
    guidata(hObject, handles);
end


%== Apply threshold
function button_qual_apply_Callback(hObject, eventdata, handles)

%- Parameters
spots_detected_all     = handles.cell_prop(handles.cell_ind_main).spots_detected_all;

if not(isempty(spots_detected_all))
    quality_score          = spots_detected_all(:,10);
    N_Spots                = size(spots_detected_all,1);    % Number of candidates 
    detect_threshold_score = str2double(get(handles.text_detect_th_qual,'String'));

    %- Threshold spots
    ind_th_out  = find ( quality_score < detect_threshold_score);
    ind_all     = (1:N_Spots);
    ind_th_in   = setdiff(ind_all,ind_th_out);

    spots_detected_all(ind_th_in,12)  = 1;    
    spots_detected_all(ind_th_out,12) = 0; 
    spots_detected = spots_detected_all(ind_th_in,:);

    %- Save results
    handles.cell_prop(handles.cell_ind_main).spots_detected_all  = spots_detected_all;
    handles.cell_prop(handles.cell_ind_main).spots_detected_     = spots_detected;

    handles.detect_threshold_score = detect_threshold_score;
    handles.flag_spots = 2;

    guidata(hObject, handles); 

    %- Update status and plot
    status_text = {' ';['== Score threshold: ', num2str(detect_threshold_score),', # spots total/in/out: ',num2str(length(ind_all)),'/',num2str(length(ind_th_in)),'/',num2str(length(ind_th_out))]};
    status_update(hObject, eventdata, handles,status_text);
    plot_qualityscore(hObject, eventdata, handles)
    plot_image(handles,handles.axes_img,[]);
else
    status_text = {' ';'== NO SPOTS DETECTED'};
    status_update(hObject, eventdata, handles,status_text);
end

%== Plot histogram of intensity
function plot_qualityscore(hObject, eventdata, handles)

detect_th_score = str2double(get(handles.text_detect_th_qual,'String'));

%- Plot histogram
axes(handles.axes_hist_qual)
bar(handles.qual_bin,handles.qual_count,'FaceColor','b') 
v = axis;
hold on
plot([detect_th_score, detect_th_score], [0.1, +20*max(handles.qual_count)],'-r')
hold off
axis(v)
xlabel('Quality score')
ylabel('Counts')
title('Histogram of quality score of all candidates')


%== Change slider of quality score
function slider_qual_score_Callback(hObject, eventdata, handles)

if not(isempty(handles.thresh.qual_max))
    %- Set text value
    slider_value = get(handles.slider_qual_score,'Value');
    int_value    = round(handles.thresh.qual_max*slider_value);
    set(handles.text_detect_th_qual,'String',num2str(int_value));

    %- Plot
    plot_qualityscore(hObject, eventdata, handles)
    guidata(hObject, handles);
else
    status_text = {' ';'== NO SPOTS DETECTED'};
    status_update(hObject, eventdata, handles,status_text);
end

%== Text value for threshold
function text_detect_th_qual_Callback(hObject, eventdata, handles)

if not(isempty(handles.thresh.qual_max))
    %- Set slider
    int_value = str2double(get(handles.text_detect_th_qual,'String'));
    slider_value = int_value/handles.thresh.qual_max;
    set(handles.slider_qual_score,'Value',slider_value);

    %- Plot
    plot_qualityscore(hObject, eventdata, handles)
    guidata(hObject, handles);
else
    status_text = {' ';'== NO SPOTS DETECTED'};
    status_update(hObject, eventdata, handles,status_text);
end



%==========================================================================
%== Mixed functions
%==========================================================================


%== Update status
function status_update(hObject, eventdata, handles,status_text)
status_old = get(handles.list_box_status,'String');
status_new = [status_old;status_text];
set(handles.list_box_status,'String',status_new)
set(handles.list_box_status,'ListboxTop',round(size(status_new,1)))
drawnow
guidata(hObject, handles); 


%== Plot detected spots
function plot_image(handles,axes_select,flag_spots)

%- Might be called with no cell properties defined
ind_cell       = handles.cell_ind_main;
cell_prop      = handles.cell_prop;
spots_detected = cell_prop(ind_cell).spots_detected_all;

%- Image-data to plot
img_plot = handles.img_plot;          

%- 1. Plot image
if isempty(axes_select)
    figure
    imshow(img_plot,[]);
else
    axes(axes_select);
    h = imshow(img_plot,[]);
    set(h, 'ButtonDownFcn', @axes_img_ButtonDownFcn)
end

title('Maximum projection of loaded image','FontSize',9);
colormap(hot)

%- 2. Plot-spots

if not(isempty(spots_detected))

    if isempty(flag_spots)
        flag_spots =  handles.flag_spots;
    end
    
    %- Select spots which will be shown
    if     flag_spots == 1  % all spots after detection
        ind_plot_in  = true(size(spots_detected,1),1);        
 
    elseif flag_spots == 2 % spots after thresholding with quality score
        ind_plot_in  = spots_detected(:,12)  == 1;

    end
    ind_plot_out = not(ind_plot_in);
    
    %- Plot spots        
    hold on
        plot(spots_detected(ind_plot_in,2), spots_detected(ind_plot_in,1),'og','MarkerSize',10);
        plot(spots_detected(ind_plot_out,2), spots_detected(ind_plot_out,1),'or','MarkerSize',10); 
    hold off
    title(['Detected spots', num2str(length(ind_plot_in))],'FontSize',9); 
    colormap(hot)
    freezeColors(gca)
    
    if sum(ind_plot_out)  
        legend('Selected Spots','Rejected Spots [auto]');      
    else
        legend('Selected Spots');
    end 
end


%- 3. Plot outline if specified

    
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
    

%== Double click opens in new window
function axes_img_ButtonDownFcn(hObject, eventdata, handles)
sel_type = get(gcf,'selectiontype');    % Normal for single click, Open for double click
   
if strcmp(sel_type,'open')
    handles = guidata(hObject);        % Appears that handles are not always input parameter for function call
    plot_image(handles,[],[]);
end


%= Change size of detection region
function text_detect_region_xy_Callback(hObject, eventdata, handles)
size_xy = str2double(get(handles.text_detect_region_xy,'String')); 
handles.detect.region.xy = size_xy; % handles.size_detect.xy = size_xy;
guidata(hObject, handles);


%= Change size of detection region
function text_detect_region_z_Callback(hObject, eventdata, handles)
size_z = str2double(get(handles.text_detect_region_z,'String'));
handles.detect.region.z = size_z; %handles.size_detect.z = size_z; 
guidata(hObject, handles);



%==========================================================================
%== Not used functions
%==========================================================================

function checkbox_proc_all_cells_Callback(hObject, eventdata, handles)

function checkbox_parallel_computing_Callback(hObject, eventdata, handles)

function text_detect_region_xy_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_detect_region_z_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function pop_up_detect_quality_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_detection_threshold_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function slider_hist_int_CreateFcn(hObject, eventdata, handles)
if isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor',[.9 .9 .9]);
end

function list_box_status_Callback(hObject, eventdata, handles)

function list_box_status_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function text_detect_th_qual_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end

function slider_qual_score_CreateFcn(hObject, eventdata, handles)
if isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor',[.9 .9 .9]);
end

function pushbutton5_Callback(hObject, eventdata, handles)

function h_fishquant_predetect_ButtonDownFcn(hObject, eventdata, handles)

function checkbox_smaller_detection_Callback(hObject, eventdata, handles)
