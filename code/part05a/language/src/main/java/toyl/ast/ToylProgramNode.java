package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

import java.util.ArrayList;
import java.util.List;
public class ToylProgramNode extends ToylNode {

  private final List<ToylNode> statements = new ArrayList<>();

  public ToylProgramNode(List<ToylNode> statements) {
    this.statements.addAll(statements);
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object result = null;
    for (ToylNode statement : statements) {
      result = statement.executeGeneric(frame);
    }
    return result != null ? result.toString() : null;
  }

}