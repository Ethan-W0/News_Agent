<template>
  <div class="news-page">

    <!-- 顶部导航栏 -->
    <header class="news-header">
      <button class="btn-back" @click="goBack" title="返回">
        <span class="btn-back__arrow">←</span>
        <span class="btn-back__text">归苑</span>
      </button>

      <div class="news-header__center">
        <h1 class="news-header__title">AI 新闻早间报</h1>
        <p class="news-header__sub">每日朝闻 · 以文观世</p>
      </div>

      <div class="news-header__session">
        <span class="session-badge">
          <span class="session-dot" :class="{ active: !chatStore.isStreaming }"></span>
          会话 {{ shortSessionId }}
        </span>
      </div>
    </header>

    <!-- 聊天记录区域 -->
    <main class="chat-area" ref="chatAreaRef">

      <!-- 装饰性卷轴顶部 -->
      <div class="scroll-top-deco" aria-hidden="true">
        <div class="scroll-rod"></div>
      </div>

      <!-- 错误提示横幅 -->
      <Transition name="alert-slide">
        <div v-if="chatStore.connectionError" class="error-banner">
          <span class="error-banner__icon">⚠</span>
          <span>{{ chatStore.connectionError }}</span>
          <button @click="chatStore.setConnectionError('')" class="error-banner__close">✕</button>
        </div>
      </Transition>

      <!-- 欢迎消息 -->
      <div v-if="chatStore.messages.length === 0" class="welcome-area">
        <div class="welcome-card">
          <div class="welcome-card__deco" aria-hidden="true">☁</div>
          <h2 class="welcome-card__title">晨曦问候</h2>
          <p class="welcome-card__text">
            敬启者，今日 AI 新闻早间报已备妥。<br>
            可询问最新 AI 动态、技术前沿或行业要闻。
          </p>
          <div class="welcome-suggestions">
            <button
              v-for="q in suggestions"
              :key="q"
              class="suggestion-tag"
              @click="useSuggestion(q)"
            >{{ q }}</button>
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <TransitionGroup name="msg-list" tag="div" class="msg-list">
        <ChatBubble
          v-for="msg in chatStore.messages"
          :key="msg.id"
          :role="msg.role"
          :content="msg.content"
          :timestamp="msg.timestamp"
          :loading="msg.loading"
        />
      </TransitionGroup>

      <!-- 底部占位（自动滚动锚点）-->
      <div ref="bottomAnchorRef" class="bottom-anchor"></div>

      <!-- 装饰性卷轴底部 -->
      <div class="scroll-bottom-deco" aria-hidden="true">
        <div class="scroll-rod"></div>
      </div>
    </main>

    <!-- 输入区域 -->
    <footer class="input-area">
      <div class="input-wrap">
        <!-- 毛笔装饰图标 -->
        <span class="input-icon" aria-hidden="true">筆</span>

        <textarea
          ref="inputRef"
          v-model="inputText"
          class="input-box"
          placeholder="请赐教，书于此处…（Enter 发送，Shift+Enter 换行）"
          :disabled="chatStore.isStreaming"
          rows="1"
          @keydown.enter.exact.prevent="sendMessage"
          @keydown.shift.enter="() => {}"
          @input="autoResize"
        ></textarea>

        <button
          class="btn-send"
          :disabled="!canSend"
          @click="sendMessage"
          :title="chatStore.isStreaming ? '正在接收回复...' : '发送'"
        >
          <span v-if="!chatStore.isStreaming" class="btn-send__text">送</span>
          <span v-else class="btn-send__loading">
            <span></span><span></span><span></span>
          </span>
        </button>
      </div>

      <p class="input-tip">
        <span v-if="chatStore.isStreaming" class="tip-streaming">
          <span class="tip-dot"></span>
          AI 正挥毫作答，请稍候…
        </span>
        <span v-else>Enter 发送 · Shift+Enter 换行 · 会话 ID：{{ chatStore.sessionId }}</span>
      </p>
    </footer>

  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chatStore'
import ChatBubble from '@/components/ChatBubble.vue'
import { createSseConnection } from '@/services/sseService'

