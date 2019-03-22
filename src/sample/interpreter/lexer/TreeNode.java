package sample.interpreter.lexer;

import javax.swing.tree.DefaultMutableTreeNode;

public class TreeNode extends DefaultMutableTreeNode {
	private static final long serialVersionUID = 123232323L;
	/**
	 * 节点类型 （保留字，运算符。。）
	 */
	private String nodeType;
	/**
	 * 节点内容
	 */
	private String content;
	/**
	 * 所在行数
	 */
	private int rowNum;

	public TreeNode(String content) {
		super(content);
		this.content = content;
		nodeType = "";
	}

	public TreeNode(String nodeType, String content,int rowNum) {
		super(content);
		this.content = content;
		this.rowNum = rowNum;
		this.nodeType = nodeType;
	}

	public String getNodeType() {
		return nodeType;
	}

	public int getRowNum() {
		return rowNum;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
		setUserObject(content);
	}

	/**
	 * 为该结点添加孩子结点
	 * @param childNode 要添加的孩子结点
	 */
	public void add(TreeNode childNode) {
		super.add(childNode);
	}

	/**
	 * 得到索引为 index 处的子节点
	 * @param index
	 * @return
	 */
	@Override
	public TreeNode getChildAt(int index) {
		return (TreeNode) super.getChildAt(index);
	}
	
}
