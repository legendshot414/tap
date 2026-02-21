/*
 * Copyright (C) 2023 Monun
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.legendshot414.tap.v1_21_1.protocol

import com.mojang.datafixers.util.Pair
import io.github.legendshot414.tap.protocol.*
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.network.protocol.game.*
import net.minecraft.world.phys.Vec3
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import java.util.*
import net.minecraft.world.entity.EquipmentSlot as NMSEquipmentSlot
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftWorld

class NMSPacketSupport : PacketSupport {
    companion object {
        private fun EquipmentSlot.toNMS(): NMSEquipmentSlot {
            return when (this) {
                EquipmentSlot.HAND -> NMSEquipmentSlot.MAINHAND
                EquipmentSlot.OFF_HAND -> NMSEquipmentSlot.OFFHAND
                EquipmentSlot.FEET -> NMSEquipmentSlot.FEET
                EquipmentSlot.LEGS -> NMSEquipmentSlot.LEGS
                EquipmentSlot.CHEST -> NMSEquipmentSlot.CHEST
                EquipmentSlot.HEAD -> NMSEquipmentSlot.HEAD
                else -> throw IllegalArgumentException("Unknown EquipmentSlot: $this")
            }
        }
    }

    override fun entityMetadata(entity: Entity): NMSPacketContainer {
        entity as CraftEntity

        val entityId = entity.entityId
        val entityData = entity.handle.entityData

        // nonDefaultValues가 null일때 ClientboundSetEntityDataPacket.pack(ClientboundSetEntityDataPacket.java:17) 에서 NPE 발생
        val packet = ClientboundSetEntityDataPacket(entityId, entityData.nonDefaultValues ?: emptyList())
        return NMSPacketContainer(packet)
    }


    override fun entityEquipment(entityId: Int, equipments: Map<EquipmentSlot, ItemStack>): NMSPacketContainer {
        val packet = ClientboundSetEquipmentPacket(entityId, equipments.map { entry ->
            Pair(entry.key.toNMS(), CraftItemStack.asNMSCopy(entry.value))
        })
        return NMSPacketContainer(packet)
    }

    override fun entityTeleport(
        entityId: Int,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
        onGround: Boolean
    ): NMSPacketContainer {
        val world = (Bukkit.getWorlds().first() as CraftWorld).handle
        val nmsEntity = world.getEntity(entityId)
            ?: throw IllegalArgumentException("Entity with id $entityId not found")

        // 위치 업데이트
        nmsEntity.moveTo(x, y, z, yaw, pitch)
        nmsEntity.setOnGround(onGround)

        // 패킷 생성 (1.21 유일한 public 생성자)
        val packet = ClientboundTeleportEntityPacket(nmsEntity)
        return NMSPacketContainer(packet)
    }

    override fun relEntityMove(
        entityId: Int,
        deltaX: Short,
        deltaY: Short,
        deltaZ: Short,
        onGround: Boolean
    ): NMSPacketContainer {
        val packet = ClientboundMoveEntityPacket.Pos(entityId, deltaX, deltaY, deltaZ, onGround)
        return NMSPacketContainer(packet)
    }

    override fun relEntityMoveLook(
        entityId: Int,
        deltaX: Short,
        deltaY: Short,
        deltaZ: Short,
        yaw: Float,
        pitch: Float,
        onGround: Boolean
    ): NMSPacketContainer {
        val packet = ClientboundMoveEntityPacket.PosRot(
            entityId,
            deltaX,
            deltaY,
            deltaZ,
            yaw.toProtocolDegrees().toByte(),
            pitch.toProtocolDegrees().toByte(),
            onGround
        )
        return NMSPacketContainer(packet)
    }

    override fun entityRotation(entityId: Int, yaw: Float, pitch: Float, onGround: Boolean): PacketContainer {
        return NMSPacketContainer(
            ClientboundMoveEntityPacket.Rot(
                entityId,
                yaw.toProtocolDegrees().toByte(),
                pitch.toProtocolDegrees().toByte(),
                onGround
            )
        )
    }

    override fun entityHeadLook(entityId: Int, yaw: Float): NMSPacketContainer {
        // 1. Entity 객체를 찾습니다.
        val world = (Bukkit.getWorlds().first() as CraftWorld).handle
        val nmsEntity = world.getEntity(entityId)
            ?: throw IllegalArgumentException("Entity with id $entityId not found for entityHeadLook")

        val packet = ClientboundRotateHeadPacket(
            nmsEntity,
            yaw.toProtocolDegrees().toByte()
        )
        return NMSPacketContainer(packet)
    }

    override fun entityVelocity(entityId: Int, vector: Vector): NMSPacketContainer {
        val packet = ClientboundSetEntityMotionPacket(entityId, Vec3(vector.x, vector.y, vector.z))

        return NMSPacketContainer(packet)
    }

    override fun entityStatus(
        entityId: Int,
        data: Byte
    ): NMSPacketContainer {
        val world = (Bukkit.getWorlds().first() as CraftWorld).handle
        val nmsEntity = world.getEntity(entityId)
            ?: throw IllegalArgumentException("Entity with id $entityId not found for entityStatus")

        val packet = ClientboundEntityEventPacket(nmsEntity, data)
        return NMSPacketContainer(packet)
    }
    override fun entityAnimation(
        entityId: Int,
        action: Int
    ): NMSPacketContainer {
        val world = (Bukkit.getWorlds().first() as CraftWorld).handle
        val nmsEntity = world.getEntity(entityId)
            ?: throw IllegalArgumentException("Entity with id $entityId not found for entityAnimation")

        val packet = ClientboundAnimatePacket(nmsEntity, action)
        return NMSPacketContainer(packet)
    }

    override fun hurtAnimation(entityId: Int, yaw: Float): PacketContainer {
        // 🔴 오류 수정: FriendlyByteBuf 대신 entityId와 yaw를 직접 전달
        // NMS 시그니처: ClientboundHurtAnimationPacket(int entityId, float yaw)
        val packet = ClientboundHurtAnimationPacket(entityId, yaw)
        return NMSPacketContainer(packet)
    }

    override fun mount(
        entityId: Int,
        mountEntityIds: IntArray
    ): NMSPacketContainer {
        // 1. NMS Entity 객체를 찾습니다. (Entity! 인수를 위해 필요)
        val world = (Bukkit.getWorlds().first() as CraftWorld).handle
        val nmsEntity = world.getEntity(entityId)
            ?: throw IllegalArgumentException("Entity with id $entityId not found for mount")


        val packet = ClientboundSetPassengersPacket(nmsEntity)
        return NMSPacketContainer(packet)
    }

    override fun takeItem(
        entityId: Int,
        collectorId: Int,
        stackAmount: Int
    ): NMSPacketContainer {
        val packet = ClientboundTakeItemEntityPacket(entityId, collectorId, stackAmount)
        return NMSPacketContainer(packet)
    }

    override fun removeEntity(
        entityId: Int
    ): NMSPacketContainer {
        val packet = ClientboundRemoveEntitiesPacket(entityId)
        return NMSPacketContainer(packet)
    }

    override fun removeEntities(vararg entityIds: Int): PacketContainer {
        return NMSPacketContainer(ClientboundRemoveEntitiesPacket(IntArrayList((entityIds))))
    }

    override fun containerSetSlot(containerId: Int, stateId: Int, slot: Int, item: ItemStack?): NMSPacketContainer {
        return NMSPacketContainer(
            ClientboundContainerSetSlotPacket(
                containerId,
                stateId,
                slot,
                CraftItemStack.asNMSCopy(item)
            )
        )
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun playerInfoAction(action: PlayerInfoAction, player: Player): PacketContainer {
        if (action == PlayerInfoAction.REMOVE) {
            return NMSPacketContainer(ClientboundPlayerInfoRemovePacket(listOf(player.uniqueId)))
        }

        when (action) {
            PlayerInfoAction.ADD -> ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER
            PlayerInfoAction.GAME_MODE -> ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE
            PlayerInfoAction.LATENCY -> ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY
            PlayerInfoAction.DISPLAY_NAME -> ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
            else -> throw IllegalArgumentException("impossible")
        }.let { nmsAction ->
            return NMSPacketContainer(ClientboundPlayerInfoUpdatePacket(nmsAction, (player as CraftPlayer).handle))
        }
    }

    override fun playerInfoUpdate(action: PlayerInfoUpdateAction, player: Player): PacketContainer {
        return when (action) {
            PlayerInfoUpdateAction.ADD_PLAYER -> ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER
            PlayerInfoUpdateAction.INITIALIZE_CHAT -> ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT
            PlayerInfoUpdateAction.UPDATE_GAME_MODE -> ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE
            PlayerInfoUpdateAction.UPDATE_LISTED -> ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED
            PlayerInfoUpdateAction.UPDATE_LATENCY -> ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY
            PlayerInfoUpdateAction.UPDATE_DISPLAY_NAME -> ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
        }.let { nmsAction ->
            NMSPacketContainer(ClientboundPlayerInfoUpdatePacket(nmsAction, (player as CraftPlayer).handle))
        }
    }

    override fun playerInfoRemove(list: List<UUID>): PacketContainer {
        return NMSPacketContainer(ClientboundPlayerInfoRemovePacket(list))
    }
}