package com.ramsrib.springbootmultitenant2.tenant;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;



public class MultiTenantInterceptor implements StatementInspector {

    private final Logger logger = LoggerFactory.getLogger(MultiTenantInterceptor.class);

    private static final String TENANT_ID = "tenant_id";
    //ignore table list
    private static final Set<String> ignoreTables = new HashSet<String>((Arrays.asList("zzzz")));

    @Override
    public String inspect(String sql) {
        Statement statement = null;
        try {
            //解析sql
            statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                Select select = (Select) statement;
                PlainSelect ps = (PlainSelect) select.getSelectBody();
                processSelect(ps, true);
            } else if (statement instanceof Insert) {
                processInsert((Insert) statement);
            } else if (statement instanceof Update) {
                processUpdate((Update) statement);
            } else if (statement instanceof Delete) {
                processDelete((Delete) statement);
            }
            //createTable暂时不开启
			/*if (statement instanceof CreateTable) {
				processCreate((CreateTable) statement);
			}*/
        } catch (Exception e) {
            logger.error("Statement analysis failed. Please check whether the statement is correct." , e);
        }
        logger.info("multiTenant sql result:" + statement != null ? statement.toString() : sql);
        return statement != null ? statement.toString() : sql;
    }

    /**
     * select 语句处理
     *
     * @param ps
     * @param addColumn 是否要返回tenant_id字段
     * @throws Exception
     */
    private void processSelect(PlainSelect ps, boolean addColumn) throws Exception {
        FromItem fromItem = ps.getFromItem();
        //解析查询字段里的select子查询
        List<SelectItem> selectItems = ps.getSelectItems();
        for (SelectItem selectItem : selectItems) {
            String itemStr = selectItem.toString();
            if (itemStr.toLowerCase().indexOf("select") < 0) {
                continue;
            }
            if (selectItem instanceof SelectExpressionItem) {
                SelectExpressionItem expressionItem = (SelectExpressionItem) selectItem;
                processSubSelect(expressionItem.getExpression());
            }
        }
        if (fromItem instanceof Table) {
            Table mainTable = ((Table) fromItem);
            //过滤不加tenant_id的表
            if (ignoreTables.contains(mainTable.getFullyQualifiedName())) {
                return;
            }
            //解析主表并加tenant_id
            ps.setWhere(addTenantExpression(mainTable, ps.getWhere()));
            if (addColumn) {
                Alias alias = mainTable.getAlias();
                String columnName = alias == null ? TENANT_ID : mainTable.getAlias().getName() + '.' + TENANT_ID;
                boolean hasTenant = ps.getSelectItems().stream().anyMatch(c -> c.toString().equals(columnName));
                if (!hasTenant) {
                    ps.getSelectItems().add(new SelectExpressionItem(new Column(columnName)));
                }
            }
        } else if (fromItem instanceof SubSelect) {
            //解析子查询并加tenant_id
            PlainSelect plainSelect = (PlainSelect) ((SubSelect) fromItem).getSelectBody();
            processSelect(plainSelect, false);
        }
        //解析join表并加tenant_id
        ps.setWhere(processJoin(ps.getJoins(), ps.getWhere()));
        //解析where条件里的子查询的表并加tenant_id
        processSubSelect(ps.getWhere());
        //解析having条件里的子查询的表并加tenant_id
        processSubSelect(ps.getHaving());
    }

    /**
     * insert 语句处理
     *
     * @param insert
     * @throws Exception
     */
    private void processInsert(Insert insert) throws Exception {
        if (ignoreTables.contains(insert.getTable().getFullyQualifiedName())) {
            return;
        }
        boolean hasTenant = insert.getColumns().stream().anyMatch(c -> c.getColumnName().equals(TENANT_ID));
        if (hasTenant) {
            return;
        }
        if (insert.getSelect() != null) {
            processSelect((PlainSelect) insert.getSelect().getSelectBody(), true);
        } else if (insert.getItemsList() != null) {
            ItemsList itemsList = insert.getItemsList();
            if (itemsList instanceof MultiExpressionList) {
                ((MultiExpressionList) itemsList).getExprList().forEach(el -> el.getExpressions().add(new StringValue(TenantContext.getCurrentTenant())));
            } else {
                ((ExpressionList) insert.getItemsList()).getExpressions().add(new StringValue(TenantContext.getCurrentTenant()));
            }
        } else {
            throw new Exception("Failed to process multiple-table update, please exclude the tableName or statementId.");
        }

    }

    /**
     * update 语句处理
     *
     * @param update
     * @throws Exception
     */
    private void processUpdate(Update update) throws Exception {
        if (ignoreTables.contains(update.getTable().getFullyQualifiedName())) {
            return;
        }
        update.setWhere(addTenantExpression(update.getTable(), update.getWhere()));
        //解析join表并加tenant_id
        update.setWhere(processJoin(update.getStartJoins(), update.getWhere()));
        update.setWhere(processJoin(update.getJoins(), update.getWhere()));
        //解析where条件里的子查询的表并加tenant_id
        processSubSelect(update.getWhere());
    }

    /**
     * delete 语句处理
     *
     * @param delete
     * @throws Exception
     */
    private void processDelete(Delete delete) throws Exception {
        if (ignoreTables.contains(delete.getTable().getFullyQualifiedName())) {
            return;
        }
        delete.setWhere(addTenantExpression(delete.getTable(), delete.getWhere()));
        //解析join表并加tenant_id
        delete.setWhere(processJoin(delete.getJoins(), delete.getWhere()));
        //解析where条件里的子查询的表并加tenant_id
        processSubSelect(delete.getWhere());
    }

    /**
     * createTable 语句处理
     *
     * @param createTable
     * @throws Exception
     */
    private void processCreate(CreateTable createTable) throws Exception {
        if (ignoreTables.contains(createTable.getTable().getFullyQualifiedName())) {
            return;
        }
        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setColumnName("tenant_id");
        ColDataType colDataType = new ColDataType();
        colDataType.setDataType("varchar(20) NOT NULL DEFAULT '0' COMMENT '租户id'");
        columnDefinition.setColDataType(colDataType);
        createTable.getColumnDefinitions().add(columnDefinition);
    }

    /**
     * 递归处理 子查询中设置tenant_id
     *
     * @param where 当前sql的where条件
     */
    private void processSubSelect(Expression where) throws Exception {
        PlainSelect plainSelect = null;
        if (where instanceof SubSelect) {
            plainSelect = (PlainSelect) ((SubSelect) where).getSelectBody();
            processSelect(plainSelect, false);
        } else if (where instanceof CaseExpression) {
            CaseExpression caseExpression = (CaseExpression) where;
            processSubSelect(caseExpression.getSwitchExpression());
            List<WhenClause> whenClauses = caseExpression.getWhenClauses();
            for (WhenClause whenClause : whenClauses) {
                processSubSelect(whenClause.getThenExpression());
                processSubSelect(whenClause.getWhenExpression());
            }
            processSubSelect(caseExpression.getElseExpression());
        } else if (where instanceof InExpression) {
            InExpression inExpression = (InExpression) where;
            ItemsList rightItems = inExpression.getRightItemsList();
            if (rightItems instanceof SubSelect) {
                plainSelect = (PlainSelect) ((SubSelect) rightItems).getSelectBody();
                processSelect(plainSelect, false);
            }
        } else if (where instanceof ExistsExpression) {
            ExistsExpression existsExpression = (ExistsExpression) where;
            Expression expression = existsExpression.getRightExpression();
            if (expression instanceof SubSelect) {
                plainSelect = (PlainSelect) ((SubSelect) expression).getSelectBody();
                processSelect(plainSelect, false);
            }
        } else if (where instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) where;
            //递归左边表达式
            processSubSelect(binaryExpression.getLeftExpression());
            //递归右边表达式
            processSubSelect(binaryExpression.getRightExpression());
        }

    }

    /**
     * join 语句处理
     * @param joins
     * @param where 返回where条件
     * @return
     * @throws Exception
     */
    private Expression processJoin(List<Join> joins, Expression where) throws Exception {
        if (joins != null) {
            for (Join join : joins) {
                Table joinTable = null;
                if (join.getRightItem() instanceof Table) {
                    joinTable = (Table) join.getRightItem();
                    if (ignoreTables.contains(joinTable.getFullyQualifiedName())) {
                        continue;
                    }
                } else if (join.getRightItem() instanceof SubSelect) {
                    SubSelect subSelect = (SubSelect) join.getRightItem();
                    processSelect((PlainSelect) subSelect.getSelectBody(), true);
                    List<SelectItem>  selectItems = ((PlainSelect) subSelect.getSelectBody()).getSelectItems();
                    boolean hasTenant = selectItems.stream().anyMatch(s -> TENANT_ID.equals(s.toString()));
                    if (hasTenant) {
                        joinTable = new Table();
                        joinTable.setAlias(join.getRightItem().getAlias());
                    }
                }
                if (joinTable != null) {
                    //from后面有多表的处理
                    if (join.isSimple()) {
                        where = addTenantExpression(joinTable, where);
                    }
                    if (join.getOnExpression() != null) {
                        join.setOnExpression(addTenantExpression(joinTable, join.getOnExpression()));
                    }
                }
                if (join.getOnExpression() != null) {
                    processSubSelect(join.getOnExpression());
                }
            }
        }
        return where;
    }
    /**
     * where条件加tenant_id判断条件
     *
     * @param table
     * @param where
     * @return
     * @throws Exception
     */
    private Expression addTenantExpression(Table table, Expression where) throws Exception {
        if (table == null) {
            return null;
        }
        EqualsTo equalsTo = new EqualsTo();
        Alias alias = table.getAlias();
        String columnName = alias == null ? TENANT_ID : table.getAlias().getName() + '.' + TENANT_ID;
        equalsTo.setLeftExpression(new Column(columnName));
        equalsTo.setRightExpression(new LongValue(TenantContext.getCurrentTenant()));
        if (where == null) {
            return equalsTo;
        } else {
            return new AndExpression(equalsTo, where);
        }
    }

    /**
     * 判断where条件里有没有tenant_id
     * @param where
     * @param tenantExpression
     * @return
     */
    private boolean hasTenant(Expression where, Expression tenantExpression) {
        if (where.toString().equals(tenantExpression.toString())) {
            return true;
        }
        if (where instanceof AndExpression) {
            if (hasTenant(((AndExpression) where).getLeftExpression(), tenantExpression)) {
                return true;
            }
            if (hasTenant(((AndExpression) where).getRightExpression(), tenantExpression)) {
                return true;
            }

        } else if (where instanceof OrExpression) {
            if (hasTenant(((OrExpression) where).getLeftExpression(), tenantExpression)) {
                return true;
            }
            if (hasTenant(((OrExpression) where).getRightExpression(), tenantExpression)) {
                return true;
            }
        }
        return false;
    }
}
