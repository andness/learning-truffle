package toyl.parser;

import toyl.ast.ToylArithmeticOpNode;
import toyl.ast.ToylLiteralNumberNode;
import toyl.ast.ToylNode;

public class ToylParseTreeVisitor extends ToylBaseVisitor<ToylNode> {
  @Override
  public ToylNode visitProgram(ToylParser.ProgramContext ctx) {
    return this.visitExpr(ctx.expr());
  }

  @Override
  public ToylNode visitExpr(ToylParser.ExprContext ctx) {
    if (ctx.LITERAL_NUMBER() != null) {
      return new ToylLiteralNumberNode(ctx.LITERAL_NUMBER().getText());
    }
    if (ctx.parenthesizedExpr() != null) {
      return this.visitParenthesizedExpr(ctx.parenthesizedExpr());
    }
    var left = this.visit(ctx.left);
    var right = this.visit(ctx.right);
    return new ToylArithmeticOpNode(left, right, ctx.binaryOp.getText());
  }

  @Override
  public ToylNode visitParenthesizedExpr(ToylParser.ParenthesizedExprContext ctx) {
    return this.visitExpr(ctx.expr());
  }
}
