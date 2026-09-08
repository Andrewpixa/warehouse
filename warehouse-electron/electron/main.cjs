const { app, BrowserWindow, ipcMain } = require('electron')
const fs = require('fs')
const path = require('path')

const DEFAULT_CONFIG = {
  apiBase: 'http://127.0.0.1:8899/warehouse'
}

function userConfigPath() {
  return path.join(app.getPath('userData'), 'config.json')
}

function bundledConfigPath() {
  if (app.isPackaged) {
    return path.join(process.resourcesPath, 'config.json')
  }
  return path.join(__dirname, '../config/config.json')
}

function readJson(filePath) {
  try {
    if (!fs.existsSync(filePath)) return null
    return JSON.parse(fs.readFileSync(filePath, 'utf8'))
  } catch {
    return null
  }
}

function readConfig() {
  return {
    ...DEFAULT_CONFIG,
    ...(readJson(bundledConfigPath()) || {}),
    ...(readJson(userConfigPath()) || {})
  }
}

function writeConfig(partial) {
  const next = { ...readConfig(), ...partial }
  fs.mkdirSync(path.dirname(userConfigPath()), { recursive: true })
  fs.writeFileSync(userConfigPath(), JSON.stringify(next, null, 2), 'utf8')
  return next
}

function createWindow() {
  const win = new BrowserWindow({
    width: 1280,
    height: 840,
    minWidth: 960,
    minHeight: 640,
    title: '药企员工工作台',
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  const distIndex = path.join(__dirname, '../dist/index.html')
  if (process.argv.includes('--dev')) {
    win.loadURL('http://127.0.0.1:5174')
  } else {
    win.loadFile(distIndex)
  }
}

ipcMain.handle('config:get', () => readConfig())
ipcMain.handle('config:set', (_event, partial) => writeConfig(partial || {}))

app.whenReady().then(() => {
  createWindow()
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    }
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
