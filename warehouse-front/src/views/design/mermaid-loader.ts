type MermaidApi = {
  initialize: (config: Record<string, unknown>) => void
  render: (id: string, text: string) => Promise<{ svg: string }>
}

declare global {
  interface Window {
    mermaid?: MermaidApi
  }
}

const SOURCES = [
  'https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js',
  'https://unpkg.com/mermaid@11.6.0/dist/mermaid.min.js'
]

let loading: Promise<MermaidApi> | null = null
let ready = false
let seq = 0

function inject(src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`无法加载 ${src}`))
    document.head.appendChild(script)
  })
}

export async function loadMermaid(): Promise<MermaidApi> {
  if (window.mermaid && ready) return window.mermaid
  if (loading) return loading

  loading = (async () => {
    if (!window.mermaid) {
      let lastError: Error | null = null
      for (const src of SOURCES) {
        try {
          await inject(src)
          lastError = null
          break
        } catch (e) {
          lastError = e instanceof Error ? e : new Error('mermaid 加载失败')
        }
      }
      if (!window.mermaid) {
        throw lastError ?? new Error('mermaid 脚本未就绪')
      }
    }
    window.mermaid.initialize({
      startOnLoad: false,
      theme: 'dark',
      securityLevel: 'loose',
      fontFamily: 'PingFang SC, Hiragino Sans GB, Microsoft YaHei, sans-serif',
      flowchart: { htmlLabels: true, useMaxWidth: true, curve: 'basis' },
      er: { useMaxWidth: true },
      sequence: { useMaxWidth: true },
      themeVariables: {
        darkMode: true,
        background: '#111218',
        primaryColor: '#1e2430',
        primaryTextColor: '#f6f1e8',
        primaryBorderColor: '#c9a227',
        lineColor: '#9aa0ab',
        secondaryColor: '#161a22',
        tertiaryColor: '#0b0d12',
        fontFamily: 'PingFang SC, Hiragino Sans GB, Microsoft YaHei, sans-serif',
        fontSize: '14px'
      }
    })
    ready = true
    return window.mermaid
  })()

  try {
    return await loading
  } catch (e) {
    loading = null
    throw e
  }
}

let queue: Promise<string> = Promise.resolve('')

export async function renderMermaid(code: string): Promise<string> {
  const run = async () => {
    const mermaid = await loadMermaid()
    seq += 1
    const id = `mmd${seq}_${Date.now()}`
    const { svg } = await mermaid.render(id, code)
    return svg
  }
  queue = queue.then(run, run)
  return queue
}
