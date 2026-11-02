// node:fs/promises
import fs from 'node:fs';
export const { readFile, writeFile, appendFile, mkdir, readdir, stat, lstat, rm, unlink, rmdir, rename, copyFile, realpath, access } = fs.promises;
export default fs.promises;
