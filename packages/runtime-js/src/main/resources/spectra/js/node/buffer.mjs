// node:buffer (a Uint8Array subclass covering the commonly used API)
const encoder = new TextEncoder();
const decoder = new TextDecoder();
const toHex = (bytes) => Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
export class Buffer extends Uint8Array {
  static from(value, encodingOrOffset, length) {
    if (typeof value === 'string') {
      const encoding = (encodingOrOffset ?? 'utf8').toLowerCase();
      if (encoding === 'hex') return new Buffer((value.match(/.{1,2}/g) ?? []).map((h) => parseInt(h, 16)));
      if (encoding === 'base64' || encoding === 'base64url') {
        const binary = atob(value.replace(/-/g, '+').replace(/_/g, '/'));
        return Buffer.from(Array.from(binary, (c) => c.charCodeAt(0)));
      }
      if (encoding === 'latin1' || encoding === 'binary' || encoding === 'ascii') return Buffer.from(Array.from(value, (c) => c.charCodeAt(0) & 255));
      const bytes = encoder.encode(value);
      return new Buffer(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    }
    if (value instanceof ArrayBuffer) return new Buffer(value, encodingOrOffset ?? 0, length ?? value.byteLength - (encodingOrOffset ?? 0));
    const out = new Buffer(value.length ?? 0);
    out.set(value);
    return out;
  }
  static alloc(size, fill = 0) { return new Buffer(size).fill(typeof fill === 'string' ? fill.charCodeAt(0) : fill); }
  static allocUnsafe(size) { return new Buffer(size); }
  static isBuffer(value) { return value instanceof Buffer; }
  static byteLength(value, encoding) { return typeof value === 'string' ? Buffer.from(value, encoding).length : value.byteLength; }
  static concat(list, total) {
    const length = total ?? list.reduce((n, b) => n + b.length, 0);
    const out = Buffer.alloc(length);
    let offset = 0;
    for (const b of list) { out.set(b.subarray(0, Math.max(0, length - offset)), offset); offset += b.length; if (offset >= length) break; }
    return out;
  }
  toString(encoding = 'utf8', start = 0, end = this.length) {
    const bytes = this.subarray(start, end);
    switch (encoding.toLowerCase()) {
      case 'hex': return toHex(bytes);
      case 'base64': return btoa(String.fromCharCode(...bytes));
      case 'base64url': return btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
      case 'latin1': case 'binary': case 'ascii': return String.fromCharCode(...bytes);
      default: return decoder.decode(bytes);
    }
  }
  toJSON() { return { type: 'Buffer', data: [...this] }; }
  equals(other) { return this.length === other.length && this.every((b, i) => b === other[i]); }
  write(string, offset = 0) { const bytes = Buffer.from(string); this.set(bytes.subarray(0, this.length - offset), offset); return Math.min(bytes.length, this.length - offset); }
  slice(start, end) { return this.subarray(start, end); }
  subarray(start, end) { const sub = super.subarray(start, end); return new Buffer(sub.buffer, sub.byteOffset, sub.byteLength); }
  readUInt8(o = 0) { return this[o]; }
  readUInt16BE(o = 0) { return (this[o] << 8) | this[o + 1]; }
  readUInt32BE(o = 0) { return new DataView(this.buffer, this.byteOffset).getUint32(o); }
  readInt32BE(o = 0) { return new DataView(this.buffer, this.byteOffset).getInt32(o); }
  writeUInt8(v, o = 0) { this[o] = v; return o + 1; }
  writeUInt32BE(v, o = 0) { new DataView(this.buffer, this.byteOffset).setUint32(o, v); return o + 4; }
}
if (!globalThis.Buffer) Object.defineProperty(globalThis, 'Buffer', { value: Buffer, writable: true, configurable: true });
export default { Buffer };
