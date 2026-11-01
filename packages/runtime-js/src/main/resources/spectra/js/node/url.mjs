// node:url
export const URL = globalThis.URL;
export const URLSearchParams = globalThis.URLSearchParams;
export const fileURLToPath = (url) => decodeURIComponent((typeof url === 'string' ? new URL(url) : url).pathname);
export const pathToFileURL = (path) => new URL(`file://${encodeURI(path.startsWith('/') ? path : `${globalThis.process.cwd()}/${path}`)}`);
export default { URL, URLSearchParams, fileURLToPath, pathToFileURL };
