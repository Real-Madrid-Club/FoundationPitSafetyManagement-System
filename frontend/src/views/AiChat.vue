<template>
<div class="ai-chat-container">
  <PageHero title="AI 智能助手" description="专业的基坑工程 AI 助手，助力安全管理与决策" />
  <el-card class="chat-card">
    <template #header>
      <div class="chat-header">
        <span class="mode-caption">选择助手模式</span>
        <el-radio-group v-model="mode" size="default" @change="handleModeChange">
          <el-radio-button value="chat">通用对话</el-radio-button>
          <el-radio-button value="maintenance">AI检修助手</el-radio-button>
          <el-radio-button value="health">健康监测分析</el-radio-button>
          <el-radio-button value="predict">趋势预测分析</el-radio-button>
        </el-radio-group>
      </div>
    </template>
    <div class="chat-body" ref="chatBodyRef">
      <div v-if="messages.length === 0" class="empty-hint">
        <DesignArt kind="robot" class="assistant-art" />
        <h2>{{ emptyHint }}</h2>
        <p>我可以帮助你解答基坑工程相关问题、分析监测数据、提供处理建议</p>
        <div class="suggestion-label">
          <span></span>
          你可以试试以下问题
          <span></span>
        </div>
        <div class="suggestion-grid">
          <button v-for="question in suggestions" :key="question" @click="useSuggestion(question)">
            <el-icon>
              <ChatDotRound />
            </el-icon>
            <span>{{ question }}</span>
            <el-icon>
              <Right />
            </el-icon>
          </button>
        </div>
      </div>
      <div v-for="(msg, index) in messages" :key="index" class="message-item" :class="msg.role">
        <div class="message-avatar">
          <el-avatar v-if="msg.role === 'user'" :size="36" icon="UserFilled" />
          <el-avatar v-else :size="36" style="background: #409eff">
            <span style="font-size: 14px; font-weight: bold">AI</span>
          </el-avatar>
        </div>
        <div class="message-content">
          <div class="message-role">{{ msg.role === 'user' ? '我' : 'DeepSeek AI' }}</div>
          <div class="message-text" v-html="formatMarkdown(msg.content)"></div>
        </div>
      </div>
      <div v-if="loading" class="message-item assistant">
        <div class="message-avatar">
          <el-avatar :size="36" style="background: #409eff">
            <span style="font-size: 14px; font-weight: bold">AI</span>
          </el-avatar>
        </div>
        <div class="message-content">
          <div class="message-role">DeepSeek AI</div>
          <div class="message-text loading-dots">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </div>
    <div class="chat-input">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="3"
        :placeholder="inputPlaceholder"
        resize="none"
        @keydown.enter.exact.prevent="handleSend"
      />
      <div class="input-actions">
        <el-button
          type="primary"
          :icon="Promotion"
          :loading="loading"
          :disabled="!inputText.trim()"
          @click="handleSend"
        >
          发送
        </el-button>
        <el-button :icon="Delete" :disabled="messages.length === 0" @click="handleClear">清空对话</el-button>
      </div>
    </div>
  </el-card>
</div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { Promotion, ChatDotRound, Right, Delete } from '@element-plus/icons-vue'
import { chatAi, maintenanceAssistant, healthMonitor, predictDevice } from '../api/ai'

const mode = ref('chat')
const inputText = ref('')
const messages = ref([])
const loading = ref(false)
const chatBodyRef = ref(null)
let conversationVersion = 0
const suggestions = computed(() => ({
  chat: ['基坑监测数据出现异常怎么办？','如何判断基坑的安全状态？','深基坑施工有哪些关键风险点？','请给出基坑降水的注意事项'],
  maintenance: ['测斜仪读数突然跳变，应如何排查？','传感器长时间没有数据怎么办？','轴力计需要进行哪些日常维护？','如何记录完整的设备维修日志？'],
  health: ['分析监测数据需要提供哪些指标？','如何区分结构异常与传感器故障？','多台传感器同时异常应如何处理？','温度变化会怎样影响轴力数据？'],
  predict: ['趋势预测需要多长时间的历史数据？','如何识别位移加速变化的趋势？','如何理解预测结果的不确定性？','轴力持续上升时应关注哪些指标？']
}[mode.value]))
function useSuggestion(question) { inputText.value = question; handleSend() }

