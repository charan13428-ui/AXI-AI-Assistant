const { app, BrowserWindow } = require('electron');
const path = require('path');
function createWindow(){ const win=new BrowserWindow({width:1100,height:760,minWidth:850,minHeight:600,backgroundColor:'#030712',title:'JARVIS',webPreferences:{preload:path.join(__dirname,'preload.js'),contextIsolation:true}}); win.loadFile('index.html'); }
app.whenReady().then(createWindow); app.on('window-all-closed',()=>{if(process.platform!=='darwin') app.quit();});
