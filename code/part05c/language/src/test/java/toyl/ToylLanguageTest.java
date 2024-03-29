package toyl;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toyl.errors.ToylSemanticError;

import static org.junit.jupiter.api.Assertions.*;

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
  void testIntegerAddition() {
    assertEquals("5", eval("2+3"));
    assertEquals("5", eval("2+3.0"));
    assertEquals("5", eval("2.0+3.0"));
  }

  @Test
  void testDecimalAddition() {
    assertEquals("4.15", eval("3.14+1.01"));
  }

  @Test
  void testIntegerOverflow() {
    assertEquals("9223372036854775808", eval("9223372036854775807 + 1"));
  }

  @Test
  void testDivision() {
    assertEquals("1.5", eval("3.0/2"));
    assertEquals("1.5", eval("3/2"));
    assertEquals("0.3333333333333333333333333333333333", eval("1/3"));
  }

  @Test
  void testAllOps() {
    assertEquals("2.5", eval("(4-3+1)*5/4"));
  }

  @Test
  void testNegativeLiteral() {
    assertEquals("-1", eval("-1"));
    assertEquals("2", eval("1- -1"));
    assertEquals("2", eval("1 - -1"));
    assertEquals("1", eval("2 -1"));
    assertEquals("1", eval("2-1"));
  }

  @Test
  void testVariables() {
    var program = """
        var pi = 3.14        
        var r = 42       
        pi * r * r
        """;
    assertEquals("5538.96", eval(program));
  }

  @Test
  void testReassign() {
    var program = """
        var a = 1
        a = 1.5        
        a
        """;
    assertEquals("1.5", eval(program));
  }

  @Test
  void testReassignNumberToLong() {
    var program = """
        var a = 1.5
        a = 1        
        a
        """;
    assertEquals("1", eval(program));
  }

  @Test
  void testRedeclaringVariableIsAnError() {
    var program = """
        var a = 1.5
        var a = 1        
        """;
    var error = assertThrows(PolyglotException.class, () -> eval(program));
    assertTrue(error.getMessage().startsWith("Attempt to redeclare previously declared variable"));
  }
}