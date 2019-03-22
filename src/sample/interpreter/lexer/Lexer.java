package sample.interpreter.lexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * CMM 词法分析
 * @author :  陶勇聪
 */
public class Lexer {

    /**
     * 错误数
     */
    private Integer errorCount = 0;
    /**
     * 错误信息
     */
    private String errorInfo = "";
    /**
     * 是否为注释
     */
    private boolean isAnnotation = true;

    /**
     * 用于语法分析的 Token 集合
     */
    private ArrayList<Token> tokens = new ArrayList<>();


    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    public boolean isAnnotation() {
        return isAnnotation;
    }

    public void setAnnotation(boolean annotation) {
        isAnnotation = annotation;
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

    public void setTokens(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }


    /**
     * 识别是否为字母
     * @param c 要识别的字符
     * @return bool
     */
    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    /**
     * 识别是否为数字
     * @param c 要识别的字符
     * @return bool
     */
    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * 识别是否为整数
     * @param input 要识别的字符串
     * @return bool
     */
    private static boolean isInteger(String input) {
        return input.matches("^-?\\d+$") && !input.matches("^-?0+\\\\d+$");
    }


    /**
     * 判断是否为浮点数
     * @param input 要识别的字符串
     * @return bool
     */
    private static boolean isFloat(String input) {
        return input.matches("^(-?\\d+)(\\.\\d+)+$")
                && !input.matches("^(-?0{2,}+)(\\.\\d+)+$");
    }

    /**
     * 识别是否标识符（由数字、字母和下划线组成的串，但必须以字母开头、且不能以下划线结尾的串）
     * @param input 要识别的字符串
     * @return bool
     */
    private static boolean isIdentifier(String input) {
        return input.matches("^\\w+$") && !input.endsWith("_")
                && input.substring(0, 1).matches("[A-Za-z]");
    }

    /**
     * 识别是否为保留字
     * @return bool
     * @param input 要识别的字符串
     */
    private static boolean isWord(String input) {
        return input.equals(ConstChars.IF) || input.equals(ConstChars.ELSE)
                || input.equals(ConstChars.WHILE) || input.equals(ConstChars.READ)
                || input.equals(ConstChars.WRITE) || input.equals(ConstChars.INT)
                || input.equals(ConstChars.REAL) || input.equals(ConstChars.BOOL)
                || input.equals(ConstChars.STRING) || input.equals(ConstChars.TRUE)
                || input.equals(ConstChars.FALSE) || input.equals(ConstChars.FOR);
    }

    /**
     * 找到输入字符串中的关键字或标识符
     * @param begin
     * @param string
     * @return 目标字符串的长度
     */
    private static int find(int begin, String string) {
        if (begin >= string.length()) {
            return string.length();
        }
        for (int i = begin; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '\n' || c == ',' || c == ' ' || c == '\t' || c == '{'
                    || c == '}' || c == '(' || c == ')' || c == ';' || c == '='
                    || c == '+' || c == '-' || c == '*' || c == '/' || c == '['
                    || c == ']' || c == '<' || c == '>') {
                return i - 1;
            }
        }
        return string.length();
    }


