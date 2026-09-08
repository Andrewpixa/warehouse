/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}

interface EmployeeAppConfig {
  apiBase: string
}

interface Window {
  employeeApp?: {
    isElectron: boolean
    getConfig: () => Promise<EmployeeAppConfig>
    setConfig: (partial: Partial<EmployeeAppConfig>) => Promise<EmployeeAppConfig>
  }
}
