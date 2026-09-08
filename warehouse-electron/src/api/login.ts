import request, { TOKEN_KEY, USER_KEY } from '../utils/request'
import { PERM_KEY } from '../utils/perm'
import { encryptPassword } from '../utils/crypto'

export function getCaptchaBase64() {
  return request.get('/login/getCaptchaBase64')
}

export async function login(data: { loginname: string; pwd: string; code: string; captchaId: string }) {
  const encryptedPwd = await encryptPassword(data.pwd)
  return request.post('/login/login', {
    loginname: data.loginname,
    pwd: encryptedPwd,
    code: data.code,
    captchaId: data.captchaId
  })
}

export function currentUser() {
  return request.get('/login/currentUser')
}

export function logout() {
  return request.get('/login/logout').finally(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(PERM_KEY)
  })
}
