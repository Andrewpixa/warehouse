import request, { BASE_URL } from '@/utils/request'
import type { ResultObj } from '@/types/api'
import { encryptPassword } from '@/utils/crypto'

export async function login(data: { loginname: string; pwd: string; code: string }): Promise<ResultObj> {
  const encryptedPwd = await encryptPassword(data.pwd)
  return request.post('/login/login', {
    loginname: data.loginname,
    pwd: encryptedPwd,
    code: data.code
  })
}

export function getPublicKey() {
  return request.get('/login/publicKey')
}

export function getCodeUrl(): string {
  return BASE_URL + '/login/getCode?t=' + Date.now()
}

export function currentUser(): Promise<ResultObj> {
  return request.get('/login/currentUser')
}

export function logout(): Promise<ResultObj> {
  return request.get('/login/logout')
}
