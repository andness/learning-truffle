package toyl.ast;

import com.oracle.truffle.api.frame.VirtualFrame;

public class ToylArithmeticOpNode extends ToylNode {

  private final ToylNode left;
  private final ToylNode right;
  private final String binaryOp;

  public ToylArithmeticOpNode(ToylNode left, ToylNode right, String binaryOp) {
    this.left = left;
    this.right = right;
    this.binaryOp = binaryOp;
  }

  @Override
  public long execute(VirtualFrame frame) {
    var leftVal = this.left.execute(frame);
    var rightVal = this.left.execute(frame);
    return switch (this.binaryOp) {
      case "+" -> leftVal + rightVal;
      case "-" -> leftVal - rightVal;
      case "*" -> leftVal * rightVal;
      case "/" -> leftVal / rightVal;
      default -> throw new AssertionError("Unexpected operator " + this.binaryOp);
    };
  }
}