const emptyHint = computed(() => {
  switch (mode.value) {
    case 'maintenance':
      return '我是基坑设备检修助手，请描述设备故障现象'
    case 'health':
      return '我是设备健康监测分析师，请提供设备监测数据'
    case 'predict':
      return '我是趋势预测分析师，请提供历史数据让我预测未来趋势'
    default:
      return '我是 AI 助手，你可以问我任何问题'
  }
})

const inputPlaceholder = computed(() => {
  switch (mode.value) {
    case 'maintenance':
      return '请描述故障现象，例如：测斜仪在3米深处读数跳变，波动范围超过±2mm...'
    case 'health':
      return '请粘贴或描述设备监测数据，例如：水位计#3 日平均水位变化...'
    case 'predict':
      return '请提供一段时间的历史数据，我会预测未来趋势，例如：4P1最近3天轴力(KN): 1100, 1120, 1150, 1190...'
    default:
      return '输入你的问题，按 Enter 发送...'
  }
})

function handleModeChange() {
  conversationVersion++
  messages.value = []
  loading.value = false
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  const version = conversationVersion
  loading.value = true
  inputText.value = ''

  messages.value.push({ role: 'user', content: text })
  await scrollToBottom()
  if (version !== conversationVersion) return

  const history = messages.value.slice(0, -1).map((m) => ({
    role: m.role,
    content: m.content,
  }))

  loading.value = true
  try {
    let response
    switch (mode.value) {
      case 'maintenance':
        response = await maintenanceAssistant(text, history)
        break
      case 'health':
        response = await healthMonitor(text, history)
        break
      case 'predict':
        response = await predictDevice(text, history)
        break
      default:
        response = await chatAi(text, history)
    }
    if (version === conversationVersion) messages.value.push({ role: 'assistant', content: response })
  } catch {
    if (version !== conversationVersion) return
    messages.value.push({
      role: 'assistant',
      content: '抱歉，AI 服务暂时不可用，请检查后端服务是否启动及 DeepSeek API Key 是否正确配置。',
    })
  } finally {
    if (version === conversationVersion) loading.value = false
    await scrollToBottom()
  }
}

function handleClear() {
  conversationVersion++
  messages.value = []
  loading.value = false
}

function formatMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/^- (.*?)(<br>|$)/gm, '<li>$1</li>')
    .replace(/(<li>.*?<\/li>)/s, '<ul>$1</ul>')
}

async function scrollToBottom() {
  await nextTick()
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
}
</script>

<style scoped>
.chat-card {
  border: 1px solid #e5efff;
  border-radius: 14px;
  box-shadow: 0 5px 18px #497fc708;
  min-height: 590px;
  height: calc(100dvh - 205px);
  display: flex;
  flex-direction: column;
  background: #ffffffed;
}

.chat-card :deep(.el-card__header) {
  border: 0;
  padding: 18px 22px 12px;
}

.chat-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
}

.mode-caption {
  margin-right: auto;
  color: #8094b5;
  font-size: 12px;
}

