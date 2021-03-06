package me.steven.indrev.gui.controllers

import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.gui.PatchouliEntryShortcut
import me.steven.indrev.gui.widgets.machines.WFluid
import me.steven.indrev.gui.widgets.machines.WProcess
import me.steven.indrev.utils.add
import me.steven.indrev.utils.configure
import me.steven.indrev.utils.identifier
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.util.Identifier

class SmelterController(syncId: Int, playerInventory: PlayerInventory, ctx: ScreenHandlerContext) :
    IRGuiController(
        IndustrialRevolution.SMELTER_HANDLER,
        syncId,
        playerInventory,
        ctx
    ), PatchouliEntryShortcut {
    init {
        val root = WGridPanel()
        setRootPanel(root)
        configure("block.indrev.smelter", ctx, playerInventory, blockInventory, propertyDelegate)

        val inputSlot = WItemSlot.of(blockInventory, 2)
        root.add(inputSlot, 2.8, 2.2)

        val processWidget = WProcess(propertyDelegate)
        root.add(processWidget, 4.0, 2.2)

        val fluid = WFluid(ctx, 0)
        root.add(fluid, 5.8, 0.7)

        root.validate(this)
    }

    override fun canUse(player: PlayerEntity?): Boolean = true

    override fun getEntry(): Identifier = identifier("machines/basic_machines")

    override fun getPage(): Int = 3

    companion object {
        val SCREEN_ID = identifier("smelter")
    }
}