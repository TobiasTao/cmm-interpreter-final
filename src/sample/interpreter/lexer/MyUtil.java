package sample.interpreter.lexer;


import java.lang.reflect.Field;

/**
 * @author :  TobaisTao
 */
public  class MyUtil {

    /**
     * 利用反射获取种别编码
     * @param token  输入
     * @return	种别编码
     */
    public static Integer getTypeNum(Token token) throws IllegalAccessException {
        ConstChars constChars = new ConstChars();
        Integer type = 0;
        String input = token.getContent();
        if("整数".equals(token.getType())){
            type = 31;
        }else if ("实数".equals(token.getType())){
            type = 32;
        }else if ("字符串".equals(token.getType())){
            type = 33;
        }else if ("标识符".equals(token.getType())){
            type = 34;
        }else {
            Field[] fields = ConstChars.class.getDeclaredFields();
            type =0;
            for (Field field : fields) {
                type++;
                if (field.get(constChars).equals(input)) {
                    return type;
                }
            }
        }
        return type;
    }
}
