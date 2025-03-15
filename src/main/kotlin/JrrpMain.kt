package org.linfeh.mirai.jrrp

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import java.io.File
import kotlin.random.Random
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.data.*

object JrrpMain : KotlinPlugin(
    JvmPluginDescription(
        id = "org.linfeh.mirai.jrrp",
        name = "mirai-jrrp",
        version = "1.0.1"
    )
) {
    private val userJrrpValues = mutableMapOf<Long, Int>()
    private val jrrpReplies = mutableMapOf<String, List<String>>()

    // 配置文件路径
    private val configFile = File(configFolder, "config.yml")


    override fun onEnable() {
        logger.info("Plugin loaded")

        // 加载配置文件
        loadConfig()

        globalEventChannel().subscribeAlways<GroupMessageEvent> {
            if (message.contentToString().startsWith("/jrrp")) {
                // 处理群消息中的 /jrrp 指令

                // 获取人品值和回复
                val userId = sender.id
                val userName = sender.nameCardOrNick
                val jrrpValue = getJrrpValue(userId)
                val reply = getJrrpReply(jrrpValue)
                // 构建消息链
                val message = buildMessageChain {
                    +At(sender)
                    +PlainText("\n今天，$userName 的人品值为：$jrrpValue，$reply")
                }
                group.sendMessage(message)
            }
        }

        globalEventChannel().subscribeAlways<FriendMessageEvent> {
            if (message.contentToString().startsWith("/jrrp")) {
                // 处理好友消息中的 /jrrp 指令
                val userId = sender.id
                val jrrpValue = getJrrpValue(userId)
                val reply = getJrrpReply(jrrpValue)
                sender.sendMessage("今天，你的人品值为：$jrrpValue，$reply")
            }
        }
    }

    // 获取用户的人品值
    private fun getJrrpValue(userId: Long): Int {
        return userJrrpValues.getOrPut(userId) { Random.nextInt(0, 101) }
    }

    // 获取人品值对应的回复语
    private fun getJrrpReply(jrrpValue: Int): String {
        return when {
            jrrpValue in 0..20 -> getRandomReply("0-20")
            jrrpValue in 20..40 -> getRandomReply("20-40")
            jrrpValue in 40..60 -> getRandomReply("40-60")
            jrrpValue in 60..80 -> getRandomReply("60-80")
            jrrpValue in 80..100 -> getRandomReply("80-100")
            else -> "未知错误"
        }
    }

    // 从区间内的多个回复语中随机选择一个
    private fun getRandomReply(range: String): String {
        val replies = jrrpReplies[range]
        return replies?.random() ?: "没有找到合适的回复。"
    }

    // 加载配置文件
    private fun loadConfig() {
        if (!configFile.exists()) {
            // 配置文件不存在，创建默认配置
            configFile.createNewFile()
            val defaultConfig = """
# 人品值区间的回复语（多个回复语用列表表示）
"0-20":
  - "主人，今天运气糟糕，最好不要外出，免得遇到不开心的事情。"
  - "运气差得不可思议，主人，还是待在家里喝杯热茶暖暖身子吧。"
  - "今天的你，注定要遭遇不幸，还是在家陪我玩吧。"
  - "有点小霉运，主人，尽量避免重大决策，免得后悔哦。"
  - "运气不太好，但还不至于糟糕，主人，给我一个抱抱，心情会好点的。"
"20-40":
  - "今天有点小霉运，主人，还是多休息，别太劳累了。"
  - "运气不太好，但还不至于糟糕，主人，来点甜点提提神吧。"
  - "今天虽然不好，但总比0分好，主人，别灰心，明天会更好！"
  - "今天的运气一般，做事可以稍微放心，但也别太大意哦。"
  - "运气平平，没什么大喜大悲的事情，主人，陪我玩会儿游戏吧。"
"40-60":
  - "今天的运气一般，主人，做事可以稍微放心，但也别掉以轻心。"
  - "运气平平，主人，没什么大喜大悲的事情，保持冷静哦。"
  - "今天稍微有点运气，主人，保持冷静，别太得意忘形了。"
  - "今天运气不错，有些事情会顺利进行，记得感谢我哦。"
  - "稍有好运，主人，适合做一些尝试，但也要小心谨慎。"
"60-80":
  - "今天运气不错，有些事情会顺利进行，记得请我吃零食哦。"
  - "稍有好运，主人，适合做一些尝试，但也别太冒险。"
  - "今天运气挺好，做事顺风顺水，主人，快去实现你的愿望吧！"
  - "运气爆棚，做什么都顺利，记得带上我一起分享喜悦。"
  - "运势极佳，能成的事都会顺利，主人，快去实现你的梦想吧！"
"80-100":
  - "今天运气爆棚，做什么都顺利！"
  - "今天的你运气极好，可以尝试一些挑战。"
  - "今天运势极佳，能成的事都会顺利！"
  - "简直是幸运日，什么都顺利，记得给我奖励哦。"
  - "运气好到爆棚，主人，快去买彩票，说不定能中大奖！"
            """
            configFile.writeText(defaultConfig)
        }

        // 解析配置文件
        val lines = configFile.readLines()
        var currentRange = ""
        var currentReplies = mutableListOf<String>()

        for (line in lines) {
            val trimmedLine = line.trim()

            // 区间定义
            if (trimmedLine.endsWith(":")) {
                // 保存前一个区间
                if (currentRange.isNotEmpty()) {
                    jrrpReplies[currentRange] = currentReplies
                }
                // 获取新的区间
                currentRange = trimmedLine.removeSuffix(":")
                currentReplies = mutableListOf()
            } else if (trimmedLine.isNotEmpty()) {
                // 添加回复语
                currentReplies.add(trimmedLine.removeSurrounding("-", " ").trim())
            }
        }

        // 保存最后一个区间
        if (currentRange.isNotEmpty()) {
            jrrpReplies[currentRange] = currentReplies
        }
    }
}
