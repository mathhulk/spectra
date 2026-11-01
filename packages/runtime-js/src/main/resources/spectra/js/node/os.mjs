// node:os
const N = globalThis[Symbol.for('spectra.native')];
const info = () => N.os();
export const EOL = info().get('platform') === 'win32' ? '\r\n' : '\n';
export const platform = () => info().get('platform');
export const arch = () => info().get('arch');
export const type = () => info().get('type');
export const release = () => info().get('release');
export const hostname = () => info().get('hostname');
export const homedir = () => info().get('homedir');
export const tmpdir = () => info().get('tmpdir');
export const totalmem = () => Number(info().get('totalmem'));
export const freemem = () => Number(info().get('freemem'));
export const uptime = () => Number(info().get('uptime'));
export const cpus = () => Array.from({ length: Number(info().get('cpus')) }, () => ({ model: 'unknown', speed: 0, times: {} }));
export const availableParallelism = () => Number(info().get('cpus'));
export const userInfo = () => ({ username: info().get('username'), homedir: homedir(), shell: null, uid: -1, gid: -1 });
export default { EOL, platform, arch, type, release, hostname, homedir, tmpdir, totalmem, freemem, uptime, cpus, availableParallelism, userInfo };
