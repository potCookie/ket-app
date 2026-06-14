package com.ketapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ketapp.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
