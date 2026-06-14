package com.ketapp.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * KET A2 35-day study plan curriculum.
 * Defines week themes, daily topics, vocabulary groups, and grammar points
 * following the Cambridge KET (A2 Key) syllabus.
 */
public class KetCurriculum {

    // Week themes
    private static final String[][] WEEK_THEMES = {
        {"Personal Information",    "个人信息与家庭"},
        {"Daily Life & School",     "日常生活与学校"},
        {"Food, Drink & Health",    "饮食与健康"},
        {"Hobbies, Sports & Free Time", "爱好运动与休闲"},
        {"Home, Places & Buildings","家居场所与建筑"},
        {"Shopping, Clothes & Services","购物服装与服务"},
        {"Travel, Transport & Nature","出行交通与自然"},
    };

    // Daily sub-topics [week][day]
    private static final String[][][] DAILY_TOPICS = {
        // Week 1: Personal Information
        {
            {"Greetings & Names",        "问候与名字"},
            {"Age & Birthday",           "年龄与生日"},
            {"Family Members",           "家庭成员"},
            {"Appearance & Body",        "外貌与身体"},
            {"Feelings & Emotions",      "感受与情绪"},
        },
        // Week 2: Daily Life & School
        {
            {"Morning Routine",          "早晨日常"},
            {"At School",                "在学校"},
            {"Subjects & Teachers",      "科目与老师"},
            {"After School Activities",  "课后活动"},
            {"Weekend Plans",            "周末计划"},
        },
        // Week 3: Food, Drink & Health
        {
            {"Fruits & Vegetables",      "水果蔬菜"},
            {"Meals & Cooking",          "餐食与烹饪"},
            {"At a Restaurant",          "在餐厅"},
            {"Healthy Habits",           "健康习惯"},
            {"At the Doctor's",          "看医生"},
        },
        // Week 4: Hobbies, Sports & Free Time
        {
            {"Sports & Games",           "运动与游戏"},
            {"Music & Art",              "音乐与艺术"},
            {"Reading & Books",          "阅读与书籍"},
            {"TV, Films & Internet",     "影视与网络"},
            {"Parties & Celebrations",   "聚会与庆祝"},
        },
        // Week 5: Home, Places & Buildings
        {
            {"My House & Rooms",         "家与房间"},
            {"Furniture & Things",       "家具与物品"},
            {"In the City",              "在城市里"},
            {"Asking & Giving Directions","问路指路"},
            {"Hotels & Accommodation",   "酒店与住宿"},
        },
        // Week 6: Shopping, Clothes & Services
        {
            {"Clothes & Colours",        "服装与颜色"},
            {"At the Shops",             "在商店"},
            {"Prices & Money",           "价格与金钱"},
            {"Post Office & Bank",       "邮局与银行"},
            {"Making Phone Calls",       "打电话"},
        },
        // Week 7: Travel, Transport & Nature
        {
            {"Means of Transport",       "交通方式"},
            {"At the Station / Airport", "车站与机场"},
            {"Weather & Seasons",        "天气与季节"},
            {"Animals & Pets",           "动物与宠物"},
            {"Holidays & Review",        "假期与总复习"},
        },
    };

