/*
package org.collisionmod.collision.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class TesterItem extends Item {

    public TesterItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }

        Vec3 eye = player.getEyePosition(1f);
        Vec3 look = player.getViewVector(1f).scale(20);
        ClipContext ctx = new ClipContext(eye, eye.add(look), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult hit = level.clip(ctx);

        if (hit.getType() != BlockHitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.literal("[Tester] No block in range"));
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);

        var props = CbcToughnessHelper.getBlockProps(level, state, pos);

        double costBase = VSCollisionConfig.COMMON.costBase.get();
        double costToughnessMult = VSCollisionConfig.COMMON.costExplosionResMult.get();

        double cost = costBase + (props.toughness() * costToughnessMult);

        String src = props.fromCbc() ? "CBC" : "fallback";
        String msg = String.format("[Tester] %s | hardness=%.2f | toughness=%.2f (%s) | cost=%.2f",
                state.getBlock().getDescriptionId(),
                props.hardness(), props.toughness(), src, cost);

        player.sendSystemMessage(Component.literal(msg));

        return InteractionResultHolder.success(player.getItemInHand(hand));

    }
}
*/