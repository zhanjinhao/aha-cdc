package cn.addenda.ahacdc.sql;

import cn.addenda.ro.grammar.ast.expression.*;
import cn.addenda.ro.grammar.ast.expression.visitor.ExpressionVisitor;
import cn.addenda.ro.grammar.lexical.token.TokenType;

/**
 * @author addenda
 * @datetime 2022/10/5 16:16
 */
public class KeyInOrKeyEqualConditionVisitor extends ExpressionVisitor<Boolean> {

    private final String keyColumn;

    public KeyInOrKeyEqualConditionVisitor(String keyColumn) {
        this.keyColumn = keyColumn;
    }

    @Override
    public Boolean visitInCondition(InCondition inCondition) {
        // identifier是主键，且in是range模式
        return keyColumn.equals(inCondition.getIdentifier().getLiteral()) && inCondition.getRange() != null;
    }

    @Override
    public Boolean visitWhereSeg(WhereSeg whereSeg) {
        return nullAccept(whereSeg.getLogic());
    }

    @Override
    public Boolean visitLogic(Logic logic) {
        return nullAccept(logic.getLeftCurd()) && nullAccept(logic.getRightCurd());
    }

    @Override
    public Boolean visitComparison(Comparison comparison) {
        Curd comparisonSymbol = comparison.getComparisonSymbol();
        Curd leftCurd = comparison.getLeftCurd();
        if (comparisonSymbol instanceof Identifier) {
            Identifier identifier = (Identifier) comparisonSymbol;
            if (TokenType.EQUAL.equals(identifier.getName().getType()) && Boolean.TRUE.equals(nullAccept(leftCurd))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitBinaryArithmetic(BinaryArithmetic binaryArithmetic) {
        return nullAccept(binaryArithmetic.getRightCurd()) && nullAccept(binaryArithmetic.getLeftCurd());
    }

    @Override
    public Boolean visitUnaryArithmetic(UnaryArithmetic unaryArithmetic) {
        return nullAccept(unaryArithmetic.getCurd());
    }

    @Override
    public Boolean visitLiteral(Literal literal) {
        return true;
    }

    @Override
    public Boolean visitGrouping(Grouping grouping) {
        return nullAccept(grouping.getCurd());
    }

    @Override
    public Boolean visitIdentifier(Identifier identifier) {
        return keyColumn.equals(identifier.getName().getLiteral());
    }

    @Override
    public Boolean visitFunction(Function function) {
        return false;
    }

    @Override
    public Boolean visitAssignmentList(AssignmentList assignmentList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitTimeInterval(TimeInterval timeInterval) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitTimeUnit(TimeUnit timeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitIsNot(IsNot isNot) {
        return false;
    }

    @Override
    public Boolean nullAccept(Curd curd) {
        if (curd == null) {
            return true;
        }
        return curd.accept(this);
    }
}
