package com.github.sommeri.less4j.core.ast;

import com.github.sommeri.less4j.core.parser.HiddenTokenAwareTree;

public abstract class Expression extends ASTCssNode {

  public Expression(HiddenTokenAwareTree token) {
    super(token);
  }

  @Override
  public Expression clone() {
    Expression clone = (Expression) super.clone();
    return clone;
  }

}
