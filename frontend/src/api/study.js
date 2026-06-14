import api from './index.js'

export function startModule(taskDate, module) {
  return api.post('/study/start', { taskDate, module })
}

export function finishModule(taskDate, module, quizScore = null, quizTotal = null, starsEarned = 1, answers = null) {
  return api.post('/study/finish', { taskDate, module, quizScore, quizTotal, starsEarned, answers })
}

export function submitQuiz(taskDate, module, score, total, answers = null) {
  return api.post('/study/quiz/submit', { taskDate, module, score, total, answers })
}

export function checkIn(date) {
  return api.post('/checkin', null, { params: { date } })
}

export function getTodayLogs() {
  return api.get('/study/today-logs')
}

export function uploadRecording(date, file) {
  const form = new FormData()
  form.append('file', file, 'recording.webm')
  form.append('date', date)
  return api.post('/recording/upload', form)
}

export function getTodayRecordings(date) {
  return api.get('/recording/today', { params: { date } })
}

export function getStudyHistory() {
  return api.get('/study/history')
}

export function getStudyLogs(date) {
  return api.get('/study/logs', { params: { date } })
}
