import { useEffect, useState, type ReactNode } from 'react'
import { FileBadge, Upload, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { btnPrimary, btnGhost, inputCls } from '@/components/Modal'
import { useAuthStore } from '@/store/authStore'
import {
  docErr, getDocSettings, saveDocIdentity, saveDocPdf, saveDocReceipt,
  uploadDocLogo, deleteDocLogo, fetchDocLogoObjectUrl,
} from '@/lib/docsettings'

const DEFAULTS = {
  businessName: '', addressLine: '', contactLine: '', tin: '', footerNote: '',
  accentColor: '#1D4ED8', receiptTitle: 'SALES RECEIPT', receiptFooter: 'Thank you!',
  logoPosition: 'LEFT', showLogoPdf: true, paperSize: 'A4', orientation: 'LANDSCAPE',
  marginPreset: 'NORMAL', fontScale: 'NORMAL', zebraStriping: true,
  showPageNumbers: true, showTimestamp: true, showPrintedBy: true,
  showLogoReceipt: false, receiptHeaderNote: '', receiptShowCashier: true,
  receiptShowCustomer: true, receiptShowVoucher: true, receiptFormat: 'THERMAL_80MM',
}

/** Admin: branding + templating, split into independently-permissioned sections. */
export default function DocSettingsPage() {
  const modules = useAuthStore((s) => s.user?.modules ?? [])
  const canIdentity = modules.includes('DOC_SETTINGS')
  const canPdf = modules.includes('PDF_CONFIG')
  const canReceipt = modules.includes('RECEIPT_CONFIG')

  const [f, setF] = useState({ ...DEFAULTS })
  const [usingDefault, setUsingDefault] = useState(true)
  const [hasLogo, setHasLogo] = useState(false)
  const [logoUrl, setLogoUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState<string | null>(null)
  const set = <K extends keyof typeof f>(k: K, v: (typeof f)[K]) => setF((p) => ({ ...p, [k]: v }))

  async function reload() {
    setLoading(true)
    try {
      const d = await getDocSettings()
      setUsingDefault(d.usingDefault)
      setHasLogo(d.hasLogo)
      setF({
        businessName: d.businessName ?? '', addressLine: d.addressLine ?? '', contactLine: d.contactLine ?? '',
        tin: d.tin ?? '', footerNote: d.footerNote ?? '', accentColor: d.accentColor,
        receiptTitle: d.receiptTitle, receiptFooter: d.receiptFooter,
        logoPosition: d.logoPosition, showLogoPdf: d.showLogoPdf, paperSize: d.paperSize,
        orientation: d.orientation, marginPreset: d.marginPreset, fontScale: d.fontScale,
        zebraStriping: d.zebraStriping, showPageNumbers: d.showPageNumbers, showTimestamp: d.showTimestamp,
        showPrintedBy: d.showPrintedBy, showLogoReceipt: d.showLogoReceipt,
        receiptHeaderNote: d.receiptHeaderNote ?? '', receiptShowCashier: d.receiptShowCashier,
        receiptShowCustomer: d.receiptShowCustomer, receiptShowVoucher: d.receiptShowVoucher,
        receiptFormat: d.receiptFormat,
      })
      setLogoUrl(d.hasLogo ? await fetchDocLogoObjectUrl() : null)
    } catch (e) { toast.error(docErr(e, 'Failed to load')) }
    finally { setLoading(false) }
  }
  useEffect(() => { reload() }, [])

  async function run(section: string, fn: () => Promise<unknown>) {
    setSaving(section)
    try { await fn(); toast.success('Saved'); reload() }
    catch (e) { toast.error(docErr(e, 'Save failed')) } finally { setSaving(null) }
  }

  const saveIdentity = () => run('identity', () => saveDocIdentity({
    businessName: f.businessName.trim() || undefined, addressLine: f.addressLine.trim() || undefined,
    contactLine: f.contactLine.trim() || undefined, tin: f.tin.trim() || undefined, logoPosition: f.logoPosition,
  }))
  const savePdf = () => run('pdf', () => saveDocPdf({
    footerNote: f.footerNote.trim() || undefined, accentColor: f.accentColor, showLogoPdf: f.showLogoPdf,
    paperSize: f.paperSize, orientation: f.orientation, marginPreset: f.marginPreset, fontScale: f.fontScale,
    zebraStriping: f.zebraStriping, showPageNumbers: f.showPageNumbers, showTimestamp: f.showTimestamp,
    showPrintedBy: f.showPrintedBy,
  }))
  const saveReceipt = () => run('receipt', () => saveDocReceipt({
    receiptTitle: f.receiptTitle.trim() || undefined, receiptFooter: f.receiptFooter.trim() || undefined,
    receiptHeaderNote: f.receiptHeaderNote.trim() || undefined, showLogoReceipt: f.showLogoReceipt,
    receiptShowCashier: f.receiptShowCashier, receiptShowCustomer: f.receiptShowCustomer,
    receiptShowVoucher: f.receiptShowVoucher, receiptFormat: f.receiptFormat,
  }))

  async function onLogoPick(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    try { await uploadDocLogo(file); toast.success('Logo uploaded'); reload() }
    catch (err) { toast.error(docErr(err, 'Upload failed')) }
  }
  async function removeLogo() {
    try { await deleteDocLogo(); toast.success('Logo removed'); setLogoUrl(null); reload() }
    catch (err) { toast.error(docErr(err, 'Remove failed')) }
  }

  const SaveBtn = ({ section, onClick }: { section: string; onClick: () => void }) => (
    <div className="flex justify-end"><button className={btnPrimary} disabled={saving === section} onClick={onClick}>{saving === section ? 'Saving…' : 'Save'}</button></div>
  )

  return (
    <div className="max-w-2xl space-y-4">
      <h1 className="flex items-center gap-2 text-xl font-semibold text-slate-800">
        <FileBadge size={18} className="text-blue-600" /> Document settings
      </h1>
      {loading ? <p className="text-slate-400">Loading…</p> : (
        <div className="space-y-5">
          {usingDefault && <div className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-500">Using defaults until you save. These appear on report PDFs and sale receipts.</div>}

          {canIdentity && (
            <section className="space-y-3 rounded-lg border border-slate-200 bg-white p-4">
              <div className="text-sm font-medium text-slate-700">Business identity &amp; logo <span className="text-xs font-normal text-slate-400">· DOC_SETTINGS</span></div>
              <F label="Business name"><input className={inputCls} value={f.businessName} onChange={(e) => set('businessName', e.target.value)} placeholder="Built4U POS" /></F>
              <F label="Address"><input className={inputCls} value={f.addressLine} onChange={(e) => set('addressLine', e.target.value)} /></F>
              <div className="grid grid-cols-2 gap-3">
                <F label="Contact"><input className={inputCls} value={f.contactLine} onChange={(e) => set('contactLine', e.target.value)} /></F>
                <F label="TIN"><input className={inputCls} value={f.tin} onChange={(e) => set('tin', e.target.value)} /></F>
              </div>
              <div className="flex items-center gap-4">
                <div className="flex h-16 w-32 items-center justify-center overflow-hidden rounded border border-dashed border-slate-300 bg-slate-50">
                  {logoUrl ? <img src={logoUrl} alt="Logo" className="max-h-16 max-w-32 object-contain" /> : <span className="text-[11px] text-slate-400">No logo</span>}
                </div>
                <div className="flex flex-col gap-2">
                  <label className={`${btnGhost} cursor-pointer`}>
                    <Upload size={14} /> {hasLogo ? 'Replace' : 'Upload'} logo
                    <input type="file" accept="image/png,image/jpeg" className="hidden" onChange={onLogoPick} />
                  </label>
                  {hasLogo && <button className={btnGhost} onClick={removeLogo}><Trash2 size={14} /> Remove</button>}
                  <span className="text-[11px] text-slate-400">PNG or JPEG, ≤512 KB.</span>
                </div>
                <Sel label="Logo position (PDF)" value={f.logoPosition} onChange={(v) => set('logoPosition', v)} options={['LEFT', 'CENTER', 'RIGHT']} />
              </div>
              <SaveBtn section="identity" onClick={saveIdentity} />
            </section>
          )}

          {canPdf && (
            <section className="space-y-3 rounded-lg border border-slate-200 bg-white p-4">
              <div className="text-sm font-medium text-slate-700">Report PDF layout <span className="text-xs font-normal text-slate-400">· PDF_CONFIG</span></div>
              <div className="grid grid-cols-2 gap-3">
                <F label="Report footer note"><input className={inputCls} value={f.footerNote} onChange={(e) => set('footerNote', e.target.value)} /></F>
                <F label="Accent colour">
                  <div className="flex items-center gap-2">
                    <input type="color" className="h-9 w-12 rounded border border-slate-300" value={f.accentColor} onChange={(e) => set('accentColor', e.target.value)} />
                    <input className={inputCls} value={f.accentColor} onChange={(e) => set('accentColor', e.target.value)} />
                  </div>
                </F>
                <Sel label="Paper size" value={f.paperSize} onChange={(v) => set('paperSize', v)} options={['A4', 'LETTER']} />
                <Sel label="Orientation" value={f.orientation} onChange={(v) => set('orientation', v)} options={['LANDSCAPE', 'PORTRAIT']} />
                <Sel label="Margins" value={f.marginPreset} onChange={(v) => set('marginPreset', v)} options={['NARROW', 'NORMAL', 'WIDE']} />
                <Sel label="Font scale" value={f.fontScale} onChange={(v) => set('fontScale', v)} options={['SMALL', 'NORMAL', 'LARGE']} />
              </div>
              <div className="grid grid-cols-2 gap-1.5">
                <Chk label="Show logo on report PDFs" checked={f.showLogoPdf} onChange={(v) => set('showLogoPdf', v)} />
                <Chk label="Zebra-striped rows" checked={f.zebraStriping} onChange={(v) => set('zebraStriping', v)} />
                <Chk label="Page numbers" checked={f.showPageNumbers} onChange={(v) => set('showPageNumbers', v)} />
                <Chk label="Timestamp footer" checked={f.showTimestamp} onChange={(v) => set('showTimestamp', v)} />
                <Chk label="“Printed by” footer" checked={f.showPrintedBy} onChange={(v) => set('showPrintedBy', v)} />
              </div>
              <SaveBtn section="pdf" onClick={savePdf} />
            </section>
          )}

          {canReceipt && (
            <section className="space-y-3 rounded-lg border border-slate-200 bg-white p-4">
              <div className="text-sm font-medium text-slate-700">Sale receipt <span className="text-xs font-normal text-slate-400">· RECEIPT_CONFIG</span></div>
              <div className="grid grid-cols-2 gap-3">
                <F label="Receipt title"><input className={inputCls} value={f.receiptTitle} onChange={(e) => set('receiptTitle', e.target.value)} /></F>
                <F label="Receipt footer"><input className={inputCls} value={f.receiptFooter} onChange={(e) => set('receiptFooter', e.target.value)} /></F>
              </div>
              <F label="Header note (under title)"><input className={inputCls} value={f.receiptHeaderNote} onChange={(e) => set('receiptHeaderNote', e.target.value)} placeholder="e.g. Official Receipt" /></F>
              <div className="grid grid-cols-2 gap-3">
                <Sel label="Physical format" value={f.receiptFormat} onChange={(v) => set('receiptFormat', v)} options={['THERMAL_80MM', 'BOND_LETTER']} />
                <div className="flex flex-col justify-end gap-1.5 pb-1">
                  <Chk label="Show logo on receipts" checked={f.showLogoReceipt} onChange={(v) => set('showLogoReceipt', v)} />
                  <Chk label="Show cashier" checked={f.receiptShowCashier} onChange={(v) => set('receiptShowCashier', v)} />
                  <Chk label="Show customer" checked={f.receiptShowCustomer} onChange={(v) => set('receiptShowCustomer', v)} />
                  <Chk label="Show voucher" checked={f.receiptShowVoucher} onChange={(v) => set('receiptShowVoucher', v)} />
                </div>
              </div>
              <SaveBtn section="receipt" onClick={saveReceipt} />
            </section>
          )}

          {!canIdentity && !canPdf && !canReceipt && (
            <p className="rounded-lg border border-slate-200 bg-white p-6 text-sm text-slate-500">You don't have access to any document settings section.</p>
          )}
        </div>
      )}
    </div>
  )
}

function F({ label, children }: { label: string; children: ReactNode }) {
  return <div><label className="mb-1 block text-sm font-medium text-slate-700">{label}</label>{children}</div>
}

function Sel({ label, value, onChange, options }: { label: string; value: string; onChange: (v: string) => void; options: string[] }) {
  return (
    <F label={label}>
      <select className={inputCls} value={value} onChange={(e) => onChange(e.target.value)}>
        {options.map((o) => <option key={o} value={o}>{o.charAt(0) + o.slice(1).toLowerCase().replace('_', ' ')}</option>)}
      </select>
    </F>
  )
}

function Chk({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <label className="flex items-center gap-2 text-[13px] text-slate-600">
      <input type="checkbox" className="h-4 w-4 rounded border-slate-300" checked={checked} onChange={(e) => onChange(e.target.checked)} />
      {label}
    </label>
  )
}
