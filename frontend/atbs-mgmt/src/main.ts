import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import autoImportAllVue from '@/plugins/autoImportAllVue'
import { createPersistedState } from 'pinia-persistedstate-plugin'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import './styles/global.scss'
// import components from './components/index'

import App from './App.vue'
import router from './router'

const app = createApp(App)
autoImportAllVue(app)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

const pinia = createPinia();
const persist = createPersistedState();
pinia.use(persist)
app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')