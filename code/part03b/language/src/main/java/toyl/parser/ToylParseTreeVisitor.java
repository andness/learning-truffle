package toyl.parser;

import toyl.ast.*;

import java.math.BigDecimal;

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
    return new ToylAddNode(this.visit(ctx.left), this.visit(ctx.right));
  }

  @Override
  public ToylNode visitLiteralNumber(ToylParser.LiteralNumberContext ctx) {
    var number = new BigDecimal(ctx.LITERAL_NUMBER().getText());
    try {
      return new ToylLiteralLongNode(number.longValueExact());
    } catch(ArithmeticException e) {
      return new ToylLiteralNumberNode(number);
    }
  }
}
