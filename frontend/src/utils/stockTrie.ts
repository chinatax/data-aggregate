/**
 * Trie-based stock name matcher.
 *
 * Build a prefix tree from stock names (2-6 Chinese characters),
 * then scan HTML text content for occurrences — longest match wins
 * at each position, avoiding false matches on short names.
 */

interface TrieNode {
  /** children keyed by character */
  children: Record<string, TrieNode>
  /** stock code if this node is a complete name match */
  code: string | null
}

let trieRoot: TrieNode | null = null
let maxNameLen = 0

export function buildTrie(stocks: { code: string; name: string }[]): void {
  const root: TrieNode = { children: {}, code: null }
  for (const s of stocks) {
    const name = s.name.trim()
    if (!name || name.length < 2) continue
    let node = root
    for (const ch of name) {
      if (!node.children[ch]) {
        node.children[ch] = { children: {}, code: null }
      }
      node = node.children[ch]
    }
    // longest match: only keep the first (longest name) inserted
    if (!node.code) {
      node.code = s.code
    }
    if (name.length > maxNameLen) maxNameLen = name.length
  }
  trieRoot = root
}

/**
 * Scan plain text for stock names using the Trie.
 * Returns an array of { start, end, code, name } matches,
 * sorted by start position, with no overlaps (longest match wins).
 */
export function findStockNames(text: string): Array<{
  start: number
  end: number
  code: string
  name: string
}> {
  if (!trieRoot) return []
  const matches: Array<{ start: number; end: number; code: string; name: string }> = []
  const n = text.length
  let i = 0
  while (i < n) {
    let bestEnd = -1
    let bestCode = ''
    let node = trieRoot
    let j = i
    while (j < n) {
      const ch = text[j]
      const next = node.children[ch]
      if (!next) break
      node = next
      j++
      if (node.code) {
        bestEnd = j
        bestCode = node.code
      }
    }
    if (bestEnd > i) {
      matches.push({
        start: i,
        end: bestEnd,
        code: bestCode,
        name: text.slice(i, bestEnd),
      })
      i = bestEnd
    } else {
      i++
    }
  }
  return matches
}

/**
 * Scan rendered HTML, wrapping matched stock names with <span class="stock-ref" data-code="XXX">.
 * Only modifies text nodes outside of HTML tags.
 * Returns the modified HTML.
 */
export function wrapStockNamesInHtml(html: string): string {
  if (!trieRoot) return html

  const result: string[] = []
  let pos = 0

  while (pos < html.length) {
    // Find next '<' (potential tag start)
    const lt = html.indexOf('<', pos)
    if (lt === -1) {
      // No more tags — scan remaining text
      result.push(replaceNamesInText(html.slice(pos)))
      break
    }

    // Text before tag: scan for stock names
    if (lt > pos) {
      result.push(replaceNamesInText(html.slice(pos, lt)))
    }

    // Skip past the HTML tag
    // Handle both open/close tags and self-closing tags
    let tagEnd = html.indexOf('>', lt)
    if (tagEnd === -1) {
      // malformed — just append rest
      result.push(html.slice(lt))
      break
    }
    tagEnd++ // include '>'
    result.push(html.slice(lt, tagEnd))
    pos = tagEnd
  }

  return result.join('')
}

function replaceNamesInText(text: string): string {
  const matches = findStockNames(text)
  if (!matches.length) return text

  const parts: string[] = []
  let pos = 0
  for (const m of matches) {
    parts.push(text.slice(pos, m.start))
    parts.push(`<span class="stock-ref" data-code="${m.code}">${text.slice(m.start, m.end)}</span>`)
    pos = m.end
  }
  parts.push(text.slice(pos))
  return parts.join('')
}

export function isTrieReady(): boolean {
  return trieRoot !== null
}
