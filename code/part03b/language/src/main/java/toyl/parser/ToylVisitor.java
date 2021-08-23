// Generated from src/main/java/toyl/parser/Toyl.g4 by ANTLR 4.9.2
package toyl.parser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ToylParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ToylVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ToylParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(ToylParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ParenthesizedExpr}
	 * labeled alternative in {@link ToylParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedExpr(ToylParser.ParenthesizedExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ArithmeticExpression}
	 * labeled alternative in {@link ToylParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticExpression(ToylParser.ArithmeticExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LiteralNumber}
	 * labeled alternative in {@link ToylParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralNumber(ToylParser.LiteralNumberContext ctx);
}