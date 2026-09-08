import JSEncrypt from 'jsencrypt'
import request from '@/utils/request'

let cachedPublicKey: string | null = null

/** 获取服务端 RSA 公钥（带内存缓存） */
export async function getPublicKey(forceRefresh = false): Promise<string> {
  if (!forceRefresh && cachedPublicKey) {
    return cachedPublicKey
  }
  const res: any = await request.get('/login/publicKey')
  const key = res.publicKey || res.data?.publicKey
  if (!key) {
    throw new Error('获取公钥失败')
  }
  cachedPublicKey = key
  return key
}

/** 使用 RSA 公钥加密密码（传输用） */
export async function encryptPassword(plain: string): Promise<string> {
  if (!plain) {
    return plain
  }
  const publicKey = await getPublicKey()
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(publicKey)
  const cipher = encryptor.encrypt(plain)
  if (!cipher) {
    // 公钥可能过期（服务端重启后密钥变化），刷新一次再试
    const freshKey = await getPublicKey(true)
    encryptor.setPublicKey(freshKey)
    const retry = encryptor.encrypt(plain)
    if (!retry) {
      throw new Error('密码加密失败，请刷新页面后重试')
    }
    return retry
  }
  return cipher
}

export function clearPublicKeyCache() {
  cachedPublicKey = null
}
