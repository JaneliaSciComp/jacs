
var G_INCOMPAT = false;
function GScript(src)
{
    document.write('<' + 'script src="' + src + '"' + ' type="text/javascript"><' + '/script>');
}
function GBrowserIsCompatible(setBodyClass)
{
    if (G_INCOMPAT) return false;
    if (!window.RegExp) return false;
    var AGENTS = ["opera","msie","safari","firefox","netscape","mozilla"];
    var agent = navigator.userAgent.toLowerCase();
    for (var i = 0; i < AGENTS.length; i++) {
        var agentStr = AGENTS[i];
        if (agent.indexOf(agentStr) != -1) {
            if (setBodyClass && document.body) {
                document.body.className = agentStr;
            }
            var versionExpr = new RegExp(agentStr + "[ \/]?([0-9]+(\.[0-9]+)?)");
            var version = 0;
            if (versionExpr.exec(agent) != null) {
                version = parseFloat(RegExp.$1);
            }
            if (agentStr == "opera") return version >= 7;
            if (agentStr == "safari") return version >= 125;
            if (agentStr == "msie") return (version >= 5.5 && agent.indexOf("powerpc") == -1);
            if (agentStr == "netscape") return version > 7;
            if (agentStr == "firefox") return version >= 0.8;
        }
    }
    return !!document.getElementById;
}
function GVerify()
{
}
function GLoad()
{
    GAddMessages({1415: '.',1416: ',',10037: 'Start address',10038: 'End address',10093: 'Terms of Use',10095: 'To see all the details that are visible on the screen,use the \x22Print\x22 link next to the map.',10096: '',10049: 'Map',10111: 'Map',10120: 'We are sorry, but we don\x27t have maps at this zoom level for this region.\x3cp\x3eTry zooming out for a broader look.\x3c/p\x3e',10050: 'Satellite',10112: 'Sat',10121: 'We are sorry, but we don\x27t have imagery at this zoom level for this region.\x3cp\x3eTry zooming out for a broader look.\x3c/p\x3e',10116: 'Hybrid',10117: 'Hyb',10021: 'Zoom In',10022: 'Zoom Out',10023: 'Click to set zoom level',10024: 'Drag to zoom',10507: 'Pan left',10508: 'Pan right',10509: 'Pan up',10510: 'Pan down',10029: 'Return to the last result',10511: 'Show street map',10512: 'Show satellite imagery',10513: 'Show imagery with street names',10806: 'Click to see this area on Google Maps',1616: 'km',1547: 'mi',10109: 'm',10110: 'ft',11259: 'Full-screen',10130: 'Address',10131: 'Details',10908: 'Untitled',10937: 'My Saved Places',10938: 'Create a new map...',10940: 'Error creating map',10941: 'Saved to %1$s',10942: 'Error saving placemark',10943: 'Saving...',10945: 'Add a placemark',10946: 'Draw a line',10947: 'Draw a shape',10948: 'Add an image',11371: 'Saved from \x3ca href\x3d\x22%1$s\x22\x3e%2$s\x3c/a\x3e',10985: 'Zoom in',10986: 'Zoom out',11047: 'Center map here',10983: 'Clear search results',10935: 'Save to My Maps',11271: 'Directions from here',11272: 'Directions to here',0: ''});
    GAddMessages({10807: 'Traffic',10808: 'Show Traffic',10809: 'Hide Traffic',11089: '\x3ca href\x3d\x22javascript:void(0);\x22\x3eZoom In\x3c/a\x3e to see traffic for this region',10293: 'Add',10294: 'Save',10295: 'Cancel',10296: 'Delete',10296: 'Delete',10297: 'New Location:',10298: 'Enable auto-saving of locations',10299: 'Select:',10299: 'Select:',10300: 'All',10300: 'All',10301: 'None',10301: 'None',10302: 'Edit',10303: 'Default',10304: 'Label',10304: 'Label',10304: 'Label',10305: 'Location',10305: 'Location',10305: 'Location',10307: 'There are no saved locations.',10308: 'Use this location as the initial map view',10309: 'Don\x27t use this location as the initial map view',11298: 'Street View',11303: 'Street View Help',11300: 'Hide Street View',540: 'New!',11274: 'To use street view, you need Adobe Flash Player version %1$d or newer.',11299: 'Show Street View',11302: 'Drag me onto a blue outlined street.\x3cbr\x3eYou can also just click on a blue outlined street.',11382: 'Get the latest Flash Player.',11304: 'Back to street view',11305: 'Using Street View',11306: 'In certain locations, you can view and navigate within street-level imagery. Here\x27s how:',11307: 'Blue outlines show roads where street view is available.',11308: 'This icon shows where you are on the map. The green arrow points in the direction you\x27re looking. You can drag the icon to navigate to a different location. You can also just click on a blue outlined road to go there.',11309: 'Drag the street view to look around 360\x26deg;. Use the arrow buttons to navigate down the street. You can also use the arrow keys on the keyboard.',11314: 'We\x27re sorry, street view is currently unavailable due to high demand.\x3cbr\x3ePlease try again later!',11315: 'Address is approximate',1559: 'N',1560: 'S',1561: 'W',1562: 'E',1608: 'NW',1591: 'NE',1605: 'SW',1606: 'SE',11298: 'Street View',11301: '\x3ca href\x3d\x22javascript:void(0);\x22\x3eZoom In\x3c/a\x3e to select a street view location',11302: 'Drag me onto a blue outlined street.\x3cbr\x3eYou can also just click on a blue outlined street.',11304: 'Back to street view',11305: 'Using Street View',11306: 'In certain locations, you can view and navigate within street-level imagery. Here\x27s how:',11307: 'Blue outlines show roads where street view is available.',11308: 'This icon shows where you are on the map. The green arrow points in the direction you\x27re looking. You can drag the icon to navigate to a different location. You can also just click on a blue outlined road to go there.',11309: 'Drag the street view to look around 360\x26deg;. Use the arrow buttons to navigate down the street. You can also use the arrow keys on the keyboard.',11310: 'Report Inappropriate Image',11311: 'Google takes concerns about its services very seriously. Please use the link below to report concerns about an inappropriate street view.',11312: 'Current Street View:',11313: 'Report inappropriate image',10001: 'Google Maps',10018: 'Loading...',10317: 'The content overlaid onto this map is provided by a third party, and Google is not responsible for it.',11068: 'Create new map',11112: '%1$s has been removed.',11113: 'Undo',11154: 'Reload',11166: 'Close',11169: 'There was a problem contacting the server, try again in a minute.',11170: 'Retry',11210: 'Browse the directory',11230: 'Unable to load...',11242: 'Edit settings',11251: 'Featured content',11255: 'By: \x3cspan class\x3d\x22mmauthor\x22\x3e%1$s\x3c/span\x3e',11265: 'Some content has been hidden',11266: 'Zoom to see more',11341: 'Created by me',11342: 'Created by others',11365: 'Personalize %1$s',11366: '\x3cb\x3eBrowse\x3c/b\x3e a directory of interactive content that you can add to Google Maps.',11367: '\x3cb\x3eCreate\x3c/b\x3e personalized, annotated maps and share them with friends \x26 family or the world.',11368: 'http://maps.google.com/help/maps/mymaps/add.html',11369: 'http://maps.google.com/help/maps/mymaps/create.html',11370: 'Learn more \x26raquo;',11684: 'This feature was not created by Google. Information you enter below may become available to the feature\x27s provider.',10018: 'Loading...',160: '\x3cH1\x3eServer Error\x3c/H1\x3eThe server encountered a temporary error and could not complete your request.\x3cp\x3ePlease try again in a minute or so.\x3c/p\x3e',10691: '/',10692: 'mdy',10693: '0',11198: 'Upcoming departures at this station:',11655: '...',10917: 'Click to place me on the map',10918: 'Click to start drawing a line',10919: 'Click to start drawing a shape',10920: 'Converting to plain text will lose some formatting. Continue?',10295: 'Cancel',10921: 'OK',10922: 'Title',10785: 'Description',10923: 'Rich text',10926: 'Line color',10927: 'Width (pixels)',10928: 'Line opacity',10929: 'Delete this point',10930: 'Continue this line',10931: 'Add a point',10933: 'Fill color',10934: 'Fill opacity',10940: 'Error creating map',10941: 'Saved to %1$s',10942: 'Error saving placemark',10943: 'Saving...',10908: 'Untitled',10944: 'Select/edit map features',10945: 'Add a placemark',10946: 'Draw a line',10947: 'Draw a shape',10948: 'Add an image',10949: 'Save',10950: 'Saved',10952: 'Please enter the URL to an image',11761: 'Please enter the URL of the image you want to overlay on the map.\n\ne.g. %1$s',10959: 'Edit',10960: 'Delete',10961: 'Drag to reposition this line',10962: 'Drag to reposition this shape',10963: 'Drag to move this point',10964: 'Drag to move this point',10965: 'Double-click to end this shape.',10966: 'Click to continue drawing a shape',10967: 'Double-click to end this line',10968: 'Click to continue drawing a line',11054: 'Drag to move this placemark',10969: 'Plain text',10970: 'Edit HTML',10868: 'First select the text that you want to make into a link.',10869: 'Enter a URL',10870: 'Huge',10871: 'Large',10872: 'Normal',10873: 'Small',10874: 'Bold',10875: 'Italic',10876: 'Underline',10877: 'Font',10878: 'Size',10879: 'Text Color',10880: 'Highlight Color',10881: 'Remove Formatting',10882: 'Link',10883: 'Numbered List',10884: 'Bulleted List',10885: 'Indent Less',10886: 'Indent More',10887: 'Align Left',10888: 'Align Center',10889: 'Align Right',10890: 'Insert Image',11098: 'Normal',11099: 'Times New Roman',11117: 'Arial',11101: 'Courier New',11102: 'Georgia',11103: 'Trebuchet',11104: 'Verdana',10913: 'Are you sure you want to delete this map?',10971: 'Are you sure you want to delete this placemark?',10972: 'Are you sure you want to delete this line?',10973: 'Are you sure you want to delete this shape?',10976: 'Are you sure you want to abandon unsaved changes to your map?',10977: 'Color',10978: 'Opacity',10979: 'Filled?',10980: 'Advanced',10981: 'Basic',10982: 'Line width (pixels)',11018: 'Properties',10914: 'This map does not exist.',11134: 'Edit title/settings',11068: 'Create new map',10529: 'My Maps',11055: 'Unable to contact server.',11056: 'Last saved at %1$s',11057: 'Edit line style',11059: 'Convert to filled shape',11058: 'Edit shape style',11072: 'Photo',11110: 'Maximum character length exceeded.',11124: 'You are no longer signed in to your Google Account.',11125: 'Please sign in',11126: 'To save your map, please sign in as %1$s',11127: 'Sorry, we\x27re having technical difficulties.\x3cbr /\x3e(Error code %1$d)',11128: 'Unable to save.',11129: 'View only',11130: 'Public',11131: 'Unlisted',11178: 'Public maps are included in search results.',11179: 'Learn more',11133: 'http://maps.google.com/help/maps/userguide/index.html#public',11371: 'Saved from \x3ca href\x3d\x22%1$s\x22\x3e%2$s\x3c/a\x3e',10734: 'Internal only',11551: 'Default icons',11552: 'My icons',11553: 'Add an icon',11554: 'You can link to a JPG, GIF, BMP or PNG file on the web.',11555: 'Icons larger than 64x64 pixels will be scaled down on the map.',11556: 'Enter the URL for the new icon.',10330: '\x26laquo; Back',11535: 'Total distance:',11585: 'Import KML',11586: 'Add map data from a KML, KMZ, or GeoRSS file to this map. This may take a few minutes depending on the speed of your internet connection.',11587: 'Maximum size: 10MB',11588: 'Browse your computer to select map data to upload',11589: 'Clear',11590: 'Or enter the url of map data on the web',11591: 'Replace everything on this map with the uploaded file',11592: 'All existing features will be deleted!',11593: 'Upload',11594: 'Upload from File',11595: 'Upload from URL',11596: 'Import KML...',11597: 'Try again',11598: 'Cancel',11599: 'Uploading file...',11600: 'Please wait \x26mdash; this may take a few minutes depending on the speed of your internet connection.',11601: 'Don\x27t worry \x26mdash; your file is still uploading. Please wait a little longer!',11602: 'We could not finish uploading your file.',11603: 'No changes have been made to the map.',11649: 'Enter the url of map data on the web',11714: 'Done',11745: 'Privacy settings',11746: 'Allow others to find this map in search results and on your profile.',11755: 'Are you sure you want to delete this placemark?',10985: 'Zoom in',10986: 'Zoom out',11047: 'Center map here',10983: 'Clear search results',10935: 'Save to My Maps',11208: 'Add a destination',11209: 'Remove this destination',11271: 'Directions from here',11272: 'Directions to here',11494: 'Share this map',11495: 'Send invitations',11496: 'Separate email addresses with commas',11497: 'Allow anyone to edit this map',11498: 'Collaborators',11499: 'Collaborators may invite others',11500: 'Remove',11501: 'Are you sure you want to remove %1$s as a collaborator?',11502: 'Invite people as collaborators',11503: 'Message:',11504: 'I\x27ve shared a map with you called %1$s:',11505: 'Add your message (optional)',11506: 'Send me a copy of this invitation',11507: 'Advanced Permissions',11509: 'remove all',11510: 'owner',11511: 'Collaborators may edit the map and invite others.',11512: 'Collaborators may edit the map.',11513: 'Me',11514: 'Are you sure you want to remove all collaborators?',11515: '1 Collaborator',11516: '%1$d',11517: '',11533: 'Sending Message...',11534: 'Collaborators have been invited.',11694: '',11717: 'Invite collaborators',11718: 'Manage collaborators',11719: 'Only the owner may change these settings',1557: 'Google Maps',11023: 'Send',11024: 'Send to:',11025: 'Email',11026: 'Car',11027: 'Include:',11028: 'Info:',11029: 'Notes:',11030: 'Cancel',11060: '\x3ca href\x3d\x22%1$s\x22\x3eLearn more\x3c/a\x3e.',11066: 'Required field',11031: 'Google Maps can send information to cars with \x3ca href\x3d\x22%1$s\x22\x3eBMW Assist\x3c/a\x3e.',11032: 'BMW Assist account name:',11033: 'Remember my account name',11034: 'Send \x26raquo;',11062: 'Sending to car ...',11035: 'Message was successfully sent to car!',11036: 'Sorry, we were unable to send this message.  Please visit \x3ca href\x3d\x22%1$s\x22\x3eBMW Assist\x3c/a\x3e and try again.',11039: 'To:',11040: 'From:',11041: 'Separate emails with ,',11042: 'Send a copy to my email.',11043: 'Message:',11044: 'Include business info.',11045: 'Send \x26raquo;',11061: 'Sending email ...',11063: 'Email was successfully sent!',11064: 'Sorry, we were unable to send this message.  Please try again later.',11074: 'Link: %1$s',11067: 'This email was sent to you by a user on %1$s (%2$s)',11075: 'Please select a result...',11076: 'Search results',10032: 'Directions',11077: 'Search results for \x22%1$s\x22',1601: 'Driving Directions',11079: 'Start address: %1$s',11080: 'End address: %1$s',11081: 'Travel: %1$s',11082: 'Start at: %1$s',10118: 'Arrive at: %1$s',11083: 'A %1$s link',11084: 'Hi, I\x27d like to share a %1$s link with you.',11085: 'Driving directions to %1$s',11086: 'http://www.bmw.com/myinfo',11087: 'http://maps.google.com/support/bin/answer.py?answer\x3d61175',11364: 'Invalid email address',11396: 'Data by %1$s',11393: 'Google Maps can send information to your navigation system.',11633: 'Google Maps can send information to cars with \x3ca href\x3d\x22%1$s\x22\x3eMercedes-Benz Search\x26amp;Send powered by Tele Aid\x3c/a\x3e.',11417: 'http://maps.google.com/support/bin/topic.py?topic\x3d12387',11459: 'http://maps.google.com/support/',11383: 'BMW Assist country:',11395: 'BMW Austria',11385: 'BMW Germany',11386: 'BMW UK',11387: 'BMW Italy',11388: 'Mercedes Tele Aid e-mail address:',11389: 'Make:',11390: 'BMW',11391: 'Mercedes-Benz',11384: 'Select one...',11613: 'Sorry, we were unable to send this message at this time. Please visit the \x3ca href\x3d\x22%1$s\x22\x3ehelp page\x3c/a\x3e.',11394: 'http://www.mbusa.com/searchandsend',11455: 'Google Maps can send information to \x3ca href\x3d\x22%1$s\x22\x3e%2$s\x3c/a\x3e.',11540: 'Type the characters you see in the picture below.',11541: 'Letters are not case-sensitive.',11456: 'GPS',11457: 'Sending to gps ...',1935: 'View Larger Map',11014: 'Today',11151: 'Please do not use inappropriate words such as: %1$s',10953: 'If you continue, you will lose unsaved changes.',11250: '\x3ca href\x3d\x22%1$s\x22\x3eSign in\x3c/a\x3e to view my existing bookmarks',11245: 'Drag to change route'});
    if (!GValidateKey("f03346eb3a99be025979045e8fa1a281c5159611")) {
        G_INCOMPAT = true;
        alert("The Google Maps API key used on this web site was registered for a different web site. You can generate a new key for this web site at http://www.google.com/apis/maps/.");
        return;
    }
    GLoadApi(["http://mt0.google.com/mt?n\x3d404\x26v\x3dap.61\x26","http://mt1.google.com/mt?n\x3d404\x26v\x3dap.61\x26","http://mt2.google.com/mt?n\x3d404\x26v\x3dap.61\x26","http://mt3.google.com/mt?n\x3d404\x26v\x3dap.61\x26"], ["http://kh0.google.com/kh?n\x3d404\x26v\x3d21\x26","http://kh1.google.com/kh?n\x3d404\x26v\x3d21\x26","http://kh2.google.com/kh?n\x3d404\x26v\x3d21\x26","http://kh3.google.com/kh?n\x3d404\x26v\x3d21\x26"], ["http://mt0.google.com/mt?n\x3d404\x26v\x3dapt.61\x26","http://mt1.google.com/mt?n\x3d404\x26v\x3dapt.61\x26","http://mt2.google.com/mt?n\x3d404\x26v\x3dapt.61\x26","http://mt3.google.com/mt?n\x3d404\x26v\x3dapt.61\x26"], "ABQIAAAAw02O7XzcrkyvtvSfu4pPdRTwM0brOpm-All5BF6PoaKBxRWWERTD1ftD0q2S6RGFoar1Qc_FByCmvA", "", "", true, "G");
    if (window.GJsLoaderInit) {
        GJsLoaderInit("http://www.google.com/intl/en_us/mapfiles/90/maps2" + ".api/main.js");
    }
}
function GUnload()
{
    if (window.GUnloadApi) {
        GUnloadApi();
    }
}
var _mF = [ 400,200,false,true,true,false,true,100,4096,"bounds_cippppt.txt","cities_cippppt.txt","local/add/flagStreetView",true,true,400,false,true,true,true,true,false,true,true,true,true,true,"/maps/c/ui/HovercardLauncher/dommanifest.js","no" ];
var _mHost = "http://maps.google.com";
var _mUri = "/maps";
var _mDomain = "google.com";
var _mStaticPath = "http://www.google.com/intl/en_us/mapfiles/";
var _mJavascriptVersion = "91";
var _mTermsUrl = "http://www.google.com/intl/en_us/help/terms_maps.html";
var _mHL = "en";
var _mGL = "us";
var _mTrafficEnableApi = true;
var _mTrafficTileServerUrlBase = "http://www.google.com/mapstt";
var _mCityblockLatestFlashUrl = "http://maps.google.com/local_url?q\x3dhttp://www.adobe.com/shockwave/download/download.cgi%3FP1_Prod_Version%3DShockwaveFlash\x26dq\x3d\x26file\x3dapi\x26v\x3d2\x26key\x3dABQIAAAAw02O7XzcrkyvtvSfu4pPdRTwM0brOpm-All5BF6PoaKBxRWWERTD1ftD0q2S6RGFoar1Qc_FByCmvA";
var _mCityblockLogUsage = true;
var _mWizActions = {hyphenSep: 1,breakSep: 2,dir: 3,searchNear: 6,savePlace: 9};
var _mIdcRouterPath = "/maps/mpl/router";
var _mIdcRelayPath = "/maps/mpl/relay";
var _mIGoogleUseXSS = false;
var _mIGoogleServerUntrustedUrl = "http://gmodules.com";
var _mIGoogleEt = "0avhrem7";
var _mMplGGeoXml = 100;
var _mMplGPoly = 1000;
var _mMplMapViews = 100;
var _mMplGeocoding = 100;
var _mMplDirections = 100;
var _mMplEnableGoogleLinks = true;
var _mIGoogleServerTrustedUrl = "";
var _mIGoogleEt = "0avhrem7";
var _mIGoogleUseXSS = false;
var _mMMEnableAddContent = true;
var _mMSEnablePublicView = true;
var _mSatelliteToken = "fzwq1M6tPOyFNbn2AYQDEtSDL2Ywc4bGikcOMA";
var _mMapCopy = "Map data \x26#169;2007 ";
var _mSatelliteCopy = "Imagery \x26#169;2007 ";
var _mGoogleCopy = "\x26#169;2007 Google";
var _mPreferMetric = false;
var _mPanelWidth = 20;
var _mMapPrintUrl = 'http://www.google.com/mapprint';
var _mAutocompleteFrom = 'from';
var _mAutocompleteTo = 'to';
var _mAutocompleteNearRe = '^(?:(?:.*?)\\s+)(?:(?:in|near|around|close to):?\\s+)(.+)$';
var _mSvgEnabled = true;
var _mSvgForced = false;
var _mLogInfoWinExp = true;
var _mLogPanZoomClks = false;
var _mLogWizard = true;
var _mLogLimitExceeded = true;
var _mLogPrefs = true;
var _mMMLogMyMapViewpoints = true;
var _mCalPopupMonths = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];
var _mCalPopupDaysOfWeek = ["S","M","T","W","T","F","S"];
var _mSXBmwAssistUrl = '';
var _mSXCarEnabled = true;
var _mSXServices = {car_bmw_at:{type:1,make:"11390",account:"11032",system:"BMW Assist",link:"11086",group:"11383",name:"11395"},car_bmw_de:{type:1,make:"11390",account:"11032",system:"BMW Assist",link:"11086",group:"11383",name:"11385"},car_bmw_it:{type:1,make:"11390",account:"11032",system:"BMW Assist",link:"11086",group:"11383",name:"11387"},car_bmw_gb:{type:1,make:"11390",account:"11032",system:"BMW Assist",link:"11086",group:"11383",name:"11386"},car_mercedes_us:{type:1,make:"11391",account:"11388",system:"Mercedes-Benz Search\x26amp;Send powered by Tele Aid",link:"11394"}};
var _mMSMarker = 'Placemark';
var _mMSLine = 'Line';
var _mMSPolygon = 'Shape';
var _mMSImage = 'Image';
var _mDirectionsDragging = true;
var _mDirectionsEnableApi = true;
var _mAdSenseForMapsEnable = "true";
var _mAdSenseForMapsFeedUrl = "http://pagead2.googlesyndication.com/afmaps/ads";
function GLoadMapsScript()
{
    if (GBrowserIsCompatible()) {
        GScript("http://www.google.com/intl/en_us/mapfiles/90/maps2" + ".api/main.js");
    }
}
GLoadMapsScript();