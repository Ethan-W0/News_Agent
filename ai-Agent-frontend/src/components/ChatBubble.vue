<template>
  <div class="bubble-row" :class="role === 'user' ? 'bubble-row--right' : 'bubble-row--left'">

    <!--
      头像统一作为 DOM 第一个子元素：
        AI   (row-left,  flex: row)         → 视觉上在左侧 ✓
        User (row-right, flex: row-reverse) → 视觉上在右侧 ✓
    -->
    <div
      class="bubble-avatar"
      :class="role === 'ai' ? 'bubble-avatar--ai' : 'bubble-avatar--user'"
      :aria-label="role === 'ai' ? 'AI' : '用户'"
    >
      <span class="avatar-icon">{{ role === 'ai' ? '折' : '客' }}</span>
    </div>

    <!-- 气泡主体 -->
    <div class="bubble-body">
      <span class="bubble-time">{{ timestamp }}</span>

      <div
        class="bubble-card"
        :class="role === 'user' ? 'bubble-card--user' : 'bubble-card--ai'"
      >

        <!-- ① 等待阶段：古诗词打字机（loading=true 且 content 为空） -->
        <Transition name="poem-fade">
          <div v-if="showPoem" class="waiting-poem">
            <span class="poem-text">{{ poemDisplay }}</span>
            <span class="poem-cursor" :class="{ blink: isCursorBlink }">｜</span>
          </div>
        </Transition>

        <!-- ② 内容阶段：流式接收 / 最终展示 -->
        <Transition name="content-fade">
          <div v-if="!showPoem" class="content-wrap">
            <!-- AI：Markdown 渲染 -->
            <div
              v-if="role === 'ai'"
              class="md-body"
              v-html="renderedContent"
            ></div>
            <!-- 用户：纯文本 -->
            <p v-else class="plain-text">{{ content }}</p>

            <!-- 流式输入光标 -->
            <span v-if="loading && content" class="stream-cursor">｜</span>
          </div>
        </Transition>

        <!-- 折角装饰 -->
        <span class="bubble-corner"></span>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { marked } from 'marked'

// 配置 marked：支持 GFM + 换行
marked.use({ gfm: true, breaks: true })

const props = defineProps({
  role:      { type: String,  default: 'ai' },
  content:   { type: String,  default: '' },
  timestamp: { type: String,  default: '' },
  loading:   { type: Boolean, default: false }
})

// ── Markdown 渲染 ──
const renderedContent = computed(() => {
  if (!props.content) return ''
  return marked.parse(props.content)
})

// ──────────────────────────────────────────────
//  诗词打字机（只对 AI loading 消息启用）
// ──────────────────────────────────────────────
const POEM = '桃李春风一杯酒，江湖夜雨十年灯'

const showPoem     = ref(props.role === 'ai' && props.loading && !props.content)
const poemDisplay  = ref('')
const isCursorBlink = ref(true)

let poemTimer    = null
let poemCharIdx  = 0
let poemDeleting = false

function tickPoem() {
  if (!showPoem.value) return

  if (!poemDeleting) {
    // 打字阶段
    if (poemCharIdx <= POEM.length) {
      poemDisplay.value = POEM.slice(0, poemCharIdx)
      poemCharIdx++
      isCursorBlink.value = false
      poemTimer = setTimeout(tickPoem, 130)
    } else {
      // 打完，停顿后开始删除
      isCursorBlink.value = true
      poemTimer = setTimeout(() => { poemDeleting = true; tickPoem() }, 1800)
    }
  } else {
    // 删除阶段
    if (poemCharIdx > 0) {
      poemCharIdx--
      poemDisplay.value = POEM.slice(0, poemCharIdx)
      isCursorBlink.value = false
      poemTimer = setTimeout(tickPoem, 70)
    } else {
      // 删完，停顿后重新打
      isCursorBlink.value = true
      poemDeleting = false
      poemTimer = setTimeout(tickPoem, 500)
    }
  }
}

