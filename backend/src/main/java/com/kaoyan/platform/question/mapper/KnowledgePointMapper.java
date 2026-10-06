package com.kaoyan.platform.question.mapper;

import com.kaoyan.platform.question.entity.KnowledgePoint;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgePointMapper {

    List<KnowledgePoint> selectAll();

    KnowledgePoint selectById(@Param("id") Long id);

    /** Excel 导入时按名称解析知识点 */
    KnowledgePoint selectByName(@Param("name") String name);

    int insert(KnowledgePoint kp);

    List<KnowledgePoint> selectByIds(@Param("ids") List<Long> ids);
}
