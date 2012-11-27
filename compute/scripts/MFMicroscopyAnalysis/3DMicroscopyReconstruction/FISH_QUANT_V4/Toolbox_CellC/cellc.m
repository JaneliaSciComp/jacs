function varargout = cellc(varargin)
% CELLC M-file for cellc.fig
%      CELLC, by itself, creates a new CELLC or raises the existing
%      singleton*.
%
%      H = CELLC returns the handle to a new CELLC or the handle to
%      the existing singleton*.
%
%      CELLC('CALLBACK',hObject,eventData,handles,...) calls the local
%      function named CALLBACK in CELLC.M with the given input arguments.
%
%      CELLC('Property','Value',...) creates a new CELLC or raises the
%      existing singleton*.  Starting from the left, property value pairs are
%      applied to the GUI before cellc_OpeningFunction gets called.  An
%      unrecognized property name or invalid value makes property application
%      stop.  All inputs are passed to cellc_OpeningFcn via varargin.
%
%      *See GUI Options on GUIDE's Tools menu.  Choose "GUI allows only one
%      instance to run (singleton)".
%
% See also: GUIDE, GUIDATA, GUIHANDLES

% Copyright 2002-2003 The MathWorks, Inc.

% Edit the above text to modify the response to help cellc

% Last Modified by GUIDE v2.5 19-Jan-2006 09:01:21

% Begin initialization code - DO NOT EDIT
gui_Singleton = 1;
gui_State = struct('gui_Name',       mfilename, ...
                   'gui_Singleton',  gui_Singleton, ...
                   'gui_OpeningFcn', @cellc_OpeningFcn, ...
                   'gui_OutputFcn',  @cellc_OutputFcn, ...
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




%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%% NOTE! The processing starts at the beginning of the function
% function pushbutton_startsingle_Callback(hObject, eventdata, handles)
%
% From there it is easiest to get an idea of the basic processing steps
% and begin modifications.
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%





% --- Executes just before cellc is made visible.
function cellc_OpeningFcn(hObject, eventdata, handles, varargin)
% This function has no output args, see OutputFcn.
% hObject    handle to figure
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)
% varargin   command line arguments to cellc (see VARARGIN)

% Choose default command line output for cellc
handles.output = hObject;

% Update handles structure
guidata(hObject, handles);

% UIWAIT makes cellc wait for user response (see UIRESUME)
% uiwait(handles.figure1);

% initialize global variables needed

% output file type, either native Excel(r) XLS (requires ActiveX), or comma
% separated values file (also readable by Excel(r))
% Currently only CSV is supported.
global writemode;
%writemode='xls';
writemode='csv';

% filenames of opened images
global totalFilename;
totalFilename='';
global specificFilename;
specificFilename='';

global procspecific;
procspecific=0;

% image for total object count
global total;
total=0;
% image of specific
global specific;
specific=0;
% B/W thr of total, 2 if set to 'auto'
global total_thr;
total_thr=2;
% B/W thr of specific, 2 if set to 'auto'
global spec_thr;
spec_thr=2;
% cutting thr of objects, see total_cutter.m
global cut_thr;
cut_thr=0.1;
% the number shown as cutlimit in GUI is actually 1-cut_thr
set(handles.text_cutlimit,'String',num2str(1-cut_thr));
set(handles.slider_cutthreshold,'Value',cut_thr);

% whether to cut or not
global do_cut;
do_cut=1;

% minimum cutting size, 0 if 'automatic'
global minsize;
minsize=0;

% background subtraction (on by default)
global back_sub;
back_sub=1;

% batch indicator, if == 1, program is in batch processing mode (no output
% images plotted etc)
global batchmode;
batchmode=0;
% folder where results are saved in batch mode
global batchpath;

% labeled result total count image
global labeled_total;
% labeled result specific image
global labeled_specific;

% result data, this matrix is saved to the result .csv file
global result;
result=0;

% ratio between micrometers and pixels (string '----' if no ratio given)
global meterratio;
meterratio=1.0;

% remove objects larger than
global toolarge;
toolarge=9999;

% remove objects smaller than
global toosmall;
toosmall=0;

% factor of resizing
global resize_factor
resize_factor=1;

global do_resize;
do_resize=1;

% The folder where last image was opened. This allows fast opening of next
% image because images often are in the same directory.
global loaddirectory;
loaddirectory=0;

