package net.fabricmc.mygolf.tools;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTool {
    /**
     * 根据类名，获取id
     * @param className 类名
     * @discuss 将大写字符转小写，且若该大写字符不是首字母，则前面添加"_"
     * @return Item的id
     */
    public static String getIdFrom(String className) {
        if(className!=null && !"".equals(className)) {
            Pattern pattern = Pattern.compile("[A-Z]");
            Matcher matcher = pattern.matcher(className);
            String result = matcher.replaceAll((MatchResult result1)-> {
                String lowerCaseStr = String.valueOf(className.charAt(result1.start())).toLowerCase();
                if(result1.start() == 0) {
                    return lowerCaseStr;
                }
                return "_" + lowerCaseStr;
            });
            return result;
        }else {
            return className;
        }
    }
}
