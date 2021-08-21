package toyl.ast;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class ToylRootNode extends RootNode {

  private final ToylProgramNode statements;

  public ToylRootNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, ToylProgramNode statements) {
    super(language, frameDescriptor);
    this.statements = statements;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return statements.executeGeneric(frame);
  }
}
