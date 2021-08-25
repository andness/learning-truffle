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

  private String eval(String program) {
    return context.eval("toyl", program).asString();
  }
  @Test
  void testAllOps() {
    assertEquals("2.5", eval("(4-2+1)*5/4"));
  }

  @Test
  void testIntegerAddition() {
    assertEquals("4", eval("2+2"));
  }

  @Test
  void testDoubleAddition() {
    assertEquals("4.14", eval("3.14+1.0"));
  }

  @Test
  void testMixedAddition() {
    assertEquals("4.0", eval("2+2.0"));
  }

  @Test
  void testIntegerOverflow() {
    assertEquals("9223372036854775808", eval("9223372036854775807 + 1"));
  }

}