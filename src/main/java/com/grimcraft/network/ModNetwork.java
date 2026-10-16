package com.grimcraft.network;

import com.grimcraft.GrimCraft40k;
import com.grimcraft.client.LaserBeamClient;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = GrimCraft40k.MODID)
public class ModNetwork {
    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        // The handler lambda only touches the client class when actually invoked (on the client),
        // so dedicated servers never load LaserBeamClient.
        registrar.playToClient(LaserBeamPayload.TYPE, LaserBeamPayload.STREAM_CODEC,
                (payload, context) -> LaserBeamClient.handle(payload));
    }
}
