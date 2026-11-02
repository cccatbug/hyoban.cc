import { describe, expect, it } from 'vite-plus/test'
import { entrySchema } from '@hyoban/data-spec/entry'
import { publicScreenWorks } from './screen-works'

const entry = (visibility: 'public' | 'private', rating = 4.5) =>
  entrySchema.parse({
    uid: '01ARZ3NDEKTSV4RRFFQ69G5FAV',
    at: '2026-01-01',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    visibility,
    parts: { screenWork: { v: 1, title: 'Test film', kind: 'movie', rating, note: 'A review' } },
  })

describe('film records', () => {
  it('shows public records while keeping private records off the website', () => {
    expect(publicScreenWorks([entry('public'), entry('private')])).toMatchObject([
      { title: 'Test film', kind: 'movie', rating: 4.5, note: 'A review' },
    ])
  })
  it('rejects ratings outside the supported range', () => {
    expect(() => entry('public', 6)).toThrow()
  })
})
