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
        return new Settings().maxCount(1).group(ItemGroup.MISC);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        final var itemStack = user.getStackInHand(hand);
        ArrayList<String> list = new ArrayList<>();
        list.add(bookChar("\n\n\n\n§4高尔夫球介绍\n" +
                "§6游戏规则：挥动球杆将球打入红旗杆处球洞中"));
        list.add(bookChar("§3高尔夫球\n§6右键放置\n§6空手或手持高尔夫球时，右键高尔夫球实体可回收\n\n" +
                "§3红旗杆\n§6对着泥土或草方块使用\n\n" +
                "§3高尔夫球杆\n§6靠近高尔夫球右键蓄力击打"));
        user.giveItemStack(writtenBook("mygolf", "高尔夫球介绍", list, true, "高尔夫球说明"));
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

    private static String bookChar(String s) {
        return Text.Serializer.toJson(Text.of(s));
    }

}
