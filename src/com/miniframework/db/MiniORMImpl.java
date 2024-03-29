package com.miniframework.db;


import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * @see MiniORM
 **/
public class MiniORMImpl<T> implements MiniORM<T> {

    private Connection conn;
    private Class clazz;
    private Field[] fields;
    private Method[] getMethods;
    private Method[] setMethods;
    private String tableName;
    private boolean hasCreate = false;

    public MiniORMImpl(Class clazz) {
        super();
        this.conn = new DBConnection().conn;
        this.clazz = clazz;
        this.fields = this.clazz.getDeclaredFields();
        getMethods = new Method[fields.length];
        setMethods = new Method[fields.length];
        for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++) {
            try {
                getMethods[fieldIndex] = new PropertyDescriptor(fields[fieldIndex].getName(), clazz).getReadMethod();
                setMethods[fieldIndex] = new PropertyDescriptor(fields[fieldIndex].getName(), clazz).getWriteMethod();
            } catch (IntrospectionException e) {
                e.printStackTrace();
            }
        }
        this.tableName = clazz.getSimpleName();
        this.createTable();
        this.hasCreate = true;
    }

    public MiniORMImpl(Class clazz, String tableName) {
        this(clazz);
        if (!this.hasCreate)
            this.createTable();
    }

    @Override
    public int insert(T[] insertEntities) {
        String baseStr = "insert into " + this.tableName;
        StringBuffer keysBuf = new StringBuffer("(");
        for (int index = 0; index < fields.length; index++) {
            Field field = fields[index];
            keysBuf.append("`" + field.getName() + "`");
            if (index < fields.length - 1) {
                keysBuf.append(",");
            }
        }
        keysBuf.append(")");
        StringBuffer valuesBuf = new StringBuffer(" ");
        for (int entityIndex = 0; entityIndex < insertEntities.length; entityIndex++) {
            valuesBuf.append("(");
            for (int index = 0; index < getMethods.length; index++) {
                valuesBuf.append("?");
                if (index < getMethods.length - 1) {
                    valuesBuf.append(",");
                }
            }
            valuesBuf.append(")");
            if (entityIndex < insertEntities.length - 1) {
                valuesBuf.append(",");
            }
        }
        String sql = baseStr + keysBuf.toString() + " values " + valuesBuf.toString();
        PreparedStatement ptmt = null;
        try {
            ptmt = conn.prepareStatement(sql);
            int count = 1;
            for (int entityIndex = 0; entityIndex < insertEntities.length; entityIndex++) {

                T entity = insertEntities[entityIndex];
                Method setCreateDateMethod = clazz.getMethod("setCreateDate", Date.class);
                Method setUpdateDateMethod = clazz.getMethod("setUpdateDate", Date.class);
                setCreateDateMethod.invoke(entity, new Date());
                setUpdateDateMethod.invoke(entity, new Date());
                for (int index = 0; index < getMethods.length; index++) {
                    Method getMethod = getMethods[index];
                    ptmt.setObject(count, getMethod.invoke(entity));
                    count++;
                }
            }

            System.out.println(new Date() + "执行的SQL语句为:" + ptmt.toString());
            int rows = ptmt.executeUpdate();
            ptmt.close();
            return rows;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int insert(T insertEntity) {
        Object[] entityarr = new Object[1];
        entityarr[0] = insertEntity;
        return this.insert((T[]) entityarr);
    }

    @Override
    public List<T> query(Param[] params) {
        return this.query(params, null, null);
    }

    @Override
    public List<T> query(Param[] params, String orderBy, String orderMethod) {
        String baseSQL = "select * from `" + this.tableName + "` where 1=1";
        StringBuffer conditionBuf = new StringBuffer("");

        for (Param param : params) {
            conditionBuf.append(" " + param.logic + " ");
            conditionBuf.append(" " + param.field + " ");
            conditionBuf.append(param.relation + "?");
        }
        List<T> resultList = new ArrayList<T>();
        try {
            PreparedStatement ptmt = conn.prepareStatement(baseSQL + conditionBuf.toString() + (orderBy == null ? "" : orderBy + " " + orderMethod));

            int count = 1;
            for (Param param : params) {
                ptmt.setObject(count, param.value);
                count++;
            }
            System.out.println(new Date() + "执行的SQL语句为:" + ptmt.toString());
            ResultSet rst = ptmt.executeQuery();

            while (rst.next()) {
                T obj = (T) clazz.newInstance();
                for (int index = 0; index < setMethods.length; index++) {
                    Method setMethod = setMethods[index];
                    setMethod.invoke(obj, rst.getObject(fields[index].getName()));
                }
                resultList.add(obj);
            }
            ptmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return resultList;
    }

    @Override
    public Page<T> query(Param[] params, int num, int size, String orderBy, String orderMethod) {
        String baseSQL = "select count(*) from `" + this.tableName + "` where 1=1";
        StringBuffer conditionBuf = new StringBuffer("");
        for (Param param : params) {
            conditionBuf.append(" " + param.logic + " ");
            conditionBuf.append(" " + param.field + " ");
            conditionBuf.append(param.relation + ((param.relation.toLowerCase().equals("'like'")) ? "%?%" : "?"));
        }

        try {
            PreparedStatement ptmt = conn.prepareStatement(baseSQL + conditionBuf.toString());
            int count = 1;
            int total = 0;
            for (Param param : params) {
                ptmt.setObject(count, param.value);
                count++;
            }

            System.out.println(new Date() + "执行的SQL语句为:" + ptmt.toString());
            ResultSet rst = ptmt.executeQuery();
            if (rst.next()) {
                total = rst.getInt(0);
            }
            baseSQL = "select * from `" + this.tableName + "` where 1=1";
            String limitString = "limit ?,?";
            ;
            ptmt = conn.prepareStatement(baseSQL + conditionBuf + limitString + (orderBy == null ? "" : orderBy + " " + orderMethod));
            Page<T> resultPage = new Page<T>();
            count = 1;
            for (Param param : params) {
                ptmt.setObject(count, param.value);
                count++;
            }
            ptmt.setInt(count++, (num - 1) * size);
            ptmt.setInt(count, size);
            System.out.println(new Date() + "执行的SQL语句为:" + ptmt.toString());
            rst = ptmt.executeQuery();

            List<T> resultList = new ArrayList<T>();
            while (rst.next()) {
                T obj = (T) clazz.newInstance();
                for (int index = 0; index < setMethods.length; index++) {
                    Method setMethod = setMethods[index];
                    setMethod.invoke(obj, rst.getObject(fields[index].getName()));
                }
                resultList.add(obj);
            }
            resultPage.currentObjs = resultList;
            resultPage.size = size;
            resultPage.number = num;
            resultPage.total = total;

            ptmt.close();
            return resultPage;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public T queryOne(Param[] params) {
        List<T> results = this.query(params);
        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
    }

    @Override
    public int delete(Param[] params) {
        String baseSQL = "delete from `" + this.tableName + "` where 1=1";
        StringBuffer conditionBuf = new StringBuffer("");
        for (Param param : params) {
            conditionBuf.append(" " + param.logic + " ");
            conditionBuf.append(" " + param.field + " ");
            conditionBuf.append(param.relation + "?");
        }
        PreparedStatement ptmt = null;
        try {
            ptmt = conn.prepareStatement(baseSQL + conditionBuf.toString());
            int count = 1;
            for (Param param : params) {
                ptmt.setObject(count, param.value);
                count++;
            }

            System.out.println(new Date() + "执行的SQL语句为:" + ptmt.toString());

            int rows = ptmt.executeUpdate();

            ptmt.close();
            return rows;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int update(Param[] params, T newEntity) {

        String baseSQL = "update `" + this.tableName + "` set ";

        try {
            StringBuffer valuesBuf = new StringBuffer(" ");
            for (int index = 0; index < getMethods.length; index++) {

                Method getMethod = getMethods[index];
                if (getMethod.invoke(newEntity) == null && !this.fields[index].getName().equals("updateDate")) continue;
                valuesBuf.append("`" + fields[index].getName() + "`=?");
                valuesBuf.append(",");
            }
            valuesBuf.deleteCharAt(valuesBuf.lastIndexOf(","));
            StringBuffer conditionBuf = new StringBuffer("where 1=1");
            for (Param param : params) {
                conditionBuf.append(" " + param.logic + " ");
                conditionBuf.append(" " + param.field + " ");
                conditionBuf.append(param.relation + "?");
            }


            PreparedStatement ptmt = null;
            ptmt = conn.prepareStatement(baseSQL + valuesBuf.toString() + conditionBuf.toString());
            int count = 1;

            Method setUpdateDateMethod = clazz.getMethod("setUpdateDate", Date.class);
            setUpdateDateMethod.invoke(newEntity, new Date());
            for (int index = 0; index < getMethods.length; index++) {
                Method getMethod = getMethods[index];
                if (getMethod.invoke(newEntity) == null) continue;
                ptmt.setObject(count, getMethod.invoke(newEntity));
                count++;
            }
            for (Param param : params) {
                ptmt.setObject(count, param.value);
                count++;
            }
            System.out.println(new Date() + "执行的SQL语句为:" + ptmt.toString());
            int rows = ptmt.executeUpdate();
            ptmt.close();
            return rows;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return 0;
    }


    @Override
    public int update(Long id, T newEntity) {
        Param[] params = {new Param().field("id").value(new Long(123))};
        return this.update(params, newEntity);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.conn.close();
    }

    protected void createTable() {
        this.tableName = tableName;
        List<Field> fieldList = new ArrayList<Field>();
        Collections.addAll(fieldList, this.fields);
        Iterator<Field> fieldIterator = fieldList.iterator();
        boolean haveId = false, haveCreateDate = false, haveUpdateDate = false;
        while (fieldIterator.hasNext()) {
            Field field = fieldIterator.next();
            haveId = haveId || field.getName().equals("id");
            haveCreateDate = haveCreateDate || field.getName().equals("createDate");
            haveUpdateDate = haveUpdateDate || field.getName().equals("updateDate");
        }
        if (haveId && haveCreateDate && haveUpdateDate) {
            String createSQL = this.createTableSql();
            try {
                Statement statement = this.conn.createStatement();
                System.out.println(new Date() + "执行的SQL语句为 " + createSQL);
                statement.execute(createSQL);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            throw new RuntimeException("实体类不和miniorm规范（不含id或createDate或updateDate");
        }

    }

    protected String createTableSql() {
        StringBuffer createTableSqlBuf = new StringBuffer("create table if not exists `" + this.tableName + "` ");
        createTableSqlBuf.append("(");
        for (int index = 0; index < this.fields.length; index++) {
            if (this.fields[index].getName().equals("id")) {
                createTableSqlBuf.append("`id` bigint(20) NOT NULL AUTO_INCREMENT,");
            } else
                createTableSqlBuf.append("`" + this.fields[index].getName() + "` " + this.JavaTypeToCreateTableSQL(this.fields[index]));
        }
        createTableSqlBuf.append("  PRIMARY KEY (`id`) USING BTREE)");
        return createTableSqlBuf.toString();
    }

    String JavaTypeToCreateTableSQL(Field field) {
        String typeName = field.getType().getSimpleName();
        if (typeName.equals("int") || typeName.equals("Integer"))
            return "int (11) NULL DEFAULT NULL,";
        else if (typeName.toLowerCase().equals("double") || typeName.toLowerCase().equals("float"))
            return "double (16,4) NULL DEFAULT NULL,";
        else if (typeName.toLowerCase().equals("string") || typeName.toLowerCase().equals("char"))
            return "varchar (255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,";
        else if (typeName.toLowerCase().equals("long"))
            return "bigint (20) NULL DEFAULT NULL,";
        else if (typeName.equals("Date")) {
            return "datetime (0) NULL DEFAULT NULL,";
        }
        return null;
    }

}