    /**
     * 扫描分析一行 cmm 文件字符串
     * @param lineText 一行 cmm 文件字符串
     * @param rowNum 所在的行数
     * @return 返回生成的 TreeNode
     */
    private TreeNode scanLine(String lineText, int rowNum) {
        // 创建当前行根结点
        String content = "第" + rowNum + "行： " + lineText;
        TreeNode node = new TreeNode(content);
        // 词法分析每行结束的标志
        lineText += "\n";
        int length = lineText.length();
        // switch状态值
        int state = 0;
        // 记录token开始位置
        int begin = 0;
        // 记录token结束位置
        int end = 0;
        // 逐个读取当前行字符，进行分析，如果不能判定，向前多看k位
        for (int i = 0; i < length; i++) {
            char ch = lineText.charAt(i);
            if (!isAnnotation) {
                if (ch == '(' || ch == ')' || ch == ';' || ch == '{'
                        || ch == '}' || ch == '[' || ch == ']' || ch == ','
                        || ch == '+' || ch == '-' || ch == '*' || ch == '/'
                        || ch == '=' || ch == '<' || ch == '>' || ch == '"'
                        || isLetter(ch) || isDigit(ch)
                        || " ".equals(String.valueOf(ch))
                        || "\n".equals(String.valueOf(ch))
                        || "\r".equals(String.valueOf(ch))
                        || "\t".equals(String.valueOf(ch))) {
                    switch (state) {
                        case 0:
                            // 分隔符直接打印
                            if (ch == '(' || ch == ')' || ch == ';' || ch == '{'
                                    || ch == '}' || ch == '[' || ch == ']'
                                    || ch == ',') {
                                state = 0;
                                tokens.add(new Token(rowNum, i + 1, "分隔符", String
                                        .valueOf(ch)));

                            }
                            // 加号+
                            else if (ch == '+') {
                                state = 1;
                            }
                                // 减号-
                            else if (ch == '-') {
                                state = 2;
                            }
                                // 乘号*
                            else if (ch == '*') {
                                state = 3;
                            }
                                // 除号/
                            else if (ch == '/') {
                                state = 4;
                            }
                                // 赋值符号==或者等号=
                            else if (ch == '=') {
                                state = 5;
                            }
                                // 小于符号<或者不等于<>
                            else if (ch == '<') {
                                state = 6;
                            }
                                // 大于>
                            else if (ch == '>') {
                                state = 9;
                            }
                                // 关键字或者标识符
                            else if (isLetter(ch)) {
                                state = 7;
                                begin = i;
                            }
                            // 整数或者浮点数
                            else if (isDigit(ch)) {
                                begin = i;
                                state = 8;
                            }
                            // 双引号"
                            else if (String.valueOf(ch).equals(ConstChars.DQ)) {
                                begin = i + 1;
                                state = 10;
                                node.add(new TreeNode("分隔符 ： " + ch));
                                tokens.add(new Token(rowNum, begin, "分隔符",
                                        ConstChars.DQ));

                            }
                            // 空白符
                            else if (" ".equals(String.valueOf(ch))) {
                                state = 0;

                            }
                            // 换行符
                            else if ("\n".equals(String.valueOf(ch))) {
                                state = 0;

                            }
                            // 回车符
                            else if ("\r".equals(String.valueOf(ch))) {
                                state = 0;

                            }
                            // 制表符
                            else if ("\t".equals(String.valueOf(ch))) {
                                state = 0;
                            }
                            break;
                        case 1:
                            node.add(new TreeNode("运算符 ： " + ConstChars.PLUS));
                            tokens.add(new Token(rowNum, i, "运算符", ConstChars.PLUS));
                            i--;
                            state = 0;
                            break;
                        case 2:
                            String temp = tokens.get(tokens.size() - 1).getType();
                            String c = tokens.get(tokens.size() - 1).getContent();
                            if ("整数".equals(temp) || "标识符".equals(temp)
                                    || "实数".equals(temp) || ")".equals(c)
                                    || "]".equals(c)) {
                                node.add(new TreeNode("运算符 ： " + ConstChars.MINUS));
                                tokens.add(new Token(rowNum, i, "运算符",
                                        ConstChars.MINUS));
                                i--;
                                state = 0;
                            } else if ("\n".equals(String.valueOf(ch))) {

                            } else {
                                begin = i - 1;
                                state = 8;
                            }
                            break;
                        case 3:
                            if (ch == '/') {
                                errorCount++;
                                errorInfo += "    ERROR:第 " + rowNum + " 行,第 " + i
                                        + " 列：" + "运算符\"" + ConstChars.TIMES
                                        + "\"使用错误  \n";
                                node.add(new TreeNode(ConstChars.ERROR + "运算符\""
                                        + ConstChars.TIMES + "\"使用错误"));
                            } else {
                                node.add(new TreeNode("运算符 ： " + ConstChars.TIMES));

                                tokens.add(new Token(rowNum, i, "运算符",
                                        ConstChars.TIMES));
                                i--;
                            }
                            state = 0;
                            break;
                        case 4:
                            if (ch == '/') {
                                i = length - 2;
                                state = 0;
                            } else if (ch == '*') {

                                begin = i + 1;
                                isAnnotation = true;
                            } else {
                                node.add(new TreeNode("运算符 ： " + ConstChars.DIVIDE));

                                tokens.add(new Token(rowNum, i, "运算符",
                                        ConstChars.DIVIDE));
                                i--;
                                state = 0;
                            }
                            break;
                        case 5:
                            if (ch == '=') {
                                node.add(new TreeNode("运算符 ： " + ConstChars.EQUAL));

                                tokens.add(new Token(rowNum, i,"运算符",
                                        ConstChars.EQUAL));

                                state = 0;
                            } else {
                                state = 0;
                                node.add(new TreeNode("运算符 ： " + ConstChars.ASSIGN));

                                tokens.add(new Token(rowNum, i, "运算符",
                                        ConstChars.ASSIGN));

                                i--;
                            }
                            break;
                        case 6:
                            if (ch == '>') {
                                node.add(new TreeNode("运算符 ： " + ConstChars.NEQUAL));

                                tokens.add(new Token(rowNum, i, "运算符",
                                        ConstChars.NEQUAL));

                                state = 0;
                            } else {
                                state = 0;
                                node.add(new TreeNode("运算符 ： " + ConstChars.LT));

                                tokens.add(new Token(rowNum, i, "运算符",
                                                ConstChars.LT));

                                i--;
                            }
                            break;
                        case 7:
                            if (isLetter(ch) || isDigit(ch)) {
                                state = 7;
                            } else {
                                end = i;
                                String id = lineText.substring(begin, end);
                                if (isWord(id)) {
                                    node.add(new TreeNode("关键字 ： " + id));

                                    tokens.add(new Token(rowNum, begin + 1, "关键字",
                                            id));

                                } else if (isIdentifier(id)) {
                                    node.add(new TreeNode("标识符 ： " + id));

                                    tokens.add(new Token(rowNum, begin + 1, "标识符",
                                            id));

                                } else {
                                    errorCount++;
                                    errorInfo += "    ERROR:第 " + rowNum + " 行,第 "
                                            + (begin + 1) + " 列：" + id + "是非法标识符\n";
                                    node.add(new TreeNode(ConstChars.ERROR + id
                                            + "是非法标识符"));

                                }
                                i--;
                                state = 0;
                            }
                            break;
                        case 8:
                            if (isDigit(ch) || ".".equals(String.valueOf(ch))) {
                                state = 8;
                            } else {
                                if (isLetter(ch)) {
                                    errorCount++;
                                    errorInfo += "    ERROR:第 " + rowNum + " 行,第 "
                                            + i + " 列：" + "数字格式错误或者标志符错误\n";
                                    node.add(new TreeNode(ConstChars.ERROR
                                            + "数字格式错误或者标志符错误"));

                                    i = find(begin, lineText);
                                } else {
                                    end = i;
                                    String id = lineText.substring(begin, end);
                                    if (!id.contains(".")) {
                                        if (isInteger(id)) {
                                            node.add(new TreeNode("整数    ： " + id));

                                            tokens.add(new Token(rowNum, begin + 1, "整数", id));
                                        } else {
                                            errorCount++;
                                            errorInfo += "    ERROR:第 " + rowNum
                                                    + " 行,第 " + (begin + 1) + " 列："
                                                    + id + "是非法实数\n";
                                            node.add(new TreeNode(ConstChars.ERROR
                                                    + id + "是非法整数"));
                                        }
                                    } else {
                                        if (isFloat(id)) {
                                            node.add(new TreeNode("实数    ： " + id));

                                            tokens.add(new Token(rowNum,
                                                    begin + 1, "实数", id));

                                        } else {
                                            errorCount++;
                                            errorInfo += "    ERROR:第 " + rowNum
                                                    + " 行,第 " + (begin + 1) + " 列："
                                                    + id + "是非法实数\n";
                                            node.add(new TreeNode(ConstChars.ERROR
                                                    + id + "是非法实数"));

                                        }
                                    }
                                    i = find(i, lineText);
                                }
                                state = 0;
                            }
                            break;
                        case 9:
                            tokens.add(new Token(rowNum, i, "运算符", ConstChars.GT));
                            node.add(new TreeNode("运算符 ： " + ConstChars.GT));
                            i--;
                            state = 0;
                            break;
                        case 10:
                            if (ch == '"') {
                                end = i;
                                String string = lineText.substring(begin, end);
                                node.add(new TreeNode("字符串 ： " + string));
                                tokens.add(new Token(rowNum, begin + 1,"字符串" ,
                                        string));
                                node.add(new TreeNode("分隔符 ： " + ConstChars.DQ));
                                tokens.add(new Token(rowNum, end + 1, "分隔符",
                                        ConstChars.DQ));
                                state = 0;
                            } else if (i == length - 1) {
                                String string = lineText.substring(begin);
                                errorCount++;
                                errorInfo += "    ERROR:第 " + rowNum + " 行,第 "
                                        + (begin + 1) + " 列：" + "字符串 " + string
                                        + " 缺少引号  \n";
                                node.add(new TreeNode(ConstChars.ERROR + "字符串 "
                                        + string + " 缺少引号  \n"));
                            }
                            break;
                            default: break;
                    }
                } else {
                    boolean b = ch > 19967 && ch < 40870 || ch == '\\' || ch == '~'
                            || ch == '`' || ch == '|' || ch == '、'
                            || ch == '?' || ch == '&' || ch == '^' || ch == '%'
                            || ch == '$' || ch == '@' || ch == '!' || ch == '#'
                            || ch == '；' || ch == '【' || ch == '】' || ch == '，'
                            || ch == '。' || ch == '“' || ch == '”' || ch == '‘'
                            || ch == '’' || ch == '？' || ch == '（' || ch == '）'
                            || ch == '《' || ch == '》' || ch == '·';
                    if (b) {
                        errorCount++;
                        errorInfo += "    ERROR:第 " + rowNum + " 行,第 "
                                + (i + 1) + " 列：" + "\"" + ch + "\"是不可识别符号  \n";
                        node.add(new TreeNode(ConstChars.ERROR + "\"" + ch
                                + "\"是不可识别符号"));
                        if (state == 0) {
                        }
                    }
                }
            } else {
                if (ch == '*') {
                    state = 3;
                } else if (ch == '/' && state == 3) {
                    state = 0;
                    isAnnotation = false;
                } else if (i == length - 2) {
                    state = 0;
                } else {
                    state = 0;
                }
            }
        }
        return node;
    }

    /**
     * 扫描整个cmm程序，得到树
     * @param cmmText cmm程序
     * @return 树的根节点
     */
    public void scanAll(String cmmText) {
        setErrorInfo("");
        setErrorCount(0);
        setTokens(new ArrayList<>());
        setAnnotation(false);
        StringReader stringReader = new StringReader(cmmText);
        TreeNode root = new TreeNode("PROGRAM");
        String eachLine = "";
        int lineNum = 1;
        BufferedReader reader = new BufferedReader(stringReader);
        while (eachLine != null) {
            try {
                eachLine = reader.readLine();
                if (eachLine != null) {
                    if (isAnnotation() && !eachLine.contains("*/")) {
                        eachLine += "\n";
                        TreeNode temp = new TreeNode(eachLine);
                        root.add(temp);
                        lineNum++;
                        continue;
                    } else {
                        root.add(scanLine(eachLine, lineNum));
                    }
                }
                lineNum++;
            } catch (IOException e) {
                System.err.println("读取文本时出错了！");
            }
        }
        //return root;
    }
}
