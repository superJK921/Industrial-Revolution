package me.steven.indrev.gui.controllers

import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.blockentities.crafters.CraftingMachineBlockEntity
import me.steven.indrev.gui.PatchouliEntryShortcut
import me.steven.indrev.gui.widgets.machines.WProcess
import me.steven.indrev.utils.add
import me.steven.indrev.utils.configure
import me.steven.indrev.utils.identifier
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.util.Identifier

class ElectricFurnaceController(
    syncId: Int,
    playerInventory: PlayerInventory,
    ctx: ScreenHandlerContext
) :
    IRGuiController(
        IndustrialRevolution.ELECTRIC_FURNACE_HANDLER,
        syncId,
        playerInventory,
        ctx
    ), PatchouliEntryShortcut {
    init {
        val root = WGridPanel()
        setRootPanel(root)
        configure("block.indrev.electric_furnace", ctx, playerInventory, blockInventory, propertyDelegate)

        val inputSlot = WItemSlot.of(blockInventory, 2)
        root.add(inputSlot, 2.7, 2.0)

        val processWidget = WProcess(propertyDelegate)
        root.add(processWidget, 3.9, 2.0)

        val outputSlot = WItemSlot.outputOf(blockInventory, 3)
        outputSlot.addChangeListener { _, _, _, _ ->
            val player = playerInventory.player
            if (!player.world.isClient) {
                ctx.run { world, pos ->
                    val blockEntity = world.getBlockEntity(pos) as? CraftingMachineBlockEntity<*> ?: return@run
                    blockEntity.dropExperience(player)
                }
            }
        }
        outputSlot.isInsertingAllowed = false
        root.add(outputSlot, 5.9, 2.0)

        root.validate(this)
    }

    override fun canUse(player: PlayerEntity?): Boolean = true

    override fun getEntry(): Identifier = identifier("machines/basic_machines")

    override fun getPage(): Int = 1

    companion object {
        val SCREEN_ID = identifier("electric_furnace_screen")
    }
}