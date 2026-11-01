// node:assert
import { isDeepStrictEqual } from 'node:util';
export class AssertionError extends Error { constructor(message) { super(message); this.name = 'AssertionError'; this.code = 'ERR_ASSERTION'; } }
const fail = (message, fallback) => { throw message instanceof Error ? message : new AssertionError(message ?? fallback); };
export default function assert(value, message) { if (!value) fail(message, 'The expression evaluated to a falsy value'); }
export const ok = assert;
export const equal = (a, b, m) => { if (a != b) fail(m, `${a} == ${b}`); };
export const strictEqual = (a, b, m) => { if (!Object.is(a, b)) fail(m, `Expected ${b}, got ${a}`); };
export const notStrictEqual = (a, b, m) => { if (Object.is(a, b)) fail(m, `Expected values to differ: ${a}`); };
export const deepStrictEqual = (a, b, m) => { if (!isDeepStrictEqual(a, b)) fail(m, 'Values are not deeply equal'); };
export const deepEqual = deepStrictEqual;
export const throws = (fn, _expected, m) => { try { fn(); } catch { return; } fail(m, 'Missing expected exception'); };
export const rejects = async (promise, _expected, m) => { try { await (typeof promise === 'function' ? promise() : promise); } catch { return; } fail(m, 'Missing expected rejection'); };
assert.ok = ok; assert.equal = equal; assert.strictEqual = strictEqual; assert.notStrictEqual = notStrictEqual;
assert.deepStrictEqual = deepStrictEqual; assert.deepEqual = deepEqual; assert.throws = throws; assert.rejects = rejects;
assert.AssertionError = AssertionError; assert.strict = assert;
