package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import java.math.BigDecimal;

@NodeChild("left")
@NodeChild("right")
public abstract class ToylMulNode extends ToylExpressionNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  protected long mulLongs(long leftValue, long rightValue) {
    return Math.multiplyExact(leftValue, rightValue);
  }

  @Specialization(replaces = "mulLongs")
  protected BigDecimal mulNumbers(BigDecimal leftValue, BigDecimal rightValue) {
    return leftValue.multiply(rightValue);
  }
}
