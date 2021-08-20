package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild("left") @NodeChild("right")
public abstract class ToylDivNode extends ToylNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  protected int divInts(int leftValue, int rightValue) {
    return leftValue / rightValue;
  }

  @Specialization(replaces = "divInts")
  protected double divDoubles(double leftValue, double rightValue) {
    return leftValue * rightValue;
  }
}
