package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeField(name = "name", type = String.class)
@NodeField(name = "slot", type = FrameSlot.class)
@NodeChild("expr")
public abstract class ToylAssignmentNode extends ToylStatementNode {
  
  protected abstract FrameSlot getSlot();

  protected boolean isIntOrIllegal(VirtualFrame frame) {
    return frame.isInt(getSlot()) || frame.getFrameDescriptor().getFrameSlotKind(getSlot()) == FrameSlotKind.Illegal;
  }

  protected boolean isDoubleOrIllegal(VirtualFrame frame) {
    return frame.isDouble(getSlot()) || frame.getFrameDescriptor().getFrameSlotKind(getSlot()) == FrameSlotKind.Illegal;
  }

  @Specialization(guards = "isIntOrIllegal(frame)")
  public int assignInt(VirtualFrame frame, int value) {
    frame.setInt(this.getSlot(), value);
    return value;
  }

  @Specialization(guards = "isDoubleOrIllegal(frame)")
  public double assignDouble(VirtualFrame frame, double value) {
    frame.setDouble(this.getSlot(), value);
    return value;
  }

}
