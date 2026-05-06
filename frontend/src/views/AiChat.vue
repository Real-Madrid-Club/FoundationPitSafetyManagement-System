<template>
  <div class="ai-chat-container">
    <el-card class="chat-card">
      <template #header>
        <div class="chat-header">
          <span>AI 智能助手</span>
          <el-radio-group v-model="mode" size="small" @change="handleModeChange">
            <el-radio-button value="chat">通用对话</el-radio-button>
            <el-radio-button value="maintenance">AI检修助手</el-radio-button>
            <el-radio-button value="health">健康监测分析</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div class="chat-body" ref="chatBodyRef">
        <div v-if="messages.length === 0" class="empty-hint">
          <el-empty :description="emptyHint" />
        </div>
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message-item"
          :class="msg.role"
        >
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
              <span></span><span></span><span></span>
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
          <el-button :disabled="messages.length === 0" @click="handleClear">清空对话</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { Promotion } from '@element-plus/icons-vue'
import { chatAi, maintenanceAssistant, healthMonitor } from '../api/ai'

const mode = ref('chat')
const inputText = ref('')
const messages = ref([])
const loading = ref(false)
const chatBodyRef = ref(null)

const emptyHint = computed(() => {
  switch (mode.value) {
    case 'maintenance':
      return '我是基坑设备检修助手，请描述设备故障现象'
    case 'health':
      return '我是设备健康监测分析师，请提供设备监测数据'
    default:
      return '我是 DeepSeek AI 助手，你可以问我任何问题'
  }
})

const inputPlaceholder = computed(() => {
  switch (mode.value) {
    case 'maintenance':
      return '请描述故障现象，例如：测斜仪在3米深处读数跳变，波动范围超过±2mm...'
    case 'health':
      return '请粘贴或描述设备监测数据，例如：水位计#3 日平均水位变化...'
    default:
      return '输入你的问题，按 Enter 发送...'
  }
})

function handleModeChange() {
  messages.value = []
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  inputText.value = ''

  messages.value.push({ role: 'user', content: text })
  await scrollToBottom()

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
      default:
        response = await chatAi(text, history)
    }
    messages.value.push({ role: 'assistant', content: response })
  } catch {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，AI 服务暂时不可用，请检查后端服务是否启动及 DeepSeek API Key 是否正确配置。',
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function handleClear() {
  messages.value = []
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
.ai-chat-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 0;
}

.chat-card {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.empty-hint {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-item.user .message-content {
  align-items: flex-end;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}

.message-role {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.message-text {
  background: #f4f4f5;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.7;
  word-break: break-word;
}

.message-item.user .message-text {
  background: #409eff;
  color: #fff;
}

.message-text :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

.message-text :deep(strong) {
  font-weight: 600;
}

.message-text :deep(ul) {
  margin: 8px 0;
  padding-left: 20px;
}

.chat-input {
  padding: 16px 20px;
  border-top: 1px solid #e4e7ed;
  background: #fafafa;
}

.chat-input :deep(.el-textarea__inner) {
  border-radius: 8px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.loading-dots {
  display: flex;
  gap: 6px;
  padding: 8px 0;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #909399;
  animation: dot-bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}
.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}
.loading-dots span:nth-child(3) {
  animation-delay: 0s;
}

@keyframes dot-bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}
</style>