// 当第一个 chunk 到来（content 从 '' 变为非空）→ 淡出诗词
watch(() => props.content, (newVal) => {
  if (newVal && showPoem.value) {
    showPoem.value = false
    if (poemTimer) clearTimeout(poemTimer)
  }
})

onMounted(() => {
  if (showPoem.value) {
    poemTimer = setTimeout(tickPoem, 400)
  }
})

onBeforeUnmount(() => {
  if (poemTimer) clearTimeout(poemTimer)
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

// ── 行布局 ──
.bubble-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-bottom: 1.4rem;
  animation: bubbleFadeIn 0.35s ease both;

  // 用户消息：row-reverse → 头像（DOM 第一位）视觉上在最右
  &--right { flex-direction: row-reverse; }
  // AI 消息：row → 头像（DOM 第一位）视觉上在最左
  &--left  { flex-direction: row; }
}

@keyframes bubbleFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

// ── 头像 ──
.bubble-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-family: $font-cursive;
  font-size: 0.85rem;
  letter-spacing: 0;

  &--ai {
    background: linear-gradient(135deg, $ink-green, darken($ink-green, 8%));
    color: $rice-white;
    box-shadow: 0 2px 8px rgba($ink-green, 0.35);
    border: 1.5px solid rgba($celadon, 0.4);
  }
  &--user {
    background: linear-gradient(135deg, $paper-yellow, darken($paper-yellow, 8%));
    color: $dark-brown;
    box-shadow: 0 2px 8px rgba($dark-brown, 0.2);
    border: 1.5px solid rgba($light-gray, 0.6);
  }
}

.avatar-icon { font-size: 0.9rem; font-weight: 600; }

