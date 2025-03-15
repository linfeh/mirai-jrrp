package org.linfeh.mirai.jrrp

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.data.*
import java.io.File
import kotlin.random.Random

object JrrpMain : KotlinPlugin(
    JvmPluginDescription(
        id = "org.linfeh.mirai.jrrp",
        name = "mirai-jrrp",
        version = "1.0.3"
    )
) {
    private val userJrrpValues = mutableMapOf<Long, Int>()
    private val jrrpReplies = mutableMapOf<String, List<String>>()

    // 配置文件路径
    private val configFile = File(configFolder, "config.yml")

    override fun onEnable() {
        logger.info("Plugin loaded")
        loadConfig()

        globalEventChannel().subscribeAlways<GroupMessageEvent> {
            if (message.contentToString().startsWith("/jrrp")) {
                val userId = sender.id
                val userName = sender.nameCardOrNick
                val jrrpValue = getJrrpValue(userId)
                val reply = getJrrpReply(jrrpValue)
                // 构建消息链：先 @ 发送者，再发送人品信息
                val messageChain = buildMessageChain {
                    +At(sender)
                    +PlainText("\n${userName}的人品值为：${jrrpValue}\n$reply")
                }
                group.sendMessage(messageChain)
            }
        }

        globalEventChannel().subscribeAlways<FriendMessageEvent> {
            if (message.contentToString().startsWith("/jrrp")) {
                val userId = sender.id
                val jrrpValue = getJrrpValue(userId)
                val reply = getJrrpReply(jrrpValue)
                sender.sendMessage("今天，你的人品值为：$jrrpValue，$reply")
            }
        }
    }

    // 获取用户的人品值
    private fun getJrrpValue(userId: Long): Int =
        userJrrpValues.getOrPut(userId) { Random.nextInt(0, 101) }

    // 根据人品值获取对应的回复语
    private fun getJrrpReply(jrrpValue: Int): String {
        return when (jrrpValue) {
            in 0..20 -> getRandomReply("0-20")
            in 21..40 -> getRandomReply("20-40")
            in 41..60 -> getRandomReply("40-60")
            in 61..80 -> getRandomReply("60-80")
            in 81..100 -> getRandomReply("80-100")
            else -> "未知错误"
        }
    }

    // 从区间内的多个回复语中随机选择一条
    private fun getRandomReply(range: String): String =
        jrrpReplies[range]?.random() ?: "没有找到合适的回复。"

    // 加载配置文件并解析回复语
    private fun loadConfig() {
        if (!configFile.exists()) {
            // 若配置文件不存在，则创建默认配置文件
            configFile.parentFile.mkdirs()
            configFile.createNewFile()
            val defaultConfig = """
                # 人品值区间的回复语（多个回复语用列表表示）
                "0-20":
                  - "哎呀，今天运气有点欠佳呢，还是在家休息吧！"
                  - "今天似乎不太顺利哦，小心行事，避免踩坑。"
                  - "运气有点糟糕，不如待在家里追剧吧！"
                  - "今天可能会遇到小麻烦，保持微笑，一切都会好起来。"
                  - "哎哟，运气不在线，别灰心，明天会更好！"
                "20-40":
                  - "今天有点小霉运，尽量避免重大决策哦。"
                  - "运气不太好，但也不算太差，保持平常心。"
                  - "虽然运气一般，但也有转机的可能，加油！"
                  - "今天可能有些小波折，保持乐观，迎接挑战。"
                  - "运气平平，无风无浪，适合安静度过一天。"
                "40-60":
                  - "今天的运气中规中矩，适合处理日常事务。"
                  - "运气一般，但凡事认真对待，仍能取得好结果。"
                  - "今天可能没有惊喜，但平淡也是一种幸福。"
                  - "运气平平，但只要努力，依然会有收获。"
                  - "今天适合踏实工作，为未来打下坚实基础。"
                "60-80":
                  - "今天运气不错，适合尝试新事物，拓展视野。"
                  - "运气在线，抓住机会，可能会有意外收获。"
                  - "今天顺风顺水，适合推进重要项目。"
                  - "运气佳，适合与朋友聚会，增进感情。"
                  - "今天可能会有小确幸，保持好心情，迎接美好。"
                "80-100":
                  - "哇，今天运气爆棚，做什么都顺利！"
                  - "运气极佳，适合挑战高难度任务，展现实力。"
                  - "今天是幸运日，可能会有意想不到的好事发生。"
                  - "运势旺盛，适合投资理财，收益可期。"
                  - "今天福星高照，抓住机遇，实现梦想！"
            """.trimIndent()
            configFile.writeText(defaultConfig)
        }

        try {
            val lines = configFile.readLines()
            var currentRange: String? = null
            val tempReplies = mutableMapOf<String, MutableList<String>>()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

                if (trimmed.endsWith(":") && !trimmed.startsWith("-")) {
                    currentRange = trimmed.removeSuffix(":").trim().trim('"')
                    tempReplies[currentRange] = mutableListOf()
                } else if (trimmed.startsWith("-") && currentRange != null) {
                    val reply = trimmed.removePrefix("-").trim().trim('"')
                    tempReplies[currentRange]?.add(reply)
                }
            }
            jrrpReplies.putAll(tempReplies)
        } catch (e: Exception) {
            logger.error("加载配置文件出错：${e.message}")
        }
    }
}
