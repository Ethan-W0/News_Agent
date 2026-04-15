/**
 * SSE 服务封装 — AI 新闻早间报
 * 使用 fetch + ReadableStream 实现流式读取，兼容 POST/GET
 * 接口：GET /api/ai/manus/chat?message=xxx&sessionId=xxx
 * 代理：Vite dev server 将 /api/* → http://localhost:8123/api/*
 */

const SSE_ENDPOINT = '/api/ai/manus/chat'

// 最大重试次数
const MAX_RETRIES = 3
// 重试间隔 (ms)
const RETRY_DELAY = 2000

/**
 * 发起 SSE 流式请求
 * @param {Object} params - { message, sessionId }
 * @param {Object} callbacks - { onChunk, onDone, onError }
 * @returns {Function} abort 函数，用于取消请求
 */
export function createSseConnection({ message, sessionId }, { onChunk, onDone, onError }) {
  const controller = new AbortController()
  let retryCount = 0

  async function connect() {
    try {
      const url = new URL(SSE_ENDPOINT, window.location.origin)
      url.searchParams.set('message', message)
      url.searchParams.set('sessionId', sessionId)

      const response = await fetch(url.toString(), {
        method: 'GET',
        signal: controller.signal,
        headers: {
          'Accept': 'text/event-stream',
          'Cache-Control': 'no-cache'
        }
      })

      if (!response.ok) {
        throw new Error(`HTTP 错误：${response.status} ${response.statusText}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()

        if (done) {
          // 处理缓冲区中剩余内容
          if (buffer.trim()) {
            processBuffer(buffer, onChunk)
          }
          onDone?.()
          break
        }

        buffer += decoder.decode(value, { stream: true })

        // 按 SSE 规范：每条消息以 \n\n 结尾
        const parts = buffer.split('\n\n')
        buffer = parts.pop() // 最后一段可能不完整，保留

        for (const part of parts) {
          processBuffer(part, onChunk)
        }
      }
    } catch (err) {
      // 用户主动取消 — 不视为错误
      if (err.name === 'AbortError') return

      console.error('[SSE] 连接错误：', err)

      // 自动重试
      if (retryCount < MAX_RETRIES) {
        retryCount++
        console.warn(`[SSE] 第 ${retryCount} 次重试，${RETRY_DELAY / 1000}s 后重连...`)
        setTimeout(connect, RETRY_DELAY)
      } else {
        onError?.(`连接失败，已重试 ${MAX_RETRIES} 次：${err.message}`)
      }
    }
  }

  connect()

  // 返回取消函数
  return () => controller.abort()
}

/**
 * 解析单条 SSE 消息块
 */
function processBuffer(buffer, onChunk) {
  const lines = buffer.split('\n')
  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) continue

    // 标准 SSE 格式：data: xxx
    if (trimmed.startsWith('data:')) {
      const data = trimmed.slice(5).trim()
      // 跳过心跳 / 结束标志
      if (data === '[DONE]' || data === 'done') return
      if (data) {
        try {
          // 尝试解析 JSON 格式 data
          const parsed = JSON.parse(data)
          // 兼容多种后端返回格式
          const text = parsed.content || parsed.text || parsed.message || parsed.data || data
          onChunk?.(text)
        } catch {
          // 非 JSON，直接作为文本追加
          onChunk?.(data)
        }
      }
    }
  }
}

/**
 * 使用原生 EventSource（兼容方案，仅支持 GET）
 */
export function createEventSource({ message, sessionId }, { onChunk, onDone, onError }) {
  const url = new URL(SSE_ENDPOINT, window.location.origin)
  url.searchParams.set('message', message)
  url.searchParams.set('sessionId', sessionId)

  const es = new EventSource(url.toString())

  es.onmessage = (event) => {
    const data = event.data
    if (data === '[DONE]' || data === 'done') {
      es.close()
      onDone?.()
      return
    }
    try {
      const parsed = JSON.parse(data)
      const text = parsed.content || parsed.text || parsed.message || data
      onChunk?.(text)
    } catch {
      onChunk?.(data)
    }
  }

  es.onerror = (err) => {
    console.error('[EventSource] 连接错误', err)
    if (es.readyState === EventSource.CLOSED) {
      onError?.('连接已断开，请重新发送消息')
    } else {
      onError?.('连接出现异常，正在尝试重连...')
    }
  }

  return () => es.close()
}
