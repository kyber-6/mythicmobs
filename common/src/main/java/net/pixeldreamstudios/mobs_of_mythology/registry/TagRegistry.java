package net.pixeldreamstudios.mobs_of_mythology.registry;

import dev.architectury.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public record TagRegistry() {
    // BLOCK TAGS
    public static final TagKey<Block> BRONZE_BLOCKS = TagKey.create(Registries.BLOCK, new ResourceLocation("c", "bronze_blocks"));

    // ITEM TAGS
    public static final TagKey<Item> PICKAXES = TagKey.create(Registries.ITEM, new ResourceLocation("c", "pickaxes"));
    public static final TagKey<Item> BRONZE_INGOTS = TagKey.create(Registries.ITEM, new ResourceLocation("c", "bronze_ingots"));

    // BIOME TAGS
    public static TagKey<Biome> WET_BIOMES = TagKey.create(Registries.BIOME, Platform.isForge() ?  new ResourceLocation("forge", "is_wet")
            : new ResourceLocation("c", "climate_wet"));
    public static TagKey<Biome> BADLANDS_BIOMES = TagKey.create(Registries.BIOME,Platform.isForge() ?  new ResourceLocation("forge", "is_dry")
            :  new ResourceLocation("c", "badlands"));
    public static TagKey<Biome> MUSHROOM_BIOMES = TagKey.create(Registries.BIOME,Platform.isForge() ?  new ResourceLocation("forge", "is_mushroom")
            :  new ResourceLocation("c", "mushroom"));
    public static TagKey<Biome> TEMPERATE_BIOMES = TagKey.create(Registries.BIOME,Platform.isForge() ?  new ResourceLocation("forge", "is_hot")
            :  new ResourceLocation("c", "climate_temperate"));

}
