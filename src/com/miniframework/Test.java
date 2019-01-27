package com.miniframework;

import com.miniframework.db.MiniORM;
import com.miniframework.db.MiniORMImpl;
import com.miniframework.db.Param;
import com.miniframework.entity.User;

import java.util.List;

public class Test {
    public static void main(String[] args) {
        User user = new User();
        /**
         * 插入老张老李
         */
        user.setName("老张");
        user.setPassword("123456");
        MiniORM<User> miniORM = new MiniORMImpl<>(User.class);
        miniORM.insert(user);
        user.setName("老李");
        miniORM.insert(user);


        /**
         *查询名字带 ‘老’的
         */
        Param[] params = {new Param().field("name").like().value("%老%")};
        List<User> users = miniORM.query(params);
        System.out.println(users);

        /**
         *把老张改成老王
         */
        Param[] params2 = {new Param().field("name").equal().value("老张")};
        user = new User();
        user.setName("老王");
        miniORM.update(params2,user);
        users = miniORM.query(params);
        System.out.println(users);

        /**
         * 把老李删了
         */
        Param[] params3 = {new Param().field("name").equal().value("老李")};
        miniORM.delete(params3);
        users = miniORM.query(params);
        System.out.println(users);

    }

}
