import { useEffect, useState, type ReactNode } from 'react'
import { FileBadge } from 'lucide-react'
import { toast } from 'sonner'
import { btnPrimary, inputCls } from '@/components/Modal'
import { docErr, getDocSettings, saveDocSettings } from '@/lib/docsettings'

/** Admin: business branding stamped on report PDFs and sale receipts. */
export default function DocSettingsPage() {
  const [f, setF] = useState({
    businessName: '', addressLine: '', contactLine: '', tin: '', footerNote: '',
    accentColor: '#1D4ED8', receiptTitle: 'SALES RECEIPT', receiptFooter: 'Thank you!',
  })
  const [usingDefault, setUsingDefault] = useState(true)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const s = (k: keyof typeof f, v: string) => setF((p) => ({ ...p, [k]: v }))

  async function reload() {
    setLoading(true)
    try {
      const d = await getDocSettings()
      setUsingDefault(d.usingDefault)
      setF({
        businessName: d.businessName ?? '', addressLine: d.addressLine ?? '', contactLine: d.contactLine ?? '',
        tin: d.tin ?? '', footerNote: d.footerNote ?? '', accentColor: d.accentColor,
        receiptTitle: d.receiptTitle, receiptFooter: d.receiptFooter,
      })
    } catch (e) { toast.error(docErr(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() }, [])

  async function save() {
    setSaving(true)
    try {
      await saveDocSettings({
        businessName: f.businessName.trim() || undefined, addressLine: f.addressLine.trim() || undefined,
        contactLine: f.contactLine.trim() || undefined, tin: f.tin.trim() || undefined,
        footerNote: f.footerNote.trim() || undefined, accentColor: f.accentColor,
        receiptTitle: f.receiptTitle.trim() || undefined, receiptFooter: f.receiptFooter.trim() || undefined,
      })
      toast.success('Saved'); reload()
    } catch (e) { toast.error(docErr(e, 'Save failed')) } finally { setSaving(false) }
  }

  return (
    <div className="max-w-2xl space-y-4">
      <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
        <FileBadge size={18} className="text-indigo-600" /> Document settings
      </h1>
      {loading ? <p className="text-slate-400">Loading…</p> : (
        <div className="space-y-5">
          {usingDefault && <div className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-500">Using defaults until you save. These appear on report PDFs and sale receipts.</div>}

          <section className="space-y-3 rounded-lg border border-slate-200 bg-white p-4">
            <div className="text-sm font-medium text-slate-700">Business identity (letterhead)</div>
            <F label="Business name"><input className={inputCls} value={f.businessName} onChange={(e) => s('businessName', e.target.value)} placeholder="Built4U POS" /></F>
            <F label="Address"><input className={inputCls} value={f.addressLine} onChange={(e) => s('addressLine', e.target.value)} /></F>
            <div className="grid grid-cols-2 gap-3">
              <F label="Contact"><input className={inputCls} value={f.contactLine} onChange={(e) => s('contactLine', e.target.value)} /></F>
              <F label="TIN"><input className={inputCls} value={f.tin} onChange={(e) => s('tin', e.target.value)} /></F>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <F label="Report footer note"><input className={inputCls} value={f.footerNote} onChange={(e) => s('footerNote', e.target.value)} /></F>
              <F label="Accent colour">
                <div className="flex items-center gap-2">
                  <input type="color" className="h-9 w-12 rounded border border-slate-300" value={f.accentColor} onChange={(e) => s('accentColor', e.target.value)} />
                  <input className={inputCls} value={f.accentColor} onChange={(e) => s('accentColor', e.target.value)} />
                </div>
              </F>
            </div>
          </section>

          <section className="space-y-3 rounded-lg border border-slate-200 bg-white p-4">
            <div className="text-sm font-medium text-slate-700">Sale receipt</div>
            <div className="grid grid-cols-2 gap-3">
              <F label="Receipt title"><input className={inputCls} value={f.receiptTitle} onChange={(e) => s('receiptTitle', e.target.value)} /></F>
              <F label="Receipt footer"><input className={inputCls} value={f.receiptFooter} onChange={(e) => s('receiptFooter', e.target.value)} /></F>
            </div>
          </section>

          <div className="flex justify-end"><button className={btnPrimary} disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Save'}</button></div>
        </div>
      )}
    </div>
  )
}

function F({ label, children }: { label: string; children: ReactNode }) {
  return <div><label className="mb-1 block text-sm font-medium text-slate-700">{label}</label>{children}</div>
}
