package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import java.math.BigDecimal;

public class ToylAddNode extends ToylNode {

  private ToylNode left;
  private ToylNode right;

  public ToylAddNode(ToylNode left, ToylNode right) {
    this.left = left;
    this.right = right;
  }

  private enum SpecializationState {
    LONG,
    NUMBER
  }

  private SpecializationState specializationState = SpecializationState.LONG;

  @Override
  public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
    long leftVal;
    try {
      leftVal = this.left.executeLong(frame);
    } catch (UnexpectedResultException e) {
      specializationState = SpecializationState.NUMBER;
      BigDecimal leftDecimal = (BigDecimal) e.getResult();
      throw new UnexpectedResultException(leftDecimal.add(this.right.executeNumber(frame)));
    }
    long rightVal;
    try {
      rightVal = this.right.executeLong(frame);
    } catch (UnexpectedResultException e) {
      specializationState = SpecializationState.NUMBER;
      BigDecimal rightDecimal = (BigDecimal) e.getResult();
      throw new UnexpectedResultException(rightDecimal.add(this.right.executeNumber(frame)));
    }
    try {
      return Math.addExact(leftVal, rightVal);
    } catch (ArithmeticException e) {
      specializationState = SpecializationState.NUMBER;
      throw new UnexpectedResultException(new BigDecimal(leftVal).add(new BigDecimal(rightVal)));
    }
  }

  @Override
  public BigDecimal executeNumber(VirtualFrame frame) {
    return this.left.executeNumber(frame).add(this.right.executeNumber(frame));
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    if (this.specializationState == SpecializationState.NUMBER) {
      return this.executeNumber(frame);
    }
    try {
      var value = this.executeLong(frame);
      this.specializationState = SpecializationState.LONG;
      return value;
    } catch (UnexpectedResultException e) {
      this.specializationState = SpecializationState.NUMBER;
      return e.getResult();
    }
  }
}
