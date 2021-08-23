package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import java.math.BigDecimal;

@NodeChild("left") @NodeChild("right")
public abstract class ToylSubNode extends ToylNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  protected int subInts(int leftValue, int rightValue) {
    return Math.subtractExact(leftValue, rightValue);
  }

  @Specialization(replaces = "subInts")
  protected BigDecimal subDoubles(BigDecimal leftValue, BigDecimal rightValue) {
    return leftValue.subtract(rightValue);
  }
}
