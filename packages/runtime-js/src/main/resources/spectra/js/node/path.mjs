// node:path (POSIX semantics, which is what scripts expect on every platform).
const cwd = () => globalThis.process.cwd();
const normalizeParts = (parts, absolute) => {
  const out = [];
  for (const part of parts) {
    if (!part || part === '.') continue;
    if (part === '..') { if (out.length && out[out.length - 1] !== '..') out.pop(); else if (!absolute) out.push('..'); }
    else out.push(part);
  }
  return out;
};
export const sep = '/';
export const delimiter = ':';
export function normalize(p) {
  if (p === '') return '.';
  const absolute = p.startsWith('/');
  const trailing = p.endsWith('/');
  let result = normalizeParts(p.split('/'), absolute).join('/');
  if (!result && !absolute) result = '.';
  if (result && trailing) result += '/';
  return (absolute ? '/' : '') + result;
}
export function join(...parts) {
  const joined = parts.filter((p) => p !== '').join('/');
  return joined ? normalize(joined) : '.';
}
export function resolve(...parts) {
  let resolved = '';
  for (let i = parts.length - 1; i >= 0 && !resolved.startsWith('/'); i--) {
    if (parts[i]) resolved = `${parts[i]}/${resolved}`;
  }
  if (!resolved.startsWith('/')) resolved = `${cwd()}/${resolved}`;
  const result = '/' + normalizeParts(resolved.split('/'), true).join('/');
  return result;
}
export function isAbsolute(p) { return p.startsWith('/'); }
export function dirname(p) {
  if (!p) return '.';
  const trimmed = p.length > 1 ? p.replace(/\/+$/, '') : p;
  const i = trimmed.lastIndexOf('/');
  if (i < 0) return '.';
  return i === 0 ? '/' : trimmed.slice(0, i);
}
export function basename(p, ext) {
  const trimmed = p.length > 1 ? p.replace(/\/+$/, '') : p;
  let base = trimmed.slice(trimmed.lastIndexOf('/') + 1);
  if (ext && base.endsWith(ext) && base !== ext) base = base.slice(0, -ext.length);
  return base;
}
export function extname(p) {
  const base = basename(p);
  const i = base.lastIndexOf('.');
  return i <= 0 ? '' : base.slice(i);
}
export function relative(from, to) {
  const a = resolve(from).split('/').filter(Boolean);
  const b = resolve(to).split('/').filter(Boolean);
  let i = 0;
  while (i < a.length && i < b.length && a[i] === b[i]) i++;
  return [...Array(a.length - i).fill('..'), ...b.slice(i)].join('/');
}
export function parse(p) {
  const root = p.startsWith('/') ? '/' : '';
  const base = basename(p);
  const ext = extname(p);
  return { root, dir: dirname(p) === '.' && !p.includes('/') ? '' : dirname(p), base, ext, name: ext ? base.slice(0, -ext.length) : base };
}
export function format(o) {
  const dir = o.dir || o.root || '';
  const base = o.base || `${o.name || ''}${o.ext || ''}`;
  return dir ? (dir.endsWith('/') ? dir + base : `${dir}/${base}`) : base;
}
export function toNamespacedPath(p) { return p; }
const path = { sep, delimiter, normalize, join, resolve, isAbsolute, dirname, basename, extname, relative, parse, format, toNamespacedPath };
path.posix = path;
path.win32 = path;
export const posix = path;
export const win32 = path;
export default path;
