package com.github.sommeri.less4j.core.compiler.stages;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.sommeri.less4j.core.ast.ASTCssNode;
import com.github.sommeri.less4j.core.ast.ASTCssNodeType;
import com.github.sommeri.less4j.core.ast.ArgumentDeclaration;
import com.github.sommeri.less4j.core.ast.CssString;
import com.github.sommeri.less4j.core.ast.Declaration;
import com.github.sommeri.less4j.core.ast.EscapedSelector;
import com.github.sommeri.less4j.core.ast.EscapedValue;
import com.github.sommeri.less4j.core.ast.Expression;
import com.github.sommeri.less4j.core.ast.FixedNamePart;
import com.github.sommeri.less4j.core.ast.IndirectVariable;
import com.github.sommeri.less4j.core.ast.InterpolableName;
import com.github.sommeri.less4j.core.ast.MixinReference;
import com.github.sommeri.less4j.core.ast.ReusableStructure;
import com.github.sommeri.less4j.core.ast.RuleSetsBody;
import com.github.sommeri.less4j.core.ast.SimpleSelector;
import com.github.sommeri.less4j.core.ast.Variable;
import com.github.sommeri.less4j.core.ast.VariableNamePart;
import com.github.sommeri.less4j.core.compiler.expressions.ExpressionEvaluator;
import com.github.sommeri.less4j.core.compiler.expressions.strings.StringInterpolator;
import com.github.sommeri.less4j.core.compiler.scopes.FullMixinDefinition;
import com.github.sommeri.less4j.core.compiler.scopes.IteratedScope;
import com.github.sommeri.less4j.core.compiler.scopes.Scope;
import com.github.sommeri.less4j.core.parser.HiddenTokenAwareTree;
import com.github.sommeri.less4j.core.problems.ProblemsHandler;
import com.github.sommeri.less4j.utils.ArraysUtils;
import com.github.sommeri.less4j.utils.QuotesKeepingInStringCssPrinter;

public class ReferencesSolver {

  public static final String ALL_ARGUMENTS = "@arguments";
  private ASTManipulator manipulator = new ASTManipulator();
  private final ProblemsHandler problemsHandler;
  private StringInterpolator stringInterpolator = new StringInterpolator();
  
  public ReferencesSolver(ProblemsHandler problemsHandler) {
    this.problemsHandler = problemsHandler;
  }

  public void solveReferences(ASTCssNode node, Scope scope) {
    doSolveReferences(node, new IteratedScope(scope));
  }

  private void doSolveReferences(ASTCssNode node, Scope scope) {
    doSolveReferences(node, new IteratedScope(scope));
  }

  private void doSolveReferences(ASTCssNode node, IteratedScope scope) {
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(scope.getScope(), problemsHandler);

    switch (node.getType()) {
    case VARIABLE: {
      Expression replacement = expressionEvaluator.evaluate((Variable) node);
      manipulator.replace(node, replacement);
      break;
    }
    case INDIRECT_VARIABLE: {
      Expression replacement = expressionEvaluator.evaluate((IndirectVariable) node);
      manipulator.replace(node, replacement);
      break;
    }
    case STRING_EXPRESSION: {
      Expression replacement = expressionEvaluator.evaluate((CssString) node); 
      manipulator.replace(node, replacement);
      break;
    }
    case ESCAPED_VALUE: {
      Expression replacement = expressionEvaluator.evaluate((EscapedValue) node); 
      manipulator.replace(node, replacement);
      break;
    }
    case MIXIN_REFERENCE: {
      MixinReference mixinReference = (MixinReference) node;
      RuleSetsBody replacement = resolveMixinReference(mixinReference, scope.getScope());
      AstLogic.validateCssBodyCompatibility(mixinReference, replacement.getChilds(), problemsHandler);
      manipulator.replaceInBody(mixinReference, replacement.getChilds());
      break;
    }
    case ESCAPED_SELECTOR: {
      SimpleSelector replacement = interpolateEscapedSelector((EscapedSelector) node, expressionEvaluator); 
      manipulator.replace(node, replacement);
      break;
    }
    case FIXED_NAME_PART: {
      FixedNamePart part = (FixedNamePart) node;
      FixedNamePart replacement = interpolateFixedNamePart(part, expressionEvaluator); 
      part.getParent().replaceMember(part, replacement);
      break;
    }
    case VARIABLE_NAME_PART: {
      VariableNamePart part = (VariableNamePart) node;
      Expression value = expressionEvaluator.evaluate(part.getVariable());
      FixedNamePart fixedName = toFixedName(value, node.getUnderlyingStructure());
      part.getParent().replaceMember(part, interpolateFixedNamePart(fixedName, expressionEvaluator));
      break;
    }
    }

    if (node.getType() != ASTCssNodeType.VARIABLE_NAME_PART) {
      List<ASTCssNode> childs = new ArrayList<ASTCssNode>(node.getChilds());
      for (ASTCssNode kid : childs) {
        if (AstLogic.hasOwnScope(kid)) {
          doSolveReferences(kid, new IteratedScope(scope.getNextChild()));
        } else {
          doSolveReferences(kid, scope);
        }
      }
    }
  }

