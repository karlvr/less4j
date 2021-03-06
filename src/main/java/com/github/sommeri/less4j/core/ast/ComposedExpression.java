package com.github.sommeri.less4j.core.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.sommeri.less4j.core.ast.ExpressionOperator.Operator;
import com.github.sommeri.less4j.core.parser.HiddenTokenAwareTree;
import com.github.sommeri.less4j.utils.ArraysUtils;

public class ComposedExpression extends Expression {

  private Expression left;
  private ExpressionOperator operator;
  private Expression right;

  public ComposedExpression(HiddenTokenAwareTree token, Expression left, ExpressionOperator operator, Expression right) {
    super(token);
    this.left = left;
    this.operator = operator;
    this.right = right;
  }

  @Override
  public ASTCssNodeType getType() {
    return ASTCssNodeType.COMPOSED_EXPRESSION;
  }
  
  public ExpressionOperator getOperator() {
    return operator;
  }

  public void setOperator(ExpressionOperator operator) {
    this.operator = operator;
  }
 
  public Expression getLeft() {
    return left;
  }

  public void setLeft(Expression left) {
    this.left = left;
  }

  public Expression getRight() {
    return right;
  }

  public void setRight(Expression right) {
    this.right = right;
  }

  public List<Expression> splitByComma() {
    List<Expression> result = new ArrayList<Expression>();
    if (operator.getOperator()!=Operator.COMMA && operator.getOperator()!=Operator.EMPTY_OPERATOR) {
      result.add(this);
      return result;
    }

    List<Expression> left = splitByComma(getLeft());
    List<Expression> right = splitByComma(getRight());
    if (operator.getOperator()!=Operator.EMPTY_OPERATOR) {
      result.addAll(left);
      result.addAll(right);
      return result;
    }
    
    Expression lastLeft = left.get(left.size()-1);
    Expression firstRight = right.get(0);
    result.addAll(left.subList(0, left.size()-1));
    result.add(new ComposedExpression(lastLeft.getUnderlyingStructure(), lastLeft, operator, firstRight));
    result.addAll(right.subList(1, right.size()));

    return result;
  }

  private List<Expression> splitByComma(Expression expression) {
    if (expression.getType()==ASTCssNodeType.COMPOSED_EXPRESSION) {
      ComposedExpression composed = (ComposedExpression) expression;
      return composed.splitByComma();
    } else {
      return Arrays.asList(expression);
    }
  }
  
  @Override
  public List<? extends ASTCssNode> getChilds() {
    return ArraysUtils.asNonNullList(left, operator, right);
  }

  @Override
  public String toString() {
    return "[" + left + operator + right + "]";
  }

  @Override
  public ComposedExpression clone() {
    ComposedExpression result = (ComposedExpression) super.clone();
    result.left = left==null?null:left.clone();
    result.operator = operator==null?null:operator.clone();
    result.right = right==null?null:right.clone();
    result.configureParentToAllChilds();
    return result;
  }
}
