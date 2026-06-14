import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getTodayTask, getTaskDetail, getTaskByDate } from '../api/task.js'
import { startModule, finishModule, submitQuiz, checkIn, getTodayLogs, uploadRecording, getTodayRecordings, getStudyLogs } from '../api/study.js'
import { getStats, getBadges } from '../api/stats.js'

export const useTaskStore = defineStore('task', () => {
  // --- Learning flow state ---
  const currentStep = ref(null)
  const completedSteps = ref([])
  const stepOrder = ['vocab', 'reading', 'listening', 'speaking', 'grammar']
  const reviewMode = ref(false)
  const studyLogs = ref([])       // today's study logs with quiz answers
  const todayRecordings = ref([]) // today's speaking recordings

  const stepLabels = {
    vocab: '词汇',
    reading: '阅读',
    listening: '听力',
    speaking: '口语',
    grammar: '语法',
  }

  const stepIcons = {
    vocab: '📚',
    reading: '📖',
    listening: '🎧',
    speaking: '🗣️',
    grammar: '✏️',
  }

  // --- Data state (from API) ---
  const todayData = ref(null)        // Full task content
  const todayOverview = ref(null)    // TodayTaskResponse (modules status)
  const taskLoading = ref(false)
  const taskError = ref(null)
  const stats = ref(null)
  const badges = ref([])
  const statsLoading = ref(false)

  // --- Computed ---
  const currentStepIndex = computed(() => {
    if (!currentStep.value) return -1
    return stepOrder.indexOf(currentStep.value)
  })

  const isStepDone = (step) => completedSteps.value.includes(step)

  const todayAllDone = computed(() =>
    stepOrder.length === 5 && completedSteps.value.length >= 5
  )

  const allStepsDone = computed(() => {
    return stepOrder.every(s => completedSteps.value.includes(s))
  })

  // --- Helpers ---
  function parseJsonField(data, field) {
    if (!data) return null
    const raw = data[field]
    if (!raw) return null
    if (typeof raw === 'object') return raw
    try {
      return JSON.parse(raw)
    } catch {
      return raw
    }
  }

  // --- API actions ---
  async function fetchTodayTask() {
    taskLoading.value = true
    taskError.value = null
    try {
      // 1. Get today overview (modules status)
      const overview = await getTodayTask()
      todayOverview.value = overview

      // Sync completedSteps from backend module status
      if (overview.modules) {
        completedSteps.value = overview.modules
          .filter(m => m.completed)
          .map(m => m.module)
      }

      // 2. Get full task content (vocab, grammar, reading, etc.)
      if (overview.taskId) {
        const detail = await getTaskDetail(overview.taskId)

        // Parse JSON fields from backend (stored as strings)
        todayData.value = {
          taskId: detail.id,
          date: detail.taskDate,
          week: detail.week,
          day: detail.day,
          weekday: detail.weekday,
          theme: detail.theme,
          duration: detail.duration,
          parentNote: detail.parentNote,
          vocab: parseJsonField(detail, 'vocabData'),
          grammar: parseJsonField(detail, 'grammarData'),
          reading: parseJsonField(detail, 'readingData'),
          listening: parseJsonField(detail, 'listeningData'),
          speaking: parseJsonField(detail, 'speakingData'),
          writing: parseJsonField(detail, 'writingData'),
          parent_note: detail.parentNote,
        }
      }
    } catch (e) {
      taskError.value = e.message
    } finally {
      taskLoading.value = false
    }
  }

  /** Load task + study logs for a specific date (for history review) */
  async function fetchTaskByDate(dateStr) {
    taskLoading.value = true
    taskError.value = null
    reviewMode.value = true
    completedSteps.value = []
    todayRecordings.value = []

    try {
      // 1. Get task summary for this date
      const overview = await getTaskByDate(dateStr)
      todayOverview.value = overview

      // 2. Get full task content
      if (overview.taskId) {
        const detail = await getTaskDetail(overview.taskId)
        todayData.value = {
          taskId: detail.id,
          date: detail.taskDate,
          week: detail.week,
          day: detail.day,
          weekday: detail.weekday,
          theme: detail.theme,
          duration: detail.duration,
          parentNote: detail.parentNote,
          vocab: parseJsonField(detail, 'vocabData'),
          grammar: parseJsonField(detail, 'grammarData'),
          reading: parseJsonField(detail, 'readingData'),
          listening: parseJsonField(detail, 'listeningData'),
          speaking: parseJsonField(detail, 'speakingData'),
          writing: parseJsonField(detail, 'writingData'),
          parent_note: detail.parentNote,
        }
      }

      // 3. Load study logs to get previous answers
      const logs = await getStudyLogs(dateStr)
      studyLogs.value = logs || []

      // 4. Load recordings for this date
      try {
        todayRecordings.value = await getTodayRecordings(dateStr) || []
      } catch {
        todayRecordings.value = []
      }
    } catch (e) {
      taskError.value = e.message
    } finally {
      taskLoading.value = false
    }
  }

  async function fetchStats() {
    statsLoading.value = true
    try {
      const [statsData, badgesData] = await Promise.all([
        getStats(),
        getBadges()
      ])
      stats.value = statsData
      badges.value = badgesData
    } finally {
      statsLoading.value = false
    }
  }

  // --- Study actions ---
  async function startModuleApi(moduleName) {
    if (!todayData.value) return
    await startModule(todayData.value.date, moduleName)
  }

  async function finishModuleApi(moduleName, quizScore = null, quizTotal = null, answers = null) {
    if (!todayData.value) return
    await finishModule(todayData.value.date, moduleName, quizScore, quizTotal, 1, answers)
    completeStep(moduleName)
  }

  async function submitQuizApi(moduleName, score, total, answers = null) {
    if (!todayData.value) return
    return await submitQuiz(todayData.value.date, moduleName, score, total, answers)
  }

  async function fetchStudyLogs() {
    try {
      const logs = await getTodayLogs()
      studyLogs.value = logs
    } catch {
      studyLogs.value = []
    }
  }

  /** Get previous answers for a module from study logs */
  function getModuleAnswers(moduleName) {
    const log = studyLogs.value.find(l => l.module === moduleName)
    if (!log || !log.quizAnswers) return null
    try {
      return JSON.parse(log.quizAnswers)
    } catch {
      return null
    }
  }

  /** Upload a speaking recording blob */
  async function doUploadRecording(blob) {
    if (!todayData.value) throw new Error('无今日数据')
    const result = await uploadRecording(todayData.value.date, blob)
    // result is the URL string from interceptor unwrap
    if (!result) throw new Error('上传返回空')
    return typeof result === 'string' ? result : (result.fileUrl || JSON.stringify(result))
  }

  /** Fetch today's recordings */
  async function fetchRecordings() {
    try {
      const date = todayData.value?.date || null
      const recs = await getTodayRecordings(date)
      todayRecordings.value = recs || []
    } catch {
      todayRecordings.value = []
    }
  }

  async function doCheckIn() {
    if (!todayData.value) return
    return await checkIn(todayData.value.date)
  }

  // --- Step management ---
  function setCurrentStep(step) {
    currentStep.value = step
  }

  function completeStep(step) {
    if (!completedSteps.value.includes(step)) {
      completedSteps.value.push(step)
    }
  }

  function goToNextStep() {
    const idx = stepOrder.indexOf(currentStep.value)
    completeStep(currentStep.value)
    if (idx < stepOrder.length - 1) {
      currentStep.value = stepOrder[idx + 1]
    } else {
      currentStep.value = null
    }
  }

  function goToPrevStep() {
    const idx = stepOrder.indexOf(currentStep.value)
    if (idx > 0) {
      currentStep.value = stepOrder[idx - 1]
    }
  }

  function resetLearn() {
    currentStep.value = null
    completedSteps.value = []
    reviewMode.value = false
  }

  return {
    currentStep,
    completedSteps,
    stepOrder,
    stepLabels,
    stepIcons,
    reviewMode,
    todayAllDone,
    studyLogs,
    todayRecordings,
    todayData,
    todayOverview,
    taskLoading,
    taskError,
    stats,
    badges,
    statsLoading,
    currentStepIndex,
    isStepDone,
    allStepsDone,
    fetchTodayTask,
    fetchTaskByDate,
    fetchStats,
    startModuleApi,
    finishModuleApi,
    submitQuizApi,
    doCheckIn,
    fetchStudyLogs,
    getModuleAnswers,
    doUploadRecording,
    fetchRecordings,
    setCurrentStep,
    completeStep,
    goToNextStep,
    goToPrevStep,
    resetLearn,
  }
})
