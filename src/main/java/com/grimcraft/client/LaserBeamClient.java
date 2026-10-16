package com.grimcraft.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.grimcraft.GrimCraft40k;
import com.grimcraft.network.LaserBeamPayload;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * Client-only: keeps a list of live laser beams and draws each one every frame using the
 * vanilla beacon beam renderer, with a radius that grows quickly and then shrinks to nothing.
 */
@EventBusSubscriber(modid = GrimCraft40k.MODID, value = Dist.CLIENT)
public class LaserBeamClient {
    /** Total lifetime of a beam, in ticks. */
    private static final float LIFETIME = 8.0f;
    /** Fraction of the lifetime spent growing to full width; the rest is spent shrinking away. */
    private static final float GROW_FRACTION = 0.2f;
    // Vanilla beacon is 0.2 / 0.25. The lasgun is a thinner beam.
    private static final float MAX_SOLID_RADIUS = 0.08f;
    private static final float MAX_GLOW_RADIUS = 0.12f;
    private static final int COLOR = 0xFFFF2A2A; // ARGB

    /**
     * Where the muzzle appears in first person, in camera space: (right, up, forward) in blocks.
     * Tune these in-game until the beam leaves the top-left pixel of the held sprite.
     */
    private static final Vector3f MUZZLE_OFFSET = new Vector3f(0.30f, -0.20f, 0.80f);

    private record Beam(int shooterId, Vec3 start, Vec3 end, long spawnTick) {}

    private static final List<Beam> BEAMS = new ArrayList<>();

    /** Called from the network handler (on the client main thread). */
    public static void handle(LaserBeamPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        BEAMS.add(new Beam(payload.shooterId(), payload.start(), payload.end(), level.getGameTime()));
    }

    @SubscribeEvent
    static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            BEAMS.clear();
            return;
        }

        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float now = level.getGameTime() + partial;
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();

        BEAMS.removeIf(b -> now - b.spawnTick() >= LIFETIME);

        for (Beam beam : BEAMS) {
            float age = now - beam.spawnTick();
            float t = Mth.clamp(age / LIFETIME, 0f, 1f);

            // Radius profile: quick ease-out grow, then a smooth shrink down to 0.
            float size;
            if (t < GROW_FRACTION) {
                float g = t / GROW_FRACTION;
                size = 1f - (1f - g) * (1f - g);
            } else {
                float s = (t - GROW_FRACTION) / (1f - GROW_FRACTION);
                size = 1f - s * s;
            }
            if (size <= 0.01f) continue;

            Vec3 start = beam.start();
            // First person, our own shot: anchor to the camera so the beam follows the view
            // and comes out of the gun model instead of a server-guessed point.
            if (mc.player != null && beam.shooterId() == mc.player.getId()
                    && mc.options.getCameraType().isFirstPerson()) {
                // Camera space: +X right, +Y up, -Z forward. Rotate it into world space.
                Vector3f off = new Vector3f(MUZZLE_OFFSET.x, MUZZLE_OFFSET.y, -MUZZLE_OFFSET.z)
                        .rotate(event.getLevelRenderState().cameraRenderState.orientation);
                start = cam.add(off.x, off.y, off.z);
            }

            Vec3 delta = beam.end().subtract(start);
            double length = delta.length();
            if (length < 1.0E-3) continue;
            Vec3 dir = delta.scale(1.0 / length);

            // The vanilla beam is drawn upward (+Y) from the origin, centred on (0.5, 0, 0.5).
            // So: move to the barrel, rotate +Y onto our direction, and undo the 0.5 offset.
            int height = Math.max(1, (int) Math.round(length));
            poseStack.pushPose();
            poseStack.translate(start.x - cam.x, start.y - cam.y, start.z - cam.z);
            poseStack.mulPose(new Quaternionf().rotationTo(0f, 1f, 0f, (float) dir.x, (float) dir.y, (float) dir.z));
            poseStack.scale(1f, (float) (length / height), 1f); // stretch so the length is exact
            poseStack.translate(-0.5, 0, -0.5);

            BeaconRenderer.submitBeaconBeam(poseStack, event.getSubmitNodeCollector(),
                    BeaconRenderer.BEAM_LOCATION, 1.0f, now, 0, height, COLOR,
                    MAX_SOLID_RADIUS * size, MAX_GLOW_RADIUS * size);
            poseStack.popPose();
        }
    }
}
