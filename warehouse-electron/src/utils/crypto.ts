import JSEncrypt from 'jsencrypt'
import request from './request'

let cachedPublicKey: string | null = null

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

export async function encryptPassword(plain: string): Promise<string> {
  if (!plain) return plain
  const publicKey = await getPublicKey()
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(publicKey)
  const cipher = encryptor.encrypt(plain)
  if (!cipher) {
    const freshKey = await getPublicKey(true)
    encryptor.setPublicKey(freshKey)
    const retry = encryptor.encrypt(plain)
    if (!retry) {
      throw new Error('密码加密失败，请重试')
    }
    return retry
  }
  return cipher
}
