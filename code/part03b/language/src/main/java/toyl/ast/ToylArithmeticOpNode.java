package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

import java.math.BigDecimal;
import java.math.MathContext;

public class ToylArithmeticOpNode extends ToylNode {

  private final ToylNode left;
  private final ToylNode right;
  private final String binaryOp;

  public ToylArithmeticOpNode(ToylNode left, ToylNode right, String binaryOp) {
    this.left = left;
    this.right = right;
    this.binaryOp = binaryOp;
  }

  @Override
  public BigDecimal execute(VirtualFrame frame) {
    var leftVal = this.left.execute(frame);
    var rightVal = this.right.execute(frame);
    return switch (this.binaryOp) {
      case "+" -> leftVal.add(rightVal);
      case "-" -> leftVal.subtract(rightVal);
      case "*" -> leftVal.multiply(rightVal);
      case "/" -> leftVal .divide(rightVal, MathContext.DECIMAL128);
      default -> throw new AssertionError("Unexpected operator " + this.binaryOp);
    };
  }
}
