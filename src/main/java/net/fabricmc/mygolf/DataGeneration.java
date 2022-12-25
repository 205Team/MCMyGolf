package net.fabricmc.mygolf;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/// 数据生成
public class DataGeneration implements DataGeneratorEntrypoint {
    private static final TagKey<Item> SMELLY_ITEMS = TagKey.of(Registry.ITEM_KEY, new Identifier(CommonStr.modId, "smelly_items"));

    ///tag生成
    private static class MyTagGenerator extends FabricTagProvider<Item> {
        public MyTagGenerator(FabricDataGenerator dataGenerator) {
            super(dataGenerator, Registry.ITEM);  // for versions 1.19.2 and below, use Registry.ITEM
        }

        @Override
        protected void generateTags() {
            getOrCreateTagBuilder(SMELLY_ITEMS)
                    .add(Items.SLIME_BALL)
                    .add(Items.ROTTEN_FLESH)
                    .addOptionalTag(ItemTags.DIRT);
        }
    }
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        fabricDataGenerator.addProvider(MyTagGenerator::new);
    }
}
