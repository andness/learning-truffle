package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild("left") @NodeChild("right")
public abstract class ToylMulNode extends ToylNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  protected int mulInts(int leftValue, int rightValue) {
    return Math.multiplyExact(leftValue, rightValue);
  }

  @Specialization(replaces = "mulInts")
  protected double mulDoubles(double leftValue, double rightValue) {
    return leftValue * rightValue;
  }
}
