import DOMPurify from 'dompurify'
import katex from 'katex'

// 将文本中的 $$...$$ 替换为 KaTeX 渲染的 HTML
// 支持行内 $...$ 和行间 $$...$$
export function renderMath(text) {
  if (!text) return ''
  let html = escapeHtml(text)
  // 行间公式 $$...$$
  html = html.replace(/\$\$([^$]+?)\$\$/g, (_, expr) => {
    try {
      return '<div class="my-2 text-center">' + katex.renderToString(unescapeHtml(expr), { displayMode: true, throwOnError: false }) + '</div>'
    } catch {
      return '<code class="text-red-500">公式错误</code>'
    }
  })
  // 行内公式 $...$
  html = html.replace(/\$([^$\n]+?)\$/g, (_, expr) => {
    try {
      return katex.renderToString(unescapeHtml(expr), { displayMode: false, throwOnError: false })
    } catch {
      return '<code class="text-red-500">公式错误</code>'
    }
  })
  return DOMPurify.sanitize(html, {
    USE_PROFILES: { html: true, mathMl: true, svg: true, svgFilters: true },
  })
}

function escapeHtml(s) {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}
function unescapeHtml(s) {
  return s
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
}
