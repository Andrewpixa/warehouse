const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('employeeApp', {
  isElectron: true,
  getConfig: () => ipcRenderer.invoke('config:get'),
  setConfig: (partial) => ipcRenderer.invoke('config:set', partial)
})