const router = useRouter()
const chatStore = useChatStore()

// ── refs ──
const inputText = ref('')
const chatAreaRef = ref(null)
const bottomAnchorRef = ref(null)
const inputRef = ref(null)

// 当前 SSE 的取消函数
let abortSse = null

// ── 初始化 ──
onMounted(() => {
  chatStore.generateSessionId()
  chatStore.clearMessages()
  chatStore.setConnectionError('')
})

onBeforeUnmount(() => {
  // 离开页面时关闭 SSE 连接
  if (abortSse) abortSse()
})

// ── 计算属性 ──
const shortSessionId = computed(() => {
  const id = chatStore.sessionId
  return id ? id.slice(0, 8) + '…' : '--'
})

const canSend = computed(() =>
  inputText.value.trim().length > 0 && !chatStore.isStreaming
)

// ── 快捷建议 ──
const suggestions = [
  '今日 AI 领域有哪些重大新闻？',
  '近期大模型技术有何突破？',
  'AI 行业监管政策最新动态？',
  '推荐几篇值得一读的 AI 论文'
]

function useSuggestion(text) {
  inputText.value = text
  nextTick(() => inputRef.value?.focus())
}

// ── 自动调整输入框高度 ──
function autoResize() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

// ── 滚动到底部 ──
async function scrollToBottom() {
  await nextTick()
  bottomAnchorRef.value?.scrollIntoView({ behavior: 'smooth' })
}

// ── 发送消息 ──
async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || chatStore.isStreaming) return

  // 清空输入
  inputText.value = ''
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }

  // 添加用户消息
  chatStore.addUserMessage(text)
  await scrollToBottom()

  // 添加 AI 占位消息
  const aiMsgId = chatStore.addAiPlaceholder()
  chatStore.isStreaming = true
  chatStore.setConnectionError('')
  await scrollToBottom()

  // 启动 SSE
  abortSse = createSseConnection(
    { message: text, sessionId: chatStore.sessionId },
    {
      onChunk(chunk) {
        chatStore.appendAiContent(aiMsgId, chunk)
        scrollToBottom()
      },
      onDone() {
        chatStore.finishAiMessage(aiMsgId)
        chatStore.isStreaming = false
        abortSse = null
        scrollToBottom()
      },
      onError(errMsg) {
        chatStore.finishAiMessage(aiMsgId)
        chatStore.isStreaming = false
        chatStore.setConnectionError(errMsg)
        abortSse = null
        // 如果 AI 消息为空，添加错误提示
        const msg = chatStore.messages.find(m => m.id === aiMsgId)
        if (msg && !msg.content) {
          msg.content = '（连接异常，请重新发送）'
        }
        scrollToBottom()
      }
    }
  )
}

// ── 返回主页 ──
function goBack() {
  if (abortSse) {
    abortSse()
    abortSse = null
  }
  router.push({ name: 'Home' })
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

// ── 页面布局 ──
.news-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f0ebe0;
  background-image:
    radial-gradient(ellipse at 10% 20%, rgba($ink-green, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 90% 80%, rgba($indigo, 0.05) 0%, transparent 50%),
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='400' height='400'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='400' height='400' filter='url(%23n)' opacity='0.035'/%3E%3C/svg%3E");
  overflow: hidden;
}

// ── 顶部导航 ──
.news-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.9rem 1.5rem;
  background: rgba($rice-white, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba($light-gray, 0.8);
  box-shadow: 0 2px 8px rgba($dark-brown, 0.07);
  flex-shrink: 0;
  position: relative;
  z-index: 10;

  // 顶部装饰线
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(to right, transparent, $ink-green, $vermilion, $ink-green, transparent);
  }
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  background: none;
  border: 1px solid rgba($ink-green, 0.3);
  border-radius: 2px;
  color: $ink-green;
  font-family: $font-serif;
  font-size: 0.85rem;
  padding: 0.35rem 0.85rem;
  cursor: pointer;
  letter-spacing: 0.15em;
  transition: $transition-base;

  &:hover {
    background: rgba($ink-green, 0.08);
    border-color: $ink-green;
  }
}

