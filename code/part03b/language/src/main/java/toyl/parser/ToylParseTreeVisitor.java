package toyl.parser;

import toyl.ast.ToylArithmeticOpNode;
import toyl.ast.ToylLiteralNumberNode;
import toyl.ast.ToylNode;

public class ToylParseTreeVisitor extends ToylBaseVisitor<ToylNode> {
  @Override
  public ToylNode visitProgram(ToylParser.ProgramContext ctx) {
    return this.visit(ctx.expr());
  }

  @Override
  public ToylNode visitParenthesizedExpr(ToylParser.ParenthesizedExprContext ctx) {
    return this.visit(ctx.expr());
  }

  @Override
  public ToylNode visitArithmeticExpression(ToylParser.ArithmeticExpressionContext ctx) {
    return new ToylArithmeticOpNode(this.visit(ctx.left), this.visit(ctx.right), ctx.binaryOp.getText());
  }

  @Override
  public ToylNode visitLiteralNumber(ToylParser.LiteralNumberContext ctx) {
    return new ToylLiteralNumberNode(ctx.LITERAL_NUMBER().getText());
  }
}
