package sample.interpreter;


import sample.interpreter.lexer.Lexer;
import sample.interpreter.lexer.Token;
import sample.interpreter.lexer.TreeNode;
import sample.interpreter.parser.Parser;
import sample.interpreter.semantic.Semantic;
import sample.interpreter.util.ParserUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * 已成功;
 * @author :  TobaisTao
 */
public class Test {

    public static void main(String[] args) throws IOException, IllegalAccessException {


        String sourceFile = "E:\\GitRepo\\JAVA\\cmm-interpreter-app\\testFile\\test2_一般变量赋值.cmm";
        Charset charset = Charset.forName("GBK");
        String cmmText = new String(Files.readAllBytes(Paths.get(sourceFile)),charset);

        //lexer.scanAll(cmmText);
        //ArrayList<Token> tokens = lexer.getTokens();
        //String errorInfo =lexer.getErrorInfo();
        //
        //if (lexer.getErrorCount() != 0) {
        //    System.out.println("词法分析出现错误！请先修改程序再进行语法分析！");
        //}
        //Parser parser = new Parser(tokens);
        //parser.setIndex(0);
        //parser.setErrorInfo("");
        //parser.setErrorNum(0);
        //TreeNode root = parser.execute();
        //Semantic semantic = new Semantic(root);
        //semantic.run();
        //String output = semantic.getOutput();
        //if (semantic.getErrorNum() > 0){
        //    output = "共发生" + semantic.getErrorNum() + "个错误：\n" + semantic.getErrorInfo();
        //    //resultContent.setText(output);
        //}else {
        //    //resultContent.setText(output);
        //    System.out.println(output);
        //}

        ArrayList<Token> tokens;
        Lexer lexer = new Lexer();
        lexer.scanAll(cmmText);
        tokens = lexer.getTokens();
        Parser parser = new Parser(tokens);
        TreeNode node = parser.execute();
        if (lexer.getErrorCount() != 0) {
            //handleLexer();
        }else if (parser.getErrorNum() != 0 || node == null) {
            //handleParser();
        } else {
            Semantic semantic = new Semantic(node);
            semantic.start();
            Scanner sc = new Scanner( System.in );
            semantic.setUserInput(sc.toString());
            String output ;
            if (semantic.getErrorNum() > 0){
                output = "共发生" + semantic.getErrorNum() + "个错误：\n" + semantic.getErrorInfo();
                //resultContent.setText(output);
            }else {
                output = semantic.getOutput();
                //resultContent.setText(output);
                System.out.println(output);
            }

        }

    }
}


