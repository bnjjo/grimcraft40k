package com.grimcraft.network;

import com.grimcraft.GrimCraft40k;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Server -> client: "draw a laser beam from start to end". Purely visual; damage is already resolved. */
public record LaserBeamPayload(int shooterId, Vec3 start, Vec3 end) implements CustomPacketPayload {
    public static final Type<LaserBeamPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(GrimCraft40k.MODID, "laser_beam"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LaserBeamPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LaserBeamPayload::shooterId,
            Vec3.STREAM_CODEC, LaserBeamPayload::start,
            Vec3.STREAM_CODEC, LaserBeamPayload::end,
            LaserBeamPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
