# Project: GrimCraft 40k

Warhammer 40k themed Minecraft mod. Guns, armor, 40k style mechanics.

## Stack
- Minecraft 26.1.2 (unobfuscated; use Mojang's official names, NOT Yarn)
- JDK 25
- Loader: NeoForge
- Build: Gradle (use ./gradlew)
- Mod id: grimcraft40k
- Package: com.grimcraft

## Critical rules
- Your training data likely covers 1.20/1.21 more than 26.1. APIs have changed.
  Do NOT write 1.21-era code from memory. Check NeoForge's 1.21.11 -> 26.1 migration primer first.
  (https://docs.neoforged.net/primer/docs/26.1/)
- If unsure an API exists in 26.1.2, search/read the decompiled sources rather than guessing.
- No remapping or Yarn mappings anywhere.

## Commands
- Build: ./gradlew build
- Run client: ./gradlew runClient
- Data gen: ./gradlew runData

## Layout
- src/main/java/com/grimcraft: all code (common + client; keep client-only classes
  gated with Dist.CLIENT / @EventBusSubscriber(value = Dist.CLIENT))
- src/main/templates/META-INF/neoforge.mods.toml: mod metadata template
  (values come from gradle.properties)
- src/main/resources/assets/grimcraft40k: textures, models, lang (hand-made)
- src/generated/resources: output of runData; don't hand-edit

## Conventions
- Localization: only the en_us language is targeted. Put all translations in
  lang/en_us.json; do not add other language files.
- I'm learning: explain non-obvious changes briefly
- Textures are made by me. Use placeholder references to
  assets/grimcraft40k/textures/item/lasgun.png and NEVER generate art.

## Current status / goals
- Just starting out; weapons come first.
- Step 1: lasgun with a "laser-like" firing effect. Hitscan mechanically.
- Hit detection is server-side raycast; beam visuals are client-side, triggered via a payload/packet.
