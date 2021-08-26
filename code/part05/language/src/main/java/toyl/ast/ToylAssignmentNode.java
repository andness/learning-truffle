package toyl.ast;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.math.BigDecimal;

@NodeField(name = "name", type = String.class)
@NodeField(name = "slot", type = FrameSlot.class)
@NodeChild("expr")
public abstract class ToylAssignmentNode extends ToylStatementNode {
  
  protected abstract FrameSlot getSlot();

  protected boolean isLongOrIllegal(VirtualFrame frame) {
    return frame.isLong(getSlot()) || frame.getFrameDescriptor().getFrameSlotKind(getSlot()) == FrameSlotKind.Illegal;
  }

  protected boolean isBigDecimalOrIllegal(VirtualFrame frame) {
    if (frame.getFrameDescriptor().getFrameSlotKind(getSlot()) == FrameSlotKind.Illegal) {
      return true;
    }
    if (frame.isObject(getSlot())) {
      try {
        var object = frame.getObject(getSlot());
        if (object instanceof BigDecimal) {
          return true;
        }
      } catch (FrameSlotTypeException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  @Specialization(guards = "isLongOrIllegal(frame)")
  public long assignLong(VirtualFrame frame, long value) {
    frame.setLong(this.getSlot(), value);
    return value;
  }

  @Specialization(guards = "isBigDecimalOrIllegal(frame)")
  public BigDecimal assignNumber(VirtualFrame frame, BigDecimal value) {
    frame.setObject(this.getSlot(), value);
    return value;
  }

}
