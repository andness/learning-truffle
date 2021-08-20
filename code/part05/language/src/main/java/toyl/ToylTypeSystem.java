package toyl;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;

@TypeSystem
public class ToylTypeSystem {
  @ImplicitCast
  public static double castIntToDouble(int value) {
    return value;
  }
}
