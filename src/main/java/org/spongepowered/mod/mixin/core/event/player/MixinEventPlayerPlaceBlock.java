/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.mod.mixin.core.event.player;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraftforge.event.world.BlockEvent;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.block.PlaceBlockEvent;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.mod.interfaces.IMixinEvent;
import org.spongepowered.mod.mixin.core.event.block.MixinEventBlock;

@NonnullByDefault
@Mixin(value = BlockEvent.PlaceEvent.class, remap = false)
public abstract class MixinEventPlayerPlaceBlock extends MixinEventBlock implements PlaceBlockEvent.SourcePlayer {

    @Shadow public EntityPlayer player;
    @Shadow public ItemStack itemInHand;
    @Shadow public net.minecraftforge.common.util.BlockSnapshot blockSnapshot;
    @Shadow public IBlockState placedBlock;
    @Shadow public IBlockState placedAgainst;

    @Override
    public Player getSourceEntity() {
        return (Player) this.player;
    }

    @Override
    public Cause getCause() {
        return Cause.of(this.player);
    }

    @Override
    public net.minecraftforge.fml.common.eventhandler.Event fromSpongeEvent(Event event) {
        PlaceBlockEvent.SourcePlayer spongeEvent = (PlaceBlockEvent.SourcePlayer) event;
        Location<World> location = spongeEvent.getTransactions().get(0).getOriginal().getLocation().get();
        net.minecraft.world.World world = (net.minecraft.world.World) location.getExtent();
        BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        BlockSnapshot replacementBlock = spongeEvent.getTransactions().get(0).getFinalReplacement();

        BlockEvent.PlaceEvent forgeEvent =
                new BlockEvent.PlaceEvent((net.minecraftforge.common.util.BlockSnapshot) replacementBlock, world.getBlockState(pos),
                        (EntityPlayer) spongeEvent.getSourceEntity());

        ((IMixinEvent) forgeEvent).setSpongeEvent(spongeEvent);
        return forgeEvent;
    }
}
