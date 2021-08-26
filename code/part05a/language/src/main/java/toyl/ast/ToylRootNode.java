package toyl.ast;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class ToylRootNode extends RootNode {

  private final ToylNode program;

  public ToylRootNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, ToylNode program) {
    super(language, frameDescriptor);
    this.program = program;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return program.executeGeneric(frame);
  }
}
