package toyl;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;

import java.math.BigDecimal;

@TypeSystem
public class ToylTypeSystem {
  @ImplicitCast
  public static BigDecimal castLongToBigDecimal(long value) {
    return new BigDecimal(value);
  }
}
