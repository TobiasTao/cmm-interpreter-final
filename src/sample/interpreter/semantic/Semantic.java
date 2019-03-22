package sample.interpreter.semantic;


import sample.interpreter.lexer.ConstChars;
import sample.interpreter.lexer.TreeNode;
import sample.view.RootLayoutController;

import java.math.BigDecimal;

/**
 * CMM语义分析器
 *
 * @author Administrator
 */
public class Semantic extends Thread {
    public String output = "";
    /**
     * 语义分析时的符号表
     */
    private SymbolTable table = new SymbolTable();
    /**
     * 语法分析得到的抽象语法树
     */
    private TreeNode root;
    /**
     * 语义分析错误信息
     */
    private String errorInfo = "";
    /**
     * 语义分析错误个数
     */
    private int errorNum = 0;
    /**
     * 语义分析标识符作用域
     */
    private int level = 0;
    /**
     * 用户输入
     */
    private String userInput;

    private RootLayoutController controller;

    public Semantic(TreeNode root) {
        this.root = root;
    }

    public Semantic(TreeNode root, RootLayoutController controller){
        this.root = root;
        this.controller = controller;
    }

    /**
     * 识别正确的整数：排除多个零的情况
     *
     * @param input 要识别的字符串
     * @return 布尔值
     */
    private static boolean matchInteger(String input) {
        return input.matches("^-?\\d+$") && !input.matches("^-?0{1,}\\d+$");
    }

    /**
     * 识别正确的浮点数：排除00.000的情况
     *
     * @param input 要识别的字符串
     * @return 布尔值
     */
    private static boolean matchReal(String input) {
        return input.matches("^(-?\\d+)(\\.\\d+)+$")
                && !input.matches("^(-?0{2,}+)(\\.\\d+)+$");
    }

    public String getOutput() {
        return output;
    }

    private void error(String error, int line) {
        errorNum++;
        String s = ConstChars.ERROR + "第 " + line + " 行：" + error + "\n";
        errorInfo += s;
    }

    /**
     * 设置用户输入
     *
     * @param userInput 输入的内容
     */
    public synchronized void setUserInput(String userInput) {
        this.userInput = userInput;
        notify();
    }

