package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import java.math.BigDecimal;
import java.math.MathContext;

@NodeChild("left") @NodeChild("right")
public abstract class ToylDivNode extends ToylNode {
  @Specialization(rewriteOn = ArithmeticException.class)
  protected int divInts(int leftValue, int rightValue) {
    return leftValue / rightValue;
  }

  @Specialization(replaces = "divInts")
  protected BigDecimal divNumbers(BigDecimal leftValue, BigDecimal rightValue) {
    return leftValue.divide(rightValue, MathContext.DECIMAL128);
  }
}
