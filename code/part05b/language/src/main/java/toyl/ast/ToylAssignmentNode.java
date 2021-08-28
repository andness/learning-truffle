package toyl.ast;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeField(name = "name", type = String.class)
@NodeField(name = "slot", type = FrameSlot.class)
@NodeChild("expr")
public abstract class ToylAssignmentNode extends ToylNode {

  abstract FrameSlot getSlot();
  abstract String getName();

  protected boolean isLong() {
    return true;
  }

  @Specialization(guards = "isLongOrIllegal(frame)")
  public long assignLong(VirtualFrame frame, long value) {
    frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Long);
    frame.setLong(getSlot(), value);
    return value;
  }

  @Fallback
  public Object assignNumber(VirtualFrame frame, Object value) {
    frame.getFrameDescriptor().setFrameSlotKind(getSlot(), FrameSlotKind.Object);
    frame.setObject(getSlot(), value);
    return value;
  }

  protected boolean isLongOrIllegal(VirtualFrame frame) {
    var kind = frame.getFrameDescriptor().getFrameSlotKind(getSlot());
    return kind == FrameSlotKind.Long || kind == FrameSlotKind.Illegal;
  }

  protected boolean isObjectOrIllegal(VirtualFrame frame) {
    var kind = frame.getFrameDescriptor().getFrameSlotKind(getSlot());
    return kind == FrameSlotKind.Object || kind == FrameSlotKind.Illegal;
  }
}