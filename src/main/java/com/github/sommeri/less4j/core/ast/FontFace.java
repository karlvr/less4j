package com.github.sommeri.less4j.core.ast;

import java.util.List;

import com.github.sommeri.less4j.core.parser.HiddenTokenAwareTree;

public class FontFace extends Body<Declaration> {

  public FontFace(HiddenTokenAwareTree underlyingStructure) {
    super(underlyingStructure);
  }

  public FontFace(HiddenTokenAwareTree underlyingStructure, List<Declaration> declarations) {
    super(underlyingStructure, declarations);
  }

  @Override
  public ASTCssNodeType getType() {
    return ASTCssNodeType.FONT_FACE;
  }

  @Override
  public FontFace clone() {
    return (FontFace) super.clone();
  }
}
