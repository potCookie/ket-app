import api from './index.js'

/** Get current user's plan config */
export function getPlanConfig() {
  return api.get('/plan')
}

/** Save or update plan config */
export function savePlanConfig(config) {
  return api.post('/plan', config)
}
