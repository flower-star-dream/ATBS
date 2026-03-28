/**
 * 先尝试远程地址，失败再返回本地地址
 * @param {string} remotePath  远程绝对路径，如 https://xxx/assets/atbs/logo.png
 * @param {string} localFile   本地相对路径，如 @/assets/atbs/logo.png
 * @returns {Promise<string>}  最终可用的图片地址
 */
export function loadAsset(remotePath: string, localFile: string): Promise<string> {
  return new Promise((resolve) => {
    const img = new Image();
    img.src = remotePath;
    img.onload = () => resolve(remotePath);
    img.onerror = () => {
      // 本地文件：Vite 场景用 import.meta.url
      const localUrl = new URL(localFile, import.meta.url).href;
      resolve(localUrl);
    };
  });
}

export default loadAsset
