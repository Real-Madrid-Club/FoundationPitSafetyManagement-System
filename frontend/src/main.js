import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import BrandMark from './components/BrandMark.vue'
import DesignArt from './components/DesignArt.vue'
import PageHero from './components/PageHero.vue'
import EmptyState from './components/EmptyState.vue'
import { User, UserFilled, Lock } from '@element-plus/icons-vue'
import './styles/theme.css'

const app = createApp(App)

app.use(ElementPlus, { locale: zhCn })
for (const [name, component] of Object.entries({ BrandMark, DesignArt, PageHero, EmptyState, User, UserFilled, Lock })) app.component(name, component)
app.use(router)
app.mount('#app')
