package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import java.math.BigDecimal;

public abstract class ToylNode extends Node {
  public abstract long executeLong(VirtualFrame frame) throws UnexpectedResultException;
  public abstract BigDecimal executeNumber(VirtualFrame frame);
  public abstract Object executeGeneric(VirtualFrame frame);
}
