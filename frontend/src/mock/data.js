// ========== Mock 数据（模拟后端 JSON 格式）==========

// 今日任务数据
export const todayTask = {
  date: '2026-06-13',
  week: 1,
  day: 1,
  weekday: '周六',
  theme: '入门启动 · 个人信息词汇',
  duration: '35分钟',
  vocab: {
    group: '个人信息',
    words: [
      { en: 'name', zh: '名字', eg: 'My name is Lily.' },
      { en: 'age', zh: '年龄', eg: 'I am eight years old.' },
      { en: 'family', zh: '家庭', eg: 'I have a happy family.' },
      { en: 'school', zh: '学校', eg: 'I go to school by bus.' },
      { en: 'teacher', zh: '老师', eg: 'My teacher is very kind.' },
      { en: 'friend', zh: '朋友', eg: 'She is my best friend.' },
      { en: 'hobby', zh: '爱好', eg: 'My hobby is reading books.' },
      { en: 'colour', zh: '颜色', eg: 'My favourite colour is blue.' },
      { en: 'birthday', zh: '生日', eg: 'My birthday is in June.' },
      { en: 'home', zh: '家', eg: 'I live in a small home.' },
    ],
  },
  grammar: {
    point: 'be动词（am/is/are）',
    explanation: '我们用 am、is、are 来描述人或事物。\n\nI → am\nHe/She/It → is\nYou/We/They → are',
    exercises: [
      'I ___ a student.',
      'She ___ my friend.',
      'They ___ happy.',
      'He ___ eight years old.',
      'We ___ in Class 2.',
    ],
    answers: ['am', 'is', 'are', 'is', 'are'],
  },
  reading: {
    passage: {
      title: 'All About Me',
      text: 'Hello! My name is Lily. I am eight years old. I live at No.5 School Road with my mum, dad and little brother. I have a pet cat. Her name is Mimi. I like playing football and reading books. My best friend is Amy. We go to the same school. I am in Class 2, Grade 2.',
      translation: '你好！我叫莉莉。我八岁了。我和妈妈、爸爸还有弟弟住在学堂路5号。我有一只宠物猫，她叫咪咪。我喜欢踢足球和看书。我最好的朋友是艾米。我们在同一个学校上学。我在二年级二班。',
    },
    questions: [
      {
        q: 'How old is Lily?',
        options: ['A. Six', 'B. Eight', 'C. Nine'],
        answer: 1,
      },
      {
        q: 'What is Lily\'s pet?',
        options: ['A. A dog', 'B. A fish', 'C. A cat'],
        answer: 2,
      },
      {
        q: 'What does Lily like doing?',
        options: ['A. Swimming', 'B. Playing football and reading', 'C. Dancing'],
        answer: 1,
      },
    ],
  },
  listening: {
    questions: [
      {
        id: 1,
        scenario: 'What time does Tom get up?',
        translation: 'Tom几点起床？',
        audio_text: 'Tom gets up at seven o\'clock every morning.',
        options: ['A. 6:30', 'B. 7:00', 'C. 7:30'],
        answer: 1,
        key_word: 'seven o\'clock',
      },
      {
        id: 2,
        scenario: 'How does Lucy go to school?',
        translation: 'Lucy怎么去上学？',
        audio_text: 'Lucy goes to school by bus every day.',
        options: ['A. By car', 'B. By bus', 'C. On foot'],
        answer: 1,
        key_word: 'by bus',
      },
      {
        id: 3,
        scenario: 'What colour is Jack\'s bag?',
        translation: 'Jack的书包是什么颜色？',
        audio_text: 'Jack has a blue school bag.',
        options: ['A. Red', 'B. Yellow', 'C. Blue'],
        answer: 2,
        key_word: 'blue',
      },
      {
        id: 4,
        scenario: 'Where is the ball?',
        translation: '球在哪里？',
        audio_text: 'The ball is under the table.',
        options: ['A. On the table', 'B. Under the table', 'C. Behind the door'],
        answer: 1,
        key_word: 'under the table',
      },
      {
        id: 5,
        scenario: 'What does Mum want to buy?',
        translation: '妈妈想买什么？',
        audio_text: 'Mum wants to buy some apples and bread.',
        options: ['A. Apples and bananas', 'B. Bread and milk', 'C. Apples and bread'],
        answer: 2,
        key_word: 'apples and bread',
      },
    ],
  },
  speaking: {
    template: 'Hello! My name is ____. I am ____ years old. I live in ____. I like ____. My favourite colour is ____.',
    phrases: [
      { en: 'My name is ...', zh: '我叫……' },
      { en: 'I am ... years old.', zh: '我……岁了。' },
      { en: 'I live in ...', zh: '我住在……' },
      { en: 'I like ...', zh: '我喜欢……' },
      { en: 'My favourite colour is ...', zh: '我最喜欢的颜色是……' },
    ],
  },
  writing: {
    prompt: "Write about yourself. Include your name, age, family and hobbies. (3-4 sentences)",
    sample: "My name is Lily. I am eight years old. I live with my mum and dad. I like playing football and reading books.",
  },
  parent_note: '今天是第一天，核心就两个事：1、学会用 am/is/are 介绍自己；2、认识常用的个人信息词汇。鼓励孩子大声朗读！',
}

// 成就统计
export const statsData = {
  stars: 12,
  completedDays: 3,
  totalDays: 35,
  accuracy: 85,
  longestStreak: 3,
  learnedWords: 30,
  badges: [
    { type: 'streak_7', name: '打卡7天', icon: '🌟', earned: true },
    { type: 'streak_30', name: '打卡30天', icon: '🔒', earned: false },
    { type: 'perfect_attendance', name: '全勤勋章', icon: '🔒', earned: false },
    { type: 'full_score', name: '满分学霸', icon: '🔒', earned: false },
  ],
}

// 用户信息
export const userInfo = {
  name: '小明',
  grade: '二升三',
  target: 'KET',
  avatar: '🐱',
  streak: 1,
}
