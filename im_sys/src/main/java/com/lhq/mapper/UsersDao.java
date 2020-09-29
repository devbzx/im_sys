package com.lhq.mapper;

import com.lhq.pojo.Users;
import org.apache.ibatis.annotations.Select;

public interface UsersDao {
    int deleteByPrimaryKey(String id);

    int insert(Users record);

    int insertSelective(Users record);

    Users selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(Users record);

    int updateByPrimaryKey(Users record);
    Users selectByUsername(String username);
}