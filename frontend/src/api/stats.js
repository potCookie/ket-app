import api from './index.js'

export function getStats() {
  return api.get('/stats')
}

export function getBadges() {
  return api.get('/badges')
}

export function getChildren() {
  return api.get('/parent/children')
}

export function getChildReport(childId) {
  return api.get('/parent/report', { params: { childId } })
}

export function getChildRecords(childId) {
  return api.get('/parent/records', { params: { childId } })
}

export function parentCheckIn(childId, taskDate) {
  return api.post('/parent/checkin', { childId, taskDate })
}
