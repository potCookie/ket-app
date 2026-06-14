import api from './index.js'

/** Get children linked to current parent */
export function getChildren() {
  return api.get('/parent/children')
}

/** Get child's learning report (stats) */
export function getChildReport(childId) {
  return api.get('/parent/report', { params: { childId } })
}

/** Get child's study logs (optional from/to date filter) */
export function getChildRecords(childId, from, to) {
  return api.get('/parent/records', { params: { childId, from, to } })
}

/** Parent manually marks a day as checked-in */
export function manualCheckIn(childId, date) {
  return api.post('/parent/checkin', null, { params: { childId, date } })
}
