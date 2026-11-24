const {contextBridge }=require('electron'); contextBridge.exposeInMainWorld('jarvisDesktop',{platform:'desktop'});
