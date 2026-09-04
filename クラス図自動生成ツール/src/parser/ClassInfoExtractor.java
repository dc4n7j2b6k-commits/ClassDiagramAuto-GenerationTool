
package src.parser;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;
import java.nio.charset.*;
import javax.tools.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;

import src.infos.*;
import src.draw.*;

public class ClassInfoExtractor extends TreePathScanner<Void, Void>{
    List<ClassInfo> classes = new ArrayList<>();
    List<String> currentImports = new ArrayList<>();
    Set<String> API;
    String currentPackage;
    Deque<ClassInfo> stack = new ArrayDeque<>();
    Types typeUtils;
    Trees trees;

    public ClassInfoExtractor(Types typeUtils, Trees trees, Set<String> API){
        this.typeUtils = typeUtils;
        this.trees = trees;
        this.API = API;
    }
    @Override
    public Void visitCompilationUnit(CompilationUnitTree unit, Void p){
        if(unit.getPackageName() != null){
            currentPackage = unit.getPackageName().toString();
        }
        List<? extends ImportTree> imports = unit.getImports();
        currentImports = imports.stream().map(it -> it.getQualifiedIdentifier().toString()).toList();
        return super.visitCompilationUnit(unit, p);
    }
    @Override
    public Void visitClass(ClassTree node, Void p){
        String name = node.getSimpleName().toString();
        if(name.isEmpty()) return super.visitClass(node, p); // 匿名クラスは回避
        ClassInfo ci = new ClassInfo();
        ci.name = stack.isEmpty() ? name : stack.peek().name + "." + name;
        ci.packageName = currentPackage;
        ci.qualifiedName = (currentPackage == null ? "" : currentPackage + ".") + ci.name;
        ci.importNames = currentImports;
        ci.kind = node.getKind().toString().toLowerCase();
        for(Modifier m : node.getModifiers().getFlags()){
            ci.modifiers.add(m.toString().toLowerCase());
        }
        ci.toStyledLine();
        if(node.getExtendsClause() != null){
            TypeMirror superType = trees.getTypeMirror(new TreePath(getCurrentPath(), node.getExtendsClause()));
            if(superType instanceof DeclaredType dt){
                String superClass = ((TypeElement) dt.asElement()).getQualifiedName().toString();
                ci.superClass = superClass;
                if(isLikeJavaAPI(superClass)){
                    classes.add(makeJavaAPIClassInfo((TypeElement) dt.asElement()));
                }
            }
        }
        for(Tree impl : node.getImplementsClause()){
            TypeMirror interfaceType = trees.getTypeMirror(new TreePath(getCurrentPath(), impl));
            if(interfaceType instanceof DeclaredType dt){
                String interfaceClass = ((TypeElement) dt.asElement()).getQualifiedName().toString();
                ci.interfaces.add(interfaceClass);
                if(isLikeJavaAPI(interfaceClass)){
                    classes.add(makeJavaAPIClassInfo((TypeElement) dt.asElement()));
                }
            }
        }
        Map<String, TypeInfo> params = new HashMap<>();
        for(Tree member : node.getMembers()){
            if(member instanceof BlockTree block){
                boolean isStatic = block.isStatic();
                List<TypeInfo> localTypes = new ArrayList<>();
                List<AssignmentInfo> ai = new ArrayList<>();
                block.getStatements().forEach(s -> {
                    scanStatement(localTypes, ai, params, s);
                });
                ci.initBlocks.add(new InitializeBlockInfo(localTypes, ai, block.toString(), isStatic));
            }
        }
        if(!stack.isEmpty()) stack.peek().nestedClasses.add(ci);
        else classes.add(ci);

        stack.push(ci);
        super.visitClass(node, p);
        stack.pop();
        return null;
    }
    @Override
    public Void visitVariable(VariableTree node, Void p){
        if(!stack.isEmpty()){
            Element el = trees.getElement(getCurrentPath());
            if(el.getKind() == ElementKind.ENUM_CONSTANT){
                stack.peek().enumConstants.add(node.getName().toString());
            }else{
                Tree parent = getCurrentPath().getParentPath().getLeaf();
                if(parent instanceof ClassTree){
                    TypeMirror mirror = trees.getTypeMirror(getCurrentPath());
                    Set<String> mods = new HashSet<>();
                    for(Modifier m : node.getModifiers().getFlags()){
                        mods.add(m.toString().toLowerCase());
                    }
                    TypeInfo type = buildTypeInfo(node.getType(), mirror);
                    TypeInfo init = buildTypeInfo(node.getInitializer(), mirror);
                    stack.peek().fields.add(new FieldInfo(
                        stack.peek().kind,
                        mods,
                        type,
                        node.getName().toString(),
                        init
                    ));
                }
            }
        }
        return super.visitVariable(node, p);
    }
    @Override
    public Void visitMethod(MethodTree node, Void p){
        if(!stack.isEmpty()){
            String parentPathClass = ""; 
            boolean isEnumConstant = false;
            TreePath parentPath = getCurrentPath().getParentPath();
            if(parentPath.getLeaf() instanceof ClassTree ct){
                TreePath grandParentPath = parentPath.getParentPath();
                if(grandParentPath.getLeaf() instanceof NewClassTree nct){
                    ExpressionTree classExpr = nct.getIdentifier();
                    TreePath varPath = grandParentPath.getParentPath();
                    if(varPath.getLeaf() instanceof VariableTree vt){
                        Element el = trees.getElement(varPath);
                        if(el.getKind() == ElementKind.ENUM_CONSTANT) isEnumConstant = true;
                    }
                }
                parentPathClass = ct.getSimpleName().toString();
            }
            if(isEnumConstant) return super.visitMethod(node, p);
            Set<String> mods = new HashSet<>();
            for(Modifier m : node.getModifiers().getFlags()){
                mods.add(m.toString().toLowerCase());
            }
            Map<String, TypeInfo> params = new HashMap<>();
            for(VariableTree pa : node.getParameters()){
                TypeMirror mirror = trees.getTypeMirror(new TreePath(getCurrentPath(), pa));
                if(mirror == null) continue;
                params.put(pa.getName().toString(), buildTypeInfo(pa.getType(), mirror));
            }
            List<TypeInfo> localTypes = new ArrayList<>();
            List<AssignmentInfo> ai = new ArrayList<>();
            if(node.getBody() != null){
                node.getBody().getStatements().forEach(s -> {
                    scanStatement(localTypes, ai, params, s);
                });
            }
            if(node.getName().contentEquals("<init>")){
                stack.peek().constructors.add(new ConstructorInfo(
                    stack.peek().kind, mods, stack.peek().name, params, localTypes, ai, node.getBody().toString(), parentPathClass
                ));
            }else{
                TypeMirror mirror = trees.getTypeMirror(new TreePath(getCurrentPath(), node.getReturnType()));
                stack.peek().methods.add(new MethodInfo(
                    stack.peek().kind,
                    mods,
                    buildTypeInfo(node.getReturnType(), mirror),
                    node.getName().toString(),
                    params,
                    localTypes,
                    ai,
                    (node.getBody() == null ? null : node.getBody().toString()),
                    parentPathClass
                ));
            }
        }
        return super.visitMethod(node, p);
    }
    private void scanStatement(List<TypeInfo> localTypes, List<AssignmentInfo> ai, Map<String, TypeInfo> params, Tree stmt){
        if(stmt == null) return;
        
        if(stmt instanceof ExpressionStatementTree est){
            scanExpression(localTypes, ai, params, est.getExpression());
        }else if(stmt instanceof VariableTree vt){
            TypeMirror mirror = trees.getTypeMirror(new TreePath(getCurrentPath(), vt));
            localTypes.add(buildTypeInfo(vt.getType(), mirror));
            scanExpression(localTypes, ai, params, vt.getInitializer());
        }else if(stmt instanceof BlockTree bt){
            bt.getStatements().forEach(s -> scanStatement(localTypes, ai, params, s));
        }else if(stmt instanceof ReturnTree rt){
            scanExpression(localTypes, ai, params, rt.getExpression());
        }else if(stmt instanceof AssertTree at){
            scanExpression(localTypes, ai, params, at.getCondition());
            scanExpression(localTypes, ai, params, at.getDetail());
        }else if(stmt instanceof SwitchTree st){
            scanExpression(localTypes, ai, params, st.getExpression());
            for(CaseTree ct : st.getCases()){
                if(ct.getStatements() != null){
                    for(StatementTree statement : ct.getStatements()){
                        scanStatement(localTypes, ai, params, statement);
                    }
                }
                ct.getExpressions().forEach(s -> scanExpression(localTypes, ai, params, s));
            }
        }else if(stmt instanceof IfTree it){
            scanExpression(localTypes, ai, params, it.getCondition());
            scanStatement(localTypes, ai, params, it.getThenStatement());
            scanStatement(localTypes, ai, params, it.getElseStatement());
        }else if(stmt instanceof ForLoopTree flt){
            scanExpression(localTypes, ai, params, flt.getCondition());
            flt.getInitializer().forEach(s -> scanStatement(localTypes, ai, params, s));
            flt.getUpdate().forEach(s -> scanStatement(localTypes, ai, params, s));
            scanStatement(localTypes, ai, params, flt.getStatement());
        }else if(stmt instanceof EnhancedForLoopTree eflt){
            scanExpression(localTypes, ai, params, eflt.getExpression());
            scanStatement(localTypes, ai, params, eflt.getStatement());
            scanStatement(localTypes, ai, params, eflt.getVariable());
        }else if(stmt instanceof WhileLoopTree wlt){
            scanExpression(localTypes, ai, params, wlt.getCondition());
            scanStatement(localTypes, ai, params, wlt.getStatement());
        }else if(stmt instanceof DoWhileLoopTree dwlt){
            scanExpression(localTypes, ai, params, dwlt.getCondition());
            scanStatement(localTypes, ai, params, dwlt.getStatement());
        }else if(stmt instanceof ThrowTree tt){
            scanExpression(localTypes, ai, params, tt.getExpression());
        }else if(stmt instanceof SynchronizedTree st){
            scanStatement(localTypes, ai, params, st.getBlock());
            scanExpression(localTypes, ai, params, st.getExpression());
        }else if(stmt instanceof TryTree tryTree){
            scanStatement(localTypes, ai, params, tryTree.getBlock());
            tryTree.getCatches().forEach(c -> {
                scanStatement(localTypes, ai, params, c.getBlock());
                scanStatement(localTypes, ai, params, c.getParameter());
            });
            scanStatement(localTypes, ai, params, tryTree.getFinallyBlock());
        }
    }
    private void scanExpression(List<TypeInfo> localTypes, List<AssignmentInfo> ai, Map<String, TypeInfo> params, ExpressionTree expr){
        if(expr == null) return;
        if(expr instanceof MethodInvocationTree mit){
            ExpressionTree methodSelect = mit.getMethodSelect();
            if(methodSelect instanceof MemberSelectTree mst1){ // メソッド呼び出し
                TypeMirror mirror = trees.getTypeMirror(new TreePath(getCurrentPath(), mst1.getExpression()));
                localTypes.add(buildTypeInfo(mst1.getExpression(), mirror));
            }
            for(ExpressionTree arg : mit.getArguments()){
                scanExpression(localTypes, ai, params, arg);
            }
        }else if(expr instanceof NewClassTree nct){
            TypeMirror mirror = trees.getTypeMirror(new TreePath(getCurrentPath(), nct));
            localTypes.add(buildTypeInfo(nct.getIdentifier(), mirror));
            for(ExpressionTree arg : nct.getArguments()){
                scanExpression(localTypes, ai, params, arg);
            }
            
        }else if(expr instanceof AssignmentTree at){
            String lhs = at.getVariable().toString();
            String rhs = at.getExpression().toString();
            String sourceType = null;
            if(rhs.startsWith("new ")) sourceType = "NEW";
            else if(params.containsKey(rhs)) sourceType = "PARAM";
            else sourceType = "REFERENCE";
            ai.add(new AssignmentInfo(lhs, rhs, sourceType));
            scanExpression(localTypes, ai, params, at.getExpression());
        }else if(expr instanceof BinaryTree bt){
            scanExpression(localTypes, ai, params, bt.getLeftOperand());
            scanExpression(localTypes, ai, params, bt.getRightOperand());
        }else if(expr instanceof MemberSelectTree mst2){ // フィールドアクセス
            TypeMirror mirror = trees.getTypeMirror(new TreePath(getCurrentPath(), mst2));
            localTypes.add(buildTypeInfo(mst2, mirror));
        }else if(expr instanceof TypeCastTree tct){
            scanExpression(localTypes, ai, params, tct.getExpression());
            TypeMirror mirror = trees.getTypeMirror(new TreePath(getCurrentPath(), tct));
            localTypes.add(buildTypeInfo(tct, mirror));
        }else if(expr instanceof LambdaExpressionTree let){
            scanStatement(localTypes, ai, params, let.getBody());
            for(VariableTree pa : let.getParameters()){
                scanStatement(localTypes, ai, params, pa);
            }
        }
    }
    private TypeMirror getTypeMirror(Tree typeTree){
        if(typeTree == null) return null;
        return trees.getTypeMirror(getCurrentPath());
    }
    private TypeInfo buildTypeInfo(Tree typeTree, TypeMirror mirror){
        if(typeTree == null) return null;
        TypeInfo info = new TypeInfo(typeTree.toString());
        info.setTypeMirror(mirror);
        info.setQualifiedName(mirror.toString());
        info.setErasedName(typeUtils.erasure(mirror).toString());
        info.setArrayType(mirror.getKind() == TypeKind.ARRAY);
        if(info.isArrayType()){
            ArrayType arr = (ArrayType) mirror;
            info.setComponentType(buildTypeInfoFromMirror(arr.getComponentType()));
        }

        if(mirror instanceof DeclaredType dt){
            List<? extends TypeMirror> args = dt.getTypeArguments();
            for(TypeMirror arg : args){
                TypeInfo g = buildTypeInfoFromMirror(arg);
                info.addGenerics(g);
            }
            if(dt instanceof TypeElement te){
                info.setQualifiedName(te.getQualifiedName().toString());
            }
        }
        return info;
    }
    private TypeInfo buildTypeInfoFromMirror(TypeMirror mirror){
        TypeInfo info = new TypeInfo(mirror.toString());
        info.setTypeMirror(mirror);
        info.setQualifiedName(mirror.toString());
        info.setErasedName(typeUtils.erasure(mirror).toString());
        info.setArrayType(mirror.getKind() == TypeKind.ARRAY);
        if(info.isArrayType()){
            ArrayType arr = (ArrayType) mirror;
            info.setComponentType(buildTypeInfoFromMirror(arr.getComponentType()));
        }

        if(mirror instanceof DeclaredType dt){
            List<? extends TypeMirror> args = dt.getTypeArguments();
            for(TypeMirror arg : args){
                info.addGenerics(buildTypeInfoFromMirror(arg));
            }
            if(dt instanceof TypeElement te){
                info.setQualifiedName(te.getQualifiedName().toString());
            }
        }
        return info;
    }
    private boolean isLikeJavaAPI(String superClass){
        if(API.contains(superClass)){
            API.remove(superClass);
            return true;
        }
        return false;
    }
    private ClassInfo makeJavaAPIClassInfo(TypeElement element){
        ClassInfo apiClass = new ClassInfo();
        apiClass.name = element.getSimpleName().toString();
        apiClass.packageName = "java";
        apiClass.qualifiedName = element.getQualifiedName().toString();
        apiClass.kind = element.getKind().toString().toLowerCase();
        for(Modifier m : element.getModifiers()){
            apiClass.modifiers.add(m.toString().toLowerCase());
        }
        apiClass.line = new StyledLine(apiClass.name,
                                       apiClass.modifiers.contains("static"),
                                       apiClass.modifiers.contains("final"),
                                       apiClass.modifiers.contains("abstract"));
        apiClass.isJavaAPI = true;
        return apiClass;
    }
}