.chat-card :deep(.el-card__body) {
  padding: 0 22px 20px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.chat-header :deep(.el-radio-button__inner) {
  padding: 10px 19px;
  font-size: 13px;
}

.chat-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid #e5edfc;
  border-radius: 10px;
  background: radial-gradient(ellipse at 50% 36%,#f0f7ff70,transparent 60%),#fcfdff;
  padding: 20px;
}

.empty-hint {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.empty-hint .assistant-art {
  width: 340px;
  max-width: 70%;
  mix-blend-mode: multiply;
}

.empty-hint h2 {
  color: #142e62;
  font-size: 20px;
  margin: 12px 0 8px;
  font-weight: 650;
}

.empty-hint p {
  font-size: 13px;
  color: #7e93b5;
  line-height: 1.8;
  margin: 0;
}

.suggestion-label {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: #8b9bb5;
  margin: 20px 0 14px;
}

.suggestion-label span {
  width: 45px;
  height: 1px;
  background: #dce6f7;
}

.suggestion-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  width: min(680px,100%);
}

.suggestion-grid button {
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
  border: 1px solid #d7e6ff;
  border-radius: 8px;
  background: linear-gradient(100deg,#f1f6ff,#fafcff);
  color: #45618f;
  padding: 11px 15px;
  font-size: 12px;
  cursor: pointer;
  transition: border-color .2s,background .2s;
}

.suggestion-grid button:hover {
  background: #edf4ff;
  border-color: #6da6ff;
}

.suggestion-grid button>.el-icon {
  color: #398cff;
  font-size: 17px;
}

.suggestion-grid button>.el-icon:last-child {
  margin-left: auto;
  font-size: 14px;
}

.chat-input {
  margin-top: 15px;
  border: 1px solid #82b1ff;
  box-shadow: 0 0 0 3px #297fff06,0 3px 12px #2777ff0c;
  border-radius: 9px;
  padding: 10px 12px;
  background: #fff;
}

.chat-input :deep(.el-textarea__inner) {
  box-shadow: none !important;
  padding: 3px 4px;
  font-size: 13px;
  min-height: 48px !important;
  line-height: 1.7;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 5px;
}

.input-actions .el-button+.el-button {
  margin-left: 0;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-content {
  max-width: 85%;
  min-width: 0;
}

.message-role {
  font-size: 12px;
  color: #8b9cba;
  margin-bottom: 6px;
}

.message-item.user .message-role {
  text-align: right;
}

.message-text {
  padding: 12px 16px;
  background: #eef4fd;
  border-radius: 0 12px 12px 12px;
  color: #385580;
  line-height: 1.85;
  overflow-wrap: anywhere;
  font-size: 14px;
}

.message-item.user .message-text {
  background: linear-gradient(140deg,#338fff,#246cf2);
  color: white;
  border-radius: 12px 0 12px 12px;
}

.message-text :deep(code) {
  background: #00000008;
  border-radius: 4px;
  padding: 2px 5px;
}

.message-text :deep(ul) {
  padding-left: 20px;
}

.message-avatar {
  flex-shrink: 0;
}

.loading-dots {
  display: flex;
  gap: 5px;
  padding: 17px;
}

.loading-dots span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #6d9bef;
  animation: pulse 1.2s infinite;
}

.loading-dots span:nth-child(2) {
  animation-delay: .2s;
}

.loading-dots span:nth-child(3) {
  animation-delay: .4s;
}

@keyframes pulse {
  50% {
    opacity: .3;
    transform: translateY(-3px);
  }
}

@media (min-width:1600px) {
  .chat-card {
    height: calc(100dvh - 220px);
    min-height: 680px;
  }
  .empty-hint .assistant-art {
    width: 385px;
  }
  .empty-hint h2 {
    font-size: 23px;
  }
  .empty-hint p {
    font-size: 15px;
  }
  .suggestion-grid {
    width: 770px;
  }
  .suggestion-grid button {
    font-size: 14px;
  }
}

@media (max-width:1000px) {
  .mode-caption {
    display: none;
  }
  .chat-header :deep(.el-radio-button__inner) {
    padding: 9px 11px;
    font-size: 12px;
  }
  .chat-card :deep(.el-card__body) {
    padding: 0 14px 14px;
  }
  .empty-hint .assistant-art {
    width: 260px;
  }
  .empty-hint h2 {
    font-size: 17px;
  }
  .suggestion-grid button {
    padding: 10px;
    gap: 7px;
  }
}

@media (max-width:700px) {
  .chat-card {
    height: auto;
    min-height: 660px;
  }
  .chat-card :deep(.el-card__header) {
    padding: 14px 10px;
  }
  .chat-header {
    justify-content: center;
  }
  .chat-header :deep(.el-radio-button__inner) {
    padding: 9px 6px;
    font-size: 10px;
  }
  .chat-body {
    padding: 14px;
    min-height: 440px;
    max-height: 65vh;
  }
  .suggestion-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  .empty-hint .assistant-art {
    width: 220px;
  }
  .empty-hint h2 {
    font-size: 16px;
  }
  .empty-hint p {
    font-size: 12px;
  }
  .suggestion-label {
    margin: 15px 0 10px;
  }
  .chat-input {
    flex-shrink: 0;
  }
}
</style>
