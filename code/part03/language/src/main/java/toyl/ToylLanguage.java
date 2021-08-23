package toyl;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import toyl.ast.ToylNode;
import toyl.ast.ToylProgramNode;
import toyl.parser.ToylLexer;
import toyl.parser.ToylParseTreeVisitor;
import toyl.parser.ToylParser;

import java.io.IOException;
import java.math.BigDecimal;

@TruffleLanguage.Registration(
    id = ToylLanguage.ID,
    name = "Toyl", defaultMimeType = ToylLanguage.MIME_TYPE,
    characterMimeTypes = ToylLanguage.MIME_TYPE,
    contextPolicy = TruffleLanguage.ContextPolicy.SHARED)
public class ToylLanguage extends TruffleLanguage<ToylContext> {

  public static final String ID = "toyl";
  public static final String MIME_TYPE = "application/x-toyl";

  @Override
  protected ToylContext createContext(Env env) {
    return new ToylContext();
  }

  @Override
  protected CallTarget parse(ParsingRequest request) throws IOException {
    var expr = this.parseExpr(request.getSource());
    var program = new ToylProgramNode(this, new FrameDescriptor(), expr);
    return Truffle.getRuntime().createCallTarget(program);
  }

  private ToylNode parseExpr(Source source) throws IOException {
    var lexer = new ToylLexer(CharStreams.fromReader(source.getReader()));
    var parser = new ToylParser(new CommonTokenStream(lexer));
    var parseTreeVisitor = new ToylParseTreeVisitor();
    return parseTreeVisitor.visit(parser.program());
  }

}