.btn-back__arrow { font-size: 1rem; }

.news-header__center {
  text-align: center;
}
.news-header__title {
  font-family: $font-cursive;
  font-size: clamp(1rem, 3vw, 1.3rem);
  color: $ink-green;
  letter-spacing: 0.25em;
  font-weight: 400;
}
.news-header__sub {
  font-size: 0.7rem;
  color: rgba($dark-brown, 0.45);
  letter-spacing: 0.2em;
  margin-top: 2px;
}

.news-header__session {
  display: flex;
  align-items: center;
}
.session-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.7rem;
  color: rgba($dark-brown, 0.45);
  letter-spacing: 0.05em;
  background: rgba($light-gray, 0.4);
  padding: 0.25rem 0.6rem;
  border-radius: 10px;
  border: 1px solid rgba($light-gray, 0.7);
}
.session-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $light-gray;
  transition: background 0.3s;
  &.active { background: $celadon; box-shadow: 0 0 4px $celadon; }
}

// ── 聊天区域 ──
.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 0;
  display: flex;
  flex-direction: column;
  // 自定义滚动条
  scrollbar-width: thin;
  scrollbar-color: rgba($ink-green, 0.3) transparent;
}

// 卷轴装饰
.scroll-top-deco,
.scroll-bottom-deco {
  flex-shrink: 0;
  padding: 0 1rem;
}
.scroll-top-deco { padding-top: 0.5rem; }
.scroll-bottom-deco { padding-bottom: 0.5rem; }

.scroll-rod {
  height: 10px;
  background: linear-gradient(to right,
    transparent,
    rgba($dark-brown, 0.12) 15%,
    rgba($dark-brown, 0.2) 50%,
    rgba($dark-brown, 0.12) 85%,
    transparent
  );
  border-radius: 5px;
}

// 错误横幅
.error-banner {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin: 0.5rem 1.5rem;
  padding: 0.7rem 1rem;
  background: rgba($vermilion, 0.08);
  border: 1px solid rgba($vermilion, 0.3);
  border-radius: $radius-md;
  font-size: 0.85rem;
  color: darken($vermilion, 10%);
}
.error-banner__icon { font-size: 1rem; }
.error-banner__close {
  margin-left: auto;
  background: none;
  border: none;
  cursor: pointer;
  color: rgba($vermilion, 0.7);
  font-size: 0.8rem;
  padding: 2px 4px;
  &:hover { color: $vermilion; }
}

// 错误横幅过渡
.alert-slide-enter-active,
.alert-slide-leave-active { transition: all 0.3s ease; }
.alert-slide-enter-from,
.alert-slide-leave-to { opacity: 0; transform: translateY(-8px); }

// 欢迎卡片
.welcome-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.welcome-card {
  text-align: center;
  max-width: 420px;
  padding: 2rem;
  background: rgba($rice-white, 0.7);
  border: 1px solid rgba($light-gray, 0.7);
  border-radius: $radius-lg;
  box-shadow: $shadow-paper;
}
.welcome-card__deco {
  font-size: 2.5rem;
  color: rgba($ink-green, 0.25);
  margin-bottom: 0.5rem;
}
.welcome-card__title {
  font-family: $font-cursive;
  font-size: 1.4rem;
  color: $ink-green;
  letter-spacing: 0.3em;
  margin-bottom: 0.8rem;
}
.welcome-card__text {
  font-size: 0.9rem;
  color: rgba($dark-brown, 0.65);
  line-height: 1.9;
  letter-spacing: 0.05em;
  margin-bottom: 1.2rem;
}

.welcome-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: center;
}
.suggestion-tag {
  padding: 0.4rem 0.85rem;
  background: rgba($paper-yellow, 0.6);
  border: 1px solid rgba($light-gray, 0.8);
  border-radius: 2px;
  font-family: $font-serif;
  font-size: 0.78rem;
  color: $dark-brown;
  cursor: pointer;
  letter-spacing: 0.05em;
  transition: $transition-base;

  &:hover {
    background: rgba($ink-green, 0.1);
    border-color: rgba($ink-green, 0.4);
    color: $ink-green;
  }
}

