package com.github.sommeri.less4j.core.ast;

import java.util.ArrayList;
import java.util.List;

import com.github.sommeri.less4j.core.parser.HiddenTokenAwareTree;
import com.github.sommeri.less4j.utils.ArraysUtils;

public class Media extends Body<ASTCssNode> {

  private List<MediaQuery> mediums;

  public Media(HiddenTokenAwareTree token) {
    super(token);
  }

  public void addDeclaration(Declaration declaration) {
    addMember(declaration);
  }

  public void addRuleSet(RuleSet ruleSet) {
    addMember(ruleSet);
  }

  public void addChild(ASTCssNode child) {
    if (!(child.getType() == ASTCssNodeType.RULE_SET || child.getType() == ASTCssNodeType.VARIABLE_DECLARATION ||child.getType() == ASTCssNodeType.DECLARATION || child.getType() == ASTCssNodeType.MEDIUM || child.getType() == ASTCssNodeType.MEDIA_QUERY|| child.getType() == ASTCssNodeType.VIEWPORT|| child.getType() == ASTCssNodeType.PAGE))
      throw new IllegalArgumentException("Unexpected media child type: " + child.getType());

    if (child.getType() == ASTCssNodeType.MEDIA_QUERY)
      addMediaQuery((MediaQuery) child);
    else
      addMember(child);
  }

  public void addMediaQuery(MediaQuery medium) {
    if (mediums==null) {
      mediums = new ArrayList<MediaQuery>();
    }
    mediums.add(medium);
  }

  public List<MediaQuery> getMediums() {
    return mediums;
  }

  public void setMediums(List<MediaQuery> mediums) {
    this.mediums = mediums;
  }

  @Override
  public List<ASTCssNode> getChilds() {
    List<ASTCssNode> childs = new ArrayList<ASTCssNode>(super.getChilds());
    childs.addAll(mediums);
    return childs;
  }

  @Override
  public ASTCssNodeType getType() {
    return ASTCssNodeType.MEDIA;
  }

  @Override
  public Media clone() {
    Media result = (Media) super.clone();
    result.mediums = ArraysUtils.deeplyClonedList(mediums);
    result.configureParentToAllChilds();
    return result;
  }
}