    // Vocab groups for each day [week][day] — 10 words + examples
    // Format: {en, zh, example}
    private static final String[][][][] VOCAB = {
        // Week 1
        {
            { // Day 1
                {"name", "名字", "My name is Lily."},
                {"hello", "你好", "Hello, how are you?"},
                {"friend", "朋友", "Tom is my best friend."},
                {"boy", "男孩", "The boy is playing."},
                {"girl", "女孩", "She is a nice girl."},
                {"man", "男人", "The man is my father."},
                {"woman", "女人", "That woman is a teacher."},
                {"child", "小孩", "The child is happy."},
                {"people", "人们", "People are friendly here."},
                {"meet", "遇见", "Nice to meet you."},
            },
            { // Day 2
                {"age", "年龄", "What is your age?"},
                {"old", "老的/…岁", "I am eight years old."},
                {"year", "年", "I am seven years old this year."},
                {"birthday", "生日", "My birthday is in June."},
                {"number", "数字", "What is your phone number?"},
                {"date", "日期", "What is the date today?"},
                {"month", "月", "My birthday month is May."},
                {"today", "今天", "Today is Monday."},
                {"young", "年轻的", "My sister is very young."},
                {"first", "第一", "This is my first day at school."},
            },
            { // Day 3
                {"family", "家庭", "I have a happy family."},
                {"father", "爸爸", "My father is tall."},
                {"mother", "妈妈", "My mother is kind."},
                {"brother", "兄弟", "I have a little brother."},
                {"sister", "姐妹", "She is my sister."},
                {"parent", "父母", "My parents love me."},
                {"grandfather", "爷爷/外公", "My grandfather tells stories."},
                {"grandmother", "奶奶/外婆", "My grandmother cooks well."},
                {"baby", "婴儿", "The baby is sleeping."},
                {"pet", "宠物", "I have a pet dog."},
            },
            { // Day 4
                {"tall", "高的", "My father is tall."},
                {"short", "矮的/短的", "The boy is short."},
                {"hair", "头发", "She has long brown hair."},
                {"eye", "眼睛", "He has blue eyes."},
                {"face", "脸", "She has a round face."},
                {"nose", "鼻子", "His nose is small."},
                {"mouth", "嘴巴", "She has a big smile on her mouth."},
                {"hand", "手", "Wash your hands please."},
                {"foot", "脚", "My left foot hurts."},
                {"wear", "穿/戴", "I wear glasses."},
            },
            { // Day 5
                {"happy", "开心的", "I am very happy today."},
                {"sad", "难过的", "Don't be sad."},
                {"angry", "生气的", "My mother is angry."},
                {"tired", "累的", "I feel tired after school."},
                {"hungry", "饿的", "I am hungry. Let's eat."},
                {"thirsty", "渴的", "I am thirsty. Water please!"},
                {"afraid", "害怕的", "I am afraid of the dark."},
                {"brave", "勇敢的", "Be brave, my boy."},
                {"kind", "友好的", "She is a kind person."},
                {"love", "爱", "I love my family."},
            },
        },
        // Week 2
        {
            { // Day 1
                {"morning", "早晨", "I get up at 7 in the morning."},
                {"wake", "醒来", "I wake up early."},
                {"wash", "洗", "I wash my face."},
                {"brush", "刷", "I brush my teeth."},
                {"breakfast", "早餐", "I eat breakfast at home."},
                {"lunch", "午餐", "I have lunch at school."},
                {"dinner", "晚餐", "We have dinner at 6 pm."},
                {"school", "学校", "I go to school every day."},
                {"bus", "公交车", "I take the bus to school."},
                {"walk", "走路", "I walk to school with my friend."},
            },
            { // Day 2
                {"teacher", "老师", "My teacher is very nice."},
                {"student", "学生", "I am a student."},
                {"class", "班级/课", "Our class has 30 students."},
                {"desk", "课桌", "My books are on my desk."},
                {"book", "书", "I like reading books."},
                {"pencil", "铅笔", "Can I borrow a pencil?"},
                {"homework", "家庭作业", "I do my homework after school."},
                {"write", "写", "Please write your name here."},
                {"read", "读", "I can read English stories."},
                {"learn", "学习", "I learn English every day."},
            },
            { // Day 3
                {"subject", "科目", "Maths is my favourite subject."},
                {"English", "英语", "I like learning English."},
                {"maths", "数学", "Maths is fun."},
                {"science", "科学", "Science is interesting."},
                {"music", "音乐", "I have music class today."},
                {"art", "美术", "I love art class."},
                {"sport", "运动", "We do sport on Wednesday."},
                {"easy", "容易的", "This question is easy."},
                {"difficult", "难的", "Maths is difficult for me."},
                {"test", "考试", "We have an English test on Friday."},
            },
            { // Day 4
                {"play", "玩耍", "I play with my friends after school."},
                {"game", "游戏", "Let's play a game."},
                {"park", "公园", "We go to the park."},
                {"home", "家", "I go home at 5 o'clock."},
                {"watch", "看", "I watch cartoons."},
                {"story", "故事", "Mum tells me a story."},
                {"draw", "画画", "I like to draw pictures."},
                {"sing", "唱歌", "We sing songs together."},
                {"dance", "跳舞", "She can dance very well."},
                {"ride", "骑", "I ride my bike after school."},
            },
            { // Day 5
                {"weekend", "周末", "The weekend is fun."},
                {"Saturday", "周六", "On Saturday I go swimming."},
                {"Sunday", "周日", "Sunday is family day."},
                {"visit", "拜访", "We visit my grandmother."},
                {"cinema", "电影院", "Let's go to the cinema."},
                {"shopping", "购物", "We go shopping on Saturday."},
                {"picnic", "野餐", "We have a picnic in the park."},
                {"swim", "游泳", "I can swim."},
                {"rest", "休息", "I rest on Sunday afternoon."},
                {"plan", "计划", "What's your plan for the weekend?"},
            },
        },
        // Week 3
        {
            { // Day 1
                {"apple", "苹果", "I eat an apple every day."},
                {"banana", "香蕉", "Monkeys like bananas."},
                {"orange", "橙子", "Orange juice is nice."},
                {"fruit", "水果", "Fruit is good for you."},
                {"vegetable", "蔬菜", "Eat your vegetables."},
                {"carrot", "胡萝卜", "Rabbits like carrots."},
                {"tomato", "番茄", "I don't like tomatoes."},
                {"potato", "土豆", "I like potato chips."},
                {"rice", "米饭", "We eat rice every day."},
                {"bread", "面包", "I eat bread for breakfast."},
            },
            { // Day 2
                {"cook", "烹饪", "My mother cooks dinner."},
                {"kitchen", "厨房", "Mum is in the kitchen."},
                {"meal", "一餐", "Breakfast is an important meal."},
                {"egg", "鸡蛋", "I eat an egg for breakfast."},
                {"milk", "牛奶", "I drink milk every morning."},
                {"water", "水", "Drink lots of water."},
                {"juice", "果汁", "I like apple juice."},
                {"sugar", "糖", "Don't eat too much sugar."},
                {"salt", "盐", "Please pass the salt."},
                {"delicious", "美味的", "This cake is delicious."},
            },
            { // Day 3
                {"restaurant", "餐厅", "We eat at a restaurant."},
                {"menu", "菜单", "Can I see the menu?"},
                {"order", "点餐", "I would like to order."},
                {"waiter", "服务员", "The waiter is friendly."},
                {"bill", "账单", "Can I have the bill?"},
                {"chicken", "鸡肉", "I want chicken and rice."},
                {"fish", "鱼", "The fish is very fresh."},
                {"salad", "沙拉", "I'll have a salad."},
                {"ice cream", "冰淇淋", "I love ice cream."},
                {"cake", "蛋糕", "The chocolate cake looks great."},
            },
            { // Day 4
                {"health", "健康", "Health is important."},
                {"exercise", "锻炼", "I do exercise every morning."},
                {"sleep", "睡觉", "I go to sleep at 9 pm."},
                {"clean", "干净的", "Keep your room clean."},
                {"doctor", "医生", "The doctor is kind."},
                {"medicine", "药", "Take your medicine."},
                {"sick", "生病的", "I feel sick today."},
                {"well", "健康的", "I hope you get well soon."},
                {"body", "身体", "Exercise is good for your body."},
                {"grow", "成长", "Children grow very fast."},
            },
            { // Day 5
                {"hospital", "医院", "She went to the hospital."},
                {"nurse", "护士", "The nurse helped me."},
                {"headache", "头痛", "I have a headache."},
                {"stomach", "胃", "My stomach hurts."},
                {"cold", "感冒", "I have a cold."},
                {"fever", "发烧", "He has a high fever."},
                {"hurt", "疼", "My arm hurts."},
                {"feel", "感觉", "How do you feel?"},
                {"better", "更好的", "I feel better now."},
                {"rest", "休息", "You need to rest."},
            },
        },
        // Week 4
        {
            { // Day 1
                {"sport", "运动", "I like sport."},
                {"football", "足球", "Let's play football."},
                {"basketball", "篮球", "He is good at basketball."},
                {"tennis", "网球", "I play tennis on Sunday."},
                {"run", "跑步", "I run in the park."},
                {"jump", "跳", "The rabbit can jump high."},
                {"team", "队伍", "Our team won the game."},
                {"win", "赢", "We want to win."},
                {"ball", "球", "Kick the ball!"},
                {"strong", "强壮的", "My father is very strong."},
            },
            { // Day 2
                {"music", "音乐", "I love listening to music."},
                {"song", "歌曲", "This is my favourite song."},
                {"piano", "钢琴", "I can play the piano."},
                {"guitar", "吉他", "He plays the guitar."},
                {"colour", "颜色", "What colour do you like?"},
                {"paint", "画画/颜料", "Let's paint a picture."},
                {"picture", "图片", "Look at this picture."},
                {"beautiful", "美丽的", "This painting is beautiful."},
                {"camera", "相机", "I have a new camera."},
                {"photo", "照片", "Take a photo of us."},
            },
            { // Day 3
                {"book", "书", "I read a book every week."},
                {"story", "故事", "Tell me a story."},
                {"page", "页", "Turn to page 10."},
                {"newspaper", "报纸", "Dad reads the newspaper."},
                {"magazine", "杂志", "I like this magazine."},
                {"library", "图书馆", "I borrow books from the library."},
                {"dictionary", "字典", "Look up the word in a dictionary."},
                {"word", "单词", "What does this word mean?"},
                {"letter", "字母/信", "A is the first letter."},
                {"writer", "作者", "Who is the writer of this book?"},
            },
            { // Day 4
                {"television", "电视", "I watch television after dinner."},
                {"film", "电影", "Let's watch a film."},
                {"computer", "电脑", "I use the computer for homework."},
                {"internet", "互联网", "I use the internet to learn."},
                {"phone", "电话", "My phone is in my bag."},
                {"message", "消息", "I sent you a message."},
                {"email", "电子邮件", "Please send me an email."},
                {"screen", "屏幕", "Don't look at the screen too long."},
                {"video", "视频", "I watch English videos."},
                {"game", "游戏", "I love playing video games."},
            },
            { // Day 5
                {"party", "聚会", "I'm going to a party."},
                {"birthday", "生日", "Happy birthday to you!"},
                {"present", "礼物", "Thank you for the present."},
                {"card", "卡片", "I made a birthday card."},
                {"invite", "邀请", "I want to invite my friends."},
                {"celebrate", "庆祝", "How do you celebrate New Year?"},
                {"fun", "有趣的", "We had a lot of fun."},
                {"enjoy", "享受", "I enjoy the party."},
                {"special", "特别的", "Today is a special day."},
                {"holiday", "假期", "The summer holiday is coming."},
            },
        },
        // Week 5
        {
            { // Day 1
                {"house", "房子", "I live in a big house."},
                {"room", "房间", "My room is on the second floor."},
                {"bedroom", "卧室", "I sleep in my bedroom."},
                {"bathroom", "浴室", "The bathroom is clean."},
                {"kitchen", "厨房", "Mum is cooking in the kitchen."},
                {"living room", "客厅", "We watch TV in the living room."},
                {"garden", "花园", "We have flowers in the garden."},
                {"door", "门", "Please close the door."},
                {"window", "窗户", "Open the window, please."},
                {"floor", "地板/楼层", "My room is on the ground floor."},
            },
            { // Day 2
                {"table", "桌子", "Put your book on the table."},
                {"chair", "椅子", "Sit on the chair."},
                {"bed", "床", "I go to bed at 9 o'clock."},
                {"sofa", "沙发", "Dad sits on the sofa."},
                {"cupboard", "柜子", "Put the cups in the cupboard."},
                {"lamp", "灯", "Turn on the lamp."},
                {"mirror", "镜子", "Look in the mirror."},
                {"clock", "钟", "The clock shows 3 o'clock."},
                {"key", "钥匙", "Where is my key?"},
                {"box", "盒子", "The toy is in the box."},
            },
            { // Day 3
                {"city", "城市", "I live in a big city."},
                {"town", "城镇", "Our town is small but nice."},
                {"street", "街道", "I live on a quiet street."},
                {"building", "建筑", "That is a tall building."},
                {"supermarket", "超市", "We buy food at the supermarket."},
                {"cinema", "电影院", "The cinema is next to the park."},
                {"museum", "博物馆", "We visited the museum."},
                {"bridge", "桥", "Cross the bridge."},
                {"river", "河流", "There is a river in our town."},
                {"map", "地图", "Can you read a map?"},
            },
            { // Day 4
                {"turn", "转弯", "Turn left at the corner."},
                {"left", "左", "Go left here."},
                {"right", "右", "Turn right at the bank."},
                {"straight", "直走", "Go straight on."},
                {"near", "近的", "The park is near my house."},
                {"far", "远的", "The airport is far from here."},
                {"behind", "在…后面", "The garden is behind the house."},
                {"in front of", "在…前面", "The bus stop is in front of the school."},
                {"next to", "在…旁边", "The shop is next to the bank."},
                {"between", "在…之间", "The park is between the school and the library."},
            },
            { // Day 5
                {"hotel", "酒店", "We stay at a big hotel."},
                {"room", "房间", "I want a room for two nights."},
                {"key", "钥匙", "Here is your room key."},
                {"lift", "电梯", "Take the lift to the third floor."},
                {"floor", "楼层", "My room is on the fifth floor."},
                {"reception", "前台", "Go to the reception desk."},
                {"swimming pool", "游泳池", "The hotel has a swimming pool."},
                {"stay", "停留/住", "We will stay for three days."},
                {"check in", "入住", "We check in at 2 pm."},
                {"leave", "离开", "We leave the hotel at 11 am."},
            },
        },
        // Week 6
        {
            { // Day 1
                {"clothes", "衣服", "I need new clothes."},
                {"shirt", "衬衫", "He wears a white shirt."},
                {"dress", "连衣裙", "She has a beautiful dress."},
                {"coat", "外套", "Put on your coat. It's cold."},
                {"hat", "帽子", "I like your hat."},
                {"shoe", "鞋", "My shoes are new."},
                {"sock", "袜子", "I need a pair of socks."},
                {"red", "红色", "I like the red dress."},
                {"blue", "蓝色", "The sky is blue."},
                {"green", "绿色", "The grass is green."},
            },
            { // Day 2
                {"shop", "商店", "The shop opens at 9."},
                {"buy", "买", "I want to buy a new bag."},
                {"sell", "卖", "This shop sells nice clothes."},
                {"size", "尺码", "What size do you need?"},
                {"small", "小的", "This shirt is too small."},
                {"big", "大的", "I need a bigger size."},
                {"try", "试", "Can I try this on?"},
                {"expensive", "贵的", "This is too expensive."},
                {"cheap", "便宜的", "These shoes are very cheap."},
                {"receipt", "收据", "Keep your receipt."},
            },
            { // Day 3
                {"money", "钱", "I don't have enough money."},
                {"pound", "英镑", "It costs 5 pounds."},
                {"pence", "便士", "It's 50 pence."},
                {"price", "价格", "What is the price?"},
                {"pay", "付款", "Can I pay by card?"},
                {"change", "找零", "Here is your change."},
                {"free", "免费的", "The museum is free."},
                {"save", "节省/存钱", "I want to save money."},
                {"cost", "花费", "How much does it cost?"},
                {"spend", "花费时间/钱", "I spent 10 pounds."},
            },
            { // Day 4
                {"post office", "邮局", "The post office is over there."},
                {"letter", "信", "I want to send a letter."},
                {"stamp", "邮票", "I need a stamp."},
                {"send", "发送", "Please send this parcel."},
                {"bank", "银行", "I need to go to the bank."},
                {"account", "账户", "I have a bank account."},
                {"form", "表格", "Please fill in this form."},
                {"sign", "签名", "Sign your name here."},
                {"help", "帮助", "Can I help you?"},
                {"information", "信息", "I need some information."},
            },
            { // Day 5
                {"phone", "电话", "Can I use your phone?"},
                {"call", "打电话", "Please call me later."},
                {"speak", "说话", "Can I speak to Tom?"},
                {"answer", "回答", "Please answer the phone."},
                {"number", "号码", "What is your phone number?"},
                {"mobile", "手机", "My mobile phone is new."},
                {"text", "短信", "I sent you a text."},
                {"ring", "响铃", "The phone is ringing."},
                {"wait", "等待", "Please wait a moment."},
                {"bye", "再见", "Bye! See you later!"},
            },
        },
        // Week 7
        {
            { // Day 1
                {"train", "火车", "I go to school by train."},
                {"bus", "公交车", "The bus comes every 10 minutes."},
                {"car", "汽车", "My father drives a car."},
                {"bike", "自行车", "I ride my bike to school."},
                {"plane", "飞机", "We went to London by plane."},
                {"taxi", "出租车", "Let's take a taxi."},
                {"ticket", "票", "I need a train ticket."},
                {"station", "车站", "The station is near here."},
                {"stop", "停", "The bus stops here."},
                {"fast", "快的", "The train is very fast."},
            },
            { // Day 2
                {"airport", "机场", "We went to the airport."},
                {"passenger", "乘客", "All passengers must wait."},
                {"luggage", "行李", "Where is my luggage?"},
                {"passport", "护照", "Show me your passport."},
                {"arrive", "到达", "We arrive at 3 pm."},
                {"depart", "出发", "The train departs at 5."},
                {"late", "迟到", "Don't be late!"},
                {"early", "早的", "I got up early today."},
                {"waiting room", "候车室", "Wait in the waiting room."},
                {"platform", "站台", "The train is at platform 3."},
            },
            { // Day 3
                {"weather", "天气", "The weather is nice today."},
                {"sun", "太阳", "The sun is shining."},
                {"rain", "雨", "It is raining outside."},
                {"wind", "风", "The wind is strong today."},
                {"cloud", "云", "There are clouds in the sky."},
                {"snow", "雪", "It snows in winter."},
                {"hot", "热的", "It is very hot in summer."},
                {"cold", "冷的", "It is cold in winter."},
                {"warm", "温暖的", "Spring is warm and nice."},
                {"season", "季节", "There are four seasons."},
            },
            { // Day 4
                {"animal", "动物", "The zoo has many animals."},
                {"dog", "狗", "I have a pet dog."},
                {"cat", "猫", "The cat is sleeping."},
                {"bird", "鸟", "The bird can fly."},
                {"fish", "鱼", "There are fish in the pond."},
                {"horse", "马", "She can ride a horse."},
                {"rabbit", "兔子", "The rabbit is white."},
                {"lion", "狮子", "The lion is the king of animals."},
                {"elephant", "大象", "The elephant is very big."},
                {"zoo", "动物园", "Let's go to the zoo."},
            },
            { // Day 5
                {"holiday", "假期", "Where do you go on holiday?"},
                {"beach", "海滩", "We play on the beach."},
                {"sea", "海", "I like swimming in the sea."},
                {"sand", "沙子", "The children play in the sand."},
                {"sunny", "晴朗的", "It is a sunny day."},
                {"trip", "旅行", "We had a nice trip."},
                {"country", "国家", "What country do you live in?"},
                {"world", "世界", "The world is very big."},
                {"remember", "记住", "Remember to bring your camera."},
                {"review", "复习", "Let's review what we learned."},
            },
        },
    };

