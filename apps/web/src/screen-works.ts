import { getPublicEntry, type Entry } from '@hyoban/data-spec/entry'

/** Only public film and television records are rendered on the website. */
export function publicScreenWorks(entries: Entry[]) {
  return entries
    .flatMap((entry) => {
      const visible = getPublicEntry(entry)
      const work = visible?.parts.screenWork
      return visible && work ? [{ ...work, uid: visible.uid, date: visible.at }] : []
    })
    .sort((a, b) => b.date.localeCompare(a.date))
}