    /**
     * 读取用户输入
     *
     * @return 返回用户输入内容的字符串形式
     */
    private synchronized String readInput() {
        String result ;
        try {
            while (userInput == null) {
                wait();
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        result = userInput;
        userInput = null;
        return result;
    }

    /**
     * 进程运行时执行的方法
     */
    @Override
    public void run() {
        table.removeAll();
        statement(root);
        //output = output + "\n";
        controller.resultContent.appendText("**********语义分析结果**********\n");
        //output = output + "**********语义分析结果**********\n";
        if (errorNum != 0) {
            //output = output + errorInfo;
            controller.resultContent.appendText(errorInfo+"\n");
            //output = output + "该程序中共有" + errorNum + "个语义错误！\n";
            controller.resultContent.appendText("该程序中共有" + errorNum + "个语义错误！\n");
            //output = output + "程序进行语义分析时发现错误，请修改！";
            controller.resultContent.appendText("程序进行语义分析时发现错误，请修改！");
        } else {
            //output = output + "该程序中共有" + errorNum + "个语义错误！\n";
            controller.resultContent.appendText("该程序中共有" + errorNum + "个语义错误！\n");
        }
    }

    /**
     * 语义分析主方法
     *
     * @param root 根结点
     */
    private void statement(TreeNode root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            TreeNode currentNode = root.getChildAt(i);
            String content = currentNode.getContent();
            switch (content) {
                case ConstChars.INT:
                case ConstChars.REAL:
                case ConstChars.BOOL:
                case ConstChars.STRING:
                    forDeclare(currentNode);
                    break;
                case ConstChars.ASSIGN:
                    forAssign(currentNode);
                    break;
                case ConstChars.FOR:
                    // 进入for循环语句，改变作用域
                    level++;
                    forFor(currentNode);
                    // 退出for循环语句，改变作用域并更新符号表
                    level--;
                    table.update(level);
                    break;
                case ConstChars.IF:
                    // 进入if语句，改变作用域
                    level++;
                    forIf(currentNode);
                    // 退出if语句，改变作用域并更新符号表
                    level--;
                    table.update(level);
                    break;
                case ConstChars.WHILE:
                    // 进入while语句，改变作用域
                    level++;
                    forWhile(currentNode);
                    // 退出while语句，改变作用域并更新符号表
                    level--;
                    table.update(level);
                    break;
                case ConstChars.READ:
                    forRead(currentNode.getChildAt(0));
                    break;
                case ConstChars.WRITE:
                    forWrite(currentNode.getChildAt(0));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 分析declare语句
     *
     * @param root 根结点
     */
    private void forDeclare(TreeNode root) {
        // 结点显示的内容,即声明变量的类型int real bool string
        String content = root.getContent();
        int index = 0;
        while (index < root.getChildCount()) {
            TreeNode temp = root.getChildAt(index);
            // 变量名
            String name = temp.getContent();
            // 判断变量是否已经被声明
            if (table.getCurrentLevel(name, level) == null) {
                // 声明普通变量
                if (temp.getChildCount() == 0) {
                    SymbolTableElement element = new SymbolTableElement(temp
                            .getContent(), content, temp.getRowNum(), level);
                    index++;
                    // 判断变量是否在声明时被初始化
                    if (index < root.getChildCount()
                            && root.getChildAt(index).getContent().equals(
                            ConstChars.ASSIGN)) {
                        // 获得变量的初始值结点
                        TreeNode valueNode = root.getChildAt(index).getChildAt(
                                0);
                        String value = valueNode.getContent();
                        switch (content) {
                            // 声明int型变量
                            case ConstChars.INT:
                                if (matchInteger(value)) {
                                    element.setIntValue(value);
                                    element.setRealValue(String.valueOf(Double
                                            .parseDouble(value)));
                                } else if (matchReal(value)) {
                                    String error = "不能将浮点数赋值给整型变量";
                                    error(error, valueNode.getRowNum());
                                } else if ("true".equals(value)
                                        || "false".equals(value)) {
                                    String error = "不能将" + value + "赋值给整型变量";
                                    error(error, valueNode.getRowNum());
                                } else if ("字符串".equals(valueNode.getNodeType())) {
                                    String error = "不能将字符串赋值给整型变量";
                                    error(error, valueNode.getRowNum());
                                } else if ("标识符".equals(valueNode.getNodeType())) {
                                    if (checkID(valueNode, level)) {
                                        switch (table.getAllLevel(
                                                valueNode.getContent(), level)
                                                .getKind()) {
                                            case ConstChars.INT:
                                                element.setIntValue(table.getAllLevel(
                                                        valueNode.getContent(), level)
                                                        .getIntValue());
                                                element.setRealValue(table.getAllLevel(
                                                        valueNode.getContent(), level)
                                                        .getRealValue());
                                                break;
                                            case ConstChars.REAL: {
                                                String error = "不能将浮点型变量赋值给整型变量";
                                                error(error, valueNode.getRowNum());
                                                break;
                                            }
                                            case ConstChars.BOOL: {
                                                String error = "不能将布尔型变量赋值给整型变量";
                                                error(error, valueNode.getRowNum());
                                                break;
                                            }
                                            case ConstChars.STRING: {
                                                String error = "不能将字符串变量赋值给整型变量";
                                                error(error, valueNode.getRowNum());
                                                break;
                                            }
                                            default:
                                                break;
                                        }

                                    } else {
                                        return;
                                    }
                                } else if (value.equals(ConstChars.PLUS)
                                        || value.equals(ConstChars.MINUS)
                                        || value.equals(ConstChars.TIMES)
                                        || value.equals(ConstChars.DIVIDE)) {
                                    String result = forExpression(valueNode);
                                    if (result != null) {
                                        if (matchInteger(result)) {
                                            element.setIntValue(result);
                                            element.setRealValue(String
                                                    .valueOf(Double
                                                            .parseDouble(result)));
                                        } else if (matchReal(result)) {
                                            String error = "不能将浮点数赋值给整型变量";
                                            error(error, valueNode.getRowNum());
                                            return;
                                        } else {
                                            return;
                                        }
                                    } else {
                                        return;
                                    }
                                }
                                break;
                            // 声明real型变量
                            case ConstChars.REAL:
                                if (matchInteger(value)) {
                                    element.setRealValue(String.valueOf(Double
                                            .parseDouble(value)));
                                } else if (matchReal(value)) {
                                    element.setRealValue(value);
                                } else if ("true".equals(value)
                                        || "false".equals(value)) {
                                    String error = "不能将" + value + "赋值给浮点型变量";
                                    error(error, valueNode.getRowNum());
                                } else if ("字符串".equals(valueNode.getNodeType())) {
                                    String error = "不能将字符串给浮点型变量";
                                    error(error, valueNode.getRowNum());
                                } else if ("标识符".equals(valueNode.getNodeType())) {
                                    if (checkID(valueNode, level)) {
                                        switch (table.getAllLevel(
                                                valueNode.getContent(), level)
                                                .getKind()) {
                                            case ConstChars.INT:
                                            case ConstChars.REAL:
                                                element.setRealValue(table.getAllLevel(
                                                        valueNode.getContent(), level)
                                                        .getRealValue());
                                                break;
                                            case ConstChars.BOOL: {
                                                String error = "不能将布尔型变量赋值给浮点型变量";
                                                error(error, valueNode.getRowNum());
                                                break;
                                            }
                                            case ConstChars.STRING: {
                                                String error = "不能将字符串变量赋值给浮点型变量";
                                                error(error, valueNode.getRowNum());
                                                break;
                                            }
                                            default:
                                                break;
                                        }
                                    } else {
                                        return;
                                    }
                                } else if (value.equals(ConstChars.PLUS)
                                        || value.equals(ConstChars.MINUS)
                                        || value.equals(ConstChars.TIMES)
                                        || value.equals(ConstChars.DIVIDE)) {
                                    String result = forExpression(valueNode);
                                    if (result != null) {
                                        if (matchInteger(result)) {
                                            element.setRealValue(String
                                                    .valueOf(Double
                                                            .parseDouble(result)));
                                        } else if (matchReal(result)) {
                                            element.setRealValue(result);
                                        }
                                    } else {
                                        return;
                                    }
                                }
                                break;
                            // 声明string型变量
                            case ConstChars.STRING:
                                if (matchInteger(value)) {
                                    String error = "不能将整数赋值给字符串型变量";
                                    error(error, valueNode.getRowNum());
                                } else if (matchReal(value)) {
                                    String error = "不能将浮点数赋值给字符串型变量";
                                    error(error, valueNode.getRowNum());
                                } else if ("true".equals(value)
                                        || "false".equals(value)) {
                                    String error = "不能将" + value + "赋值给字符串型变量";
                                    error(error, valueNode.getRowNum());
                                } else if ("字符串".equals(valueNode.getNodeType())) {
                                    element.setStringValue(value);
                                } else if ("标识符".equals(valueNode.getNodeType())) {
                                    if (checkID(valueNode, level)) {
                                        switch (table.getAllLevel(
                                                valueNode.getContent(), level)
                                                .getKind()) {
                                            case ConstChars.INT: {
                                                String error = "不能将整数赋值给字符串型变量";
                                                error(error, valueNode.getRowNum());
                                                break;
                                            }
                                            case ConstChars.REAL: {
                                                String error = "不能将浮点数赋值给字符串型变量";
                                                error(error, valueNode.getRowNum());
                                                break;
                                            }
                                            case ConstChars.BOOL: {
                                                String error = "不能将布尔型变量赋值给字符串型变量";
                                                error(error, valueNode.getRowNum());
                                                break;
                                            }
                                            case ConstChars.STRING:
                                                element.setStringValue(value);
                                                break;
                                            default:
                                                break;
                                        }
                                    } else {
                                        return;
                                    }
                                } else if (value.equals(ConstChars.PLUS)
                                        || value.equals(ConstChars.MINUS)
                                        || value.equals(ConstChars.TIMES)
                                        || value.equals(ConstChars.DIVIDE)) {
                                    String error = "不能将算术表达式赋值给字符串型变量";
                                    error(error, valueNode.getRowNum());
                                }
                                break;
                            default:  // 声明bool型变量
                                if (matchInteger(value)) {
                                    // 如果是0或负数则记为false,其他记为true
                                    int i = Integer.parseInt(value);
                                    if (i <= 0) {
                                        element.setStringValue("false");
                                    } else {
                                        element.setStringValue("true");
                                    }
                                } else if (matchReal(value)) {
                                    String error = "不能将浮点数赋值给布尔型变量";
                                    error(error, valueNode.getRowNum());
                                } else if ("true".equals(value)
                                        || "false".equals(value)) {
                                    element.setStringValue(value);
                                } else if ("字符串".equals(valueNode.getNodeType())) {
                                    String error = "不能将字符串给布尔型变量";
                                    error(error, valueNode.getRowNum());
                                } else if ("标识符".equals(valueNode.getNodeType())) {
                                    if (checkID(valueNode, level)) {
                                        switch (table.getAllLevel(
                                                valueNode.getContent(), level)
                                                .getKind()) {
                                            case ConstChars.INT:
                                                int i = Integer.parseInt(table
                                                        .getAllLevel(
                                                                valueNode.getContent(),
                                                                level).getIntValue());
                                                if (i <= 0) {
                                                    element.setStringValue("false");
                                                } else {
                                                    element.setStringValue("true");
                                                }
                                                break;
                                            case ConstChars.REAL: {
                                                String error = "不能将浮点型变量赋值给布尔型变量";
                                                error(error, valueNode.getRowNum());
                                                break;
                                            }
                                            case ConstChars.BOOL:
                                                element
                                                        .setStringValue(table
                                                                .getAllLevel(
                                                                        valueNode
                                                                                .getContent(),
                                                                        level)
                                                                .getStringValue());
                                                break;
                                            case ConstChars.STRING: {
                                                String error = "不能将字符串变量赋值给布尔型变量";
                                                error(error, valueNode.getRowNum());
                                                break;
                                            }
                                            default:
                                                break;
                                        }
                                    } else {
                                        return;
                                    }
                                } else if (value.equals(ConstChars.EQUAL)
                                        || value.equals(ConstChars.NEQUAL)
                                        || value.equals(ConstChars.LT)
                                        || value.equals(ConstChars.GT)) {
                                    boolean result = forCondition(valueNode);
                                    if (result) {
                                        element.setStringValue("true");
                                    } else {
                                        element.setStringValue("false");
                                    }
                                }
                                break;
                        }
                        index++;
                    }
                    table.add(element);
                } else { // 声明数组
                    SymbolTableElement element = new SymbolTableElement(temp
                            .getContent(), content, temp.getRowNum(), level);
                    String sizeValue = temp.getChildAt(0).getContent();
                    if (matchInteger(sizeValue)) {
                        int i = Integer.parseInt(sizeValue);
                        if (i < 1) {
                            String error = "数组大小必须大于零";
                            error(error, root.getRowNum());
                            return;
                        }
                    } else if ("标识符".equals(temp.getChildAt(0).getNodeType())) {
                        if (checkID(root, level)) {
                            SymbolTableElement tempElement = table.getAllLevel(
                                    root.getContent(), level);
                            if (tempElement.getKind().equals(ConstChars.INT)) {
                                int i = Integer.parseInt(tempElement
                                        .getIntValue());
                                if (i < 1) {
                                    String error = "数组大小必须大于零";
                                    error(error, root.getRowNum());
                                    return;
                                } else {
                                    sizeValue = tempElement.getIntValue();
                                }
                            } else {
                                String error = "类型不匹配,数组大小必须为整数类型";
                                error(error, root.getRowNum());
                                return;
                            }
                        } else {
                            return;
                        }
                    } else if (sizeValue.equals(ConstChars.PLUS)
                            || sizeValue.equals(ConstChars.MINUS)
                            || sizeValue.equals(ConstChars.TIMES)
                            || sizeValue.equals(ConstChars.DIVIDE)) {
                        sizeValue = forExpression(temp.getChildAt(0));
                        if (sizeValue != null) {
                            if (matchInteger(sizeValue)) {
                                int i = Integer.parseInt(sizeValue);
                                if (i < 1) {
                                    String error = "数组大小必须大于零";
                                    error(error, root.getRowNum());
                                    return;
                                }
                            } else {
                                String error = "类型不匹配,数组大小必须为整数类型";
                                error(error, root.getRowNum());
                                return;
                            }
                        } else {
                            return;
                        }
                    }
                    element.setArrayElementsNum(Integer.parseInt(sizeValue));
                    table.add(element);
                    index++;
                    for (int j = 0; j < Integer.parseInt(sizeValue); j++) {
                        String s = temp.getContent() + "@" + j;
                        SymbolTableElement ste = new SymbolTableElement(s,
                                content, temp.getRowNum(), level);
                        table.add(ste);
                    }
                }
            } else { // 报错
                String error = "变量" + name + "已被声明,请重命名该变量";
                error(error, temp.getRowNum());
                return;
            }
        }
    }

    /**
     * 分析assign语句
     *
     * @param root 语法树中assign语句结点
     */
    private void forAssign(TreeNode root) {
        // 赋值语句左半部分
        TreeNode node1 = root.getChildAt(0);
        // 赋值语句左半部分标识符
        String node1Value = node1.getContent();
        if (table.getAllLevel(node1Value, level) != null) {
            if (node1.getChildCount() != 0) {
                String s = forArray(node1.getChildAt(0), table.getAllLevel(
                        node1Value, level).getArrayElementsNum());
                if (s != null) {
                    node1Value += "@" + s;
                } else {
                    return;
                }
            }
        } else {
            String error = "变量" + node1Value + "在使用前未声明";
            error(error, node1.getRowNum());
            return;
        }
        // 赋值语句左半部分标识符类型
        String node1Kind = table.getAllLevel(node1Value, level).getKind();
        // 赋值语句右半部分
        TreeNode node2 = root.getChildAt(1);
        String node2Kind = node2.getNodeType();
        String node2Value = node2.getContent();
        // 赋值语句右半部分的值
        String value = "";
        if ("整数".equals(node2Kind)) {
            value = node2Value;
            node2Kind = "int";
        } else if ("实数".equals(node2Kind)) {
            value = node2Value;
            node2Kind = "real";
        } else if ("字符串".equals(node2Kind)) {
            value = node2Value;
            node2Kind = "string";
        } else if ("布尔值".equals(node2Kind)) {
            value = node2Value;
            node2Kind = "bool";
        } else if ("标识符".equals(node2Kind)) {
            if (checkID(node2, level)) {
                if (node2.getChildCount() != 0) {
                    String s = forArray(node2.getChildAt(0), table.getAllLevel(
                            node2Value, level).getArrayElementsNum());
                    if (s != null) {
                        node2Value += "@" + s;
                    } else {
                        return;
                    }
                }
                SymbolTableElement temp = table.getAllLevel(node2Value, level);
                switch (temp.getKind()) {
                    case ConstChars.INT:
                        value = temp.getIntValue();
                        break;
                    case ConstChars.REAL:
                        value = temp.getRealValue();
                        break;
                    case ConstChars.BOOL:
                    case ConstChars.STRING:
                        value = temp.getStringValue();
                        break;
                    default:
                        break;
                }
                node2Kind = table.getAllLevel(node2Value, level).getKind();
            } else {
                return;
            }
        } else if (node2Value.equals(ConstChars.PLUS)
                || node2Value.equals(ConstChars.MINUS)
                || node2Value.equals(ConstChars.TIMES)
                || node2Value.equals(ConstChars.DIVIDE)) {
            String result = forExpression(node2);
            if (result != null) {
                if (matchInteger(result)) {
                    node2Kind = "int";
                } else if (matchReal(result)) {
                    node2Kind = "real";
                }
                value = result;
            } else {
                return;
            }
        } else if (node2Value.equals(ConstChars.EQUAL)
                || node2Value.equals(ConstChars.NEQUAL)
                || node2Value.equals(ConstChars.LT)
                || node2Value.equals(ConstChars.GT)) {
            boolean result = forCondition(node2);
            node2Kind = "bool";
            value = String.valueOf(result);
        }
        switch (node1Kind) {
            case ConstChars.INT:
                switch (node2Kind) {
                    case ConstChars.INT:
                        table.getAllLevel(node1Value, level).setIntValue(value);
                        table.getAllLevel(node1Value, level).setRealValue(
                                String.valueOf(Double.parseDouble(value)));
                        break;
                    case ConstChars.REAL: {
                        String error = "不能将浮点数赋值给整型变量";
                        error(error, node1.getRowNum());
                        return;
                    }
                    case ConstChars.BOOL: {
                        String error = "不能将布尔值赋值给整型变量";
                        error(error, node1.getRowNum());
                        return;
                    }
                    case ConstChars.STRING: {
                        String error = "不能将字符串给整型变量";
                        error(error, node1.getRowNum());
                        return;
                    }
                    default:
                        break;
                }
                break;
            case ConstChars.REAL:
                switch (node2Kind) {
                    case ConstChars.INT:
                        table.getAllLevel(node1Value, level).setRealValue(
                                String.valueOf(Double.parseDouble(value)));
                        break;
                    case ConstChars.REAL:
                        table.getAllLevel(node1Value, level).setRealValue(value);
                        break;
                    case ConstChars.BOOL: {
                        String error = "不能将布尔值赋值给浮点型变量";
                        error(error, node1.getRowNum());
                        return;
                    }
                    case ConstChars.STRING: {
                        String error = "不能将字符串给浮点型变量";
                        error(error, node1.getRowNum());
                        return;
                    }
                    default:
                        break;
                }
                break;
            case ConstChars.BOOL:
                switch (node2Kind) {
                    case ConstChars.INT:
                        int i = Integer.parseInt(node2Value);
                        if (i <= 0) {
                            table.getAllLevel(node1Value, level).setStringValue("false");
                        } else {
                            table.getAllLevel(node1Value, level).setStringValue("true");
                        }
                        break;
                    case ConstChars.REAL: {
                        String error = "不能将浮点数赋值给布尔型变量";
                        error(error, node1.getRowNum());
                        return;
                    }
                    case ConstChars.BOOL:
                        table.getAllLevel(node1Value, level).setStringValue(value);
                        break;
                    case ConstChars.STRING: {
                        String error = "不能将字符串赋值给布尔型变量";
                        error(error, node1.getRowNum());
                        return;

                    }
                    default:
                        break;
                }
                break;
            case ConstChars.STRING:
                switch (node2Kind) {
                    case ConstChars.INT: {
                        String error = "不能将整数赋值给字符串变量";
                        error(error, node1.getRowNum());
                        return;
                    }
                    case ConstChars.REAL: {
                        String error = "不能将浮点数赋值给字符串变量";
                        error(error, node1.getRowNum());
                        return;
                    }
                    case ConstChars.BOOL: {
                        String error = "不能将布尔变量赋值给字符串变量";
                        error(error, node1.getRowNum());
                        return;
                    }
                    case ConstChars.STRING:
                        table.getAllLevel(node1Value, level).setStringValue(value);
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }

    /**
     * 分析for语句
     *
     * @param root 语法树中for语句结点
     */
    private void forFor(TreeNode root) {
        // 根结点Initialization
        TreeNode initializationNode = root.getChildAt(0);
        // 根结点Condition
        TreeNode conditionNode = root.getChildAt(1);
        // 根结点Change
        TreeNode changeNode = root.getChildAt(2);
        // 根结点Statements
        TreeNode statementNode = root.getChildAt(3);
        // for循环语句初始化
        forAssign(initializationNode.getChildAt(0));
        // 条件为真
        while (forCondition(conditionNode.getChildAt(0))) {
            statement(statementNode);
            level--;
            table.update(level);
            level++;
            // for循环执行一次后改变循环条件中的变量
            forAssign(changeNode.getChildAt(0));
        }
    }

    /**
     * 分析if语句
     *
     * @param root 语法树中if语句结点
     */
    private void forIf(TreeNode root) {
        int count = root.getChildCount();
        // 根结点Condition
        TreeNode conditionNode = root.getChildAt(0);
        // 根结点Statements
        TreeNode statementNode = root.getChildAt(1);
        // 条件为真
        if (forCondition(conditionNode.getChildAt(0))) {
            statement(statementNode);
        } else if (count == 3) {
            // 条件为假且有else语句
            TreeNode elseNode = root.getChildAt(2);
            level++;
            statement(elseNode);
            level--;
            table.update(level);
        }
    }

    /**
     * 分析while语句
     *
     * @param root 语法树中while语句结点
     */
    private void forWhile(TreeNode root) {
        // 根结点Condition
        TreeNode conditionNode = root.getChildAt(0);
        // 根结点Statements
        TreeNode statementNode = root.getChildAt(1);
        while (forCondition(conditionNode.getChildAt(0))) {
            statement(statementNode);
            level--;
            table.update(level);
            level++;
        }
    }

    /**
     * 分析read语句
     *
     * @param root 语法树中read语句结点
     */
    private void forRead(TreeNode root) {
        // 要读取的变量的名字
        String idName = root.getContent();
        // 查找变量
        SymbolTableElement element = table.getAllLevel(idName, level);
        // 判断变量是否已经声明
        if (element != null) {
            if (root.getChildCount() != 0) {
                String s = forArray(root.getChildAt(0), element
                        .getArrayElementsNum());
                if (s != null) {
                    idName += "@" + s;
                } else {
                    return;
                }
            }
            String value = readInput();
            switch (element.getKind()) {
                case ConstChars.INT:
                    if (matchInteger(value)) {
                        table.getAllLevel(idName, level).setIntValue(value);
                        table.getAllLevel(idName, level).setRealValue(
                                String.valueOf(Double.parseDouble(value)));
                    } else { // 报错
                        String error = "不能将\"" + value + "\"赋值给变量" + idName;
                        //error(error, valueNode.getRowNum());
                        controller.resultContent.appendText(error+"\n");
                    }
                    break;
                case ConstChars.REAL:
                    if (matchReal(value)) {
                        table.getAllLevel(idName, level).setRealValue(value);
                    } else if (matchInteger(value)) {
                        table.getAllLevel(idName, level).setRealValue(
                                String.valueOf(Double.parseDouble(value)));
                    } else { // 报错
                        String error = "不能将\"" + value + "\"赋值给变量" + idName;
                        controller.resultContent.appendText(error+"\n");

                    }
                    break;
                case ConstChars.BOOL:
                    if ("true".equals(value)) {
                        table.getAllLevel(idName, level).setStringValue("true");
                    } else if ("false".equals(value)) {
                        table.getAllLevel(idName, level).setStringValue("false");
                    } else { // 报错
                        String error = "不能将\"" + value + "\"赋值给变量" + idName;
                        controller.resultContent.appendText(error+"\n");

                    }
                    break;
                case ConstChars.STRING:
                    table.getAllLevel(idName, level).setStringValue(value);
                    break;
                default:
                    break;
            }
        } else { // 报错
            String error = "变量" + idName + "在使用前未声明";
            error(error, root.getRowNum());
        }
    }

    /**
     * 分析write语句
     *
     * @param root 语法树中write语句结点
     */
    private void forWrite(TreeNode root) {
        // 结点显示的内容
        String content = root.getContent();
        // 结点的类型
        String kind = root.getNodeType();
        if ("整数".equals(kind) || "实数".equals(kind)) {
            output = output + content + "\n";
            controller.resultContent.appendText(content+"\n");
        } else if ("字符串".equals(kind)) {
            output = output + content + "\n";
            controller.resultContent.appendText(content+"\n");
        } else if ("标识符".equals(kind)) {
            if (checkID(root, level)) {
                if (root.getChildCount() != 0) {
                    String s = forArray(root.getChildAt(0), table.getAllLevel(
                            content, level).getArrayElementsNum());
                    if (s != null) {
                        content += "@" + s;
                    } else {
                        return;
                    }
                }
                SymbolTableElement temp = table.getAllLevel(content, level);
                switch (temp.getKind()) {
                    case ConstChars.INT:
                        //output = output + content + "\n";
                        controller.resultContent.appendText(temp.getIntValue()+"\n");
                        break;
                    case ConstChars.REAL:
                        //output = output + content + "\n";
                        controller.resultContent.appendText(temp.getRealValue()+"\n");
                        break;
                    default:
                        controller.resultContent.appendText(temp.getStringValue()+"\n");
                        //output = output + content + "\n";
                        break;
                }
            }
        } else if (content.equals(ConstChars.PLUS)
                || content.equals(ConstChars.MINUS)
                || content.equals(ConstChars.TIMES)
                || content.equals(ConstChars.DIVIDE)) {
            String value = forExpression(root);
            if (value != null) {
                //output = output + value + "\n";
                controller.resultContent.appendText(value +"\n");

            }
        }
    }

    /**
     * 分析if和while语句的条件
     *
     * @param root 根结点
     * @return 返回计算结果
     */
    private boolean forCondition(TreeNode root) {
        // > < <> == true false 布尔变量
        String content = root.getContent();
        if (content.equals(ConstChars.TRUE)) {
            return true;
        } else if (content.equals(ConstChars.FALSE)) {
            return false;
        } else if ("标识符".equals(root.getNodeType())) {
            if (checkID(root, level)) {
                if (root.getChildCount() != 0) {
                    String s = forArray(root.getChildAt(0), table.getAllLevel(
                            content, level).getArrayElementsNum());
                    if (s != null) {
                        content += "@" + s;
                    } else {
                        return false;
                    }
                }
                SymbolTableElement temp = table.getAllLevel(content, level);
                if (temp.getKind().equals(ConstChars.BOOL)) {
                    return temp.getStringValue().equals(ConstChars.TRUE);
                } else { // 报错
                    String error = "不能将变量" + content + "作为判断条件";
                    error(error, root.getRowNum());
                }
            } else {
                return false;
            }
        } else if (content.equals(ConstChars.EQUAL)
                || content.equals(ConstChars.NEQUAL)
                || content.equals(ConstChars.LT) || content.equals(ConstChars.GT)) {
            // 存放两个待比较对象的值
            String[] results = new String[2];
            for (int i = 0; i < root.getChildCount(); i++) {
                String kind = root.getChildAt(i).getNodeType();
                String tempContent = root.getChildAt(i).getContent();
                if ("整数".equals(kind) || "实数".equals(kind)) {
                    results[i] = tempContent;
                } else if ("标识符".equals(kind)) {
                    if (checkID(root.getChildAt(i), level)) {
                        if (root.getChildAt(i).getChildCount() != 0) {
                            String s = forArray(root.getChildAt(i)
                                    .getChildAt(0), table.getAllLevel(
                                    tempContent, level).getArrayElementsNum());
                            if (s != null) {
                                tempContent += "@" + s;
                            } else {
                                return false;
                            }
                        }
                        SymbolTableElement temp = table.getAllLevel(
                                tempContent, level);
                        if (temp.getKind().equals(ConstChars.INT)) {
                            results[i] = temp.getIntValue();
                        } else {
                            results[i] = temp.getRealValue();
                        }
                    } else {
                        return false;
                    }
                } else if (tempContent.equals(ConstChars.PLUS)
                        || tempContent.equals(ConstChars.MINUS)
                        || tempContent.equals(ConstChars.TIMES)
                        || tempContent.equals(ConstChars.DIVIDE)) {
                    String result = forExpression(root.getChildAt(i));
                    if (result != null) {
                        results[i] = result;
                    } else {
                        return false;
                    }
                }
            }
            if (!"".equals(results[0]) && !"".equals(results[1])) {
                double element1 = Double.parseDouble(results[0]);
                double element2 = Double.parseDouble(results[1]);
                switch (content) {
                    case ConstChars.GT:
                        return element1 > element2;
                    case ConstChars.LT:
                        return element1 < element2;
                    case ConstChars.EQUAL:
                        return element1 == element2;
                    default:  // <>
                        return element1 != element2;
                }
            }
        }
        // 语义分析出错或者分析条件结果为假返回false
        return false;
    }

    /**
     * 分析表达式
     *
     * @param root 根结点
     * @return 返回计算结果
     */
    private String forExpression(TreeNode root) {
        boolean isInt = true;
        // + -
        String content = root.getContent();
        // 存放两个运算对象的值
        String[] results = new String[2];
        for (int i = 0; i < root.getChildCount(); i++) {
            TreeNode tempNode = root.getChildAt(i);
            String kind = tempNode.getNodeType();
            String tempContent = tempNode.getContent();
            if ("整数".equals(kind)) {
                results[i] = tempContent;
            } else if ("实数".equals(kind)) {
                results[i] = tempContent;
                isInt = false;
            } else if ("标识符".equals(kind)) {
                if (checkID(tempNode, level)) {
                    if (tempNode.getChildCount() != 0) {
                        String s = forArray(tempNode.getChildAt(0), table
                                .getAllLevel(tempContent, level)
                                .getArrayElementsNum());
                        if (s != null) {
                            tempContent += "@" + s;
                        } else {
                            return null;
                        }
                    }
                    SymbolTableElement temp = table.getAllLevel(tempNode
                            .getContent(), level);
                    if (temp.getKind().equals(ConstChars.INT)) {
                        results[i] = temp.getIntValue();
                    } else if (temp.getKind().equals(ConstChars.REAL)) {
                        results[i] = temp.getRealValue();
                        isInt = false;
                    }
                } else {
                    return null;
                }
            } else if (tempContent.equals(ConstChars.PLUS)
                    || tempContent.equals(ConstChars.MINUS)
                    || tempContent.equals(ConstChars.TIMES)
                    || tempContent.equals(ConstChars.DIVIDE)) {
                String result = forExpression(root.getChildAt(i));
                if (result != null) {
                    results[i] = result;
                    if (matchReal(result)) {
                        isInt = false;
                    }
                } else {
                    return null;
                }
            }
        }
        if (isInt) {
            int e1 = Integer.parseInt(results[0]);
            int e2 = Integer.parseInt(results[1]);
            switch (content) {
                case ConstChars.PLUS:
                    return String.valueOf(e1 + e2);
                case ConstChars.MINUS:
                    return String.valueOf(e1 - e2);
                case ConstChars.TIMES:
                    return String.valueOf(e1 * e2);
                default:
                    if (e2 == 0) {
                        String error = "除数不能为零";
                        error(error, root.getRowNum());
                        return "";
                    }else{
                        return String.valueOf(e1 / e2);
                    }

            }
        } else {
            double e1 = Double.parseDouble(results[0]);
            double e2 = Double.parseDouble(results[1]);
            BigDecimal bd1 = new BigDecimal(e1);
            BigDecimal bd2 = new BigDecimal(e2);
            switch (content) {
                case ConstChars.PLUS:
                    return String.valueOf(bd1.add(bd2).floatValue());
                case ConstChars.MINUS:
                    return String.valueOf(bd1.subtract(bd2).floatValue());
                case ConstChars.TIMES:
                    return String.valueOf(bd1.multiply(bd2).floatValue());
                default:
                    return String.valueOf(bd1.divide(bd2, 3,
                            BigDecimal.ROUND_HALF_UP).floatValue());
            }
        }
    }

    /**
     * array
     *
     * @param root      根结点
     * @param arraySize 数组大小
     * @return 出错返回null
     */
    private String forArray(TreeNode root, int arraySize) {
        if ("整数".equals(root.getNodeType())) {
            int i = Integer.parseInt(root.getContent());
            if (i > -1 && i < arraySize) {
                return root.getContent();
            } else if (i < 0) {
                String error = "数组下标不能为负数";
                error(error, root.getRowNum());
                return null;
            } else {
                String error = "数组下标越界";
                error(error, root.getRowNum());
                return null;
            }
        } else if ("标识符".equals(root.getNodeType())) {
            // 检查标识符
            if (checkID(root, level)) {
                SymbolTableElement temp = table.getAllLevel(root.getContent(),
                        level);
                if (temp.getKind().equals(ConstChars.INT)) {
                    int i = Integer.parseInt(temp.getIntValue());
                    if (i > -1 && i < arraySize) {
                        return temp.getIntValue();
                    } else if (i < 0) {
                        String error = "数组下标不能为负数";
                        error(error, root.getRowNum());
                        return null;
                    } else {
                        String error = "数组下标越界";
                        error(error, root.getRowNum());
                        return null;
                    }
                } else {
                    String error = "类型不匹配,数组索引号必须为整数类型";
                    error(error, root.getRowNum());
                    return null;
                }
            } else {
                return null;
            }
        } else if (root.getContent().equals(ConstChars.PLUS)
                || root.getContent().equals(ConstChars.MINUS)
                || root.getContent().equals(ConstChars.TIMES)
                || root.getContent().equals(ConstChars.DIVIDE)) {
            String result = forExpression(root);
            if (result != null) {
                if (matchInteger(result)) {
                    int i = Integer.parseInt(result);
                    if (i > -1 && i < arraySize) {
                        return result;
                    } else if (i < 0) {
                        String error = "数组下标不能为负数";
                        error(error, root.getRowNum());
                        return null;
                    } else {
                        String error = "数组下标越界";
                        error(error, root.getRowNum());
                        return null;
                    }
                } else {
                    String error = "类型不匹配,数组索引号必须为整数类型";
                    error(error, root.getRowNum());
                    return null;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * 检查字符串是否声明和初始化
     *
     * @param root  字符串结点
     * @param level 字符串作用域
     * @return 如果声明且初始化则返回true, 否则返回false
     */
    private boolean checkID(TreeNode root, int level) {
        // 标识符名字
        String idName = root.getContent();
        // 标识符未声明
        if (table.getAllLevel(idName, level) == null) {
            String error = "变量" + idName + "在使用前未声明";
            error(error, root.getRowNum());
            return false;
        } else {
            if (root.getChildCount() != 0) {
                String tempString = forArray(root.getChildAt(0), table
                        .getAllLevel(idName, level).getArrayElementsNum());
                if (tempString != null) {
                    idName += "@" + tempString;
                } else {
                    return false;
                }
            }
            SymbolTableElement temp = table.getAllLevel(idName, level);
            // 变量未初始化
            if ("".equals(temp.getIntValue()) && "".equals(temp.getRealValue())
                    && "".equals(temp.getStringValue())) {
                String error = "变量" + idName + "在使用前未初始化";
                error(error, root.getRowNum());
                return false;
            } else {
                return true;
            }
        }
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    public int getErrorNum() {
        return errorNum;
    }

    public void setErrorNum(int errorNum) {
        this.errorNum = errorNum;
    }

}
