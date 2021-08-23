package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import java.math.BigDecimal;

public abstract class ToylNode extends Node {
  public abstract BigDecimal execute(VirtualFrame frame);
}
