package com.grimcraft.item;

import com.grimcraft.network.LaserBeamPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class LasgunItem extends Item {
    public static final double RANGE = 64.0;
    public static final float DAMAGE = 8.0f;
    public static final int COOLDOWN_TICKS = 10;

    public LasgunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // All hit detection is server-side; the client just gets the SUCCESS result (arm swing).
        if (level instanceof ServerLevel serverLevel) {
            fire(serverLevel, player);
            player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
        }
        return InteractionResult.SUCCESS;
    }

    private static void fire(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        Vec3 maxEnd = eye.add(look.scale(RANGE));

        // 1) How far does the beam get before a block stops it?
        BlockHitResult blockHit = level.clip(
                new ClipContext(eye, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getLocation(); // equals maxEnd if nothing was hit

        // 2) Is there an entity in the way, before that point?
        AABB searchBox = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level, player, eye, end, searchBox,
                (Entity e) -> !e.isSpectator() && e.isPickable(), 0.0f);

        if (entityHit != null) {
            end = entityHit.getLocation();
            entityHit.getEntity().hurtServer(level, level.damageSources().playerAttack(player), DAMAGE);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.6f, 1.8f);

        // Fallback start (used for other players / third person). The shooter's own first-person
        // view replaces this on the client with a muzzle position (see LaserBeamClient).
        Vec3 visualStart = eye.add(look.scale(0.6)).add(0, -0.15, 0);
        PacketDistributor.sendToPlayersNear(level, null, eye.x, eye.y, eye.z, RANGE + 16.0,
                new LaserBeamPayload(player.getId(), visualStart, end));
    }
}
