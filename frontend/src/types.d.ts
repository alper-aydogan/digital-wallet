// CSS Modules
declare module '*.module.css' {
  const classes: { readonly [key: string]: string }
  export default classes
}

// Environment variables
declare interface ImportMetaEnv {
  readonly VITE_API_URL?: string
}

declare interface ImportMeta {
  readonly env: ImportMetaEnv
}
