package me.steven.indrev.utils

import me.steven.indrev.blockentities.MachineBlockEntity
import me.steven.indrev.blockentities.cables.CableBlockEntity
import me.steven.indrev.blocks.machine.CableBlock
import me.steven.indrev.mixin.AccessorEnergyHandler
import net.minecraft.block.entity.BlockEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import team.reborn.energy.Energy
import team.reborn.energy.EnergySide
import team.reborn.energy.EnergyStorage

@Suppress("CAST_NEVER_SUCCEEDS")
object EnergyMovement {
    fun spreadNeighbors(source: BlockEntity, pos: BlockPos) {
        val world = source.world
        if (source !is EnergyStorage) return
        val sourceHandler = Energy.of(source)
        val targets = Direction.values()
            .mapNotNull { direction ->
                if (source.getMaxOutput(EnergySide.fromMinecraft(direction)) > 0 && isSideConnected(source, direction)) {
                    val targetPos = pos.offset(direction)
                    val target = world?.getBlockEntity(targetPos)
                    if (target != null && Energy.valid(target) && isSideConnected(target, direction.opposite)) {
                        val targetHandler = Energy.of(target).side(direction.opposite)
                        if (targetHandler.energy < targetHandler.maxStored) targetHandler
                        else null
                    } else null
                } else null
            }
        val sum = targets.sumByDouble { targetHandler ->
            (targetHandler.maxStored - targetHandler.energy).coerceAtMost(targetHandler.maxInput)
        }
        targets.sortedByDescending { it.energy }.forEach { targetHandler ->
            val accessor = targetHandler as AccessorEnergyHandler
            val direction = accessor.side
            val target = accessor.holder
            sourceHandler.side(direction.opposite())
            val targetMaxInput = targetHandler.maxInput
            val energy = sourceHandler.energy.coerceAtMost(sourceHandler.maxOutput)
            val amount = (targetMaxInput / sum) * energy
            if (amount > 0) {
                if (source is MachineBlockEntity<*> && target is MachineBlockEntity<*>) {
                    target.temperatureComponent?.inputOverflow = amount > target.getMaxInput(direction)
                }
                sourceHandler.into(targetHandler).move(amount)
            }
        }
    }

    private fun isSideConnected(blockEntity: BlockEntity, direction: Direction) =
        blockEntity !is CableBlockEntity || blockEntity.cachedState[CableBlock.getProperty(direction)]
}