    // Grammar points for each day
    private static final String[][][] GRAMMAR = {
        // Week 1
        {
            {"Verb 'to be' (am/is/are)", "I am, He is, They are"},
            {"Numbers & Ordinals", "1st, 2nd, 3rd... Dates and ages"},
            {"Possessive Adjectives", "my, your, his, her, our, their"},
            {"Have got / Has got", "I have got brown hair. She has got blue eyes."},
            {"Present Simple: Feelings", "I feel happy. She feels tired."},
        },
        // Week 2
        {
            {"Present Simple: Routines", "I get up at 7. She goes to school."},
            {"There is / There are", "There is a book. There are 30 students."},
            {"Can / Can't (ability)", "I can swim. He can't play piano."},
            {"Adverbs of Frequency", "always, usually, sometimes, never"},
            {"Going to (future plans)", "I'm going to visit my grandma."},
        },
        // Week 3
        {
            {"Countable/Uncountable Nouns", "an apple vs some rice"},
            {"Some / Any", "I have some milk. Do you have any bread?"},
            {"Would like", "I would like a sandwich. Would you like some tea?"},
            {"Imperatives", "Wash your hands. Don't eat too much."},
            {"Should / Shouldn't", "You should rest. You shouldn't run."},
        },
        // Week 4
        {
            {"Like + -ing", "I like swimming. She likes dancing."},
            {"Present Continuous", "I am singing. They are playing."},
            {"Past Simple: Regular verbs", "I played football. She watched TV."},
            {"Past Simple: Irregular verbs", "I went home. She ate dinner."},
            {"Comparatives", "bigger, smaller, more interesting than"},
        },
        // Week 5
        {
            {"Prepositions of Place", "in, on, under, behind, next to"},
            {"Articles (a/an/the)", "a table, an apple, the kitchen"},
            {"Prepositions of Movement", "go to, walk along, turn left"},
            {"Giving Directions", "Go straight on. Turn right at the bank."},
            {"Questions with 'How'", "How much? How many? How far?"},
        },
        // Week 6
        {
            {"Adjectives: Order & Colours", "a big red bag"},
            {"Too / Enough", "too big, not big enough"},
            {"How much is/are?", "How much is this shirt? It's 10 pounds."},
            {"Must / Mustn't", "You must sign here. You mustn't run."},
            {"Present Perfect: Experiences", "I have been to London. Have you ever...?"},
        },
        // Week 7
        {
            {"By + transport", "by bus, by train, on foot"},
            {"Future with 'Will'", "I will call you. The train will arrive soon."},
            {"Future Time Clauses", "When I arrive, I'll call you. If it rains, we'll stay home."},
            {"Superlatives", "the biggest, the most beautiful, the best"},
            {"Review: All Tenses", "present, past, future review"},
        },
    };

