package toyl.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import toyl.ast.*;

import java.math.BigDecimal;

public class ToylParseTreeVisitor extends ToylBaseVisitor<ToylStatementNode> {

  private FrameDescriptor frameDescriptor;

  public ToylParseTreeVisitor(FrameDescriptor frameDescriptor) {
    this.frameDescriptor = frameDescriptor;
  }

  @Override
  public ToylProgramNode visitProgram(ToylParser.ProgramContext ctx) {
    return new ToylProgramNode(ctx.statement().stream().map(this::visit).toList());
  }

  @Override
  public ToylExpressionNode visitParenthesizedExpr(ToylParser.ParenthesizedExprContext ctx) {
    return (ToylExpressionNode) this.visit(ctx.expr());
  }

  @Override
  public ToylExpressionNode visitArithmeticExpression(ToylParser.ArithmeticExpressionContext ctx) {
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
  public ToylExpressionNode visitLiteralNumber(ToylParser.LiteralNumberContext ctx) {
    var number = new BigDecimal(ctx.LITERAL_NUMBER().getText());
    if (number.scale() > 0 || number.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
      return new ToylLiteralDoubleNode(number.doubleValue());
    } else {
      return new ToylLiteralIntNode(number.intValue());
    }
  }

  @Override
  public ToylStatementNode visitAssignment(ToylParser.AssignmentContext ctx) {
    final String name = ctx.NAME().getText();
    var slot = this.frameDescriptor.findOrAddFrameSlot(name);
    return new ToylAssignmentNode(name, slot, (ToylExpressionNode) this.visit(ctx.expr()));
  }

  @Override
  public ToylStatementNode visitVarRefExpr(ToylParser.VarRefExprContext ctx) {
    final String name = ctx.NAME().getText();
    return new ToylVarRefNode(name, this.frameDescriptor.findFrameSlot(name));
  }
}
