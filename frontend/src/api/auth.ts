import request from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
  nickname?: string
}

export interface LoginResponse {
  token: string
  refreshToken: string
  userId: number
  username: string
  nickname: string
  avatar: string
  email: string
  role: string
}

export function loginApi(data: LoginRequest): Promise<LoginResponse> {
  return request.post('/api/auth/login', data) as Promise<LoginResponse>
}

export function registerApi(data: RegisterRequest): Promise<void> {
  return request.post('/api/auth/register', data) as Promise<void>
}

export function verifyEmailApi(data: { email: string; code: string }): Promise<void> {
  return request.post('/api/auth/verify-email', data) as Promise<void>
}

export function getWechatQrcodeApi(): Promise<{ authUrl: string; state: string }> {
  return request.get('/api/auth/wechat/qrcode') as Promise<{ authUrl: string; state: string }>
}

export function wechatCheckApi(state: string): Promise<{ success: boolean; token?: string }> {
  return request.get('/api/auth/wechat/check', { params: { state } }) as Promise<{ success: boolean; token?: string }>
}

export function getCurrentUserApi(): Promise<LoginResponse> {
  return request.get('/api/user/me', { _skipAuthRedirect: true } as any) as Promise<LoginResponse>
}

export function changePasswordApi(data: { oldPassword: string; newPassword: string }): Promise<void> {
  return request.put('/api/user/change-password', data) as any
}