    /**
     * Get the week index (0-based) for a given day number (1-based)
     */
    public static int getWeekIndex(int day) {
        return (day - 1) / 5;
    }

    /**
     * Get the day index within a week (0-based) for a given day number
     */
    public static int getDayInWeekIndex(int day) {
        return (day - 1) % 5;
    }

    /**
     * Get week themes for display
     */
    public static String[] getWeekTheme(int weekIdx) {
        return WEEK_THEMES[weekIdx];
    }

    /**
     * Get daily topic [en, zh]
     */
    public static String[] getDailyTopic(int weekIdx, int dayIdx) {
        return DAILY_TOPICS[weekIdx][dayIdx];
    }

    /**
     * Get vocabulary array for a day [en, zh, example]
     */
    public static String[][] getVocabulary(int weekIdx, int dayIdx) {
        return VOCAB[weekIdx][dayIdx];
    }

    /**
     * Get grammar point [point, explanation]
     */
    public static String[] getGrammar(int weekIdx, int dayIdx) {
        return GRAMMAR[weekIdx][dayIdx];
    }

    /**
     * Get the Chinese weekday name
     */
    public static String getChineseWeekday(LocalDate date) {
        String[] weekdays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        return weekdays[date.getDayOfWeek().getValue() % 7];
    }

    public static int getTotalDays() {
        return 35;
    }
}
