package net.fabricmc.mygolf.global;

//该文件用来声明共享字符串
public class CommonStr {
    final public static String modId = "mygolf"; //模组id

    //itemName 代号
    //! 仅支持 [a-z0-9/._-]，否则crash
    final public class ItemCodeName {
        final public static String GolfItem = "golf_item";  //高尔夫球代号
        final public static String FlagstickItem = "flagstick_item";  //红旗杆代号
        final public static String GolfClubItem = "golf_club_item";  //高尔夫球杆代号
    }


}
