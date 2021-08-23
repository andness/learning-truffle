package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import java.math.BigDecimal;

@NodeChild("left") @NodeChild("right")
public abstract class ToylAddNode extends ToylNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  protected int addInts(int leftValue, int rightValue) {
    return Math.addExact(leftValue, rightValue);
  }

  @Specialization(replaces = "addInts")
  protected BigDecimal addNumbers(BigDecimal leftValue, BigDecimal rightValue) {
    return leftValue.add(rightValue);
  }
}