// ── 气泡主体 ──
.bubble-body {
  max-width: 68%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.bubble-row--right .bubble-body { align-items: flex-end; }
.bubble-row--left  .bubble-body { align-items: flex-start; }

.bubble-time {
  font-size: 0.7rem;
  color: rgba($dark-brown, 0.45);
  letter-spacing: 0.05em;
  padding: 0 4px;
}

// ── 气泡卡片 ──
.bubble-card {
  position: relative;
  padding: 12px 16px;
  border-radius: $radius-md;
  font-family: $font-serif;
  font-size: 0.95rem;
  line-height: 1.85;
  word-break: break-word;
  min-width: 60px;
  min-height: 44px;

  // 信笺横线纹理
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background-image: repeating-linear-gradient(
      transparent, transparent 27px,
      rgba($light-gray, 0.25) 27px, rgba($light-gray, 0.25) 28px
    );
    pointer-events: none;
    z-index: 0;
  }

  &--user {
    background: linear-gradient(135deg, #faf6ec, #f0ead8);
    border: 1px solid rgba($dark-brown, 0.18);
    border-bottom-right-radius: 2px;
    box-shadow: 2px 3px 10px rgba($dark-brown, 0.10);
    color: $dark-brown;
  }
  &--ai {
    background: linear-gradient(135deg, #e8f0ee, #dde8e4);
    border: 1px solid rgba($ink-green, 0.22);
    border-bottom-left-radius: 2px;
    box-shadow: 2px 3px 10px rgba($ink-green, 0.10);
    color: $ink-black;
  }
}

// 折角
.bubble-corner {
  position: absolute;
  bottom: 0;
  width: 0;
  height: 0;
  .bubble-card--user & { right: -8px; border-top: 8px solid transparent; border-left: 9px solid rgba($dark-brown, 0.18); }
  .bubble-card--ai  & { left:  -8px; border-top: 8px solid transparent; border-right: 9px solid rgba($ink-green, 0.22); }
}

// ── ① 诗词等待区 ──
.waiting-poem {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: baseline;
  gap: 2px;
  padding: 4px 0;
  font-family: $font-cursive;
  font-size: 0.9rem;
  color: rgba($ink-green, 0.75);
  letter-spacing: 0.18em;
  white-space: nowrap;
}

.poem-cursor {
  color: $vermilion;
  font-weight: 300;
  &.blink { animation: poemCursorBlink 0.9s ease-in-out infinite; }
}
@keyframes poemCursorBlink {
  0%, 100% { opacity: 1; } 50% { opacity: 0; }
}

// 诗词淡出
.poem-fade-leave-active {
  transition: opacity 0.5s ease, transform 0.5s ease;
  position: absolute;   // 离场时脱离文档流，不撑开卡片高度
  z-index: 1;
}
.poem-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

// 内容淡入（比诗词离场晚 200ms 开始，形成交叠渐变）
.content-fade-enter-active {
  transition: opacity 0.5s ease 0.2s, transform 0.5s ease 0.2s;
}
.content-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

// ── ② 内容区 ──
.content-wrap {
  position: relative;
  z-index: 1;
}

.plain-text {
  white-space: pre-wrap;
  margin: 0;
}

// ── 流式光标 ──
.stream-cursor {
  display: inline-block;
  color: $vermilion;
  font-weight: 300;
  margin-left: 1px;
  animation: streamBlink 0.7s ease-in-out infinite;
}
@keyframes streamBlink {
  0%, 100% { opacity: 1; } 50% { opacity: 0; }
}

// ──────────────────────────────────────────────
//  ③ Markdown 渲染样式（中国风）
// ──────────────────────────────────────────────
.md-body {
  position: relative;
  z-index: 1;
  line-height: 1.9;
  color: $ink-black;

  // 段落
  :deep(p) {
    margin: 0.5em 0;
    &:first-child { margin-top: 0; }
    &:last-child  { margin-bottom: 0; }
  }

  // 标题
  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    font-family: $font-cursive;
    color: $ink-green;
    letter-spacing: 0.08em;
    margin: 1em 0 0.4em;
    font-weight: 500;
    line-height: 1.4;
    &:first-child { margin-top: 0; }
  }
  :deep(h1) { font-size: 1.2em; border-bottom: 1px solid rgba($light-gray, 0.8); padding-bottom: 0.3em; }
  :deep(h2) { font-size: 1.1em; }
  :deep(h3) { font-size: 1.05em; color: darken($ink-green, 5%); }
  :deep(h4) { font-size: 1em; color: $dark-brown; }

  // 列表
  :deep(ul), :deep(ol) {
    padding-left: 1.4em;
    margin: 0.4em 0;
  }
  :deep(li) {
    margin: 0.25em 0;
    // 自定义无序列表符号为朱红圆点
  }
  :deep(ul > li)::marker {
    color: $vermilion;
    content: '◆ ';
    font-size: 0.6em;
  }

  // 粗体
  :deep(strong) {
    font-weight: 600;
    color: $ink-green;
  }

  // 斜体
  :deep(em) {
    font-style: italic;
    color: rgba($dark-brown, 0.8);
  }

  // 分隔线
  :deep(hr) {
    border: none;
    height: 1px;
    background: linear-gradient(to right, transparent, $light-gray 20%, rgba($ink-green, 0.4) 50%, $light-gray 80%, transparent);
    margin: 0.8em 0;
  }

  // 行内代码
  :deep(code) {
    font-family: 'Courier New', monospace;
    font-size: 0.88em;
    background: rgba($ink-green, 0.08);
    color: $ink-green;
    padding: 0.1em 0.4em;
    border-radius: 3px;
    border: 1px solid rgba($ink-green, 0.15);
  }

  // 代码块
  :deep(pre) {
    background: rgba($ink-black, 0.05);
    border: 1px solid rgba($light-gray, 0.8);
    border-radius: $radius-sm;
    padding: 0.8em 1em;
    overflow-x: auto;
    margin: 0.6em 0;
    code {
      background: none;
      border: none;
      padding: 0;
      font-size: 0.85em;
      color: $ink-black;
    }
  }

  // 引用块
  :deep(blockquote) {
    border-left: 3px solid $vermilion;
    margin: 0.6em 0;
    padding: 0.3em 0.8em;
    background: rgba($vermilion, 0.04);
    color: rgba($dark-brown, 0.75);
    font-style: italic;
  }

  // 链接
  :deep(a) {
    color: $indigo;
    text-decoration: underline;
    text-decoration-color: rgba($indigo, 0.4);
    &:hover { color: $vermilion; }
  }
}
</style>
