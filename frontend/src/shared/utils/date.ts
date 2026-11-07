/** 日期工具：与后端契约一致，业务日期一律 YYYY-MM-DD */

const pad = (n: number) => String(n).padStart(2, '0')

/** Date → YYYY-MM-DD（本地时区） */
export function toDateKey(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** YYYY-MM-DD → Date（本地时区 00:00） */
export function parseDateKey(key: string): Date {
  const [y, m, d] = key.split('-').map(Number)
  return new Date(y, m - 1, d)
}

/** 今天（YYYY-MM-DD） */
export function todayKey(): string {
  return toDateKey(new Date())
}

/** 某年某月的天数 */
export function daysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate()
}

/** 某年某月 1 号是星期几（0=周日） */
export function firstWeekday(year: number, month: number): number {
  return new Date(year, month - 1, 1).getDay()
}

export interface CalendarCell {
  date: string | null
  day: number
  inMonth: boolean
}

/** 生成月历网格（含前后月补齐；标准 6 行） */
export function buildMonthGrid(year: number, month: number): CalendarCell[] {
  const first = firstWeekday(year, month)
  const days = daysInMonth(year, month)
  const cells: CalendarCell[] = []
  const prevDays = daysInMonth(year, month - 1)

  for (let i = first - 1; i >= 0; i--) {
    cells.push({ date: toDateKey(new Date(year, month - 2, prevDays - i)), day: prevDays - i, inMonth: false })
  }
  for (let d = 1; d <= days; d++) {
    cells.push({ date: `${year}-${pad(month)}-${pad(d)}`, day: d, inMonth: true })
  }
  let next = 1
  while (cells.length < 42) {
    cells.push({ date: toDateKey(new Date(year, month, next)), day: next, inMonth: false })
    next++
  }
  return cells
}

/** 中文月份名 */
export function monthNameCn(month: number): string {
  return `${month} 月`
}

/** 中文年份行（用于时间线大数字旁的小字） */
export function yearCaption(year: number): string {
  return `${year} 年`
}