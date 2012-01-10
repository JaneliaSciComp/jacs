
// JavaScript Document

// hide/unhide tabs

function changeTabOption(which){
document.getElementById('optionUsers').style.display = 'none';
document.getElementById('optionRoles').style.display = 'none';
document.getElementById('optionMappings').style.display = 'none';
document.getElementById('optionUsers' + 'Tab').style.backgroundColor = '#80b2ce';
document.getElementById('optionRoles' + 'Tab').style.backgroundColor = '#80b2ce';
document.getElementById('optionMappings' + 'Tab').style.backgroundColor = '#80b2ce';
document.getElementById(which).style.display = 'block';
document.getElementById(which + 'Tab').style.backgroundColor = '#1b73a4';
}
