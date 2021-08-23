package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import java.math.BigDecimal;

@NodeChild("left")
@NodeChild("right")
public abstract class ToylSubNode extends ToylNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  protected long subLongs(long leftValue, long rightValue) {
    return Math.subtractExact(leftValue, rightValue);
  }

  @Specialization(replaces = "subLongs")
  protected BigDecimal subDoubles(BigDecimal leftValue, BigDecimal rightValue) {
    return leftValue.subtract(rightValue);
  }
}
