package net.fabricmc.mygolf.tools;

import net.minecraft.item.Item;

import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTool {
    /**
     * 根据Item类名，获取Item的id
     * @param itemClassName Item类名
     * @discuss 将大写字符转小写，且若该大写字符不是首字母，则前面添加"_"
     * @return Item的id
     */
    public static String getItemId(String itemClassName) {
        if(itemClassName!=null && !"".equals(itemClassName)) {
            Pattern pattern = Pattern.compile("[A-Z]");
            Matcher matcher = pattern.matcher(itemClassName);
            String result = matcher.replaceAll((MatchResult result1)-> {
                String lowerCaseStr = String.valueOf(itemClassName.charAt(result1.start())).toLowerCase();
                if(result1.start() == 0) {
                    return lowerCaseStr;
                }
                return "_" + lowerCaseStr;
            });
            return result;
        }else {
            return itemClassName;
        }
    }
}
