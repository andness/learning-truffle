package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import java.math.BigDecimal;
import java.math.MathContext;

@NodeChild("left")
@NodeChild("right")
public abstract class ToylDivNode extends ToylExpressionNode {

  @Specialization(rewriteOn = ArithmeticException.class)
  protected long divLongs(long leftValue, long rightValue) {
    var mod = leftValue % rightValue;
    if (mod == 0) {
      return leftValue / rightValue;
    } else {
      throw new ArithmeticException();
    }
  }

  @Specialization(replaces = "divLongs")
  protected BigDecimal divNumbers(BigDecimal leftValue, BigDecimal rightValue) {
    return leftValue.divide(rightValue, MathContext.DECIMAL128);
  }
}
