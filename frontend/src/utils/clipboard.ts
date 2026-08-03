// navigator.clipboard requires a secure context (https or localhost) and can
// still reject (e.g. missing permission, insecure iframe) — callers get a
// boolean back instead of a thrown error so they can show a fallback UI.
export async function copyToClipboard(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    return false
  }
}
