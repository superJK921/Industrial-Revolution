package me.steven.indrev.blockentities.battery

import me.steven.indrev.blockentities.InterfacedMachineBlockEntity
import me.steven.indrev.blocks.BasicMachineBlock
import me.steven.indrev.content.MachineRegistry
import me.steven.indrev.inventories.DefaultSidedInventory
import me.steven.indrev.items.RechargeableItem
import net.minecraft.block.BlockState
import net.minecraft.container.ArrayPropertyDelegate
import net.minecraft.container.PropertyDelegate
import net.minecraft.inventory.SidedInventory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IWorld
import team.reborn.energy.EnergySide

class BatteryBlockEntity : InterfacedMachineBlockEntity(MachineRegistry.BATTERY_BLOCK_ENTITY, 750.0) {
    val inventory: SidedInventory = DefaultSidedInventory(1, intArrayOf(0), intArrayOf()) { _, stack -> stack?.item is RechargeableItem }

    override fun getInventory(state: BlockState?, world: IWorld?, pos: BlockPos?): SidedInventory = inventory

    override fun createDelegate(): PropertyDelegate = ArrayPropertyDelegate(2)

    override fun getMaxOutput(side: EnergySide?): Double {
        val state = this.cachedState
        return if (side != EnergySide.fromMinecraft(state[BasicMachineBlock.FACING])) 32.0 else 0.0
    }

    override fun getMaxInput(side: EnergySide?): Double {
        val state = this.cachedState
        return if (side == EnergySide.fromMinecraft(state[BasicMachineBlock.FACING])) 32.0 else 0.0
    }
}