  private FixedNamePart toFixedName(Expression value, HiddenTokenAwareTree parent) {
    QuotesKeepingInStringCssPrinter printer = new QuotesKeepingInStringCssPrinter();
    printer.append(value);    
    //property based alternative would be nice, but does not seem to be needed
    FixedNamePart fixedName = new FixedNamePart(parent, printer.toString());
    return fixedName;
  }

  private SimpleSelector interpolateEscapedSelector(EscapedSelector input, ExpressionEvaluator expressionEvaluator) {
    HiddenTokenAwareTree underlying = input.getUnderlyingStructure();
    String value = stringInterpolator.replaceIn(input.getValue(), expressionEvaluator, input.getUnderlyingStructure());
    InterpolableName interpolableName = new InterpolableName(underlying, new FixedNamePart(underlying, value));
    return new SimpleSelector(input.getUnderlyingStructure(), interpolableName, false);
  }

  private FixedNamePart interpolateFixedNamePart(FixedNamePart input, ExpressionEvaluator expressionEvaluator) {
    String value = stringInterpolator.replaceIn(input.getName(), expressionEvaluator, input.getUnderlyingStructure());
    return new FixedNamePart(input.getUnderlyingStructure(), value);
  }

  private RuleSetsBody resolveMixinReference(MixinReference reference, Scope scope) {
    List<FullMixinDefinition> sameNameMixins = scope.getNearestMixins(reference, problemsHandler);
    return resolveReferencedMixins(reference, scope, sameNameMixins);
  }

  private RuleSetsBody resolveReferencedMixins(MixinReference reference, Scope referenceScope, List<FullMixinDefinition> sameNameMixins) {
    if (sameNameMixins.isEmpty())
      problemsHandler.undefinedMixin(reference);

    List<FullMixinDefinition> mixins = (new MixinsReferenceMatcher(referenceScope, problemsHandler)).filter(reference, sameNameMixins);
    if (mixins.isEmpty())
      problemsHandler.unmatchedMixin(reference);

    RuleSetsBody result = new RuleSetsBody(reference.getUnderlyingStructure());
    for (FullMixinDefinition fullMixin : mixins) {
      Scope combinedScope = calculateMixinsOwnVariables(reference, referenceScope, fullMixin);
      ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(combinedScope, problemsHandler);

      ReusableStructure mixin = fullMixin.getMixin();
      if (expressionEvaluator.evaluate(mixin.getGuards())) {
        RuleSetsBody body = mixin.getBody().clone();
        doSolveReferences(body, combinedScope);
        result.addMembers(body.getChilds());
      }
    }

    resolveImportance(reference, result);
    shiftComments(reference, result);

    return result;
  }

  private void shiftComments(MixinReference reference, RuleSetsBody result) {
    List<ASTCssNode> childs = result.getChilds();
    if (!childs.isEmpty()) {
      childs.get(0).addOpeningComments(reference.getOpeningComments());
      childs.get(childs.size() - 1).addTrailingComments(reference.getTrailingComments());
    }
  }

  private void resolveImportance(MixinReference reference, RuleSetsBody result) {
    if (reference.isImportant()) {
      declarationsAreImportant(result);
    }
  }

  private void declarationsAreImportant(RuleSetsBody result) {
    for (ASTCssNode kid : result.getChilds()) {
      if (kid instanceof Declaration) {
        Declaration declaration = (Declaration) kid;
        declaration.setImportant(true);
      }
    }
  }

  private Scope calculateMixinsOwnVariables(MixinReference reference, Scope referenceScope, FullMixinDefinition mixin) {
    Scope joinScopes = joinScopes(mixin.getScope(), buildMixinsArgumentsScope(reference, referenceScope, mixin), referenceScope);
    return joinScopes;
  }

