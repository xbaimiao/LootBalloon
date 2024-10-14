package com.xbaimiao.lootballoon.core

import com.xbaimiao.easylib.bridge.player.parseToESound
import com.xbaimiao.easylib.chat.TellrawJson
import com.xbaimiao.easylib.chat.colored
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.util.*
import com.xbaimiao.lootballoon.LootBalloon
import de.tr7zw.changeme.nbtapi.NBTContainer
import de.tr7zw.changeme.nbtapi.NBTItem
import dev.lone.itemsadder.api.CustomBlock
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import java.time.LocalTime
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Balloon
 *
 * @author xbaimiao
 * @since 2023/10/22 08:00
 */
class Balloon(
    val name: String,
    private val maxAmount: Int,
    private val minAmount: Int,
    internal val mobName: String,
    private val mobDeathSound: String,
    private val chestDownSound: String,
    private val mobChestName: String,
    private val mobMoveSpeed: String,
    internal val iaBlock: String,
    private val worlds: List<String>,
    private val time: Pair<LocalTime, LocalTime>,
    private val maxAmountPerPlayer: Int,
    private val probability: Double,
    internal val height: Double,
    internal var items: List<Pair<Double, String>>,
    // 是否暴露坐标
    private val exposeLocation: Boolean,
    // 暴露坐标时是否可以点击传送
    private val clickTeleport: Boolean,
    // 传送时偏移量
    val teleportOffset: Int,
    private val exposeLocationMessage: String,
    private val clickTeleportMessage: String,
    private val clickTeleportHover: String,
) {

    suspend fun refresh(player: Player): Boolean = suspendCoroutine {
        launchCoroutine {
            val max = async {
                LootBalloon.inst.database.getTodayCount(player.name, this@Balloon) >= maxAmountPerPlayer
            }
            debug("max: $max maxAmountPerPlayer: $maxAmountPerPlayer")
            if (max) {
                it.resume(false)
                return@launchCoroutine
            }
            if (player.world.name in worlds) {
                debug("玩家在黑名单世界")
                it.resume(false)
                return@launchCoroutine
            }
            if (Math.random() > probability) {
                debug("概率不成立")
                it.resume(false)
                return@launchCoroutine
            }
            val now = LocalTime.now()
            if (now !in time.first..time.second) {
                debug("不在刷新时间内")
                it.resume(false)
                return@launchCoroutine
            }
            debug("生成成功  位置 ${player.location.clone().also { it.y += height }}")
            summon(player.location.clone().also { it.y += height })
            async {
                LootBalloon.inst.database.addTodayCount(player.name, this@Balloon)
            }
            it.resume(true)
        }
    }

    fun summon(location: Location) {
        val mythicMob = MythicBukkit.inst().mobManager.getMythicMob(mobName).getOrNull()
        if (mythicMob == null) {
            warn("§c召唤失败, 未找到生物 $mobName")
            return
        }
        mythicMob.spawn(BukkitAdapter.adapt(location), 1.0).also {
            it.entity.bukkitEntity.setMetadata("LootBalloonMoveSpeed", FixedMetadataValue(plugin, mobMoveSpeed))
        }
        if (exposeLocation) {
            val locationMessage = "${location.world!!.name},${location.blockX},${location.blockY},${location.blockZ}"
            val rawMessage = exposeLocationMessage.replace("{location}", locationMessage).colored()
            TellrawJson().broadcast {
                append(rawMessage)
                if (clickTeleport) {
                    val token = BalloonTeleport.cache(this@Balloon, location.clone())
                    append(
                        TellrawJson()
                            .append(clickTeleportMessage.colored())
                            .runCommand("/lootballoon api teleport|$token")
                            .hoverText(clickTeleportHover.colored())
                    )
                }
            }
        }
    }

    fun death(location: Location) {
        mobDeathSound.parseToESound().also { it.volume = 1f }.playSound(location)
        down(location)
    }

    fun roll(): List<ItemStack> {
        if (items.isEmpty()) {
            return emptyList()
        }
        val result = HashSet<String>()
        val amount = (minAmount..maxAmount).random()
        repeat(amount) {
            result.add(rollItem())
        }
        return result.map { NBTItem.convertNBTtoItem(NBTContainer(it))!! }
    }

    private fun rollItem(deep: Int = 0): String {
        if (deep > 10) {
            return items.random().second
        }
        for ((probability, item) in items.shuffled()) {
            if (Math.random() < probability) {
                return item
            }
        }
        return rollItem(deep + 1)
    }

    private fun down(location: Location) {
        val entity = MythicBukkit.inst().mobManager.getMythicMob(mobChestName).getOrNull()
        if (entity == null) {
            warn("未找到生物 $mobChestName")
            return
        }
        val mythicMob = entity.spawn(BukkitAdapter.adapt(location), 1.0)
        val bukkitEntity = BukkitAdapter.adapt(mythicMob.entity)
        bukkitEntity.location.chunk.addPluginChunkTicket(plugin)

        val done = {
            chestDownSound.parseToESound().also { it.volume = 1f }.playSound(location)
            place(bukkitEntity.location.clone())
            bukkitEntity.location.chunk.removePluginChunkTicket(plugin)
            bukkitEntity.remove()
        }

        submit(period = 2) {
            if (mythicMob.entity.isDead) {
                done.invoke()
                cancel()
                return@submit
            }
            if (mythicMob.entity.isOnGround) {
                done.invoke()
                cancel()
                return@submit
            }
        }

    }

    private fun place(location: Location) {
        if (!location.block.type.isAir) {
            location.add(0.0, 1.0, 0.0)
        }
        val block = CustomBlock.place(iaBlock, location)
        if (block == null) {
            warn("$iaBlock place block failed")
            return
        }
    }

    override fun toString(): String {
        return "Balloon(name='$name', maxAmount=$maxAmount, minAmount=$minAmount, mobName='$mobName', mobMoveSpeed='$mobMoveSpeed', iaBlock='$iaBlock', worlds=$worlds, time=$time, maxAmountPerPlayer=$maxAmountPerPlayer)"
    }

}
