// 供多个视图复用的展示工具

export function diffColor(d) {
  return d === 1 ? 'text-emerald-600 bg-emerald-50'
    : d === 2 ? 'text-amber-600 bg-amber-50'
    : 'text-rose-600 bg-rose-50'
}
