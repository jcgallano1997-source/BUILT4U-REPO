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
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-slate-900/50 p-4" onKeyDown={onKeyDown}>
      <div ref={ref} className={`mt-12 w-full ${width} overflow-hidden rounded-[18px] border border-slate-200 bg-white shadow-2xl`}>
        {/* Signature safety-stripe header accent. */}
        <div className="safety-stripe h-1" />
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-3.5">
          <h2 className="text-[15px] font-bold text-slate-800">{title}</h2>
          <button onClick={onClose} className="rounded-md p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600" aria-label="Close">
            <X size={18} />
          </button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>
  )
}

export const inputCls =
  'w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-blue-600 focus:ring-4 focus:ring-blue-600/12'
export const btnPrimary =
  'inline-flex items-center justify-center gap-1.5 rounded-lg bg-blue-600 px-3 py-2 text-sm font-semibold text-white shadow-sm shadow-blue-600/30 transition hover:bg-blue-700 disabled:opacity-60'
export const btnGhost =
  'inline-flex items-center justify-center gap-1.5 rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100'
