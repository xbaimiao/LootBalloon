package com.xbaimiao.lootballoon

import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.database.OrmliteSQLite
import com.xbaimiao.easylib.ui.SpigotBasic
import com.xbaimiao.easylib.util.info
import com.xbaimiao.easylib.util.isNotAir
import com.xbaimiao.easylib.util.plugin
import com.xbaimiao.lootballoon.ai.LootBalloonAI
import com.xbaimiao.lootballoon.core.Balloon
import com.xbaimiao.lootballoon.core.DatabaseImpl
import com.xbaimiao.lootballoon.core.LootBalloonRefreshTask
import com.xbaimiao.lootballoon.jector.MythicInjector
import de.tr7zw.changeme.nbtapi.NBTContainer
import de.tr7zw.changeme.nbtapi.NBTItem
import org.bukkit.entity.Player
import java.time.LocalTime

// todo 自动刷新 黑名单世界
@Suppress("unused")
class LootBalloon : EasyPlugin() {

    companion object {
        @JvmStatic
        val inst get() = plugin as LootBalloon

        private const val probabilitySpit = "<<<<<>>>>>"
    }

    val balloonList = ArrayList<Balloon>()
    val database by lazy { DatabaseImpl(OrmliteSQLite("database.db")) }

    override fun enable() {
        saveDefaultConfig()
        reload()
        MythicInjector.instance.injectGoals(listOf(LootBalloonAI::class.java))
        LootBalloonRefreshTask.start()
    }

    fun reload() {
        reloadConfig()
        balloonList.clear()
        for (name in config.getKeys(false)) {
            val path = "$name."
            val maxAmount = config.getInt(path + "max-amount")
            val minAmount = config.getInt(path + "min-amount")
            val mobName = config.getString(path + "mob-name")!!
            val mobDeathSound = config.getString(path + "mob-death-sound")!!
            val chestDownSound = config.getString(path + "chest-down-sound")!!
            val mobChestName = config.getString(path + "mob-chest-name")!!
            val mobMoveSpeed = config.getString(path + "mob-move-speed")!!
            val iaBlock = config.getString(path + "ia-block")!!
            val items = config.getStringList(path + "items").map {
                val split = it.split(probabilitySpit)
                split[0].toDouble() to split[1].fromBase64()
            }
            val worlds = config.getStringList(path + "worlds")
            val time = config.getString(path + "time", "00:00-24:00")!!.split("-").let {
                LocalTime.parse(it[0]) to LocalTime.parse(it[1])
            }
            val maxAmountPerPlayer = config.getInt(path + "max-amount-per-player")
            val probability = config.getDouble(path + "refresh-probability", 0.3)
            val height = config.getDouble(path + "height", 20.0)
            val exposeLocation = config.getBoolean(path + "expose-location")
            val clickTeleport = config.getBoolean(path + "click-teleport")
            val exposeLocationMessage = config.getString(path + "expose-location-message", "")!!
            val clickTeleportMessage = config.getString(path + "click-teleport-message", "")!!
            val clickTeleportHover = config.getString(path + "click-teleport-hover", "")!!
            val teleportOffset = config.getInt(path + "teleport-offset", 1)
            balloonList.add(Balloon(
                name,
                maxAmount,
                minAmount,
                mobName,
                mobDeathSound,
                chestDownSound,
                mobChestName,
                mobMoveSpeed,
                iaBlock,
                worlds,
                time,
                maxAmountPerPlayer,
                probability,
                height,
                items,
                exposeLocation,
                clickTeleport,
                teleportOffset,
                exposeLocationMessage,
                clickTeleportMessage,
                clickTeleportHover
            ).also {
                info("§a加载气球 $name")
            })
        }
    }

    fun edit(player: Player, balloon: Balloon) {
        val basic = SpigotBasic(player, "§c编辑roll物品")

        basic.rows(6)
        basic.handLocked(false)

        balloon.items.withIndex().forEach { (index, item) ->
            if (index > 53) {
                return@forEach
            }
            basic.set(index, NBTItem.convertNBTtoItem(NBTContainer(item.second))!!)
        }

        basic.onClose { event ->
            val newItems = ArrayList<Pair<Double, String>>()
            for (s in event.inventory.contents.filter { it.isNotAir() }
                .map { NBTItem.convertItemtoNBT(it).toString() }) {
                val probability = balloon.items.find { it.second == s }?.first ?: 1.0
                newItems.add(probability to s)
            }
            balloon.items = newItems
            config.set(
                "${balloon.name}.items",
                balloon.items.map { "${it.first}$probabilitySpit${it.second.toBase64()}" })
            saveConfig()
            player.sendMessage("§c编辑成功")
        }

        basic.open()
    }

    private fun String.toBase64(): String {
        return java.util.Base64.getEncoder().encodeToString(toByteArray())
    }

    private fun String.fromBase64(): String {
        return String(java.util.Base64.getDecoder().decode(this))
    }

}
