import api from './index.js'

export function login(username, password) {
  return api.post('/auth/login', { username, password })
}

export function register(data) {
  return api.post('/auth/register', data)
}

export function getMe() {
  return api.get('/auth/me')
}

export function uploadAvatar(file) {
  const form = new FormData()
  form.append('file', file, 'avatar' + file.name.substring(file.name.lastIndexOf('.')))
  return api.post('/auth/avatar', form)
}
