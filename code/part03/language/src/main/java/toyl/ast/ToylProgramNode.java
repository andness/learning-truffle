package toyl.ast;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class ToylProgramNode extends RootNode {

  private final ToylNode expr;

  public ToylProgramNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, ToylNode expr) {
    super(language, frameDescriptor);
    this.expr = expr;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return this.expr.execute(frame);
  }
}
