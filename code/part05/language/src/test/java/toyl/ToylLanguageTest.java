package toyl;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToylLanguageTest {

  Context context;

  @BeforeEach
  void setUp() {
    context = Context.create();
  }

  @AfterEach
  void tearDown() {
    context.close();
  }

  private int evalInt(String program) {
    return context.eval("toyl", program).asInt();
  }

  private double evalDouble(String program) {
    return context.eval("toyl", program).asDouble();
  }

  @Test
  void testIntegerAddition() {
    assertEquals(2 + 2, evalInt("2+2"));
  }

  @Test
  void testDoubleAddition() {
    assertEquals(2.0 + 2.0, evalDouble("2.0+2.0"));
  }

  @Test
  void testMixedAddition() {
    assertEquals(2 + 2.0, evalDouble("2+2.0"));
  }

  @Test
  void testIntegerOverflow() {
    assertEquals(Integer.MAX_VALUE + 1L, evalDouble("2147483647 + 1"));
  }

  @Test
  void testVariables() {
    var program = """
        var pi = 3.14
        var r = 42
        pi * r * r
        """;
    assertEquals(5538.96, evalDouble(program));
  }

  @Test
  void testIntVariables() {
    var program = """
        var a = 2
        var b = 2
        a + b
        """;
    assertEquals(4, evalInt(program));
  }

}