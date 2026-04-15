import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useChatStore = defineStore('chat', () => {
  // ── 会话 ID（每次进入新闻页面时重新生成）──
  const sessionId = ref('')

  // ── 消息列表 ──
  // 每条消息格式：{ id, role: 'user'|'ai', content, timestamp, loading? }
  const messages = ref([])

  // ── SSE 连接状态 ──
  const isStreaming = ref(false)
  const connectionError = ref('')

  // 生成唯一会话 ID
  function generateSessionId() {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      sessionId.value = crypto.randomUUID()
    } else {
      // 降级：时间戳 + 随机数
      sessionId.value = `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
    }
  }

  // 添加用户消息
  function addUserMessage(content) {
    const msg = {
      id: Date.now(),
      role: 'user',
      content,
      timestamp: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }
    messages.value.push(msg)
    return msg.id
  }

  // 添加 AI 消息占位（流式追加用）
  function addAiPlaceholder() {
    const msg = {
      id: Date.now() + 1,
      role: 'ai',
      content: '',
      timestamp: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
      loading: true
    }
    messages.value.push(msg)
    return msg.id
  }

  // 追加 AI 流式内容（保持 loading=true，等 finishAiMessage 才结束）
  function appendAiContent(msgId, chunk) {
    const msg = messages.value.find(m => m.id === msgId)
    if (msg) {
      msg.content += chunk
      // 不在这里改 loading，让诗词→内容的过渡动画由 content 变化触发
    }
  }

  // 标记 AI 消息加载完成
  function finishAiMessage(msgId) {
    const msg = messages.value.find(m => m.id === msgId)
    if (msg) msg.loading = false
  }

  // 清空消息列表
  function clearMessages() {
    messages.value = []
  }

  // 设置连接错误
  function setConnectionError(err) {
    connectionError.value = err
  }

  return {
    sessionId,
    messages,
    isStreaming,
    connectionError,
    generateSessionId,
    addUserMessage,
    addAiPlaceholder,
    appendAiContent,
    finishAiMessage,
    clearMessages,
    setConnectionError
  }
})
