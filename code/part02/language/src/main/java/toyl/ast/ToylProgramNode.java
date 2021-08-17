package toyl.ast;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class ToylProgramNode extends RootNode {

  private final long program;

  public ToylProgramNode(TruffleLanguage<?> language, FrameDescriptor frameDescriptor, String program) {
    super(language, frameDescriptor);
    this.program = Long.parseLong(program);
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return this.program;
  }
}
