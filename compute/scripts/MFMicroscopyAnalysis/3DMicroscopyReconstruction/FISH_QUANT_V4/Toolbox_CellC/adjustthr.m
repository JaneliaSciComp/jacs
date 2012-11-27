function varargout = adjustthr(varargin)
% ADJUSTTHR M-file for adjustthr.fig
%      ADJUSTTHR, by itself, creates a new ADJUSTTHR or raises the existing
%      singleton*.
%
%      H = ADJUSTTHR returns the handle to a new ADJUSTTHR or the handle to
%      the existing singleton*.
%
%      ADJUSTTHR('CALLBACK',hObject,eventData,handles,...) calls the local
%      function named CALLBACK in ADJUSTTHR.M with the given input arguments.
%
%      ADJUSTTHR('Property','Value',...) creates a new ADJUSTTHR or raises the
%      existing singleton*.  Starting from the left, property value pairs are
%      applied to the GUI before adjustthr_OpeningFunction gets called.  An
%      unrecognized property name or invalid value makes property application
%      stop.  All inputs are passed to adjustthr_OpeningFcn via varargin.
%
%      *See GUI Options on GUIDE's Tools menu.  Choose "GUI allows only one
%      instance to run (singleton)".
%
% See also: GUIDE, GUIDATA, GUIHANDLES
% Copyright (c) 2004 by Jyrki Selinummi

% Edit the above text to modify the response to help adjustthr


% Last Modified by GUIDE v2.5 17-Oct-2003 12:12:12

% Begin initialization code - DO NOT EDIT
gui_Singleton = 1;
gui_State = struct('gui_Name',       mfilename, ...
                   'gui_Singleton',  gui_Singleton, ...
                   'gui_OpeningFcn', @adjustthr_OpeningFcn, ...
                   'gui_OutputFcn',  @adjustthr_OutputFcn, ...
                   'gui_LayoutFcn',  [] , ...
                   'gui_Callback',   []);
if nargin & isstr(varargin{1})
    gui_State.gui_Callback = str2func(varargin{1});
end

if nargout
    [varargout{1:nargout}] = gui_mainfcn(gui_State, varargin{:});
else
    gui_mainfcn(gui_State, varargin{:});
end
% End initialization code - DO NOT EDIT


% --- Executes just before adjustthr is made visible.
function adjustthr_OpeningFcn(hObject, eventdata, handles, varargin)
% This function has no output args, see OutputFcn.
% hObject    handle to figure
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)
% varargin   command line arguments to adjustthr (see VARARGIN)

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Purpose of adjustthr_OpeningFcn function:
% Initialization function

% original image
global orig;
% variable defining whether background subtraction is activated
global back_sub;

% When adjusting threshold manually, image is always resized. This does not
% afffect image resizing in Cellc main program, we resize here just to
% make the size of the image OK for visual analysis purposes.
resize_factor=sqrt(470000/(size(varargin{1},1)*size(varargin{1},2)));
orig=imresize(varargin{1},resize_factor);

% Preprocessing of the input image
if strcmp(varargin{2},'total')
    [bwimage,thr_value,orig]=total_prepare(orig,2,'otsu',back_sub);
else
    % this line is never executed in CELLC
    [bwimage,thr_value,orig]=cyto_prepare(orig,2,'otsu',back_sub);
end

colormap gray;
imagesc(bwimage);

% set slider and threshold window to right initial values
set(handles.slider1,'Value',thr_value);
set(handles.edit1,'String',num2str(thr_value));

% Choose default command line output for adjustthr
handles.output = thr_value;


% Update handles structure
guidata(hObject, handles);

% closes waitbar
close(varargin{3})

% UIWAIT makes adjustthr wait for user response (see UIRESUME)
uiwait(handles.figure1);


% --- Outputs from this function are returned to the command line.
function varargout = adjustthr_OutputFcn(hObject, eventdata, handles)
% varargout  cell array for returning output args (see VARARGOUT);
% hObject    handle to figure
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Get default command line output from handles structure
varargout{1} = handles.output;
delete(handles.figure1);

% --- Executes during object creation, after setting all properties.
function slider1_CreateFcn(hObject, eventdata, handles)
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


% --- Executes on slider movement.
function slider1_Callback(hObject, eventdata, handles)
% hObject    handle to slider1 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'Value') returns position of slider
%        get(hObject,'Min') and get(hObject,'Max') to determine range of slider

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Purpose of slider1_Callback function:
% Redraw image after threshold has been changed with slider

global orig;

thr_value = get(hObject,'Value');
axes(handles.bin_image)
X=orig;
bwimage = im2bw(X(:,:,1),thr_value);
colormap gray;
imagesc(bwimage);

set(handles.edit1,'String',num2str(thr_value));

% --- Executes during object creation, after setting all properties.
function edit1_CreateFcn(hObject, eventdata, handles)
% hObject    handle to edit1 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc
    set(hObject,'BackgroundColor','white');
else
    set(hObject,'BackgroundColor',get(0,'defaultUicontrolBackgroundColor'));
end



function edit1_Callback(hObject, eventdata, handles)
% hObject    handle to edit1 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of edit1 as text
%        str2double(get(hObject,'String')) returns contents of edit1 as a double

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Purpose of edit1_Callback function:
% Redraw image after threshold has been changed with threshold window

global orig;
thr_value=str2double(get(hObject,'String'));
axes(handles.bin_image)
bwimage = im2bw(orig,thr_value);
colormap gray;
imagesc(bwimage);

set(handles.slider1,'Value',thr_value);


% --- Executes on button press in OK.
function OK_Callback(hObject, eventdata, handles)
% hObject    handle to OK (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Purpose of OK_Callback function:
% Set output parameters correctly if OK button was pressed and changes
% accepted

handles.output = get(handles.slider1,'Value');
guidata(hObject, handles);
uiresume(handles.figure1);


% --- Executes on button press in cancel.
function cancel_Callback(hObject, eventdata, handles)
% hObject    handle to cancel (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Purpose of cancel_Callback function:
% Set output parameters correctly if Cancel button was pressed and changes
% rejected

handles.output = 2;
guidata(hObject, handles);
uiresume(handles.figure1);
