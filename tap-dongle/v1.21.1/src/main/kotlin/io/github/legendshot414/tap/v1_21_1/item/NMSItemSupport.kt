/*
 * Copyright (C) 2022 Monun
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

package io.github.legendshot414.tap.v1_21_1.item

import io.github.legendshot414.tap.item.ItemSupport
import net.minecraft.nbt.NbtOps
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import org.bukkit.craftbukkit.CraftEquipmentSlot
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftInventoryPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack as BukkitItemStack
import org.bukkit.inventory.PlayerInventory as BukkitPlayerInventory

class NMSItemSupport : ItemSupport {
    override fun saveToJsonString(item: BukkitItemStack): String {
        val nmsItem = CraftItemStack.asNMSCopy(item)

        val nbt = ItemStack.CODEC
            .encodeStart(NbtOps.INSTANCE, nmsItem)
            .getOrThrow()

        return nbt.toString()
    }

    override fun damageArmor(playerInventory: BukkitPlayerInventory, attackDamage: Double) {
        if (attackDamage <= 0.0) return
        val player = (playerInventory.holder as CraftPlayer).handle
        val armorDamage = (attackDamage / 4.0).toFloat().coerceAtLeast(1.0f).toInt()

        val armorSlots = listOf(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        )
        EquipmentSlot.entries
            .filter { it.isArmor }
            .forEach { slot ->
                val nmsSlot = CraftEquipmentSlot.getNMS(slot)
                val item = player.getItemBySlot(nmsSlot)
                if (!item.isEmpty) {
                    item.hurtAndBreak(armorDamage, player, nmsSlot)
                }
            }

    }

    override fun damageSlot(playerInventory: BukkitPlayerInventory, slot: EquipmentSlot, damage: Int) {
        if (damage <= 0.0) return

        val nmsInventory = (playerInventory as CraftInventoryPlayer).inventory
        val nmsSlot = CraftEquipmentSlot.getNMS(slot)

        val nmsPlayerHandle = (playerInventory.holder as CraftPlayer).handle

        val nmsItem = nmsInventory.getItem(slot)

        if (!nmsItem.isEmpty) {
            nmsItem.hurtAndBreak(damage, nmsPlayerHandle, nmsSlot)
        }
    }
}

internal fun Inventory.getItem(slot: EquipmentSlot): ItemStack {
    return when (slot) {
        EquipmentSlot.HAND -> getSelected()
        EquipmentSlot.OFF_HAND -> offhand[0]
        EquipmentSlot.FEET -> armorContents[0]
        EquipmentSlot.LEGS -> armorContents[1]
        EquipmentSlot.CHEST -> armorContents[2]
        EquipmentSlot.HEAD -> armorContents[3]
        else -> throw IllegalArgumentException("Unknown EquipmentSlot: $slot")
    }
}