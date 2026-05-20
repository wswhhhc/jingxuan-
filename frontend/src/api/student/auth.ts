import request from '../request'

export interface LoginForm {
  username: string
  password: string
  remember?: boolean
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  roleId: number
  roleCode: string
  roleName: string
  className: string
  classId?: number
  avatar?: string
  email?: string
  phone?: string
  firstLogin?: boolean
}

export function login(data: LoginForm) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: {
      username: data.username,
      password: data.password,
      rememberMe: data.remember,
    },
  })
}

export function getUserInfo() {
  return request({
    url: '/auth/user-info',
    method: 'get',
  })
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request({
    url: '/auth/password',
    method: 'put',
    data,
  })
}

export function updateProfile(data: Partial<UserInfo>) {
  return request({
    url: '/auth/profile',
    method: 'put',
    data,
  })
}
