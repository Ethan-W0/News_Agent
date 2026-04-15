<template>
  <div class="typewriter-wrap" aria-live="polite">
    <span class="typewriter-text">{{ displayText }}</span>
    <span class="typewriter-cursor" :class="{ blink: isCursorBlinking }">｜</span>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  // 要循环展示的诗句列表
  poems: {
    type: Array,
    default: () => [
      '桃李春风一杯酒，',
      '江湖夜雨十年灯。'
    ]
  },
  // 打字速度 (ms/字)
  typeSpeed: { type: Number, default: 150 },
  // 删除速度 (ms/字)
  deleteSpeed: { type: Number, default: 80 },
  // 完整显示后的停顿 (ms)
  pauseAfterType: { type: Number, default: 2000 },
  // 删除完后的停顿 (ms)
  pauseAfterDelete: { type: Number, default: 600 }
})

const displayText = ref('')
const isCursorBlinking = ref(true)

let timer = null
let poemIndex = 0       // 当前展示第几句
let charIndex = 0       // 当前字符位置
let isDeleting = false  // 是否处于删除阶段
let isPausing = false   // 是否处于停顿阶段

// 将所有诗句拼接为一个完整字符串循环
function getCurrentPoem() {
  return props.poems[poemIndex % props.poems.length]
}

function tick() {
  if (isPausing) return

  const currentPoem = getCurrentPoem()

  if (!isDeleting) {
    // 正在打字
    if (charIndex <= currentPoem.length) {
      displayText.value = currentPoem.slice(0, charIndex)
      charIndex++
      isCursorBlinking.value = false
      timer = setTimeout(tick, props.typeSpeed)
    } else {
      // 打完一句 — 停顿后开始删除
      isCursorBlinking.value = true
      isPausing = true
      timer = setTimeout(() => {
        isPausing = false
        isDeleting = true
        tick()
      }, props.pauseAfterType)
    }
  } else {
    // 正在删除
    if (charIndex > 0) {
      charIndex--
      displayText.value = currentPoem.slice(0, charIndex)
      isCursorBlinking.value = false
      timer = setTimeout(tick, props.deleteSpeed)
    } else {
      // 删完 — 切换到下一句诗，停顿后继续打
      isCursorBlinking.value = true
      isDeleting = false
      poemIndex++
      isPausing = true
      timer = setTimeout(() => {
        isPausing = false
        tick()
      }, props.pauseAfterDelete)
    }
  }
}

onMounted(() => {
  timer = setTimeout(tick, 800)
})

onBeforeUnmount(() => {
  if (timer) clearTimeout(timer)
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.typewriter-wrap {
  display: inline-flex;
  align-items: baseline;
  font-family: $font-cursive;
  font-size: clamp(1.4rem, 3vw, 2.2rem);
  color: $ink-green;
  letter-spacing: 0.25em;
  line-height: 1.6;
}

.typewriter-text {
  white-space: pre;
}

.typewriter-cursor {
  display: inline-block;
  margin-left: 2px;
  color: $vermilion;
  font-weight: 300;
  opacity: 1;
  transform-origin: center bottom;

  &.blink {
    animation: cursorBlink 0.9s ease-in-out infinite;
  }
}

@keyframes cursorBlink {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0; }
}
</style>
