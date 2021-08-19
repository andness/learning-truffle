package toyl.ast;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import toyl.ToylTypeSystem;

@TypeSystemReference(ToylTypeSystem.class)
public abstract class ToylNode extends Node {
  public abstract int executeInt(VirtualFrame frame) throws UnexpectedResultException;
  public abstract double executeDouble(VirtualFrame frame);
  public abstract Object executeGeneric(VirtualFrame frame);
}
