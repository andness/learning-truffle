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
    return switch (ctx.binaryOp.getText()) {
      case "+" -> ToylAddNodeGen.create(this.visit(ctx.left), this.visit(ctx.right));
      case "-" -> ToylSubNodeGen.create(this.visit(ctx.left), this.visit(ctx.right));
      case "/" -> ToylDivNodeGen.create(this.visit(ctx.left), this.visit(ctx.right));
      case "*" -> ToylMulNodeGen.create(this.visit(ctx.left), this.visit(ctx.right));
      default -> throw new IllegalStateException("Unexpected arithmetic operator: " + ctx.binaryOp.getText());
    };
  }

  @Override
  public ToylNode visitLiteralNumber(ToylParser.LiteralNumberContext ctx) {
    var number = new BigDecimal(ctx.LITERAL_NUMBER().getText());
    if (number.scale() > 0 || number.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
      return new ToylLiteralNumberNode(number);
    } else {
      return new ToylLiteralIntNode(number.intValue());
    }
  }
}
