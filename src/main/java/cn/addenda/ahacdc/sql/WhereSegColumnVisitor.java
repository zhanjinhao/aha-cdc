package cn.addenda.ahacdc.sql;

import cn.addenda.businesseasy.util.BEArrayUtils;
import cn.addenda.ro.grammar.ast.expression.*;
import cn.addenda.ro.grammar.ast.expression.visitor.ExpressionVisitor;
import cn.addenda.ro.grammar.lexical.token.Token;

import java.util.HashSet;
import java.util.Set;

/**
 * @author addenda
 * @datetime 2022/9/4 13:41
 */
public class WhereSegColumnVisitor extends ExpressionVisitor<Set<Token>> {

    private static final WhereSegColumnVisitor instance = new WhereSegColumnVisitor();

    public static WhereSegColumnVisitor getInstance() {
        return instance;
    }

    @Override
    public Set<Token> visitInCondition(InCondition inCondition) {
        Token in = inCondition.getIn();
        Set<Token> tokenSet = new HashSet<>();
        tokenSet.add(in);
        return tokenSet;
    }

    @Override
    public Set<Token> visitWhereSeg(WhereSeg whereSeg) {
        return whereSeg.getLogic().accept(this);
    }

    @Override
    public Set<Token> visitLogic(Logic logic) {
        Set<Token> left = logic.getLeftCurd().accept(this);
        Set<Token> right = nullAccept(logic.getRightCurd());
        if (right != null) {
            left.addAll(right);
        }
        return left;
    }

    @Override
    public Set<Token> visitComparison(Comparison comparison) {
        return comparison.getLeftCurd().accept(this);
    }

    @Override
    public Set<Token> visitBinaryArithmetic(BinaryArithmetic binaryArithmetic) {
        return new HashSet<>(0);
    }

    @Override
    public Set<Token> visitUnaryArithmetic(UnaryArithmetic unaryArithmetic) {
        return new HashSet<>(0);
    }

    @Override
    public Set<Token> visitLiteral(Literal literal) {
        return new HashSet<>(0);
    }

    @Override
    public Set<Token> visitGrouping(Grouping grouping) {
        return grouping.getCurd().accept(this);
    }

    @Override
    public Set<Token> visitIdentifier(Identifier identifier) {
        return BEArrayUtils.asHashSet(identifier.getName());
    }

    @Override
    public Set<Token> visitFunction(Function function) {
        return new HashSet<>(0);
    }

    @Override
    public Set<Token> visitAssignmentList(AssignmentList assignmentList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Token> visitTimeInterval(TimeInterval timeInterval) {
        return new HashSet<>(0);
    }

    @Override
    public Set<Token> visitTimeUnit(TimeUnit timeUnit) {
        return new HashSet<>(0);
    }

    @Override
    public Set<Token> visitIsNot(IsNot isNot) {
        return new HashSet<>(0);
    }
}
