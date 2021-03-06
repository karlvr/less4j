package com.github.sommeri.less4j.core.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.sommeri.less4j.core.parser.HiddenTokenAwareTree;
import com.github.sommeri.less4j.utils.ArraysUtils;

public abstract class Body <T extends ASTCssNode> extends ASTCssNode {

    private List<T> body = new ArrayList<T>();

    public Body(HiddenTokenAwareTree underlyingStructure) {
      super(underlyingStructure);
    }

    public Body(HiddenTokenAwareTree underlyingStructure, List<T> declarations) {
      this(underlyingStructure);
      body.addAll(declarations);
    }

    @Override
    public List<T> getChilds() {
      return body;
    }

    public List<T> getMembers() {
      return body;
    }

    protected List<T> getBody() {
      return body;
    }

    public boolean isEmpty() {
      return body.isEmpty() && getOrphanComments().isEmpty();
    }

    public void addMembers(List<? extends T> members) {
      body.addAll(members);
    }

    public void addMembersAfter(List<? extends T> nestedRulesets, ASTCssNode kid) {
      int index = body.indexOf(kid);
      if (index==-1)
        index = body.size();
      else 
        index++;
      
      body.addAll(index, nestedRulesets);

      
    }

    public void addMember(T member) {
      body.add(member);
    }

    public void replaceMember(T oldMember, List<T> newMembers) {
      body.addAll(body.indexOf(oldMember), newMembers);
      body.remove(oldMember);
      oldMember.setParent(null);
      configureParentToAllChilds();
    }

    public void replaceMember(T oldMember, T newMember) {
      body.add(body.indexOf(oldMember), newMember);
      body.remove(oldMember);
      oldMember.setParent(null);
      newMember.setParent(this);
    }

    public List<T> membersByType(ASTCssNodeType type) {
      List<T> result = new ArrayList<T>();
      List<T> body = getBody();
      for (T node : body) {
        if (node.getType()==type) {
          result.add(node);
        }
      }
      return result;
    }

    public List<T> membersByNotType(ASTCssNodeType type) {
      List<T> result = new ArrayList<T>();
      List<T> body = getBody();
      for (T node : body) {
        if (node.getType()!=type) {
          result.add(node);
        }
      }
      return result;
    }

    public boolean removeMember(T node) {
      return body.remove(node);
    }
    
    public Set<ASTCssNodeType> getSupportedMembers() {
      return new HashSet<ASTCssNodeType>(Arrays.asList(ASTCssNodeType.values()));  
    }

    @SuppressWarnings("unchecked")
    public Body<T> clone() {
      Body<T> result = (Body<T>) super.clone();
      result.body = ArraysUtils.deeplyClonedList(body);
      result.configureParentToAllChilds();
      return result;
    }

    public List<T> getDeclarations() {
      return membersByType(ASTCssNodeType.DECLARATION);
    }

    public List<T> getNotDeclarations() {
      return membersByNotType(ASTCssNodeType.DECLARATION);
    }

}
