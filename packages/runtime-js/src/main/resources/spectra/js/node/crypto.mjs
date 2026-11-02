// node:crypto
import { Buffer } from 'node:buffer';
const N = globalThis[Symbol.for('spectra.native')];
const { fromHost, toHost } = globalThis[Symbol.for('spectra.bytes')];
const input = (data, encoding) => toHost(typeof data === 'string' ? Buffer.from(data, encoding) : data);
const output = (bytes, encoding) => { const b = Buffer.from(fromHost(N.buffer(bytes))); return encoding ? b.toString(encoding) : b; };
class Hash {
  #parts = [];
  constructor(algorithm, key) { this.algorithm = algorithm; this.key = key; }
  update(data, encoding) { this.#parts.push(typeof data === 'string' ? Buffer.from(data, encoding) : Buffer.from(data)); return this; }
  digest(encoding) {
    const all = toHost(Buffer.concat(this.#parts));
    return output(this.key === undefined ? N.digest(this.algorithm, all) : N.hmac(this.algorithm, input(this.key), all), encoding);
  }
}
export const createHash = (algorithm) => new Hash(algorithm);
export const createHmac = (algorithm, key) => new Hash(algorithm, key);
export const randomUUID = () => N.randomUUID();
export const randomBytes = (size) => output(N.randomBytes(size));
export const randomInt = (min, max) => {
  if (max === undefined) { max = min; min = 0; }
  const range = max - min;
  const value = new DataView(fromHost(N.buffer(N.randomBytes(4))).buffer).getUint32(0);
  return min + (value % range);
};
export const getRandomValues = (array) => globalThis.crypto.getRandomValues(array);
export const webcrypto = globalThis.crypto;
export const subtle = globalThis.crypto.subtle;
export default { createHash, createHmac, randomUUID, randomBytes, randomInt, getRandomValues, webcrypto, subtle };
