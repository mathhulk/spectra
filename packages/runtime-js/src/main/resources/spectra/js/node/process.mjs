// node:process
const process = globalThis.process;
export const { env, argv, platform, arch, pid, version, versions, cwd, nextTick, hrtime, uptime, memoryUsage, exit } = process;
export default process;