% To show pixel indexes when displaying the images
iptsetpref('ImshowAxesVisible','on');

% --- Outputs from this function are returned to the command line.
function varargout = cellc_OutputFcn(hObject, eventdata, handles) 
% varargout  cell array for returning output args (see VARARGOUT);
% hObject    handle to figure
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Get default command line output from handles structure
varargout{1} = handles.output;


% --- Executes on button press in checkbox_background.
function checkbox_background_Callback(hObject, eventdata, handles)
% hObject    handle to checkbox_background (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hint: get(hObject,'Value') returns toggle state of checkbox_background

global back_sub;

if ~get(hObject,'Value')
    back_sub=0;
else
    back_sub=1;  
end


% --- Executes on button press in checkbox2.
function checkbox_threshold_Callback(hObject, eventdata, handles)
% hObject    handle to checkbox2 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hint: get(hObject,'Value') returns toggle state of checkbox2
% Hint: get(hObject,'Value') returns toggle state of total_thr
global total;
global total_thr;

% check if image has been opened
if isequal(size(total),[1 1])
    temp=get(hObject,'Value');
    set(hObject,'Value',1-temp);
    errordlg('Open image for total object count (File/Open)','File Error');
end

% if checkbox is turned off
if ~get(hObject,'Value')
    set(hObject,'Enable','inactive');
    h=waitbar(0.65,'Opening threshold adjuster...');
    % open manual threshold adjuster
    total_thr=adjustthr(total,'total',h);
    set(hObject,'Enable','on');
    if total_thr==2
        % cancel button pressed in adjuster
        set(hObject,'Value',1);
    end
else
    % else checkbox is turned on, set threshold to 'auto'
    total_thr=2;
end

% --- Executes on button press in checkbox_threshspec.
function checkbox_threshspec_Callback(hObject, eventdata, handles)
% hObject    handle to checkbox_threshspec (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hint: get(hObject,'Value') returns toggle state of checkbox2
% Hint: get(hObject,'Value') returns toggle state of total_thr
global specific;
global spec_thr;

% check if image has been opened
if isequal(size(specific),[1 1])
    temp=get(hObject,'Value');
    set(hObject,'Value',1-temp);
    errordlg('Open image for specific object count first (File/Open)','File Error');
end

% if checkbox is turned off
if ~get(hObject,'Value')
    set(hObject,'Enable','inactive');
    h=waitbar(0.65,'Opening threshold adjuster...');
    % open manual threshold adjuster
    spec_thr=adjustthr(specific,'total',h);
    set(hObject,'Enable','on');
    if spec_thr==2
        % cancel button pressed in adjuster
        set(hObject,'Value',1);
    end
else
    % else checkbox is turned on, set threshold to 'auto'
    spec_thr=2;
end


% --- Executes on button press in checkbox3.
function checkbox_separation_Callback(hObject, eventdata, handles)
% hObject    handle to checkbox3 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hint: get(hObject,'Value') returns toggle state of checkbox3

global do_cut;
if ~get(hObject,'Value')
    do_cut=0;
    set(handles.slider_cutthreshold,'Enable','Off');
    set(handles.text_cutlimit,'Visible','Off');
else
    do_cut=1;
    set(handles.slider_cutthreshold,'Enable','On');
    set(handles.text_cutlimit,'Visible','On');
end


% --- Executes on button press in checkbox_sizelimit.
function checkbox_sizelimit_Callback(hObject, eventdata, handles)
% hObject    handle to checkbox_sizelimit (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hint: get(hObject,'Value') returns toggle state of checkbox_sizelimit
global toolarge;
global toosmall;
if ~get(hObject,'Value')
    set(handles.edit_toosmall,'Enable','On');
    set(handles.edit_toolarge,'Enable','On');
    set(handles.edit_toosmall,'String',num2str(toosmall));
    set(handles.edit_toolarge,'String',num2str(toolarge));
else
    set(handles.edit_toosmall,'String','Auto');
    set(handles.edit_toosmall,'Enable','Off');
    set(handles.edit_toolarge,'String','Auto');
    set(handles.edit_toolarge,'Enable','Off');
end


function edit_toosmall_Callback(hObject, eventdata, handles)
% hObject    handle to edit_toosmall (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of edit_toosmall as text
%        str2double(get(hObject,'String')) returns contents of edit_toosmall as a double
global toosmall;

% change comma to period
tempratio=get(hObject,'String');
dots = strfind(tempratio, ',');
if ~isequal(dots,[])
    tempratio(dots(1))='.';
end
   
toosmall=str2num(tempratio);
set(hObject,'String',tempratio);

% --- Executes during object creation, after setting all properties.
function edit_toosmall_CreateFcn(hObject, eventdata, handles)
% hObject    handle to edit_toosmall (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc
    set(hObject,'BackgroundColor','white');
else
    set(hObject,'BackgroundColor',get(0,'defaultUicontrolBackgroundColor'));
end



function edit_toolarge_Callback(hObject, eventdata, handles)
% hObject    handle to edit_toolarge (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of edit_toolarge as text
%        str2double(get(hObject,'String')) returns contents of edit_toolarge as a double
global toolarge;

% change comma to period
tempratio=get(hObject,'String');
dots = strfind(tempratio, ',');
if ~isequal(dots,[])
    tempratio(dots(1))='.';
end
   
toolarge=str2num(tempratio);
set(hObject,'String',tempratio);


% --- Executes during object creation, after setting all properties.
function edit_toolarge_CreateFcn(hObject, eventdata, handles)
% hObject    handle to edit_toolarge (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc
    set(hObject,'BackgroundColor','white');
else
    set(hObject,'BackgroundColor',get(0,'defaultUicontrolBackgroundColor'));
end


% --- Executes on button press in checkbox5.
function checkbox_micrometers_Callback(hObject, eventdata, handles)
% hObject    handle to checkbox5 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hint: get(hObject,'Value') returns toggle state of checkbox5
global meterratio;
if ~get(hObject,'Value')
    set(handles.edit_millimeters,'String','----');
    set(handles.edit_millimeters,'Enable','Off');
    set(handles.text3,'String','pixels');
    set(handles.text4,'String','pixels');
else
    set(handles.edit_millimeters,'Enable','On');
    set(handles.edit_millimeters,'String',num2str(meterratio));
    set(handles.text3,'String','?m^2');
    set(handles.text4,'String','?m^2');
end


function edit_millimeters_Callback(hObject, eventdata, handles)
% hObject    handle to edit_millimeters (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of edit_millimeters as text
%        str2double(get(hObject,'String')) returns contents of edit_millimeters as a double

global meterratio;

% change comma to period
tempratio=get(hObject,'String');
dots = strfind(tempratio, ',');
if ~isequal(dots,[])
    tempratio(dots(1))='.';
end
   
meterratio=str2num(tempratio);
set(hObject,'String',tempratio);

% --- Executes during object creation, after setting all properties.
function edit_millimeters_CreateFcn(hObject, eventdata, handles)
% hObject    handle to edit_millimeters (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc
    set(hObject,'BackgroundColor','white');
else
    set(hObject,'BackgroundColor',get(0,'defaultUicontrolBackgroundColor'));
end


% --- Executes on slider movement.
function slider_cutthreshold_Callback(hObject, eventdata, handles)
% hObject    handle to slider1 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'Value') returns position of slider
%        get(hObject,'Min') and get(hObject,'Max') to determine range of slider

global cut_thr;

cut_thr=get(hObject,'Value');
% set new value to cutting threshold window
set(handles.text_cutlimit,'String',num2str(1-cut_thr));


% --- Executes during object creation, after setting all properties.
function slider_cutthreshold_CreateFcn(hObject, eventdata, handles)
% hObject    handle to slider1 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: slider controls usually have a light gray background, change
%       'usewhitebg' to 0 to use default.  See ISPC and COMPUTER.
usewhitebg = 1;
if usewhitebg
    set(hObject,'BackgroundColor',[.9 .9 .9]);
else
    set(hObject,'BackgroundColor',get(0,'defaultUicontrolBackgroundColor'));
end


% --- Executes on button press in pushbutton_startsingle.
function pushbutton_startsingle_Callback(hObject, eventdata, handles)
% hObject    handle to pushbutton_startsingle (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

global totalFilename;
global specificFilename;
global total;
global specific;
global total_thr;
global spec_thr;
global cut_thr;
global do_cut;
global minsize;
global back_sub;
global batchmode;
global batchpath;
global labeled_total;
global labeled_specific;
global result;
global meterratio;
global toolarge;
global toosmall;
global resize_factor;
global do_resize;
global writemode;

global procspecific;

% savedirectory does not exist in the beginning of the analysis
global savedirectory;
savedirectory=0;

if isequal(size(total),[1 1])
    errordlg('Open image for total object count (File/Open)','File Error');
    return;
end

% procspecific is already set if in batch mode, otherwise check whether
% both images are opened
if ~batchmode
    procspecific=~isequal(size(specific),[1 1]);
end

if ~procspecific
    str=['Analyzing cells of Figure 1: ' totalFilename];
else
    str=['Analyzing cells that exist in both Figures 1 and 2'];
end
h=waitbar(0.2,str);

% resize
[small_total,resize_factor]=total_resize(total,do_resize);
if procspecific
    [small_spec,resize_factor]=total_resize(specific,do_resize);
end

% binarize
bw=total_prepare(small_total,total_thr,'otsu',back_sub);
if procspecific
    bwspec=total_prepare(small_spec,spec_thr,'otsu',back_sub);
end

waitbar(0.3,h);

% invert if not fluorescent
if get(handles.popupmenu_imagetype,'Value')==2
    bw=~bw;
    if procspecific
        bwspec=~bwspec;
    end
end


% preprocess the binarized image
clean=total_clean(bw,get(handles.checkbox_fillholes,'Value'));
if procspecific
    cleanspec=total_clean(bwspec,get(handles.checkbox_fillholes,'Value'));
end

waitbar(0.4,h);

% cutting method
if get(handles.popupmenu_divide,'Value')==1
    meth='inte';
else
    meth='shape';
end

% divide clustered objects
cut_total=total_cutter(clean,small_total,cut_thr,meth, do_cut);
if procspecific
    cut_spec=total_cutter(cleanspec,small_spec,cut_thr,meth, do_cut);
end

waitbar(0.5,h);

% remove aftefacts
cut_total=total_clean_aft(cut_total,get(handles.checkbox_micrometers,'Value'),...
    get(handles.checkbox_sizelimit,'Value'),meterratio,toosmall,toolarge,resize_factor);
if procspecific
    cut_spec=total_clean_aft(cut_spec,get(handles.checkbox_micrometers,'Value'),...
        get(handles.checkbox_sizelimit,'Value'),meterratio,toosmall,toolarge,resize_factor);
end


% Result figure for total object count. (The number of cells is calculated
% here and not in the calcprops function to make it possible to easily
% disable the save feature, controlled by the "Save options" option)

[labeled_total,number_of_objects] = bwlabel(cut_total,4);

BWoutline = bwperim(labeled_total);

Segout = small_total;
Segout(BWoutline) = 10*max(small_total(:));

figure
totalfig=imshow(Segout); 

%figure;
%totalfig=imshow(labeled_total>0,[]);

% This code can be used in presenting the cells "serial number" in the
% displayed image
s = regionprops(labeled_total, 'Centroid');
hold on
for k = 1:numel(s)
    c = s(k).Centroid;
    text(c(1), c(2), sprintf('%d', k), ...
        'HorizontalAlignment', 'center', ...
        'VerticalAlignment', 'middle','Color','w');
end
hold off

nos=sprintf('%g',number_of_objects);
heading=['Total object count. Number of cells: ' nos...
        '   ---   File: ' totalFilename];
title(heading)

% result figure for specific object count 
if procspecific
    selected=spec_select(cut_total,cut_spec);

    [labeled_sel,number_of_sel] = bwlabel(selected,4);

    figure;
    specfig=imshow(labeled_sel>0,[]);
    nos=sprintf('%g',number_of_sel);
    heading=['Specifically stained objects. Number of cells: ' nos...
        '   ---   File: ' specificFilename];
    title(heading)
end

% if in batch mode, save the figure automatically and close it
if batchmode
    [pathstr,name,ext] = fileparts(totalFilename);
    saveas(totalfig,[batchpath filesep name '_totcount_result.jpg']);
    close(1);
end
if batchmode&procspecific
    [pathstr,name,ext] = fileparts(specificFilename);
    saveas(specfig,[batchpath filesep name '_specific_result.jpg']);
    close(2);
end

%%%%% From here on, the "Save options" feature is checked whether saving is
%%%%% needed or not.

save_images=isequal(get(handles.save_images,'Checked'),'on');
save_sheets=isequal(get(handles.save_sheets,'Checked'),'on');

if save_images
    % if not in batch mode, save the figure manually
    if ~batchmode
        [pathstr,name,ext] = fileparts(totalFilename);
        [file,savedirectory,filterindex] = uiputfile([name '_totcount_result.jpg'],'Save Image of Total Cell Count As');
        % save only if cancel is not pressed
        if ~isequal(file,0)
            saveas(totalfig,fullfile(savedirectory, file));
        end
    end
    if (~batchmode)&procspecific
        [pathstr,name,ext] = fileparts(specificFilename);
        % if "directory" exists, total image must have been saved, and the same
        % folder is used for saving the specific image
        if ~isequal(savedirectory,0)
            [file,savedirectory,filterindex] = uiputfile([savedirectory name '_specific_result.jpg'],'Save Image of Specific Cell Count As');
        else
            [file,savedirectory,filterindex] = uiputfile([name '_specific_result.jpg'],'Save Image of Specific Cell Count As');
        end
        if ~isequal(file,0)
            saveas(specfig,fullfile(savedirectory, file));
        end
    end
end

% TEMPORARY SOLUTION to enable "UNIT OF MEASURE" info to result files
global trash;
trash=get(handles.edit_millimeters,'String');

% now, "savedirectory" contains the folder in which images have been saved
% if batch processing was not active. If nothing was saved
% savedirectory==0

waitbar(0.7,h);
figure(h);
drawnow;
waitbar(0.8,h);
drawnow;
drawnow;

if save_sheets
    % calculation of results
    if procspecific
        result=calcprops(labeled_total,small_total,resize_factor,...
            get(handles.checkbox_micrometers,'Value'),meterratio,...
            labeled_sel);
    else
        result=calcprops(labeled_total,small_total,resize_factor,...
            get(handles.checkbox_micrometers,'Value'),meterratio);
    end
    
    if ~batchmode
        % save results
        saveresults(result, totalFilename);
    elseif procspecific
        % if in batch mode, save path must be sent to save-function
        % The specific count is also taken into account
        saveresults(result, totalFilename, batchpath, 1, 1);
    else
        % if in batch mode, save path must be sent to save-function
        % Only total cell count
        saveresults(result, totalFilename, batchpath, 1, 0);
    end
end

% reset the state of "process the specific count image" only if not in 
% batch mode (if in batch, the batch function will handle the setting of 
% procspedific)
if ~batchmode
    procspecific=0;
end

close(h);

% --- Executes on selection change in popupmenu_imagetype.
function popupmenu_imagetype_Callback(hObject, eventdata, handles)
% hObject    handle to popupmenu_imagetype (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: contents = get(hObject,'String') returns popupmenu_imagetype contents as cell array
%        contents{get(hObject,'Value')} returns selected item from popupmenu_imagetype


% --- Executes during object creation, after setting all properties.
function popupmenu_imagetype_CreateFcn(hObject, eventdata, handles)
% hObject    handle to popupmenu_imagetype (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: popupmenu controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc
    set(hObject,'BackgroundColor','white');
else
    set(hObject,'BackgroundColor',get(0,'defaultUicontrolBackgroundColor'));
end


% --------------------------------------------------------------------
function Untitled_2_Callback(hObject, eventdata, handles)
% hObject    handle to Untitled_2 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)


% --------------------------------------------------------------------
function Untitled_1_Callback(hObject, eventdata, handles)
% hObject    handle to Untitled_1 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)


% --------------------------------------------------------------------
function File_Open_Callback(hObject, eventdata, handles)
% hObject    handle to File_Open (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)



% --------------------------------------------------------------------
function file_open_specific_Callback(hObject, eventdata, handles)
% hObject    handle to file_open_specific (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

global loaddirectory;

if ~isequal(loaddirectory,0)

    [FileName, PathName] = uigetfile(...
        {'*.jpg;*.tiff;*.tif;*.gif;*.png;*.bmp',...
        'Image Files (*.jpg,*.tiff,*.tif,*.gif,*.png,*.bmp)';...
        '*.*',  'All Files (*.*)'},'Open image for specific cell count',loaddirectory);
else
        [FileName, PathName] = uigetfile(...
        {'*.jpg;*.tiff;*.tif;*.gif;*.png;*.bmp',...
        'Image Files (*.jpg,*.tiff,*.tif,*.gif,*.png,*.bmp)';...
        '*.*',  'All Files (*.*)'},'Open image for specific cell count');
end

if (~isequal(FileName,0))
    % Read and show image
    h=waitbar(0.30,'Opening image...');
    X=imread([PathName FileName]);
    axes(handles.image_specific)
    imshow(X);
    zoom on;
    close(h);
    
    global specificFilename;
    specificFilename=FileName;
    global specific;
    specific=X;
end


% --------------------------------------------------------------------
function File_Open_total_Callback(hObject, eventdata, handles)
% hObject    handle to File_Open_total (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

global loaddirectory;

if ~isequal(loaddirectory,0)
    [FileName, PathName] = uigetfile(...
        {'*.jpg;*.tiff;*.tif;*.gif;*.png;*.bmp',...
        'Image Files (*.jpg,*.tiff,*.tif,*.gif,*.png,*.bmp)';...
        '*.*',  'All Files (*.*)'},'Open image for total cell count',loaddirectory);
else
    [FileName, PathName] = uigetfile(...
        {'*.jpg;*.tiff;*.tif;*.gif;*.png;*.bmp',...
        'Image Files (*.jpg,*.tiff,*.tif,*.gif,*.png,*.bmp)';...
        '*.*',  'All Files (*.*)'},'Open image for total cell count');
end

if (~isequal(FileName,0))
    % Read and show image
    h=waitbar(0.30,'Opening Figure...');
    X=imread([PathName FileName]);
    axes(handles.image_total)
    imshow(X);
    zoom on;
    close(h);
    
    global totalFilename;
    totalFilename=FileName;
    global total;
    total=X;
    % remember the folder where image was loaded from
    loaddirectory=PathName;
end




% --------------------------------------------------------------------
function file_exit_Callback(hObject, eventdata, handles)
% hObject    handle to file_exit (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% image pixel indexes off from figures
iptsetpref('ImshowAxesVisible','off');

% exit
delete(handles.figure1)


% --------------------------------------------------------------------
function file_help_Callback(hObject, eventdata, handles)
% hObject    handle to file_help (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)


% --------------------------------------------------------------------
function Untitled_3_Callback(hObject, eventdata, handles)
% hObject    handle to Untitled_3 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)


% --------------------------------------------------------------------
function Batch_starttotal_Callback(hObject, eventdata, handles)
% hObject    handle to Batch_starttotal (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)


global batchmode;
global totalFilename;
global total;
global specificFilename;
global specific;
global procspecific;

global batchpath;

% enter batch processing mode
batchmode=1;
% save state of the program
temptotalFilename=totalFilename;
temptotal=total;
if procspecific
    tempspecificFilename=specificFilename;
    tempspecific=specific;
end

% select required folder
totaldir = uigetdir('','Select folder containing images for total count');
if procspecific
    specificdir = uigetdir('','Select folder containing specifically stained images');
end
batchpath = uigetdir('','Select folder where to save result files');

% check whether all folders were selected
   
if procspecific&(isequal(totaldir,0)|isequal(batchpath,0)|isequal(specificdir,0))
    errordlg('Select all three folders','folder error');
elseif (~procspecific)&(isequal(totaldir,0)|isequal(batchpath,0))
    errordlg('Select both folders','folder error');
else
    % The Windows operating system caches small "thumbnails" of the images
    % in folders if "thumbnail" view is selected from the "view" menu in 
    % Windows Explorer. This file must be removed before the analysis.
    if (ispc)
        if (exist([totaldir '\Thumbs.db']))
            delete ([totaldir '\Thumbs.db']);
        end
        if procspecific
            if (exist([specificdir '\Thumbs.db']))
                delete ([specificdir '\Thumbs.db']);
            end
        end
    end
    
    totalfiles=dir(totaldir);
    % folder listing for specifically stained objects to test
    % whether it includes same files as totaldir
    if procspecific
        specificfiles=dir(specificdir);
        specificfiles=specificfiles(3:end);
    end
    % remove folders . and ..
    totalfiles=totalfiles(3:end);
    
    % check that totaldir and specificdir really contain exactly the same
    % filenames
    if procspecific
        if ~isequal([totalfiles.name],[specificfiles.name])
            errordlg('Files in both folders must have the same names.'...
                ,'File Error');
            return;
        end
    end
    
    % process each image in folder
    for iter=1:size(totalfiles,1)
        totalFilename=totalfiles(iter).name;
        if procspecific
            specificFilename=totalfiles(iter).name;
        end
        
        % Open the right image files to variables
        % in PC computers, separator of folders is '\'
        try
            if ispc==1
                total=imread([totaldir '\' totalFilename]);
                if procspecific
                    specific=imread([specificdir '\' totalFilename]);
                end
            else
                % in UNIX, separator is '/'
                total=imread([totaldir '/' totalFilename]);
                if procspecific
                    specific=imread([specificdir '/' totalFilename]);
                end
            end
        catch
            errordlg('The selected folders must contain only images','File error');
            return;
        end

        pushbutton_startsingle_Callback(hObject, eventdata, handles);
    end

    message=['Batch processing complete, see folder ' batchpath ' for results'];
    msgbox(message,'Task complete','help'); 
end

% exit batch processing mode
batchmode=0;

% restore state of the program
totalFilename=temptotalFilename;
total=temptotal;
if procspecific
    specificFilename=tempspecificFilename;
    specific=tempspecific;
end
batchmode=0;
procspecific=0;


% --------------------------------------------------------------------
function Batch_startspec_Callback(hObject, eventdata, handles)
% hObject    handle to Batch_startspec (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

global procspecific;
procspecific=1;
Batch_starttotal_Callback(hObject, eventdata, handles);

% --------------------------------------------------------------------
function Help_quick_Callback(hObject, eventdata, handles)
% hObject    handle to Help_quick (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

questiontext=['A web browser is needed to view the CellC help.                  '
              'Intenet connection is not required.                              '
              '                                                                 '
              'Do you want to start a browser to view the help?                 '
              '(if the browser cannot be started automatically,                 '
              'open the help file "faq.html" in CellC installation folder.)     '];

if (isequal(questdlg(questiontext,'CellC Help','OK','Cancel','OK'),'OK'))
    dire=pwd;
    status=web(['file://' dire filesep 'faq.html'], '-browser');
    if status>0
        errorstring=['Web browser cannot be started.                                         '
                     'Please read the help manually from http://www.cs.tut.fi/sgn/csb/cellc/)'];
        errordlg('errorstring');
    end
end

% --------------------------------------------------------------------
function Help_about_Callback(hObject, eventdata, handles)
% hObject    handle to Help_about (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% display help/about window
abouttext=['CELLC, properties of cell images. '
           'Copyright (c) 2005 Jyrki Selinummi'
           '& Jenni Sepp?l?.                  '
           'MATLAB(r).(C)1984-2005            '
           'The MathWorks, Inc.               '];

msgbox(abouttext,'About CELLC','help')




% --- Executes on selection change in popupmenu_divide.
function popupmenu_divide_Callback(hObject, eventdata, handles)
% hObject    handle to popupmenu_divide (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: contents = get(hObject,'String') returns popupmenu_divide contents as cell array
%        contents{get(hObject,'Value')} returns selected item from popupmenu_divide


% --- Executes during object creation, after setting all properties.
function popupmenu_divide_CreateFcn(hObject, eventdata, handles)
% hObject    handle to popupmenu_divide (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: popupmenu controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end




% --- Executes on button press in checkbox_fillholes.
function checkbox_fillholes_Callback(hObject, eventdata, handles)
% hObject    handle to checkbox_fillholes (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hint: get(hObject,'Value') returns toggle state of checkbox_fillholes




% --------------------------------------------------------------------
function save_images_Callback(hObject, eventdata, handles)
% hObject    handle to save_images (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

notem=['Saving of result images is set OFF.               '
       '(In batch processing the images are always saved.)'];
notet=['Save options'];

if isequal(get(handles.save_images,'Checked'),'on')
    set(handles.save_images,'Checked','off')
    msgbox(notem,notet,'help');
else
    set(handles.save_images,'Checked','on')
end

% --------------------------------------------------------------------
function save_sheets_Callback(hObject, eventdata, handles)
% hObject    handle to save_sheets (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

notem=['Saving of result data files is set OFF'];
notet=['Save options'];

if isequal(get(handles.save_sheets,'Checked'),'on')
    set(handles.save_sheets,'Checked','off')
    msgbox(notem,notet,'help');
else
    set(handles.save_sheets,'Checked','on')
end

% --------------------------------------------------------------------
function Untitled_4_Callback(hObject, eventdata, handles)
% hObject    handle to Untitled_4 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)