// 消息列表
.msg-list {
  padding: 0.5rem 1.5rem 1rem;
  display: flex;
  flex-direction: column;
  flex: 1;
}
.msg-list-enter-active { transition: all 0.3s ease; }
.msg-list-enter-from   { opacity: 0; transform: translateY(12px); }

.bottom-anchor { height: 1px; }

// ── 输入区域 ──
.input-area {
  flex-shrink: 0;
  padding: 0.9rem 1.5rem 1rem;
  background: rgba($rice-white, 0.92);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba($light-gray, 0.8);
  box-shadow: 0 -2px 10px rgba($dark-brown, 0.06);

  // 底部装饰线
  &::after {
    content: '';
    display: block;
    margin-top: 0.6rem;
    height: 1px;
    background: linear-gradient(to right, transparent, rgba($light-gray, 0.5), transparent);
  }
}

.input-wrap {
  display: flex;
  align-items: flex-end;
  gap: 0.6rem;
  background: rgba($paper-yellow, 0.35);
  border: 1px solid rgba($light-gray, 0.9);
  border-radius: $radius-md;
  padding: 0.55rem 0.6rem 0.55rem 0.8rem;
  transition: $transition-base;
  box-shadow: inset 0 1px 3px rgba($dark-brown, 0.06);

  &:focus-within {
    border-color: rgba($ink-green, 0.5);
    box-shadow: inset 0 1px 3px rgba($dark-brown, 0.06), 0 0 0 3px rgba($ink-green, 0.08);
  }
}

.input-icon {
  font-family: $font-cursive;
  font-size: 1.2rem;
  color: rgba($ink-green, 0.35);
  margin-bottom: 2px;
  user-select: none;
  flex-shrink: 0;
}

.input-box {
  flex: 1;
  background: none;
  border: none;
  outline: none;
  resize: none;
  font-family: $font-serif;
  font-size: 0.95rem;
  color: $dark-brown;
  line-height: 1.7;
  min-height: 26px;
  max-height: 120px;
  overflow-y: auto;
  scrollbar-width: thin;

  &::placeholder {
    color: rgba($dark-brown, 0.35);
    font-style: italic;
    letter-spacing: 0.05em;
  }
  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.btn-send {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $ink-green, darken($ink-green, 8%));
  border: none;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: $transition-base;
  box-shadow: 2px 2px 6px rgba($ink-green, 0.3);

  &:hover:not(:disabled) {
    background: linear-gradient(135deg, lighten($ink-green, 5%), $ink-green);
    transform: translateY(-1px);
    box-shadow: 3px 4px 10px rgba($ink-green, 0.35);
  }
  &:active:not(:disabled) {
    transform: translateY(0);
  }
  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }
}

.btn-send__text {
  font-family: $font-cursive;
  font-size: 1rem;
  color: $rice-white;
  letter-spacing: 0;
}

.btn-send__loading {
  display: flex;
  gap: 3px;
  align-items: center;
  span {
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: $rice-white;
    opacity: 0.8;
    animation: sendDot 1s ease-in-out infinite;
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}
@keyframes sendDot {
  0%, 80%, 100% { transform: scale(0.6); }
  40%           { transform: scale(1.2); }
}

.input-tip {
  margin-top: 0.45rem;
  font-size: 0.7rem;
  color: rgba($dark-brown, 0.38);
  letter-spacing: 0.05em;
  text-align: center;
  height: 1em;
}

.tip-streaming {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: $ink-green;
}
.tip-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $ink-green;
  animation: tipPulse 1.2s ease-in-out infinite;
}
@keyframes tipPulse {
  0%, 100% { transform: scale(1); opacity: 0.7; }
  50%       { transform: scale(1.4); opacity: 1; }
}

// ── 响应式 ──
@media (max-width: 600px) {
  .news-header { padding: 0.7rem 1rem; }
  .btn-back__text { display: none; }
  .session-badge { display: none; }
  .msg-list { padding: 0.5rem 0.8rem 1rem; }
  .input-area { padding: 0.7rem 0.8rem 0.8rem; }
}
</style>
