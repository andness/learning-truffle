package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

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
  public Object executeGeneric(VirtualFrame frame) {
    System.out.println("executeGeneric");
    try {
      var leftValLong = this.left.executeLong(frame);
      var rightValLong = this.right.executeLong(frame);
      try {
        return Math.addExact(leftValLong, rightValLong);
      } catch (ArithmeticException ae) {
        var leftValNumber = this.left.executeNumber(frame);
        var rightValNumber = this.right.executeNumber(frame);
        return leftValNumber.add(rightValNumber);
      }
    } catch (UnexpectedResultException e) {
      var leftVal = this.left.executeNumber(frame);
      var rightVal = this.right.executeNumber(frame);
      return leftVal.add(rightVal);
    }
  }

  @Override
  public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
    System.out.println("executeLong");
    try {
      var leftVal = this.left.executeLong(frame);
      var rightVal = this.right.executeLong(frame);
      return Math.addExact(leftVal, rightVal);
    } catch (ArithmeticException e) {
      var leftVal = this.left.executeNumber(frame);
      var rightVal = this.right.executeNumber(frame);
      throw new UnexpectedResultException(leftVal.add(rightVal));
    }
  }

  @Override
  public BigDecimal executeNumber(VirtualFrame frame) {
    System.out.println("executeNumber");
    return this.left.executeNumber(frame).add(this.right.executeNumber(frame));
  }

//  @Override
//  public BigDecimal execute(VirtualFrame frame) {
//    var leftVal = this.left.execute(frame);
//    var rightVal = this.right.execute(frame);
//    return switch (this.binaryOp) {
//      case "+" -> leftVal.add(rightVal);
//      case "-" -> leftVal.subtract(rightVal);
//      case "*" -> leftVal.multiply(rightVal);
//      case "/" -> leftVal .divide(rightVal, MathContext.DECIMAL128);
//      default -> throw new AssertionError("Unexpected operator " + this.binaryOp);
//    };
//  }
}
