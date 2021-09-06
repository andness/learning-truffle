package toyl.ast;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import toyl.ToylTypeSystem;

import java.math.BigDecimal;

@TypeSystemReference(ToylTypeSystem.class)
public abstract class ToylNode extends Node {
  public abstract Object executeGeneric(VirtualFrame frame);
}
