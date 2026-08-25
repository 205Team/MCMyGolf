package net.fabricmc.mygolf.items;

import net.fabricmc.mygolf.items.base.BaseItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.ArrayList;

public class IntroBook extends BaseItem {
    public IntroBook(Settings settings) {
        super(settings);
    }

    public static IntroBook defaultInstance() {
        return new IntroBook(defaultSetting());
    }

    //默认设置
    private static Settings defaultSetting() {
        return new Settings().maxCount(1);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        final var itemStack = user.getStackInHand(hand);
        ArrayList<String> list = new ArrayList<>();
        list.add(bookChar("book.mygolf.guide.page1"));
        list.add(bookChar("book.mygolf.guide.page2"));

        String filtterd_title = Text.translatable("book.mygolf.guide.filtterd_title").getString();
        String title = Text.translatable("book.mygolf.guide.title").getString();
        user.giveItemStack(writtenBook("mygolf", filtterd_title, list, true, title));
        itemStack.decrement(1);
        return super.use(world, user, hand);
    }

    ///返回成书nbt
    private static ItemStack writtenBook(String author, String filtterd_title, ArrayList<String> pages, boolean resolved, String title) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound baseNbt = new NbtCompound();
        baseNbt.putString("author", author);
        baseNbt.putString("filtterd_title", filtterd_title);
        NbtList pageList = new NbtList();
        for (String s : pages) {
            NbtString string = NbtString.of(s);
            pageList.add(string);
        }
        baseNbt.put("pages", pageList);
        baseNbt.putBoolean("resolved", resolved);
        baseNbt.putString("title", title);
        stack.setNbt(baseNbt);
        return stack;
    }

    private static String bookChar(String translationKey) {
        return Text.Serializer.toJson(Text.translatable(translationKey));
    }

}
