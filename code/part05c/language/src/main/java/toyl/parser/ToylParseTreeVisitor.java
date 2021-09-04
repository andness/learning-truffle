package toyl.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import toyl.ast.*;

import java.math.BigDecimal;

public class ToylParseTreeVisitor extends ToylBaseVisitor<ToylNode> {
  private FrameDescriptor frameDescriptor;

  public ToylParseTreeVisitor(FrameDescriptor frameDescriptor) {
    this.frameDescriptor = frameDescriptor;
  }

  @Override
  public ToylNode visitProgram(ToylParser.ProgramContext ctx) {
    return new ToylProgramNode(ctx.statement().stream().map(this::visit).toList());
  }

  @Override
  public ToylNode visitParenthesizedExpr(ToylParser.ParenthesizedExprContext ctx) {
    return this.visit(ctx.expr());
  }

  @Override
  public ToylNode visitArithmeticExpression(ToylParser.ArithmeticExpressionContext ctx) {
    var left = (ToylExpressionNode) this.visit(ctx.left);
    var right = (ToylExpressionNode) this.visit(ctx.right);
    return switch (ctx.binaryOp.getText()) {
      case "+" -> ToylAddNodeGen.create(left, right);
      case "-" -> ToylSubNodeGen.create(left, right);
      case "/" -> ToylDivNodeGen.create(left, right);
      case "*" -> ToylMulNodeGen.create(left, right);
      default -> throw new IllegalStateException("Unexpected arithmetic operator: " + ctx.binaryOp.getText());
    };
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

  @Override
  public ToylNode visitUnaryMinus(ToylParser.UnaryMinusContext ctx) {
    // unary minus is implemented simply as 0 - expr
    return ToylSubNodeGen.create(new ToylLiteralLongNode(0), (ToylExpressionNode) this.visit(ctx.expr()));
  }

  @Override
  public ToylNode visitAssignment(ToylParser.AssignmentContext ctx) {
    final String name = ctx.NAME().getText();
    var slot = this.frameDescriptor.findOrAddFrameSlot(name);
    return ToylAssignmentNodeGen.create(this.visit(ctx.expr()), name, slot);
  }

  @Override
  public ToylNode visitVarRefExpr(ToylParser.VarRefExprContext ctx) {
    final String name = ctx.NAME().getText();
    return ToylVarRefNodeGen.create(name, this.frameDescriptor.findFrameSlot(name));
  }
}