  public static Scope joinScopes(Scope mixinsScope, Scope arguments, Scope callerScope) {
    Scope result = mixinsScope.copyWithChildChain(arguments);
    Scope mixinsScopeParent = mixinsScope.getParent();
    if (mixinsScopeParent != null) {
      arguments.setParent(mixinsScopeParent.copyWithParentsChain());
    }
    Scope rootOfTheMixinsScope = result.getRootScope();
    rootOfTheMixinsScope.setParent(callerScope.copyWithParentsChain());
    return result;
  }

  private Scope buildMixinsArgumentsScope(MixinReference reference, Scope referenceScope, FullMixinDefinition mixin) {
    ArgumentsBuilder builder = new ArgumentsBuilder(reference, mixin.getMixin(), new ExpressionEvaluator(referenceScope, problemsHandler), problemsHandler);
    return builder.build();
  }

}

class ArgumentsBuilder {

  //utils
  private final ProblemsHandler problemsHandler;
  private final ExpressionEvaluator referenceEvaluator;
  private final String ALL_ARGUMENTS = ReferencesSolver.ALL_ARGUMENTS;

  //input
  private Iterator<Expression> positionalParameters;
  private ReusableStructure mixin;
  private MixinReference reference;

  //results
  private List<Expression> allValues = new ArrayList<Expression>();
  private Scope argumentsScope;

  public ArgumentsBuilder(MixinReference reference, ReusableStructure pureMixin, ExpressionEvaluator referenceEvaluator, ProblemsHandler problemsHandler) {
    super();
    this.referenceEvaluator = referenceEvaluator;
    this.problemsHandler = problemsHandler;
    positionalParameters = reference.getPositionalParameters().iterator();
    argumentsScope = Scope.createScope(reference, "#arguments-" + reference + "#", null);
    mixin = pureMixin;
    this.reference = reference;
  }

  public Scope build() {
    int length = mixin.getParameters().size();
    for (int i = 0; i < length; i++) {
      ASTCssNode parameter = mixin.getParameters().get(i);
      if (parameter.getType() == ASTCssNodeType.ARGUMENT_DECLARATION) {
        add((ArgumentDeclaration) parameter);
      } else {
        skipPositionalParameter();
      }
          
    } 
    
    Expression compoundedValues = referenceEvaluator.joinAll(allValues, reference);
    argumentsScope.registerVariableIfNotPresent(ALL_ARGUMENTS, compoundedValues);
    return argumentsScope;
  }

  private void skipPositionalParameter() {
    positionalParameters.next();
  }

  private void add(ArgumentDeclaration declaration) {
    if (canFillFromNamed(declaration)) {
      fillFromNamed(declaration);
    } else if (declaration.isCollector()) {
      addAsCollector(declaration);
    } else if (canFillFromPositional()) {
      fillFromPositional(declaration);
    } else if (hasDefault(declaration)) {
      fillFromDefault(declaration);
    } else {
      if (declaration.getValue() == null)
        problemsHandler.undefinedMixinParameterValue(mixin, declaration, reference);
    }

  }

  private void fillFromNamed(ArgumentDeclaration declaration) {
    Expression value = referenceEvaluator.evaluate(reference.getNamedParameter(declaration.getVariable()));
    allValues.add(value);
    argumentsScope.registerVariable(declaration, value);
  }

  private boolean canFillFromNamed(ArgumentDeclaration declaration) {
    return reference.hasNamedParameter(declaration.getVariable());
  }

  private void fillFromDefault(ArgumentDeclaration declaration) {
    allValues.add(declaration.getValue());
    argumentsScope.registerVariable(declaration);
  }

  private boolean hasDefault(ArgumentDeclaration declaration) {
    return declaration.getValue() != null;
  }

  private void fillFromPositional(ArgumentDeclaration declaration) {
    Expression value = referenceEvaluator.evaluate(positionalParameters.next());
    allValues.add(value);
    argumentsScope.registerVariable(declaration, value);
  }

  private boolean canFillFromPositional() {
    return positionalParameters.hasNext();
  }

  private void addAsCollector(ArgumentDeclaration declaration) {
    List<Expression> allArgumentsFrom = referenceEvaluator.evaluateAll(ArraysUtils.remaining(positionalParameters));
    allValues.addAll(allArgumentsFrom);
    Expression value = referenceEvaluator.joinAll(allArgumentsFrom, reference);
    argumentsScope.registerVariable(declaration, value);

  }

}
