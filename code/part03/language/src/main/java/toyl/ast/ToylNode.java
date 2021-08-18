package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public abstract class ToylNode extends Node {
  public abstract double execute(VirtualFrame frame);
}
