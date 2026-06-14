import api from './index.js'

export function getTodayTask() {
  return api.get('/task/today')
}

export function getTaskByDate(date) {
  return api.get('/task/date', { params: { date } })
}

export function getTaskDetail(id) {
  return api.get(`/task/${id}`)
}
