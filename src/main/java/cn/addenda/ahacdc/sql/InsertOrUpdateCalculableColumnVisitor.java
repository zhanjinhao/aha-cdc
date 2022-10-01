package cn.addenda.ahacdc.sql;

import cn.addenda.ro.grammar.ast.expression.*;
import cn.addenda.ro.grammar.ast.expression.visitor.ExpressionVisitor;

import java.util.List;

/**
 * @author addenda
 * @datetime 2022/9/25 11:08
 */
public class InsertOrUpdateCalculableColumnVisitor extends ExpressionVisitor<Boolean> {

    private InsertOrUpdateCalculableColumnVisitor() {
    }

    private static final InsertOrUpdateCalculableColumnVisitor INSTANCE = new InsertOrUpdateCalculableColumnVisitor();

    public static InsertOrUpdateCalculableColumnVisitor getInstance() {
        return INSTANCE;
    }

    @Override
    public Boolean visitInCondition(InCondition inCondition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitWhereSeg(WhereSeg whereSeg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitLogic(Logic logic) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitComparison(Comparison comparison) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitBinaryArithmetic(BinaryArithmetic binaryArithmetic) {
        return nullAccept(binaryArithmetic.getLeftCurd()) && nullAccept(binaryArithmetic.getRightCurd());
    }

    @Override
    public Boolean visitUnaryArithmetic(UnaryArithmetic unaryArithmetic) {
        return unaryArithmetic.getCurd().accept(this);
    }

    @Override
    public Boolean visitLiteral(Literal literal) {
        return true;
    }

    @Override
    public Boolean visitGrouping(Grouping grouping) {
        return grouping.getCurd().accept(this);
    }

    @Override
    public Boolean visitIdentifier(Identifier identifier) {
        return false;
    }

    @Override
    public Boolean visitFunction(Function function) {
        if (!function.isIndependent()) {
            return false;
        }
        List<Curd> parameterList = function.getParameterList();
        if (parameterList == null) {
            return true;
        }
        for (Curd parameter : parameterList) {
            if (Boolean.FALSE.equals(parameter.accept(this))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Boolean visitAssignmentList(AssignmentList assignmentList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitTimeInterval(TimeInterval timeInterval) {
        return true;
    }

    @Override
    public Boolean visitTimeUnit(TimeUnit timeUnit) {
        return true;
    }

    @Override
    public Boolean visitIsNot(IsNot isNot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean nullAccept(Curd curd) {
        if (curd == null) {
            return true;
        }
        return curd.accept(this);
    }
}
