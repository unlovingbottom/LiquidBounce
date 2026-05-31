/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.features.command.commands.ingame.creative.CommandItemGive.giveItem
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.item.getCommandComponents
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.INVENTORY_LOCATION
import net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW
import net.ccbluex.liquidbounce.utils.client.player as localplayer


class ViewedInventoryScreen(private val player: () -> Player?) : Screen(PlainText.EMPTY) {

    val handler: InventoryMenu?
        get() = player()?.inventoryMenu

    private val backgroundWidth: Int = 176
    private val backgroundHeight: Int = 166
    private var x: Int = (width - backgroundWidth) / 2
    private var y: Int = (height - backgroundHeight) / 2

    override fun init() {
        x = (width - backgroundWidth) / 2
        y = (height - backgroundHeight) / 2
    }
    private var currentItemStack: ItemStack? = null

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(context, mouseX, mouseY, delta)

        val handler = handler ?: return
        context.pose().pushMatrix()
        context.pose().translate(x.toFloat(), y.toFloat())
        var hoveredSlot: Slot? = null

        for (slot in handler.slots) {
            if (slot.isActive) {
                drawSlot(context, slot)
            }

            if (isPointOverSlot(slot, mouseX.toDouble(), mouseY.toDouble()) && slot.isActive) {
                hoveredSlot = slot
                if (slot.isHighlightable) {
                    // draw slot highlight
                    context.fillGradient(
                        slot.x, slot.y,
                        slot.x + GuiRenderer.DEFAULT_ITEM_SIZE,
                        slot.y + GuiRenderer.DEFAULT_ITEM_SIZE,
                        -2130706433, -2130706433,
                    )
                }
            }
        }

        val cursorStack = handler.carried
        if (!cursorStack.isEmpty) {
            drawItem(context, cursorStack, mouseX - x - 8, mouseY - y - 8)
        }

        context.pose().popMatrix()

        if (cursorStack.isEmpty && hoveredSlot != null && hoveredSlot.hasItem()) {
            val hoveredItemStack = hoveredSlot.item
            currentItemStack = hoveredItemStack
            context.setTooltipForNextFrame(
                font, getTooltipFromItem(mc, hoveredItemStack),
                hoveredItemStack.tooltipImage, mouseX, mouseY
            )
        }
    }

    override fun extractBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        extractTransparentBackground(context)
        drawBackground(context, mouseX, mouseY)
    }

    private fun drawItem(context: GuiGraphicsExtractor, stack: ItemStack, x: Int, y: Int) {
        context.pose().withPush {
            context.item(stack, x, y)
            context.itemDecorations(font, stack, x, y, null)
        }
    }

    private fun drawBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        context.blit(
            RenderPipelines.GUI_TEXTURED, INVENTORY_LOCATION, x, y,
            0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256)
        player()?.let { player ->
            extractEntityInInventoryFollowsMouse(
                context, x + 26, y + 8, x + 75, y + 78,
                30, 0.0625f, mouseX.toFloat(), mouseY.toFloat(), player
            )
        }
    }

    private fun drawSlot(context: GuiGraphicsExtractor, slot: Slot) {
        var spriteDrawn = false

        context.pose().pushMatrix()
        context.pose().translate(0f, 0f)
        if (slot.item.isEmpty && slot.isActive) {
            val identifier = slot.noItemIcon
            if (identifier != null) {
                context.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    identifier, slot.x, slot.y,
                    GuiRenderer.DEFAULT_ITEM_SIZE, GuiRenderer.DEFAULT_ITEM_SIZE
                )
                spriteDrawn = true
            }
        }

        if (!spriteDrawn) {
            val seed = slot.x + slot.y * backgroundWidth
            if (slot.isFake) {
                context.fakeItem(slot.item, slot.x, slot.y, seed)
            } else {
                context.item(slot.item, slot.x, slot.y, seed)
            }

            context.itemDecorations(font, slot.item, slot.x, slot.y, null)
        }

        context.pose().popMatrix()
    }

    private fun isPointOverSlot(slot: Slot, pointX: Double, pointY: Double): Boolean {
        val width = GuiRenderer.DEFAULT_ITEM_SIZE
        val height = GuiRenderer.DEFAULT_ITEM_SIZE
        val pX = pointX - x
        val pY = pointY - y
        return pX >= slot.x - 1 && pX < slot.x + width + 1
            && pY >= slot.y - 1 && pY < slot.y + height + 1
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        super.keyPressed(input)

        if (mc.options.keyInventory.matches(input)) {
            onClose()
        }

        if (input.key == GLFW.GLFW_KEY_G) {
            giveItem()
        }

        if (input.key == GLFW.GLFW_KEY_C) {
            val itemStack = currentItemStack ?: return true

            val command = itemStackToGiveCommand(itemStack);
            mc.keyboardHandler.clipboard = command
            chat("Copied the give command to your clipboard.")
            if (command.length > 256) {
                chat("Command is over 256 characters long and may not fit in your chat!")
            }
        }

        return true
    }

    override fun isPauseScreen() = false

    override fun tick() {
        if (handler == null) {
            onClose()
        }
    }

    fun itemStackToGiveCommand(stack: ItemStack): String {
        val itemArgs = stack.getCommandComponents();
        val itemCount = if (stack.isEmpty) 1 else maxOf(1, stack.count)

        return ".give $itemArgs $itemCount"
    }

    private fun giveItem() {
        val translationBaseKey = "liquidbounce.command.give.result"

        val itemStack = currentItemStack ?: return
        val giveAmount = localplayer.giveItem(itemStack, itemStack.count)

        if (!localplayer.hasInfiniteMaterials()) {
            chat(translation("$translationBaseKey.mustBeCreative"))
            return
        }

        if (giveAmount == 0) {
            chat(translation(("$translationBaseKey.noEmptySlot")))
            return
        }

        chat(
            regular(
                translation(
                    "$translationBaseKey.itemGiven",
                    itemStack.displayName,
                    variable(giveAmount.toString())
                )
            )
        )
    }
}
