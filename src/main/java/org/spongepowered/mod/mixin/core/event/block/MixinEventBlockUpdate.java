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
package org.spongepowered.mod.mixin.core.event.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.BlockEvent.NeighborNotifyEvent;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTransaction;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.block.UpdateNeighborBlockEvent;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.registry.SpongeGameRegistry;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.mod.interfaces.IMixinEvent;

import java.util.EnumSet;
import java.util.List;

@NonnullByDefault
@Mixin(value = BlockEvent.NeighborNotifyEvent.class, remap = false)
public abstract class MixinEventBlockUpdate extends MixinEventBlock implements UpdateNeighborBlockEvent.SourceBlock {

    @Shadow private EnumSet<EnumFacing> notifiedSides;

    @Override
    public ImmutableList<BlockTransaction> generateTransactions() {
        List<BlockTransaction> transactions = Lists.newArrayList();
        for (EnumFacing notifiedSide : this.notifiedSides) {
            BlockPos offset = this.pos.offset(notifiedSide);
            BlockSnapshot replacementSnapshot =
                    ((World) this.world).createSnapshot(VecHelper.toVector(offset)).withState(BlockTypes.AIR.getDefaultState());
            BlockSnapshot originalSnapshot = ((World) this.world).createSnapshot(VecHelper.toVector(offset));
            transactions.add(new BlockTransaction(originalSnapshot, replacementSnapshot));
        }
        this.blockTransactions = ImmutableList.copyOf(transactions);
        return this.blockTransactions;
    }

    @Override
    public net.minecraftforge.fml.common.eventhandler.Event fromSpongeEvent(Event event) {
        UpdateNeighborBlockEvent.SourceBlock spongeEvent = (UpdateNeighborBlockEvent.SourceBlock) event;
        EnumSet<EnumFacing> facings = EnumSet.noneOf(EnumFacing.class);
        net.minecraft.world.World world =
                (net.minecraft.world.World) spongeEvent.getTransactions().get(0).getOriginal().getLocation().get().getExtent();
        Location<World> targetLocation = spongeEvent.getTransactions().get(0).getOriginal().getLocation().get();

        for (BlockTransaction transaction : spongeEvent.getTransactions()) {
            for (Direction direction : Direction.values()) {
                Location<World> location = transaction.getOriginal().getLocation().get();
                if ((direction.isCardinal() || direction == Direction.UP || direction == Direction.DOWN)
                        && location.equals(location.getRelative(direction))) {
                    facings.add(SpongeGameRegistry.directionMap.get(direction));
                }
            }
        }

        final NeighborNotifyEvent forgeEvent = new NeighborNotifyEvent(world,
                VecHelper.toBlockPos(targetLocation.getBlockPosition()), (IBlockState) targetLocation.getBlock(), facings);
        ((IMixinEvent) forgeEvent).setSpongeEvent(spongeEvent);
        return forgeEvent;
    }
}
