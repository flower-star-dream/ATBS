// @ts-ignore
import * as CryptoJS from 'crypto-js';

/**
 * 使用MD5算法对输入的字符串进行加密
 * @param str 需要进行MD5加密的字符串
 * @returns 返回加密后的十六进制字符串
 */
export function md5ToHex(str: string): string { // 导出md5函数，接收字符串参数，返回字符串
  return CryptoJS.MD5(str).toString(CryptoJS.enc.Hex);
}

/** 把 hex 字符串再按字节转 hex（即每个字符的 ASCII 再转 hex） */
export function hexOfHex(str: string): string {
  return Array.from(str).map(ch => ch.charCodeAt(0).toString(16).padStart(2, '0')).join('');
}

export function md5TwoHex(str: string): string {
  return hexOfHex(md5ToHex(str));
}