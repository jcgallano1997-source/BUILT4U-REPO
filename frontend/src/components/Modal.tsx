import { useEffect, useRef, type KeyboardEvent as ReactKeyboardEvent, type ReactNode } from 'react'
import { X } from 'lucide-react'

/** Minimal centered modal with a backdrop. Esc closes; Enter (from a text/select
 *  field) triggers the primary Save button. */
export default function Modal({
  title,
  onClose,
  children,
  width = 'max-w-lg',
}: {
  title: string
  onClose: () => void
  children: ReactNode
  width?: string
}) {
  const ref = useRef<HTMLDivElement>(null)

  // Focus the first field on open so Enter/typing work immediately.
  useEffect(() => {
    const root = ref.current
    if (!root || root.contains(document.activeElement)) return
    ;(root.querySelector('input, select, textarea') as HTMLElement | null)?.focus()
  }, [])

  function onKeyDown(e: ReactKeyboardEvent<HTMLDivElement>) {
    if (e.key === 'Escape') { e.stopPropagation(); onClose(); return }
    if (e.key === 'Enter' && !e.shiftKey) {
      // Only submit from a text/select field — never a textarea (newline) or a
      // list dialog (would fire a row action). Clicks the primary Save button.
      const tag = (e.target as HTMLElement).tagName
      if (tag !== 'INPUT' && tag !== 'SELECT') return
      const save = ref.current?.querySelector('button.bg-blue-600:not([disabled])') as HTMLButtonElement | null
      if (save) { e.preventDefault(); save.click() }
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center bg-slate-900/40 p-4 overflow-y-auto" onKeyDown={onKeyDown}>
      <div ref={ref} className={`mt-12 w-full ${width} rounded-xl bg-white shadow-xl border border-slate-200`}>
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3">
          <h2 className="font-semibold text-slate-800">{title}</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600" aria-label="Close">
            <X size={18} />
          </button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>
  )
}

export const inputCls =
  'w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500'
export const btnPrimary =
  'inline-flex items-center justify-center gap-1.5 rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-60'
export const btnGhost =
  'inline-flex items-center justify-center gap-1.5 rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-700 hover:bg-slate-50'
