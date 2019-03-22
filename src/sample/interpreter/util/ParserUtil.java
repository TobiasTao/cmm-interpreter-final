package sample.interpreter.util;

import sample.interpreter.lexer.TreeNode;

/**
 * @author :  TobaisTao
 */
public class ParserUtil {
    public StringBuilder getStringBuilder() {
        return stringBuilder;
    }

    public void setStringBuilder(StringBuilder stringBuilder) {
        this.stringBuilder = stringBuilder;
    }

    public StringBuilder stringBuilder = new StringBuilder("");

    public   void lastOrder(TreeNode root, int depth){
        String str = "";
        for (int i = 0; i < depth; i++ ){
            str= str + "    ";
        }
        //System.out.println(str + root.getContent());
        stringBuilder.append(str).append(root.getContent()).append("\n");
        int childrenCount = root.getChildCount();
        for (int i = 0; i < childrenCount; i++){
            lastOrder(root.getChildAt(i), depth+1);
        }
    }
}
