package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild("left") @NodeChild("right")
public abstract class ToylAddNode extends ToylExpressionNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  protected int addInts(int leftValue, int rightValue) {
    return Math.addExact(leftValue, rightValue);
  }

  @Specialization(replaces = "addInts")
  protected double addDoubles(double leftValue, double rightValue) {
    return leftValue + rightValue;
  }
}
