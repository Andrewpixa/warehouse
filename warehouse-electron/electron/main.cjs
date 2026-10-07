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

function webBase() {
  const raw = readConfig().webBase || 'https://yaoheng.cloud'
  return String(raw).replace(/\/$/, '')
}

function createWindow() {
  const win = new BrowserWindow({
    width: 1280,
    height: 840,
    minWidth: 960,
    minHeight: 640,
    title: '药企进销存',
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  const started = Date.now()
  let failed = false
  win.webContents.on('did-finish-load', () => {
    console.log(`[electron] loaded in ${Date.now() - started}ms ${win.webContents.getURL()}`)
  })
  win.webContents.on('did-fail-load', (_event, code, desc, url, isMainFrame) => {
    if (!isMainFrame || failed || code === -3) return
    failed = true
    const html = `<!DOCTYPE html><html lang="zh-CN"><meta charset="utf-8"><title>打不开站点</title>
<body style="font-family:sans-serif;padding:40px;line-height:1.6">
<h1>打不开进销存站点</h1>
<p>${desc}（${code}）</p>
<p>地址：${url}</p>
<p>请检查网络，或修改 config.json 里的 webBase。</p>
</body></html>`
    win.loadURL('data:text/html;charset=utf-8,' + encodeURIComponent(html))
  })

  if (process.argv.includes('--dev')) {
    win.loadURL('http://127.0.0.1:5174')
    return
  }
  win.loadURL(webBase())
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
