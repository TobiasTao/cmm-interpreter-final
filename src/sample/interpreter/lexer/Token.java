package sample.interpreter.lexer;

/**
 * Toke 实体类
 * @author :  陶勇聪
 */
public class Token  {

    /**
     * Token 的类型
     */
    private String type;

    /**
     * 种别码
     */
    private Integer typeNum;

    /**
     * Toke 所在行
     */
    private Integer rowNum;
    /**
     * Token 所在列
     */
    private Integer colNum;
    /**
     * 该 Token 的内容
     */
    private String content;
    /**
     * 标识符类型
     */
    private String identifierType;

    public Token(Integer rowNum, Integer colNum, String type, String content) {
        this.type = type;
        this.rowNum = rowNum;
        this.colNum = colNum;
        this.content = content;
    }
    public Integer getTypeNum() throws IllegalAccessException {
        return MyUtil.getTypeNum(this);
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getRowNum() {
        return rowNum;
    }

    public void setRowNum(Integer rowNum) {
        this.rowNum = rowNum;
    }

    public Integer getColNum() {
        return colNum;
    }

    public void setColNum(Integer colNum) {
        this.colNum = colNum;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

}
