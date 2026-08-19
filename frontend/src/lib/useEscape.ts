import { useEffect, useRef } from 'react'

// Close handlers for every overlay currently open, in the order they opened —
// the last entry is the topmost, so Esc dismisses one layer at a time.
const stack: Array<() => void> = []

function onKeyDown(e: KeyboardEvent) {
  if (e.key !== 'Escape') return
  const top = stack[stack.length - 1]
  if (!top) return
  // Claim the key so the app-wide shortcut handler doesn't also act on it
  // (it would blur the focused field instead of closing this overlay).
  e.stopPropagation()
  top()
}

/**
 * Dismiss an overlay (modal, drawer, popover) with Esc.
 *
 * Listens on the document in the capture phase rather than on the overlay's own
 * element, so it fires wherever focus happens to be — including an overlay with
 * nothing focusable in it, where a React onKeyDown never sees the key at all.
 * When overlays are nested, only the topmost one closes.
 */
export function useEscape(onClose: () => void, enabled = true) {
  // Keep the latest callback without re-registering (and thus re-ordering the
  // stack) on every render, since callers usually pass an inline arrow.
  const cb = useRef(onClose)
  useEffect(() => { cb.current = onClose })

  useEffect(() => {
    if (!enabled) return
    const entry = () => cb.current()
    stack.push(entry)
    if (stack.length === 1) document.addEventListener('keydown', onKeyDown, true)
    return () => {
      const i = stack.lastIndexOf(entry)
      if (i >= 0) stack.splice(i, 1)
      if (stack.length === 0) document.removeEventListener('keydown', onKeyDown, true)
    }
  }, [enabled])
}
