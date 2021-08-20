package toyl.ast;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import toyl.ToylTypeSystem;

@TypeSystemReference(ToylTypeSystem.class)
public abstract class ToylStatementNode extends Node {
  public abstract Object executeGeneric(VirtualFrame frame);
}
