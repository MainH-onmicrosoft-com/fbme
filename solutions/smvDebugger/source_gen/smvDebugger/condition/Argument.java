package smvDebugger.condition;

/*Generated by MPS */

import java.util.Map;

public class Argument implements Expression {
  private final String content;

  public Argument(final String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  @Override
  public boolean evaluate(final Map<String, String> stepValues) {
    return true;
  }
}
