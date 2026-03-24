// src/plugins/autoImportAllVue.ts
import type { App } from 'vue'

export default function autoImportAllVue(app: App) {
  // 1. 递归获取当前目录及所有子目录的 *.vue
  const modules = import.meta.glob('@/components/**/*.vue', {
    eager: true,
    import: 'default'   // 只取默认导出
  })

  // 2. 文件名（不含扩展名）直接当组件名
  Object.entries(modules).forEach(([filePath, component]) => {
    const fileName = filePath.split('/').pop()!.replace(/\.vue$/, '')
    console.log('autoImportAllVue:', fileName)
    app.component(fileName, component as any)
  })
}
