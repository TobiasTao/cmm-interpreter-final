package sample.view;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import sample.MainApp;
import sample.interpreter.lexer.Lexer;
import sample.interpreter.lexer.Token;
import sample.interpreter.lexer.TreeNode;
import sample.interpreter.parser.Parser;
import sample.interpreter.semantic.Semantic;
import sample.interpreter.util.ParserUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static javafx.scene.input.KeyCode.*;

/**
 * @author :  TobaisTao
 */
public class RootLayoutController {

    @FXML
    private TextArea cmmContent;

    @FXML
    public TextArea resultContent;

    private MainApp mainApp;

    private String oldInput = "";
    private String currentInput = "";

    private Lexer lexer;
    private Parser parser;
    private Semantic semantic;



    /**
     * Is called by the main application to give a reference back to itself.
     * @param mainApp mainapp
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }


    @FXML
    private void initialize() {

        resultContent.setOnKeyReleased(
                event -> {
                    if (event.getCode() == ENTER){
                        currentInput = resultContent.getText(oldInput.length(), resultContent.getText().length()-1);
                        semantic.setUserInput(currentInput);
                        oldInput = resultContent.getText();
                    }
                }
        );
    }

    /**
     * Opens a FileChooser to let the user select an address book to load.
     */
    @FXML
    private void handleOpen() {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        // Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
            "CMM files (*.cmm)", "*.cmm");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show save file dialog
        File file = fileChooser.showOpenDialog(mainApp.getPrimaryStage());

        if (file != null) {
            String content = "";
            try {
                Charset charset = Charset.forName("GBK");
                content = new String(Files.readAllBytes(Paths.get(file.getPath())), charset);
            } catch (IOException e) {
                e.printStackTrace();
            }
            cmmContent.clear();
            resultContent.clear();
            oldInput = "";
            currentInput = "";
            cmmContent.setText(content);
        }
    }

    @FXML
    private void handleLexer() throws IllegalAccessException {
        Integer errorCount;
        String content = cmmContent.getText();
        String str;


        lexer = new Lexer();
        lexer.scanAll(content);
        ArrayList<Token> tokens;
        errorCount = lexer.getErrorCount();
        tokens = lexer.getTokens();

        if (errorCount > 0){
            str = ("发生了" + errorCount + "个错误！\n") + lexer.getErrorInfo();
        }else{
            StringBuilder stringBuilder = new StringBuilder();
            for(Token token : tokens){
                stringBuilder
                        .append("<")
                        .append(token.getTypeNum())
                        .append(",")
                        .append(token.getContent())
                        .append(">")
                        .append("\n");
            }
             str = stringBuilder.toString();
        }
        resultContent.clear();
        resultContent.setText(str);
    }

    @FXML
    private void handleParser(){
        Integer errorCount;
        String content = cmmContent.getText();
        String str;

        lexer = new Lexer();
        lexer.scanAll(content);
        ArrayList<Token> tokens;
        errorCount = lexer.getErrorCount();
        tokens = lexer.getTokens();
        if (errorCount!= 0) {
            resultContent.clear();
            resultContent.setText("词法分析出现错误！请先修改程序再进行语法分析！");
        }else{
            parser = new Parser(tokens);
            parser.setIndex(0);
            parser.setErrorInfo("");
            parser.setErrorNum(0);
            TreeNode root = parser.execute();
            if (parser.getErrorNum()!=0){
                resultContent.clear();
                resultContent.setText(parser.getErrorInfo());
            }else{
                ParserUtil parserUtil = new ParserUtil();
                parserUtil.lastOrder(root, 0);
                str = parserUtil.getStringBuilder().toString();
                resultContent.clear();
                resultContent.setText(str);
            }

        }

    }

    @FXML
    private void handleOutput() throws IllegalAccessException {
        oldInput = "";
        currentInput = "";

        String content = cmmContent.getText();
        resultContent.clear();


        ArrayList<Token> tokens;
        lexer = new Lexer();
        lexer.scanAll(content);
        tokens = lexer.getTokens();
        parser = new Parser(tokens);
        TreeNode node = parser.execute();

        if (lexer.getErrorCount() != 0) {
            handleLexer();
        } else if (parser.getErrorNum() != 0 || node == null) {
            handleParser();
        } else {
            semantic = new Semantic(node, this);
            semantic.start();
            //String output ;
            //while(!"BLOCKED".equals(semantic.getState())){
            //    if (semantic.getErrorNum() > 0){
            //        output = "共发生" + semantic.getErrorNum() + "个错误：\n" + semantic.getErrorInfo();
            //        resultContent.setText(output);
            //    }else {
            //        output = semantic.getOutput();
            //        resultContent.setText(output);
            //    }
            //}


        }
    }


}
