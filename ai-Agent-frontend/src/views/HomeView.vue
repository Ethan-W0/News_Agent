<template>
  <main class="home">

    <!-- 山水背景装饰 -->
    <div class="home__bg-deco" aria-hidden="true">
      <!-- 远山轮廓 SVG -->
      <svg class="mountain-svg" viewBox="0 0 1440 320" preserveAspectRatio="none">
        <path d="M0,220 C180,180 260,100 360,130 C460,160 500,80 600,90 C700,100 740,170 840,150 C940,130 980,60 1080,80 C1180,100 1300,160 1440,140 L1440,320 L0,320 Z"
          fill="rgba(45,74,62,0.06)" />
        <path d="M0,260 C200,230 320,160 460,180 C600,200 640,140 760,155 C880,170 920,240 1060,220 C1200,200 1320,170 1440,185 L1440,320 L0,320 Z"
          fill="rgba(45,74,62,0.10)" />
        <path d="M0,300 C240,280 400,240 540,250 C680,260 760,290 900,280 C1040,270 1200,250 1440,260 L1440,320 L0,320 Z"
          fill="rgba(45,74,62,0.08)" />
      </svg>
    </div>

    <!-- 顶部竖排装饰字 -->
    <aside class="home__vertical-deco" aria-hidden="true">
      <span>雅</span><span>苑</span><span>墨</span><span>韵</span>
    </aside>

    <!-- 主内容区 -->
    <div class="home__content">
      <!-- 印章装饰 -->
      <div class="home__seal" aria-hidden="true">閱</div>

      <!-- 标题 -->
      <header class="home__header">
        <h1 class="home__title">雅苑</h1>
        <p class="home__subtitle">詩文薈萃 · 墨香書卷</p>
      </header>

      <!-- 毛笔横线 -->
      <div class="brush-divider home__divider"></div>

      <!-- 打字机诗词区 -->
      <section class="home__poem-area">
        <div class="poem-frame">
          <span class="poem-frame__corner poem-frame__corner--tl"></span>
          <span class="poem-frame__corner poem-frame__corner--tr"></span>
          <span class="poem-frame__corner poem-frame__corner--bl"></span>
          <span class="poem-frame__corner poem-frame__corner--br"></span>

          <Typewriter
            :poems="poems"
            :type-speed="145"
            :delete-speed="75"
            :pause-after-type="2200"
            :pause-after-delete="700"
          />
        </div>
        <p class="poem-author">— 黄庭坚《寄黄几复》</p>
      </section>

      <!-- 毛笔横线 -->
      <div class="brush-divider home__divider"></div>

      <!-- AI 新闻入口按钮 -->
      <section class="home__entrance">
        <p class="home__entrance-hint">每日朝闻，文以载道</p>
        <button class="btn-entrance" @click="goToNews">
          <span class="btn-entrance__inner">
            <span class="btn-entrance__icon" aria-hidden="true">📰</span>
            AI 新闻早间报
          </span>
          <span class="btn-entrance__ripple" aria-hidden="true"></span>
        </button>
        <p class="home__entrance-desc">每日 AI 动态，以古雅之笔娓娓道来</p>
      </section>
    </div>

    <!-- 底部装饰 -->
    <footer class="home__footer">
      <span class="footer-deco" aria-hidden="true">❖</span>
      <span>雅苑 · {{ currentYear }}</span>
      <span class="footer-deco" aria-hidden="true">❖</span>
    </footer>

  </main>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import Typewriter from '@/components/Typewriter.vue'

const router = useRouter()

const poems = [
  '桃李春风一杯酒，',
  '江湖夜雨十年灯。'
]

const currentYear = computed(() => new Date().getFullYear())

