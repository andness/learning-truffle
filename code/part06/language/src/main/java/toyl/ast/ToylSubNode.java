package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild("left") @NodeChild("right")
public abstract class ToylSubNode extends ToylExpressionNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  protected int subInts(int leftValue, int rightValue) {
    return Math.subtractExact(leftValue, rightValue);
  }

  @Specialization(replaces = "subInts")
  protected double subDoubles(double leftValue, double rightValue) {
    return leftValue - rightValue;
  }
}
