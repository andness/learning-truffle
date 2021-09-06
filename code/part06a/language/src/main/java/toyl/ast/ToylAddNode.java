package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import java.math.BigDecimal;

@NodeChild("left")
@NodeChild("right")
public abstract class ToylAddNode extends ToylExpressionNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  protected long addLongs(long leftValue, long rightValue) {
    return Math.addExact(leftValue, rightValue);
  }

  @Specialization(replaces = "addLongs")
  protected BigDecimal addNumbers(BigDecimal leftValue, BigDecimal rightValue) {
    return leftValue.add(rightValue);
  }
}