function goToNews() {
  router.push({ name: 'News' })
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

// ── 页面整体 ──
.home {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 2rem 1.5rem;
}

// ── 山水背景 ──
.home__bg-deco {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.mountain-svg {
  position: absolute;
  bottom: 0;
  width: 100%;
  height: auto;
}

// ── 竖排装饰字 ──
.home__vertical-deco {
  position: fixed;
  right: 2.5rem;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  z-index: 1;
  writing-mode: vertical-lr;

  span {
    font-family: $font-cursive;
    font-size: 1.1rem;
    color: rgba($ink-green, 0.3);
    letter-spacing: 0.4em;
    transition: color 0.3s ease;
    &:hover { color: rgba($ink-green, 0.7); }
  }

  @media (max-width: 600px) { display: none; }
}

// ── 主内容 ──
.home__content {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 680px;
  text-align: center;
  padding: 3rem 2.5rem;
  background: rgba($rice-white, 0.82);
  backdrop-filter: blur(8px);
  border: 1px solid rgba($light-gray, 0.7);
  border-radius: $radius-xl;
  box-shadow: $shadow-card, inset 0 0 60px rgba($paper-yellow, 0.3);

  // 仿宣纸纹理
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='200'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.75' numOctaves='4' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='200' height='200' filter='url(%23n)' opacity='0.03'/%3E%3C/svg%3E");
    pointer-events: none;
  }
}

// ── 印章 ──
.home__seal {
  position: absolute;
  top: -18px;
  left: 50%;
  transform: translateX(-50%);
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: $font-cursive;
  font-size: 1rem;
  color: $vermilion;
  border: 2px solid $vermilion;
  border-radius: 4px;
  background: $rice-white;
  box-shadow: 0 0 0 3px rgba($vermilion, 0.12);
  letter-spacing: 0;
}

// ── 标题 ──
.home__header {
  margin-bottom: 0.5rem;
}
.home__title {
  font-family: $font-cursive;
  font-size: clamp(2.4rem, 6vw, 3.8rem);
  font-weight: 400;
  color: $ink-green;
  letter-spacing: 0.5em;
  text-shadow: 1px 1px 0 rgba($dark-brown, 0.08);
  line-height: 1.2;
}
.home__subtitle {
  font-family: $font-serif;
  font-size: 0.9rem;
  color: rgba($dark-brown, 0.55);
  letter-spacing: 0.35em;
  margin-top: 0.4rem;
}

.home__divider {
  margin: 1.8rem 0;
}

// ── 诗词区域 ──
.home__poem-area {
  margin-bottom: 0.5rem;
}

.poem-frame {
  position: relative;
  display: inline-block;
  padding: 1.4rem 2rem;
  background: rgba($paper-yellow, 0.45);
  border: 1px solid rgba($light-gray, 0.8);
  border-radius: $radius-md;
  min-width: 280px;
  min-height: 68px;
}

// 窗棂四角装饰
.poem-frame__corner {
  position: absolute;
  width: 18px;
  height: 18px;
  border-color: $ink-green;
  border-style: solid;
  opacity: 0.45;

  &--tl { top: 6px; left: 6px;   border-width: 1.5px 0 0 1.5px; }
  &--tr { top: 6px; right: 6px;  border-width: 1.5px 1.5px 0 0; }
  &--bl { bottom: 6px; left: 6px;  border-width: 0 0 1.5px 1.5px; }
  &--br { bottom: 6px; right: 6px; border-width: 0 1.5px 1.5px 0; }
}

.poem-author {
  margin-top: 0.8rem;
  font-size: 0.8rem;
  color: rgba($dark-brown, 0.45);
  letter-spacing: 0.2em;
  font-style: italic;
}

// ── 入口按钮区 ──
.home__entrance {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.8rem;
}

.home__entrance-hint {
  font-size: 0.82rem;
  color: rgba($dark-brown, 0.45);
  letter-spacing: 0.25em;
}

.home__entrance-desc {
  font-size: 0.78rem;
  color: rgba($dark-brown, 0.38);
  letter-spacing: 0.15em;
}

// 入口按钮
.btn-entrance {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.8rem 2.4rem;
  background: linear-gradient(135deg, $ink-green, darken($ink-green, 10%));
  color: $rice-white;
  border: none;
  border-radius: 2px;
  font-family: $font-serif;
  font-size: 1.05rem;
  letter-spacing: 0.3em;
  cursor: pointer;
  overflow: hidden;
  transition: $transition-base;
  box-shadow: 3px 3px 12px rgba($ink-green, 0.35), inset 0 1px 0 rgba(255,255,255,0.08);

  // 上下双线边框（仿古印风）
  &::before,
  &::after {
    content: '';
    position: absolute;
    left: 8px;
    right: 8px;
    height: 1px;
    background: rgba($rice-white, 0.25);
  }
  &::before { top: 5px; }
  &::after  { bottom: 5px; }

  &:hover {
    background: linear-gradient(135deg, lighten($ink-green, 5%), $ink-green);
    transform: translateY(-2px);
    box-shadow: 4px 6px 18px rgba($ink-green, 0.4), inset 0 1px 0 rgba(255,255,255,0.12);
    color: #fff;
  }

  &:active {
    transform: translateY(0);
    box-shadow: 2px 2px 8px rgba($ink-green, 0.3);
  }
}

.btn-entrance__inner {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  position: relative;
  z-index: 1;
}

.btn-entrance__icon {
  font-size: 1rem;
}

// 波纹效果
.btn-entrance__ripple {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, rgba(255,255,255,0.15) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
}
.btn-entrance:hover .btn-entrance__ripple {
  opacity: 1;
}

// ── 底部 ──
.home__footer {
  position: relative;
  z-index: 1;
  margin-top: 2.5rem;
  font-size: 0.75rem;
  color: rgba($dark-brown, 0.35);
  letter-spacing: 0.3em;
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.footer-deco {
  color: rgba($vermilion, 0.4);
  font-size: 0.65rem;
}

// ── 响应式 ──
@media (max-width: 600px) {
  .home__content {
    padding: 2.5rem 1.5rem;
    border-radius: $radius-lg;
  }
  .poem-frame {
    min-width: 220px;
    padding: 1rem 1.2rem;
  }
}
</style>
