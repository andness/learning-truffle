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
import toyl.ast.ToylRootNode;
import toyl.parser.ToylErrorListener;
import toyl.parser.ToylLexer;
import toyl.parser.ToylParseTreeVisitor;
import toyl.parser.ToylParser;

import java.io.IOException;

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
    final FrameDescriptor frameDescriptor = new FrameDescriptor();
    var statements = this.parseProgram(frameDescriptor, request.getSource());
    var program = new ToylRootNode(this, frameDescriptor, statements);
    return Truffle.getRuntime().createCallTarget(program);
  }

  private ToylNode parseProgram(FrameDescriptor frameDescriptor, Source source) throws IOException {
    var lexer = new ToylLexer(CharStreams.fromReader(source.getReader()));
    var parser = new ToylParser(new CommonTokenStream(lexer));
    lexer.removeErrorListeners();
    parser.removeErrorListeners();
    final ToylErrorListener errorListener = new ToylErrorListener(source);
    lexer.addErrorListener(errorListener);
    parser.addErrorListener(errorListener);
    var parseTreeVisitor = new ToylParseTreeVisitor(frameDescriptor);
    return parseTreeVisitor.visitProgram(parser.program());
  }

}
