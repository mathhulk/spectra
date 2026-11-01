// node:fs. Paths are relative to the server directory (process.cwd()). Async variants keep Node's callback and
// promise contracts but run on the server thread after the current task.
import { Buffer } from 'node:buffer';
const N = globalThis[Symbol.for('spectra.native')];
const { fromHost, toHost } = globalThis[Symbol.for('spectra.bytes')];
const asPath = (p) => (p instanceof URL ? decodeURIComponent(p.pathname) : String(p));
const encodingOf = (options) => (typeof options === 'string' ? options : options?.encoding);
const wrap = (fn) => (...args) => {
  try { return fn(...args); } catch (error) {
    const message = String(error?.message ?? error);
    const code = /NoSuchFile|not found/i.test(message) ? 'ENOENT' : /FileAlreadyExists/i.test(message) ? 'EEXIST'
      : /DirectoryNotEmpty/i.test(message) ? 'ENOTEMPTY' : /AccessDenied/i.test(message) ? 'EACCES' : 'EIO';
    throw Object.assign(new Error(`${code}: ${message}`), { code });
  }
};
class Stats {
  constructor(raw) {
    this.size = Number(raw.get('size'));
    this.mtimeMs = raw.get('mtimeMs'); this.atimeMs = raw.get('atimeMs'); this.birthtimeMs = raw.get('birthtimeMs');
    this.mtime = new Date(this.mtimeMs); this.atime = new Date(this.atimeMs); this.birthtime = new Date(this.birthtimeMs);
    this._file = raw.get('isFile'); this._dir = raw.get('isDirectory'); this._link = raw.get('isSymbolicLink');
  }
  isFile() { return this._file; } isDirectory() { return this._dir; } isSymbolicLink() { return this._link; }
}
const data = (value, options) => (typeof value === 'string' ? value : toHost(value));

export const readFileSync = wrap((path, options) => {
  const bytes = Buffer.from(fromHost(N.buffer(N.readFile(asPath(path)))));
  const encoding = encodingOf(options);
  return encoding ? bytes.toString(encoding) : bytes;
});
export const writeFileSync = wrap((path, value, options) => N.writeFile(asPath(path), data(value, options), false));
export const appendFileSync = wrap((path, value, options) => N.writeFile(asPath(path), data(value, options), true));
export const existsSync = (path) => N.exists(asPath(path));
export const mkdirSync = wrap((path, options) => N.mkdir(asPath(path), !!options?.recursive));
export const readdirSync = wrap((path, options) => {
  const names = [...N.readdir(asPath(path))];
  if (!options?.withFileTypes) return names;
  return names.map((name) => {
    const stats = statSync(`${asPath(path)}/${name}`);
    return { name, isFile: () => stats.isFile(), isDirectory: () => stats.isDirectory(), isSymbolicLink: () => stats.isSymbolicLink() };
  });
});
export const statSync = wrap((path) => new Stats(N.stat(asPath(path), true)));
export const lstatSync = wrap((path) => new Stats(N.stat(asPath(path), false)));
export const rmSync = wrap((path, options) => N.rm(asPath(path), !!options?.recursive, !!options?.force));
export const unlinkSync = wrap((path) => N.rm(asPath(path), false, false));
export const rmdirSync = wrap((path, options) => N.rm(asPath(path), !!options?.recursive, false));
export const renameSync = wrap((from, to) => N.rename(asPath(from), asPath(to)));
export const copyFileSync = wrap((from, to) => N.copyFile(asPath(from), asPath(to)));
export const realpathSync = wrap((path) => N.realpath(asPath(path)));

const later = (fn) => (...args) => new Promise((resolve, reject) => setImmediate(() => { try { resolve(fn(...args)); } catch (e) { reject(e); } }));
export const promises = {
  readFile: later(readFileSync), writeFile: later(writeFileSync), appendFile: later(appendFileSync),
  mkdir: later(mkdirSync), readdir: later(readdirSync), stat: later(statSync), lstat: later(lstatSync),
  rm: later(rmSync), unlink: later(unlinkSync), rmdir: later(rmdirSync), rename: later(renameSync),
  copyFile: later(copyFileSync), realpath: later(realpathSync),
  access: later((path) => { if (!existsSync(path)) throw Object.assign(new Error(`ENOENT: ${path}`), { code: 'ENOENT' }); }),
};
const callback = (fn) => (...args) => {
  const done = typeof args[args.length - 1] === 'function' ? args.pop() : () => {};
  promises[fn](...args).then((value) => done(null, value), (error) => done(error));
};
export const readFile = callback('readFile');
export const writeFile = callback('writeFile');
export const appendFile = callback('appendFile');
export const mkdir = callback('mkdir');
export const readdir = callback('readdir');
export const stat = callback('stat');
export const rm = callback('rm');
export const unlink = callback('unlink');
export const rename = callback('rename');
export const constants = { F_OK: 0, R_OK: 4, W_OK: 2, X_OK: 1 };
export default {
  readFileSync, writeFileSync, appendFileSync, existsSync, mkdirSync, readdirSync, statSync, lstatSync, rmSync, unlinkSync,
  rmdirSync, renameSync, copyFileSync, realpathSync, promises, readFile, writeFile, appendFile, mkdir, readdir, stat, rm, unlink,
  rename, constants,
};
