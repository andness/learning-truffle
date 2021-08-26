package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

import java.util.ArrayList;
import java.util.List;

public class ToylProgramNode extends ToylStatementNode {

  private final List<ToylStatementNode> statements = new ArrayList<>();

  public ToylProgramNode(List<ToylStatementNode> statements) {
    this.statements.addAll(statements);
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object result = null;
    for (ToylStatementNode statement : statements) {
      result = statement.executeGeneric(frame);
    }
    return result != null ? result.toString() : null;
  }

}
