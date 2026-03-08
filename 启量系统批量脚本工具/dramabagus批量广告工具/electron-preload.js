const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  startAutomation: () => ipcRenderer.invoke('start-automation'),
  openDataFolder: () => ipcRenderer.invoke('open-data-folder'),
  openConfigFolder: () => ipcRenderer.invoke('open-config-folder'),
  onLog: (cb) => ipcRenderer.on('log', (_, line) => cb(line)),
  onShowPause: (cb) => ipcRenderer.on('show-pause', (_, msg) => cb(msg)),
  pauseContinue: () => ipcRenderer.send('pause-continue'),
  pauseQuit: () => ipcRenderer.send('pause-quit'),
});
